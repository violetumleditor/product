package com.horstmann.violet.application.cheerpj;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.horstmann.violet.application.menu.MenuFactory;

class DiagramArchiveTest {

    @TempDir
    Path directory;

    @Test
    void shouldReadBackTheDiagramsItWrote() throws IOException {
        Path orders = Files.writeString(directory.resolve("orders.class.violet.html"), "<html>orders</html>");
        Path login = Files.writeString(directory.resolve("login été.seq.violet.html"), "<html>login</html>");
        ByteArrayOutputStream archive = new ByteArrayOutputStream();

        new DiagramArchive().write(List.of(orders.toFile(), login.toFile()), archive);
        List<DiagramArchive.Entry> entries = new DiagramArchive().read(new ByteArrayInputStream(archive.toByteArray()),
                name -> name.endsWith(".violet.html") ? name : null);

        assertEquals(2, entries.size());
        assertEquals("orders.class.violet.html", entries.get(0).filename());
        assertArrayEquals(Files.readAllBytes(orders), entries.get(0).content());
        assertEquals("login été.seq.violet.html", entries.get(1).filename());
        assertArrayEquals(Files.readAllBytes(login), entries.get(1).content());
    }

    @Test
    void shouldKeepOnlyTheDiagramsWithoutTheirFolders() throws IOException {
        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(archive, StandardCharsets.UTF_8)) {
            addEntry(zip, "backup/", null);
            addEntry(zip, "backup/orders.class.violet.html", "first");
            addEntry(zip, "../../evil.class.violet.html", "evil");
            addEntry(zip, "other\\orders.class.violet.html", "second");
            addEntry(zip, "__MACOSX/backup/._orders.class.violet.html", "metadata");
            addEntry(zip, "readme.txt", "text");
        }

        List<DiagramArchive.Entry> entries = new DiagramArchive().read(new ByteArrayInputStream(archive.toByteArray()),
                name -> name.endsWith(".violet.html") ? name : null);

        assertEquals(2, entries.size());
        assertEquals("orders.class.violet.html", entries.get(0).filename());
        assertEquals("first", new String(entries.get(0).content(), StandardCharsets.UTF_8));
        assertEquals("evil.class.violet.html", entries.get(1).filename());
    }

    @Test
    void shouldNameTheDiagramsAsTheyAreStored() throws IOException {
        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(archive, StandardCharsets.UTF_8)) {
            addEntry(zip, "orders.class.violet (2).html", "copy");
            addEntry(zip, "orders.class.violet.html", "original");
        }

        List<DiagramArchive.Entry> entries = new DiagramArchive().read(new ByteArrayInputStream(archive.toByteArray()),
                name -> name.replace(" (2)", ""));

        assertEquals(1, entries.size());
        assertEquals("orders.class.violet.html", entries.get(0).filename());
        assertEquals("copy", new String(entries.get(0).content(), StandardCharsets.UTF_8));
    }

    @Test
    void shouldTellAFileWhichIsNotAZip() {
        assertProblem(DiagramArchive.Problem.NOT_AN_ARCHIVE, "<html>not a zip</html>".getBytes(StandardCharsets.UTF_8));
        assertProblem(DiagramArchive.Problem.NOT_AN_ARCHIVE, new byte[0]);
    }

    @Test
    void shouldTellADamagedArchive() throws IOException {
        byte[] archive = archiveOf("orders.class.violet.html", "<html>" + "orders ".repeat(500) + "</html>");
        assertProblem(DiagramArchive.Problem.DAMAGED, Arrays.copyOf(archive, archive.length / 3));
    }

    @Test
    void shouldTellAnArchiveProtectedByAPassword() throws IOException {
        byte[] archive = archiveOf("orders.class.violet.html", "<html>orders</html>");
        // Bit 0 of the general purpose flag of the local file header (offset 6): the entry is encrypted
        archive[6] |= 1;
        assertProblem(DiagramArchive.Problem.ENCRYPTED, archive);
    }

    @Test
    void shouldReadNoDiagramInAnEmptyArchive() throws IOException {
        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        new ZipOutputStream(archive).close();

        List<DiagramArchive.Entry> entries = new DiagramArchive().read(new ByteArrayInputStream(archive.toByteArray()),
                name -> name.endsWith(".violet.html") ? name : null);

        assertTrue(entries.isEmpty());
    }

    private byte[] archiveOf(String name, String content) throws IOException {
        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(archive, StandardCharsets.UTF_8)) {
            addEntry(zip, name, content);
        }
        return archive.toByteArray();
    }

    private void assertProblem(DiagramArchive.Problem expected, byte[] archive) {
        DiagramArchive.UnreadableArchiveException exception = assertThrows(DiagramArchive.UnreadableArchiveException.class,
                () -> new DiagramArchive().read(new ByteArrayInputStream(archive), name -> name.endsWith(".violet.html") ? name : null));
        assertEquals(expected, exception.getProblem());
    }

    @Test
    void shouldFormatTheMessagesOfTheImport() {
        for (Locale aLocale : List.of(Locale.ROOT, Locale.FRENCH, Locale.GERMAN)) {
            ResourceBundle bundle = ResourceBundle.getBundle(MenuFactory.class.getName(), aLocale);
            String done = bundle.getString("dialog.import_archive.done");
            String forAll = bundle.getString("dialog.import_conflict.for_all");
            for (int imported = 0; imported < 3; imported++) {
                for (int skipped = 0; skipped < 3; skipped++) {
                    MessageFormat.format(done, imported, skipped);
                }
            }
            MessageFormat.format(forAll, 1);
            MessageFormat.format(bundle.getString("dialog.import_conflict.message"), "a.class.violet.html");
            for (String aKey : List.of("not_an_archive", "damaged", "encrypted")) {
                String message = MessageFormat.format(bundle.getString("dialog.import_archive." + aKey), new Object[0]);
                assertTrue(!message.contains("''"), message);
            }
            MessageFormat.format(bundle.getString("dialog.manage_files.summary"), 0, "0 B");
            MessageFormat.format(bundle.getString("dialog.manage_files.export_archive_many"), 2);
            MessageFormat.format(bundle.getString("dialog.manage_files.short.export_archive_many"), 2);
            MessageFormat.format(bundle.getString("dialog.manage_files.close_files_many"), 2);
            MessageFormat.format(bundle.getString("dialog.manage_files.short.close_files_many"), 2);
            MessageFormat.format(bundle.getString("dialog.manage_files.size.kilobytes"), 12.34);
            MessageFormat.format(bundle.getString("dialog.manage_files.today"), "9:41");
            MessageFormat.format(bundle.getString("dialog.delete_from_storage.confirm_many"), 2, "a, b");
        }
        ResourceBundle english = ResourceBundle.getBundle(MenuFactory.class.getName(), Locale.ROOT);
        assertEquals("2 diagrams imported, 1 skipped.\nOpen them with File > Open.",
                MessageFormat.format(english.getString("dialog.import_archive.done"), 2, 1));
        assertEquals("No diagram imported.", MessageFormat.format(english.getString("dialog.import_archive.done"), 0, 0));
        assertEquals("Do this for the next 3 conflicts",
                MessageFormat.format(english.getString("dialog.import_conflict.for_all"), 3));
    }

    private void addEntry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        if (content != null) {
            zip.write(content.getBytes(StandardCharsets.UTF_8));
        }
        zip.closeEntry();
    }
}
