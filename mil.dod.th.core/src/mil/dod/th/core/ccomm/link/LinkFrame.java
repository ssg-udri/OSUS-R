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
 * This interface specifies the BARE minimum interface for a {@link LinkLayer} frame. It
 * is intended that all LinkLayers will support this default case, but also
 * derive off this interface/base class to support the specific options their
 * {@link LinkLayer} supports like addressing, flags, headers, etc. 
 */
public interface LinkFrame
{
    /**
     * Get the address field of the frame.
     * 
     * @return The integer representation of the address field.
     */
    int getAddr();

    /**
     * Get the payload as a ByteBuffer.
     * 
     * @return The payload of this frame. 
     */
    ByteBuffer getPayload();

    /**
     * Set the address field of the frame.  This is used as a sub-address in correlation with the 
     * {@link mil.dod.th.core.ccomm.Address}.  The use of this field is up to the {@link LinkLayer}.
     * 
     * @param addr The integer representation of the address field. 
     */
    void setAddr(int addr);

    /**
     * Set the payload as a ByteBuffer.
     * 
     * @param payload The payload to use 
     */
    void setPayload(ByteBuffer payload);

}
