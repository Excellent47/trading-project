package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.model.dto.AvgPriceResponse;
import org.example.service.ProducerService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(
        name = "ProductServlet",
        urlPatterns = "/api/v1/product",
        description = "Сервлет по CRUD операциям, связанных с данными о продуктах.")
public class ProductServlet extends HttpServlet {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * Метод для получения средней цены продукта
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        long productId = Long.parseLong(req.getParameter("product_id"));
        Double avgPrice = ProducerService.getAvgPriceByProductId(productId);
        AvgPriceResponse response = new AvgPriceResponse("Среднее цена по продукту", productId, avgPrice);
        String body = objectMapper.writeValueAsString(response);
        try (PrintWriter writer = resp.getWriter()) {
            writer.write(body);
        }
    }
}
