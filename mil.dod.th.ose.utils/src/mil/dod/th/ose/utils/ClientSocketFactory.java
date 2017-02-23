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

import java.io.IOException;
import java.net.Socket;

/**
 * Create a client socket. This interface supports dependency injection to other components that need a service for
 * creating new client sockets.
 * 
 * @author callen
 */
public interface ClientSocketFactory 
{
    /**
     * Create a {@link Socket} bound to a given port based off the specified host. Optionally enables SSL on the
     * client socket.
     * 
     * @param host
     *     the host address 
     * @param port
     *     port that this socket is bound to
     * @param useSsl
     *      flag to enable/disable SSL on the socket
     * @return
     *     socket created by this factory
     * @throws IOException
     *     thrown in the event that there is an issue creating the socket     
     */
    Socket createClientSocket(String host, int port, boolean useSsl) throws IOException;

    /**
     * Create a {@link Socket} bound to a given port based off the specified host. SSL is not enabled on the
     * client socket.
     * 
     * @param host
     *     the host address 
     * @param port
     *     port that this socket is bound to
     * @return
     *     socket created by this factory
     * @throws IOException
     *     thrown in the event that there is an issue creating the socket     
     */
    Socket createClientSocket(String host, int port) throws IOException;
}
