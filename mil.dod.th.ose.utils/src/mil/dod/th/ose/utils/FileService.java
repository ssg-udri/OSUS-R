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
package mil.dod.th.ose.utils;

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

import aQute.bnd.annotation.ProviderType;

/**
 * This service should be used when components must access files.
 * @author allenchl
 *
 */
@ProviderType
public interface FileService
{
    /**
     * Get a file by pathname.
     * @param pathName
     *      the path containing the file name
     * @return
     *      the file object found or created
     */
    File getFile(String pathName);
    
    /**
     * Get a file based off the parent file and child pathname string.
     * @param parent
     *      the file that represents the parent directory
     * @param child
     *      the pathname of the child file
     * @return
     *      the file object found or created using the parent and child pathnames.
     */
    File getFile(File parent, String child);
    
    /**
     * Get a file based off the parent directory and child file paths.
     * 
     * @param parent
     *      the pathname of the parent directory
     * @param child
     *      the pathname of the child file with respect to the parent.
     * @return
     *      the file object found or created using the parent and child pathnames.
     */
    File getFile(String parent, String child);
    
    /**
     * Create a file reader for a given file.
     *  
     * @param file
     *      the file to open for reading
     * @return
     *      reader for the given file
     * @throws FileNotFoundException
     *      if the given file does not exist
     */
    FileReader createFileReader(File file) throws FileNotFoundException;
    
    /**
     * Create a file writer for a given file.
     * @param file
     *      the file to open for writing
     * @return
     *      writer for the given file
     * @throws IOException
     *      if the file exists but is a directory rather than a regular file, 
     *      does not exist but cannot be created, or cannot be opened for any other reason
     */
    FileWriter createFileWriter(File file) throws IOException;
    
    /**
     * Create an output stream for a file.
     * 
     * @param file
     *      path to output stream that is to be created
     * @param append
     *      whether or not the the contents being written to the file should be append to the end of the file
     * @return
     *      the created file output stream
     * @throws FileNotFoundException
     *      if the given file is not found
     */
    FileOutputStream createFileOutputStream(File file, boolean append) throws FileNotFoundException;
    
    /**
     * Create an output stream for a file.
     * 
     * @param file
     *      path to output stream that is to be created
     * @return
     *      the created file output stream
     * @throws FileNotFoundException
     *      if the given file is not found
     */
    FileOutputStream createFileOutputStream(File file) throws FileNotFoundException;
    
    /**
     * Create an input stream for a file.
     * @param file
     *      the file to create the input stream for
     * @return
     *      the created file input stream
     * @throws FileNotFoundException
     *      if the given file is not found
     */
    FileInputStream createFileInputStream(File file) throws FileNotFoundException;
    
    /**
     * Create {@link PrintStream} for the given file.
     * @param file
     *      the {@link File} to use when creating the {@link PrintStream}
     * @return
     *      the output stream
     * @throws FileNotFoundException
     *      if the given file is not found
     */
    PrintStream createPrintStream(File file) throws FileNotFoundException;
    
    /**
     * Create {@link PrintStream} for the given file.
     * @param stream
     *      the {@link OutputStream} to use when creating the {@link PrintStream}
     * @return
     *      the output stream
     * @throws FileNotFoundException
     *      if the given file is not found
     */
    PrintStream createPrintStream(OutputStream stream) throws FileNotFoundException;
    
    /**
     * Create {@link ZipOutputStream} from a given {@link FileOutputStream}.
     * @param outputStream
     *  the {@link FileOutputStream} to use when creating the {@link ZipOutputStream}
     * @return
     *  the output stream
     */
    ZipOutputStream createZipOutputStream(FileOutputStream outputStream);
    
    /**
     * Create {@link BufferedReader} from a give {@link FileReader}.
     * @param fileReader
     *  the {@link FileReader} to use when creating the {@link BufferedReader}
     * @return
     *  the buffered reader
     */
    BufferedReader createBufferedReader(FileReader fileReader);
    
    /**
     * Checks to see if a given file exists or not.
     * @param file
     *  the file that is to be checked for existence
     * @return
     *  true if the given file exists; false otherwise
     */
    boolean doesFileExist(final File file);

    /**
     * Makes a directory at the given path using {@link File#mkdir()}.
     * 
     * @param path
     *      path to create directories for
     * @return
     *      true if the directory was created
     */
    boolean mkdir(File path);
}
