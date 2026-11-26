package com.horstmann.violet.application.cheerpj;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
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
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import com.horstmann.violet.application.gui.MainFrame;
import com.horstmann.violet.application.menu.MenuFactory;
import com.horstmann.violet.framework.dialog.DialogFactory;
import com.horstmann.violet.framework.file.IFile;
import com.horstmann.violet.framework.file.IGraphFile;
import com.horstmann.violet.framework.file.chooser.IFileChooserService;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.InjectedBean;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.plugin.IDiagramPlugin;
import com.horstmann.violet.framework.plugin.PluginRegistry;
import com.horstmann.violet.framework.userpreferences.UserPreferencesService;
import com.horstmann.violet.workspace.IWorkspace;

/**
 * "File > Manage files" of the browser version: lists every diagram kept in the browser storage (name, type, date, size)
 * and deletes the checked ones, or one line by its trash icon. Every deletion is confirmed, as it can't be undone. A
 * diagram opened in a workspace is shown behind the dialog, then closed before being deleted, so that its auto-save can't
 * write it back. A double click on a line opens its diagram.
 */
@ResourceBundleBean(resourceReference = MenuFactory.class)
class StoredFilesManager {

	private static final String FILES_DIRECTORY = "/files";

	private static final String FILE_SUFFIX = ".violet.html";

	private static final int CHECK_COLUMN = 0;
	private static final int NAME_COLUMN = 1;
	private static final int TYPE_COLUMN = 2;
	private static final int MODIFIED_COLUMN = 3;
	private static final int SIZE_COLUMN = 4;
	private static final int TRASH_COLUMN = 5;

	/** Lines of a confirmation listing several diagrams, the next ones being summed up by "…" */
	private static final int MAX_LISTED_FILES = 10;

	private static final Color DEFAULT_ACCENT_COLOR = new Color(0x6C4EBE);

	/**
	 * A diagram file of the browser storage
	 */
	private record StoredFile(String filename, String type, long lastModified, long length) {
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
			content.add(new JLabel(dialogHeader), BorderLayout.NORTH);
			JScrollPane scrollPane = new JScrollPane(getTable());
			int width = Math.max(320, Math.min(680, this.mainFrame.getWidth() - 60));
			int height = Math.max(140, Math.min(300, this.mainFrame.getHeight() - 260));
			scrollPane.setPreferredSize(new Dimension(width, height));
			content.add(scrollPane, BorderLayout.CENTER);
			content.add(getSummaryLabel(), BorderLayout.SOUTH);
			Object[] options = { getSelectAllButton(), getDeleteButton(), getCloseButton() };
			this.optionPane = new JOptionPane(content, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, options,
					getCloseButton());
		}
		return this.optionPane;
	}

	private JOptionPane optionPane;

	private JTable getTable() {
		if (this.table == null) {
			this.table = new JTable(getTableModel()) {
				@Override
				public String getToolTipText(MouseEvent e) {
					return columnAtPoint(e.getPoint()) == TRASH_COLUMN && rowAtPoint(e.getPoint()) >= 0
							? dialogDeleteFromStorageTooltip : null;
				}
			};
			this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			this.table.setRowHeight(28);
			this.table.setShowGrid(false);
			this.table.setFillsViewportHeight(true);
			this.table.getTableHeader().setReorderingAllowed(false);
			TableColumnModel columns = this.table.getColumnModel();
			setColumnWidth(columns, CHECK_COLUMN, 32, true);
			setColumnWidth(columns, NAME_COLUMN, 220, false);
			setColumnWidth(columns, TYPE_COLUMN, 150, false);
			setColumnWidth(columns, MODIFIED_COLUMN, 130, false);
			setColumnWidth(columns, SIZE_COLUMN, 70, false);
			setColumnWidth(columns, TRASH_COLUMN, 32, true);
			columns.getColumn(NAME_COLUMN).setCellRenderer(new NameCellRenderer());
			DefaultTableCellRenderer sizeRenderer = new DefaultTableCellRenderer();
			sizeRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
			columns.getColumn(SIZE_COLUMN).setCellRenderer(sizeRenderer);
			columns.getColumn(TRASH_COLUMN).setCellRenderer(new TrashCellRenderer());
			this.table.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					onTableClicked(e);
				}
			});
			this.table.addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseMoved(MouseEvent e) {
					boolean onTrash = getTable().columnAtPoint(e.getPoint()) == TRASH_COLUMN
							&& getTable().rowAtPoint(e.getPoint()) >= 0;
					getTable().setCursor(Cursor.getPredefinedCursor(onTrash ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
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
		int column = getTable().columnAtPoint(e.getPoint());
		if (row < 0) {
			return;
		}
		StoredFile file = getStoredFiles().get(row);
		if (column == TRASH_COLUMN) {
			if (e.getClickCount() == 1) {
				delete(List.of(file));
			}
			return;
		}
		if (column != CHECK_COLUMN && e.getClickCount() == 2) {
			this.filenameToOpen = file.filename();
			getOptionPane().setValue(getCloseButton());
		}
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
			IGraphFile graphFile = workspace.getGraphFile();
			try {
				// Clears the unsaved state, else the pending auto-save would write the file back once deleted
				graphFile.save(false);
			} catch (RuntimeException e) {
				// The file is deleted anyway
			}
			this.mainFrame.removeWorkspace(workspace);
		}
		this.fileChooserService.getFileDeleter(file).delete();
		this.userPreferencesService.removeRecentFile(file);
		this.userPreferencesService.removeOpenedFile(file);
		return !new File(FILES_DIRECTORY, filename).exists();
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
	 * Check box, name, type, date, size, trash icon
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
			return column == CHECK_COLUMN ? Boolean.class : String.class;
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
	 * The name, followed by an "Open" badge when the diagram is opened in a workspace
	 */
	private class NameCellRenderer extends JPanel implements TableCellRenderer {

		private final DefaultTableCellRenderer label = new DefaultTableCellRenderer();

		private final JLabel badge = new BadgeLabel();

		NameCellRenderer() {
			super(new FlowLayout(FlowLayout.LEFT, 0, 0));
			this.label.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 8));
			add(this.label);
			add(this.badge);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			this.label.getTableCellRendererComponent(table, value, isSelected, false, row, column);
			setBackground(this.label.getBackground());
			setBorder(BorderFactory.createEmptyBorder((table.getRowHeight(row) - this.label.getPreferredSize().height) / 2, 0, 0, 0));
			this.badge.setText(dialogOpenedBadge);
			this.badge.setVisible(findOpenedWorkspace(getStoredFiles().get(row).filename()) != null);
			return this;
		}
	}

	/**
	 * Small white text on a rounded pill of the accent color
	 */
	private static class BadgeLabel extends JLabel {

		BadgeLabel() {
			setForeground(Color.WHITE);
			setBorder(BorderFactory.createEmptyBorder(1, 7, 1, 7));
		}

		@Override
		public void updateUI() {
			super.updateUI();
			setFont(getFont().deriveFont(getFont().getSize2D() - 1f));
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Color accent = UIManager.getColor("Component.accentColor");
			g2.setColor(accent != null ? accent : DEFAULT_ACCENT_COLOR);
			g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
			g2.dispose();
			super.paintComponent(g);
		}
	}

	/**
	 * Trash icon, in the text color of the line
	 */
	private static class TrashCellRenderer extends DefaultTableCellRenderer {

		TrashCellRenderer() {
			setIcon(new TrashIcon());
			setHorizontalAlignment(SwingConstants.CENTER);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			super.getTableCellRendererComponent(table, "", isSelected, false, row, column);
			return this;
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

	@ResourceBundleBean(key = "dialog.delete_from_storage.tooltip")
	private String dialogDeleteFromStorageTooltip;

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
}
