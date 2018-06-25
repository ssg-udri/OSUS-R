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
package example.message.client;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace;
import mil.dod.th.core.remote.proto
    .AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace.AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetRequestData;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.system.TerraHarvestSystem;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import example.message.SocketListener;


/**
 * Example of a service using a custom routing system to network controllers.
 * 
 * @author jlatham
 *
 */
@Component(provide = ExampleMessageClient.class, designate = ExampleMessageClientConfig.class, 
    configurationPolicy = ConfigurationPolicy.optional, immediate = true)
public class ExampleMessageClient implements EventHandler
{
    private static final String SEND_ERROR = "Error sending message of type [%s] to remote controller at %s:%s";
    private static final String MESSAGE_SENT = "Message sent!";
    
    private TerraHarvestSystem m_System;
    private long m_SystemId;
    private String m_IpAddress;
    private int m_Port;
    private Socket m_Socket;
    private LoggingService m_Logging;
    private byte[] m_IdBytes;
    private SocketListener m_SocketListener;
    private long m_ServerSystemId;
    private AtomicInteger m_SequenceId;
    private ComponentFactory m_SocketListenerFactory;
    private ComponentInstance m_ComponentInstance;
    private ServiceRegistration<EventHandler> m_EventHandlerReg;
    
    
    @Reference
    public void setTerraHarvestSystem(final TerraHarvestSystem sys)
    {
        m_System = sys;
    }
    
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }
    
    @Reference(target = "(" + ComponentConstants.COMPONENT_FACTORY + "=" + SocketListener.FACTORY + ")")
    public void setSocketListenerFactory(final ComponentFactory factory)
    {
        m_SocketListenerFactory = factory;
    }
    
    /**
     * Activates the example message client.
     * 
     * @param context
     *      Bundle context the example message client is associated with.
     * @param props
     *      Properties to be passed to the example message client.
     */
    @Activate
    public void activate(final BundleContext context, final Map<String, Object> props)
    {
        final ExampleMessageClientConfig config = Configurable
                .createConfigurable(ExampleMessageClientConfig.class, props);
        m_IpAddress = config.ipAdress();
        m_Port = config.port();
        
        m_SystemId = m_System.getId();        
        m_IdBytes = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(m_SystemId).array();
        m_SequenceId = new AtomicInteger(0);
        
        m_EventHandlerReg = registerEvents(context);
        
        m_Logging.debug("Starting client. System Id: %s", m_SystemId);
    }
    
    /**
     * Deactivates the example message client.
     */
    @Deactivate
    public void deactivate()
    {
        m_EventHandlerReg.unregister();
        m_SocketListener.shutdown();
        m_ComponentInstance.dispose();
        m_SocketListener = null;
    }

    /**
     * Connects to the example message router at the specified address.
     * 
     * @param ipAddress
     *      IP address of the message router to connect to.
     * @param port
     *      Port number of the message router to connect to.
     */
    public void connect(String ipAddress, Integer port)
    {
        if (m_SocketListener == null)
        {
            if (ipAddress == null)
            {
                ipAddress = m_IpAddress;
            }
            
            if (port == null)
            {
                port = m_Port;
            }
            
            try
            {
                m_Socket = new Socket(ipAddress, port); 
                m_Socket.getOutputStream().write(m_IdBytes);
                
                final Dictionary<String, Object> props = new Hashtable<>();
                props.put(SocketListener.PROP_SOCKET, m_Socket);
                
                m_ComponentInstance = m_SocketListenerFactory.newInstance(props);
                m_SocketListener = (SocketListener)m_ComponentInstance.getInstance();
                
                final Thread thread = new Thread(m_SocketListener);
                thread.setName("SocketListener-server-systemId-" + m_ServerSystemId);
                thread.start();
            }
            catch (final UnknownHostException ex)
            {
                m_Logging.error(ex, "Unknown host exception for %s:%s", ipAddress, port);
            }
            catch (final IOException ex)
            {
                m_Logging.error(ex, "IOException on connection %s:%s", ipAddress, port);
            }     
        }
        else
        {
            m_Logging.info("Client already connected. Disconnect first.");
        }
    }
    
    /**
     * Disconnect the client from the server.
     */
    public void disconnect()
    {
        if (m_SocketListener != null)
        {
            m_Logging.info("Disconnecting client from message router server.");
            m_SocketListener.shutdown();
            m_ComponentInstance.dispose();
            m_SocketListener = null;
            m_Logging.info("Client successfully disconnected from server.");
        }
        else
        {
            m_Logging.info("Client is not currently connected.");
        }
    }
    
    /**
     * Sends a request controller info message to the system with the specified ID.
     * 
     * @param destId
     *      ID of the system the get assets message should be sent to.
     */
    public void sendTestMessage(final long destId)
    {
        m_Logging.debug("Sending RequestControllerInfo message to remote controller at %s:%s", 
                m_Socket.getInetAddress(), m_Socket.getPort());
        final TerraHarvestMessage.Builder thmsgBuilder = TerraHarvestMessage.newBuilder();
        
        final TerraHarvestPayload.Builder payloadBuilder = TerraHarvestPayload.newBuilder();
        payloadBuilder.setNamespace(Namespace.Base);
        payloadBuilder.setNamespaceMessage(
                BaseNamespace.newBuilder()
                .setType(BaseNamespace.BaseMessageType.RequestControllerInfo)
                .build()
                .toByteString());
                
        thmsgBuilder.setDestId((int)destId);
        thmsgBuilder.setMessageId(m_SequenceId.getAndIncrement());
        thmsgBuilder.setSourceId((int)m_SystemId);
        thmsgBuilder.setVersion(RemoteConstants.SPEC_VERSION);
        thmsgBuilder.setTerraHarvestPayload(payloadBuilder.build().toByteString());
        
        final TerraHarvestMessage msg = thmsgBuilder.build();
        
        try
        {            
            msg.writeDelimitedTo(m_Socket.getOutputStream());
            m_Logging.debug(MESSAGE_SENT);
        }
        catch (final IOException ex)
        {
            m_Logging.error(ex, SEND_ERROR, 
                    BaseMessageType.RequestControllerInfo, m_Socket.getInetAddress(), m_Socket.getPort());
        }
        
    }
    
    /**
     * Method that sends a get assets message to the system with the specified ID.
     * 
     * @param destId
     *      ID of the system the get assets message should be sent to.
     */
    public void sendGetAssetsMessage(final long destId)
    {
        m_Logging.debug("Sending GetAssetsRequest message to remote controller at %s:%s", m_Socket.getInetAddress(), 
                m_Socket.getPort());
        final TerraHarvestMessage.Builder thmsgbuilder = TerraHarvestMessage.newBuilder();
        
        final TerraHarvestPayload.Builder payloadBuilder = TerraHarvestPayload.newBuilder();
        payloadBuilder.setNamespace(Namespace.AssetDirectoryService);
        payloadBuilder.setNamespaceMessage(
                AssetDirectoryServiceNamespace.newBuilder()
                .setType(AssetDirectoryServiceMessageType.GetAssetsRequest)
                .build()
                .toByteString());
        
        thmsgbuilder.setDestId((int)destId);
        thmsgbuilder.setMessageId(m_SequenceId.getAndIncrement());
        thmsgbuilder.setSourceId((int)m_SystemId);
        thmsgbuilder.setVersion(RemoteConstants.SPEC_VERSION);
        thmsgbuilder.setTerraHarvestPayload(payloadBuilder.build().toByteString());
        
        final TerraHarvestMessage msg = thmsgbuilder.build();
        
        try
        {
            msg.writeDelimitedTo(m_Socket.getOutputStream());
            m_Logging.debug(MESSAGE_SENT);
        }
        catch (final IOException ex)
        {
            m_Logging.error(ex, SEND_ERROR,
                    AssetDirectoryServiceMessageType.GetAssetsRequest, m_Socket.getInetAddress(), m_Socket.getPort());
        }
    }
    
    /**
     * Method that sends a create asset message to the system with the specified ID. This method assumes that the 
     * receiving system has the example.asset.ExampleAsset factory available.
     * 
     * @param destId
     *      ID of the system the create asset message should be sent to.
     */
    public void sendCreateAssetMessage(final long destId)
    {
        final CreateAssetRequestData createAsset = CreateAssetRequestData.newBuilder()
                .setProductType("example.asset.ExampleAsset")
                .setName("TestAsset").build();
        final AssetDirectoryServiceNamespace namespace = AssetDirectoryServiceNamespace.newBuilder().setType(
                AssetDirectoryServiceMessageType.CreateAssetRequest).setData(createAsset.toByteString()).build();
        final TerraHarvestPayload payload = TerraHarvestPayload.newBuilder()
                .setNamespace(Namespace.AssetDirectoryService)
                .setNamespaceMessage(namespace.toByteString()).build();
        final TerraHarvestMessage thMessage = TerraHarvestMessage.newBuilder()
                .setDestId((int)destId)
                .setSourceId((int)m_SystemId)
                .setMessageId(m_SequenceId.getAndIncrement()).setTerraHarvestPayload(payload.toByteString())
                .setVersion(RemoteConstants.SPEC_VERSION).build();
        
        try
        {
            thMessage.writeDelimitedTo(m_Socket.getOutputStream());
            m_Logging.debug(MESSAGE_SENT);
        }
        catch (final IOException ex)
        {
            m_Logging.error(ex, SEND_ERROR,
                    AssetDirectoryServiceMessageType.CreateAssetRequest, m_Socket.getInetAddress(), m_Socket.getPort());
        }        
    }

    @Override
    public void handleEvent(final Event event)
    {
        if (event.getTopic().equals(RemoteConstants.TOPIC_MESSAGE_RECEIVED))
        {
            m_Logging.debug("===== EVENT MESSAGE RECEIVED =====");
            m_Logging.debug("Received a message of type [%s]", 
                    event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE));
            m_Logging.debug("Response [%s]", event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_RESPONSE));
        }
    }

    /**
     * Method used to register the event handler to listener for message received events.
     * 
     * @param context
     *      Bundle context the event handler should be registered with.
     * @return
     *      The service registration that represents the event handler.
     */
    private ServiceRegistration<EventHandler> registerEvents(final BundleContext context)
    {
        m_Logging.debug("Registering for event [%s]", RemoteConstants.TOPIC_MESSAGE_RECEIVED);
        
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(EventConstants.EVENT_TOPIC, RemoteConstants.TOPIC_MESSAGE_RECEIVED);
        
        return context.registerService(EventHandler.class, this, props);
    }
}
