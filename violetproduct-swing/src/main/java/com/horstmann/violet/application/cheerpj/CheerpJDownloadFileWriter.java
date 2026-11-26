package com.horstmann.violet.application.cheerpj;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.horstmann.violet.framework.file.IFile;
import com.horstmann.violet.framework.file.persistence.IFileWriter;

public class CheerpJDownloadFileWriter implements IFileWriter {

    private final String filename;
    private final BrowserDownloadOutputStream stream;

    public CheerpJDownloadFileWriter(String filename) {
        this.filename = filename;
        this.stream = new BrowserDownloadOutputStream(filename);
    }

    @Override
    public OutputStream getOutputStream() {
        return stream;
    }

    @Override
    public IFile getFileDefinition() {
        return new IFile() {
            @Override
            public String getDirectory() {
                return "browser";
            }

            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private static String guessMimeType(String filename, byte[] content) {
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".zip")) {
            return "application/zip";
        }
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            String contentText = new String(content, StandardCharsets.UTF_8);
            if (contentText.contains("<html")) {
                return "text/html;charset=UTF-8";
            }
            if (contentText.contains("<svg")) {
                return "image/svg+xml";
            }
        }
        return "application/octet-stream";
    }

    private static final class BrowserDownloadOutputStream extends ByteArrayOutputStream {

        private final String filename;
        private boolean closed = false;

        private BrowserDownloadOutputStream(String filename) {
            this.filename = filename;
        }

        @Override
        public void close() throws IOException {
            if (this.closed) {
                return;
            }
            this.closed = true;
            super.close();
            byte[] content = toByteArray();
            String mimeType = guessMimeType(filename, content);
            CheerpJInterfaceService.downloadFile(filename, mimeType, content);
        }
    }
}