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
 * Interface for a remote channel that is {@link java.net.Socket} based.
 * 
 * @author Dave Humeniuk
 *
 */
@ProviderType
public interface SocketChannel extends RemoteChannel
{

    /**
     * Get the hostname of the remote endpoint this channel connects to.
     * 
     * @return
     *      hostname of the remote endpoint
     */
    String getHost();

    /**
     * Get the remote port of the endpoint this channel connects to.
     * 
     * @return
     *      remote port of the socket
     */
    int getPort();

}
