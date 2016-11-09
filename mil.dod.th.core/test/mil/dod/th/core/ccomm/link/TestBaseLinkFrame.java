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
package mil.dod.th.core.ccomm.link;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

/**
 * @author dhumeniuk
 *
 */
public class TestBaseLinkFrame
{
    private BaseLinkFrame m_SUT;

    @Before
    public void setUp() throws Exception
    {
        m_SUT = new BaseLinkFrame();
    }

    @Test
    public void testAddress()
    {
        m_SUT.setAddr(8);
        assertThat(m_SUT.getAddr(), is(8));
    }
    
    @Test
    public void testPaylod()
    {
        ByteBuffer payload =  mock(ByteBuffer.class);
        m_SUT.setPayload(payload);
        assertThat(m_SUT.getPayload(), is(payload));
    }
}
