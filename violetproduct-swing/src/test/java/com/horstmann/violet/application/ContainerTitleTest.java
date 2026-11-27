package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.XMLPersistenceService;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.property.AngleStyle;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.product.diagram.bpmn.ProcessDiagramGraph;
import com.horstmann.violet.product.diagram.bpmn.ProcessPoolNode;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.common.ContainerNode;

/**
 * The title of a grouping area: at the top inside its frame, in its border color, centered, bold and a little larger than the
 * standard text by default, all of which comes back from the HTML of the title after a reload.
 */
class ContainerTitleTest
{
    @Test
    void newTitleIsCenteredBoldAndALittleLarger()
    {
        MultiLineString title = new ContainerNode().getTitle();

        assertTrue(title.isBlank());
        assertTrue(title.isBold());
        assertEquals(14, title.getSize());
        assertEquals(MultiLineString.Justification.CENTER, title.getJustification());
    }

    @Test
    void typedTitleKeepsTheDefaultStyle()
    {
        ContainerNode container = new ContainerNode();
        container.getTitle().setText("Back office");

        assertTrue(container.getTitle().isBold());
        assertEquals(14, container.getTitle().getSize());
    }

    @Test
    void boldTitleIsWiderThanAPlainOne()
    {
        MultiLineString plain = new MultiLineString();
        plain.setSize(14);
        plain.setText("Back office");
        MultiLineString bold = new ContainerNode().getTitle();
        bold.setText("Back office");

        assertTrue(bold.getBounds().getWidth() > plain.getBounds().getWidth());
    }

    @Test
    void titleKeepsItsTextAndStyleAfterAReload() throws IOException
    {
        ContainerNode container = new ContainerNode();
        container.getTitle().setText("Back office");
        container.getTitle().setJustification(MultiLineString.Justification.LEFT);

        ContainerNode reloaded = reload(container);

        assertEquals("Back office", reloaded.getTitle().toString().replaceAll("<[^>]*>", "").trim());
        assertTrue(reloaded.getTitle().isBold());
        assertEquals(14, reloaded.getTitle().getSize());
        assertEquals(MultiLineString.Justification.LEFT, reloaded.getTitle().getJustification());
    }

    @Test
    void titleMadePlainStaysPlainAfterAReload() throws IOException
    {
        ContainerNode container = new ContainerNode();
        container.getTitle().setText("Back office");
        container.getTitle().setBold(false);

        assertFalse(reload(container).getTitle().isBold());
    }

    @Test
    void emptyTitleGetsItsDefaultsBackAfterAReload() throws IOException
    {
        ContainerNode container = new ContainerNode();
        container.getTitle().setText("");

        ContainerNode reloaded = reload(container);
        reloaded.getTitle().setText("Typed later");

        assertTrue(reloaded.getTitle().isBold());
        assertEquals(14, reloaded.getTitle().getSize());
    }

    @Test
    void titleIsDrawnAtTheTopInTheBorderColor()
    {
        IGraph graph = new ClassDiagramGraph();
        ContainerNode container = new ContainerNode();
        container.setPreferredSize(new Rectangle2D.Double(0, 0, 300, 200));
        container.setBorderColor(new Color(200, 0, 0));
        container.getTitle().setText("WWWWWWWW");
        graph.addNode(container, new Point2D.Double(0, 0));

        BufferedImage image = new BufferedImage(300, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        container.draw(g2);
        g2.dispose();

        int titlePixels = 0;
        int lowestTitlePixel = 0;
        for (int y = 2; y < 198; y++)
        {
            for (int x = 2; x < 298; x++)
            {
                Color pixel = new Color(image.getRGB(x, y), true);
                if (pixel.getAlpha() > 128)
                {
                    assertTrue(pixel.getRed() > 100 && pixel.getGreen() < 80, "Not in the border color: " + pixel);
                    titlePixels++;
                    lowestTitlePixel = Math.max(lowestTitlePixel, y);
                }
            }
        }
        assertTrue(titlePixels > 0);
        assertTrue(lowestTitlePixel < 30, "Title drawn down to " + lowestTitlePixel);
        // Centered by default
        assertTrue(container.getTitle().getDrawnTextArea().getCenterX() == 150);
    }

    @Test
    void titleStaysInsideStraightAndRoundedCornersWhateverItsAlignment()
    {
        AngleStyle[] angleStyles = { AngleStyle.RAW, AngleStyle.ROUNDED1, AngleStyle.ROUNDED2, AngleStyle.ROUNDED3 };
        int[] justifications = { MultiLineString.Justification.LEFT, MultiLineString.Justification.CENTER,
                MultiLineString.Justification.RIGHT };
        for (AngleStyle angleStyle : angleStyles)
        {
            for (int justification : justifications)
            {
                for (String text : new String[] { "Back office", "A rather long title for a small area" })
                {
                    for (int borderWidth : new int[] { 1, 6 })
                    {
                        ContainerNode container = new ContainerNode();
                        container.setPreferredSize(new Rectangle2D.Double(0, 0, 160, 100));
                        container.setAngleStyle(angleStyle);
                        container.setBorderWidth(borderWidth);
                        container.getTitle().setText(text);
                        container.getTitle().setJustification(justification);
                        new ClassDiagramGraph().addNode(container, new Point2D.Double(0, 0));
                        Graphics2D g2 = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
                        container.draw(g2);
                        g2.dispose();

                        Rectangle2D lines = container.getTitle().getDrawnTextArea();
                        double textWidth = container.getTitle().getBounds().getWidth();
                        double textX = lines.getX();
                        if (justification == MultiLineString.Justification.CENTER)
                        {
                            textX = lines.getCenterX() - textWidth / 2;
                        }
                        else if (justification == MultiLineString.Justification.RIGHT)
                        {
                            textX = lines.getMaxX() - textWidth;
                        }
                        // With half the border on each side of the line
                        double half = borderWidth / 2.0;
                        Rectangle2D textWithBorder = new Rectangle2D.Double(textX - half, lines.getY() - half, textWidth
                                + borderWidth, lines.getHeight() + borderWidth);
                        String message = angleStyle.getRadius() + " " + justification + " " + text + " " + borderWidth;
                        assertTrue(container.getShape().contains(textWithBorder), message + " " + textWithBorder + " in "
                                + container.getBounds());
                    }
                }
            }
        }
    }

    @Test
    void containerIsNeverNarrowerThanItsTitle()
    {
        ContainerNode container = new ContainerNode();
        container.setPreferredSize(new Rectangle2D.Double(0, 0, 40, 40));
        container.getTitle().setText("A rather long title for a small area");

        assertTrue(container.getBounds().getWidth() >= container.getTitle().getBoundsWithMargins().getWidth());
    }

    @Test
    void poolSavesNoTitle() throws IOException
    {
        IGraph graph = new ProcessDiagramGraph();
        ProcessPoolNode pool = new ProcessPoolNode();
        pool.getName().setText("Customer");
        graph.addNode(pool.clone(), new Point2D.Double(0, 0));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new XMLPersistenceService().write(graph, out);

        assertFalse(out.toString(StandardCharsets.UTF_8).contains("<title"), out.toString(StandardCharsets.UTF_8));
    }

    private ContainerNode reload(ContainerNode container) throws IOException
    {
        IGraph graph = new ClassDiagramGraph();
        graph.addNode(container, new Point2D.Double(20, 30));
        IFilePersistenceService persistenceService = new XMLPersistenceService();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persistenceService.write(graph, out);
        IGraph reloaded = persistenceService.read(new ByteArrayInputStream(out.toByteArray()));
        return (ContainerNode) reloaded.getAllNodes().iterator().next();
    }
}
