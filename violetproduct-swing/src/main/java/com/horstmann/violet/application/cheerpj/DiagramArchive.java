package com.horstmann.violet.application.cheerpj;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Zip archive of the diagrams kept in the browser storage: written by "Export to > This device > Archive file", read
 * back by "Import from > This device > Archive file". The diagram files are stored as they are, at the root of the
 * archive.
 */
public class DiagramArchive {

    public static final String EXTENSION = ".zip";

    /**
     * A diagram read from an archive
     */
    public record Entry(String filename, byte[] content) {
    }

    /**
     * Writes the given files into a zip archive, then closes the stream
     */
    public void write(List<File> files, OutputStream out) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            for (File aFile : files) {
                ZipEntry entry = new ZipEntry(aFile.getName());
                entry.setTime(aFile.lastModified());
                zip.putNextEntry(entry);
                try (InputStream in = new FileInputStream(aFile)) {
                    in.transferTo(zip);
                }
                zip.closeEntry();
            }
        }
    }

    /**
     * Reads the diagrams of a zip archive, then closes the stream. Folders are ignored (an archive made by hand may hold
     * its files in one), so is anything else than a diagram, and the first file of a name wins.
     *
     * @param isDiagram tells the file names of the diagrams
     * @return the diagrams, in the order of the archive
     */
    public List<Entry> read(InputStream in, Predicate<String> isDiagram) throws IOException {
        Map<String, Entry> diagrams = new LinkedHashMap<String, Entry>();
        try (ZipInputStream zip = new ZipInputStream(in, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String filename = getFilename(entry);
                if (filename == null || !isDiagram.test(filename) || diagrams.containsKey(filename)) {
                    continue;
                }
                diagrams.put(filename, new Entry(filename, zip.readAllBytes()));
            }
        }
        return new ArrayList<Entry>(diagrams.values());
    }

    /**
     * @return the name of the entry without its folders, or null for a folder or a file of macOS metadata ("._name",
     * which the Finder adds to the archives it makes)
     */
    private String getFilename(ZipEntry entry) {
        if (entry.isDirectory()) {
            return null;
        }
        String path = entry.getName().replace('\\', '/');
        String filename = path.substring(path.lastIndexOf('/') + 1).trim();
        if (filename.isEmpty() || filename.startsWith("._")) {
            return null;
        }
        return filename;
    }
}
