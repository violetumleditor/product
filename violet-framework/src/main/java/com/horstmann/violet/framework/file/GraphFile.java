package com.horstmann.violet.framework.file;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import com.horstmann.violet.framework.dialog.DialogFactory;
import com.horstmann.violet.framework.file.chooser.IFileChooserService;
import com.horstmann.violet.framework.file.export.FileExportService;
import com.horstmann.violet.framework.file.naming.ExtensionFilter;
import com.horstmann.violet.framework.file.naming.FileNamingService;
import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.IFileReader;
import com.horstmann.violet.framework.file.persistence.IFileWriter;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.InjectedBean;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.printer.PrintEngine;
import com.horstmann.violet.framework.userpreferences.PreferredFile;
import com.horstmann.violet.framework.userpreferences.UserPreferencesService;
import com.horstmann.violet.product.diagram.abstracts.IGraph;

public class GraphFile implements IGraphFile
{
    /**
     * Creates a new graph file with a new graph instance
     *
     * @param graphClass
     */
    public GraphFile(Class<? extends IGraph> graphClass)
    {
        ResourceBundleInjector.getInjector().inject(this);
        BeanInjector.getInjector().inject(this);
        try
        {
            this.graph = graphClass.getDeclaredConstructor().newInstance();
        }
        catch (InstantiationException | IllegalAccessException | NoSuchMethodException | java.lang.reflect.InvocationTargetException e)
        {
            DialogFactory.getInstance().showErrorDialog(e.getMessage());
            throw new RuntimeException(e);
        }
        String extension = this.fileNamingService.getExtensionFilter(this.graph).getExtension();
        this.currentFilename = TempFilenameGenerator.generate() + extension;
        this.currentDirectory = this.fileChooserService.getTempDirectory();
        this.hasTemporaryFilename = true;
    }

    /**
     * Constructs a graph file from an existing file
     *
     * @param file
     */
    public GraphFile(IFile file) throws IOException
    {
        ResourceBundleInjector.getInjector().inject(this);
        BeanInjector.getInjector().inject(this);
        IFileReader fileOpener = fileChooserService.getFileReader(file);
        if (fileOpener == null)
        {
            throw new IOException("Open file action cancelled by user");
        }
        InputStream in = fileOpener.getInputStream();
        if (in != null)
        {
			this.graph = this.filePersistenceService.read(in);
			this.currentFilename = fileOpener.getFileDefinition().getFilename();
            this.currentDirectory = fileOpener.getFileDefinition().getDirectory();
            this.hasTemporaryFilename = file.isTemporaryFilename();
        }
        else
        {
            throw new IOException("Unable to read file " + fileOpener.getFileDefinition().getFilename() + " from location " +
                    fileOpener.getFileDefinition().getDirectory());
        }
    }

    @Override
    public IGraph getGraph()
    {
        return this.graph;
    }

    @Override
    public String getFilename()
    {
        return this.currentFilename;
    }

    @Override
    public String getDirectory()
    {
        return this.currentDirectory;
    }

    @Override
    public void setSaveRequired()
    {
        this.isSaveRequired = true;
        fireGraphModified();
    }

    @Override
    public boolean isSaveRequired()
    {
        return this.isSaveRequired;
    }

    @Override
    public boolean isTemporaryFilename()
    {
        return this.hasTemporaryFilename;
    }

    @Override
    public void delete()
    {
        if (this.currentDirectory == null || this.currentFilename == null)
        {
            return;
        }
        this.fileChooserService.getFileDeleter(this).delete();
    }

    @Override
    public void save(boolean allowNewLocation)
    {
        // ORDER MATTERS: the temporary-filename redirect must come BEFORE the isSaveRequired guard.
        // Auto-save (allowNewLocation=false) writes the temp file and clears isSaveRequired.
        // If the isSaveRequired guard were first, a subsequent user-initiated save (allowNewLocation=true)
        // would return immediately without ever prompting for a real filename.
        if (isTemporaryFilename() && allowNewLocation)
        {
            saveToNewLocation();
            return;
        }
        if (!this.isSaveRequired)
        {
            return;
        }
        try
        {
            IFileWriter fileSaver = this.fileChooserService.getFileWriter(this);
            OutputStream outputStream = fileSaver.getOutputStream();
            this.filePersistenceService.write(this.graph, outputStream);
            outputStream.close();
            this.isSaveRequired = false;
            this.currentFilename = fileSaver.getFileDefinition().getFilename();
            this.currentDirectory = fileSaver.getFileDefinition().getDirectory();
            // Notify only once the location is up to date, so listeners record where the file really is
            fireGraphSaved();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveToNewLocation()
    {
        // Remember the old temp location so we can clean it up after a successful save.
        final String oldFilename  = this.hasTemporaryFilename ? this.currentFilename  : null;
        final String oldDirectory = this.hasTemporaryFilename ? this.currentDirectory : null;

        try
        {
            IFileWriter fileSaver = getFileSaver(true);
            if (fileSaver == null)
            {
                // User cancelled the dialog — keep whatever state we have.
                return;
            }
            OutputStream outputStream = fileSaver.getOutputStream();
            this.filePersistenceService.write(this.graph, outputStream);
            outputStream.close();
            this.isSaveRequired = false;
            this.currentFilename = fileSaver.getFileDefinition().getFilename();
            this.currentDirectory = fileSaver.getFileDefinition().getDirectory();
            this.hasTemporaryFilename = false;
            fireGraphSaved();

            // Delete the old temp file now that the real file is saved, and forget it in user preferences.
            // Skipped when the user kept the temp name at the same location: it IS the real file now.
            boolean isSameLocation = oldFilename != null && oldFilename.equals(this.currentFilename)
                    && oldDirectory != null && oldDirectory.equals(this.currentDirectory);
            if (oldFilename != null && oldDirectory != null && !isSameLocation)
            {
                final String fd = oldDirectory;
                final String fn = oldFilename;
                IFile oldTempFile = new IFile() {
                    @Override public String getDirectory() { return fd; }
                    @Override public String getFilename()  { return fn; }
                };
                this.fileChooserService.getFileDeleter(oldTempFile).delete();
                this.userPreferencesService.removeRecentFile(oldTempFile);
                this.userPreferencesService.removeOpenedFile(oldTempFile);
            }
        }
        catch (IOException e1)
        {
            String message = MessageFormat.format(fileExportErrorMessage, e1.getMessage());
            JOptionPane.showMessageDialog(null, message, fileExportError, JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void rename(String newFilename) throws IOException
    {
        if (newFilename.equals(this.currentFilename))
        {
            return;
        }
        this.fileChooserService.renameFile(this, newFilename);
        IFile oldFile = new PreferredFile(this.currentDirectory, this.currentFilename);
        this.currentFilename = newFilename;
        this.hasTemporaryFilename = false;
        this.userPreferencesService.removeRecentFile(oldFile);
        this.userPreferencesService.removeOpenedFile(oldFile);
        // Also creates the file if it has never been written, and tells listeners about the new name
        this.isSaveRequired = true;
        save(false);
    }

    /**
     * Returns a IFileSaver instance. Then, this object allows to save graph content. If the graph has never been saved, the
     * FileChooserService<br/>
     * will open a dialog box to select a location. If not, the returned IFileSaver will automatically be bound to the last saving
     * location.<br/>
     * You can also force the FileChooserService to open the dialog box with the given argument.<br/>
     *
     * @param isAskedForNewLocation if true, then the FileChooser will open a dialog box to allow to choice a new location
     * @return f
     */
    private IFileWriter getFileSaver(boolean isAskedForNewLocation)
    {
        try
        {
            if (isAskedForNewLocation)
            {
                ExtensionFilter extensionFilter = this.fileNamingService.getExtensionFilter(this.graph);
                String hint = this.hasTemporaryFilename ? this.currentFilename : null;
                return this.fileChooserService.chooseAndGetFileWriter(hint, extensionFilter);
            }
            return this.fileChooserService.getFileWriter(this);
        }
        catch (IOException e1)
        {
            String message = MessageFormat.format(fileExportErrorMessage, e1.getMessage());
            JOptionPane.showMessageDialog(null, message, fileExportError, JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    @Override
    public void addListener(IGraphFileListener listener)
    {
        synchronized (listeners)
        {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(IGraphFileListener listener)
    {
        synchronized (listeners)
        {
            listeners.remove(listener);
        }
    }

    /**
     * Sends an event to listeners each time the graph is modified
     */
    private void fireGraphModified()
    {
        synchronized (listeners)
        {
            for (IGraphFileListener listener : listeners)
            {
                listener.onFileModified();
            }
        }
    }

    /**
     * Sends an event to listeners when the graph has been saved
     */
    private void fireGraphSaved()
    {
        synchronized (listeners)
        {
            for (IGraphFileListener listener : listeners)
            {
                listener.onFileSaved();
            }
        }
    }

    @Override
    public void exportToClipboard()
    {
        FileExportService.exportToclipBoard(this.graph);
        JOptionPane optionPane = new JOptionPane();
        optionPane.setIcon(this.clipBoardDialogIcon);
        optionPane.setMessage(this.clipBoardDialogMessage);
        optionPane.setName(this.clipBoardDialogTitle);
        this.dialogFactory.showDialog(optionPane, this.clipBoardDialogTitle, true);
    }

    @Override
    public void exportImage(OutputStream out, String format)
    {
        if (!ImageIO.getImageWritersByFormatName(format).hasNext())
        {
            MessageFormat formatter = new MessageFormat(this.exportImageErrorMessage);
            String message = formatter.format(new Object[]
                    {
                            format
                    });
            JOptionPane optionPane = new JOptionPane();
            optionPane.setMessage(message);
            this.dialogFactory.showDialog(optionPane, this.exportImageDialogTitle, true);
            return;
        }
        try
        {

            try
            {
                ImageIO.write(FileExportService.getImage(this.graph), format, out);
            }
            finally
            {
                out.close();
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void exportToPdf(OutputStream out)
    {
        FileExportService.exportToPdf(graph, out);
    }

    @Override
    public void exportToPrinter()
    {
        PrintEngine engine = new PrintEngine(this.graph);
        engine.start();
    }

    private IGraph graph;

    /**
     * Needed to identify the physical file used to save the graph
     */
    private String currentFilename;

    /**
     * Needed to identify the physical file used to save the graph
     */
    private String currentDirectory;

    private boolean isSaveRequired = false;

    /** True when the filename was auto-generated and the user has not yet chosen a real name. */
    private boolean hasTemporaryFilename = false;

    @ResourceBundleBean(key = "dialog.export_to_clipboard.icon")
    private ImageIcon clipBoardDialogIcon;

    @ResourceBundleBean(key = "dialog.export_to_clipboard.title")
    private String clipBoardDialogTitle;

    @ResourceBundleBean(key = "dialog.export_to_clipboard.ok")
    private String clipBoardDialogMessage;

    @ResourceBundleBean(key = "dialog.error.unsupported_image")
    private String exportImageErrorMessage;

    @ResourceBundleBean(key = "dialog.error.title")
    private String exportImageDialogTitle;

    @InjectedBean
    private IFileChooserService fileChooserService;

    @InjectedBean
    private FileNamingService fileNamingService;

    @InjectedBean
    private IFilePersistenceService filePersistenceService;

    @ResourceBundleBean(key = "file.export.error.message")
    private String fileExportErrorMessage;

    @ResourceBundleBean(key = "file.export.error")
    private String fileExportError;

    @InjectedBean
    private DialogFactory dialogFactory;

    @InjectedBean
    private UserPreferencesService userPreferencesService;

    private List<IGraphFileListener> listeners = new ArrayList<IGraphFileListener>();

}
