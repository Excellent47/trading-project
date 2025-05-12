package org.example.config;

import org.example.model.entity.Producer;
import org.example.service.DataPoolProcessor;
import org.example.service.ProducerService;
import org.example.service.WorkerProcessor;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@WebListener
public class AppContextListener implements ServletContextListener {

    private final List<WorkerProcessor<Producer>> workerProcessors = new ArrayList<>();

    @Override
    public void contextInitialized(ServletContextEvent context) {
        ServletContext servletContext = context.getServletContext();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.yml")) {
            Properties props = new Properties();
            props.load(input);
            int workerCount = Integer.parseInt(props.getProperty("workerCount"));
            long waitIntervalMillis = Long.parseLong(props.getProperty("waitIntervalMillis"));

            DataPoolProcessor<Producer> dataPoolProcessor = new DataPoolProcessor<>(workerCount);
            for (int i = 0; i < workerCount; i++) {
                WorkerProcessor<Producer> workerProcessor = new WorkerProcessor<>(
                        dataPoolProcessor, waitIntervalMillis, i, ProducerService.batchUpsert);
                workerProcessors.add(workerProcessor);
                servletContext.setAttribute("workerProcessor_" + (i + 1), workerProcessor);
            }
            servletContext.setAttribute("dataPoolProcessor", dataPoolProcessor);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        for (WorkerProcessor<Producer> workerProcessor : workerProcessors) {
            try {
                workerProcessor.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        ServletContextListener.super.contextDestroyed(sce);
    }
}