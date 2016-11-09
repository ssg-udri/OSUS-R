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
 * Test class for debugging the input stream.  Works with the actual input stream and wraps all calls.
 */
public class TestInputStream extends InputStream
{
    /**
     * Actual input stream to wrap.
     */
    final private InputStream m_In;

    /**
     * Base constructor.
     * 
     * @param input
     *      input stream to wrap
     */
    public TestInputStream(final InputStream input)
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
    public void close() throws IOException
    {
        m_In.close();
    }

    @Override
    public synchronized void mark(final int readLimit)
    {
        m_In.mark(readLimit);
    }

    @Override
    public boolean markSupported()
    {
        return m_In.markSupported();
    }

    @Override
    public int read() throws IOException
    {
        System.err.printf("Reading one byte..."); // NOPMD: use of system err, for debug only
        final int val = m_In.read();
        System.err.println("ok"); // NOPMD: use of system err, for debug only
        return val;
    }

    @Override
    public int read(final byte[] buf, final int offset, final int length) throws IOException
    {
        System.err.printf("Reading %d bytes...", length); // NOPMD: use of system err, for debug only
        final int rval = m_In.read(buf, offset, length);
        System.err.printf("ok %d bytes\n", rval); // NOPMD: use of system err, for debug only
        return rval;
    }

    @Override
    public int read(final byte[] buf) throws IOException
    {
        System.err.printf("Reading byte array..."); // NOPMD: use of system err, for debug only
        final int rval = m_In.read(buf);
        System.err.printf("ok\n"); // NOPMD: use of system err, for debug only
        return rval;
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

}
