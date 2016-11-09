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

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * {@link InputStream} that reads from a {@link ByteBuffer}.
 * 
 * @author dhumeniuk
 *
 */
public class ByteBufferBackedInputStream extends InputStream
{
    /**
     * Internal buffer to read from.
     */
    private final ByteBuffer m_Buf;

    /**
     * Default constructor.
     * 
     * @param buf
     *      buffer to back input stream with
     */
    public ByteBufferBackedInputStream(final ByteBuffer buf)
    {
        super();
        this.m_Buf = buf;
    }

    @Override
    public int read()
    {
        if (!m_Buf.hasRemaining())
        {
            return -1;
        }
        return m_Buf.get();
    }

    @Override
    public int read(final byte[] bytes, final int off, final int len)
    {
        final int actualLen = Math.min(len, m_Buf.remaining());
        m_Buf.get(bytes, off, actualLen);
        return actualLen;
    }
}
