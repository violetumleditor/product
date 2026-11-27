package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.horstmann.violet.framework.file.GraphFile;
import com.horstmann.violet.framework.file.chooser.IFileChooserService;
import com.horstmann.violet.framework.file.chooser.JFileChooserService;
import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.XHTMLPersistenceService;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanFactory;
import com.horstmann.violet.framework.userpreferences.DefaultUserPreferencesDao;
import com.horstmann.violet.framework.userpreferences.IUserPreferencesDao;
import com.horstmann.violet.framework.userpreferences.PreferredFile;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;

/**
 * At startup, the diagrams of the former session that are not displayed are opened by {@link GraphFile#openLater}: their file is
 * read only once their graph is needed.
 */
class GraphFileOpenLaterTest
{

    private static final String ORDERS = "orders.class.violet.html";

    @TempDir
    Path directory;

    @BeforeAll
    static void registerBeans()
    {
        if (BeanFactory.getFactory().contains(IFileChooserService.class))
        {
            return;
        }
        BeanFactory.getFactory().register(IUserPreferencesDao.class, new DefaultUserPreferencesDao());
        BeanFactory.getFactory().register(IFileChooserService.class, new JFileChooserService());
        BeanFactory.getFactory().register(IFilePersistenceService.class, new XHTMLPersistenceService());
    }

    @Test
    void shouldReadTheFileOnlyWhenItsGraphIsNeeded() throws Exception
    {
        writeDiagramWithOneClass(ORDERS);

        GraphFile graphFile = GraphFile.openLater(new PreferredFile(this.directory.toString(), ORDERS));

        assertFalse(graphFile.isLoaded());
        assertEquals(ORDERS, graphFile.getFilename());
        assertEquals(this.directory.toString(), graphFile.getDirectory());
        assertFalse(graphFile.isSaveRequired());
        assertEquals(1, graphFile.getGraph().getAllNodes().size());
        assertTrue(graphFile.isLoaded());
    }

    @Test
    void shouldReadTheFileWhenLoaded() throws Exception
    {
        writeDiagramWithOneClass(ORDERS);
        GraphFile graphFile = GraphFile.openLater(new PreferredFile(this.directory.toString(), ORDERS));

        graphFile.load();

        assertTrue(graphFile.isLoaded());
        assertEquals(1, graphFile.getGraph().getAllNodes().size());
    }

    @Test
    void shouldSaveNothingBeforeBeingRead() throws Exception
    {
        writeDiagramWithOneClass(ORDERS);
        byte[] before = Files.readAllBytes(this.directory.resolve(ORDERS));
        GraphFile graphFile = GraphFile.openLater(new PreferredFile(this.directory.toString(), ORDERS));

        graphFile.save(false);

        assertFalse(graphFile.isLoaded());
        assertEquals(new String(before), new String(Files.readAllBytes(this.directory.resolve(ORDERS))));
    }

    @Test
    void shouldTellWhenTheFileCantBeRead()
    {
        GraphFile graphFile = GraphFile.openLater(new PreferredFile(this.directory.toString(), "missing.class.violet.html"));

        assertThrows(IOException.class, graphFile::load);
        assertThrows(UncheckedIOException.class, graphFile::getGraph);
        assertFalse(graphFile.isLoaded());
    }

    private void writeDiagramWithOneClass(String filename) throws Exception
    {
        ClassDiagramGraph graph = new ClassDiagramGraph();
        graph.addNode(new ClassNode(), new Point2D.Double(40, 40));
        IFilePersistenceService persistenceService = BeanFactory.getFactory().getBean(IFilePersistenceService.class);
        try (OutputStream out = Files.newOutputStream(this.directory.resolve(filename)))
        {
            persistenceService.write(graph, out);
        }
    }
}
