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

import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.ChannelStatus;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.types.remote.RemoteChannelTypeEnum;
import mil.dod.th.ose.remote.api.RemoteSettings;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.ose.utils.ClientSocketFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;

public class TestClientSocketChannel
{
    private ClientSocketChannel m_SUT;
    private LoggingService m_Logging;
    private ComponentFactory m_ListenerFactory;
    private ComponentInstance m_ListenerInstance;
    private ComponentInstance m_SenderInstance;
    private QueuedMessageSender m_MessageSender;
    private ComponentFactory m_SenderFactory;
    private RemoteSettings m_RemoteSettings;
    private Socket m_Socket;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new ClientSocketChannel();
        m_Logging = LoggingServiceMocker.createMock();
        m_SUT.setLoggingService(m_Logging);
        
        // mock out remote settings
        m_RemoteSettings = mock(RemoteSettings.class);
        m_SUT.setRemoteSettings(m_RemoteSettings);
        when(m_RemoteSettings.isLogRemoteMessagesEnabled()).thenReturn(true);
        
        // mock out the socket factory
        ClientSocketFactory clientSocketFactory = mock(ClientSocketFactory.class);
        m_SUT.setClientSocketFactory(clientSocketFactory);
        m_Socket = mock(Socket.class);
        when(clientSocketFactory.createClientSocket("test", 10)).thenReturn(m_Socket);
        
        // mock out listener factory
        m_ListenerFactory = mock(ComponentFactory.class);
        m_SUT.setSocketMessageListenerFactory(m_ListenerFactory);
        m_ListenerInstance = mock(ComponentInstance.class);
        when(m_ListenerFactory.newInstance(Mockito.any(Dictionary.class))).thenReturn(m_ListenerInstance);
    
        // mock out message sender factory
        m_MessageSender = mock(QueuedMessageSender.class);
        m_SenderFactory = mock(ComponentFactory.class);
        m_SUT.setQueuedMessageSenderFactory(m_SenderFactory);
        m_SenderInstance = mock(ComponentInstance.class);
        when(m_SenderFactory.newInstance(Mockito.any(Dictionary.class))).thenReturn(m_SenderInstance);
        when(m_SenderInstance.getInstance()).thenReturn(m_MessageSender);
    }

    /**
     * Verify the factory name matches the class name
     */
    @Test
    public void testFactoryName()
    {
        assertThat(ClientSocketChannel.FACTORY_NAME, is(m_SUT.getClass().getName()));
    }
    
    /**
     * Verify channel status is initialized and socket message listener is created at startup.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    // this timeout is less then time the thread below blocks to prove it is blocking on a separate thread
    @Test(timeout = 2000)
    public void testActivate() throws InterruptedException, IOException
    {
        // mock the message listener and its run method to block, need to block to verify run method is called on 
        // separate thread
        SocketMessageListener socketMessageListener = mock(SocketMessageListener.class);
        when(m_ListenerInstance.getInstance()).thenReturn(socketMessageListener);
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                Thread.sleep(200000); // simulate a blocking call
                return null;
            }
        }).when(socketMessageListener).run();
        
        // activate the component
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ClientSocketChannel.HOST_PROP_KEY, "test");
        props.put(ClientSocketChannel.PORT_PROP_KEY, 10);
        m_SUT.activate(props);
        
        //all new channels start out as unknown
        assertThat(m_SUT.getStatus(), is(ChannelStatus.Unknown));
        
        // verify message listener is created and given the socket and channel instance
        ArgumentCaptor<Dictionary> dictionaryCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(m_ListenerFactory).newInstance(dictionaryCaptor.capture());
        assertThat((Socket)dictionaryCaptor.getValue().get(SocketMessageListener.SOCKET_PROP_KEY), is(m_Socket));
        assertThat((ClientSocketChannel)dictionaryCaptor.getValue().get(SocketMessageListener.CHANNEL_PROP_KEY), 
                is(m_SUT));
        
        // verify queued message sender is created and given the socket and channel instance
        verify(m_SenderFactory).newInstance(dictionaryCaptor.capture());
        assertThat((ClientSocketChannel)dictionaryCaptor.getValue().get(QueuedMessageSender.CHANNEL_PROP_KEY), 
                is(m_SUT));
        
        Thread.sleep(500); // allow thread to run
        
        // verify listener was told to run and on a separate thread, know on a separate thread because this test
        // didn't timeout waiting on the run method to exit
        verify(socketMessageListener).run();
    }
    
    /**
     * Verify socket and output stream is closed on deactivation.
     * 
     * Verify message listener is disposed
     */
    @Test
    public void testDeactivation() throws IOException, InterruptedException
    {
        // mock the socket and input stream that will be used to send the message
        OutputStream outStream = mock(OutputStream.class);
        when(m_Socket.getOutputStream()).thenReturn(outStream);
        
        // activate method expects a connected socket to be passed in and remote channel
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ClientSocketChannel.HOST_PROP_KEY, "test");
        props.put(ClientSocketChannel.PORT_PROP_KEY, 10);
        m_SUT.activate(props);
        
        m_SUT.deactivate();
        verify(outStream).close();
        verify(m_Socket).close();
        verify(m_ListenerInstance).dispose();
        verify(m_SenderInstance).dispose();
        
        // this time have both methods throw exception, should still complete deactivation and each one called
        doThrow(new IOException()).when(m_Socket).close();
        doThrow(new IOException()).when(outStream).close();
        
        m_SUT.deactivate();
        //verify
        verify(outStream, times(2)).close();
        verify(m_Socket, times(2)).close();        
        verify(m_ListenerInstance, times(2)).dispose();
    }
    
    /**
     * Verify the message is sent using the given socket in binary form.
     * 
     * Verify method returns false if socket throws exception
     */
    @Test
    public void testTrySendMessage() throws IOException
    {
        // mock the socket that will be used to send the message
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        when(m_Socket.getOutputStream()).thenReturn(outStream);
        
        // activate method expects a connected socket to be passed in
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ClientSocketChannel.HOST_PROP_KEY, "test");
        props.put(ClientSocketChannel.PORT_PROP_KEY, 10);
        m_SUT.activate(props);
        
        // construct a single base namespace message to verify sent to socket
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .build();
        
        TerraHarvestMessage message = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.Base, 100, baseNamespaceMessage);
        
        // replay
        boolean result = m_SUT.trySendMessage(message);
        assertThat(result, is(true));
        
        // verify mock socket's output stream was asked to send message in binary form with delimiter at beginning
        byte[] binaryMessage = message.toByteArray();
        // first byte sent will be the varint (https://developers.google.com/protocol-buffers/docs/encoding#varints)
        // representing the length of the message following
        byte[] fullBinaryMessage = new byte[binaryMessage.length + 1]; // leave room for delimiter
        fullBinaryMessage[0] = (byte)binaryMessage.length;
        System.arraycopy(binaryMessage, 0, fullBinaryMessage, 1, binaryMessage.length);
        
        assertThat(outStream.toByteArray(), is(fullBinaryMessage));
    
        // verify debug logging is enabled
        verify(m_Logging, times(1)).debug(anyString(), anyVararg());
    }
    
    /**
     * Verify bytes transmitted is correct.
     */
    @Test
    public void testGetBytesTransmitted() throws IOException
    {
        // activate method expects a connected socket to be passed in
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ClientSocketChannel.HOST_PROP_KEY, "test");
        props.put(ClientSocketChannel.PORT_PROP_KEY, 10);
        m_SUT.activate(props);
        
        // verify bytes transmitted starts off at zero
        assertThat(m_SUT.getBytesTransmitted(), is(0L));
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.createBaseMessage();
        m_SUT.trySendMessage(message);
        
        // verify bytes transmitted now equals data transmitted
        assertThat(m_SUT.getBytesTransmitted(), is(new Integer(message.getSerializedSize()).longValue()));
        
        m_SUT.trySendMessage(message);
        
        // verify bytes transmitted is updated again
        assertThat(m_SUT.getBytesTransmitted(), is(new Integer(message.getSerializedSize() * 2).longValue()));
    }
    
    /**
     * Verify bytes received is correct.
     */
    @Test
    public void testGetSetByteReceived()
    {
        assertThat("Initial value is 0", m_SUT.getBytesReceived(), is(0L));
        
        m_SUT.setBytesReceived(5L);
        assertThat(m_SUT.getBytesReceived(), is(5L));
    }
    
    /**
     * Verify method returns false if socket throws exception
     */
    @Test
    public void testTrySendMessageFailure() throws IOException
    {
        // mock the socket that will throw exception when trying to send
        OutputStream outStream = new OutputStream()
        {
            @Override
            public void write(int b) throws IOException
            {
                throw new IOException();
            }
        };
        when(m_Socket.getOutputStream()).thenReturn(outStream);
        
        // activate method expects a connected socket to be passed in
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ClientSocketChannel.HOST_PROP_KEY, "test");
        props.put(ClientSocketChannel.PORT_PROP_KEY, 10);
        m_SUT.activate(props);
        
        // construct a single base namespace message to verify sent to socket
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .build();
        
        TerraHarvestMessage message = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.Base, 100, baseNamespaceMessage);
        
        // replay
        boolean result = m_SUT.trySendMessage(message);
        assertThat(result, is(false));
    }
    
    /**
     * Verify the message is sent using the given socket in binary form if no error sending message.
     */
    @Test
    public void testQueueMessage() throws IOException
    {
        // activate method expects a connected socket to be passed in
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ClientSocketChannel.HOST_PROP_KEY, "test");
        props.put(ClientSocketChannel.PORT_PROP_KEY, 10);
        m_SUT.activate(props);
        
        // construct a single base namespace message to verify sent to socket
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .build();
        
        TerraHarvestMessage message = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.Base, 100, baseNamespaceMessage);
        
        when(m_MessageSender.queue(message)).thenReturn(false);
        
        // replay
        boolean result = m_SUT.queueMessage(message);
        
        // verify message is passed down to queue and result returned by queue is returned
        verify(m_MessageSender).queue(message);
        assertThat("message reported the same as returned by sender", result, is(false));
        
        // mock to return true instead, verify result is passed correctly
        when(m_MessageSender.queue(message)).thenReturn(true);
        
        // replay
        result = m_SUT.queueMessage(message);
        
        // verify message is passed down 2nd time to queue and result returned by queue is returned
        verify(m_MessageSender, times(2)).queue(message);
        assertThat("message reported the same as returned by sender", result, is(true));   
    }
    
    /**
     * Verify count is returned from queue.
     */
    @Test
    public void testGetQueuedMessageCount() throws IOException
    {
        // activate the component with mocked socket
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ClientSocketChannel.HOST_PROP_KEY, "test");
        props.put(ClientSocketChannel.PORT_PROP_KEY, 10);
        m_SUT.activate(props);
        
        when(m_MessageSender.getQueuedMessageCount()).thenReturn(100);
        
        assertThat(m_SUT.getQueuedMessageCount(), is(100));
    }

    /**
     * Verify the queue can be requested to clear.
     */
    @Test
    public void testClearQueue() throws IOException
    {
        // activate the component with mocked socket
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ClientSocketChannel.HOST_PROP_KEY, "test");
        props.put(ClientSocketChannel.PORT_PROP_KEY, 10);
        m_SUT.activate(props);
        
        //request clear
        m_SUT.clearQueuedMessages();
        
        verify(m_MessageSender).clearQueue();
    }
    
    /**
     * Verify the matches method returns true if the socket property matches
     */
    @Test
    public void testMatches() throws IOException
    {
        // setup a channel
        Map<String, Object> actualProps = new HashMap<String, Object>();
        actualProps.put(ClientSocketChannel.HOST_PROP_KEY, "test");
        actualProps.put(ClientSocketChannel.PORT_PROP_KEY, 10);
        actualProps.put(ComponentConstants.COMPONENT_ID, 23);
        actualProps.put(ComponentConstants.COMPONENT_NAME, AbstractSocketChannel.class.getName());
        actualProps.put("some random value that might be there", "blah");
        m_SUT.activate(actualProps);
        
        // same host, different port, should not match
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ClientSocketChannel.HOST_PROP_KEY, "test");
        props.put(ClientSocketChannel.PORT_PROP_KEY, 20);
        assertThat(m_SUT.matches(props), is(false));
        
        // different host, same port, should not match
        props.put(ClientSocketChannel.HOST_PROP_KEY, "other-host");
        props.put(ClientSocketChannel.PORT_PROP_KEY, 10);
        assertThat(m_SUT.matches(props), is(false));
        
        // same props for host and port should match, should ignore default props of a component
        props.put(ClientSocketChannel.HOST_PROP_KEY, "test");
        props.put(ClientSocketChannel.PORT_PROP_KEY, 10);
        assertThat(m_SUT.matches(props), is(true));
    }
    
    /**
     * Verify that the channel type is set at initialization of the component.
     */
    @Test
    public void testGetChannelType() throws IOException
    {
        // activate the component
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ClientSocketChannel.HOST_PROP_KEY, "test");
        props.put(ClientSocketChannel.PORT_PROP_KEY, 10);
        m_SUT.activate(props);
        
        assertThat(m_SUT.getChannelType(), is(RemoteChannelTypeEnum.SOCKET));
    }
    
    /**
     * Verify host returned is the one associated with the incoming socket. 
     */
    @Test
    public void testGetHost() throws IOException
    {
        // activate the component
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ClientSocketChannel.HOST_PROP_KEY, "test");
        props.put(ClientSocketChannel.PORT_PROP_KEY, 10);
        m_SUT.activate(props);
        
        assertThat(m_SUT.getHost(), is("test"));
    }
    
    /**
     * Verify port returned is the one associated with the incoming socket. 
     */
    @Test
    public void testGetPort() throws IOException
    {
        // activate the component
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ClientSocketChannel.HOST_PROP_KEY, "test");
        props.put(ClientSocketChannel.PORT_PROP_KEY, 10);
        m_SUT.activate(props);
        
        assertThat(m_SUT.getPort(), is(10));
    }
    
    /**
     * Verify toString prints a short readable string.
     */
    @Test
    public void testToString() throws IOException
    {
        // activate the component
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ClientSocketChannel.HOST_PROP_KEY, "test");
        props.put(ClientSocketChannel.PORT_PROP_KEY, 10);
        m_SUT.activate(props);
        
        assertThat("string rep is combination of host and port", m_SUT.toString(), is("test:10"));
    }
    
    /**
     * Verify logging does not occur when a message is sent if logging is disabled in the RemoteSettings
     */
    @Test
    public void testTrySendMessageLoggingDisabled() throws IOException
    {
        // mock disabled logging
        when(m_RemoteSettings.isLogRemoteMessagesEnabled()).thenReturn(false);
    
        // mock the socket that will be used to send the message
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        when(m_Socket.getOutputStream()).thenReturn(outStream);
        
        // activate method expects a connected socket to be passed in
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ClientSocketChannel.HOST_PROP_KEY, "test");
        props.put(ClientSocketChannel.PORT_PROP_KEY, 10);
        m_SUT.activate(props);
        
        // construct a single base namespace message to verify sent to socket
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .build();
        
        TerraHarvestMessage message = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.Base, 100, baseNamespaceMessage);
        
        // replay
        boolean result = m_SUT.trySendMessage(message);
        assertThat(result, is(true));
        
        // verify mock socket's output stream was asked to send message in binary form with delimiter at beginning
        byte[] binaryMessage = message.toByteArray();
        // first byte sent will be the varint (https://developers.google.com/protocol-buffers/docs/encoding#varints)
        // representing the length of the message following
        byte[] fullBinaryMessage = new byte[binaryMessage.length + 1]; // leave room for delimiter
        fullBinaryMessage[0] = (byte)binaryMessage.length;
        System.arraycopy(binaryMessage, 0, fullBinaryMessage, 1, binaryMessage.length);
        
        assertThat(outStream.toByteArray(), is(fullBinaryMessage));
        verify(m_Logging, times(0)).debug(anyString());
    }
}
