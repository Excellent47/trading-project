package org.example.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.model.dto.EventResponse;
import org.example.model.dto.EventStatus;
import org.example.model.entity.Producer;
import org.example.service.DataPoolProcessor;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@WebServlet(
        name = "ProducerServlet",
        urlPatterns = "/api/v1/producer",
        description = "Сервлет по CRUD операциям, связанных с данными о поставщиках.")
public class ProducerServlet extends HttpServlet {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private DataPoolProcessor<Producer> dataPoolProcessor;

    @Override
    public void init() {
        dataPoolProcessor = (DataPoolProcessor<Producer>) getServletContext()
                .getAttribute("dataPoolProcessor");
    }

    /**
     * Метод для пакетного сохранения данных о поставщиках.
     * Валидирует и подготавливает данные для успешной вставки в базу.
     * Предусмотрена обработка дублей в пакете.
     * Выбирается запись 'created' дата которой наибольшая.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_CREATED);
        Set<Producer> producers = new HashSet<>(objectMapper.readValue(request.getInputStream(), new TypeReference<List<Producer>>() {
                })
                .stream()
                .sorted(Comparator.comparing(Producer::created).reversed())
                .toList());

        dataPoolProcessor.sendBatchForAsyncSaveToDataBase(producers);

        EventResponse eventResponse = new EventResponse(
                "Данные успешно прошли валидацию и были отправлены в очередь на сохранение/обновление",
                EventStatus.IN_PROGRESS,
                producers.size()
        );
        String body = objectMapper.writeValueAsString(eventResponse);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(body);
        }
    }
}
