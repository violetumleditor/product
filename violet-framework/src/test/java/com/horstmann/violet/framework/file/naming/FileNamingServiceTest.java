package com.horstmann.violet.framework.file.naming;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
