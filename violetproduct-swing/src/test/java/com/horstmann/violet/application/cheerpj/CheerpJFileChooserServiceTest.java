package com.horstmann.violet.application.cheerpj;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.file.naming.ExtensionFilter;

class CheerpJFileChooserServiceTest {

    private static final ExtensionFilter PNG = new ExtensionFilter("PNG", ".png");

    @Test
    void shouldDownloadUnderTheSuggestedName() throws IOException {
        String filename = new CheerpJFileChooserService().chooseAndGetFileWriter("orders.png", PNG)
                .getFileDefinition().getFilename();

        assertEquals("orders.png", filename);
    }

    @Test
    void shouldFallBackToDefaultNameWithoutSuggestion() throws IOException {
        String filename = new CheerpJFileChooserService().chooseAndGetFileWriter(PNG)
                .getFileDefinition().getFilename();

        assertEquals("diagram.png", filename);
    }
}
