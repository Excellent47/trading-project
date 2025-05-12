package org.example.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

@FunctionalInterface
public interface WorkerConsumer<T> {

    void accept(Set<T> set, Connection connection) throws SQLException;
}
