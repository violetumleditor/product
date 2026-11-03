package com.horstmann.violet.framework.file.persistence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import com.horstmann.violet.framework.file.IFile;
/**
 * Standard Java FileSaver implementation
 * 
 * @author Alexandre de Pellegrin
 *
 */
public class JFileWriter implements IFileWriter
{

    public JFileWriter(File f) throws FileNotFoundException
    {
        this.f = f;
        this.out = new FileOutputStream(f);
    }

    
    @Override
    public OutputStream getOutputStream()
    {
        return out;
    }

    @Override
    public IFile getFileDefinition()
    {
        return new IFile()
        {
            @Override
            public String getDirectory() { return f.getParent(); }
            @Override
            public String getFilename()  { return f.getName(); }
        };
    }
    

    private File f;
    private OutputStream out;

}
