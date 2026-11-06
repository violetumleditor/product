package com.horstmann.violet.framework.file;

import java.io.IOException;
import java.io.OutputStream;

import com.horstmann.violet.product.diagram.abstracts.IGraph;

public interface IGraphFile extends IFile
{

    /**
     * @return associated graph
     */
    public abstract IGraph getGraph();
    
    
    /**
     * Must be triggered to allow file saving
     */
    public abstract void setSaveRequired();
    
    /**
     * To know if the graph needs to be saved
     * @return true if save is required
     */
    public abstract boolean isSaveRequired();
    
    
    /**
     * Deletes the file described by this descriptor from the underlying storage.
     * Uses an {@link com.horstmann.violet.framework.file.persistence.IFileDeleter} obtained
     * from the file chooser service. Best-effort — does not throw.
     */
    public abstract void delete();

    /**
     * Saves the graph.
     * <p>
     * When {@code allowNewLocation} is {@code true} (user-initiated save): if the file still
     * has a temporary name the user is prompted for a real location via {@code saveToNewLocation()};
     * otherwise the file is written in-place, listeners are notified, and any I/O error is
     * re-thrown as a {@link RuntimeException}.
     * <p>
     * When {@code allowNewLocation} is {@code false} (auto-save): the method is a no-op if
     * nothing has changed ({@code isSaveRequired() == false}); it writes silently to the current
     * location (temp or real) and swallows any I/O error so as never to interrupt the user.
     *
     * @param allowNewLocation {@code true} to let the user choose a new location when the file
     *                         is still temporary; {@code false} for a silent background save.
     */
    public abstract void save(boolean allowNewLocation);

    /**
     * Saves the graph to a new URI (this will open a file chooser)
     */
    public abstract void saveToNewLocation();

    /**
     * Gives a new name to this file, in its current directory, and saves it. The file is then no longer temporary.
     *
     * @param newFilename the complete filename, with its extension
     * @throws java.nio.file.FileAlreadyExistsException if another file already has this name
     * @throws IOException if the file can't be renamed. It then keeps its name.
     */
    public abstract void rename(String newFilename) throws IOException;
    
    /**
     * Adds a listener to be informed each time the graph is modified or saved
     * 
     * @param listener
     */
    public abstract void addListener(IGraphFileListener listener);

    /**
     * Removes a listener
     * 
     * @param listener
     */
    public abstract void removeListener(IGraphFileListener listener);

    /**
     * Exports the graph as an image to the clipboard and shows a dialog box on the parent frame when it is done.
     * 
     */
    public abstract void exportToClipboard();

    /**
     * Exports the current graph to an image file.
     * 
     * @param out the output stream
     * @param format the desired file format
     */
    public abstract void exportImage(OutputStream out, String format);

    /**
     * Exports the current graph to a PDF file.
     *
     * @param out the output stream
     */
    public abstract void exportToPdf(OutputStream out);

    /**
     * Prints the graph
     * 
     */
    public abstract void exportToPrinter();
    

    /**
     * @return current file name (or null if not saved yet)
     */
    public String getFilename();
    
    /**
     * @return graph file location (or null if not saved yet)
     */
    public String getDirectory();
    
}