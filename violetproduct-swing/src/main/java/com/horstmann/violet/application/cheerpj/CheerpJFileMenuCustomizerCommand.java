package com.horstmann.violet.application.cheerpj;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

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

    @ResourceBundleBean(key = "file.import_archive")
    private JMenuItem fileImportFromArchiveItem;

    @ResourceBundleBean(key = "file.export_to_device")
    private JMenu fileExportToDeviceMenu;

    @ResourceBundleBean(key = "file.export_to_diagram")
    private JMenuItem fileExportToDiagramItem;

    @ResourceBundleBean(key = "file.export_to_archive")
    private JMenuItem fileExportToArchiveItem;

    @ResourceBundleBean(key = "dialog.import_archive.title")
    private String dialogImportArchiveTitle;

    @ResourceBundleBean(key = "dialog.import_archive.empty")
    private String dialogImportArchiveEmptyMessage;

    @ResourceBundleBean(key = "dialog.import_archive.done")
    private String dialogImportArchiveDoneMessage;

    @ResourceBundleBean(key = "dialog.import_conflict.title")
    private String dialogImportConflictTitle;

    @ResourceBundleBean(key = "dialog.import_conflict.message")
    private String dialogImportConflictMessage;

    @ResourceBundleBean(key = "dialog.import_conflict.for_all")
    private String dialogImportConflictForAllText;

    @ResourceBundleBean(key = "dialog.import_conflict.replace")
    private String dialogImportConflictReplaceText;

    @ResourceBundleBean(key = "dialog.import_conflict.skip")
    private String dialogImportConflictSkipText;

    @ResourceBundleBean(key = "dialog.import_conflict.cancel")
    private String dialogImportConflictCancelText;

    @ResourceBundleBean(key = "dialog.export_archive.failed")
    private String dialogExportArchiveFailedMessage;

    @ResourceBundleBean(key = "dialog.open_file_failed.text")
    private String dialogOpenFileErrorMessage;

    @ResourceBundleBean(key = "dialog.open_from_storage.title")
    private String dialogOpenFromStorageTitle;

    @ResourceBundleBean(key = "dialog.open_from_storage.empty")
    private String dialogOpenFromStorageEmptyMessage;

    @ResourceBundleBean(key = "dialog.delete_from_storage.title")
    private String dialogDeleteFromStorageTitle;

    @ResourceBundleBean(key = "dialog.delete_from_storage.tooltip")
    private String dialogDeleteFromStorageTooltip;

    @ResourceBundleBean(key = "dialog.delete_from_storage.confirm")
    private String dialogDeleteFromStorageConfirmMessage;

    @ResourceBundleBean(key = "dialog.delete_from_storage.opened")
    private String dialogDeleteFromStorageOpenedMessage;

    @ResourceBundleBean(key = "dialog.delete_from_storage.failed")
    private String dialogDeleteFromStorageFailedMessage;

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

        // "Import from > This device > Diagram file / Archive file"
        this.fileImportFromDeviceMenu.add(this.fileImportFromDiagramItem);
        this.fileImportFromDiagramItem.addActionListener(e -> runImportFromLocalDrive());
        this.fileImportFromDeviceMenu.add(this.fileImportFromArchiveItem);
        this.fileImportFromArchiveItem.addActionListener(e -> runImportFromArchive());
        this.fileImportMenu.add(this.fileImportFromDeviceMenu);
        this.fileMenu.add(this.fileImportMenu, 1);

        // "Export to > This device > Diagram file / Image file / PDF file / Archive file": every export downloaded to the device
        // (the image and PDF items move there, adding them elsewhere takes them out of "Export to"), the clipboard staying beside
        this.fileExportToDeviceMenu.add(this.fileExportToDiagramItem);
        this.fileExportToDiagramItem.addActionListener(e -> runExportToDiagram());
        this.fileExportToDeviceMenu.add(this.fileMenu.getFileExportToImageItem());
        this.fileExportToDeviceMenu.add(this.fileMenu.getFileExportToPdfItem());
        this.fileExportToDeviceMenu.add(this.fileExportToArchiveItem);
        this.fileExportToArchiveItem.addActionListener(e -> runExportToArchive());
        this.fileMenu.getFileExportMenu().add(this.fileExportToDeviceMenu, 0);
		
		return this.fileMenu;
    }

	private boolean runOpenFromBrowserStorage() {
		try {
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
	 * Shows a modal list of stored diagrams (double-click or OK to open). Each line ends with a
	 * trash icon to delete the file from the browser datastore (the Delete key does the same on the
	 * selected line).
	 *
	 * @return the selected filename, or null if cancelled
	 */
	private String chooseStoredDiagramFilename(List<String> filenames) {
		DefaultListModel<String> model = new DefaultListModel<String>();
		filenames.forEach(model::addElement);
		JList<String> list = new JList<String>(model) {
			@Override
			public String getToolTipText(MouseEvent e) {
				return trashIconIndexAt(this, e.getPoint()) >= 0 ? dialogDeleteFromStorageTooltip : null;
			}
		};
		list.setToolTipText(""); // registers the list with the tooltip manager
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setSelectedIndex(0);
		list.setCellRenderer(new StoredDiagramCellRenderer());
		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setPreferredSize(new Dimension(400, 250));

		JOptionPane optionPane = new JOptionPane(scrollPane, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		Runnable deleteSelected = () -> {
			int index = list.getSelectedIndex();
			if (index >= 0 && deleteStoredDiagram(model.get(index))) {
				model.remove(index);
				if (model.isEmpty()) {
					optionPane.setValue(JOptionPane.CANCEL_OPTION);
					return;
				}
				list.setSelectedIndex(Math.min(index, model.size() - 1));
			}
		};
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int trashIndex = trashIconIndexAt(list, e.getPoint());
				if (trashIndex >= 0) {
					if (e.getClickCount() == 1) {
						list.setSelectedIndex(trashIndex);
						deleteSelected.run();
					}
					return;
				}
				if (e.getClickCount() == 2 && list.getSelectedValue() != null) {
					optionPane.setValue(JOptionPane.OK_OPTION);
				}
			}
		});
		list.addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				boolean onTrash = trashIconIndexAt(list, e.getPoint()) >= 0;
				list.setCursor(Cursor.getPredefinedCursor(onTrash ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
			}
		});
		list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteStoredDiagram");
		list.getActionMap().put("deleteStoredDiagram", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				deleteSelected.run();
			}
		});
		dialogFactory.showDialog(optionPane, dialogOpenFromStorageTitle, true);

		Object value = optionPane.getValue();
		if (!(value instanceof Integer) || ((Integer) value).intValue() != JOptionPane.OK_OPTION) {
			return null;
		}
		return list.getSelectedValue();
	}

	/**
	 * Asks for confirmation, then deletes a diagram from the browser datastore and forgets it in user preferences.
	 * A diagram opened in a workspace is not deleted: auto-save would write it back.
	 *
	 * @return true if the file has been deleted
	 */
	private boolean deleteStoredDiagram(String filename) {
		IFile file = new CheerpJFileReader(FILES_DIRECTORY, filename).getFileDefinition();
		if (findOpenedWorkspace(file) != null) {
			dialogFactory.showWarningDialog(MessageFormat.format(dialogDeleteFromStorageOpenedMessage, filename));
			return false;
		}
		JOptionPane optionPane = new JOptionPane(MessageFormat.format(dialogDeleteFromStorageConfirmMessage, filename),
				JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION);
		dialogFactory.showDialog(optionPane, dialogDeleteFromStorageTitle, true);
		Object value = optionPane.getValue();
		if (!(value instanceof Integer) || ((Integer) value).intValue() != JOptionPane.YES_OPTION) {
			return false;
		}
		this.fileChooserService.getFileDeleter(file).delete();
		if (new File(FILES_DIRECTORY, filename).exists()) {
			dialogFactory.showErrorDialog(MessageFormat.format(dialogDeleteFromStorageFailedMessage, filename));
			return false;
		}
		this.userPreferencesService.removeRecentFile(file);
		this.userPreferencesService.removeOpenedFile(file);
		return true;
	}

	/**
	 * @return the index of the list line whose trash icon is under the given point, or -1
	 */
	private static int trashIconIndexAt(JList<?> list, Point point) {
		int index = list.locationToIndex(point);
		if (index < 0) {
			return -1;
		}
		Rectangle bounds = list.getCellBounds(index, index);
		if (bounds == null || !bounds.contains(point)) {
			return -1;
		}
		return point.x >= bounds.x + bounds.width - TRASH_ZONE_WIDTH ? index : -1;
	}

	private static final int TRASH_ZONE_WIDTH = 28;

	/**
	 * Renders a stored diagram line: filename on the left, trash icon on the right
	 */
	private static class StoredDiagramCellRenderer extends JPanel implements ListCellRenderer<String> {

		private final DefaultListCellRenderer label = new DefaultListCellRenderer();
		private final JLabel trash = new JLabel(new TrashIcon());

		StoredDiagramCellRenderer() {
			super(new BorderLayout());
			trash.setHorizontalAlignment(SwingConstants.CENTER);
			trash.setPreferredSize(new Dimension(TRASH_ZONE_WIDTH, 0));
			add(label, BorderLayout.CENTER);
			add(trash, BorderLayout.EAST);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
				boolean isSelected, boolean cellHasFocus) {
			label.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			setBackground(label.getBackground());
			trash.setForeground(label.getForeground());
			return this;
		}
	}

	/**
	 * 16x16 trash can drawn with the component foreground color, so it follows the theme and the selection
	 */
	private static class TrashIcon implements Icon {

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(c.getForeground());
			g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g2.translate(x, y);
			g2.drawLine(2, 4, 14, 4);                  // lid
			g2.drawRect(6, 2, 4, 2);                   // handle
			Path2D can = new Path2D.Float();
			can.moveTo(3.5f, 4.5f);
			can.lineTo(4.5f, 14.5f);
			can.lineTo(11.5f, 14.5f);
			can.lineTo(12.5f, 4.5f);
			g2.draw(can);
			g2.drawLine(6, 7, 6, 12);                  // ribs
			g2.drawLine(8, 7, 8, 12);
			g2.drawLine(10, 7, 10, 12);
			g2.dispose();
		}

		@Override
		public int getIconWidth() {
			return 16;
		}

		@Override
		public int getIconHeight() {
			return 16;
		}
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

	/**
	 * Downloads one zip archive holding every diagram kept in the browser storage, as they are stored
	 */
	private boolean runExportToArchive() {
		try {
			saveOpenedWorkspaces();
			List<String> filenames = listStoredDiagramFilenames();
			if (filenames.isEmpty()) {
				dialogFactory.showInformationDialog(dialogOpenFromStorageEmptyMessage);
				return false;
			}
			List<File> files = new ArrayList<File>();
			for (String aFilename : filenames) {
				files.add(new File(FILES_DIRECTORY, aFilename));
			}
			String archiveFilename = "violet-diagrams-" + LocalDate.now() + DiagramArchive.EXTENSION;
			IFileWriter writer = new CheerpJDownloadFileWriter(archiveFilename);
			getDiagramArchive().write(files, writer.getOutputStream());
			return true;
		} catch (Exception e) {
			dialogFactory.showErrorDialog(dialogExportArchiveFailedMessage + " : " + e.getMessage());
			return false;
		}
	}

	/**
	 * Copies the diagrams of a zip archive into the browser storage, asking what to do with each diagram of the same name
	 * already there, as the file explorer of Windows does. Nothing is written if the user cancels. The diagrams opened in a
	 * workspace and replaced are opened again.
	 */
	private boolean runImportFromArchive() {
		try {
			// A pending auto-save of an opened diagram would write it over the imported file of the same name
			saveOpenedWorkspaces();
			ExtensionFilter archiveFilter = new ExtensionFilter(this.fileImportFromArchiveItem.getText(), DiagramArchive.EXTENSION);
			IFileReader archiveReader = fileChooserService.chooseAndGetFileReader(archiveFilter);
			if (archiveReader == null) {
				// Action cancelled by user
				return false;
			}
			List<DiagramArchive.Entry> entries;
			try (InputStream in = archiveReader.getInputStream()) {
				entries = getDiagramArchive().read(in, this::isDiagramFilename);
			}
			if (entries.isEmpty()) {
				dialogFactory.showInformationDialog(dialogImportArchiveEmptyMessage);
				return false;
			}
			List<DiagramArchive.Entry> entriesToWrite = chooseEntriesToWrite(entries);
			if (entriesToWrite == null) {
				return false;
			}
			for (DiagramArchive.Entry anEntry : entriesToWrite) {
				IFileWriter writer = new CheerpJStorageFileWriter(anEntry.filename());
				try (OutputStream out = writer.getOutputStream()) {
					out.write(anEntry.content());
				}
			}
			reopenWorkspacesOf(entriesToWrite);
			String message = MessageFormat.format(dialogImportArchiveDoneMessage, entriesToWrite.size(),
					entries.size() - entriesToWrite.size());
			JOptionPane optionPane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE);
			dialogFactory.showDialog(optionPane, dialogImportArchiveTitle, true);
			return true;
		} catch (Exception e) {
			dialogFactory.showErrorDialog(dialogOpenFileErrorMessage + " : " + e.getMessage());
			return false;
		}
	}

	/**
	 * Asks, for each diagram already in the browser storage, whether to replace it or to skip it; "Do this for the next
	 * conflicts" applies the answer to the following ones.
	 *
	 * @return the entries to write, or null if the user cancelled the import
	 */
	private List<DiagramArchive.Entry> chooseEntriesToWrite(List<DiagramArchive.Entry> entries) {
		int conflictCount = 0;
		for (DiagramArchive.Entry anEntry : entries) {
			if (isStored(anEntry.filename())) {
				conflictCount++;
			}
		}
		List<DiagramArchive.Entry> result = new ArrayList<DiagramArchive.Entry>();
		Boolean replaceForAll = null;
		for (DiagramArchive.Entry anEntry : entries) {
			if (!isStored(anEntry.filename())) {
				result.add(anEntry);
				continue;
			}
			conflictCount--;
			boolean replace;
			if (replaceForAll != null) {
				replace = replaceForAll.booleanValue();
			} else {
				ConflictAnswer answer = askConflictAnswer(anEntry.filename(), conflictCount);
				if (answer == null) {
					return null;
				}
				replace = answer.replace();
				if (answer.forAll()) {
					replaceForAll = Boolean.valueOf(replace);
				}
			}
			if (replace) {
				result.add(anEntry);
			}
		}
		return result;
	}

	private boolean isStored(String filename) {
		return new File(FILES_DIRECTORY, filename).exists();
	}

	/**
	 * What the user chose for a diagram of the same name: replace it or skip the imported one, for this conflict only or
	 * for the following ones too
	 */
	private record ConflictAnswer(boolean replace, boolean forAll) {
	}

	/**
	 * @param nextConflictCount the conflicts left after this one: the check box applying the answer to them is shown only
	 * if there are some
	 * @return the answer, or null if the user cancelled the import
	 */
	private ConflictAnswer askConflictAnswer(String filename, int nextConflictCount) {
		List<Object> message = new ArrayList<Object>();
		message.add(MessageFormat.format(dialogImportConflictMessage, filename));
		JCheckBox forAllCheckBox = new JCheckBox(MessageFormat.format(dialogImportConflictForAllText, nextConflictCount));
		if (nextConflictCount > 0) {
			message.add(forAllCheckBox);
		}
		Object[] options = { dialogImportConflictReplaceText, dialogImportConflictSkipText, dialogImportConflictCancelText };
		JOptionPane optionPane = new JOptionPane(message.toArray(), JOptionPane.WARNING_MESSAGE, JOptionPane.DEFAULT_OPTION,
				null, options, dialogImportConflictSkipText);
		dialogFactory.showDialog(optionPane, dialogImportConflictTitle, true);
		Object value = optionPane.getValue();
		if (dialogImportConflictReplaceText.equals(value)) {
			return new ConflictAnswer(true, forAllCheckBox.isSelected());
		}
		if (dialogImportConflictSkipText.equals(value)) {
			return new ConflictAnswer(false, forAllCheckBox.isSelected());
		}
		return null;
	}

	/**
	 * Opens again the workspaces showing a replaced diagram, so that they show the imported one; the active workspace
	 * stays the active one
	 */
	private void reopenWorkspacesOf(List<DiagramArchive.Entry> writtenEntries) throws IOException {
		IWorkspace active = this.fileMenu.getMainFrame().getActiveWorkspace();
		for (DiagramArchive.Entry anEntry : writtenEntries) {
			IFile file = new CheerpJFileReader(FILES_DIRECTORY, anEntry.filename()).getFileDefinition();
			IWorkspace replaced = findOpenedWorkspace(file);
			if (replaced == null) {
				continue;
			}
			this.fileMenu.getMainFrame().removeWorkspace(replaced);
			IWorkspace workspace = new Workspace(new GraphFile(file));
			this.fileMenu.getMainFrame().addWorkspace(workspace);
			if (replaced == active) {
				active = workspace;
			}
		}
		if (active != null) {
			this.fileMenu.getMainFrame().setActiveWorkspace(active);
		}
	}

	private DiagramArchive getDiagramArchive() {
		if (this.diagramArchive == null) {
			this.diagramArchive = new DiagramArchive();
		}
		return this.diagramArchive;
	}

	private DiagramArchive diagramArchive;

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
