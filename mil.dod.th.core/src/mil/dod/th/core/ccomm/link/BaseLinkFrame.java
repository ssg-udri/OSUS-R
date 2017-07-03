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

import java.nio.ByteBuffer;


/**
 * This is the first concrete implementation of a {@link LinkFrame}. It is intended that
 * people wanting to support {@link LinkFrame}s will extend this class to gain common
 * implementation for the required elements. 
 */
public class BaseLinkFrame implements LinkFrame
{
    /** 
     * Holds the integer representation of the Address.
     */
    private int m_Address;

    /** 
     * Holds the ByteBuffer for this LinkFrame object.
     */
    private ByteBuffer m_Payload;

    /**
     * Create a default base link frame with no payload.
     */
    public BaseLinkFrame()
    {
        m_Address = 0;
    }
    
    /**
     * Get the address field of the frame.
     * 
     * @return The integer representation of the address field.
     * 
     * @deprecated Does not appear to be used. Consider removing in 2.0.
     */
    @Override
    @Deprecated
    public int getAddr()
    {
        return m_Address;
    }

    @Override
    public ByteBuffer getPayload()
    {
        return m_Payload;
    }

    /**
     * Set the address field of the frame. 
     * 
     * @param address The integer representation of the address field.
     * 
     * @deprecated Does not appear to be used. Consider removing in 2.0.
     */
    @Override
    @Deprecated
    public void setAddr(final int address)
    {
        m_Address = address;
    }

    @Override
    public void setPayload(final ByteBuffer payload)
    {
        m_Payload = payload;
    }
}
