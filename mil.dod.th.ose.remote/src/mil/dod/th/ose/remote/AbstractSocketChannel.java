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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.core.remote.ChannelStatus;
import mil.dod.th.core.remote.SocketChannel;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.types.remote.RemoteChannelTypeEnum;
import mil.dod.th.ose.remote.api.RemoteSettings;

import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;

/**
 * Partial Implementation of the {@link SocketChannel}.  There is a full implementation for client and server sockets
 * as they both work a little differently.
 * 
 * @author Dave Humeniuk
 *
 */
abstract class AbstractSocketChannel extends AbstractRemoteChannel implements SocketChannel
{
    /**
     * Factory for creating {@link SocketMessageListener} components.
     */
    private ComponentFactory m_SocketMessageListenerFactory;
    
    /**
     * Service for logging messages.
     */
    private LoggingService m_Logging;

    /**
     * Socket used to send message.
     */
    private Socket m_Socket;

    /**
     * The status of the channel. Used to denote if the channel is available, unavailable, or unknown.
     */
    private ChannelStatus m_Status;

    /**
     * Keep track of the {@link SocketMessageListener} instance for later disposal.
     */
    private ComponentInstance m_ListenerInstance;

    /**
     * Thread that the listener runs on.
     */
    private Thread m_ListenerThread;

    /**
     * Running count of bytes transmitted on this channel.
     */
    private long m_BytesTransmitted;

    /**
     * Running count of bytes received on this channel.
     */
    private long m_BytesReceived;

    /**
     * Service contains current settings from config admin.
     */
    private RemoteSettings m_RemoteSettings;

    /**
     * Service used for power management.
     */
    private PowerManager m_PowerManager;

    /**
     * Wake lock used to keep the system awake while the socket channel is active.
     */
    private WakeLock m_WakeLock;

    /**
     * Default constructor.
     */
    AbstractSocketChannel()
    {
        super(RemoteChannelTypeEnum.SOCKET);
    }
    
    /**
     * Bind the factory that creates the socket message listeners.
     * 
     * @param factory
     *      factory that creates socket listeners
     */
    public void setSocketMessageListenerFactory(final ComponentFactory factory)
    {
        m_SocketMessageListenerFactory = factory;
    }
    
    /**
     * Binds the logging service for logging messages.
     * 
     * @param logging
     *            Logging service object
     */
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }
    
    /**
     * Bind the settings for the remote interface.
     * 
     * @param remoteSettings
     *      interface containing remote settings from config admin
     */
    public void setRemoteSettings(final RemoteSettings remoteSettings)
    {
        m_RemoteSettings = remoteSettings;
    }
    
    /**
     * Binds the power manager service.
     * 
     * @param powerManager
     *            Power manager service object
     */
    public void setPowerManager(final PowerManager powerManager)
    {
        m_PowerManager = powerManager;
    }

    /**
     * Activate the component by getting a reference to the connected socket.
     *
     * @param socket
     *      socket used by this channel to send/receive data
     * @param matchProps
     *      properties of the component that will be used by {@link #matches(Map)} (exclude others)
     */
    public void subActivate(final Socket socket, final Map<String, Object> matchProps)
    {
        m_Socket = socket;

        m_WakeLock = m_PowerManager.createWakeLock(getClass(),
                String.format("remoteSocket:%s", m_Socket.getRemoteSocketAddress()));

        if (m_RemoteSettings.isPreventSleepModeEnabled())
        {
            m_WakeLock.activate();
        }

        m_Logging.info("Activated remote channel for socket (%s)", m_Socket.getRemoteSocketAddress());

        initMessageSender(matchProps);

        //assign initial channel status
        m_Status = ChannelStatus.Unknown;
        
        // create message listener for this channel and run it on a thread
        final Dictionary<String, Object> listenerProps = new Hashtable<String, Object>();
        listenerProps.put(SocketMessageListener.SOCKET_PROP_KEY, m_Socket);
        listenerProps.put(SocketMessageListener.CHANNEL_PROP_KEY, this);
        m_ListenerInstance = m_SocketMessageListenerFactory.newInstance(listenerProps);
        
        // now that sending is setup through subclass, start listener
        final SocketMessageListener listener = (SocketMessageListener)m_ListenerInstance.getInstance();
        m_ListenerThread = new Thread(listener);
        m_ListenerThread.setName(m_Socket.getRemoteSocketAddress() + "-MessageListener");
        m_ListenerThread.start();
    }
    
    /**
     * Deactivate the channel by closing underlying socket. 
     * 
     * @throws InterruptedException
     *      if interrupted while waiting for thread to stop 
     */
    public void subDeactivate() throws InterruptedException
    {
        m_ListenerInstance.dispose();
        
        final int ThreadWaitMs = 1000;
        m_ListenerThread.join(ThreadWaitMs);
        assert !m_ListenerThread.isAlive();
        
        cleanupMessageSender();
        
        try
        {
            m_Socket.getOutputStream().close();
        }
        catch (final IOException e)
        {
            m_Logging.warning("Failed to close socket output stream (%s)", m_Socket.getRemoteSocketAddress());
        }

        try
        {
            m_Socket.close();
        }
        catch (final IOException e)
        {
            m_Logging.debug("Unable to close socket (%s), already closed? %b", m_Socket.getRemoteSocketAddress(), 
                    m_Socket.isClosed());
        }
        finally
        {
            m_WakeLock.delete();
        }
    }
    
    @Override
    public boolean trySendMessage(final TerraHarvestMessage message)
    {
        try
        {
            synchronized (this) 
            {
                message.writeDelimitedTo(m_Socket.getOutputStream());
            }
            m_BytesTransmitted += message.getSerializedSize();
            m_Status = ChannelStatus.Active;
        }
        catch (final IOException e)
        {
            m_Logging.debug("Failed to send message to socket [%s]", m_Socket.getRemoteSocketAddress());
            m_Status = ChannelStatus.Unavailable;
            return false;
        }

        if (m_RemoteSettings.isLogRemoteMessagesEnabled())
        {
            m_Logging.debug("Remote message written to socket [%s]%n%s", m_Socket.getRemoteSocketAddress(), message);
        }

        return true;
    }
    
    @Override
    public ChannelStatus getStatus()
    {
        return m_Status;
    }
    
    @Override
    public String toString()
    {
        return String.format("%s:%d", getHost(), getPort());
    }
    
    @Override
    public long getBytesTransmitted()
    {
        return m_BytesTransmitted;
    }
    
    @Override
    public long getBytesReceived()
    {
        return m_BytesReceived;
    }
    
    /**
     * Get the socket for this channel.
     * 
     * @return
     *      the underlying socket
     */
    protected Socket getSocket()
    {
        return m_Socket;
    }
    
    /**
     * Updates the byte received.  Should be called by the component that reads messages.
     * 
     * @param newValue
     *      new value for bytes received
     */
    public void setBytesReceived(final long newValue)
    {
        m_BytesReceived = newValue;
    }
}
