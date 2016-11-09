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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.messaging.MessageRouter;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.ose.remote.api.RemoteSettings;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.google.protobuf.ByteString;

/**
 * Test the the socket message listener reads in messages from a socket on a thread. Verify that new messages read 
 * in are passed to the {@link MessageRouter} and that new connections are synced with the 
 * {@link mil.dod.th.core.remote.RemoteChannelLookup}.
 * @author Dave Humeniuk
 *
 */
public class TestSocketMessageListener extends SocketMessageListener
{
    private SocketMessageListener m_SUT;
    private MessageRouter m_MessageRouter;
    private LoggingService m_Logging;
    private EventAdmin m_EventAdmin;
    private RemoteSettings m_RemoteSettings;

    @Before
    public void setUp()
    {
        m_SUT = new SocketMessageListener();
        m_MessageRouter = mock(MessageRouter.class);
        m_SUT.setMessageRouter(m_MessageRouter);
        m_Logging = LoggingServiceMocker.createMock();
        m_SUT.setLoggingService(m_Logging);
        
        m_RemoteSettings = mock(RemoteSettings.class);
        m_SUT.setRemoteSettings(m_RemoteSettings);
        when(m_RemoteSettings.isLogRemoteMessagesEnabled()).thenReturn(true);
        
        // set max message size limit
        when(m_RemoteSettings.getMaxMessageSize()).thenReturn(1024L);        
        
        m_EventAdmin = mock(EventAdmin.class);
        m_SUT.setEventAdmin(m_EventAdmin);
    }
    
    /**
     * Verify the factory name matches the class name
     */
    @Test
    public void testFactoryName()
    {
        assertThat(SocketMessageListener.FACTORY_NAME, is(m_SUT.getClass().getName()));
    }
    
    /**
     * Verify run method exits when deactivate is called.
     */
    @Test(timeout = 4000)
    public void testDeactivate() throws InterruptedException, IOException
    {
        final TerraHarvestMessage message = TerraHarvestMessageHelper.createBaseMessage();
     
        // simulate received message from socket
        Socket socket = mock(Socket.class);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        message.writeDelimitedTo(output);
        
        // Circular array is used to keep the SocketMessageListener from terminating before deactivate is called.  If 
        // we do not use this class and pass in a regular InputStream object, the SocketMessageListener will read from 
        // the InputStream once. Then it will try to read again, but data is no longer there so the 
        // SocketMessageListener will set running to false and will deactivate itself.
        InputStream inStream = new CircularArrayInputStream(output.toByteArray());
      
        when(socket.getInputStream()).thenReturn(inStream);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SocketMessageListener.SOCKET_PROP_KEY, socket);
        // mock the channel that will be sent in map
        AbstractSocketChannel channel = mock(AbstractSocketChannel.class);
        props.put(SocketMessageListener.CHANNEL_PROP_KEY, channel);
        m_SUT.activate(props);
        
        Thread thread = new Thread(m_SUT);
        thread.start();
       
        // give thread time to start
        Thread.sleep(1000);
        
        //make sure that the thread is alive
        assertThat(thread.isAlive(), is(true));
        
        // should cause thread to stop
        m_SUT.deactivate();
        
        // wait for thread to stop or just timeout
        thread.join(1000);
        
        // verify thread has stopped in case of timeout
        assertThat(thread.isAlive(), is(false));
    }
    
    /**
     * Verify the thread will read messages as long as running and passed to router, also verify event for 
     * syncing of addresses is posted to the Event Admin.
     * 
     * Verify cleanup of socket and stream before thread stops as well as removing socket from lookup
     * 
     * Test that the channel is updated with the number of bytes received.
     * 
     * Test that logging does not occur if disabled.
     */
    @Test
    public void testRun() throws InterruptedException, IOException
    {
        //disable logging
        when(m_RemoteSettings.isLogRemoteMessagesEnabled()).thenReturn(false);
    
        int sourceId = 100;
        
        // setup the message to be received
        BaseNamespace baseMessage = BaseNamespace.newBuilder().
                setType(BaseMessageType.RequestControllerInfo).
                build();
        
        final TerraHarvestMessage message = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(sourceId, 1, Namespace.Base, 100, baseMessage);
        
        final TerraHarvestMessage message2 = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(sourceId, 2, Namespace.Base, 101, baseMessage);
        
        final TerraHarvestMessage message3 = 
              TerraHarvestMessageHelper.createTerraHarvestMessage(sourceId + 100, 1, Namespace.Base, 102, baseMessage);
     
        // simulate received message from socket
        Map<String, Object> props = new HashMap<String, Object>();
        Socket socket = mock(Socket.class);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        message.writeDelimitedTo(output);
        message2.writeDelimitedTo(output);
        message3.writeDelimitedTo(output);
        
        InputStream inStream = spy(new ByteArrayInputStream(output.toByteArray()));
        when(socket.getInputStream()).thenReturn(inStream);
        props.put(SocketMessageListener.SOCKET_PROP_KEY, socket);
        // mock the channel that will be sent in map
        AbstractSocketChannel channel = mock(AbstractSocketChannel.class);
        props.put(SocketMessageListener.CHANNEL_PROP_KEY, channel);
        m_SUT.activate(props);
        
        Thread thread = new Thread(m_SUT);
        thread.start();
        
        // the socket should read in each message and parse as the same message
        verify(m_MessageRouter, timeout(1000)).handleMessage(message, channel);
        verify(m_MessageRouter, timeout(1000)).handleMessage(message2, channel);
        verify(m_MessageRouter, timeout(1000)).handleMessage(message3, channel);
        
        // verify event is sent to notify remotechannellookupimpl to sync channel, should happen once
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        //give time for thread to start and for mock interactions to register
        verify(m_EventAdmin, timeout(1000)).sendEvent(eventCaptor.capture());
        
        //Need to iterate through events this way. End of input stream is executed before 
        //a check can be made for the NEW/CHANGED topic. This is because input stream is a 
        //set size and the message sent on the stream is the only data.
        List<Event> postedEvents = eventCaptor.getAllValues();
        
        //Assert that the first message caused a new or changed channel id event.
        Event event1 = postedEvents.get(0);
        assertThat(event1.getTopic(), is(RemoteConstants.TOPIC_NEW_OR_CHANGED_CHANNEL_ID));
        assertThat((AbstractSocketChannel)event1.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((Integer)event1.getProperty(RemoteConstants.EVENT_PROP_SYS_ID), is(sourceId));

        // verify event is posted to notify remotechannellookupimpl to remove channel
        verify(m_EventAdmin, timeout(1000)).postEvent(eventCaptor.capture());
        
        //Assert that a remove channel event is posted.
        Event event3 = eventCaptor.getValue();
        assertThat(event3.getTopic(), is(RemoteConstants.TOPIC_REMOVE_CHANNEL));
        assertThat((AbstractSocketChannel)event3.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        
        //SocketMessageListener exits out because input stream passed to message listener is a set size. 
        // therefore deactivate does not need to be called. (i.e. the message read in is the only message
        // on the stream).
        thread.join(100);
        
        // ensure input stream and socket gets closed
        verify(inStream).close();
        verify(socket).close();
        
        // verify bytes received is correct
        Long expectedBytesReceived = 
                (long)(message.getSerializedSize() + message2.getSerializedSize() + message3.getSerializedSize());
        verify(channel).setBytesReceived(expectedBytesReceived);
    
        // verify no logging
        verify(m_Logging, times(0)).debug(anyString());
    }
    
    /**
     * Verify that the remote ID is updated when equal to the max integer value.
     */
    @Test
    public void testRunWithMessageMaxSourceId() throws IOException
    {
        // setup the message to be received
        BaseNamespace baseMessage = BaseNamespace.newBuilder().
                setType(BaseMessageType.RequestControllerInfo).
                build();
        
        final TerraHarvestMessage message = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(Integer.MAX_VALUE, 1, Namespace.Base, 102, 
                        baseMessage);
        
        final TerraHarvestMessage message2 = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(100, 1, Namespace.Base, 100, baseMessage);
        
        // simulate received message from socket
        Map<String, Object> props = new HashMap<String, Object>();
        Socket socket = mock(Socket.class);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        message.writeDelimitedTo(output);
        message2.writeDelimitedTo(output);
        
        InputStream inStream = spy(new ByteArrayInputStream(output.toByteArray()));
        when(socket.getInputStream()).thenReturn(inStream);
        props.put(SocketMessageListener.SOCKET_PROP_KEY, socket);
        // mock the channel that will be sent in map
        AbstractSocketChannel channel = mock(AbstractSocketChannel.class);
        props.put(SocketMessageListener.CHANNEL_PROP_KEY, channel);
        m_SUT.activate(props);
        
        Thread thread = new Thread(m_SUT);
        thread.start();
        
        // the socket should read in each message and parse as the same message
        verify(m_MessageRouter, timeout(1000)).handleMessage(message, channel);
        verify(m_MessageRouter, timeout(1000)).handleMessage(message2, channel);
        
        // verify event is sent to notify remotechannellookupimpl to sync channel, should happen twice, once to set
        // the remote channel to max integer ID and once to set actual ID.
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        //give time for thread to start and for mock interactions to register
        verify(m_EventAdmin, timeout(1000).times(2)).sendEvent(eventCaptor.capture());
        
        //Need to iterate through events this way. End of input stream is executed before 
        //a check can be made for the NEW/CHANGED topic. This is because input stream is a 
        //set size and the message sent on the stream is the only data.
        List<Event> postedEvents = eventCaptor.getAllValues();
        
        //Assert that the first message caused a new or changed channel id event.
        Event event1 = postedEvents.get(0);
        assertThat(event1.getTopic(), is(RemoteConstants.TOPIC_NEW_OR_CHANGED_CHANNEL_ID));
        assertThat((AbstractSocketChannel)event1.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((Integer)event1.getProperty(RemoteConstants.EVENT_PROP_SYS_ID), is(Integer.MAX_VALUE));
        
        //Assert that the second message caused a new or changed channel id event.
        Event event2 = postedEvents.get(1);
        assertThat(event2.getTopic(), is(RemoteConstants.TOPIC_NEW_OR_CHANGED_CHANNEL_ID));
        assertThat((AbstractSocketChannel)event2.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((Integer)event2.getProperty(RemoteConstants.EVENT_PROP_SYS_ID), is(100));
    }
    
    /**
     * Test the message max size limit.
     * @throws IOException Thrown on failure to establish socket.
     * @throws InterruptedException Thrown if thread is interrupted during sleep to initialize socket.
     */
    @Test
    public void testRunWithMessageSizeError() throws IOException, InterruptedException
    {
        // setup the message to be received
        byte[] tooBig = new byte[1025]; // a byte array that is just 1 byte too big
        for (int i = 0; i < tooBig.length; i++)
        {
            tooBig[i] = 0x01;
        }
        
        BaseNamespace baseMessage = BaseNamespace.newBuilder().
                setType(BaseMessageType.RequestControllerInfo).
                setData(ByteString.copyFrom(tooBig)).
                build();
        
        final TerraHarvestMessage message = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(100, 1, Namespace.Base, 100, baseMessage);
     
        // simulate received message from socket
        Map<String, Object> props = new HashMap<String, Object>();
        Socket socket = mock(Socket.class);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        message.writeDelimitedTo(output);
        
        InputStream inStream = spy(new ByteArrayInputStream(output.toByteArray()));
        when(socket.getInputStream()).thenReturn(inStream);
        props.put(SocketMessageListener.SOCKET_PROP_KEY, socket);
        // mock the channel that will be sent in map
        AbstractSocketChannel channel = mock(AbstractSocketChannel.class);
        props.put(SocketMessageListener.CHANNEL_PROP_KEY, channel);
        m_SUT.activate(props);
        
        Thread thread = new Thread(m_SUT);
        thread.start();
                
        // should exit out immediately without a call to deactivate since input stream is closed
        thread.join(1000);
        
        assertThat(thread.isAlive(), is(false));
        
        // ensure input stream and socket gets closed
        verify(inStream).close();
        verify(socket).close();
        
        verify(m_Logging).error(eq("Error reading message: %s"), contains("Message over Max Size"));
    }
    
    /**
     * Verify that exception is logged appropriately when input stream does not close correctly.
     */
    @Test
    public void testRunWithCloseInputException() throws IOException, InterruptedException
    {
        int sourceId = 100;
        
        // setup the message to be received
        BaseNamespace baseMessage = BaseNamespace.newBuilder().
                setType(BaseMessageType.RequestControllerInfo).
                build();
        
        final TerraHarvestMessage message = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(sourceId, 1, Namespace.Base, 100, baseMessage);

        // simulate received message from socket
        Map<String, Object> props = new HashMap<String, Object>();
        Socket socket = mock(Socket.class);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        message.writeDelimitedTo(output);
        
        InputStream inStream = spy(new ByteArrayInputStream(output.toByteArray()));
        when(socket.getInputStream()).thenReturn(inStream);
        doThrow(IOException.class).when(inStream).close();
        props.put(SocketMessageListener.SOCKET_PROP_KEY, socket);
        // mock the channel that will be sent in map
        AbstractSocketChannel channel = mock(AbstractSocketChannel.class);
        props.put(SocketMessageListener.CHANNEL_PROP_KEY, channel);
        m_SUT.activate(props);
        
        Thread thread = new Thread(m_SUT);
        thread.start();
        
        // wait to give thread time to start and try to read message
        Thread.sleep(1000);
        
        //SocketMessageListener exits out because input stream passed to message listener is a set size. 
        //therefore deactivate does not need to be called. (i.e. the message read in is the only message
        //on the stream).
        thread.join(100);
        
        verify(m_Logging).error(Mockito.any(IOException.class), eq("Unable to close input stream")); 
    }
    
    /**
     * Verify that the exception is logged appropriately when the socket does not close correctly.
     */
    @Test
    public void testRunWithCloseSocketException() throws IOException, InterruptedException
    {
        int sourceId = 100;
        
        // setup the message to be received
        BaseNamespace baseMessage = BaseNamespace.newBuilder().
                setType(BaseMessageType.RequestControllerInfo).
                build();
        
        final TerraHarvestMessage message = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(sourceId, 1, Namespace.Base, 100, baseMessage);

        // simulate received message from socket
        Map<String, Object> props = new HashMap<String, Object>();
        Socket socket = mock(Socket.class);
        doThrow(IOException.class).when(socket).close();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        message.writeDelimitedTo(output);
        
        InputStream inStream = spy(new ByteArrayInputStream(output.toByteArray()));
        when(socket.getInputStream()).thenReturn(inStream);
        props.put(SocketMessageListener.SOCKET_PROP_KEY, socket);
        // mock the channel that will be sent in map
        AbstractSocketChannel channel = mock(AbstractSocketChannel.class);
        props.put(SocketMessageListener.CHANNEL_PROP_KEY, channel);
        m_SUT.activate(props);
        
        Thread thread = new Thread(m_SUT);
        thread.start();
        
        // wait to give thread time to start and try to read message
        Thread.sleep(1000);
        
        //SocketMessageListener exits out because input stream passed to message listener is a set size. 
        //therefore deactivate does not need to be called. (i.e. the message read in is the only message
        //on the stream).
        thread.join(100);
        
        //Verify that the log service is called when an exception occurs while closing the socket.
        verify(m_Logging).error(Mockito.any(IOException.class), eq("Unable to close socket"));
    }
    
    /**
     * Verify if the socket throws an exception, the thread will stop.
     */
    @Test
    public void testRunWithSocketException() throws InterruptedException, IOException
    {
        Socket socket = mock(Socket.class);
        InputStream inStream = mock(InputStream.class);
        when(socket.getInputStream()).thenReturn(inStream);
        when(inStream.read()).thenThrow(new IOException("socket exception"));
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SocketMessageListener.SOCKET_PROP_KEY, socket);
        m_SUT.activate(props);
        
        Thread thread = new Thread(m_SUT);
        thread.start();
        
        // wait to give thread time to start and try to use socket
        Thread.sleep(1000);
        
        thread.join(100);
        // verify thread has stopped because of the socket exception
        assertThat(thread.isAlive(), is(false));
        
        // ensure input stream and socket gets closed
        verify(inStream).close();
        verify(socket).close();        
    }
    
    /**
     * Verify the thread will read a message even after a failure.
     */
    @Test
    public void testRunAfterBadMessage() throws InterruptedException, IOException
    {
        // setup the message to be received
        BaseNamespace baseMessage = BaseNamespace.newBuilder().
                setType(BaseMessageType.RequestControllerInfo).
                build();
        
        final TerraHarvestMessage message = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.Base, 100, baseMessage);
     
        Map<String, Object> props = new HashMap<String, Object>();
        Socket socket = mock(Socket.class);
        
        // setup data coming in, first garbage then good message
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] garbage = new byte[] { 0x3, 0x4d, 0x7e, 0x3f };  // first byte is size, last 3 bytes is just garbage
        output.write(garbage);
        message.writeDelimitedTo(output);
        
        InputStream inStream = new ByteArrayInputStream(output.toByteArray());
        when(socket.getInputStream()).thenReturn(inStream);
        props.put(SocketMessageListener.SOCKET_PROP_KEY, socket);
        // mock the channel that will be sent in map
        AbstractSocketChannel channel = mock(AbstractSocketChannel.class);
        props.put(SocketMessageListener.CHANNEL_PROP_KEY, channel);
        m_SUT.activate(props);
        
        Thread thread = new Thread(m_SUT);
        thread.start();
        
        // wait to give thread time to start and try to read message
        Thread.sleep(1000);
        
        // the socket should read in the message and parse as the same message
        verify(m_MessageRouter).handleMessage(message, channel);
        
        // thread should complete on its own since it runs out of messages after 2
        thread.join(100);
        assertThat(thread.isAlive(), is(false));
    }
    
    /**
     * Verify the thread will stop if it reaches the end of the input stream
     */
    @Test
    public void testEndOfSocketFileStream() throws InterruptedException, IOException
    {
        // simulate EOF from socket
        Map<String, Object> props = new HashMap<String, Object>();
        Socket socket = mock(Socket.class);
        InputStream inStream = mock(InputStream.class);
        when(socket.getInputStream()).thenReturn(inStream);
        // when a socket is closed by the client, the input stream will return -1 signaling an EOF
        when(inStream.read()).thenReturn(-1); 
        props.put(SocketMessageListener.SOCKET_PROP_KEY, socket);
        m_SUT.activate(props);
        
        Thread thread = new Thread(m_SUT);
        thread.start();
        
        // thread should stop by itself since the input stream is at the end of file
        thread.join(300);
        
        assertThat(thread.isAlive(), is(false));
        
        // ensure input stream and socket gets closed
        verify(inStream).close();
        verify(socket).close();
    }
    
    /**
     * Verify that if the input stream only returns part of the message, the listener will keep on reading until the 
     * whole message is there
     */
    @Test
    public void testRunMultipleReads() throws InterruptedException, IOException
    {
        int sourceId = 100;
        
        // setup the message to be received
        BaseNamespace baseMessage = BaseNamespace.newBuilder().
                setType(BaseMessageType.RequestControllerInfo).
                build();
        
        final TerraHarvestMessage message = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(sourceId, 1, Namespace.Base, 100, baseMessage);

        // simulate received message from socket
        Map<String, Object> props = new HashMap<String, Object>();
        Socket socket = mock(Socket.class);
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        message.writeDelimitedTo(output);
        
        InputStream inStream = mock(InputStream.class);
        // mock initial byte for size
        final int messageSize = output.toByteArray()[0];
        when(inStream.read()).thenReturn(messageSize);
        // mock rest of message returned in two different segments
        when(inStream.read(Mockito.any(byte[].class), anyInt(), anyInt())).thenAnswer(new Answer<Integer>()
        {
            private int step = 0;

            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable
            {
                byte[] buffer = (byte[])invocation.getArguments()[0];
                if (step == 0)  // first read will get the first half of the message
                {
                    System.arraycopy(output.toByteArray(), 1, buffer, 0, 4);
                    step++;
                    return 4;
                }
                
                Thread.sleep(50); // simulate break between reads
                // copy in the rest of the message buffer to be read back
                System.arraycopy(output.toByteArray(), 5, buffer, 4, messageSize - 4);
                step = 0; // reset to first step to get 1st half next time
                return 4;
            }
        });
        
        when(socket.getInputStream()).thenReturn(inStream);
        props.put(SocketMessageListener.SOCKET_PROP_KEY, socket);
        // mock the channel that will be sent in map
        AbstractSocketChannel channel = mock(AbstractSocketChannel.class);
        props.put(SocketMessageListener.CHANNEL_PROP_KEY, channel);
        m_SUT.activate(props);
        
        Thread thread = new Thread(m_SUT);
        thread.start();
        
        // wait to give thread time to start and try to read message
        Thread.sleep(1000);
        
        // the socket should read in the message and parse as the same message
        verify(m_MessageRouter, atLeastOnce()).handleMessage(message, channel);
        
        // thread will continue to read in the same message over and over, so must deactivate
        m_SUT.deactivate();
        
        thread.join(300);
        
        assertThat(thread.isAlive(), is(false));
        
        // ensure input stream and socket gets closed
        verify(inStream).close();
        verify(socket).close();
        
        // verify debug logging
        verify(m_Logging, atLeast(1)).debug(anyString());       
    }
    
    /**
     * Verify that if the input stream reaches EOF in the middle of a message the thread will stop.
     */
    @Test
    public void testRunMultipleReadsWithEof() throws InterruptedException, IOException
    {
        // simulate received message from socket
        Map<String, Object> props = new HashMap<String, Object>();
        Socket socket = mock(Socket.class);
        
        InputStream inStream = mock(InputStream.class);
        // mock initial byte for size, will only return 3 bytes before reaching EOF
        when(inStream.read()).thenReturn(6);
        // mock rest of message returned in two different segments
        when(inStream.read(Mockito.any(byte[].class), anyInt(), anyInt())).thenAnswer(new Answer<Integer>()
        {
            private int step = 0;

            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable
            {
                byte[] buffer = (byte[])invocation.getArguments()[0];
                if (step == 0)  // first read will get the first half of the message
                {
                    buffer[0] = 0x01;
                    buffer[1] = 0x02;
                    buffer[2] = 0x03;
                    step++;
                    return 3;
                }
                
                return -1;  // don't reset step count so EOF will be returned forever
            }
        });
        
        when(socket.getInputStream()).thenReturn(inStream);
        props.put(SocketMessageListener.SOCKET_PROP_KEY, socket);
        m_SUT.activate(props);
        
        Thread thread = new Thread(m_SUT);
        thread.start();
        
        // should exit out immediately without a call to deactivate since EOF is reached
        thread.join(1000);
        
        assertThat(thread.isAlive(), is(false));
        
        // ensure input stream and socket gets closed
        verify(inStream).close();
        verify(socket).close();
    }
    
    /**
     * Verify that if the input stream reads the message size as 0 for a message it will read a message after that.
     */
    @Test
    public void testRunWithEmptyMessage() throws InterruptedException, IOException
    {
        // setup the message to be received
        BaseNamespace baseMessage = BaseNamespace.newBuilder().
                setType(BaseMessageType.RequestControllerInfo).
                build();
        
        final TerraHarvestMessage message = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.Base, 100, baseMessage);
     
        Map<String, Object> props = new HashMap<String, Object>();
        Socket socket = mock(Socket.class);
        
        // setup data coming in, first size 0 message then actual message
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(0);  // all messages start with an octet count, this one is zero
        message.writeDelimitedTo(output);
        
        InputStream inStream = new ByteArrayInputStream(output.toByteArray());
        when(socket.getInputStream()).thenReturn(inStream);
        props.put(SocketMessageListener.SOCKET_PROP_KEY, socket);
        // mock the channel that will be sent in map
        AbstractSocketChannel channel = mock(AbstractSocketChannel.class);
        props.put(SocketMessageListener.CHANNEL_PROP_KEY, channel);
        m_SUT.activate(props);
        
        Thread thread = new Thread(m_SUT);
        thread.start();
        
        // wait to give thread time to start and try to read message
        Thread.sleep(1000);
        
        // the socket should read in the message and parse as the same message
        verify(m_MessageRouter).handleMessage(message, channel);
        
        // thread should complete on its own since it runs out of messages after 2
        thread.join(100);
        assertThat(thread.isAlive(), is(false));
    }
    
    /**
     * Verify that if the input stream is closed in the middle of reading, the thread will stop.
     */
    @Test
    public void testRunSocketClosedDuringRead() throws InterruptedException, IOException
    {
        // simulate received message from socket
        Map<String, Object> props = new HashMap<String, Object>();
        Socket socket = mock(Socket.class);
        
        InputStream inStream = mock(InputStream.class);
        // mock initial size read to throw exception
        when(inStream.read()).thenThrow(new SocketException());
        when(socket.getInputStream()).thenReturn(inStream);
        props.put(SocketMessageListener.SOCKET_PROP_KEY, socket);
        m_SUT.activate(props);
        
        Thread thread = new Thread(m_SUT);
        thread.start();
        
        // should exit out immediately without a call to deactivate since input stream is closed
        thread.join(1000);
        
        assertThat(thread.isAlive(), is(false));
        
        // ensure input stream and socket gets closed
        verify(inStream).close();
        verify(socket).close();
        
        // no other way to check, but we don't want a stack trace to be logged as code should handle this as a normal
        // condition
        verify(m_Logging, never()).error(Mockito.any(Throwable.class), anyString());
    }
    
    /**
     * This class is used to provide a mock of constant data for a SocketMessageListener.
     *
     * @author nickmarcucci
     *
     */
    private class CircularArrayInputStream extends InputStream
    {
        /**
         * The byte array that this input stream is reading from.
         */
        private byte[] m_CircleArray; 
        
        /**
         * Index in the byte array.
         */
        private int m_Index = 0;
        
        CircularArrayInputStream(byte[] array)
        {
            m_CircleArray = array;
        }
               
        /* (non-Javadoc)
         * @see java.io.InputStream#read()
         */
        @Override
        public int read() throws IOException
        {
            //make sure that index is reset to 0 if 
            //we get to the end.
            if(m_Index >= m_CircleArray.length)
            {
                m_Index = 0;
            }
                        
            return m_CircleArray[m_Index++];
        }
    }
}
