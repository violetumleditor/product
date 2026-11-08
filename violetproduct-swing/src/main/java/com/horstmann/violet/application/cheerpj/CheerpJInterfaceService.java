package com.horstmann.violet.application.cheerpj;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.IOException;

import javax.swing.SwingUtilities;

import com.horstmann.violet.framework.injection.bean.ManiocFramework.ManagedBean;

/**
 * Bridge between Java and the web page when the application runs in a browser with CheerpJ.<br>
 * Each <code>native</code> method is implemented in JavaScript by the page (<code>web/index.html</code>), in a function
 * named <code>Java_com_horstmann_violet_application_cheerpj_CheerpJInterfaceService_&lt;method&gt;</code> that is also
 * listed in <code>cheerpjInit({natives:{...}})</code>. The other methods wrap them and handle the case where the page
 * doesn't answer.<br>
 * On the desktop, there is no page: this service does nothing.
 */
@ManagedBean
public class CheerpJInterfaceService
{

    /**
     * Default constructor
     */
    public CheerpJInterfaceService()
    {
        initialize();
    }

    /**
     * When the page answers, gives it this service and keeps the application frame on the whole page
     */
    public void initialize()
    {
        if (!CheerpJInterfaceService.isJavaScriptBridgeAvailable())
        {
            // Desktop version: no JavaScript bridge
            return;
        }
        new Thread(() ->
        {
            try
            {
                CheerpJInterfaceService.nativeSetApplication(this);
            }
            catch (Throwable t)
            {
                System.out.println("CheerpJInterfaceService.initialize(): service not given to the page: " + t);
            }
        }).start();
        followDisplaySize();
    }

    /**
     * @return true when the application runs in a browser page that implements the natives
     */
    public static boolean isJavaScriptBridgeAvailable()
    {
        try
        {
            nativeMethodCallback();
            return true;
        }
        catch (Throwable t)
        {
            return false;
        }
    }

    // ------------------------------------------------------------------------------------------
    // Files

    /**
     * Makes the browser download a file
     *
     * @param filename name proposed to the user
     * @param mimeType type of the content
     * @param content file content
     * @throws IOException if the page doesn't answer
     */
    public static void downloadFile(String filename, String mimeType, byte[] content) throws IOException
    {
        try
        {
            nativeDownloadFile(filename, mimeType, content);
        }
        catch (Throwable t)
        {
            throw new IOException("Browser download bridge is not available", t);
        }
    }

    /**
     * Opens the browser file picker. The picked file is then staged by the page: see {@link #hasPendingImport()}.
     *
     * @param acceptedExtensions file extensions shown in the picker
     * @return true if the user picked a file
     * @throws IOException if the page doesn't answer
     */
    public static boolean showOpenDialog(String acceptedExtensions) throws IOException
    {
        try
        {
            return nativeShowOpenDialog(acceptedExtensions);
        }
        catch (Throwable t)
        {
            throw new IOException("File open dialog is not available", t);
        }
    }

    /**
     * @return name of the last file picked in the browser
     * @throws IOException if the page doesn't answer
     */
    public static String getFileName() throws IOException
    {
        try
        {
            return nativeGetFileName();
        }
        catch (Throwable t)
        {
            throw new IOException("Cannot retrieve file name", t);
        }
    }

    /**
     * @return true if a file picked in the browser waits to be read by Java
     * @throws IOException if the page doesn't answer
     */
    public static boolean hasPendingImport() throws IOException
    {
        try
        {
            return nativeHasPendingImport();
        }
        catch (Throwable t)
        {
            throw new IOException("Cannot determine whether an import is pending", t);
        }
    }

    /**
     * @return name of the file that waits to be read by Java
     * @throws IOException if the page doesn't answer
     */
    public static String getPendingImportName() throws IOException
    {
        try
        {
            return nativeGetPendingImportName();
        }
        catch (Throwable t)
        {
            throw new IOException("Cannot retrieve pending import file name", t);
        }
    }

    /**
     * Reads the file that waits to be read by Java. The page forgets it afterwards.
     *
     * @return file content
     * @throws IOException if the page doesn't answer
     */
    public static byte[] consumePendingImportData() throws IOException
    {
        try
        {
            return nativeConsumePendingImportData();
        }
        catch (Throwable t)
        {
            throw new IOException("Cannot retrieve pending import content", t);
        }
    }

    /**
     * Asks the browser to save a file, with its own save dialog when it has one
     *
     * @param defaultFilename name proposed to the user
     * @param content file content
     * @param mimeType type of the content
     * @return true if the file was saved
     * @throws IOException if the page doesn't answer
     */
    public static boolean showSaveDialog(String defaultFilename, byte[] content, String mimeType) throws IOException
    {
        try
        {
            return nativeShowSaveDialog(defaultFilename, content, mimeType);
        }
        catch (Throwable t)
        {
            throw new IOException("File save dialog is not available", t);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Browser tabs

    /**
     * Shows a content (an exported image for example) in a new browser tab
     *
     * @param filename name of the content
     * @param mimeType type of the content
     * @param content content to show
     * @throws IOException if the page doesn't answer
     */
    public static void openBlobInNewTab(String filename, String mimeType, byte[] content) throws IOException
    {
        try
        {
            nativeOpenBlobInNewTab(filename, mimeType, content);
        }
        catch (Throwable t)
        {
            throw new IOException("Open in new tab bridge is not available", t);
        }
    }

    /**
     * Shows a PDF in a new browser tab, from which the user prints it (replaces printing in the browser)
     *
     * @param content PDF content
     * @throws IOException if the page doesn't answer
     */
    public static void openPdfPrintTab(byte[] content) throws IOException
    {
        try
        {
            nativeOpenPdfPrintTab(content);
        }
        catch (Throwable t)
        {
            throw new IOException("PDF print bridge is not available", t);
        }
    }

    /**
     * Opens a web address in a new browser tab
     *
     * @param url address to open
     * @throws IOException if the page doesn't answer
     */
    public static void openUrl(String url) throws IOException
    {
        System.out.println("CheerpJInterfaceService.openUrl() -> " + url);
        try
        {
            nativeOpenUrl(url);
            System.out.println("CheerpJInterfaceService.openUrl(): nativeOpenUrl invoked successfully");
        }
        catch (Throwable t)
        {
            System.out.println("CheerpJInterfaceService.openUrl(): nativeOpenUrl threw: " + t);
            t.printStackTrace(System.out);
            throw new IOException("Open URL bridge is not available", t);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Page state

    /**
     * Remembers the user's theme in the browser, which gives it back on the next launch
     *
     * @param theme LaunchingPreferences.LIGHT_THEME or LaunchingPreferences.DARK_THEME
     */
    public static void storeTheme(String theme)
    {
        try
        {
            nativeStoreTheme(theme);
        }
        catch (Throwable t)
        {
            System.out.println("CheerpJInterfaceService.storeTheme(): theme not stored: " + t);
        }
    }

    /**
     * Tells the page whether a diagram still waits for its save, so it asks before being left or reloaded
     *
     * @param isUnsaved true while a diagram isn't saved
     */
    public static void setUnsavedChanges(boolean isUnsaved)
    {
        try
        {
            nativeSetUnsavedChanges(isUnsaved);
        }
        catch (Throwable t)
        {
            System.out.println("CheerpJInterfaceService.setUnsavedChanges(): page not told: " + t);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Display size

    /**
     * @return size of the page area given to the CheerpJ display, which follows the browser window (resize, mobile
     *         screen rotation, address bar shown or hidden). Falls back to the screen size.
     */
    public static Dimension getDisplaySize()
    {
        try
        {
            int width = nativeGetDisplayWidth();
            int height = nativeGetDisplayHeight();
            if (width > 0 && height > 0)
            {
                return new Dimension(width, height);
            }
        }
        catch (Throwable t)
        {
            // No bridge: use the screen size
        }
        return Toolkit.getDefaultToolkit().getScreenSize();
    }

    /**
     * Keeps full page frames on the whole display, as a maximized frame is not resized when the page changes size.
     * The display size is read from a background thread, as the natives are asynchronous on the JavaScript side.
     */
    private static void followDisplaySize()
    {
        Thread follower = new Thread(() ->
        {
            while (true)
            {
                Dimension size = getDisplaySize();
                SwingUtilities.invokeLater(() -> fitFullPageFrames(size));
                try
                {
                    Thread.sleep(DISPLAY_SIZE_POLLING_DELAY);
                }
                catch (InterruptedException e)
                {
                    return;
                }
            }
        }, DISPLAY_SIZE_FOLLOWER_NAME);
        follower.setDaemon(true);
        follower.start();
    }

    /**
     * Gives the display size to each showing frame that is undecorated or maximized, as the main frame in kiosk mode.
     * Dialogs are not frames: they keep their size.
     *
     * @param size display size
     */
    private static void fitFullPageFrames(Dimension size)
    {
        Rectangle displayBounds = new Rectangle(0, 0, size.width, size.height);
        for (Frame frame : Frame.getFrames())
        {
            boolean isMaximized = (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
            boolean isFullPage = frame.isUndecorated() || isMaximized;
            if (frame.isShowing() && isFullPage && !frame.getBounds().equals(displayBounds))
            {
                // A maximized frame may ignore new bounds
                frame.setExtendedState(Frame.NORMAL);
                frame.setBounds(displayBounds);
                frame.validate();
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Natives, implemented in JavaScript by the page

    /** Gives this service to the page */
    public static native void nativeSetApplication(CheerpJInterfaceService appInstance);

    /** Does nothing: only throws when there is no page, see {@link #isJavaScriptBridgeAvailable()} */
    public static native void nativeMethodCallback();

    /** See {@link #downloadFile(String, String, byte[])} */
    public static native void nativeDownloadFile(String filename, String mimeType, byte[] content);

    /** See {@link #showOpenDialog(String)} */
    public static native boolean nativeShowOpenDialog(String acceptedExtensions);

    /** See {@link #getFileName()} */
    public static native String nativeGetFileName();

    /** See {@link #hasPendingImport()} */
    public static native boolean nativeHasPendingImport();

    /** See {@link #getPendingImportName()} */
    public static native String nativeGetPendingImportName();

    /** See {@link #consumePendingImportData()} */
    public static native byte[] nativeConsumePendingImportData();

    /** See {@link #showSaveDialog(String, byte[], String)} */
    public static native boolean nativeShowSaveDialog(String defaultFilename, byte[] content, String mimeType);

    /** See {@link #openBlobInNewTab(String, String, byte[])} */
    public static native void nativeOpenBlobInNewTab(String filename, String mimeType, byte[] content);

    /** See {@link #openPdfPrintTab(byte[])} */
    public static native void nativeOpenPdfPrintTab(byte[] content);

    /** See {@link #openUrl(String)} */
    public static native void nativeOpenUrl(String url);

    /** See {@link #storeTheme(String)} */
    public static native void nativeStoreTheme(String theme);

    /** See {@link #setUnsavedChanges(boolean)} */
    public static native void nativeSetUnsavedChanges(boolean isUnsaved);

    /** Width of the page area given to the CheerpJ display, see {@link #getDisplaySize()} */
    public static native int nativeGetDisplayWidth();

    /** Height of the page area given to the CheerpJ display, see {@link #getDisplaySize()} */
    public static native int nativeGetDisplayHeight();

    /**
     * Delay between two readings of the display size, in milliseconds
     */
    private static final long DISPLAY_SIZE_POLLING_DELAY = 400;

    /**
     * Name of the thread that keeps the frames on the whole display
     */
    private static final String DISPLAY_SIZE_FOLLOWER_NAME = "CheerpJ display size follower";

}
