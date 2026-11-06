package com.horstmann.violet.framework.file.naming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class FileNamingServiceTest
{

    @Test
    void shouldExportImagesAsPngOnly()
    {
        assertEquals(".png", new FileNamingService().getImageExportExtensionFilter().getExtension());
    }

    @Test
    void shouldStillImportJpegAndPngImages()
    {
        List<String> extensions = Arrays.stream(new FileNamingService().getImageExtensionFilters())
                .map(ExtensionFilter::getExtension)
                .collect(Collectors.toList());

        assertEquals(List.of(".jpg", ".png"), extensions);
    }

    @Test
    void shouldOnlyRemoveHtmlFromExportFilename()
    {
        FileNamingService service = new FileNamingService();
        ExtensionFilter png = service.getImageExportExtensionFilter();

        assertEquals("test.activity.violet.png", service.getExportFilename("test.activity.violet.html", png));
        assertEquals("orders.violet.png", service.getExportFilename("orders.violet.html", png));
        assertEquals("orders.png", service.getExportFilename("orders", png));
    }

    @Test
    void shouldHaveNoExportFilenameWithoutDiagramName()
    {
        FileNamingService service = new FileNamingService();

        assertNull(service.getExportFilename(null, service.getImageExportExtensionFilter()));
    }
}
