/*
 Violet - A program for editing UML diagrams.

 Copyright (C) 2007 Cay S. Horstmann (http://horstmann.com)
 Alexandre de Pellegrin (http://alexdp.free.fr);

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.horstmann.violet.framework.file.chooser;

import java.io.IOException;

import com.horstmann.violet.framework.file.IFile;
import com.horstmann.violet.framework.file.naming.ExtensionFilter;
import com.horstmann.violet.framework.file.persistence.IFileDeleter;
import com.horstmann.violet.framework.file.persistence.IFileReader;
import com.horstmann.violet.framework.file.persistence.IFileWriter;


/**
 * Services needed to choose a file on the filesystem when opening or saving a graph. This service 
 * could seem bizarre but, for example, the dialog box will be different when the program
 * is started from Eclipse than when it is started from JavaWebStart. M'kay!!!...
 * @author Alexandre de Pellegrin
 *
 */
public interface IFileChooserService
{

    /**
     * Tests whether the service is provided by WebStart
     * 
     * @return true if this service is provided by WebStart
     */
    public boolean isWebStart();

    /**
     * Pick up a file and return a file handler
     * 
     * @return the Open object for the selected file
     * @throws IOException
     */
    public IFileReader chooseAndGetFileReader(ExtensionFilter... extensions) throws IOException;
    
    /**
     * Returns a file handler to read the given file
     * 
     * @param file : existing file definition
     * @return the Open object for the selected file
     * @throws IOException
     */
    public IFileReader getFileReader(IFile file) throws IOException;

    /**
     * Choose a file and return a file handler to save content 
     * 
     * @param acceptable file filters
     * @return the Save object for the selected file
     * @throws IOException
     */
    public IFileWriter chooseAndGetFileWriter(ExtensionFilter... extensions) throws IOException;

    /**
     * Choose a file and return a file handler to save content, with an optional pre-filled filename.
     *
     * @param suggestedFilename filename to pre-populate in the dialog, or {@code null} for none
     * @param extensions acceptable extension filters
     * @return the Save object for the selected file, or {@code null} if the user cancelled
     * @throws IOException
     */
    public IFileWriter chooseAndGetFileWriter(String suggestedFilename, ExtensionFilter... extensions) throws IOException;
    
    /**
     * Returns a file handler to save the given file
     * 
     * @param extensions acceptable extension filters
     * @return the Save object for the selected file
     * @throws IOException
     */
    public IFileWriter getFileWriter(IFile file) throws IOException;

    /**
     * Returns a deleter for the given file, using the appropriate storage mechanism
     * for the current environment (standard filesystem, CheerpJ virtual FS, etc.).
     * Analogous to {@link #getFileWriter(IFile)} for write operations.
     *
     * @param file the file to delete
     * @return an {@link IFileDeleter} bound to that file
     */
    public IFileDeleter getFileDeleter(IFile file);

    /**
     * Renames a stored file, without overwriting another one. Does nothing on the storage if the file hasn't been written yet.
     *
     * @param file the file to rename
     * @param newFilename its new name, in the same directory
     * @throws java.nio.file.FileAlreadyExistsException if another file already has this name
     * @throws IOException if the file can't be renamed
     */
    public void renameFile(IFile file, String newFilename) throws IOException;

    /**
     * Returns the directory used for temporary auto-saves in this environment.
     * For Swing: the OS temporary directory. For CheerpJ: the browser storage mount (/files).
     *
     * @return absolute path of the temp directory
     */
    public String getTempDirectory();

}
