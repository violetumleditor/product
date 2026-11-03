package com.horstmann.violet.workspace.sidebar.colortools;

import java.awt.Color;
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

    
    public void fireColorChoiceChanged(ColorChoice newColorChoice)
    {
        for (IColorChoiceChangeListener aListener : this.colorChoiceChangeListenersList)
        {
            aListener.onColorChoiceChange(newColorChoice);
        }
    }

    /**
     * @return the border colors of the predefined color choices, in palette order
     */
    public static List<Color> getBorderColors()
    {
        List<Color> borderColors = new ArrayList<Color>();
        for (ColorChoice aChoice : CHOICE_LIST)
        {
            borderColors.add(aChoice.getBorderColor());
        }
        return borderColors;
    }

    /**
     * Current diagram panel
     */
    @SuppressWarnings("unused")
    private IWorkspace diagramPanel;

    // Source : http://www.tinygorilla.com/Easter_eggs/pallatehex.html
    // http://www.colorhexa.com/
    public static final ColorChoice DEFAULT_COLOR = new ColorChoice(Color.WHITE, new Color(191,191,191), new Color(91,91,91));
    public static final ColorChoice PASTEL_GREY = new ColorChoice(new Color(240,240,240), new Color(91,91,91), new Color(91,91,91));
    private static final ColorChoice PASTEL_RED = new ColorChoice(new Color(250,189,170), new Color(246,132,98), new Color(91,91,91));
    private static final ColorChoice PASTEL_RED_ORANGE = new ColorChoice(new Color(251,205,178), new Color(248,157,105), new Color(91,91,91));
    public static final ColorChoice PASTEL_YELLOW_ORANCE = new ColorChoice(new Color(254,222,188), new Color(253,186,113), new Color(91,91,91));
    private static final ColorChoice PASTEL_YELLOW = new ColorChoice(new Color(255,251,205), new Color(255,222,154), new Color(91,91,91));
    private static final ColorChoice PASTEL_PEA_GREEN = new ColorChoice(new Color(219,235,194), new Color(185,217,136), new Color(91,91,91));
    private static final ColorChoice PASTEL_YELLOW_GREEN = new ColorChoice(new Color(195,227,191), new Color(145,203,138), new Color(91,91,91));
    private static final ColorChoice PASTEL_GREEN = new ColorChoice(new Color(166,217,185), new Color(112,194,143), new Color(91,91,91));
    private static final ColorChoice PASTEL_GREEN_CYAN = new ColorChoice(new Color(178,226,223), new Color(123,205,200), new Color(91,91,91));
    private static final ColorChoice PASTEL_CYAN = new ColorChoice(new Color(182,231,250), new Color(110,207,246), new Color(91,91,91));
    private static final ColorChoice PASTEL_CYAN_BLUE = new ColorChoice(new Color(185,207,234), new Color(126,167,216), new Color(91,91,91));
    private static final ColorChoice PASTEL_BLUE = new ColorChoice(new Color(185,194,225), new Color(132,147,202), new Color(91,91,91));
    private static final ColorChoice PASTEL_BLUE_VIOLET = new ColorChoice(new Color(184,180,216), new Color(136,130,190), new Color(91,91,91));
    private static final ColorChoice PASTEL_VIOLET = new ColorChoice(new Color(200,185,217), new Color(161,135,190), new Color(91,91,91));
    private static final ColorChoice PASTEL_VIOLET_MAGENTA = new ColorChoice(new Color(217,190,219), new Color(188,141,191), new Color(91,91,91));
    private static final ColorChoice PASTEL_MAGENTA = new ColorChoice(new Color(249,200,222), new Color(241,131,180), new Color(91,91,91));
    private static final ColorChoice PASTEL_MAGENTA_RED = new ColorChoice(new Color(250,199,202), new Color(244,129,135), new Color(91,91,91));
    private static final ColorChoice PASTEL_BLACK = new ColorChoice(new Color(191,191,191), new Color(0,0,0), new Color(0,0,0));

    protected static final List<ColorChoice> CHOICE_LIST = new ArrayList<ColorChoice>();
    private List<IColorChoiceChangeListener> colorChoiceChangeListenersList = new ArrayList<IColorChoiceChangeListener>();
    private ColorToolsBarPanelUI ui;
    
    
    static
    {
        CHOICE_LIST.add(DEFAULT_COLOR);
        CHOICE_LIST.add(PASTEL_GREY);
        CHOICE_LIST.add(PASTEL_RED);
        CHOICE_LIST.add(PASTEL_RED_ORANGE);
        CHOICE_LIST.add(PASTEL_YELLOW_ORANCE);
        CHOICE_LIST.add(PASTEL_YELLOW);
        CHOICE_LIST.add(PASTEL_PEA_GREEN);
        CHOICE_LIST.add(PASTEL_YELLOW_GREEN);
        CHOICE_LIST.add(PASTEL_GREEN);
        CHOICE_LIST.add(PASTEL_GREEN_CYAN);
        CHOICE_LIST.add(PASTEL_CYAN);
        CHOICE_LIST.add(PASTEL_CYAN_BLUE);
        CHOICE_LIST.add(PASTEL_BLUE);
        CHOICE_LIST.add(PASTEL_BLUE_VIOLET);
        CHOICE_LIST.add(PASTEL_VIOLET);
        CHOICE_LIST.add(PASTEL_VIOLET_MAGENTA);
        CHOICE_LIST.add(PASTEL_MAGENTA);
        CHOICE_LIST.add(PASTEL_MAGENTA_RED);
        CHOICE_LIST.add(PASTEL_BLACK);
    }

}
