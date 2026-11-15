package com.horstmann.violet.workspace.sidebar.colortools;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.workspace.IWorkspace;
import com.horstmann.violet.workspace.sidebar.SideBar;

@ResourceBundleBean(resourceReference = SideBar.class)
public class ColorToolsBarPanel extends JPanel implements IColorChoiceBar
{

    public ColorToolsBarPanel()
    {
        ResourceBundleInjector.getInjector().inject(this);
        this.ui = new ColorToolsBarPanelUI(this);
        setUI(this.ui);
    }

    /**
     * Keeps the custom UI, and takes the colors of the new theme, when the look and feel changes
     */
    @Override
    public void updateUI()
    {
        if (getUI() instanceof ColorToolsBarPanelUI)
        {
            setUI(this.ui);
            return;
        }
        super.updateUI();
    }

    @Override
    public void install(IWorkspace workspace)
    {
        this.diagramPanel = workspace;
    }

    @Override
    public Component getAWTComponent()
    {
        return this;
    }

    @Override
    public void addColorChoiceChangeListener(IColorChoiceChangeListener listener)
    {
        this.colorChoiceChangeListenersList.add(listener);
    }

    
    @Override
    public void setShownColorChoice(ColorChoice colorChoice)
    {
        ColorChoice newColorChoice = colorChoice != null ? colorChoice : ColorChoice.DEFAULT;
        if (newColorChoice.equals(this.shownColorChoice))
        {
            return;
        }
        this.shownColorChoice = newColorChoice;
        repaint();
        for (Runnable aListener : this.shownColorChoiceListeners)
        {
            aListener.run();
        }
    }

    @Override
    public ColorChoice getShownColorChoice()
    {
        return this.shownColorChoice;
    }

    @Override
    public void addShownColorChoiceListener(Runnable listener)
    {
        this.shownColorChoiceListeners.add(listener);
    }

    private ColorChoice shownColorChoice = ColorChoice.DEFAULT;
    private List<Runnable> shownColorChoiceListeners = new ArrayList<Runnable>();

    public void fireColorChoiceChanged(ColorChoice newColorChoice)
    {
        setShownColorChoice(newColorChoice);
        for (IColorChoiceChangeListener aListener : this.colorChoiceChangeListenersList)
        {
            aListener.onColorChoiceChange(newColorChoice);
        }
    }

    /**
     * @return the title shown above the choices
     */
    public String getTitle()
    {
        return this.title;
    }

    @ResourceBundleBean(key = "title.colortools.text")
    private String title;

    /**
     * Current diagram panel
     */
    @SuppressWarnings("unused")
    private IWorkspace diagramPanel;

    private List<IColorChoiceChangeListener> colorChoiceChangeListenersList = new ArrayList<IColorChoiceChangeListener>();
    private ColorToolsBarPanelUI ui;

}
