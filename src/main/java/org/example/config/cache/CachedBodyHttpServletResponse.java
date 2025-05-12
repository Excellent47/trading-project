package org.example.config.cache;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Кешируется ответ, для оптимизации последующего чтения.
 */
public class CachedBodyHttpServletResponse extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream contentBuffer = new ByteArrayOutputStream();
    private ServletOutputStream outputStream;
    private PrintWriter writer;
    private boolean committed = false;

    public CachedBodyHttpServletResponse(HttpServletResponse response) {
        super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() {
        if (writer != null) {
            throw new IllegalStateException("getWriter() has already been called for this response");
        }

        if (outputStream == null) {
            outputStream = new CachedServletOutputStream(contentBuffer);
        }
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (outputStream != null) {
            throw new IllegalStateException("getOutputStream() has already been called for this response");
        }

        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(contentBuffer, getCharacterEncoding()));
        }
        return writer;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (!committed) {
            byte[] content = contentBuffer.toByteArray();
            super.getOutputStream().write(content);
            super.flushBuffer();
            committed = true;
        }
    }

    public String getBody() {
        byte[] bytes = contentBuffer.toByteArray();

        if (bytes.length == 0) {
            return null;
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static class CachedServletOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream buffer;

        public CachedServletOutputStream(ByteArrayOutputStream buffer) {
            this.buffer = buffer;
        }

        @Override
        public void write(int b) {
            buffer.write(b);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener listener) {
            throw new UnsupportedOperationException("Not implemented");
        }
    }
}
