package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.horstmann.violet.framework.file.GraphFile;
import com.horstmann.violet.framework.file.IFile;
import com.horstmann.violet.framework.file.IGraphFileListener;
import com.horstmann.violet.framework.file.chooser.IFileChooserService;
import com.horstmann.violet.framework.file.chooser.JFileChooserService;
import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.XHTMLPersistenceService;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanFactory;
import com.horstmann.violet.framework.userpreferences.DefaultUserPreferencesDao;
import com.horstmann.violet.framework.userpreferences.IUserPreferencesDao;
import com.horstmann.violet.framework.userpreferences.PreferencesConstant;
import com.horstmann.violet.framework.userpreferences.PreferredFile;
import com.horstmann.violet.framework.userpreferences.UserPreferencesService;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.workspace.editorpart.behavior.AutoSaveBehavior;

class GraphFileRenameTest
{

    private static final String ORDERS = "orders.class.violet.html";

    private static final String CUSTOMERS = "customers.class.violet.html";

    @TempDir
    Path directory;

    private UserPreferencesService preferences;

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

    @BeforeEach
    void useInMemoryPreferences() throws Exception
    {
        this.preferences = new UserPreferencesService();
        setField(UserPreferencesService.class, this.preferences, "dao", new InMemoryPreferencesDao());
    }

    @Test
    void shouldMoveTheFileAndTellListeners() throws Exception
    {
        GraphFile graphFile = openDiagram(ORDERS);
        boolean[] isSaved = { false };
        graphFile.addListener(new IGraphFileListener()
        {
            @Override
            public void onFileModified()
            {
            }

            @Override
            public void onFileSaved()
            {
                isSaved[0] = graphFile.getFilename().equals(CUSTOMERS);
            }
        });

        graphFile.rename(CUSTOMERS);

        assertEquals(CUSTOMERS, graphFile.getFilename());
        assertEquals(List.of(CUSTOMERS), storedFilenames());
        assertTrue(isSaved[0]);
    }

    @Test
    void shouldForgetTheOldNameInPreferences() throws Exception
    {
        GraphFile graphFile = openDiagram(ORDERS);
        IFile oldFile = new PreferredFile(this.directory.toString(), ORDERS);
        this.preferences.addRecentFile(oldFile);
        this.preferences.addOpenedFile(oldFile);

        graphFile.rename(CUSTOMERS);

        assertTrue(this.preferences.getRecentFiles().isEmpty());
        assertTrue(this.preferences.getOpenedFilesDuringLastSession().isEmpty());
    }

    @Test
    void shouldReplaceTheOldNameByTheNewOneInPreferences() throws Exception
    {
        GraphFile graphFile = openDiagram(ORDERS);
        AutoSaveBehavior autoSaveBehavior = new AutoSaveBehavior(graphFile);
        setField(AutoSaveBehavior.class, autoSaveBehavior, "userPreferencesService", this.preferences);
        IFile oldFile = new PreferredFile(this.directory.toString(), ORDERS);
        IFile otherFile = new PreferredFile(this.directory.toString(), "other.class.violet.html");
        this.preferences.addRecentFile(oldFile);
        this.preferences.addRecentFile(otherFile);
        this.preferences.addOpenedFile(oldFile);

        graphFile.rename(CUSTOMERS);

        IFile newFile = new PreferredFile(this.directory.toString(), CUSTOMERS);
        assertEquals(List.of(newFile, otherFile), this.preferences.getRecentFiles());
        assertEquals(List.of(newFile), this.preferences.getOpenedFilesDuringLastSession());
    }

    @Test
    void shouldNotOverwriteAnotherFile() throws Exception
    {
        GraphFile graphFile = openDiagram(ORDERS);
        byte[] otherContent = { 1, 2, 3 };
        Files.write(this.directory.resolve(CUSTOMERS), otherContent);

        assertThrows(FileAlreadyExistsException.class, () -> graphFile.rename(CUSTOMERS));

        assertEquals(ORDERS, graphFile.getFilename());
        assertEquals(List.of(CUSTOMERS, ORDERS), storedFilenames());
        assertArrayEquals(otherContent, Files.readAllBytes(this.directory.resolve(CUSTOMERS)));
    }

    @Test
    void shouldKeepTheFileWhenOnlyTheCaseChanges() throws Exception
    {
        GraphFile graphFile = openDiagram(ORDERS);

        graphFile.rename("Orders.class.violet.html");

        assertEquals(List.of("Orders.class.violet.html"), storedFilenames());
        assertTrue(Files.size(this.directory.resolve("Orders.class.violet.html")) > 0);
    }

    @Test
    void shouldWritePendingChanges() throws Exception
    {
        GraphFile graphFile = openDiagram(ORDERS);
        graphFile.getGraph().addNode(new ClassNode(), new Point2D.Double(10, 10));
        graphFile.setSaveRequired();

        graphFile.rename(CUSTOMERS);

        assertFalse(graphFile.isSaveRequired());
        GraphFile reloaded = new GraphFile(new PreferredFile(this.directory.toString(), CUSTOMERS));
        assertEquals(1, reloaded.getGraph().getAllNodes().size());
    }

    @Test
    void shouldGiveARealNameToADiagramNeverWritten() throws Exception
    {
        GraphFile graphFile = new GraphFile(ClassDiagramGraph.class);
        assertTrue(graphFile.isTemporaryFilename());
        String name = "rename-test-" + System.nanoTime() + ".class.violet.html";

        try
        {
            graphFile.rename(name);

            assertFalse(graphFile.isTemporaryFilename());
            assertTrue(Files.exists(Path.of(graphFile.getDirectory(), name)));
        }
        finally
        {
            graphFile.delete();
        }
    }

    private GraphFile openDiagram(String filename) throws Exception
    {
        IFilePersistenceService persistenceService = BeanFactory.getFactory().getBean(IFilePersistenceService.class);
        try (OutputStream out = Files.newOutputStream(this.directory.resolve(filename)))
        {
            persistenceService.write(new ClassDiagramGraph(), out);
        }
        GraphFile graphFile = new GraphFile(new PreferredFile(this.directory.toString(), filename));
        setField(GraphFile.class, graphFile, "userPreferencesService", this.preferences);
        return graphFile;
    }

    private List<String> storedFilenames() throws IOException
    {
        try (Stream<Path> files = Files.list(this.directory))
        {
            return files.map(file -> file.getFileName().toString()).sorted().collect(Collectors.toList());
        }
    }

    private static void setField(Class<?> type, Object target, String name, Object value) throws Exception
    {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class InMemoryPreferencesDao implements IUserPreferencesDao
    {
        @Override
        public String get(PreferencesConstant key, String defval)
        {
            return this.values.getOrDefault(key.toString(), defval);
        }

        @Override
        public void put(PreferencesConstant key, String value)
        {
            this.values.put(key.toString(), value);
        }

        @Override
        public void reset()
        {
            this.values.clear();
        }

        private final Map<String, String> values = new HashMap<>();
    }
}
