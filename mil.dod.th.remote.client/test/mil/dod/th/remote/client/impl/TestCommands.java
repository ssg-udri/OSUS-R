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

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

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
import mil.dod.th.core.remote.proto.SharedMessages.UUID;
import mil.dod.th.remote.client.ChannelStateCallback;
import mil.dod.th.remote.client.MessageListenerCallback;
import mil.dod.th.remote.client.MessageListenerService;
import mil.dod.th.remote.client.MessageSenderService;
import mil.dod.th.remote.client.RemoteMessage;
import mil.dod.th.remote.client.generate.AssetDirectoryMessageGenerator;
import mil.dod.th.remote.client.generate.DataStreamServiceMessageGenerator;
import mil.dod.th.remote.client.generate.EventAdminMessageGenerator;
import mil.dod.th.remote.client.generate.EventRegistrationBuilder;
import mil.dod.th.remote.client.generate.MessageBuilder;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen.LexiconFormat;

import org.apache.felix.service.command.CommandSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public class TestCommands
{
    private static final String LOCALHOST = "127.0.0.1";

    private Commands m_SUT;

    @Mock private BundleContext m_Context;
    @Mock private CommandSession m_CmdSession;
    @Mock private AssetDirectoryMessageGenerator m_AssetDirMessageGen;
    @Mock private DataStreamServiceMessageGenerator m_DataStreamMessageGen;
    @Mock private EventAdminMessageGenerator m_EventAdminMessageGen;
    @Mock private MessageListenerService m_MessageListenerService;
    @Mock private MessageSenderService m_MessageSenderService;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        when(m_CmdSession.getConsole()).thenReturn(mock(PrintStream.class));

        m_SUT = new Commands();
        m_SUT.setAssetDirMessageGen(m_AssetDirMessageGen);
        m_SUT.setDataStreamMessageGen(m_DataStreamMessageGen);
        m_SUT.setEventAdminMessageGen(m_EventAdminMessageGen);
        m_SUT.setMessageListenerService(m_MessageListenerService);
        m_SUT.setMessageSenderService(m_MessageSenderService);

        m_SUT.activate(m_Context);
    }

    @Test
    public void testConnectDisconnect() throws IOException
    {
        // mock
        ArgumentCaptor<ChannelStateCallback> callbackCaptor = ArgumentCaptor.forClass(ChannelStateCallback.class);

        try (ServerSocket server = new ServerSocket(0))
        {
            // replay (Connect)
            m_SUT.connect(m_CmdSession, LOCALHOST, server.getLocalPort());

            // verify
            verify(m_MessageListenerService).addRemoteChannel(anyInt(), Mockito.any(InputStream.class),
                    callbackCaptor.capture());

            verify(m_MessageSenderService).setClientId(anyInt());
            verify(m_MessageSenderService).addRemoteChannel(anyInt(), any(OutputStream.class),
                any(ChannelStateCallback.class));

            // replay (Disconnect)
            m_SUT.disconnect();

            // verify
            verify(m_MessageListenerService).removeRemoteChannel(anyInt());
            verify(m_MessageSenderService).removeRemoteChannel(anyInt());
            verify(m_MessageListenerService, never()).unregisterCallback(any(MessageListenerCallback.class));
        }

        // verify callback methods execute without throwing exceptions
        ChannelStateCallback callback = callbackCaptor.getValue();
        callback.onChannelRemoved(0);
        callback.onChannelRemoved(0, new Exception("remove error"));
    }

    /**
     * Verify that an exception is thrown when trying to make a connection twice without disconnecting first.
     */
    @Test
    public void testConnectException() throws IOException
    {
        try (ServerSocket server = new ServerSocket(0))
        {
            m_SUT.connect(m_CmdSession, LOCALHOST, server.getLocalPort());

            // mock
            doThrow(new IllegalStateException()).when(m_MessageListenerService)
                .addRemoteChannel(anyInt(), Mockito.any(InputStream.class), any(ChannelStateCallback.class));

            // replay
            try
            {
                m_SUT.connect(m_CmdSession, LOCALHOST, server.getLocalPort());
                fail("connect should exception");
            }
            catch (Exception ex)
            {
                // Expected
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDisconnectAfterSend() throws IOException
    {
        try (ServerSocket server = new ServerSocket(0))
        {
            // mock
            EventRegistrationBuilder builder = mock(EventRegistrationBuilder.class);
            when(builder.setCanQueueEvent(Mockito.anyBoolean())).thenReturn(builder);
            when(builder.setExpirationTimeHours(Mockito.anyInt())).thenReturn(builder);
            when(builder.setTopics(Mockito.anyString(), Mockito.anyString())).thenReturn(builder);
            when(builder.setObjectFormat(Mockito.any(LexiconFormat.Enum.class))).thenReturn(builder);
            when(m_EventAdminMessageGen.createEventRegRequest()).thenReturn(builder);
    
            MessageBuilder builder2 = mock(MessageBuilder.class);
            when(m_AssetDirMessageGen.createGetAssetsRequest()).thenReturn(builder2);

            ServiceRegistration<EventHandler> eventReg = mock(ServiceRegistration.class);
            when(m_Context.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class),
                Mockito.any(Dictionary.class))).thenReturn(eventReg);

            // replay
            m_SUT.connect(m_CmdSession, LOCALHOST, server.getLocalPort());
            m_SUT.sendEventRegister(m_CmdSession);
            m_SUT.sendGetAssets(m_CmdSession);
            m_SUT.disconnect();
    
            // verify
            verify(m_MessageListenerService).removeRemoteChannel(anyInt());
            verify(m_MessageSenderService).removeRemoteChannel(anyInt());
            verify(m_MessageListenerService).unregisterCallback(any(MessageListenerCallback.class));
            verify(eventReg).unregister();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSendEventRegister() throws IOException
    {
        // mock
        EventRegistrationBuilder builder = mock(EventRegistrationBuilder.class);
        when(builder.setCanQueueEvent(Mockito.anyBoolean())).thenReturn(builder);
        when(builder.setExpirationTimeHours(Mockito.anyInt())).thenReturn(builder);
        when(builder.setTopics(Mockito.anyString(), Mockito.anyString())).thenReturn(builder);
        when(builder.setObjectFormat(Mockito.any(LexiconFormat.Enum.class))).thenReturn(builder);
        when(m_EventAdminMessageGen.createEventRegRequest()).thenReturn(builder);

        Map<String, Object> props = new HashMap<>();
        props.put("prop1", "value1");
        Event event = new Event("topic", props);

        // replay
        m_SUT.sendEventRegister(m_CmdSession);

        // verify
        ArgumentCaptor<EventHandler> eventHandlerCaptor = ArgumentCaptor.forClass(EventHandler.class);
        verify(m_Context).registerService(eq(EventHandler.class), eventHandlerCaptor.capture(),
            Mockito.any(Dictionary.class));
        verify(m_EventAdminMessageGen).createEventRegRequest();
        verify(builder).send(Mockito.anyInt());

        // verify calls made to the EventHandler
        EventHandler eventHandler = eventHandlerCaptor.getValue();
        eventHandler.handleEvent(event);
    }

    @Test
    public void testSendClearEventRegs() throws IOException
    {
        // mock
        MessageBuilder builder = mock(MessageBuilder.class);
        when(m_EventAdminMessageGen.createEventCleanupRequest()).thenReturn(builder);

        // replay
        m_SUT.sendClearEventRegs();

        // verify
        verify(m_EventAdminMessageGen).createEventCleanupRequest();
        verify(builder).send(Mockito.anyInt());
    }

    @Test
    public void testSendGetAssets() throws IOException
    {
        // mock
        MessageBuilder builder = mock(MessageBuilder.class);
        when(m_AssetDirMessageGen.createGetAssetsRequest()).thenReturn(builder);

        @SuppressWarnings("unchecked")
        RemoteMessage<AssetDirectoryServiceNamespace> remoteMessage = mock(RemoteMessage.class);
        when(remoteMessage.getNamespaceMessage()).thenReturn(AssetDirectoryServiceNamespace.newBuilder()
                .setType(AssetDirectoryServiceMessageType.GetAssetsResponse)
                .build());
        when(remoteMessage.getDataMessage()).thenReturn(GetAssetsResponseData.newBuilder()
                .addAssetInfo(FactoryObjectInfo.newBuilder()
                    .setPid("pid")
                    .setProductType("product")
                    .setUuid(UUID.newBuilder().setMostSignificantBits(0).setLeastSignificantBits(0)))
                .build());

        // replay
        m_SUT.sendGetAssets(m_CmdSession);

        // verify
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<MessageListenerCallback> callbackCaptor = ArgumentCaptor.forClass(MessageListenerCallback.class);
        verify(m_MessageListenerService).registerCallback(eq(Namespace.AssetDirectoryService),
                callbackCaptor.capture());
        verify(m_AssetDirMessageGen).createGetAssetsRequest();
        verify(builder).send(Mockito.anyInt());

        // verify callback throws no exceptions
        @SuppressWarnings("unchecked")
        MessageListenerCallback<AssetDirectoryServiceNamespace> callback = callbackCaptor.getValue();
        callback.handleMessage(remoteMessage);
    }
    
    @Test
    public void testSendGetStreamProfiles() throws IOException
    {
        // mock
        MessageBuilder builder = mock(MessageBuilder.class);
        when(m_DataStreamMessageGen.createGetStreamProfilesRequest()).thenReturn(builder);

        @SuppressWarnings("unchecked")
        RemoteMessage<DataStreamServiceNamespace> remoteMessage = mock(RemoteMessage.class);
        when(remoteMessage.getNamespaceMessage()).thenReturn(DataStreamServiceNamespace.newBuilder()
            .setType(DataStreamServiceMessageType.GetStreamProfilesResponse)
            .build());
        when(remoteMessage.getDataMessage()).thenReturn(GetStreamProfilesResponseData.newBuilder()
            .addStreamProfile(StreamProfile.newBuilder()
                .setInfo(FactoryObjectInfo.newBuilder()
                    .setPid("pid")
                    .setProductType("product")
                    .setUuid(UUID.newBuilder().setMostSignificantBits(0).setLeastSignificantBits(0)).build())
                .setBitrateKbps(100.0)
                .setAssetUuid(UUID.newBuilder().setMostSignificantBits(1).setLeastSignificantBits(1).build())
                .setStreamPort("rtp://226.5.6.7:12000")
                .setIsEnabled(true)
                .setFormat("video/mp4").build()).build());

        // replay
        m_SUT.sendGetStreamProfiles(m_CmdSession);

        // verify
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<MessageListenerCallback> callbackCaptor = ArgumentCaptor.forClass(MessageListenerCallback.class);
        verify(m_MessageListenerService).registerCallback(eq(Namespace.DataStreamService),
                callbackCaptor.capture());
        verify(m_DataStreamMessageGen).createGetStreamProfilesRequest();
        verify(builder).send(Mockito.anyInt());

        // verify callback throws no exceptions
        @SuppressWarnings("unchecked")
        MessageListenerCallback<DataStreamServiceNamespace> callback = callbackCaptor.getValue();
        callback.handleMessage(remoteMessage);
    }
}
