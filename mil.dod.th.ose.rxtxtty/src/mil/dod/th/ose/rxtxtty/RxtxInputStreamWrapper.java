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
package mil.dod.th.ose.rxtxtty;

import java.io.IOException;
import java.io.InputStream;

/**
 * Wrapper input stream implementation.
 * @author allenchl
 *
 */
public class RxtxInputStreamWrapper extends InputStream
{
    /**
     * Actual input stream from RXTX input stream impl to wrap.
     */
    final private InputStream m_In;
    
    /**
     * Base constructor.
     * 
     * @param input
     *      input stream to wrap
     */
    public RxtxInputStreamWrapper(final InputStream input)
    {
        super();
        m_In = input;
    }
    
    @Override
    public int available() throws IOException
    {
        return m_In.available();
    }
    
    @Override
    public synchronized void mark(final int readLimit)
    {
        m_In.mark(readLimit);
    }
    
    @Override
    public void close() throws IOException
    {
        m_In.close();
    }
    
    @Override
    public synchronized void reset() throws IOException
    {
        m_In.reset();
    }

    @Override
    public long skip(final long nBytes) throws IOException
    {
        return m_In.skip(nBytes);
    }
    
    @Override
    public int read() throws IOException
    {
        final byte[] byteRead = new byte[1];
        
        read(byteRead);
        
        return byteRead[0];
    }

    @Override
    public int read(final byte[] buf, final int offset, final int length) throws IOException
    {
        final int toReturn = m_In.read(buf, offset, length);
        if (toReturn == 0)
        {
            throw new mil.dod.th.core.ccomm.physical.TimeoutException("RXTX Serial port timed out while reading");
        }
        else
        {
            return toReturn;
        }
    }

    @Override
    public int read(final byte[] buf) throws IOException
    {
        return read(buf, 0, buf.length);
    }
}
