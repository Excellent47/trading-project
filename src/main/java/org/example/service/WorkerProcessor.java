package org.example.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.config.DataBaseConfig;
import org.example.util.WorkerConsumer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WorkerProcessor - это некий пул потоков, которые держат открытое соединение с БД
 * и выполняют пакетные операции вставки/обновления данных, опрашивая DataPoolProcessor.
 * - isRunning: Признак работы воркера.
 * - waitIntervalMillis: Время(мс) ожидания повторного опроса пула данных.
 * - indexDataPool: Индекс пула.
 * - reservedConnection: Зарезервированный коннект к БД, который используется для повторных операций вставки/обновления.
 * - workerConsumer: Консьюмер в котором определяется метод для работы с данными.
 */
public class WorkerProcessor<T> implements AutoCloseable {

    private static final Logger log = LogManager.getLogger(WorkerProcessor.class);

    private final DataPoolProcessor<T> processor;
    private final ExecutorService executor;
    private final AtomicBoolean isRunning;
    private final long waitIntervalMillis;
    private final int indexDataPool;
    private Connection reservedConnection;
    private final WorkerConsumer<T> workerConsumer;

    public WorkerProcessor(DataPoolProcessor<T> processor,
                           long waitIntervalMillis,
                           int indexDataPool,
                           WorkerConsumer<T> workerConsumer) {
        this.processor = processor;
        this.workerConsumer = workerConsumer;
        this.waitIntervalMillis = waitIntervalMillis;
        try {
            this.reservedConnection = DataBaseConfig.getConnection();
        } catch (SQLException e) {
            throw new IllegalStateException("Error initial connection to DataBase");
        }
        this.executor = Executors.newSingleThreadExecutor();
        this.isRunning = new AtomicBoolean(true);
        this.indexDataPool = indexDataPool;
        executor.execute(this::processLoop);
    }

    /**
     * Опрашивает пул данных и вызывает метод WorkerConsumer`а
     * Если пул данных пуст, уходит в режим ожидания на ${waitIntervalMillis}
     */
    private void processLoop() {
        while (isRunning.get()) {
            if (!processor.isEmpty(indexDataPool)) {
                Set<T> batch = processor.pollBatch(indexDataPool, 100);
                if (!batch.isEmpty()) {
                    try (Connection connection = getReservedConnection()) {
                        workerConsumer.accept(batch, connection);
                    } catch (Exception e) {
                        log.error("Error processing batch operation", e);
                    }
                }
            } else {
                synchronized (this) {
                    try {
                        log.debug("Worker[{}] waiting - {} ms", indexDataPool, waitIntervalMillis);
                        this.wait(waitIntervalMillis);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    private Connection getReservedConnection() {
        if (reservedConnection == null) {
            throw new IllegalStateException("Connection is not reserved");
        }
        try {
            if (reservedConnection.isClosed()) {
                this.reservedConnection = DataBaseConfig.getConnection();
            }
            return reservedConnection;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed check connection status", ex);
        }
    }

    @Override
    public void close() throws SQLException {
        if (this.reservedConnection != null && !this.reservedConnection.isClosed()) {
            this.reservedConnection.close();
        }
        isRunning.set(false);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}