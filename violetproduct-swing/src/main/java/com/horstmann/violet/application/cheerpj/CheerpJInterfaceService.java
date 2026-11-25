package com.horstmann.violet.application.cheerpj;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.SwingUtilities;

import com.horstmann.violet.framework.injection.bean.ManiocFramework.ManagedBean;
import com.horstmann.violet.workspace.editorpart.EditorPart;
import com.horstmann.violet.workspace.editorpart.behavior.ZoomByPinchBehavior;

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
        followPinches();
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

    /**
     * Shows the name of the active diagram in the title of the page
     *
     * @param filename the file name of the active diagram, empty when there is none
     */
    public static void setActiveFilename(String filename)
    {
        try
        {
            nativeSetActiveFilename(filename);
        }
        catch (Throwable t)
        {
            System.out.println("CheerpJInterfaceService.setActiveFilename(): page not told: " + t);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Full screen

    /**
     * Asks the page to enter or leave the browser full screen. Called during the click on the button, as browsers only
     * allow full screen right after a user gesture.
     */
    public static void toggleFullScreen()
    {
        try
        {
            nativeToggleFullScreen();
        }
        catch (Throwable t)
        {
            System.out.println("CheerpJInterfaceService.toggleFullScreen(): page not told: " + t);
        }
    }

    /**
     * @return true if the page is shown in full screen, as last read by the display size follower
     */
    public static boolean isFullScreen()
    {
        return fullScreenState == FULL_SCREEN_ON;
    }

    /**
     * @return false if the browser can't show the page in full screen (iPhone for example), as last read by the display
     *         size follower
     */
    public static boolean isFullScreenSupported()
    {
        return fullScreenState != FULL_SCREEN_UNSUPPORTED;
    }

    /**
     * Reads the full screen state of the page. Leaving it with the Escape key of the browser isn't told to Java.
     */
    private static void readFullScreenState()
    {
        try
        {
            fullScreenState = nativeGetFullScreenState();
        }
        catch (Throwable t)
        {
            fullScreenState = FULL_SCREEN_UNSUPPORTED;
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
                readFullScreenState();
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
    // Two finger zoom

    /**
     * Zooms the diagram under two fingers: CheerpJ only gives the first one to Java, as a mouse. The native answers once the
     * fingers move, so it is called from a background thread.
     */
    private void followPinches()
    {
        Thread follower = new Thread(() ->
        {
            while (true)
            {
                try
                {
                    String[] values = nativeWaitForPinch().split(";");
                    int newPinchId = Integer.parseInt(values[0]);
                    Point center = new Point((int) Math.round(Double.parseDouble(values[1])),
                            (int) Math.round(Double.parseDouble(values[2])));
                    double distance = Double.parseDouble(values[3]);
                    // Waits, so that the positions skipped meanwhile don't pile up
                    SwingUtilities.invokeAndWait(() -> onPinch(newPinchId, center, distance));
                }
                catch (InvocationTargetException e)
                {
                    e.printStackTrace(System.out);
                }
                catch (Throwable t)
                {
                    System.out.println("CheerpJInterfaceService.followPinches(): no two finger zoom: " + t);
                    return;
                }
            }
        }, PINCH_FOLLOWER_NAME);
        follower.setDaemon(true);
        follower.start();
    }

    /**
     * @param newPinchId changes at each pinch
     * @param center the middle of both fingers, on screen
     * @param distance between both fingers
     */
    private void onPinch(int newPinchId, Point center, double distance)
    {
        if (newPinchId != this.pinchId)
        {
            this.pinchId = newPinchId;
            this.pinchedEditor = findEditor(center);
            if (this.pinchedEditor != null)
            {
                Point point = getPointOnEditor(center);
                getPinchBehaviors().forEach(b -> b.startPinch(point, distance));
            }
            return;
        }
        if (this.pinchedEditor != null && this.pinchedEditor.isShowing())
        {
            Point point = getPointOnEditor(center);
            getPinchBehaviors().forEach(b -> b.movePinch(point, distance));
        }
    }

    private List<ZoomByPinchBehavior> getPinchBehaviors()
    {
        return this.pinchedEditor.getBehaviorManager().getBehaviors(ZoomByPinchBehavior.class);
    }

    private Point getPointOnEditor(Point pointOnScreen)
    {
        Point point = new Point(pointOnScreen);
        SwingUtilities.convertPointFromScreen(point, this.pinchedEditor);
        return point;
    }

    /**
     * @return the diagram shown at this point of the screen, or null if there is none, or if a dialog blocks it
     */
    private EditorPart findEditor(Point pointOnScreen)
    {
        for (Window window : Window.getWindows())
        {
            if (window.isShowing() && window instanceof Dialog && ((Dialog) window).isModal())
            {
                return null;
            }
        }
        for (Frame frame : Frame.getFrames())
        {
            if (!frame.isShowing())
            {
                continue;
            }
            Point point = new Point(pointOnScreen);
            SwingUtilities.convertPointFromScreen(point, frame);
            Component component = SwingUtilities.getDeepestComponentAt(frame, point.x, point.y);
            if (component instanceof EditorPart)
            {
                return (EditorPart) component;
            }
            EditorPart editor = (EditorPart) SwingUtilities.getAncestorOfClass(EditorPart.class, component);
            if (editor != null)
            {
                return editor;
            }
        }
        return null;
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

    /** See {@link #setActiveFilename(String)} */
    public static native void nativeSetActiveFilename(String filename);

    /** Width of the page area given to the CheerpJ display, see {@link #getDisplaySize()} */
    public static native int nativeGetDisplayWidth();

    /** Height of the page area given to the CheerpJ display, see {@link #getDisplaySize()} */
    public static native int nativeGetDisplayHeight();

    /** See {@link #toggleFullScreen()} */
    public static native void nativeToggleFullScreen();

    /** @return FULL_SCREEN_OFF, FULL_SCREEN_ON or FULL_SCREEN_UNSUPPORTED, see {@link #readFullScreenState()} */
    public static native int nativeGetFullScreenState();

    /** @return "pinch id;center x;center y;distance" once two fingers are put on the screen or move, see {@link #followPinches()} */
    public static native String nativeWaitForPinch();

    /** Values given by {@link #nativeGetFullScreenState()} */
    private static final int FULL_SCREEN_UNSUPPORTED = -1;
    private static final int FULL_SCREEN_ON = 1;

    /** Last state read from the page, written by the display size follower thread */
    private static volatile int fullScreenState = 0;

    /**
     * Delay between two readings of the display size, in milliseconds
     */
    private static final long DISPLAY_SIZE_POLLING_DELAY = 400;

    /**
     * Name of the thread that keeps the frames on the whole display
     */
    private static final String DISPLAY_SIZE_FOLLOWER_NAME = "CheerpJ display size follower";

    /**
     * Name of the thread that follows the fingers on the screen
     */
    private static final String PINCH_FOLLOWER_NAME = "CheerpJ pinch follower";

    /** The last pinch, 0 before the first one */
    private int pinchId;

    /** The diagram zoomed by the last pinch, or null if it started elsewhere */
    private EditorPart pinchedEditor;

}
