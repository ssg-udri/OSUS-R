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

import java.io.IOException;
import java.net.Socket;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import aQute.bnd.annotation.component.Component;

import mil.dod.th.ose.utils.ClientSocketFactory;


/**
 * Implementation of the {@link ClientSocketFactory}.
 * 
 * @author callen
 */
@Component
public class ClientSocketFactoryImpl implements ClientSocketFactory 
{
    private final static int SSL_HANDSHAKE_TIMEOUT_MILLIS = 10000;

    @Override
    public Socket createClientSocket(final String host, final int port) throws IOException
    {
        return createClientSocket(host, port, false);
    }

    @Override
    public Socket createClientSocket(final String host, final int port, final boolean useSsl) throws IOException
    {
        if (useSsl)
        {
            SSLSocket sslSocket = null;
            try
            {
                sslSocket = (SSLSocket)SSLContext.getDefault().getSocketFactory().createSocket(host, port);

                // Immediately initiate the handshake process to verify a valid connection before trying to send
                // real data.
                sslSocket.setSoTimeout(SSL_HANDSHAKE_TIMEOUT_MILLIS);
                sslSocket.startHandshake();
                sslSocket.setSoTimeout(0);

                return sslSocket;
            }
            catch (final Exception e)
            {
                if (sslSocket != null)
                {
                    // Make sure socket is closed when the SSL handshake fails
                    sslSocket.close();
                }
                throw new IOException(e);
            }
        }
        else
        {
            return new Socket(host, port);
        }
    }
}
