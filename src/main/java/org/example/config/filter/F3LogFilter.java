package org.example.config.filter;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.example.config.cache.CachedBodyHttpServletRequest;
import org.example.config.cache.CachedBodyHttpServletResponse;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.example.config.cache.CachedBodyHttpServletRequest.POST_METHODS;

/**
 * Добавлен заголовок X_REQUEST_ID для трассировки логов, так же установлен в контекст для корректного парсинга логов.
 */
@WebFilter(
        urlPatterns = "/*",
        description = "Фильтр для глобального логирования всех запросов/ответов API")
public class F3LogFilter implements Filter {

    private static final Logger log = LogManager.getLogger(F3LogFilter.class);
    private static final String X_REQUEST_ID = "X-Request-ID";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        CachedBodyHttpServletRequest httpRequest = (CachedBodyHttpServletRequest) request;
        CachedBodyHttpServletResponse httpResponse = (CachedBodyHttpServletResponse) response;

        ThreadContext.put("remoteAddress", httpRequest.getRemoteAddr());
        ThreadContext.put("requestURI", httpRequest.getRequestURI());
        ThreadContext.put("method", httpRequest.getMethod());
        ThreadContext.put("sessionId", httpRequest.getSession().getId());

        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = httpRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String value = httpRequest.getHeader(headerName);
            headers.put(headerName, value);
        }

        String requestId = headers.getOrDefault("x-request-id", UUID.randomUUID().toString());
        ThreadContext.put(X_REQUEST_ID, requestId);
        httpResponse.addHeader(X_REQUEST_ID, requestId);

        Map<String, String> params = new HashMap<>();
        Enumeration<String> parameterNames = httpRequest.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            params.put(paramName, httpRequest.getParameter(paramName));
        }

        try {
            if (POST_METHODS.contains(httpRequest.getMethod())) {
                logRequestWithBody(headers, params, httpRequest.getRequestBody());
            } else {
                logRequestWithHeadersAndParams(headers, params);
            }

            chain.doFilter(httpRequest, httpResponse);

            Map<String, String> resHeaders = httpResponse.getHeaderNames()
                    .stream()
                    .collect(Collectors.toMap(e -> e, httpResponse::getHeader));
            logResponseWithBody(httpResponse.getStatus(), resHeaders, httpResponse.getBody());
        } finally {
            ThreadContext.clearAll();
        }
    }

    private void logRequestWithHeadersAndParams(Map<String, String> headers, Map<String, String> params) {
        log.trace("Request - Headers: {}. Params: {}", headers, params);
    }

    private void logRequestWithBody(Map<String, String> headers, Map<String, String> params, String body) {
        log.trace("Request - Headers: {}. Params: {}.\nBody: {}", headers, params, body);
    }

    private void logResponseWithBody(int status, Map<String, String> headers, String body) {
        log.trace("Response({}) - Headers: {}.\nBody: {}", status, headers, body);
    }
}
