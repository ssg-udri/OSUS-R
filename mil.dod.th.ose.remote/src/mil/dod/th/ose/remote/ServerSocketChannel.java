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

import org.osgi.service.component.ComponentFactory;

/**
 * Implementation of the {@link mil.dod.th.core.remote.SocketChannel} interface for server sockets.  Instance created 
 * using a {@link org.osgi.service.component.ComponentFactory}.
 * 
 * @author Dave Humeniuk
 *
 */
@Component(factory = ServerSocketChannel.FACTORY_NAME)
public class ServerSocketChannel extends AbstractSocketChannel
{
    /**
     * Name of the OSGi component factory, used for filtering.
     */
    public final static String FACTORY_NAME = "mil.dod.th.ose.remote.ServerSocketChannel";
    
    /**
     * Key for the property containing the underlying socket to use for sending messages.
     */
    public final static String SOCKET_PROP_KEY = "socket";
    
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
     * Activate the component by getting a reference to the connected socket.
     * 
     * @param props
     *      properties of the component, namely {@link #SOCKET_PROP_KEY}
     */
    @Activate
    public void activate(final Map<String, Object> props)
    {
        final Socket socket = (Socket)props.get(SOCKET_PROP_KEY);
        
        final Map<String, Object> matchProps = new HashMap<String, Object>();
        matchProps.put(SOCKET_PROP_KEY, socket);
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
        return getSocket().getInetAddress().getHostName();
    }
    
    @Override
    public int getPort()
    {
        return getSocket().getPort();
    }
}
