package com.horstmann.violet.workspace.sidebar.colortools;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
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
     * Current diagram panel
     */
    @SuppressWarnings("unused")
    private IWorkspace diagramPanel;

    // Source : http://www.tinygorilla.com/Easter_eggs/pallatehex.html
    // http://www.colorhexa.com/
    public static final ColorChoice DEFAULT_COLOR = new ColorChoice(Color.WHITE, new Color(191,191,191));
    public static final ColorChoice PASTEL_GREY = new ColorChoice(new Color(242,242,242), new Color(91,91,91));
    private static final ColorChoice PASTEL_RED = new ColorChoice(new Color(251,199,183), new Color(246,132,98));
    private static final ColorChoice PASTEL_RED_ORANGE = new ColorChoice(new Color(252,212,190), new Color(248,157,105));
    public static final ColorChoice PASTEL_YELLOW_ORANCE = new ColorChoice(new Color(254,227,198), new Color(253,186,113));
    private static final ColorChoice PASTEL_YELLOW = new ColorChoice(new Color(255,252,212), new Color(255,222,154));
    private static final ColorChoice PASTEL_PEA_GREEN = new ColorChoice(new Color(224,238,203), new Color(185,217,136));
    private static final ColorChoice PASTEL_YELLOW_GREEN = new ColorChoice(new Color(204,231,201), new Color(145,203,138));
    private static final ColorChoice PASTEL_GREEN = new ColorChoice(new Color(179,223,196), new Color(112,194,143));
    private static final ColorChoice PASTEL_GREEN_CYAN = new ColorChoice(new Color(190,230,228), new Color(123,205,200));
    private static final ColorChoice PASTEL_CYAN = new ColorChoice(new Color(193,235,251), new Color(110,207,246));
    private static final ColorChoice PASTEL_CYAN_BLUE = new ColorChoice(new Color(196,214,237), new Color(126,167,216));
    private static final ColorChoice PASTEL_BLUE = new ColorChoice(new Color(196,203,230), new Color(132,147,202));
    private static final ColorChoice PASTEL_BLUE_VIOLET = new ColorChoice(new Color(195,191,222), new Color(136,130,190));
    private static final ColorChoice PASTEL_VIOLET = new ColorChoice(new Color(208,196,223), new Color(161,135,190));
    private static final ColorChoice PASTEL_VIOLET_MAGENTA = new ColorChoice(new Color(223,200,224), new Color(188,141,191));
    private static final ColorChoice PASTEL_MAGENTA = new ColorChoice(new Color(250,208,227), new Color(241,131,180));
    private static final ColorChoice PASTEL_MAGENTA_RED = new ColorChoice(new Color(251,207,210), new Color(244,129,135));
    private static final ColorChoice PASTEL_BLACK = new ColorChoice(new Color(201,201,201), new Color(0,0,0));

    public static final Color DEFAULT_TEXT_COLOR = new Color(51,51,51);
    public static final List<Color> TEXT_COLOR_LIST = Arrays.asList(
            DEFAULT_TEXT_COLOR,
            new Color(91,91,91),
            new Color(205,70,29),
            new Color(190,86,27),
            new Color(170,100,24),
            new Color(152,109,22),
            new Color(86,129,18),
            new Color(31,134,19),
            new Color(19,134,63),
            new Color(18,129,123),
            new Color(24,125,165),
            new Color(31,115,216),
            new Color(74,107,229),
            new Color(92,76,230),
            new Color(149,76,230),
            new Color(199,30,210),
            new Color(221,32,116),
            new Color(224,41,50),
            Color.BLACK);

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
