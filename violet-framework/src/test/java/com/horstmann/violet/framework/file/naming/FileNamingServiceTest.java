package com.horstmann.violet.framework.file.naming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FileNamingServiceTest
{

    private static final ExtensionFilter CLASS_DIAGRAM = new ExtensionFilter("Class diagram", ".class.violet.html");

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
    void shouldOnlyLetTheUserEditTheNameBeforeTheDiagramExtension()
    {
        FileNamingService service = new FileNamingService();

        assertEquals("orders", service.getEditableFilename("orders.class.violet.html", CLASS_DIAGRAM));
        assertEquals("orders.v2", service.getEditableFilename("orders.v2.CLASS.violet.html", CLASS_DIAGRAM));
        assertEquals("orders.seq.violet.html", service.getEditableFilename("orders.seq.violet.html", CLASS_DIAGRAM));
    }

    @Test
    void shouldAddTheDiagramExtensionToAnEditedName()
    {
        FileNamingService service = new FileNamingService();

        assertEquals("orders.class.violet.html", service.getFilenameFromEdition("  orders ", CLASS_DIAGRAM));
        assertEquals("orders.class.violet.html", service.getFilenameFromEdition("orders.class.violet.html", CLASS_DIAGRAM));
        assertEquals("my orders.v2.class.violet.html", service.getFilenameFromEdition("my orders.v2", CLASS_DIAGRAM));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "   ", ".class.violet.html", "../orders", "a/b", "a\\b", "c:orders", "a*", "a?", "a\"b", "a<b", "a -> b", "a|b", "a\tb" })
    void shouldRefuseEditedNamesThatAreNotFilenames(String editedName)
    {
        assertNull(new FileNamingService().getFilenameFromEdition(editedName, CLASS_DIAGRAM));
    }

    @Test
    void shouldHaveNoExportFilenameWithoutDiagramName()
    {
        FileNamingService service = new FileNamingService();

        assertNull(service.getExportFilename(null, service.getImageExportExtensionFilter()));
    }
}
