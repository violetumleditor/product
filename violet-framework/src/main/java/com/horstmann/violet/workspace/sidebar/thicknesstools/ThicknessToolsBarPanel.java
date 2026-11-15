package com.horstmann.violet.workspace.sidebar.thicknesstools;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.workspace.IWorkspace;
import com.horstmann.violet.workspace.sidebar.SideBar;

@ResourceBundleBean(resourceReference = SideBar.class)
public class ThicknessToolsBarPanel extends JPanel implements IThicknessChoiceBar
{

    public ThicknessToolsBarPanel()
    {
        ResourceBundleInjector.getInjector().inject(this);
        this.ui = new ThicknessToolsBarPanelUI(this);
        setUI(this.ui);
    }

    /**
     * Keeps the custom UI, and takes the colors of the new theme, when the look and feel changes
     */
    @Override
    public void updateUI()
    {
        if (getUI() instanceof ThicknessToolsBarPanelUI)
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
    public void addThicknessChangeListener(IThicknessChangeListener listener)
    {
        this.thicknessChangeListenersList.add(listener);
    }

    @Override
    public void setShownThickness(int thickness)
    {
        int newThickness = Math.max(MIN_THICKNESS, Math.min(MAX_THICKNESS, thickness));
        if (newThickness == this.shownThickness)
        {
            return;
        }
        this.shownThickness = newThickness;
        this.ui.refresh();
        for (Runnable aListener : this.shownThicknessListeners)
        {
            aListener.run();
        }
    }

    @Override
    public int getShownThickness()
    {
        return this.shownThickness;
    }

    @Override
    public void addShownThicknessListener(Runnable listener)
    {
        this.shownThicknessListeners.add(listener);
    }

    private int shownThickness = DEFAULT_THICKNESS;
    private List<Runnable> shownThicknessListeners = new ArrayList<Runnable>();

    public void fireThicknessChanged(int newThickness)
    {
        setShownThickness(newThickness);
        for (IThicknessChangeListener aListener : this.thicknessChangeListenersList)
        {
            aListener.onThicknessChange(newThickness);
        }
    }

    /**
     * @return the title shown above the choices
     */
    public String getTitle()
    {
        return this.title;
    }

    @ResourceBundleBean(key = "title.thicknesstools.text")
    private String title;

    /** Minimum thickness value */
    public static final int MIN_THICKNESS = 1;

    /** Maximum thickness value */
    public static final int MAX_THICKNESS = 10;

    /** Default thickness value */
    public static final int DEFAULT_THICKNESS = 1;

    @SuppressWarnings("unused")
    private IWorkspace diagramPanel;
    private List<IThicknessChangeListener> thicknessChangeListenersList = new ArrayList<IThicknessChangeListener>();
    private ThicknessToolsBarPanelUI ui;
}
