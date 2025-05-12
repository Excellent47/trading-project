package org.example.config.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.dto.ErrorResponse;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebFilter(
        urlPatterns = "/*",
        description = "Фильтр для глобальной обработки ошибок")
public class F4ExceptionFilter implements Filter {

    private static final Logger log = LogManager.getLogger(F4ExceptionFilter.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        try {
            chain.doFilter(httpRequest, httpResponse);
        } catch (Exception ex) {
            handleException(ex, httpRequest, httpResponse, objectMapper, 500);
        }
    }

    public static void handleException(Exception ex, HttpServletRequest request, HttpServletResponse response,
                                       ObjectMapper objectMapper, int httpStatus) {
        response.setStatus(httpStatus);
        ErrorResponse error = new ErrorResponse(
                ex.getMessage(),
                httpStatus,
                request.getRequestURI(),
                ex.getClass().getSimpleName()
        );
        try {
            String body = objectMapper.writeValueAsString(error);
            try (PrintWriter writer = response.getWriter()) {
                writer.write(body);
            }
        } catch (IOException e) {
            log.error("Ошибка парсинга или записи в тело сообщения", e);
        }
    }
}
