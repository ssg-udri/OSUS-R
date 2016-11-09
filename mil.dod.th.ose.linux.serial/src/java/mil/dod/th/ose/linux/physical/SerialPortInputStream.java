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
// Contains the SerialPortInputStream class implementation.
//
//==============================================================================
package mil.dod.th.ose.linux.physical;

import java.io.IOException;
import java.io.InputStream;

import mil.dod.th.core.ccomm.physical.TimeoutException;
import mil.dod.th.ose.linux.gnu_c.FileOperations;


/**
 * Class serves as the {@link InputStream} implementation for the SerialPortProxyImpl service.
 * 
 * @author dhumeniuk
 */
public class SerialPortInputStream extends InputStream
{
    /**
     * File descriptor for the input stream.
     */
    private final int m_Fd;
    
    /**
     * Read timeout value, 0 means no timeout, wait forever.
     */
    private int m_TimeoutMS;

    /**
     * Create the SerialPortInputStream using the file descriptor given.
     * @param fileDescriptor - file descriptor to read from
     */
    public SerialPortInputStream(final int fileDescriptor)
    {
        super();
        
        m_Fd = fileDescriptor;
    }
    
    /**
     * Set the read timeout for the input stream.
     * 
     * @param TimeoutMS - timeout value in milliseconds, 0 for no timeout, wait forever
     */
    public void setReadTimeout(final int TimeoutMS)
    {
        m_TimeoutMS = TimeoutMS;
    }
    
    /**
     * Handle return values from the {@link FileOperations#readBuffer} native method.
     * 
     * @param returnValue
     *          The value that was returned by {@link FileOperations#readBuffer}
     * @return normally will throw exception, but if error isn't handled will return -1 and assert
     * @throws IOException
     *          thrown if the error is a file operation error (read, select) or timeout
     */
    private int handleReadError(final int returnValue) throws IOException
    {
        if (returnValue == 0) // 0 bytes read
        {
            throw new IllegalStateException("No bytes read (rv=0), but not due to a known error");
        }
        if (returnValue == FileOperations.FO_READ_TIMEOUT)
        {
            throw new TimeoutException("Timeout reading byte from serial port");
        }
        else if (returnValue == FileOperations.FO_READ_FAILURE)
        {
            throw new IOException("Native read failed");
        }
        else if (returnValue == FileOperations.FO_SELECT_FAILURE)
        {
            throw new IOException("Native select failed");
        }
        else if (returnValue == FileOperations.FO_INVALID_ARG)
        {
            throw new IllegalArgumentException("Invalid argument sent to tryRead");
        }
        
        // should never reach this point
        assert false : String.format("Unexpected return value from tryRead = %d%n", returnValue);
        return -1;
    }

    @Override
    public int read() throws IOException
    {
        final byte[] buf = new byte[1];
        final int returnValue = FileOperations.readBuffer(m_Fd, buf, 0, buf.length, m_TimeoutMS);
        
        if (returnValue == 1)
        {
            return buf[0] & 0xFF; //NOCHECKSTYLE: byte mask for unsigned
        }
        else
        {
            return handleReadError(returnValue);
        }
    }

    @Override
    public int read(final byte[] buffer, final int offset, final int length) throws IOException
    {
        final int returnValue = FileOperations.readBuffer(m_Fd, buffer, offset, length, m_TimeoutMS);
        
        if (returnValue >= 0)
        {
            return returnValue; 
        }
        else
        {
            return handleReadError(returnValue);
        }
    }
}
