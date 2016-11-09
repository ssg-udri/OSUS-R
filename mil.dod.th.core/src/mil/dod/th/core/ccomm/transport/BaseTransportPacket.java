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

/**
 * This is a base, concrete implementation of a {@link TransportPacket}. It simply
 * contains a ByteBuffer to store payload data. 
 */
public class BaseTransportPacket implements TransportPacket
{
    /** The data buffer being transported. */
    private ByteBuffer m_Payload;
    
    /**
     * Create a packet with the provided, initial payload. 
     * 
     * @param payload The data to be held by this frame.
     */
    public BaseTransportPacket(final ByteBuffer payload)
    {
        m_Payload = payload;
    }

    @Override
    public ByteBuffer getPayload()
    {
        return m_Payload;
    }

    @Override
    public void setPayload(final ByteBuffer payload)
    {
        m_Payload = payload;
    }

}
