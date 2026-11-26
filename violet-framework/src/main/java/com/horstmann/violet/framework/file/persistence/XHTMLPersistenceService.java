package com.horstmann.violet.framework.file.persistence;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64OutputStream;

import com.horstmann.violet.framework.file.export.FileExportService;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.InjectedBean;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.ManagedBean;
import com.horstmann.violet.framework.util.VersionChecker;
import com.horstmann.violet.product.diagram.abstracts.IGraph;

@ManagedBean
public class XHTMLPersistenceService implements IFilePersistenceService
{

    private static final String TEMPLATE_FILE = "XHTMLFileTemplate.violet.html";

    private static final String IMAGE_TYPE = "png";

    private static final String HTML_INLINE_IMAGE_PREFIX = "data:image/png;base64,";

    private static final String TEMPLATE_VERSION_KEY = "${version}";
    
    private static final String TEMPLATE_IMAGE_KEY = "${image}";

    /** Image shown when the page is in dark mode (its theme switch or the system's) */
    private static final String TEMPLATE_DARK_IMAGE_KEY = "${imageDark}";

    /** Size the page gives to the images, in CSS pixels = graph units, whatever their number of pixels */
    private static final String TEMPLATE_IMAGE_WIDTH_KEY = "${imageWidth}";

    private static final String TEMPLATE_IMAGE_HEIGHT_KEY = "${imageHeight}";

    private static final String TEMPLATE_XMLCONTENT_KEY = "${content}";

    /**
     * Image pixels per graph unit: sharp on high density screens and up to a 200 % zoom of the page, and the browser shrinks a
     * larger image better than it enlarges a smaller one
     */
    private static final double IMAGE_SCALE = 2;

    /**
     * Above this number of pixels per image, the scale goes down (not below 1): beyond, the PNG takes long to write at each save,
     * makes the file heavy and some browsers (phones) refuse to decode it
     */
    private static final double MAX_IMAGE_PIXELS = 16_000_000;

    /** Largest width or height of an image that browsers decode everywhere */
    private static final int MAX_IMAGE_SIDE = 16_000;

    private IFilePersistenceService graphPersistenceService = new XMLPersistenceService();

    @InjectedBean
    private VersionChecker versionChecker;

    /** False for the files only Violet reads back (browser storage): rendering the images would cost time and space for nothing */
    private final boolean isWithImages;

    /**
     * Writes the light and dark images of the diagram, for files people open in a web browser
     */
    public XHTMLPersistenceService()
    {
        this(true);
    }

    /**
     * @param isWithImages false to leave the images out, for files only Violet reads back (the page then shows no diagram)
     */
    public XHTMLPersistenceService(boolean isWithImages)
    {
        this.isWithImages = isWithImages;
    }

    @Override
    public void write(IGraph graph, OutputStream out)
    {
        try
        {
            OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        	InputStream templateAsStream = this.getClass().getResourceAsStream(TEMPLATE_FILE);
            String template = getInputStreamContent(templateAsStream);
            ByteArrayOutputStream graphOutputStream = new ByteArrayOutputStream();
            graphPersistenceService.write(graph, graphOutputStream);
            String graphString = graphOutputStream.toString(StandardCharsets.UTF_8);
            template = template.replace(TEMPLATE_VERSION_KEY, this.versionChecker.getAppVersionNumber());
            Dimension imageSize = FileExportService.getImageSize(graph);
            double scale = getImageScale(imageSize);
            template = template.replace(TEMPLATE_IMAGE_KEY, this.isWithImages ? getInlineImage(FileExportService.getImage(graph, scale)) : "");
            template = template.replace(TEMPLATE_DARK_IMAGE_KEY, this.isWithImages ? getInlineImage(FileExportService.getDarkImage(graph, scale)) : "");
            template = template.replace(TEMPLATE_IMAGE_WIDTH_KEY, this.isWithImages ? String.valueOf(imageSize.width) : "");
            template = template.replace(TEMPLATE_IMAGE_HEIGHT_KEY, this.isWithImages ? String.valueOf(imageSize.height) : "");
            // Last, so that a text of the diagram looking like a key is kept as typed
            template = template.replace(TEMPLATE_XMLCONTENT_KEY, graphString);
            writer.write(template);
            templateAsStream.close();
            writer.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IGraph read(InputStream in) throws IOException
    {
        String htmlContent = getInputStreamContent(in);
        String xmlContent = extractXmlContent(htmlContent);
        InputStream xmlContentStream = new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8));
        IGraph graph = this.graphPersistenceService.read(xmlContentStream);
        xmlContentStream.close();
        return graph;
    }

    /**
     * @param imageSize size of the diagram's image at scale 1
     * @return the number of image pixels per graph unit, {@link #IMAGE_SCALE} unless the image would get too large
     */
    private double getImageScale(Dimension imageSize)
    {
        double scale = IMAGE_SCALE;
        scale = Math.min(scale, Math.sqrt(MAX_IMAGE_PIXELS / ((double) imageSize.width * imageSize.height)));
        scale = Math.min(scale, (double) MAX_IMAGE_SIDE / Math.max(imageSize.width, imageSize.height));
        return Math.max(1, scale);
    }

    /**
     * @param image
     * @return the image as a PNG data URI, to put in an img src attribute
     * @throws IOException
     */
    private String getInlineImage(BufferedImage image) throws IOException
    {
        ByteArrayOutputStream imageOutputStream = new ByteArrayOutputStream();
        try (Base64OutputStream base64ImageOutputStream = new Base64OutputStream(imageOutputStream))
        {
            ImageIO.write(image, IMAGE_TYPE, base64ImageOutputStream);
        }
        return HTML_INLINE_IMAGE_PREFIX + imageOutputStream.toString(StandardCharsets.UTF_8);
    }

    private static final Pattern CONTENT_CDATA_PATTERN = Pattern.compile(
            "<script[^>]+id=[\"']content[\"'][^>]*>\\s*<!\\[CDATA\\[(.*?)]]>\\s*</script>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private String extractXmlContent(String html) throws IOException
    {
        Matcher matcher = CONTENT_CDATA_PATTERN.matcher(html);
        if (!matcher.find())
        {
            throw new IOException("Invalid XHTML file format: missing CDATA section in <SCRIPT id=\"content\">");
        }
        return matcher.group(1).trim();
    }

    private String getInputStreamContent(InputStream in) throws IOException
    {
        BufferedInputStream bis = new BufferedInputStream(in);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int result = bis.read();
        while (result != -1)
        {
            byte b = (byte) result;
            buf.write(b);
            result = bis.read();
        }
        String content = buf.toString(StandardCharsets.UTF_8);
        bis.close();
        buf.close();
        in.close();
        return content;
    }

    

}
