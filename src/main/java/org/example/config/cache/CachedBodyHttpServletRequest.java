package org.example.config.cache;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Кешируется запрос, для оптимизации последующего чтения.
 * Предусмотрено ограничение на максимальный размер. Значение вынесено в конфигурационный файл
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    public static final Set<String> POST_METHODS = Set.of("POST", "PATCH", "PUT");
    private byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request, int maxBodyRequestSize) throws IOException {
        super(request);
        if (POST_METHODS.contains(request.getMethod())) {
            try (BufferedReader reader = request.getReader()) {
                String body = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                cachedBody = body.getBytes(StandardCharsets.UTF_8);
                if (maxBodyRequestSize < cachedBody.length) {
                    throw new IllegalArgumentException("Сообщение превышает установленный размер максимального кол-ва байтов - " + maxBodyRequestSize);
                }
            }
        }
    }

    public String getRequestBody() {
        if (cachedBody.length == 0) {
            return null;
        }
        return new String(cachedBody, StandardCharsets.UTF_8);
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);
        return new BufferedReader(new InputStreamReader(byteArrayInputStream));
    }

    private static class CachedBodyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream inputStream;

        public CachedBodyServletInputStream(byte[] cachedBody) {
            this.inputStream = new ByteArrayInputStream(cachedBody);
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read() {
            return inputStream.read();
        }
    }
}
