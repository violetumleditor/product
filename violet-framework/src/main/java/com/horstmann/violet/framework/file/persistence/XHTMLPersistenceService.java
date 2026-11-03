package com.horstmann.violet.framework.file.persistence;

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

    private static final String TEMPLATE_XMLCONTENT_KEY = "${content}";

    private IFilePersistenceService graphPersistenceService = new XMLPersistenceService();

    @InjectedBean
    private VersionChecker versionChecker;
    
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
            ByteArrayOutputStream imageOutputStream = new ByteArrayOutputStream();
            Base64OutputStream base64ImageOutputStream = new Base64OutputStream(imageOutputStream);
            ImageIO.write(FileExportService.getImage(graph), IMAGE_TYPE, base64ImageOutputStream);
            String imageString = HTML_INLINE_IMAGE_PREFIX + imageOutputStream.toString(StandardCharsets.UTF_8);
            template = template.replace(TEMPLATE_VERSION_KEY, this.versionChecker.getAppVersionNumber());
            template = template.replace(TEMPLATE_XMLCONTENT_KEY, graphString);
            template = template.replace(TEMPLATE_IMAGE_KEY, imageString);
            writer.write(template);
            templateAsStream.close();
            imageOutputStream.close();
            base64ImageOutputStream.close();
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
