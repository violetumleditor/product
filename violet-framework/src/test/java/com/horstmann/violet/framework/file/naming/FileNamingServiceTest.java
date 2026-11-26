package com.horstmann.violet.framework.file.naming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class FileNamingServiceTest
{

    private static final String BPMN = ".bpmn.violet.html";

    @Test
    void shouldRemoveTheCopyNumberOfABrowser()
    {
        FileNamingService service = new FileNamingService();
        String expected = "Gestion des tickets de support.bpmn.violet.html";

        assertEquals(expected, service.getFilenameWithoutCopyNumber("Gestion des tickets de support.bpmn.violet.html", BPMN));
        assertEquals(expected, service.getFilenameWithoutCopyNumber("Gestion des tickets de support.bpmn.violet (2).html", BPMN));
        assertEquals(expected, service.getFilenameWithoutCopyNumber("Gestion des tickets de support.bpmn.violet(12).html", BPMN));
        assertEquals(expected, service.getFilenameWithoutCopyNumber("Gestion des tickets de support.bpmn (1).violet.html", BPMN));
        assertEquals(expected, service.getFilenameWithoutCopyNumber("Gestion des tickets de support.BPMN.Violet (3).HTML", BPMN));
    }

    @Test
    void shouldKeepANumberInTheDiagramName()
    {
        FileNamingService service = new FileNamingService();

        assertEquals("Version (2).bpmn.violet.html", service.getFilenameWithoutCopyNumber("Version (2).bpmn.violet (3).html", BPMN));
    }

    @Test
    void shouldRefuseAnotherDiagramType()
    {
        FileNamingService service = new FileNamingService();

        assertNull(service.getFilenameWithoutCopyNumber("orders.class.violet (2).html", BPMN));
        assertNull(service.getFilenameWithoutCopyNumber("orders.bpmn.violet (2).htm", BPMN));
        assertNull(service.getFilenameWithoutCopyNumber(".bpmn.violet.html", BPMN));
        assertNull(service.getFilenameWithoutCopyNumber("orders.bpmn.violet (two).html", BPMN));
    }
}
