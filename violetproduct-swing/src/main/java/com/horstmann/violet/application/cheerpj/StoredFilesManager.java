package com.horstmann.violet.application.cheerpj;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import com.formdev.flatlaf.FlatLaf;
import com.horstmann.violet.application.gui.MainFrame;
import com.horstmann.violet.application.menu.MenuFactory;
import com.horstmann.violet.framework.dialog.DialogFactory;
import com.horstmann.violet.framework.file.GraphFile;
import com.horstmann.violet.framework.file.IFile;
import com.horstmann.violet.framework.file.IGraphFile;
import com.horstmann.violet.framework.file.chooser.IFileChooserService;
import com.horstmann.violet.framework.file.naming.ExtensionFilter;
import com.horstmann.violet.framework.file.persistence.IFileReader;
import com.horstmann.violet.framework.file.persistence.IFileWriter;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.InjectedBean;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.plugin.IDiagramPlugin;
import com.horstmann.violet.framework.plugin.PluginRegistry;
import com.horstmann.violet.framework.userpreferences.UserPreferencesService;
import com.horstmann.violet.workspace.IWorkspace;
import com.horstmann.violet.workspace.Workspace;

/**
 * "File > Manage files" of the browser version: lists every diagram kept in the browser storage (name, whether it is
 * opened, type, date, size), deletes the checked ones, and exports them (or all of them) to a zip archive or imports the
 * diagrams of one. Every deletion is confirmed, as it can't be undone. A diagram opened in a workspace is shown behind the
 * dialog, then closed before being deleted, so that its auto-save can't write it back. A double click on a line opens its
 * diagram. Under {@link #NARROW_WIDTH}, the type and size columns are left out.
 */
@ResourceBundleBean(resourceReference = MenuFactory.class)
class StoredFilesManager {

	private static final String FILES_DIRECTORY = "/files";

	private static final String FILE_SUFFIX = ".violet.html";

	private static final int CHECK_COLUMN = 0;
	private static final int NAME_COLUMN = 1;
	private static final int STATUS_COLUMN = 2;
	private static final int TYPE_COLUMN = 3;
	private static final int MODIFIED_COLUMN = 4;
	private static final int SIZE_COLUMN = 5;

	/** Width of the table under which only the check box, name, status and date are shown (phones) */
	private static final int NARROW_WIDTH = 520;

	/** Lines of a confirmation listing several diagrams, the next ones being summed up by "…" */
	private static final int MAX_LISTED_FILES = 10;

	/** "Open" badge: soft green, light and dark themes */
	private static final Color BADGE_BACKGROUND = new Color(0xD8F3DF);
	private static final Color BADGE_FOREGROUND = new Color(0x1B6B34);
	private static final Color DARK_BADGE_BACKGROUND = new Color(0x1F4A2C);
	private static final Color DARK_BADGE_FOREGROUND = new Color(0xA9E5B9);

	/**
	 * A diagram file of the browser storage
	 */
	private record StoredFile(String filename, String type, long lastModified, long length) {
	}

	/**
	 * What the user chose for a diagram of the same name: replace it or skip the imported one, for this conflict only or
	 * for the following ones too
	 */
	private record ConflictAnswer(boolean replace, boolean forAll) {
	}

	private final MainFrame mainFrame;

	private final Consumer<String> opener;

	/** Name of the diagram to open once the dialog is closed (double click on a line), or null */
	private String filenameToOpen;

	/**
	 * @param mainFrame where the diagrams are opened
	 * @param opener opens a diagram of the browser storage, given its file name
	 */
	StoredFilesManager(MainFrame mainFrame, Consumer<String> opener) {
		ResourceBundleInjector.getInjector().inject(this);
		BeanInjector.getInjector().inject(this);
		this.mainFrame = mainFrame;
		this.opener = opener;
	}

	/**
	 * Shows the dialog until the user closes it, then opens the diagram double clicked, if any
	 */
	void show() {
		refresh();
		if (!getStoredFiles().isEmpty()) {
			getTable().getSelectionModel().setSelectionInterval(0, 0);
		}
		dialogFactory.showDialog(getOptionPane(), dialogTitle, true);
		if (this.filenameToOpen != null) {
			this.opener.accept(this.filenameToOpen);
		}
	}

	private JOptionPane getOptionPane() {
		if (this.optionPane == null) {
			JPanel content = new JPanel(new BorderLayout(0, 8));
			content.setOpaque(false);
			JPanel top = new JPanel();
			top.setOpaque(false);
			top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
			JLabel header = new JLabel(dialogHeader);
			header.setAlignmentX(Component.LEFT_ALIGNMENT);
			top.add(header);
			JPanel archiveButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			archiveButtons.setOpaque(false);
			archiveButtons.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
			archiveButtons.setAlignmentX(Component.LEFT_ALIGNMENT);
			archiveButtons.add(getImportButton());
			archiveButtons.add(Box.createHorizontalStrut(8));
			archiveButtons.add(getExportButton());
			top.add(archiveButtons);
			content.add(top, BorderLayout.NORTH);
			JScrollPane scrollPane = new JScrollPane(getTable());
			scrollPane.setPreferredSize(new Dimension(getTableWidth(), Math.max(140, Math.min(300, this.mainFrame.getHeight() - 300))));
			content.add(scrollPane, BorderLayout.CENTER);
			content.add(getSummaryLabel(), BorderLayout.SOUTH);
			Object[] options = { getSelectAllButton(), getDeleteButton(), getCloseButton() };
			this.optionPane = new JOptionPane(content, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, options,
					getCloseButton());
		}
		return this.optionPane;
	}

	private JOptionPane optionPane;

	/**
	 * @return the width of the table: up to 680 px, else what the window leaves around the dialog
	 */
	private int getTableWidth() {
		return Math.max(260, Math.min(680, this.mainFrame.getWidth() - 60));
	}

	private JTable getTable() {
		if (this.table == null) {
			this.table = new JTable(getTableModel());
			this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			this.table.setRowHeight(28);
			this.table.setShowGrid(false);
			this.table.setFillsViewportHeight(true);
			this.table.getTableHeader().setReorderingAllowed(false);
			TableColumnModel columns = this.table.getColumnModel();
			setColumnWidth(columns, CHECK_COLUMN, 32, true);
			setColumnWidth(columns, NAME_COLUMN, 220, false);
			setColumnWidth(columns, STATUS_COLUMN, 80, false);
			setColumnWidth(columns, TYPE_COLUMN, 140, false);
			setColumnWidth(columns, MODIFIED_COLUMN, 130, false);
			setColumnWidth(columns, SIZE_COLUMN, 75, false);
			columns.getColumn(STATUS_COLUMN).setCellRenderer(new StatusCellRenderer());
			DefaultTableCellRenderer sizeRenderer = new DefaultTableCellRenderer();
			sizeRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
			columns.getColumn(SIZE_COLUMN).setCellRenderer(sizeRenderer);
			if (getTableWidth() < NARROW_WIDTH) {
				// The name ends with the type ("order.class"), and the size matters less than the date
				columns.removeColumn(columns.getColumn(SIZE_COLUMN));
				columns.removeColumn(columns.getColumn(TYPE_COLUMN));
			}
			this.table.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					onTableClicked(e);
				}
			});
			this.table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
					"deleteStoredDiagrams");
			this.table.getActionMap().put("deleteStoredDiagrams", new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					deleteCheckedOrSelected();
				}
			});
		}
		return this.table;
	}

	private JTable table;

	private void setColumnWidth(TableColumnModel columns, int index, int width, boolean fixed) {
		columns.getColumn(index).setPreferredWidth(width);
		if (fixed) {
			columns.getColumn(index).setMinWidth(width);
			columns.getColumn(index).setMaxWidth(width);
		}
	}

	private void onTableClicked(MouseEvent e) {
		int row = getTable().rowAtPoint(e.getPoint());
		int column = getTable().convertColumnIndexToModel(getTable().columnAtPoint(e.getPoint()));
		if (row < 0 || column == CHECK_COLUMN || e.getClickCount() != 2) {
			return;
		}
		this.filenameToOpen = getStoredFiles().get(row).filename();
		getOptionPane().setValue(getCloseButton());
	}

	/**
	 * Delete key: deletes the checked diagrams, else the selected one
	 */
	private void deleteCheckedOrSelected() {
		List<StoredFile> files = getCheckedFiles();
		if (files.isEmpty()) {
			int row = getTable().getSelectedRow();
			if (row < 0) {
				return;
			}
			files = List.of(getStoredFiles().get(row));
		}
		delete(files);
	}

	private List<StoredFile> getCheckedFiles() {
		List<StoredFile> result = new ArrayList<StoredFile>();
		for (StoredFile aFile : getStoredFiles()) {
			if (getCheckedFilenames().contains(aFile.filename())) {
				result.add(aFile);
			}
		}
		return result;
	}

	/**
	 * Shows the opened diagrams among them behind the dialog, asks for confirmation, then closes them and deletes the files
	 */
	private void delete(List<StoredFile> files) {
		IWorkspace lastOpened = null;
		for (StoredFile aFile : files) {
			IWorkspace workspace = findOpenedWorkspace(aFile.filename());
			if (workspace != null) {
				lastOpened = workspace;
			}
		}
		if (lastOpened != null && lastOpened != this.mainFrame.getActiveWorkspace()) {
			this.mainFrame.setActiveWorkspace(lastOpened);
		}
		if (!confirmDeletion(files)) {
			return;
		}
		List<String> failed = new ArrayList<String>();
		for (StoredFile aFile : files) {
			if (!deleteStoredFile(aFile.filename())) {
				failed.add(getDisplayName(aFile.filename()));
			}
		}
		refresh();
		if (!failed.isEmpty()) {
			dialogFactory.showErrorDialog(MessageFormat.format(dialogDeleteFromStorageFailedMessage, String.join(", ", failed)));
		}
	}

	/**
	 * @return true if the user confirmed the deletion, which can't be undone
	 */
	private boolean confirmDeletion(List<StoredFile> files) {
		String message;
		String title;
		String confirmLabel;
		if (files.size() == 1) {
			String name = getDisplayName(files.get(0).filename());
			boolean isOpened = findOpenedWorkspace(files.get(0).filename()) != null;
			message = MessageFormat.format(isOpened ? dialogConfirmOpenedMessage : dialogConfirmMessage, name);
			title = dialogDeleteTitle;
			confirmLabel = isOpened ? dialogCloseAndDeleteText : dialogDeleteText;
		} else {
			List<String> lines = new ArrayList<String>();
			for (StoredFile aFile : files) {
				if (lines.size() == MAX_LISTED_FILES) {
					lines.add("   …");
					break;
				}
				String line = "   • " + getDisplayName(aFile.filename());
				if (findOpenedWorkspace(aFile.filename()) != null) {
					line += " " + dialogOpenedSuffix;
				}
				lines.add(line);
			}
			message = MessageFormat.format(dialogConfirmManyMessage, files.size(), String.join("\n", lines));
			title = dialogDeleteManyTitle;
			confirmLabel = MessageFormat.format(dialogDeleteManyText, files.size());
		}
		Object[] options = { confirmLabel, dialogCancelText };
		JOptionPane confirmPane = new JOptionPane(message, JOptionPane.WARNING_MESSAGE, JOptionPane.DEFAULT_OPTION, null,
				options, dialogCancelText);
		dialogFactory.showDialog(confirmPane, title, true);
		return confirmLabel.equals(confirmPane.getValue());
	}

	/**
	 * Closes the diagram if it is opened, without asking to save it, then deletes it from the browser storage and forgets it
	 * in user preferences
	 *
	 * @return true if the file is gone
	 */
	private boolean deleteStoredFile(String filename) {
		IFile file = new CheerpJFileReader(FILES_DIRECTORY, filename).getFileDefinition();
		IWorkspace workspace = findOpenedWorkspace(filename);
		if (workspace != null) {
			try {
				// Clears the unsaved state, else the pending auto-save would write the file back once deleted
				workspace.getGraphFile().save(false);
			} catch (RuntimeException e) {
				// The file is deleted anyway
			}
			this.mainFrame.removeWorkspace(workspace);
		}
		this.fileChooserService.getFileDeleter(file).delete();
		this.userPreferencesService.removeRecentFile(file);
		this.userPreferencesService.removeOpenedFile(file);
		return !isStored(filename);
	}

	/**
	 * Downloads one zip archive holding the checked diagrams, or all of them when none is checked, as they are stored
	 */
	private void exportArchive() {
		try {
			saveOpenedWorkspaces();
			List<StoredFile> storedFiles = getCheckedFiles().isEmpty() ? getStoredFiles() : getCheckedFiles();
			if (storedFiles.isEmpty()) {
				return;
			}
			List<File> files = new ArrayList<File>();
			for (StoredFile aFile : storedFiles) {
				files.add(new File(FILES_DIRECTORY, aFile.filename()));
			}
			String archiveFilename = "violet-diagrams-" + LocalDate.now() + DiagramArchive.EXTENSION;
			IFileWriter writer = new CheerpJDownloadFileWriter(archiveFilename);
			getDiagramArchive().write(files, writer.getOutputStream());
		} catch (Exception e) {
			dialogFactory.showErrorDialog(dialogExportArchiveFailedMessage + " : " + e.getMessage());
		}
	}

	/**
	 * Copies the diagrams of a zip archive into the browser storage, asking what to do with each diagram of the same name
	 * already there, as the file explorer of Windows does. Nothing is written if the user cancels, nor if the archive can't
	 * be read (not a zip, damaged, protected by a password) or holds no diagram. The diagrams opened in a workspace and
	 * replaced are opened again.
	 */
	private void importArchive() {
		try {
			// A pending auto-save of an opened diagram would write it over the imported file of the same name
			saveOpenedWorkspaces();
			ExtensionFilter archiveFilter = new ExtensionFilter(archiveFilterName, DiagramArchive.EXTENSION);
			IFileReader archiveReader = fileChooserService.chooseAndGetFileReader(archiveFilter);
			if (archiveReader == null) {
				// Action cancelled by user
				return;
			}
			List<DiagramArchive.Entry> entries;
			try (InputStream in = archiveReader.getInputStream()) {
				entries = getDiagramArchive().read(in, filename -> getDiagramType(filename) != null);
			} catch (DiagramArchive.UnreadableArchiveException e) {
				showImportMessage(getUnreadableArchiveMessage(e.getProblem()), JOptionPane.WARNING_MESSAGE);
				return;
			}
			if (entries.isEmpty()) {
				showImportMessage(dialogImportArchiveEmptyMessage, JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			List<DiagramArchive.Entry> entriesToWrite = chooseEntriesToWrite(entries);
			if (entriesToWrite == null) {
				return;
			}
			for (DiagramArchive.Entry anEntry : entriesToWrite) {
				IFileWriter writer = new CheerpJStorageFileWriter(anEntry.filename());
				try (OutputStream out = writer.getOutputStream()) {
					out.write(anEntry.content());
				}
			}
			reopenWorkspacesOf(entriesToWrite);
			refresh();
			showImportMessage(MessageFormat.format(dialogImportArchiveDoneMessage, entriesToWrite.size(),
					entries.size() - entriesToWrite.size()), JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception e) {
			refresh();
			dialogFactory.showErrorDialog(dialogOpenFileErrorMessage + " : " + e.getMessage());
		}
	}

	private String getUnreadableArchiveMessage(DiagramArchive.Problem problem) {
		switch (problem) {
		case ENCRYPTED:
			return MessageFormat.format(dialogImportArchiveEncryptedMessage, new Object[0]);
		case DAMAGED:
			return MessageFormat.format(dialogImportArchiveDamagedMessage, new Object[0]);
		default:
			return MessageFormat.format(dialogImportArchiveNotAnArchiveMessage, new Object[0]);
		}
	}

	private void showImportMessage(String message, int messageType) {
		JOptionPane messagePane = new JOptionPane(message, messageType);
		dialogFactory.showDialog(messagePane, dialogImportArchiveTitle, true);
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
		JOptionPane conflictPane = new JOptionPane(message.toArray(), JOptionPane.WARNING_MESSAGE, JOptionPane.DEFAULT_OPTION,
				null, options, dialogImportConflictSkipText);
		dialogFactory.showDialog(conflictPane, dialogImportConflictTitle, true);
		Object value = conflictPane.getValue();
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
		IWorkspace active = this.mainFrame.getActiveWorkspace();
		for (DiagramArchive.Entry anEntry : writtenEntries) {
			IWorkspace replaced = findOpenedWorkspace(anEntry.filename());
			if (replaced == null) {
				continue;
			}
			this.mainFrame.removeWorkspace(replaced);
			IFile file = new CheerpJFileReader(FILES_DIRECTORY, anEntry.filename()).getFileDefinition();
			IWorkspace workspace = new Workspace(new GraphFile(file));
			this.mainFrame.addWorkspace(workspace);
			if (replaced == active) {
				active = workspace;
			}
		}
		if (active != null) {
			this.mainFrame.setActiveWorkspace(active);
		}
	}

	private void saveOpenedWorkspaces() {
		for (IWorkspace aWorkspace : this.mainFrame.getWorkspaceList()) {
			IGraphFile graphFile = aWorkspace.getGraphFile();
			if (graphFile != null && graphFile.isSaveRequired()) {
				graphFile.save(false);
			}
		}
	}

	private boolean isStored(String filename) {
		return new File(FILES_DIRECTORY, filename).exists();
	}

	private IWorkspace findOpenedWorkspace(String filename) {
		for (IWorkspace aWorkspace : this.mainFrame.getWorkspaceList()) {
			IGraphFile graphFile = aWorkspace.getGraphFile();
			if (filename.equals(graphFile.getFilename()) && FILES_DIRECTORY.equals(graphFile.getDirectory())) {
				return aWorkspace;
			}
		}
		return null;
	}

	/**
	 * Reads the browser storage again, then updates the table, the summary and the buttons
	 */
	private void refresh() {
		if (getTable().isEditing()) {
			getTable().getCellEditor().cancelCellEditing();
		}
		getStoredFiles().clear();
		getStoredFiles().addAll(readStoredFiles());
		Set<String> storedFilenames = new HashSet<String>();
		long totalLength = 0;
		for (StoredFile aFile : getStoredFiles()) {
			storedFilenames.add(aFile.filename());
			totalLength += aFile.length();
		}
		getCheckedFilenames().retainAll(storedFilenames);
		int selectedRow = getTable().getSelectedRow();
		getTableModel().fireTableDataChanged();
		if (!getStoredFiles().isEmpty() && selectedRow >= 0) {
			int row = Math.min(selectedRow, getStoredFiles().size() - 1);
			getTable().getSelectionModel().setSelectionInterval(row, row);
		}
		getSummaryLabel().setText(MessageFormat.format(dialogSummary, getStoredFiles().size(), formatLength(totalLength)));
		updateButtons();
	}

	private void updateButtons() {
		int checkedCount = getCheckedFilenames().size();
		boolean isAllChecked = !getStoredFiles().isEmpty() && checkedCount == getStoredFiles().size();
		getSelectAllButton().setText(isAllChecked ? dialogDeselectAllText : dialogSelectAllText);
		getSelectAllButton().setEnabled(!getStoredFiles().isEmpty());
		getDeleteButton().setText(checkedCount > 0 ? MessageFormat.format(dialogDeleteManyText, checkedCount) : dialogDeleteText);
		getDeleteButton().setEnabled(checkedCount > 0);
		getExportButton().setText(checkedCount > 0 ? MessageFormat.format(dialogExportArchiveManyText, checkedCount)
				: dialogExportArchiveText);
		getExportButton().setEnabled(!getStoredFiles().isEmpty());
	}

	/**
	 * @return the diagram files of the browser storage, most recently modified first
	 */
	private List<StoredFile> readStoredFiles() {
		List<StoredFile> result = new ArrayList<StoredFile>();
		File[] files = new File(FILES_DIRECTORY).listFiles(File::isFile);
		if (files == null) {
			return result;
		}
		Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
		for (File aFile : files) {
			String type = getDiagramType(aFile.getName());
			if (type != null) {
				result.add(new StoredFile(aFile.getName(), type, aFile.lastModified(), aFile.length()));
			}
		}
		return result;
	}

	/**
	 * @return the name of the diagram type whose extension ends the file name ("Class diagram"), or null if it isn't a
	 * diagram
	 */
	private String getDiagramType(String filename) {
		String lowerCaseFilename = filename.toLowerCase(Locale.ROOT);
		for (IDiagramPlugin aPlugin : this.pluginRegistry.getDiagramPlugins()) {
			String extension = aPlugin.getFileExtension();
			if (extension != null && lowerCaseFilename.endsWith(extension.toLowerCase(Locale.ROOT))) {
				// Menu order prefixes such as "1." are not part of the name
				return aPlugin.getName().replaceFirst("^\\d+\\.", "");
			}
		}
		return null;
	}

	/**
	 * @return the file name without ".violet.html", the diagram type staying: "order.class"
	 */
	private String getDisplayName(String filename) {
		if (filename.toLowerCase(Locale.ROOT).endsWith(FILE_SUFFIX)) {
			return filename.substring(0, filename.length() - FILE_SUFFIX.length());
		}
		return filename;
	}

	/**
	 * @return "Today 14:02", "Yesterday 18:40", else the short date of the locale
	 */
	private String formatDate(long time) {
		LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
		LocalDate today = LocalDate.now();
		String hour = dateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
		if (dateTime.toLocalDate().equals(today)) {
			return MessageFormat.format(dialogToday, hour);
		}
		if (dateTime.toLocalDate().equals(today.minusDays(1))) {
			return MessageFormat.format(dialogYesterday, hour);
		}
		return dateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));
	}

	private String formatLength(long length) {
		if (length < 1024) {
			return MessageFormat.format(dialogSizeBytes, length);
		}
		if (length < 1024 * 1024) {
			return MessageFormat.format(dialogSizeKilobytes, length / 1024.0);
		}
		return MessageFormat.format(dialogSizeMegabytes, length / (1024.0 * 1024.0));
	}

	private DiagramArchive getDiagramArchive() {
		if (this.diagramArchive == null) {
			this.diagramArchive = new DiagramArchive();
		}
		return this.diagramArchive;
	}

	private DiagramArchive diagramArchive;

	private List<StoredFile> getStoredFiles() {
		if (this.storedFiles == null) {
			this.storedFiles = new ArrayList<StoredFile>();
		}
		return this.storedFiles;
	}

	private List<StoredFile> storedFiles;

	private Set<String> getCheckedFilenames() {
		if (this.checkedFilenames == null) {
			this.checkedFilenames = new HashSet<String>();
		}
		return this.checkedFilenames;
	}

	private Set<String> checkedFilenames;

	private AbstractTableModel getTableModel() {
		if (this.tableModel == null) {
			this.tableModel = new StoredFilesTableModel();
		}
		return this.tableModel;
	}

	private AbstractTableModel tableModel;

	private JLabel getSummaryLabel() {
		if (this.summaryLabel == null) {
			this.summaryLabel = new JLabel();
		}
		return this.summaryLabel;
	}

	private JLabel summaryLabel;

	private JButton getImportButton() {
		if (this.importButton == null) {
			this.importButton = new JButton(dialogImportArchiveText);
			this.importButton.setToolTipText(MessageFormat.format(dialogImportArchiveTooltip, new Object[0]));
			this.importButton.addActionListener(e -> importArchive());
		}
		return this.importButton;
	}

	private JButton importButton;

	private JButton getExportButton() {
		if (this.exportButton == null) {
			this.exportButton = new JButton(dialogExportArchiveText);
			this.exportButton.setToolTipText(MessageFormat.format(dialogExportArchiveTooltip, new Object[0]));
			this.exportButton.addActionListener(e -> exportArchive());
		}
		return this.exportButton;
	}

	private JButton exportButton;

	private JButton getSelectAllButton() {
		if (this.selectAllButton == null) {
			this.selectAllButton = new JButton(dialogSelectAllText);
			this.selectAllButton.addActionListener(e -> {
				boolean isAllChecked = getCheckedFilenames().size() == getStoredFiles().size();
				getCheckedFilenames().clear();
				if (!isAllChecked) {
					getStoredFiles().forEach(aFile -> getCheckedFilenames().add(aFile.filename()));
				}
				getTableModel().fireTableRowsUpdated(0, Math.max(0, getStoredFiles().size() - 1));
				updateButtons();
			});
		}
		return this.selectAllButton;
	}

	private JButton selectAllButton;

	private JButton getDeleteButton() {
		if (this.deleteButton == null) {
			this.deleteButton = new JButton(dialogDeleteText);
			this.deleteButton.addActionListener(e -> {
				List<StoredFile> files = getCheckedFiles();
				if (!files.isEmpty()) {
					delete(files);
				}
			});
		}
		return this.deleteButton;
	}

	private JButton deleteButton;

	private JButton getCloseButton() {
		if (this.closeButton == null) {
			this.closeButton = new JButton(dialogCloseText);
			this.closeButton.addActionListener(e -> getOptionPane().setValue(getCloseButton()));
		}
		return this.closeButton;
	}

	private JButton closeButton;

	/**
	 * Check box, name, opened or not, type, date, size
	 */
	private class StoredFilesTableModel extends AbstractTableModel {

		@Override
		public int getRowCount() {
			return getStoredFiles().size();
		}

		@Override
		public int getColumnCount() {
			return 6;
		}

		@Override
		public String getColumnName(int column) {
			switch (column) {
			case NAME_COLUMN:
				return dialogColumnName;
			case STATUS_COLUMN:
				return dialogColumnStatus;
			case TYPE_COLUMN:
				return dialogColumnType;
			case MODIFIED_COLUMN:
				return dialogColumnModified;
			case SIZE_COLUMN:
				return dialogColumnSize;
			default:
				return "";
			}
		}

		@Override
		public Class<?> getColumnClass(int column) {
			if (column == CHECK_COLUMN) {
				return Boolean.class;
			}
			return column == STATUS_COLUMN ? Boolean.class : String.class;
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return column == CHECK_COLUMN;
		}

		@Override
		public Object getValueAt(int row, int column) {
			StoredFile file = getStoredFiles().get(row);
			switch (column) {
			case CHECK_COLUMN:
				return Boolean.valueOf(getCheckedFilenames().contains(file.filename()));
			case NAME_COLUMN:
				return getDisplayName(file.filename());
			case STATUS_COLUMN:
				return Boolean.valueOf(findOpenedWorkspace(file.filename()) != null);
			case TYPE_COLUMN:
				return file.type();
			case MODIFIED_COLUMN:
				return formatDate(file.lastModified());
			case SIZE_COLUMN:
				return formatLength(file.length());
			default:
				return "";
			}
		}

		@Override
		public void setValueAt(Object value, int row, int column) {
			if (column != CHECK_COLUMN) {
				return;
			}
			String filename = getStoredFiles().get(row).filename();
			if (Boolean.TRUE.equals(value)) {
				getCheckedFilenames().add(filename);
			} else {
				getCheckedFilenames().remove(filename);
			}
			fireTableCellUpdated(row, column);
			updateButtons();
		}
	}

	/**
	 * "Open" badge, soft green pill centered in its cell, when the diagram is opened in a workspace; nothing otherwise
	 */
	private class StatusCellRenderer extends DefaultTableCellRenderer {

		private boolean isOpened;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			super.getTableCellRendererComponent(table, "", isSelected, false, row, column);
			this.isOpened = Boolean.TRUE.equals(value);
			return this;
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setColor(getBackground());
			g2.fillRect(0, 0, getWidth(), getHeight());
			if (this.isOpened) {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2.setFont(getFont().deriveFont(getFont().getSize2D() - 1f));
				FontMetrics metrics = g2.getFontMetrics();
				int textWidth = metrics.stringWidth(dialogOpenedBadge);
				int pillWidth = Math.min(getWidth() - 4, textWidth + 16);
				int pillHeight = metrics.getHeight() + 2;
				int x = (getWidth() - pillWidth) / 2;
				int y = (getHeight() - pillHeight) / 2;
				boolean isDark = FlatLaf.isLafDark();
				g2.setColor(isDark ? DARK_BADGE_BACKGROUND : BADGE_BACKGROUND);
				g2.fillRoundRect(x, y, pillWidth, pillHeight, pillHeight, pillHeight);
				g2.setColor(isDark ? DARK_BADGE_FOREGROUND : BADGE_FOREGROUND);
				g2.drawString(dialogOpenedBadge, x + (pillWidth - textWidth) / 2, y + 1 + metrics.getAscent());
			}
			g2.dispose();
		}
	}

	@InjectedBean
	private DialogFactory dialogFactory;

	@InjectedBean
	private IFileChooserService fileChooserService;

	@InjectedBean
	private UserPreferencesService userPreferencesService;

	@InjectedBean
	private PluginRegistry pluginRegistry;

	@ResourceBundleBean(key = "dialog.manage_files.title")
	private String dialogTitle;

	@ResourceBundleBean(key = "dialog.manage_files.header")
	private String dialogHeader;

	@ResourceBundleBean(key = "dialog.manage_files.column.name")
	private String dialogColumnName;

	@ResourceBundleBean(key = "dialog.manage_files.column.status")
	private String dialogColumnStatus;

	@ResourceBundleBean(key = "dialog.manage_files.column.type")
	private String dialogColumnType;

	@ResourceBundleBean(key = "dialog.manage_files.column.modified")
	private String dialogColumnModified;

	@ResourceBundleBean(key = "dialog.manage_files.column.size")
	private String dialogColumnSize;

	@ResourceBundleBean(key = "dialog.manage_files.opened")
	private String dialogOpenedBadge;

	@ResourceBundleBean(key = "dialog.manage_files.today")
	private String dialogToday;

	@ResourceBundleBean(key = "dialog.manage_files.yesterday")
	private String dialogYesterday;

	@ResourceBundleBean(key = "dialog.manage_files.size.bytes")
	private String dialogSizeBytes;

	@ResourceBundleBean(key = "dialog.manage_files.size.kilobytes")
	private String dialogSizeKilobytes;

	@ResourceBundleBean(key = "dialog.manage_files.size.megabytes")
	private String dialogSizeMegabytes;

	@ResourceBundleBean(key = "dialog.manage_files.summary")
	private String dialogSummary;

	@ResourceBundleBean(key = "dialog.manage_files.select_all")
	private String dialogSelectAllText;

	@ResourceBundleBean(key = "dialog.manage_files.deselect_all")
	private String dialogDeselectAllText;

	@ResourceBundleBean(key = "dialog.manage_files.close")
	private String dialogCloseText;

	@ResourceBundleBean(key = "dialog.manage_files.import_archive")
	private String dialogImportArchiveText;

	@ResourceBundleBean(key = "dialog.manage_files.import_archive.tooltip")
	private String dialogImportArchiveTooltip;

	@ResourceBundleBean(key = "dialog.manage_files.export_archive")
	private String dialogExportArchiveText;

	@ResourceBundleBean(key = "dialog.manage_files.export_archive_many")
	private String dialogExportArchiveManyText;

	@ResourceBundleBean(key = "dialog.manage_files.export_archive.tooltip")
	private String dialogExportArchiveTooltip;

	@ResourceBundleBean(key = "dialog.delete_from_storage.title")
	private String dialogDeleteTitle;

	@ResourceBundleBean(key = "dialog.delete_from_storage.title_many")
	private String dialogDeleteManyTitle;

	@ResourceBundleBean(key = "dialog.delete_from_storage.confirm")
	private String dialogConfirmMessage;

	@ResourceBundleBean(key = "dialog.delete_from_storage.confirm_opened")
	private String dialogConfirmOpenedMessage;

	@ResourceBundleBean(key = "dialog.delete_from_storage.confirm_many")
	private String dialogConfirmManyMessage;

	@ResourceBundleBean(key = "dialog.delete_from_storage.opened_suffix")
	private String dialogOpenedSuffix;

	@ResourceBundleBean(key = "dialog.delete_from_storage.delete")
	private String dialogDeleteText;

	@ResourceBundleBean(key = "dialog.delete_from_storage.close_and_delete")
	private String dialogCloseAndDeleteText;

	@ResourceBundleBean(key = "dialog.delete_from_storage.delete_many")
	private String dialogDeleteManyText;

	@ResourceBundleBean(key = "dialog.delete_from_storage.cancel")
	private String dialogCancelText;

	@ResourceBundleBean(key = "dialog.delete_from_storage.failed")
	private String dialogDeleteFromStorageFailedMessage;

	@ResourceBundleBean(key = "file.import_archive.text")
	private String archiveFilterName;

	@ResourceBundleBean(key = "dialog.open_file_failed.text")
	private String dialogOpenFileErrorMessage;

	@ResourceBundleBean(key = "dialog.import_archive.title")
	private String dialogImportArchiveTitle;

	@ResourceBundleBean(key = "dialog.import_archive.empty")
	private String dialogImportArchiveEmptyMessage;

	@ResourceBundleBean(key = "dialog.import_archive.done")
	private String dialogImportArchiveDoneMessage;

	@ResourceBundleBean(key = "dialog.import_archive.not_an_archive")
	private String dialogImportArchiveNotAnArchiveMessage;

	@ResourceBundleBean(key = "dialog.import_archive.damaged")
	private String dialogImportArchiveDamagedMessage;

	@ResourceBundleBean(key = "dialog.import_archive.encrypted")
	private String dialogImportArchiveEncryptedMessage;

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
}
