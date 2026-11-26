/*
 Violet - A program for editing UML diagrams.

 Copyright (C) 2007 Cay S. Horstmann (http://horstmann.com)
 Alexandre de Pellegrin (http://alexdp.free.fr);

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.horstmann.violet.framework.file.export;

import com.horstmann.violet.framework.theme.DarkDiagramGraphics2D;
import com.horstmann.violet.framework.util.ClipboardPipe;
import com.horstmann.violet.framework.util.PDFGraphics2DStringWriter;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import org.freehep.graphicsbase.util.UserProperties;
import org.freehep.graphicsio.pdf.PDFGraphics2D;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.OutputStream;

public class FileExportService
{
    /**
     * Return the image correspondiojng to the graph
     *
     * @param graph
     * @return bufferedImage. To convert it into an image, use the syntax :
     * Toolkit.getDefaultToolkit().createImage(bufferedImage.getSource());
     * @author Alexandre de Pellegrin
     */
    public static BufferedImage getImage(IGraph graph)
    {
        return getImage(graph, 1);
    }

    /**
     * @param graph
     * @param scale number of image pixels per graph unit (2 = twice as sharp, for high density screens and zooming in)
     * @return the image of the graph, of the size of {@link #getImageSize(IGraph)} multiplied by the scale
     */
    public static BufferedImage getImage(IGraph graph, double scale)
    {
        BufferedImage image = createImage(graph, scale);
        renderIGraphToGraphics2D(graph, (Graphics2D) image.getGraphics(), scale);
        return image;
    }

    /**
     * @param graph
     * @return the image of the graph as the dark theme shows it on screen (lightness mirrored, hues and pasted images
     * kept, see DarkDiagramGraphics2D), for the dark mode of the saved HTML files
     */
    public static BufferedImage getDarkImage(IGraph graph)
    {
        return getDarkImage(graph, 1);
    }

    /**
     * @param graph
     * @param scale number of image pixels per graph unit
     * @return the dark image of the graph (see {@link #getDarkImage(IGraph)}) at this scale
     */
    public static BufferedImage getDarkImage(IGraph graph, double scale)
    {
        BufferedImage image = createImage(graph, scale);
        renderIGraphToGraphics2D(graph, new DarkDiagramGraphics2D((Graphics2D) image.getGraphics()), scale);
        return image;
    }

    /**
     * @param graph
     * @return the size of the image of the graph at scale 1, in graph units
     */
    public static Dimension getImageSize(IGraph graph)
    {
        Rectangle2D bounds = graph.getClipBounds();
        return new Dimension((int) bounds.getWidth() + 1, (int) bounds.getHeight() + 1);
    }

    private static BufferedImage createImage(IGraph graph, double scale)
    {
        Dimension size = getImageSize(graph);
        return new BufferedImage((int) Math.ceil(size.width * scale), (int) Math.ceil(size.height * scale), BufferedImage.TYPE_INT_RGB);
    }

    /**
     * Export graph to clipboard (Do not merge with exportToClipBoard(). Used in Eclipse plugin)
     *
     * @param graph
     * @author Alexandre de Pellegrin
     */
    public static void exportToclipBoard(IGraph graph)
    {
        BufferedImage bImage = getImage(graph);
        ClipboardPipe pipe = new ClipboardPipe(bImage);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(pipe, null);
    }

    /**
     * Export graph to PDF file
     *
     * @param graph
     * @param out   output stream to file
     * @author Michał Leśniak
     */
    public static void exportToPdf(IGraph graph, OutputStream out)
    {
        Rectangle2D bounds = graph.getClipBounds();

        UserProperties p = new UserProperties();
        p.setProperty(PDFGraphics2D.PAGE_SIZE, PDFGraphics2D.CUSTOM_PAGE_SIZE);
        p.setProperty(PDFGraphics2D.CUSTOM_PAGE_SIZE, new Dimension((int) bounds.getWidth(), (int) bounds.getHeight()));
        p.setProperty(PDFGraphics2D.VERSION, PDFGraphics2D.VERSION6);
        p.setProperty(PDFGraphics2D.FIT_TO_PAGE, "false");
        p.setProperty(PDFGraphics2D.EMBED_FONTS, "true");

        PDFGraphics2D g = new PDFGraphics2DStringWriter(out, bounds.getBounds().getSize());
        g.setProperties(p);
        g.startExport();

        renderIGraphToGraphics2D(graph, g, 1);

        g.endExport();
    }

    private static Graphics2D renderIGraphToGraphics2D(IGraph graph, Graphics2D g2, double scale)
    {
        Rectangle2D bounds = graph.getClipBounds();

        g2.scale(scale, scale);
        g2.translate(-bounds.getX(), -bounds.getY());
        g2.setColor(Color.WHITE);
        g2.fill(new Rectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth() + 1, bounds.getHeight() + 1));

        g2.setColor(Color.BLACK);
        g2.setBackground(Color.WHITE);

        graph.draw(g2);

        return g2;
    }

    /**
     * Auteur : a.depellegrin<br>
     * Definition : Exports class diagram graph to xmi <br>
     *
     * @param graph to export
     * @param out   to write result
     */
    public static void exportToXMI(IGraph graph, OutputStream out)
    {
        // if (!(graph instanceof ClassDiagramGraph))
        // {
        // // Only exports class diagrams
        // return;
        // }
        // try
        // {
        // // Gets xsl files
        // ResourceBundle fileResourceBundle = ResourceBundle.getBundle(ResourceBundleConstant.FILE_STRINGS, Locale.getDefault());
        // URL xslResource1 = FileExportService.class.getResource(fileResourceBundle.getString("files.xmi.step1.xsl"));
        // URL xslResource2 = FileExportService.class.getResource(fileResourceBundle.getString("files.xmi.step2.xsl"));
        // // Converts graph to Violet's XML
        // ByteArrayOutputStream graphOut = new ByteArrayOutputStream();
        // FileExportService.write(graph, graphOut);
        // ByteArrayInputStream graphIn = new ByteArrayInputStream(graphOut.toByteArray());
        // // XSL transform - step 1
        // ByteArrayOutputStream xmiOut = new ByteArrayOutputStream();
        // InputStream xslResource1InputStream = xslResource1.openStream();
        // TransformerFactory factory = TransformerFactory.newInstance();
        // Transformer transformer = factory.newTransformer(new StreamSource(xslResource1InputStream));
        // transformer.transform(new StreamSource(graphIn), new StreamResult(xmiOut));
        // // XSL transform - step 2
        // ByteArrayInputStream xmiIn = new ByteArrayInputStream(xmiOut.toByteArray());
        // InputStream xslResource2InputStream = xslResource2.openStream();
        // transformer = factory.newTransformer(new StreamSource(xslResource2InputStream));
        // transformer.transform(new StreamSource(xmiIn), new StreamResult(out));
        // // Closes unused streams
        // xslResource1InputStream.close();
        // xslResource2InputStream.close();
        // graphOut.close();
        // graphIn.close();
        // xmiOut.close();
        // xmiIn.close();
        // }
        // catch (Exception e)
        // {
        // // Well... we tried!
        // }
    }

}
