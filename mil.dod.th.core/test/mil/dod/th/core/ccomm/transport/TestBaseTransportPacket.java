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
package mil.dod.th.core.ccomm.transport;

import java.nio.ByteBuffer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

/**
 * @author dhumeniuk
 *
 */
public class TestBaseTransportPacket
{
    @Test
    public final void testPayloadProp()
    {
        ByteBuffer payload = ByteBuffer.wrap(new byte[] {1,2,3,4});
        BaseTransportPacket pkt = new BaseTransportPacket(payload);
        
        assertThat(pkt.getPayload(), is(payload));
        
        ByteBuffer payload2 = ByteBuffer.wrap(new byte[] {3,1,4,6});
        pkt.setPayload(payload2);
        assertThat(pkt.getPayload(), is(payload2));
    }
}
