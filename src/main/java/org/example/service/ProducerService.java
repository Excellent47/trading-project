package org.example.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.config.DataBaseConfig;
import org.example.model.entity.Producer;
import org.example.util.WorkerConsumer;

import java.sql.*;

/**
 * Класс обслуживает все sql операции с таблицой public.producers
 */
public class ProducerService {

    private static final Logger log = LogManager.getLogger(ProducerService.class);

    private static final String UPSERT_SQL_QUERY = """
            INSERT INTO public.producers AS p (id, product_id, producer_name, price, created, updated)
                        VALUES (?, ?, ?, ?, ?, ?)
                        ON CONFLICT (product_id, producer_name)
                        DO UPDATE SET
                            price = EXCLUDED.price,
                            updated = now()
                        WHERE p.created < EXCLUDED.created
            """;

    private static final String AVG_PRICE_SQL_QUERY = """
            select avg(p.price)
            from public.producers p
            where p.product_id = ?
            """;

    public static final WorkerConsumer<Producer> batchUpsert = (set, connection) -> {
        try (PreparedStatement ps = connection.prepareStatement(UPSERT_SQL_QUERY)) {
            connection.setAutoCommit(false);

            for (Producer producer : set) {
                ps.setObject(1, producer.id(), Types.OTHER);
                ps.setLong(2, producer.productId());
                ps.setString(3, producer.producerName());
                ps.setInt(4, producer.price());
                ps.setTimestamp(5, Timestamp.valueOf(producer.created()));
                ps.setTimestamp(6, producer.updated() != null ? Timestamp.valueOf(producer.updated()) : null);
                ps.addBatch();
            }

            int[] updateCounts = ps.executeBatch();

            connection.commit();

            log.info("Успешно обработано {} записей", updateCounts.length);
        } catch (SQLException ex) {
            log.error("Ошибка обработки sql запроса. Причина: {}", ex.getMessage(), ex);
            connection.rollback();
            throw ex;
        }
    };

    public static Double getAvgPriceByProductId(Long productId) {
        try (Connection connection = DataBaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(AVG_PRICE_SQL_QUERY)) {
            ps.setLong(1, productId);
            try (ResultSet resultSet = ps.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble(1);
                } else {
                    return 0.0;
                }
            }
        } catch (SQLException ex) {
            log.error("Ошибка обработки sql запроса. Причина: {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }
}