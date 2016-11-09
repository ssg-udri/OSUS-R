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
package mil.dod.th.ose.remote;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.ose.utils.ServerSocketFactory;

import org.osgi.framework.BundleContext;

/**
 * Component will accept multiple connections from a configured port.  Will listen to each connected socket on a 
 * separate thread.
 * 
 * Ideally this component would use a designator class for configuration, but there is a bug in Felix SCR 1.6.0 
 * (FELIX-3090) where it ignore target filters in factory components.  This component had a target filter for a service
 * reference so it couldn't also have a designator that makes it a factory component.  For now, will use a framework
 * property, but could be updated.
 * 
 * @author Dave Humeniuk
 *
 */
@Component
public class ServerSocketMessageListener
{
    /**
     * Default port number for {@link #PORT_PROP_KEY} property.
     */
    public final static int DEFAULT_PORT = 4000;
    
    /**
     * Key for the property containing the port for listening for remote interface connections.
     */
    public final static String PORT_PROP_KEY = "mil.dod.th.ose.remote.socket.port";

    /**
     * How long to wait when joining on the server socket thread.
     */
    private final static int THREAD_JOIN_TIMEOUT = 1000;
    
    /**
     * Factory for creating server sockets.
     */
    private ServerSocketFactory m_ServerSocketFactory;
    
    /**
     * Service for looking up remote channels.
     */
    private RemoteChannelLookup m_RemoteChannelLookup;
    
    /**
     * Server socket used to accept connections.
     */
    private ServerSocket m_ServerSocket;

    /**
     * Service for logging messages.
     */
    private LoggingService m_Logging;
    
    /**
     * Thread that is accepting incoming connections.
     */
    private Thread m_ServerSocketThread;

    /**
     * Bind a factory for creating server sockets.
     * 
     * @param factory
     *      creates server sockets
     */
    @Reference
    public void setServerSocketFactory(final ServerSocketFactory factory)
    {
        m_ServerSocketFactory = factory;
    }
    
    /**
     * Bind the service for looking up remote channels and syncing them.
     * 
     * @param remoteChannelLookup
     *      service for looking up remote channels
     */
    @Reference
    public void setRemoteChannelLookup(final RemoteChannelLookup remoteChannelLookup)
    {
        m_RemoteChannelLookup = remoteChannelLookup;
    }
    
    /**
     * Binds the logging service for logging messages.
     * 
     * @param logging
     *            Logging service object
     */
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }
    
    /**
     * Activate this component by starting to listen on a configured port.
     * 
     * @param context
     *      bundle context used to get port property
     * 
     * @throws IOException
     *      if the component is unable to create a server socket to listen on
     */
    @Activate
    public void activate(final BundleContext context) throws IOException
    {
        // set port, use default if not set
        int serverPort = DEFAULT_PORT;
        
        if (context.getProperty(PORT_PROP_KEY) != null)
        {
            serverPort = Integer.parseInt(context.getProperty(PORT_PROP_KEY));
        }
            
        m_ServerSocket = m_ServerSocketFactory.createServerSocket(serverPort);

        m_Logging.info("Server socket listening on port %d for remote messages", m_ServerSocket.getLocalPort());
        
        m_ServerSocketThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                //run until the socket is closed
                while (true)
                {
                    try
                    {
                        final Socket socket = m_ServerSocket.accept(); // NOFORTIFY: unreleased resource:
                                                    //resources are managed by the remote channel lookup
                        m_Logging.info("Connection made from %s for remote interface", 
                                socket.getRemoteSocketAddress());
                        
                        m_RemoteChannelLookup.newServerSocketChannel(socket);
                    }
                    catch (final IOException e)
                    {
                        if (m_ServerSocket.isClosed())
                        {
                            m_Logging.info("Server socket has been closed, will stop accepting connections");
                            return;
                        }
                    }
                }
            }
        });
        
        m_ServerSocketThread.setName("RemoteInterfaceServerSocket");
        m_ServerSocketThread.start();
    }

    /**
     * Stop listening on the server socket port.
     */
    @Deactivate
    public void deactivate()
    {
        try
        {
            m_ServerSocket.close();
        }
        catch (final Exception e)
        {
            m_Logging.error(e, "Unable to close server socket");
        }
        
        try
        {
            m_ServerSocketThread.join(THREAD_JOIN_TIMEOUT);
        }
        catch (final InterruptedException e)
        {   
            m_Logging.info("Server socket interrupted while joining thread");
        }
    }
}
