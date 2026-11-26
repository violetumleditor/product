package com.horstmann.violet.application.cheerpj;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import com.horstmann.violet.application.menu.FileMenu;
import com.horstmann.violet.application.menu.MenuFactory;
import com.horstmann.violet.framework.dialog.DialogFactory;
import com.horstmann.violet.framework.file.GraphFile;
import com.horstmann.violet.framework.file.IFile;
import com.horstmann.violet.framework.file.IGraphFile;
import com.horstmann.violet.framework.file.chooser.IFileChooserService;
import com.horstmann.violet.framework.file.naming.ExtensionFilter;
import com.horstmann.violet.framework.file.naming.FileNamingService;
import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.IFileReader;
import com.horstmann.violet.framework.file.persistence.IFileWriter;
import com.horstmann.violet.framework.file.persistence.XHTMLPersistenceService;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.InjectedBean;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.userpreferences.UserPreferencesService;
import com.horstmann.violet.workspace.IWorkspace;
import com.horstmann.violet.workspace.Workspace;

@ResourceBundleBean(resourceReference = MenuFactory.class)
public class CheerpJFileMenuCustomizerCommand {

	private static final String FILES_DIRECTORY = "/files";

    private FileMenu fileMenu;

    @ResourceBundleBean(key = "file.import")
    private JMenu fileImportMenu;

    @ResourceBundleBean(key = "file.import_from_device")
    private JMenu fileImportFromDeviceMenu;

    @ResourceBundleBean(key = "file.import_diagram")
    private JMenuItem fileImportFromDiagramItem;

    @ResourceBundleBean(key = "file.export_to_device")
    private JMenu fileExportToDeviceMenu;

    @ResourceBundleBean(key = "file.export_to_diagram")
    private JMenuItem fileExportToDiagramItem;

    @ResourceBundleBean(key = "dialog.open_file_failed.text")
    private String dialogOpenFileErrorMessage;

    @ResourceBundleBean(key = "dialog.open_from_storage.title")
    private String dialogOpenFromStorageTitle;

    @ResourceBundleBean(key = "dialog.open_from_storage.empty")
    private String dialogOpenFromStorageEmptyMessage;

    @ResourceBundleBean(key = "file.manage_files")
    private JMenuItem fileManageFilesItem;

    @InjectedBean
    private FileNamingService fileNamingService;

    @InjectedBean
    private IFilePersistenceService filePersistenceService;

    @InjectedBean
    private DialogFactory dialogFactory;

    @InjectedBean
    private IFileChooserService fileChooserService;

    private XHTMLPersistenceService downloadPersistenceService;

    @InjectedBean
    private UserPreferencesService userPreferencesService;

    public CheerpJFileMenuCustomizerCommand(FileMenu fileMenu) {
        ResourceBundleInjector.getInjector().inject(this);
        BeanInjector.getInjector().inject(this);
        this.fileMenu = fileMenu;
    }

	public FileMenu execute() {
        // Change print action to use CheerpJ printing
        for (ActionListener listener : this.fileMenu.getFilePrintItem().getActionListeners()) {
            this.fileMenu.getFilePrintItem().removeActionListener(listener);
        }
        this.fileMenu.getFilePrintItem().addActionListener(e -> runPrintWorkspace());

        // Override the "Open" action to pick a diagram already stored in the browser datastore (/files).
        // Files from the device are brought in through "Import from > This device".
        for (ActionListener listener : this.fileMenu.getFileOpenItem().getActionListeners()) {
            this.fileMenu.getFileOpenItem().removeActionListener(listener);
        }
        this.fileMenu.getFileOpenItem().addActionListener(e -> runOpenFromBrowserStorage());

        // Browsers keep Ctrl+W to close the tab and never give it to the page: don't display it
        this.fileMenu.getFileCloseItem().setAccelerator(null);

        // Hide the "Exit" menu item
        this.fileMenu.getFileExitItem().setVisible(false);

        // "Import from > This device > Diagram file"
        this.fileImportFromDeviceMenu.add(this.fileImportFromDiagramItem);
        this.fileImportFromDiagramItem.addActionListener(e -> runImportFromLocalDrive());
        this.fileImportMenu.add(this.fileImportFromDeviceMenu);
        this.fileMenu.add(this.fileImportMenu, 1);

        // "Manage files", right after "Recent files": lists the diagrams of the browser storage to delete them, or to export
        // or import them as an archive
        int recentMenuIndex = this.fileMenu.getPopupMenu().getComponentIndex(this.fileMenu.getFileRecentMenu());
        this.fileMenu.add(this.fileManageFilesItem, recentMenuIndex + 1);
        this.fileManageFilesItem.addActionListener(e -> runManageFiles());

        // "Export to > This device > Diagram file / Image file / PDF file": every export downloaded to the device
        // (the image and PDF items move there, adding them elsewhere takes them out of "Export to"), the clipboard staying beside
        this.fileExportToDeviceMenu.add(this.fileExportToDiagramItem);
        this.fileExportToDiagramItem.addActionListener(e -> runExportToDiagram());
        this.fileExportToDeviceMenu.add(this.fileMenu.getFileExportToImageItem());
        this.fileExportToDeviceMenu.add(this.fileMenu.getFileExportToPdfItem());
        this.fileMenu.getFileExportMenu().add(this.fileExportToDeviceMenu, 0);
		
		return this.fileMenu;
    }

	private boolean runOpenFromBrowserStorage() {
		List<String> filenames = listStoredDiagramFilenames();
		if (filenames.isEmpty()) {
			dialogFactory.showInformationDialog(dialogOpenFromStorageEmptyMessage);
			return false;
		}
		String filename = chooseStoredDiagramFilename(filenames);
		if (filename == null) {
			// Action cancelled by user
			return false;
		}
		return openStoredDiagram(filename);
	}

	/**
	 * Shows the diagram of the browser storage in its workspace, opening it if needed
	 *
	 * @return true if it is shown
	 */
	private boolean openStoredDiagram(String filename) {
		try {
			IFileReader fileReader = new CheerpJFileReader(FILES_DIRECTORY, filename);
			IWorkspace alreadyOpened = findOpenedWorkspace(fileReader.getFileDefinition());
			if (alreadyOpened != null) {
				this.fileMenu.getMainFrame().setActiveWorkspace(alreadyOpened);
				return true;
			}
			GraphFile graphFile = new GraphFile(fileReader.getFileDefinition());
			IWorkspace workspace = new Workspace(graphFile);
			this.fileMenu.getMainFrame().addWorkspace(workspace);
			this.userPreferencesService.addOpenedFile(graphFile);
			this.userPreferencesService.addRecentFile(graphFile);
			return true;
		} catch (Exception e) {
			dialogFactory.showErrorDialog(dialogOpenFileErrorMessage + " : " + e.getMessage());
			return false;
		}
	}

	/**
	 * "Manage files": lists the diagrams of the browser storage to delete them, export or import them as an archive
	 */
	private void runManageFiles() {
		new StoredFilesManager(this.fileMenu.getMainFrame(), this::openStoredDiagram, this.fileMenu::closeWorkspace).show();
	}

	/**
	 * @return names of the diagram files kept in the browser datastore, most recently modified first
	 */
	private List<String> listStoredDiagramFilenames() {
		List<String> result = new ArrayList<String>();
		File[] files = new File(FILES_DIRECTORY).listFiles(File::isFile);
		if (files == null) {
			return result;
		}
		Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
		for (File aFile : files) {
			if (isDiagramFilename(aFile.getName())) {
				result.add(aFile.getName());
			}
		}
		return result;
	}

	/**
	 * @return true if the name ends with the extension of a diagram type
	 */
	private boolean isDiagramFilename(String filename) {
		for (ExtensionFilter aFilter : fileNamingService.getFileFilters()) {
			String extension = aFilter.getExtension();
			if (extension != null && filename.toLowerCase().endsWith(extension.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Shows a modal list of stored diagrams (double-click or OK to open). Deleting them is done by "Manage files".
	 *
	 * @return the selected filename, or null if cancelled
	 */
	private String chooseStoredDiagramFilename(List<String> filenames) {
		JList<String> list = new JList<String>(filenames.toArray(new String[0]));
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setSelectedIndex(0);
		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setPreferredSize(new Dimension(400, 250));

		JOptionPane optionPane = new JOptionPane(scrollPane, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && list.getSelectedValue() != null) {
					optionPane.setValue(JOptionPane.OK_OPTION);
				}
			}
		});
		dialogFactory.showDialog(optionPane, dialogOpenFromStorageTitle, true);

		Object value = optionPane.getValue();
		if (!(value instanceof Integer) || ((Integer) value).intValue() != JOptionPane.OK_OPTION) {
			return null;
		}
		return list.getSelectedValue();
	}

	private IWorkspace findOpenedWorkspace(IFile file) {
		for (IWorkspace aWorkspace : this.fileMenu.getMainFrame().getWorkspaceList()) {
			IGraphFile graphFile = aWorkspace.getGraphFile();
			if (file.getFilename().equals(graphFile.getFilename()) && file.getDirectory().equals(graphFile.getDirectory())) {
				return aWorkspace;
			}
		}
		return null;
	}

	private boolean runImportFromLocalDrive() {
		try {
			// A pending auto-save of an opened diagram would write it over the imported file of the same name
			saveOpenedWorkspaces();
			ExtensionFilter[] filters = fileNamingService.getFileFilters();
			IFileReader fileReader = fileChooserService.chooseAndGetFileReader(filters);
			if (fileReader == null) {
				// The user refused to replace the stored diagram of the same name
				return false;
			}
			IWorkspace replaced = findOpenedWorkspace(fileReader.getFileDefinition());
			if (replaced != null) {
				this.fileMenu.getMainFrame().removeWorkspace(replaced);
			}
			GraphFile graphFile = new GraphFile(fileReader.getFileDefinition());
			IWorkspace workspace = new Workspace(graphFile);
			this.fileMenu.getMainFrame().addWorkspace(workspace);
			this.userPreferencesService.addOpenedFile(graphFile);
			this.userPreferencesService.addRecentFile(graphFile);
			return true;
		} catch (Exception e) {
			dialogFactory.showErrorDialog(dialogOpenFileErrorMessage + " : " + e.getMessage());
			return false;
		}
	}

	private void saveOpenedWorkspaces() {
		for (IWorkspace aWorkspace : this.fileMenu.getMainFrame().getWorkspaceList()) {
			IGraphFile graphFile = aWorkspace.getGraphFile();
			if (graphFile != null && graphFile.isSaveRequired()) {
				graphFile.save(false);
			}
		}
	}

	private boolean runExportToDiagram() {
		IWorkspace workspace = this.fileMenu.getMainFrame().getActiveWorkspace();
		if (workspace == null) {
			return false;
		}
		try {
			IGraphFile graphFile = workspace.getGraphFile();
			if (graphFile == null) {
				return false;
			}
            this.fileMenu.getFileSaveItem().doClick();
			if (graphFile.isSaveRequired()) {
				return false;
			}
			IFileWriter writer = new CheerpJDownloadFileWriter(graphFile.getFilename());
			OutputStream out = writer.getOutputStream();
			getDownloadPersistenceService().write(workspace.getGraphFile().getGraph(), out);
			out.close();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * @return the writer of the downloaded files, with the light and dark images of the diagram, which the files kept in
	 * the browser storage leave out
	 */
	private XHTMLPersistenceService getDownloadPersistenceService() {
		if (this.downloadPersistenceService == null) {
			this.downloadPersistenceService = new XHTMLPersistenceService(true);
			BeanInjector.getInjector().inject(this.downloadPersistenceService);
		}
		return this.downloadPersistenceService;
	}

	private boolean runPrintWorkspace() {
		IWorkspace workspace = this.fileMenu.getMainFrame().getActiveWorkspace();
		if (workspace == null) {
			return false;
		}
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			workspace.getGraphFile().exportToPdf(out);
			out.close();
			CheerpJInterfaceService.openPdfPrintTab(out.toByteArray());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

}
