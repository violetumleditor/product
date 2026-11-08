package com.horstmann.violet.application.cheerpj;

import java.io.IOException;

import com.horstmann.violet.framework.injection.bean.ManiocFramework.ManagedBean;

@ManagedBean
public class CheerpJInterfaceService {


    public CheerpJInterfaceService() {
        initialize();
    }

    public void initialize() {
        if (CheerpJInterfaceService.isJavaScriptBridgeAvailable()) {
            new Thread(() -> {
                try {
                    CheerpJInterfaceService.nativeSetApplication(this);
                    System.out.println("Starting Thread");
                } catch (Throwable t) {
                    // Ignore. The desktop build runs without the JavaScript bridge.
                }
            }).start();
        }
    }

    public static native void nativeSetApplication(CheerpJInterfaceService appInstance);

    public static native void nativeMethodCallback();

    public static native void nativeDownloadFile(String filename, String mimeType, byte[] content);

    public static native boolean nativeShowOpenDialog(String acceptedExtensions);

    public static native String nativeGetFileName();

    public static native boolean nativeHasPendingImport();

    public static native String nativeGetPendingImportName();

    public static native byte[] nativeConsumePendingImportData();

    public static native boolean nativeShowSaveDialog(String defaultFilename, byte[] content, String mimeType);

    public static native void nativeOpenBlobInNewTab(String filename, String mimeType, byte[] content);

    public static native void nativeOpenPdfPrintTab(byte[] content);

    public static boolean isJavaScriptBridgeAvailable() {
        try {
            nativeMethodCallback();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void downloadFile(String filename, String mimeType, byte[] content) throws IOException {
        try {
            nativeDownloadFile(filename, mimeType, content);
        } catch (Throwable t) {
            throw new IOException("Browser download bridge is not available", t);
        }
    }

    public static boolean showOpenDialog(String acceptedExtensions) throws IOException {
        try {
            return nativeShowOpenDialog(acceptedExtensions);
        } catch (Throwable t) {
            throw new IOException("File open dialog is not available", t);
        }
    }

    public static String getFileName() throws IOException {
        try {
            return nativeGetFileName();
        } catch (Throwable t) {
            throw new IOException("Cannot retrieve file name", t);
        }
    }

    public static boolean hasPendingImport() throws IOException {
        try {
            return nativeHasPendingImport();
        } catch (Throwable t) {
            throw new IOException("Cannot determine whether an import is pending", t);
        }
    }

    public static String getPendingImportName() throws IOException {
        try {
            return nativeGetPendingImportName();
        } catch (Throwable t) {
            throw new IOException("Cannot retrieve pending import file name", t);
        }
    }

    public static byte[] consumePendingImportData() throws IOException {
        try {
            return nativeConsumePendingImportData();
        } catch (Throwable t) {
            throw new IOException("Cannot retrieve pending import content", t);
        }
    }

    public static boolean showSaveDialog(String defaultFilename, byte[] content, String mimeType) throws IOException {
        try {
            return nativeShowSaveDialog(defaultFilename, content, mimeType);
        } catch (Throwable t) {
            throw new IOException("File save dialog is not available", t);
        }
    }

    public static void openBlobInNewTab(String filename, String mimeType, byte[] content) throws IOException {
        try {
            nativeOpenBlobInNewTab(filename, mimeType, content);
        } catch (Throwable t) {
            throw new IOException("Open in new tab bridge is not available", t);
        }
    }

    public static void openPdfPrintTab(byte[] content) throws IOException {
        try {
            nativeOpenPdfPrintTab(content);
        } catch (Throwable t) {
            throw new IOException("PDF print bridge is not available", t);
        }
    }

    public static native void nativeStoreTheme(String theme);

    /**
     * Remembers the user's theme in the browser, which gives it back on the next launch.
     */
    public static void storeTheme(String theme) {
        try {
            nativeStoreTheme(theme);
        } catch (Throwable t) {
            System.out.println("CheerpJInterfaceService.storeTheme(): theme not stored: " + t);
        }
    }

    public static native void nativeSetUnsavedChanges(boolean isUnsaved);

    /**
     * Tells the page whether a diagram still waits for its save, so it asks before being left or reloaded.
     */
    public static void setUnsavedChanges(boolean isUnsaved) {
        try {
            nativeSetUnsavedChanges(isUnsaved);
        } catch (Throwable t) {
            System.out.println("CheerpJInterfaceService.setUnsavedChanges(): page not told: " + t);
        }
    }

    public static native void nativeOpenUrl(String url);

    public static void openUrl(String url) throws IOException {
        System.out.println("CheerpJInterfaceService.openUrl() -> " + url);
        try {
            nativeOpenUrl(url);
            System.out.println("CheerpJInterfaceService.openUrl(): nativeOpenUrl invoked successfully");
        } catch (Throwable t) {
            System.out.println("CheerpJInterfaceService.openUrl(): nativeOpenUrl threw: " + t);
            t.printStackTrace(System.out);
            throw new IOException("Open URL bridge is not available", t);
        }
    }
}