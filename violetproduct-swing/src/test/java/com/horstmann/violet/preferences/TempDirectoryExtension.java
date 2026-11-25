package com.horstmann.violet.preferences;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * The diagrams without a name are saved in the temporary directory: gives the tests one of their own, deleted when they end.
 * Registered for all tests through META-INF/services and junit-platform.properties.
 */
public class TempDirectoryExtension implements BeforeAllCallback
{
    @Override
    public void beforeAll(ExtensionContext context)
    {
        context.getRoot().getStore(ExtensionContext.Namespace.create(TempDirectoryExtension.class))
                .getOrComputeIfAbsent(TestDirectory.class);
    }

    static class TestDirectory
    {
        TestDirectory() throws IOException
        {
            this.directory = Files.createTempDirectory("violet-tests");
            System.setProperty("java.io.tmpdir", this.directory.toString());
            // Not before: diagrams are still saved when the JVM stops
            Runtime.getRuntime().addShutdownHook(new Thread(this::delete));
        }

        private void delete()
        {
            try
            {
                Thread.sleep(AUTO_SAVE_DELAY);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            try (Stream<Path> paths = Files.walk(this.directory))
            {
                paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
            catch (IOException e)
            {
                // Left in the temporary directory of the system
            }
        }

        /** The diagrams are saved by another shutdown hook, at the same time; in ms */
        private static final int AUTO_SAVE_DELAY = 1000;

        private final Path directory;
    }
}
