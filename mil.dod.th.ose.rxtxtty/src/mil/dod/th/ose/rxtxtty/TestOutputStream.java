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
import java.io.OutputStream;

/**
 * Test class for debugging the output stream.  Works with the actual output stream and wraps all calls.
 */
public class TestOutputStream extends OutputStream
{
    /**
     * Actual output stream to wrap.
     */
    final private OutputStream m_Output;

    /**
     * Base constructor.
     * 
     * @param output
     *      output stream to wrap
     */
    public TestOutputStream(final OutputStream output)
    {
        super();
        m_Output = output;
    }

    @Override
    public void close() throws IOException
    {
        m_Output.close();
    }

    @Override
    public void flush() throws IOException
    {
        m_Output.flush();
    }

    @Override
    public void write(final byte[] buf, final int off, final int len) throws IOException
    {
        System.err.printf("writing %d bytes...", len); // NOPMD: use of system err, for debug only
        m_Output.write(buf, off, len);
        System.err.printf("OK\n"); // NOPMD: use of system err, for debug only
    }

    @Override
    public void write(final byte[] buf) throws IOException
    {
        write(buf, 0, buf.length);
    }

    @Override
    public void write(final int buf) throws IOException
    {
        System.err.printf("writing one byte..."); // NOPMD: use of system err, for debug only
        m_Output.write(buf);
        System.err.printf("ok"); // NOPMD: use of system err, for debug only
    }
}
