package com.horstmann.violet.application.cheerpj;

import java.io.BufferedInputStream;
import java.io.EOFException;
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
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Zip archive of the diagrams kept in the browser storage: written and read back by the export and import buttons of
 * "File > Manage files". The diagram files are stored as they are, at the root of the archive.
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
     * Why an archive can't be read
     */
    public enum Problem {
        /** Not a zip file at all */
        NOT_AN_ARCHIVE,
        /** A zip file, but truncated or corrupted */
        DAMAGED,
        /** A zip file whose entries are encrypted with a password, which java.util.zip can't read */
        ENCRYPTED
    }

    /**
     * Thrown when the file given as an archive can't be read, for one of the {@link Problem}s
     */
    public static class UnreadableArchiveException extends IOException {

        private final Problem problem;

        public UnreadableArchiveException(Problem problem, Throwable cause) {
            super(problem.name(), cause);
            this.problem = problem;
        }

        public Problem getProblem() {
            return this.problem;
        }
    }

    /**
     * Reads the diagrams of a zip archive, then closes the stream. Folders are ignored (an archive made by hand may hold
     * its files in one), so is anything else than a diagram, and the first file of a name wins.
     *
     * @param isDiagram tells the file names of the diagrams
     * @return the diagrams, in the order of the archive (none for an empty archive)
     * @throws UnreadableArchiveException if it isn't a zip file, or a damaged one, or one protected by a password
     */
    public List<Entry> read(InputStream in, Predicate<String> isDiagram) throws IOException {
        Map<String, Entry> diagrams = new LinkedHashMap<String, Entry>();
        BufferedInputStream buffered = new BufferedInputStream(in);
        try (ZipInputStream zip = new ZipInputStream(buffered, StandardCharsets.UTF_8)) {
            // ZipInputStream sees no entry at all in a file which isn't a zip: tell it by its signature
            buffered.mark(4);
            byte[] signature = buffered.readNBytes(4);
            buffered.reset();
            if (!isZipSignature(signature)) {
                throw new UnreadableArchiveException(Problem.NOT_AN_ARCHIVE, null);
            }
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String filename = getFilename(entry);
                if (filename == null || !isDiagram.test(filename) || diagrams.containsKey(filename)) {
                    continue;
                }
                diagrams.put(filename, new Entry(filename, zip.readAllBytes()));
            }
        } catch (ZipException e) {
            // Message of java.util.zip.ZipInputStream for an entry whose "encrypted" flag is set
            boolean isEncrypted = e.getMessage() != null && e.getMessage().contains("encrypted");
            throw new UnreadableArchiveException(isEncrypted ? Problem.ENCRYPTED : Problem.DAMAGED, e);
        } catch (EOFException | IllegalArgumentException e) {
            // Truncated file, or entry name which isn't valid in its charset
            throw new UnreadableArchiveException(Problem.DAMAGED, e);
        }
        return new ArrayList<Entry>(diagrams.values());
    }

    /**
     * @return true for the signature of a local file header ("PK" 3 4), of an empty archive ("PK" 5 6) or of a spanned
     * archive ("PK" 7 8)
     */
    private boolean isZipSignature(byte[] signature) {
        if (signature.length < 4 || signature[0] != 'P' || signature[1] != 'K') {
            return false;
        }
        return (signature[2] == 3 && signature[3] == 4) || (signature[2] == 5 && signature[3] == 6)
                || (signature[2] == 7 && signature[3] == 8);
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
