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
package mil.dod.th.ose.remote.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.ChannelStatus;
import mil.dod.th.core.remote.TransportChannel;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.types.remote.RemoteChannelTypeEnum;
import mil.dod.th.ose.remote.AbstractRemoteChannel;
import mil.dod.th.ose.remote.QueuedMessageSender;
import mil.dod.th.ose.remote.api.RemoteSettings;

import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;

/**
 * Implementation of the {@link TransportChannel} for {@link mil.dod.th.core.ccomm.transport.TransportLayer}s.  Instance
 * created using a {@link org.osgi.service.component.ComponentFactory}.  The channel will send remote messages over the
 * <code>TransportLayer</code> from a local system to a single remote system.
 * 
 * @author Dave Humeniuk
 *
 */
@Component(factory = TransportChannelImpl.FACTORY_NAME)
public class TransportChannelImpl extends AbstractRemoteChannel implements TransportChannel
{
    /**
     * Name of the OSGi component factory, used for filtering.
     */
    public final static String FACTORY_NAME = "mil.dod.th.ose.remote.transport.TransportChannelImpl";

    /**
     * Component property key containing the name of transport layer that is being listened to. 
     */
    public static final String TRANSPORT_NAME_PROP_KEY = "transport.name";
    
    /**
     * Component property key containing the string representation of the address for the local endpoint. 
     */
    public static final String LOCAL_ADDRESS_PROP_KEY = "local.address";
    
    /**
     * Component property key containing the string representation of the address for the remote endpoint. 
     */
    public static final String REMOTE_ADDRESS_PROP_KEY = "remote.address";

    /**
     * Service for logging messages.
     */
    private LoggingService m_Logging;

    /**
     * Capabilities for the transport layer being used.
     */
    private TransportLayerCapabilities m_TransportCapabilities;

    /**
     * The status of the channel. Used to denote if the channel is available, unavailable, or unknown.
     */
    private ChannelStatus m_Status;
    
    /**
     * Service to get the transport layer object based on a given transport name.
     */
    private CustomCommsService m_CustomCommsService;
    
    /**
     * Service to get the address object based on a given address name.
     */
    private AddressManagerService m_AddressManagerService;
    
    /**
     * Factory for creating {@link TransportMessageListener} components.
     */
    private ComponentFactory m_TransportMessageListenerFactory;

    /**
     * Instance of the {@link TransportMessageListener} for this channel.
     */
    private ComponentInstance m_ListenerInstance;
    
    /**
     * Service contains current settings from config admin.
     */
    private RemoteSettings m_RemoteSettings;

    /**
     * Default constructor.
     */
    public TransportChannelImpl()
    {
        super(RemoteChannelTypeEnum.TRANSPORT);
    }

    /**
     * Binds the custom communications service with this transport message listener.  
     * @param commService
     *      service to use for obtaining the transport layer object for syncing channels
     */
    @Reference
    public void setCustomCommsService(final CustomCommsService commService)
    {
        m_CustomCommsService = commService;
    }
    
    /**
     * Binds the {@link AddressManagerService} service with this transport message listener.  
     * @param addressManager
     *      service to use for obtaining the address object for syncing channels
     */
    @Reference
    public void setAddressManagerService(final AddressManagerService addressManager)
    {
        m_AddressManagerService = addressManager;
    }    
    
    /**
     * Bind the factory that creates the transport message listeners.
     * 
     * @param factory
     *      factory that creates transport listeners
     */
    @Reference(target = "(component.factory=" + TransportMessageListener.FACTORY_NAME + ")")
    public void setTransportMessageListenerFactory(final ComponentFactory factory)
    {
        m_TransportMessageListenerFactory = factory;
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
     * Bind the settings for the remote interface.
     * 
     * @param remoteSettings
     *      interface containing remote settings from config admin
     */
    @Reference
    public void setRemoteSettings(final RemoteSettings remoteSettings)
    {
        m_RemoteSettings = remoteSettings;
    }
    
    /**
     * Activate the component by getting a reference to the transport layer and address.
     * 
     * @param props
     *      properties of the component, namely {@link #TRANSPORT_NAME_PROP_KEY}, {@link #LOCAL_ADDRESS_PROP_KEY} and
     *      {@link #REMOTE_ADDRESS_PROP_KEY}
     */
    @Activate
    public void activate(final Map<String, Object> props)
    {
        final String transportName = (String)props.get(TRANSPORT_NAME_PROP_KEY);
        final String localMessageAddress = (String)props.get(LOCAL_ADDRESS_PROP_KEY);
        final String remoteMessageAddress = (String)props.get(REMOTE_ADDRESS_PROP_KEY);

        final TransportLayer transport = getTransportLayer(transportName);
        if (transport != null)
        {
            m_TransportCapabilities = transport.getFactory().getTransportLayerCapabilities();
        }

        m_Logging.info("Activated remote channel for transport layer [%s] from [%s] to [%s]", transportName, 
                localMessageAddress, remoteMessageAddress); 

        final Map<String, Object> matchProps = new HashMap<String, Object>();
        matchProps.put(TRANSPORT_NAME_PROP_KEY, transportName);
        matchProps.put(LOCAL_ADDRESS_PROP_KEY, localMessageAddress);
        matchProps.put(REMOTE_ADDRESS_PROP_KEY, remoteMessageAddress);
        initMessageSender(matchProps);

        //assign initial channel status
        if (m_TransportCapabilities != null && m_TransportCapabilities.isConnectionOriented()
                && transport.isConnected())
        {
            m_Status = ChannelStatus.Active;
        }
        else
        {
            m_Status = ChannelStatus.Unknown;
        }
        
        // create message listener for this channel and run it on a thread
        final Dictionary<String, Object> listenerProps = new Hashtable<String, Object>();
        listenerProps.put(TransportMessageListener.CHANNEL_PROP_KEY, this);
        m_ListenerInstance = m_TransportMessageListenerFactory.newInstance(listenerProps);
        
        // now that sending is setup through subclass, start listener
        final TransportMessageListener listener = (TransportMessageListener)m_ListenerInstance.getInstance();
        final Thread thread = new Thread(listener);
        thread.start();
    }

    /**
     * Deactivate the component by disposing the underlying message listener.
     */
    @Deactivate
    public void deactivate()
    {
        // TODO: TH-1279. Should interrupt the thread running the listener instance.
        m_ListenerInstance.dispose();

        if (m_TransportCapabilities != null && m_TransportCapabilities.isConnectionOriented())
        {
            final TransportLayer transport = getTransportLayer(getTransportLayerName());
            if (transport != null && transport.isConnected())
            {
                try
                {
                    transport.disconnect();
                }
                catch (final Exception e)
                {
                    m_Logging.warning(e, "Error trying to disconnect transport layer during deactivation");
                }
            }
        }
    }

    @Override
    public boolean trySendMessage(final TerraHarvestMessage message)
    {
        final TransportLayer transport = getTransportLayer(getTransportLayerName());
        if (transport == null)
        {
            return false;
        }
        else
        {
            m_TransportCapabilities = transport.getFactory().getTransportLayerCapabilities();
        }

        final Address remoteAddress;
        try
        {
            remoteAddress = m_AddressManagerService.getOrCreateAddress(getRemoteMessageAddress());
        }
        catch (final CCommException e)
        {
            setStatusAndLog(ChannelStatus.Unavailable, 
                    String.format("Failed to send remote message to address [%s], address not valid",
                            getRemoteMessageAddress()));
            return false;
        }
        
        if (!transport.isAvailable(remoteAddress))
        {
            if (m_TransportCapabilities.isConnectionOriented())
            {
                
                final boolean connectionSuccessful = connectToRemoteAddress(transport, remoteAddress);
                
                if (!connectionSuccessful)
                {
                    return false;
                }
            }
            else
            {
                setStatusAndLog(ChannelStatus.Unavailable, 
                        String.format("Failed to send remote message to address [%s], endpoint not available", 
                                getRemoteMessageAddress()));
                return false;
            }
        }
        
        final boolean messageSent = sendMessage(message, transport, remoteAddress);
                
        if (m_RemoteSettings.isLogRemoteMessagesEnabled() && messageSent)
        {
            m_Logging.debug("Remote message sent to address [%s]%n%s", getRemoteMessageAddress(), message);
        }
        
        return messageSent;
    }

   /**
    * Sets the channel status and logs reason for status change.
    * @param status
    *   the new {@link ChannelStatus} to set for this transport channel
    * @param logMessage
    *   the reason for the status change
    */
    private void setStatusAndLog(final ChannelStatus status, final String logMessage)
    {
        m_Status = status;
        m_Logging.debug(logMessage);
    }

    /**
     * Sends a {@link TerraHarvestMessage} via a transport layer to the specified remote address.
     * The transport layer can be either a connection oriented or non-connection oriented layer. 
     * @param message
     *  the {@link TerraHarvestMessage} to send
     * @param transport
     *  the {@link TransportLayer} to use for sending
     * @param remoteAddress
     *  the remote {@link Address} to send the message to
     * @return
     *  true if the message sending was successful
     */
    private boolean sendMessage(final TerraHarvestMessage message, final TransportLayer transport,
            final Address remoteAddress)
    {
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try
        {
            message.writeDelimitedTo(outStream);
        }
        catch (final IOException e)
        {
            // this should never happen has stream used never throws IOException
            throw new IllegalStateException("Unable to write to byte array", e);
        }
        
        try
        {
            if (m_TransportCapabilities.isConnectionOriented())
            {
                transport.send(ByteBuffer.wrap(outStream.toByteArray()));
            }
            else
            {
                transport.send(ByteBuffer.wrap(outStream.toByteArray()), remoteAddress);
            }
        }
        catch (final IllegalStateException e)
        {
            setStatusAndLog(ChannelStatus.Unknown, 
                    String.format("Tried to send remote message with transport [%s] in unknown state, %s", 
                            transport.getName(), e.getMessage()));
            return false;
        }
        catch (final CCommException e)
        {
            setStatusAndLog(ChannelStatus.Unavailable, 
                    String.format("Failed to send remote message to address [%s], %s", getRemoteMessageAddress(), 
                            e.getMessage()));
            return false;
        }
        
        return true;
    }

    /**
     * Connect or reconnect to a remote address.
     * @param transport
     *  transport layer to connect to remote address
     * @param remoteAddress
     *  remote address to connect to
     * @return
     *  true if the connection/re-connection was successful
     */
    private boolean connectToRemoteAddress(final TransportLayer transport, final Address remoteAddress)
    {
        // Try to make a new connection or reconnect
        try
        {
            transport.connect(remoteAddress);
            setStatusAndLog(ChannelStatus.Active, String.format("Successfully connected to [%s]", 
                    getRemoteMessageAddress()));
        }
        catch (final IllegalStateException e)
        {
            m_Status = ChannelStatus.Active;
            m_Logging.warning(e, "Tried to make a connection to [%s] that was already established",
                getRemoteMessageAddress());
        }
        catch (final CCommException e)
        {
            setStatusAndLog(ChannelStatus.Unavailable, String.format("Unable to connect to [%s]", 
                    getRemoteMessageAddress()));
            return false;
        }
        
        return true;
    }

    @Override
    public ChannelStatus getStatus()
    {
        return m_Status;
    }
    
    @Override
    public long getBytesReceived()
    {
        // TODO: TH-1122: Implement method
        return 0;
    }
    
    @Override
    public long getBytesTransmitted()
    {
        // TODO: TH-1122: Implement method
        return 0;
    }
    
    @Override
    public String getTransportLayerName()
    {
        return (String)getProperties().get(TRANSPORT_NAME_PROP_KEY);
    }
    
    @Override
    public String getLocalMessageAddress()
    {
        return (String)getProperties().get(LOCAL_ADDRESS_PROP_KEY);
    }
    
    @Override
    public String getRemoteMessageAddress()
    {
        return (String)getProperties().get(REMOTE_ADDRESS_PROP_KEY);
    }

    @Override
    public String toString()
    {
        return String.format("%s;remote=%s;local=%s;connectionOriented=%s", getTransportLayerName(),
                getRemoteMessageAddress(), getLocalMessageAddress(),
                m_TransportCapabilities == null ? "?" : m_TransportCapabilities.isConnectionOriented());
    }

    /**
     * Retrieves the transport layer from custom comms service, if available.
     * 
     * @param transportName
     *      Name of the transport layer to retrieve
     * @return
     *      Transport layer if it exists, null otherwise
     */
    private TransportLayer getTransportLayer(final String transportName)
    {
        TransportLayer transport = null;
        try
        {
            transport = m_CustomCommsService.getTransportLayer(transportName);
        }
        catch (final IllegalArgumentException e)
        {
            setStatusAndLog(ChannelStatus.Unavailable, 
                    String.format("Failed to retrieve transport layer [%s], not found", transportName));
        }

        return transport;
    }
}
