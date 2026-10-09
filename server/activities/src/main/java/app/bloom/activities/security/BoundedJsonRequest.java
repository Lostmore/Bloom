package app.bloom.activities.security;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

final class BoundedJsonRequest extends HttpServletRequestWrapper {
    private final byte[] body;

    BoundedJsonRequest(HttpServletRequest request, byte[] body) {
        super(request);
        this.body = body;
    }

    @Override
    public ServletInputStream getInputStream() {
        var source = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public int read() {
                return source.read();
            }

            @Override
            public int read(byte[] bytes, int offset, int length) {
                return source.read(bytes, offset, length);
            }

            @Override
            public boolean isFinished() {
                return source.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                try {
                    listener.onDataAvailable();
                    if (isFinished()) {
                        listener.onAllDataRead();
                    }
                } catch (IOException exception) {
                    listener.onError(exception);
                }
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
