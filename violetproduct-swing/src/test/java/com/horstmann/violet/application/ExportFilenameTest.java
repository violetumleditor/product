package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.file.naming.ExtensionFilter;
import com.horstmann.violet.framework.file.naming.FileNamingService;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanFactory;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.framework.plugin.PluginRegistry;
import com.horstmann.violet.product.diagram.classes.ClassDiagramPlugin;

class ExportFilenameTest
{
    private static final ExtensionFilter PNG = new ExtensionFilter("PNG", ".png");

    private FileNamingService fileNamingService;

    @BeforeEach
    void setUp()
    {
        BeanFactory.getFactory().getBean(PluginRegistry.class).register(new ClassDiagramPlugin());
        this.fileNamingService = new FileNamingService();
        BeanInjector.getInjector().inject(this.fileNamingService);
    }

    @Test
    void shouldReplaceDiagramSpecificExtension()
    {
        assertEquals("orders.png", this.fileNamingService.getExportFilename("orders.class.violet.html", PNG));
    }

    @Test
    void shouldReplaceGenericExtension()
    {
        assertEquals("orders.png", this.fileNamingService.getExportFilename("orders.violet.html", PNG));
    }

    @Test
    void shouldKeepDotsOfTheName()
    {
        assertEquals("orders.v2.png", this.fileNamingService.getExportFilename("orders.v2.class.violet.html", PNG));
    }

    @Test
    void shouldAppendExtensionToUnknownName()
    {
        assertEquals("orders.png", this.fileNamingService.getExportFilename("orders", PNG));
    }

    @Test
    void shouldReturnNullWithoutName()
    {
        assertNull(this.fileNamingService.getExportFilename(null, PNG));
    }
}
