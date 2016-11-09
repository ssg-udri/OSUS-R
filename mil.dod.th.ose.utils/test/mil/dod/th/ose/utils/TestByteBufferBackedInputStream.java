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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

public class TestByteBufferBackedInputStream
{

    @Before
    public void setUp() throws Exception
    {
    }

    @Test
    public void testRead() throws IOException
    {
        ByteBuffer buf = ByteBuffer.wrap(new byte[] { 0x52, (byte)0x88, (byte)0xf8 });
        ByteBufferBackedInputStream in = new ByteBufferBackedInputStream(buf);
        
        assertThat(in.read(), is(0x52));
        assertThat((byte)in.read(), is((byte)0x88));
        assertThat((byte)in.read(), is((byte)0xf8));
        assertThat(in.read(), is(-1));
        in.close();
    }

    @Test
    public void testReadByteArrayIntInt() throws IOException
    {
        ByteBuffer buf = ByteBuffer.wrap(new byte[] { 0x52, (byte)0x88, (byte)0xf8, 0x15, 0x78 });
        ByteBufferBackedInputStream in = new ByteBufferBackedInputStream(buf);
        
        byte[] b = new byte[4];
        assertThat(in.read(b, 1, 2), is(2));
        assertThat(b, is(new byte[]{0x0, 0x52, (byte)0x88, 0x0}));
        
        assertThat(in.read(b, 0, 4), is(3));
        assertThat(b, is(new byte[]{(byte)0xf8, 0x15, 0x78, 0x0}));
        in.close();
    }
}
