package org.example.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class DataPoolProcessor<T> {

    private static final Logger log = LogManager.getLogger(DataPoolProcessor.class);

    private final AtomicInteger counter = new AtomicInteger(0);
    private final List<ConcurrentLinkedDeque<T>> queues = new ArrayList<>();
    private final int workerCount;

    public DataPoolProcessor(int workerCount) {
        this.workerCount = workerCount;
        for (int i = 0; i < workerCount; i++) {
            queues.add(new ConcurrentLinkedDeque<>());
        }
        log.info("DataPoolProcessor initialize with workerCount: {}", workerCount);
    }

    /**
     * Метод используется для добавления данных в общий пул. Пул делится на партиции.
     * Кол-во партиций зависит от кол-ва WorkerProcessor`ов. Индекс вычисляется по стратегии RoundRobin.
     * @param items Список элементов для отправки в базу данных на сохранение/обновление.
     */
    public void sendBatchForAsyncSaveToDataBase(Set<T> items) {
        int index = counter.getAndIncrement() % workerCount;
        log.info("Add batch[{}] to queues[{}] for save to database", items.size(), index);
        queues.get(index).addAll(items);
    }

    /**
     * WorkerProcessor постоянно опрашивает зарезервированную за ним область пула данных для insert/update операций в БД
     * @param index из какой партиции брать данные
     * @param batchSize размер вычитки сообщений за раз
     * @return Возвращает уникальный набор элементов согласно типизации в конструкторе
     */
    public Set<T> pollBatch(int index, int batchSize) {
        Set<T> batch = new HashSet<>(batchSize);
        for (int i = 0; i < batchSize; i++) {
            T item = queues.get(index).pollFirst();
            if (item == null) {
                break;
            }
            batch.add(item);
        }
        return batch;
    }

    public boolean isEmpty(int index) {
        return queues.get(index).isEmpty();
    }
}