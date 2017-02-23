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
package mil.dod.th.ose.remote.integration;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Class is used to retrieve the host/port to use for connecting to the remote interface.  This allows the test to be
 * run on the same system or a different one.
 * 
 * @author Dave Humeniuk
 *
 */
public class SocketHostHelper
{
    /**
     * System property that holds the host to connect to the controller.
     */
    public static final String SOCKET_HOST_PROP_NAME = "mil.dod.th.ose.remote.host";
    
    /**
     * System property that holds the port to connect to the controller.
     */
    public static final String SOCKET_PORT_PROP_NAME = "mil.dod.th.ose.remote.port";
    
    public static String getHost()
    {
        String hostStr = System.getProperty(SOCKET_HOST_PROP_NAME);
        
        if (hostStr == null)
        {
            return "localhost";
        }
            
        return hostStr;
    }
    
    public static int getPort()
    {
        String portStr = System.getProperty(SOCKET_PORT_PROP_NAME);
        
        if (portStr == null)
        {
            return 4000;
        }
        
        return Integer.parseInt(portStr);
    }

    public static Socket connectToController() throws UnknownHostException, IOException
    {
        Socket socket = new Socket(getHost(), getPort());
        socket.setSoTimeout(4000);
        // Bouncy castle provider needs to be added to the security manager
        Security.addProvider(new BouncyCastleProvider());
        return socket;
    }

    public static Socket connectToControllerSsl() throws IOException, NoSuchAlgorithmException
    {
        final SSLSocket sslSocket =
                (SSLSocket)SSLContext.getDefault().getSocketFactory().createSocket(getHost(), getPort());

        // Immediately initiate the handshake process to verify a valid connection before trying to send
        // real data.
        sslSocket.startHandshake();

        return sslSocket;
    }
}
