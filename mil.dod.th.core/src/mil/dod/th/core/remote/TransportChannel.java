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
package mil.dod.th.core.remote;

import aQute.bnd.annotation.ProviderType;

/**
 * Interface for a remote channel that is {@link mil.dod.th.core.ccomm.transport.TransportLayer} based.
 * 
 * @author Dave Humeniuk
 *
 */
@ProviderType
public interface TransportChannel extends RemoteChannel
{

    /**
     * Get the name of {@link mil.dod.th.core.ccomm.transport.TransportLayer} used to send messages with this channel.
     * 
     * @return
     *      name of the transport layer
     */
    String getTransportLayerName();

    /**
     * Get the string representation of local address used by the transport layer when receiving messages.
     * 
     * @return
     *      address string defined by {@link mil.dod.th.core.ccomm.Address#getDescription()} 
     */
    String getLocalMessageAddress();

    /**
     * Get the string representation of remote address used by the transport layer when sending/receiving messages.
     * 
     * @return
     *      address string defined by {@link mil.dod.th.core.ccomm.Address#getDescription()} 
     */
    String getRemoteMessageAddress();

}
