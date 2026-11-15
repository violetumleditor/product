package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Component;
import java.awt.geom.Point2D;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.workspace.IWorkspace;
import com.horstmann.violet.workspace.editorpart.EditorPart;
import com.horstmann.violet.workspace.editorpart.behavior.ShowSelectionStyleBehavior;
import com.horstmann.violet.workspace.sidebar.colortools.ColorChoice;
import com.horstmann.violet.workspace.sidebar.colortools.IColorChoiceBar;
import com.horstmann.violet.workspace.sidebar.colortools.IColorChoiceChangeListener;
import com.horstmann.violet.workspace.sidebar.thicknesstools.IThicknessChangeListener;
import com.horstmann.violet.workspace.sidebar.thicknesstools.IThicknessChoiceBar;

/**
 * The style tools of the side bar show the style of the selection
 */
class ShowSelectionStyleBehaviorTest
{

    private EditorPart editorPart;
    private ClassNode blueNode;
    private ClassNode otherBlueNode;
    private ClassNode redNode;
    private ColorBar colorBar;
    private ThicknessBar thicknessBar;
    private ShowSelectionStyleBehavior behavior;

    @BeforeEach
    void createDiagram()
    {
        ClassDiagramGraph graph = new ClassDiagramGraph();
        this.blueNode = createNode(graph, ColorChoice.PASTEL_BLUE, 4);
        this.otherBlueNode = createNode(graph, ColorChoice.PASTEL_BLUE, 4);
        this.redNode = createNode(graph, ColorChoice.PASTEL_RED, 7);
        this.editorPart = new EditorPart(graph);
        this.colorBar = new ColorBar();
        this.thicknessBar = new ThicknessBar();
        this.behavior = new ShowSelectionStyleBehavior(this.editorPart, this.colorBar, this.thicknessBar);
    }

    private ClassNode createNode(ClassDiagramGraph graph, ColorChoice colorChoice, int thickness)
    {
        ClassNode node = new ClassNode();
        node.setBackgroundColor(colorChoice.getBackgroundColor());
        node.setBorderColor(colorChoice.getBorderColor());
        node.setBorderWidth(thickness);
        graph.addNode(node, new Point2D.Double(0, 0));
        return node;
    }

    @Test
    void shouldShowTheStyleOfTheSelectedElement()
    {
        this.editorPart.getSelectionHandler().setSelectedElement(this.blueNode);
        this.behavior.onPaint(null);
        assertEquals(ColorChoice.PASTEL_BLUE, this.colorBar.shown);
        assertEquals(4, this.thicknessBar.shown);

        this.editorPart.getSelectionHandler().setSelectedElement(this.redNode);
        this.behavior.onPaint(null);
        assertEquals(ColorChoice.PASTEL_RED, this.colorBar.shown);
        assertEquals(7, this.thicknessBar.shown);
    }

    @Test
    void shouldShowTheStyleSharedBySeveralElements()
    {
        this.editorPart.getSelectionHandler().setSelectedElement(this.blueNode);
        this.editorPart.getSelectionHandler().addSelectedElement(this.otherBlueNode);
        this.behavior.onPaint(null);
        assertEquals(ColorChoice.PASTEL_BLUE, this.colorBar.shown);
        assertEquals(4, this.thicknessBar.shown);
    }

    @Test
    void shouldShowTheDefaultStyleWhenStylesDifferOrNothingIsSelected()
    {
        this.editorPart.getSelectionHandler().setSelectedElement(this.blueNode);
        this.behavior.onPaint(null);
        this.editorPart.getSelectionHandler().addSelectedElement(this.redNode);
        this.behavior.onPaint(null);
        assertEquals(ColorChoice.DEFAULT, this.colorBar.shown);
        assertEquals(1, this.thicknessBar.shown);

        this.editorPart.getSelectionHandler().setSelectedElement(this.redNode);
        this.behavior.onPaint(null);
        this.editorPart.getSelectionHandler().clearSelection();
        this.behavior.onPaint(null);
        assertEquals(ColorChoice.DEFAULT, this.colorBar.shown);
        assertEquals(1, this.thicknessBar.shown);
    }

    @Test
    void shouldKeepAChoiceMadeWithNothingSelectedUntilTheSelectionChanges()
    {
        this.editorPart.getSelectionHandler().clearSelection();
        this.behavior.onPaint(null);
        this.colorBar.shown = ColorChoice.PASTEL_GREEN;
        this.thicknessBar.shown = 9;
        this.behavior.onPaint(null);
        assertEquals(ColorChoice.PASTEL_GREEN, this.colorBar.shown);
        assertEquals(9, this.thicknessBar.shown);

        // An element with the default style, as the empty selection had
        ClassNode defaultNode = new ClassNode();
        this.editorPart.getGraph().addNode(defaultNode, new Point2D.Double(0, 0));
        this.editorPart.getSelectionHandler().setSelectedElement(defaultNode);
        this.behavior.onPaint(null);
        assertEquals(ColorChoice.DEFAULT, this.colorBar.shown);
        assertEquals(1, this.thicknessBar.shown);
    }

    private static class ColorBar implements IColorChoiceBar
    {
        private ColorChoice shown;

        @Override
        public void setShownColorChoice(ColorChoice colorChoice)
        {
            this.shown = colorChoice;
        }

        @Override
        public ColorChoice getShownColorChoice()
        {
            return this.shown;
        }

        @Override
        public void addShownColorChoiceListener(Runnable listener)
        {
        }

        @Override
        public void addColorChoiceChangeListener(IColorChoiceChangeListener listener)
        {
        }

        @Override
        public void install(IWorkspace workspace)
        {
        }

        @Override
        public Component getAWTComponent()
        {
            return null;
        }
    }

    private static class ThicknessBar implements IThicknessChoiceBar
    {
        private int shown;

        @Override
        public void setShownThickness(int thickness)
        {
            this.shown = thickness;
        }

        @Override
        public int getShownThickness()
        {
            return this.shown;
        }

        @Override
        public void addShownThicknessListener(Runnable listener)
        {
        }

        @Override
        public void addThicknessChangeListener(IThicknessChangeListener listener)
        {
        }

        @Override
        public void install(IWorkspace workspace)
        {
        }

        @Override
        public Component getAWTComponent()
        {
            return null;
        }
    }

}
