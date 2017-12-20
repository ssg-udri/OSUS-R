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
package mil.dod.th.remote.client.impl;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Dictionary;
import java.util.Hashtable;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace
    .AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetAssetsResponseData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.DataStreamServiceNamespace;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.DataStreamServiceNamespace.DataStreamServiceMessageType;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.GetStreamProfilesResponseData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.StreamProfile;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.SharedMessages.FactoryObjectInfo;
import mil.dod.th.remote.client.ChannelStateCallback;
import mil.dod.th.remote.client.MessageListenerCallback;
import mil.dod.th.remote.client.MessageListenerService;
import mil.dod.th.remote.client.MessageSenderService;
import mil.dod.th.remote.client.RemoteMessage;
import mil.dod.th.remote.client.generate.AssetDirectoryMessageGenerator;
import mil.dod.th.remote.client.generate.DataStreamServiceMessageGenerator;
import mil.dod.th.remote.client.generate.EventAdminMessageGenerator;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen.LexiconFormat;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * Provides a set of commands that can be used from the Gogo shell to interact with the remote client API.
 * 
 * @author dlandoll
 */
@Component(provide = Commands.class,
    properties = {
        "osgi.command.scope=riclient",
        "osgi.command.function=connect|disconnect|sendEventRegister|sendEventUnregister|sendClearEventRegs"
            + "|sendGetAssets"
    })
public class Commands
{
    /**
     * Constant used for the controller ID being connected to.
     */
    private final static int CONTROLLER_ID = 0;

    private MessageListenerService m_MessageListenerService;
    private MessageSenderService m_MessageSenderService;
    private AssetDirectoryMessageGenerator m_AssetDirMessageGen;
    private DataStreamServiceMessageGenerator m_DataStreamMessageGen;
    private EventAdminMessageGenerator m_EventAdminMessageGen;
    private AssetDirectoryMessageCallback m_AssetDirMessageCallback;
    private DataStreamServiceMessageCallback m_DataStreamMessageCalback;
    private Socket m_Socket;
    private ServiceRegistration<EventHandler> m_EventHandlerReg;
    private BundleContext m_Context;

    @Reference
    public void setMessageListenerService(final MessageListenerService messageListenerService)
    {
        m_MessageListenerService = messageListenerService;
    }

    @Reference
    public void setMessageSenderService(final MessageSenderService messageSenderService)
    {
        m_MessageSenderService = messageSenderService;
    }

    @Reference
    public void setAssetDirMessageGen(final AssetDirectoryMessageGenerator assetDirMessageGen)
    {
        m_AssetDirMessageGen = assetDirMessageGen;
    }
    
    @Reference
    public void setDataStreamMessageGen(final DataStreamServiceMessageGenerator dataStreamMessageGen)
    {
        m_DataStreamMessageGen = dataStreamMessageGen;
    }

    @Reference
    public void setEventAdminMessageGen(final EventAdminMessageGenerator eventAdminMessageGen)
    {
        m_EventAdminMessageGen = eventAdminMessageGen;
    }

    /**
     * Activates this component.
     * 
     * @param context
     *      bundle context reference
     */
    @Activate
    public void activate(final BundleContext context)
    {
        m_Context = context;
    }

    /**
     * Create a socket connection with a controller and add in/out streams to {@link MessageListenerService} and
     * {@link MessageSenderService}.
     * 
     * @param session
     *      Provides access to the Gogo shell session
     * @param host
     *      Host name to connect to
     * @param port
     *      Port number assigned to the controller remote interface
     * @throws UnknownHostException
     *      if IP address of host not found
     * @throws IOException
     *      if error occurs creating socket
     */
    @Descriptor("Connect to a controller")
    public void connect(final CommandSession session,
            @Descriptor("Controller host name") final String host,
            @Descriptor("Controller port number") final Integer port) throws UnknownHostException, IOException
    {
        final Socket socket = new Socket(host, port);
        try
        {
            final PrintStream out = session.getConsole();

            m_MessageListenerService.addRemoteChannel(CONTROLLER_ID, socket.getInputStream(),
                new ListenerChannelCallback(out));

            // TD: Update to make the client ID configurable
            m_MessageSenderService.setClientId(1);

            m_MessageSenderService.addRemoteChannel(CONTROLLER_ID, socket.getOutputStream(), null);
        }
        catch (final Exception ex)
        {
            socket.close();
            throw ex;
        }

        m_Socket = socket;
    }

    /**
     * Disconnect from the currently connected controller and remove streams from {@link MessageListenerService} and
     * {@link MessageSenderService}.
     * 
     * @throws IOException
     *      if error occurs when closing the socket
     */
    @Descriptor("Disconnect from the controller")
    public void disconnect() throws IOException
    {
        m_MessageSenderService.removeRemoteChannel(CONTROLLER_ID);
        m_MessageListenerService.removeRemoteChannel(CONTROLLER_ID);

        if (!m_Socket.isClosed())
        {
            m_Socket.close();
        }

        if (m_AssetDirMessageCallback != null)
        {
            m_MessageListenerService.unregisterCallback(m_AssetDirMessageCallback);
            m_AssetDirMessageCallback = null; // NOPMD: Reference is no longer valid, so reset to null
        }
        
        if (m_DataStreamMessageCalback != null)
        {
            m_MessageListenerService.unregisterCallback(m_DataStreamMessageCalback);
            m_DataStreamMessageCalback = null; // NOPMD: Reference is no longer valid, so reset to null
        }

        if (m_EventHandlerReg != null)
        {
            m_EventHandlerReg.unregister();
            m_EventHandlerReg = null; // NOPMD: Reference is no longer valid, so reset to null
        }
    }

    /**
     * Send an event registration request message for observation events. The {@link EventAdminMessageListener}
     * component will automatically receive the event messages and post them as local OSGi events.
     * 
     * @param session
     *      Provides access to the Gogo shell session
     * @throws IOException
     *      if there is an error sending the message
     */
    @Descriptor("Send event registration request message to receive observations")
    public void sendEventRegister(final CommandSession session) throws IOException
    {
        final PrintStream out = session.getConsole();
        final EventHandler eventHandler = new EventHandler()
        {
            @Override
            public void handleEvent(final Event event)
            {
                out.println("Received event: " + event.getTopic());
                for (String name : event.getPropertyNames())
                {
                    if (!name.equals(EventConstants.EVENT_TOPIC))
                    {
                        out.println(event.getProperty(name));
                    }
                }
            }
        };

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(EventConstants.EVENT_TOPIC,
            new String[] {ObservationStore.TOPIC_OBSERVATION_PERSISTED_WITH_OBS,
                ObservationStore.TOPIC_OBSERVATION_MERGED_WITH_OBS});
        m_EventHandlerReg = m_Context.registerService(EventHandler.class, eventHandler, props);

        m_EventAdminMessageGen.createEventRegRequest()
            .setCanQueueEvent(true)
            .setExpirationTimeHours(RemoteConstants.REMOTE_EVENT_DEFAULT_REG_TIMEOUT_HOURS)
            .setObjectFormat(LexiconFormat.Enum.XML)
            .setTopics(ObservationStore.TOPIC_OBSERVATION_PERSISTED_WITH_OBS,
                ObservationStore.TOPIC_OBSERVATION_MERGED_WITH_OBS)
            .send(CONTROLLER_ID);
    }

    /**
     * Send a clear event registrations request message to remove all event registrations on the controller.
     * 
     * @throws IOException
     *      if there is an error sending the message
     */
    @Descriptor("Send a clear event registrations request")
    public void sendClearEventRegs() throws IOException
    {
        m_EventAdminMessageGen.createEventCleanupRequest().send(CONTROLLER_ID);
    }

    /**
     * Send a get assets request message to get a list of assets that currently exist on the controller.
     * 
     * @param session
     *      Provides access to the Gogo shell session
     * @throws IOException
     *      if there is an error sending the message
     */
    @Descriptor("Send a get assets request")
    public void sendGetAssets(final CommandSession session) throws IOException
    {
        if (m_AssetDirMessageCallback == null)
        {
            final PrintStream out = session.getConsole();
            m_AssetDirMessageCallback = new AssetDirectoryMessageCallback(out);
            m_MessageListenerService.registerCallback(Namespace.AssetDirectoryService, m_AssetDirMessageCallback);
        }

        m_AssetDirMessageGen.createGetAssetsRequest().send(CONTROLLER_ID);
    }
    
    /**
     * Send a GetStreamProfiles request message to get a list of stream profile instances that currently
     * exist on the controller.
     * 
     * @param session
     *      provides access to the Gogo shell session
     * @throws IOException
     *      if there is an error sending the message
     */
    @Descriptor("Send a GetStreamProfiles request")
    public void sendGetStreamProfiles(final CommandSession session) throws IOException
    {
        if (m_DataStreamMessageCalback == null)
        {
            final PrintStream out = session.getConsole();
            m_DataStreamMessageCalback = new DataStreamServiceMessageCallback(out);
            m_MessageListenerService.registerCallback(Namespace.DataStreamService, m_DataStreamMessageCalback);
        }
        
        m_DataStreamMessageGen.createGetStreamProfilesRequest().send(CONTROLLER_ID);
    }

    /**
     * Channel state callback handler used to notify when the channel has been removed.
     */
    private class ListenerChannelCallback implements ChannelStateCallback
    {
        private final PrintStream m_Out;

        /**
         * Creates a channel state callback handler.
         * 
         * @param out
         *      Print stream for the Gogo shell
         */
        ListenerChannelCallback(final PrintStream out)
        {
            m_Out = out;
        }

        @Override
        public void onChannelRemoved(final int channelId)
        {
            m_Out.println(String.format("Channel %d removed", channelId));
        }

        @Override
        public void onChannelRemoved(final int channelId, final Exception exception)
        {
            m_Out.println(String.format("Channel %d removed by exception [%s]", channelId, exception.getMessage()));
        }
    }

    /**
     * Message callback handler for AssetDirectoryService messages used to notify when response messages are received.
     */
    private class AssetDirectoryMessageCallback implements MessageListenerCallback<AssetDirectoryServiceNamespace>
    {
        private final PrintStream m_Out;

        /**
         * Creates a message callback for AssetDirectoryService messages.
         * 
         * @param out
         *      Print stream for the Gogo shell
         */
        AssetDirectoryMessageCallback(final PrintStream out)
        {
            m_Out = out;
        }

        @Override
        public void handleMessage(final RemoteMessage<AssetDirectoryServiceNamespace> message)
        {
            if (message.getNamespaceMessage().getType().equals(AssetDirectoryServiceMessageType.GetAssetsResponse))
            {
                final GetAssetsResponseData data = (GetAssetsResponseData)message.getDataMessage();
                for (FactoryObjectInfo assetInfo : data.getAssetInfoList())
                {
                    m_Out.println("Asset: " + assetInfo.getProductType());
                }
                m_Out.println();
            }
        }
    }
    
    /**
     * Message callback handler for DataStreamService messages used to notify when response messages are received.
     */
    private class DataStreamServiceMessageCallback implements MessageListenerCallback<DataStreamServiceNamespace>
    {
        private final PrintStream m_Out;
        
        /**
         * Creates a message callback for DataStreamService messages.
         * 
         * @param out
         *      Print stream for the Gogo shell
         */
        DataStreamServiceMessageCallback(final PrintStream out)
        {
            m_Out = out;
        }
        
        @Override
        public void handleMessage(final RemoteMessage<DataStreamServiceNamespace> message)
        {
            if (message.getNamespaceMessage().getType().equals(DataStreamServiceMessageType.GetStreamProfilesResponse))
            {
                final GetStreamProfilesResponseData data = (GetStreamProfilesResponseData)message.getDataMessage();
                for (StreamProfile profile : data.getStreamProfileList())
                {
                    m_Out.println("Stream Profile: " + profile.getInfo().getUuid().toString());
                    m_Out.println("    Is enabled? " + profile.getIsEnabled());
                    m_Out.println("    Stream location: " + profile.getStreamPort());
                    m_Out.println("    Bitrate (kbps): " + profile.getBitrateKbps());
                    m_Out.println("    Format: " + profile.getFormat());
                    m_Out.println("    Asset UUID: " + profile.getAssetUuid().toString());
                    m_Out.println("    Sensor: " + profile.getSensorId());
                    m_Out.println();
                }
                m_Out.println();
                
            }
        }
        
    }
}
