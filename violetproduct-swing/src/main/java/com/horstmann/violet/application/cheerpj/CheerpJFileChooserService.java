package com.horstmann.violet.application.cheerpj;

import java.awt.event.HierarchyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.text.MessageFormat;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.horstmann.violet.application.menu.MenuFactory;
import com.horstmann.violet.framework.dialog.DialogFactory;
import com.horstmann.violet.framework.file.IFile;
import com.horstmann.violet.framework.file.chooser.IFileChooserService;
import com.horstmann.violet.framework.file.naming.ExtensionFilter;
import com.horstmann.violet.framework.file.naming.FileNamingService;
import com.horstmann.violet.framework.file.persistence.IFileDeleter;
import com.horstmann.violet.framework.file.persistence.IFileReader;
import com.horstmann.violet.framework.file.persistence.IFileWriter;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.InjectedBean;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.ManagedBean;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;


@ManagedBean
@ResourceBundleBean(resourceReference = MenuFactory.class)
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
        boolean stagedOnly = isSelectionOf(".png", extensions) || isSelectionOf(".zip", extensions);
        boolean fileSelected = CheerpJInterfaceService.showOpenDialog(acceptedExtensions);
        if (!fileSelected) {
            if (stagedOnly) {
                return null;
            }
            throw new IOException("File selection cancelled by user");
        }
        String filename = CheerpJInterfaceService.getFileName();
        if (filename == null || filename.trim().isEmpty()) {
            throw new IOException("No filename received after file selection");
        }
        if (stagedOnly) {
            // Only the pixels of an image are kept (in the diagram), and the caller unpacks an archive
            // itself: no need for persistent storage.
            return new CheerpJFileReader(STAGING_DIRECTORY, filename);
        }
        // JS staged the file to /str/ via cheerpOSAddStringFile. /str/ is in-memory only, so copy it
        // to the persistent /files/ storage: recent/opened file entries then survive a page reload.
        String storedFilename = getStoredFilename(filename);
        File target = new File(STORAGE_DIRECTORY, storedFilename);
        if (target.exists() && !confirmOverwrite(storedFilename)) {
            return null;
        }
        copyToStorage(new File(STAGING_DIRECTORY, filename), target);
        return new CheerpJFileReader(STORAGE_DIRECTORY, storedFilename);
    }

    /**
     * @return the name under which a picked diagram is stored: without the copy number of a file downloaded again
     * ("orders.class.violet (2).html" → "orders.class.violet.html"), so that it is compared with the stored diagrams by its
     * name and type
     */
    private String getStoredFilename(String pickedFilename) {
        String result = getFileNamingService().getFilenameWithoutCopyNumber(pickedFilename);
        return result != null ? result : pickedFilename;
    }

    private FileNamingService getFileNamingService() {
        if (this.fileNamingService == null) {
            BeanInjector.getInjector().inject(this);
        }
        return this.fileNamingService;
    }

    @InjectedBean
    private FileNamingService fileNamingService;

    private DialogFactory getDialogFactory() {
        if (this.dialogFactory == null) {
            BeanInjector.getInjector().inject(this);
        }
        return this.dialogFactory;
    }

    @InjectedBean
    private DialogFactory dialogFactory;

    /**
     * Asks the name of a diagram saved to the browser storage, in a dialog of the application (a page on a small screen)
     *
     * @return the typed name, or null if cancelled
     */
    private String askFilename(String defaultName) {
        JOptionPane optionPane = new JOptionPane("File name:", JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        optionPane.setWantsInput(true);
        optionPane.setInitialSelectionValue(defaultName);
        // The field takes the focus once shown, as JOptionPane.showInputDialog does
        optionPane.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && optionPane.isShowing()) {
                SwingUtilities.invokeLater(optionPane::selectInitialValue);
            }
        });
        getDialogFactory().showDialog(optionPane, "Save As", true);
        Object value = optionPane.getValue();
        Object input = optionPane.getInputValue();
        boolean isAccepted = value instanceof Integer && ((Integer) value).intValue() == JOptionPane.OK_OPTION;
        return isAccepted && input instanceof String ? (String) input : null;
    }

    /**
     * Asks before a picked file replaces the diagram of the same name kept in the browser storage
     *
     * @return true if the user accepts to replace it
     */
    private boolean confirmOverwrite(String filename) {
        JOptionPane optionPane = new JOptionPane(MessageFormat.format(getOverwriteConfirmMessage(), filename),
                JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION);
        optionPane.setInitialValue(Integer.valueOf(JOptionPane.NO_OPTION));
        getDialogFactory().showDialog(optionPane, getOverwriteTitle(), true);
        Object value = optionPane.getValue();
        return value instanceof Integer && ((Integer) value).intValue() == JOptionPane.YES_OPTION;
    }

    private String getOverwriteTitle() {
        injectResources();
        return this.overwriteTitle;
    }

    private String getOverwriteConfirmMessage() {
        injectResources();
        return this.overwriteConfirmMessage;
    }

    /**
     * Texts are read on first use only: this service is created before the locale is set (-english)
     */
    private void injectResources() {
        if (this.overwriteConfirmMessage == null) {
            ResourceBundleInjector.getInjector().inject(this);
        }
    }

    @ResourceBundleBean(key = "dialog.import_overwrite.title")
    private String overwriteTitle;

    @ResourceBundleBean(key = "dialog.import_overwrite.confirm")
    private String overwriteConfirmMessage;

    private boolean isSelectionOf(String extension, ExtensionFilter... extensions) {
        if (extensions != null) {
            for (ExtensionFilter filter : extensions) {
                if (filter != null && extension.equalsIgnoreCase(filter.getExtension())) {
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
            String input = askFilename(defaultName);
            if (input == null || input.trim().isEmpty()) {
                return null;
            }
            String filename = input.trim();
            if (!filename.toLowerCase().endsWith(extension.toLowerCase())) {
                filename = filename + extension;
            }
            return new CheerpJStorageFileWriter(filename);
        }
        return new CheerpJDownloadFileWriter((suggestedFilename != null) ? suggestedFilename : DEFAULT_BASENAME + extension);
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

    /**
     * Copies then deletes, as only these operations are known to work on the browser storage. The file always ends in /files, even
     * if it was still in the staging directory.
     */
    @Override
    public void renameFile(IFile file, String newFilename) throws IOException {
        File source = new File(file.getDirectory(), file.getFilename());
        File target = new File(STORAGE_DIRECTORY, newFilename);
        if (target.exists()) {
            throw new FileAlreadyExistsException(newFilename);
        }
        if (source.exists()) {
            copyToStorage(source, target);
            source.delete();
        }
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