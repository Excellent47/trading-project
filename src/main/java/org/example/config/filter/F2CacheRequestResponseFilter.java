package org.example.config.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.config.cache.CachedBodyHttpServletRequest;
import org.example.config.cache.CachedBodyHttpServletResponse;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Переопределены ServletRequest, ServletResponse для кеширования тел запросов/ответов.
 * Добавлен лимит размера сообщения(вынесен в конфиг) и обработка ошибки в случае превышения.
 */
@WebFilter(
        urlPatterns = "/*",
        description = "Фильтр для кеширования body всех POST запросов, а также все ответы API",
        initParams = {
                @WebInitParam(name = "maxBodyRequestSize", value = "")})
public class F2CacheRequestResponseFilter implements Filter {

    private static final Logger log = LogManager.getLogger(F2CacheRequestResponseFilter.class);

    private int maxBodyRequestSize = 1024;

    @Override
    public void init(FilterConfig filterConfig) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.yml")) {
            Properties props = new Properties();
            props.load(input);
            this.maxBodyRequestSize = Integer.parseInt(props.getProperty("maxBodyRequestSize"));
            log.info("F1CacheRequestResponseFilter initialized. maxBodyRequestSize: {} bytes", maxBodyRequestSize);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            httpResponse = new CachedBodyHttpServletResponse((HttpServletResponse) response);
            httpRequest = new CachedBodyHttpServletRequest(httpRequest, maxBodyRequestSize);
            chain.doFilter(httpRequest, httpResponse);
        } catch (IllegalArgumentException ex) {
            F4ExceptionFilter.handleException(ex, httpRequest, httpResponse, new ObjectMapper(), 400);
            chain.doFilter(httpRequest, httpResponse);
        } finally {
            httpResponse.flushBuffer();
        }
    }
}
