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
//
// DESCRIPTION:
// Contains the SerialPortOutputStream class implementation.
//
//==============================================================================
package mil.dod.th.ose.linux.physical;

import java.io.IOException;
import java.io.OutputStream;

import mil.dod.th.ose.linux.gnu_c.FileOperations;

/**
 * Class serves as the {@link OutputStream} implementation for the SerialPortProxyImpl service.
 * 
 * @author dhumeniuk
 */
public class SerialPortOutputStream extends OutputStream
{
    /**
     * Failure string for the {@link FileOperations#writeBuffer} native method. 
     */
    private static final String WRITE_BUFFER_FAILED_STR = "writeBuffer native method failed";
    
    /**
     * File descriptor for the output stream.
     */
    private final int m_Fd;
    
    /**
     * Create the SerialPortOutputStream using the file descriptor given.
     * @param fileDescriptor - file descriptor to write to
     */
    public SerialPortOutputStream(final int fileDescriptor)
    {
        super();
        
        m_Fd = fileDescriptor;
    }

    @Override
    public void write(final int data) throws IOException
    {
        final byte[] buf = new byte[] {(byte)data};
        final int returnValue = FileOperations.writeBuffer(m_Fd, buf, 0, buf.length);
        if (returnValue == -1)
        {
            throw new IOException(WRITE_BUFFER_FAILED_STR);
        }
    }

    @Override
    public void write(final byte[] buffer, final int offset, final int length) throws IOException
    {
        int bytesWritten = 0;
        while (bytesWritten < length)
        {
            final int returnValue = FileOperations.writeBuffer(m_Fd, 
                                                               buffer, 
                                                               offset + bytesWritten, 
                                                               length - bytesWritten);
            if (returnValue == -1)
            {
                throw new IOException(WRITE_BUFFER_FAILED_STR);
            }
            else
            {
                // not error, so return value means how many bits were written
                bytesWritten += returnValue;
            }
        }
    }
}
