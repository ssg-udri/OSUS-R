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
import java.net.ServerSocket;

/**
 * Create a server socket.  Abstracted interface to support dependency injection.
 * 
 * @author Dave Humeniuk
 *
 */
public interface ServerSocketFactory
{
    /**
     * Creates a {@link ServerSocket} bound to the given port.
     * 
     * @param port
     *      port to listen on
     * @return
     *      server socket created by this factory
     * @throws IOException
     *      if there is an error creating the server socket
     * 
     * @see ServerSocket#ServerSocket(int)
     */
    ServerSocket createServerSocket(int port) throws IOException;
}
