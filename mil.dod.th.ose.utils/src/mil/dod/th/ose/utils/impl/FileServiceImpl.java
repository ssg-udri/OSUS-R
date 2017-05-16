//==============================================================================
// This software is part of the Open Standard for Unattended Sensors (OSUS)
// reference implementation (OSUS-R).
//
// To the extent possible under law, the author(s) have dedicated all copyright
// and related and neighboring rights to this software to the public domain
// worldwide. This software is distributed without any warranty.
//
// You should have received a copy of the CC0 Public Domain Dedication along
// with this software. If not, see
// <http://creativecommons.org/publicdomain/zero/1.0/>.
//==============================================================================
package mil.dod.th.ose.utils.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.zip.ZipOutputStream;

import aQute.bnd.annotation.component.Component;

import mil.dod.th.ose.utils.CoverageIgnore;
import mil.dod.th.ose.utils.FileService;

/**
 * Implementation of (@link {@link FileService}.
 * @author allenchl
 *
 */
@Component //NOCHECKSTYLE data abstraction, class is only a simple wrapper.
public class FileServiceImpl implements FileService
{
    @Override
    public File getFile(final String pathName)
    {
        return new File(pathName);
    }
    
    @CoverageIgnore
    @Override
    public File getFile(final File parent, final String child)
    {
        return new File(parent, child);
    }
    
    @CoverageIgnore
    @Override
    public File getFile(final String parent, final String child)
    {
        return new File(parent, child);
    }
    
    @CoverageIgnore
    @Override
    public FileReader createFileReader(final File file) throws FileNotFoundException
    {
        return new FileReader(file);
    }
    
    @CoverageIgnore
    @Override
    public FileOutputStream createFileOutputStream(final File file) throws FileNotFoundException
    {
        return new FileOutputStream(file);
    }
    
    @CoverageIgnore
    @Override
    public FileInputStream createFileInputStream(final File file) throws FileNotFoundException
    {
        return new FileInputStream(file);
    }

    @Override
    public boolean doesFileExist(final File file)
    {
        return file.exists();
    }

    @CoverageIgnore
    @Override
    public FileWriter createFileWriter(final File file) throws IOException
    {
        return new FileWriter(file);
    }
    
    @CoverageIgnore
    @Override
    public FileOutputStream createFileOutputStream(final File file, final boolean append) throws FileNotFoundException
    {
        return new FileOutputStream(file, append);
    }
    
    @CoverageIgnore
    @Override
    public PrintStream createPrintStream(final File file) throws FileNotFoundException
    {
        return new PrintStream(file);
    }
    
    @CoverageIgnore
    @Override
    public PrintStream createPrintStream(final OutputStream stream) throws FileNotFoundException
    {
        return new PrintStream(stream);
    }

    @CoverageIgnore
    @Override
    public ZipOutputStream createZipOutputStream(final FileOutputStream outputStream)
    {
        return new ZipOutputStream(outputStream);
    }

    @CoverageIgnore
    @Override
    public boolean mkdir(final File path)
    {
        return path.mkdir();
    }

    @CoverageIgnore
    @Override
    public BufferedReader createBufferedReader(final FileReader fileReader)
    {
        return new BufferedReader(fileReader);
    }
}
