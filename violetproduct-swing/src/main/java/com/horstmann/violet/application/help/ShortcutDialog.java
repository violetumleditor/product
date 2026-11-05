package com.horstmann.violet.application.help;

import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.ResourceShortcutProvider;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Class responsible for create shortcut dialog
 * Created by piter on 02.01.16.
 */
public class ShortcutDialog extends JDialog 
{
    @ResourceBundleBean(key="dialog.title")
    private String dialogTitle;

    @ResourceBundleBean(key="dialog.table.behavior")
    private String behaviorName;

    @ResourceBundleBean(key="dialog.table.shortcut")
    private String shortcut ;

    @ResourceBundleBean(key="dialog.table.nodata")
    private String noData;

    private JPanel shortcutPanel;

    /**
     * Default constructor of ShortcutDialog
     * @param parent JFrame parent
     */
    public ShortcutDialog(JFrame parent)
    {
        super(parent);
        ResourceBundleInjector.getInjector().inject(this);
        
        this.setTitle(dialogTitle);
        this.setLocationRelativeTo(null);
        this.setModal(true);
        this.setResizable(false);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(buildShortcutPanel(), BorderLayout.CENTER);
        pack();
        setLocation(parent);
    }

    private JPanel buildShortcutPanel()
    {
        if(shortcutPanel == null)
        {
            shortcutPanel = new JPanel(new BorderLayout());
            
            String[] columnNames = {behaviorName, shortcut};

            JTable table = new JTable(prepareDataForTable(), columnNames);
            table.setEnabled(false);
            table.setCellSelectionEnabled(false);
            table.setShowGrid(true);
            table.setGridColor(UIManager.getColor("Separator.foreground"));
            table.setRowHeight(30);
            table.setIntercellSpacing(new Dimension(15, 0));
            table.getTableHeader().setFont(new Font("Times New Roman", Font.BOLD, 14));
            
            JScrollPane scrollPane = new JScrollPane(table);
            shortcutPanel.add(scrollPane, BorderLayout.CENTER);
        }
        return this.shortcutPanel;
    }
    
    private String[][] prepareDataForTable()
    {
        final int shortcutNumber = ResourceShortcutProvider.getInstance().getAllShortcuts().size();
        String[][] shortcutArray;

        if(shortcutNumber != 0)
        {
            shortcutArray = new String[shortcutNumber][2];

            int counter = 0;
            for (Map.Entry<String, String> entry : ResourceShortcutProvider.getInstance().getAllShortcuts().entrySet())
            {
                shortcutArray[counter][0] = entry.getKey();
                shortcutArray[counter][1] = entry.getValue();
                counter++;
            }
        } else
            {
                shortcutArray = new String[1][2];
                shortcutArray[0][0] = noData;
                shortcutArray[0][1] = noData;
            }
        return shortcutArray;
    }
    
    private void setLocation(JFrame parent)
    {
        Point point = parent.getLocationOnScreen();
        int x = (int) point.getX() + parent.getWidth() / 2;
        int y = (int) point.getY() + parent.getHeight() / 2;
        setLocation(x - getWidth() / 2, y - getHeight() / 2);
    }
    
}
