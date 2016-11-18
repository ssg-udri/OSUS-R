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
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.ose.remote.api.RemoteSettings;
import mil.dod.th.ose.utils.ClientSocketFactory;

import org.osgi.service.component.ComponentFactory;

/**
 * Implementation of the {@link mil.dod.th.core.remote.SocketChannel} interface for client sockets.  Instance created 
 * using a {@link org.osgi.service.component.ComponentFactory}.
 * 
 * @author Dave Humeniuk
 */
@Component(factory = ClientSocketChannel.FACTORY_NAME)
public class ClientSocketChannel extends AbstractSocketChannel
{
    /**
     * Name of the OSGi component factory, used for filtering.
     */
    public final static String FACTORY_NAME = "mil.dod.th.ose.remote.ClientSocketChannel";

    /**
     * Key for the property containing the host name for the socket.
     */
    public final static String HOST_PROP_KEY = "host";
    
    /**
     * Key for the property containing the port for the socket.
     */
    public final static String PORT_PROP_KEY = "port";

    /**
     * Factory to create {@link Socket}s.
     */
    private ClientSocketFactory m_ClientSocketFactory;

    /**
     * Factory for creating {@link Socket}s.
     * 
     * @param clientSocketFactory
     *      factory that creates {@link Socket}s
     */
    @Reference
    public void setClientSocketFactory(final ClientSocketFactory clientSocketFactory)
    {
        m_ClientSocketFactory = clientSocketFactory;
    }

    /**
     * Bind the factory that creates the socket message listeners.
     * 
     * @param factory
     *      factory that creates socket listeners
     */
    @Override
    @Reference(target = "(component.name=" + SocketMessageListener.FACTORY_NAME + ")")
    public void setSocketMessageListenerFactory(final ComponentFactory factory)
    {
        super.setSocketMessageListenerFactory(factory);
    }
    
    /**
     * Bind the factory that creates the message sender.
     * 
     * @param factory
     *      factory that creates {@link QueuedMessageSender}s
     */
    @Override
    @Reference(target = "(component.name=" + QueuedMessageSender.FACTORY_NAME + ")")
    public void setQueuedMessageSenderFactory(final ComponentFactory factory)
    {
        super.setQueuedMessageSenderFactory(factory);
    }
    
    @Override
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        super.setLoggingService(logging);
    }
    
    @Override
    @Reference
    public void setRemoteSettings(final RemoteSettings remoteSettings)
    {
        super.setRemoteSettings(remoteSettings);
    }

    @Override
    @Reference
    public void setPowerManager(final PowerManager powerManager)
    {
        super.setPowerManager(powerManager);
    }

    /**
     * Activate the component by creating the socket based on the input.
     * 
     * @param props
     *      properties of the component, namely {@link #HOST_PROP_KEY} and {@link #PORT_PROP_KEY}
     * @throws IOException
     *      if unable to connect to the given host and port
     */
    @Activate
    public void activate(final Map<String, Object> props) throws IOException
    {
        final String host = (String)props.get(HOST_PROP_KEY);
        final int port = (Integer)props.get(PORT_PROP_KEY);
        final Socket socket = m_ClientSocketFactory.createClientSocket(host, port);
        
        final Map<String, Object> matchProps = new HashMap<String, Object>();
        matchProps.put(HOST_PROP_KEY, host);
        matchProps.put(PORT_PROP_KEY, port);
        subActivate(socket, matchProps);
    }
    
    /**
     * Deactivate the channel by closing underlying socket. 
     * 
     * @throws InterruptedException
     *      if interrupted while waiting for thread to stop 
     */
    @Deactivate
    public void deactivate() throws InterruptedException
    {
        subDeactivate();   
    }
    
    @Override
    public String getHost()
    {
        return (String)getProperties().get(HOST_PROP_KEY);
    }

    @Override
    public int getPort()
    {
        return (Integer)getProperties().get(PORT_PROP_KEY);
    }
}
