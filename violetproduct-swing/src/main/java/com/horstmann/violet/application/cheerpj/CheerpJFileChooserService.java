package com.horstmann.violet.application.cheerpj;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.JOptionPane;

import com.horstmann.violet.framework.file.IFile;
import com.horstmann.violet.framework.file.chooser.IFileChooserService;
import com.horstmann.violet.framework.file.naming.ExtensionFilter;
import com.horstmann.violet.framework.file.persistence.IFileDeleter;
import com.horstmann.violet.framework.file.persistence.IFileReader;
import com.horstmann.violet.framework.file.persistence.IFileWriter;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.ManagedBean;


@ManagedBean
public class CheerpJFileChooserService implements IFileChooserService {

    private static final String DEFAULT_BASENAME = "diagram";

    private static final String STAGING_DIRECTORY = "/str";

    private static final String STORAGE_DIRECTORY = "/files";

    @Override
    public boolean isWebStart() {
        return false;
    }

    @Override
    public IFileReader chooseAndGetFileReader(ExtensionFilter... extensions) throws IOException {
        String acceptedExtensions = buildAcceptedExtensions(extensions);
        boolean imageExpected = isImageSelection(extensions);
        boolean fileSelected = CheerpJInterfaceService.showOpenDialog(acceptedExtensions);
        if (!fileSelected) {
            if (imageExpected) {
                return null;
            }
            throw new IOException("File selection cancelled by user");
        }
        String filename = CheerpJInterfaceService.getFileName();
        if (filename == null || filename.trim().isEmpty()) {
            throw new IOException("No filename received after file selection");
        }
        if (imageExpected) {
            // Only the pixels of an image are kept (in the diagram): no need for persistent storage.
            return new CheerpJFileReader(STAGING_DIRECTORY, filename);
        }
        // JS staged the file to /str/ via cheerpOSAddStringFile. /str/ is in-memory only, so copy it
        // to the persistent /files/ storage: recent/opened file entries then survive a page reload.
        copyToStorage(new File(STAGING_DIRECTORY, filename), new File(STORAGE_DIRECTORY, filename));
        return new CheerpJFileReader(STORAGE_DIRECTORY, filename);
    }

    private boolean isImageSelection(ExtensionFilter... extensions) {
        if (extensions != null) {
            for (ExtensionFilter filter : extensions) {
                if (filter != null && ".png".equalsIgnoreCase(filter.getExtension())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void copyToStorage(File source, File target) throws IOException {
        try (InputStream in = new FileInputStream(source); OutputStream out = new FileOutputStream(target)) {
            in.transferTo(out);
        }
    }

    @Override
    public IFileReader getFileReader(IFile file) throws IOException {
        if (file == null) {
            throw new IOException("No file specified");
        }
        String dir = file.getDirectory();
        if (dir == null || dir.trim().isEmpty()) {
            dir = "/files";
        }
        return new CheerpJFileReader(dir, file.getFilename());
    }

    @Override
    public IFileWriter chooseAndGetFileWriter(ExtensionFilter... extensions) throws IOException {
        return chooseAndGetFileWriter(null, extensions);
    }

    @Override
    public IFileWriter chooseAndGetFileWriter(String suggestedFilename, ExtensionFilter... extensions) throws IOException {
        String extension = resolveDefaultExtension(extensions);
        if (extension.toLowerCase().endsWith(".violet.html")) {
            String defaultName = (suggestedFilename != null) ? suggestedFilename : DEFAULT_BASENAME + extension;
            String input = (String) JOptionPane.showInputDialog(null, "File name:", "Save As",
                    JOptionPane.PLAIN_MESSAGE, null, null, defaultName);
            if (input == null || input.trim().isEmpty()) {
                return null;
            }
            String filename = input.trim();
            if (!filename.toLowerCase().endsWith(extension.toLowerCase())) {
                filename = filename + extension;
            }
            return new CheerpJStorageFileWriter(filename);
        }
        return new CheerpJDownloadFileWriter(DEFAULT_BASENAME + extension);
    }

    @Override
    public IFileWriter getFileWriter(IFile file) throws IOException {
        String filename = file == null ? null : file.getFilename();
        if (filename == null || filename.trim().isEmpty()) {
            filename = DEFAULT_BASENAME + ".violet.html";
        }
        return new CheerpJStorageFileWriter(filename.trim());
    }

    @Override
    public IFileDeleter getFileDeleter(IFile file) {
        String filename = file == null ? null : file.getFilename();
        if (filename == null || filename.trim().isEmpty()) {
            return () -> {};
        }
        return new CheerpJStorageFileDeleter(filename.trim());
    }

    @Override
    public String getTempDirectory() {
        return "/files";
    }

    /**
     * Build accepted file extensions string for HTML5 file input.
     * Format: ".ext1,.ext2,.ext3"
     */
    private String buildAcceptedExtensions(ExtensionFilter... extensions) {
        if (extensions == null || extensions.length == 0) {
            return ".violet.html";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < extensions.length; i++) {
            if (extensions[i] != null) {
                String ext = extensions[i].getExtension();
                if (ext != null && !ext.trim().isEmpty()) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    sb.append(ext);
                }
            }
        }
        
        return sb.length() > 0 ? sb.toString() : ".violet.html";
    }

    private String resolveDefaultExtension(ExtensionFilter... extensions) {
        if (extensions != null && extensions.length > 0 && extensions[0] != null) {
            String extension = extensions[0].getExtension();
            if (extension != null && !extension.trim().isEmpty()) {
                return extension;
            }
        }
        return ".violet.html";
    }
}