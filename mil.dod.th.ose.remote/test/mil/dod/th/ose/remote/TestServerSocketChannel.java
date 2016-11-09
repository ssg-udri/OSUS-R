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

import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;

public class TestServerSocketChannel
{
    private ServerSocketChannel m_SUT;
    private LoggingService m_Logging;
    private ComponentFactory m_ListenerFactory;
    private ComponentInstance m_ListenerInstance;
    private ComponentInstance m_SenderInstance;
    private QueuedMessageSender m_MessageSender;
    private ComponentFactory m_SenderFactory;
    private RemoteSettings m_RemoteSettings;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new ServerSocketChannel();
        m_Logging = LoggingServiceMocker.createMock();
        m_SUT.setLoggingService(m_Logging);
        
        // mock out remote settings
        m_RemoteSettings = mock(RemoteSettings.class);
        m_SUT.setRemoteSettings(m_RemoteSettings);
        when(m_RemoteSettings.isLogRemoteMessagesEnabled()).thenReturn(true);
        
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
        assertThat(ServerSocketChannel.FACTORY_NAME, is(m_SUT.getClass().getName()));
    }
    
    /**
     * Verify channel status is initialized and socket message listener is created at startup.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    // this timeout is less then time the thread below blocks to prove it is blocking on a separate thread
    @Test(timeout = 2000)
    public void testActivate() throws InterruptedException
    {
        // mock the input
        Socket socket = mock(Socket.class);

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
        props.put(ServerSocketChannel.SOCKET_PROP_KEY, socket);
        m_SUT.activate(props);
        
        //all new channels start out as unknown
        assertThat(m_SUT.getStatus(), is(ChannelStatus.Unknown));
        
        // verify message listener is created and given the socket and channel instance
        ArgumentCaptor<Dictionary> dictionaryCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(m_ListenerFactory).newInstance(dictionaryCaptor.capture());
        assertThat((Socket)dictionaryCaptor.getValue().get(ServerSocketChannel.SOCKET_PROP_KEY), is(socket));
        assertThat((ServerSocketChannel)dictionaryCaptor.getValue().get(SocketMessageListener.CHANNEL_PROP_KEY), 
                is(m_SUT));
        
        // verify queued message sender is created and given the socket and channel instance
        verify(m_SenderFactory).newInstance(dictionaryCaptor.capture());
        assertThat((ServerSocketChannel)dictionaryCaptor.getValue().get(QueuedMessageSender.CHANNEL_PROP_KEY), 
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
        Socket socket = mock(Socket.class);
        OutputStream outStream = mock(OutputStream.class);
        when(socket.getOutputStream()).thenReturn(outStream);
        
        // activate method expects a connected socket to be passed in and remote channel
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ServerSocketChannel.SOCKET_PROP_KEY, socket);
        m_SUT.activate(props);
        
        m_SUT.deactivate();
        verify(outStream).close();
        verify(socket).close();
        verify(m_ListenerInstance).dispose();
        verify(m_SenderInstance).dispose();
        
        // this time have both methods throw exception, should still complete deactivation and each one called
        doThrow(new IOException()).when(socket).close();
        doThrow(new IOException()).when(outStream).close();
        
        m_SUT.deactivate();
        //verify
        verify(outStream, times(2)).close();
        verify(socket, times(2)).close();        
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
        Socket socket = mock(Socket.class);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        when(socket.getOutputStream()).thenReturn(outStream);
        
        // activate method expects a connected socket to be passed in
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ServerSocketChannel.SOCKET_PROP_KEY, socket);
        m_SUT.activate(props);
        
        TerraHarvestMessage message = createBaseMessage();
        
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
        
        // verify logging occurs
        verify(m_Logging, times(1)).debug(anyString(), anyVararg());
    }
    
    /**
     * Verify the message is sent using the given socket in binary form but no message is logged
     */
    @Test
    public void testTrySendMessageLoggingDisabled() throws IOException
    {
        //disable logging
        when(m_RemoteSettings.isLogRemoteMessagesEnabled()).thenReturn(false);
    
        // mock the socket that will be used to send the message
        Socket socket = mock(Socket.class);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        when(socket.getOutputStream()).thenReturn(outStream);
        
        // activate method expects a connected socket to be passed in
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ServerSocketChannel.SOCKET_PROP_KEY, socket);
        m_SUT.activate(props);
        
        TerraHarvestMessage message = createBaseMessage();
        
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
        
        // verify logging is disabled
        verify(m_Logging, times(0)).debug(anyString());
    }
    
    /**
     * Verify bytes transmitted is correct.
     */
    @Test
    public void testGetBytesTransmitted() throws IOException
    {
        // mock the socket that will be used to send the message
        Socket socket = mock(Socket.class);
        
        // activate method expects a connected socket to be passed in
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ServerSocketChannel.SOCKET_PROP_KEY, socket);
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
        Socket socket = mock(Socket.class);
        OutputStream outStream = new OutputStream()
        {
            @Override
            public void write(int b) throws IOException
            {
                throw new IOException();
            }
        };
        when(socket.getOutputStream()).thenReturn(outStream);
        
        // activate method expects a connected socket to be passed in
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ServerSocketChannel.SOCKET_PROP_KEY, socket);
        m_SUT.activate(props);
        
        TerraHarvestMessage message = createBaseMessage();
        
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
        // mock the socket that will be used to send the message
        Socket socket = mock(Socket.class);
        
        // activate method expects a connected socket to be passed in
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ServerSocketChannel.SOCKET_PROP_KEY, socket);
        m_SUT.activate(props);
        
        TerraHarvestMessage message = createBaseMessage();
        
        when(m_MessageSender.queue(message)).thenReturn(false);
        
        // replay
        boolean result = m_SUT.queueMessage(message);
        
        // verify message is passed down to queue and result returned by queue is returned
        verify(m_MessageSender).queue(message);
        assertThat("message reorted the same as returned by sender", result, is(false));
        
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
    public void testGetQueuedMessageCount()
    {
        // activate the component with mocked socket
        Socket socket = mock(Socket.class);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ServerSocketChannel.SOCKET_PROP_KEY, socket);
        m_SUT.activate(props);
        
        when(m_MessageSender.getQueuedMessageCount()).thenReturn(100);
        
        assertThat(m_SUT.getQueuedMessageCount(), is(100));
    }
    
    /**
     * Verify the queue can be emptied for a particular channel. 
     * 
     * Verify message senders clear queue call is made.
     */
    @Test
    public void testEmptyQueue()
    {
        // activate the component with mocked socket
        Socket socket = mock(Socket.class);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ServerSocketChannel.SOCKET_PROP_KEY, socket);
        m_SUT.activate(props);
        
        m_SUT.clearQueuedMessages();
        
        verify(m_MessageSender).clearQueue();
    }

    /**
     * Verify the matches method returns true if the socket property matches
     */
    @Test
    public void testMatches()
    {
        // mock some different property values
        Socket socket1 = mock(Socket.class);
        Socket socket2 = mock(Socket.class);
        
        // setup a channel
        Map<String, Object> actualProps = new HashMap<String, Object>();
        actualProps.put(ServerSocketChannel.SOCKET_PROP_KEY, socket1);
        actualProps.put(ComponentConstants.COMPONENT_ID, 23);
        actualProps.put(ComponentConstants.COMPONENT_NAME, AbstractSocketChannel.class.getName());
        actualProps.put("some random value that might be there", "blah");
        m_SUT.activate(actualProps);
        
        // different props should not match
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ServerSocketChannel.SOCKET_PROP_KEY, socket2);
        assertThat(m_SUT.matches(props), is(false));
        
        // same props for transport and address should match, should ignore default props of a component
        props.put(ServerSocketChannel.SOCKET_PROP_KEY, socket1);
        assertThat(m_SUT.matches(props), is(true));
    }
    
    /**
     * Verify that the channel type is set at initialization of the component.
     */
    @Test
    public void testGetChannelType() throws IOException
    {
        // mock the socket
        Socket socket = mock(Socket.class);
        
        // activate the component
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ServerSocketChannel.SOCKET_PROP_KEY, socket);
        m_SUT.activate(props);
        
        assertThat(m_SUT.getChannelType(), is(RemoteChannelTypeEnum.SOCKET));
    }
    
    /**
     * Verify port returned is the one associated with the incoming socket. 
     */
    @Test
    public void testGetPort()
    {
        // mock the socket
        Socket socket = mock(Socket.class);
        when(socket.getPort()).thenReturn(1000);
        
        // activate the component
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ServerSocketChannel.SOCKET_PROP_KEY, socket);
        m_SUT.activate(props);
        
        assertThat(m_SUT.getPort(), is(1000));
    }
    
    /**
     * Verify host returned is the one associated with the incoming socket. 
     */
    @Test
    public void testGetHost()
    {
        // mock the socket and its InetAddress as it contains the hostname used by the method
        Socket socket = mock(Socket.class);
        InetAddress inetAddress = mock(InetAddress.class);
        when(socket.getInetAddress()).thenReturn(inetAddress);
        when(inetAddress.getHostName()).thenReturn("test-host");
        
        // activate the component
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ServerSocketChannel.SOCKET_PROP_KEY, socket);
        m_SUT.activate(props);
        
        assertThat(m_SUT.getHost(), is("test-host"));
    }
    
    /**
     * Verify toString prints a short readable string.
     */
    @Test
    public void testToString()
    {
        // mock the socket so getHost and getPort work
        Socket socket = mock(Socket.class);
        InetAddress inetAddress = mock(InetAddress.class);
        when(socket.getInetAddress()).thenReturn(inetAddress);
        when(inetAddress.getHostName()).thenReturn("test-host");
        when(socket.getPort()).thenReturn(1000);
        
        // activate the component
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ServerSocketChannel.SOCKET_PROP_KEY, socket);
        m_SUT.activate(props);
        
        assertThat("string rep is combination of host and port", m_SUT.toString(), is("test-host:1000"));
    }
    
    private TerraHarvestMessage createBaseMessage()
    {
        // construct a single base namespace message to verify sent to socket
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .build();
        
        return TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.Base, 100, baseNamespaceMessage);
    }   
}
