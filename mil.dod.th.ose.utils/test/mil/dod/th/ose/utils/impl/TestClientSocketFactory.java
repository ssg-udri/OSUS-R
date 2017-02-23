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
package mil.dod.th.ose.utils.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.Before;
import org.junit.Test;

/**
 * Test the remote interface's client socket factory. 
 * @author callen
 *
 */
public class TestClientSocketFactory 
{
    private ClientSocketFactoryImpl m_SUT;
    
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new ClientSocketFactoryImpl();
    }

    /**
     * Test method for 
     * {@link mil.dod.th.ose.utils.impl.ClientSocketFactoryImpl#createClientSocket(String, int, boolean)}.
     * Test that a socket created with the factory can in fact connect as expected to another (Server) socket.
     */
    @Test
    public void testCreateClientSocket() throws IOException
    {
        //need server socket for the client to connect to
        ServerSocket server = new ServerSocket(0);
        int serverPort = server.getLocalPort();
        
        String host = "localhost";
        //connect using the same port as the server socket
        Socket socket = m_SUT.createClientSocket(host, serverPort, false);
        
        assertThat(socket.getPort(), is(serverPort));
        assertThat(socket.getInetAddress().getHostName(), is("localhost"));
        
        //cleanup
        server.close();
        socket.close();
    }
}
