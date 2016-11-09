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

import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CCommException.FormatProblem;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.ccomm.transport.TransportLayerFactory;
import mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.ChannelStatus;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.types.remote.RemoteChannelTypeEnum;
import mil.dod.th.ose.remote.QueuedMessageSender;
import mil.dod.th.ose.remote.TerraHarvestMessageHelper;
import mil.dod.th.ose.remote.api.RemoteSettings;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;

/**
 * @author Dave Humeniuk
 *
 */
public class TestTransportChannelImpl
{
    private TransportChannelImpl m_SUT;
    private LoggingService m_Logging;

    @Mock
    private CustomCommsService m_CustomCommsService;
    
    @Mock
    private TransportLayer m_TransportLayer;

    @Mock
    private AddressManagerService m_AddressManagerService;

    @Mock
    private Address m_Address;

    @Mock
    private ComponentFactory m_ListenerFactory;

    @Mock
    private ComponentInstance m_Instance;

    @Mock
    private ComponentInstance m_SenderInstance;

    @Mock
    private QueuedMessageSender m_MessageSender;

    @Mock
    private ComponentFactory m_SenderFactory;

    @Mock
    private RemoteSettings m_RemoteSettings;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        m_SUT = new TransportChannelImpl();
        m_Logging = LoggingServiceMocker.createMock();
        m_SUT.setLoggingService(m_Logging);
        
        m_SUT.setCustomCommsService(m_CustomCommsService);
        m_SUT.setAddressManagerService(m_AddressManagerService);
        
        // mock out listener factory
        m_SUT.setTransportMessageListenerFactory(m_ListenerFactory);
        when(m_ListenerFactory.newInstance(Mockito.any(Dictionary.class))).thenReturn(m_Instance);
        
        // mock out message sender factory
        m_SUT.setQueuedMessageSenderFactory(m_SenderFactory);
        when(m_SenderFactory.newInstance(Mockito.any(Dictionary.class))).thenReturn(m_SenderInstance);
        when(m_SenderInstance.getInstance()).thenReturn(m_MessageSender);
        
        // mock out remote settings
        m_SUT.setRemoteSettings(m_RemoteSettings);
        when(m_RemoteSettings.isLogRemoteMessagesEnabled()).thenReturn(true);
    }
    
    private void activateChannel(boolean isConnectionOriented, boolean isConnected, Map<String, Object> extraProps)
        throws CCommException
    {
        // mock items need to send data
        TransportLayerFactory transportFactory = mock(TransportLayerFactory.class);
        TransportLayerCapabilities capabilities = new TransportLayerCapabilities();
        capabilities.setConnectionOriented(isConnectionOriented);
        
        when(m_CustomCommsService.getTransportLayer("tl1")).thenReturn(m_TransportLayer);
        when(m_AddressManagerService.getOrCreateAddress("remote-addr")).thenReturn(m_Address);
        when(m_TransportLayer.isAvailable(m_Address)).thenReturn(isConnected);
        if (isConnectionOriented)
        {
            when(m_TransportLayer.isConnected()).thenReturn(isConnected);
        }
        else
        {
            when(m_TransportLayer.isConnected()).thenReturn(false);
        }
        when(m_TransportLayer.getFactory()).thenReturn(transportFactory);
        when(transportFactory.getTransportLayerCapabilities()).thenReturn(capabilities);

        // setup a channel
        Map<String, Object> actualProps = new HashMap<String, Object>();
        actualProps.put(TransportChannelImpl.TRANSPORT_NAME_PROP_KEY, "tl1");
        actualProps.put(TransportChannelImpl.LOCAL_ADDRESS_PROP_KEY, "local-addr");
        actualProps.put(TransportChannelImpl.REMOTE_ADDRESS_PROP_KEY, "remote-addr");

        if (extraProps != null)
        {
            actualProps.putAll(extraProps);
        }

        m_SUT.activate(actualProps);

        //all new channels start out as unknown, unless a connection-oriented transport is already connected
        if (isConnectionOriented && isConnected)
        {
            assertThat(m_SUT.getStatus(), is(ChannelStatus.Active));
        }
        else
        {
            assertThat(m_SUT.getStatus(), is(ChannelStatus.Unknown));
        }
        
        assertThat(m_SUT.getTransportLayerName(), is("tl1"));
        assertThat(m_SUT.getLocalMessageAddress(), is("local-addr"));
        assertThat(m_SUT.getRemoteMessageAddress(), is("remote-addr"));
    }

    /**
     * Verify the factory name matches the class name
     */
    @Test
    public void testFactoryName()
    {
        assertThat(TransportChannelImpl.FACTORY_NAME, is(m_SUT.getClass().getName()));
    }
    
    /**
     * Verify that the channel status and props are initialized.
     * 
     * Verify message listener is created and started on a thread.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    // this timeout is less then time the thread below blocks to prove it is blocking on a separate thread
    @Test(timeout = 2000)
    public void testActivate() throws InterruptedException, CCommException
    {
        // mock the message listener and its run method to block, need to block to verify run method is called on 
        // separate thread
        TransportMessageListener transportMessageListener = mock(TransportMessageListener.class);
        when(m_Instance.getInstance()).thenReturn(transportMessageListener);
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                Thread.sleep(200000); // simulate a blocking call
                return null;
            }
        }).when(transportMessageListener).run();

        // Connection-less channel that is available
        activateChannel(false, true, null);

        ArgumentCaptor<Dictionary> dictionaryCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(m_ListenerFactory).newInstance(dictionaryCaptor.capture());
        assertThat((TransportChannelImpl)dictionaryCaptor.getValue().get(TransportMessageListener.CHANNEL_PROP_KEY), 
                is(m_SUT));
        
        Thread.sleep(500); // allow thread to run
        
        // verify listener was told to run and on a separate thread, know on a separate thread because this test
        // didn't timeout waiting on the run method to exit
        verify(transportMessageListener).run();
    }

    @Test
    public void testDeactivate() throws CCommException
    {
        // Connection-less channel that is available
        activateChannel(false, true, null);
        
        m_SUT.deactivate();
        
        verify(m_Instance).dispose();
        verify(m_TransportLayer, never()).disconnect();
    }

    @Test
    public void testDeactivateDisconnect() throws CCommException
    {
        // Connection-oriented channel that is connected
        activateChannel(true, true, null);
        
        m_SUT.deactivate();
        
        verify(m_Instance).dispose();
        verify(m_TransportLayer).disconnect();
    }

    @Test
    public void testDeactivateNoDisconnect() throws CCommException
    {
        // Connection-oriented channel that is disconnected
        activateChannel(true, false, null);
        
        m_SUT.deactivate();
        
        verify(m_Instance).dispose();
        verify(m_TransportLayer, never()).disconnect();
    }

    /**
     * Verify the message is sent using the given transport layer to the address in binary form.
     */
    @Test
    public void testTrySendMessage() throws IOException, CCommException
    {
        // Connection-less channel that is available
        activateChannel(false, true, null);

        // construct a single base namespace message to verify sent to socket
        TerraHarvestMessage message = TerraHarvestMessageHelper.createBaseMessage();
        
        // replay
        boolean sent = m_SUT.trySendMessage(message);
        assertThat(sent, is(true));
        
        // verify mock socket's output stream was asked to send message in binary form with delimiter at beginning
        byte[] binaryMessage = message.toByteArray();
        // first byte sent will be the varint (https://developers.google.com/protocol-buffers/docs/encoding#varints)
        // representing the length of the message following
        byte[] fullBinaryMessage = new byte[binaryMessage.length + 1]; // leave room for delimiter
        fullBinaryMessage[0] = (byte)binaryMessage.length;
        System.arraycopy(binaryMessage, 0, fullBinaryMessage, 1, binaryMessage.length);
        
        // should pass complete message as byte buffer to correct address
        verify(m_TransportLayer).send(ByteBuffer.wrap(fullBinaryMessage), m_Address);
        
        // verify logging is enabled
        verify(m_Logging, times(1)).debug(anyString(), anyVararg());
    }

    /**
     * Verify the message is sent using the given transport layer to the address in binary form when the transport
     * layer (connection oriented) is already connected.
     */
    @Test
    public void testTrySendMessageConnected() throws IOException, CCommException
    {
        // Connection-oriented channel that is connected
        activateChannel(true, true, null);

        // construct a single base namespace message to verify sent to socket
        TerraHarvestMessage message = TerraHarvestMessageHelper.createBaseMessage();
        
        // replay
        boolean sent = m_SUT.trySendMessage(message);
        assertThat(sent, is(true));

        //check that status is connected
        assertThat(m_SUT.getStatus(), is(ChannelStatus.Active));

        // verify mock socket's output stream was asked to send message in binary form with delimiter at beginning
        byte[] binaryMessage = message.toByteArray();
        // first byte sent will be the varint (https://developers.google.com/protocol-buffers/docs/encoding#varints)
        // representing the length of the message following
        byte[] fullBinaryMessage = new byte[binaryMessage.length + 1]; // leave room for delimiter
        fullBinaryMessage[0] = (byte)binaryMessage.length;
        System.arraycopy(binaryMessage, 0, fullBinaryMessage, 1, binaryMessage.length);
        
        // should pass complete message as byte buffer
        verify(m_TransportLayer, never()).connect(m_Address);
        verify(m_TransportLayer).send(ByteBuffer.wrap(fullBinaryMessage));
        verify(m_TransportLayer, never()).send(ByteBuffer.wrap(fullBinaryMessage), m_Address);
        
        // verify logging is enabled
        verify(m_Logging, times(1)).debug(anyString(), anyVararg());
    }

    /**
     * Verify the message is sent using the given transport layer to the address in binary form when the transport
     * layer (connection oriented) is disconnected.
     */
    @Test
    public void testTrySendMessageDisconnected() throws IOException, CCommException
    {
        // Connection-oriented channel that is disconnected
        activateChannel(true, false, null);

        // construct a single base namespace message to verify sent to socket
        TerraHarvestMessage message = TerraHarvestMessageHelper.createBaseMessage();
        
        // replay
        boolean sent = m_SUT.trySendMessage(message);
        assertThat(sent, is(true));

        //check that status is connected
        assertThat(m_SUT.getStatus(), is(ChannelStatus.Active));

        // verify mock socket's output stream was asked to send message in binary form with delimiter at beginning
        byte[] binaryMessage = message.toByteArray();
        // first byte sent will be the varint (https://developers.google.com/protocol-buffers/docs/encoding#varints)
        // representing the length of the message following
        byte[] fullBinaryMessage = new byte[binaryMessage.length + 1]; // leave room for delimiter
        fullBinaryMessage[0] = (byte)binaryMessage.length;
        System.arraycopy(binaryMessage, 0, fullBinaryMessage, 1, binaryMessage.length);
        
        // should pass complete message as byte buffer
        verify(m_TransportLayer).connect(m_Address);
        verify(m_TransportLayer).send(ByteBuffer.wrap(fullBinaryMessage));
        verify(m_TransportLayer, never()).send(ByteBuffer.wrap(fullBinaryMessage), m_Address);
        
        // verify logging is enabled
        verify(m_Logging, times(2)).debug(anyString(), anyVararg());
    }

    /**
     * Verify the message is not sent if the endpoint is not available according to transport layer.
     */
    @Test
    public void testTrySendMessageConnectException() throws IOException, CCommException
    {
        // Connection-oriented channel that is not available
        activateChannel(true, false, null);

        // construct a single base namespace message to verify sent to socket
        TerraHarvestMessage message = TerraHarvestMessageHelper.createBaseMessage();

        doThrow(new CCommException(FormatProblem.TIMEOUT)).when(m_TransportLayer).connect(Mockito.any(Address.class));

        // replay
        boolean sent = m_SUT.trySendMessage(message);
        assertThat("message is not sent because unable to connect", sent, is(false));

        //check that status is unavailable
        assertThat(m_SUT.getStatus(), is(ChannelStatus.Unavailable));

        doThrow(new IllegalStateException()).when(m_TransportLayer).connect(Mockito.any(Address.class));

        // replay
        sent = m_SUT.trySendMessage(message);
        assertThat(sent, is(true));

        //check that status is still active
        assertThat(m_SUT.getStatus(), is(ChannelStatus.Active));
    }

    /**
     * Verify the message is sent using the given transport layer to the address in binary form, but no debug message 
     * is logged, because logging is disabled.
     */
    @Test
    public void testTrySendMessageLoggingDisabled() throws IOException, CCommException
    {
        // Connection-less channel that is available
        activateChannel(false, true, null);

        //disable logging
        when(m_RemoteSettings.isLogRemoteMessagesEnabled()).thenReturn(false);
        
        // construct a single base namespace message to verify sent to socket
        TerraHarvestMessage message = TerraHarvestMessageHelper.createBaseMessage();
        
        // replay
        boolean sent = m_SUT.trySendMessage(message);
        assertThat(sent, is(true));
        
        // verify mock socket's output stream was asked to send message in binary form with delimiter at beginning
        byte[] binaryMessage = message.toByteArray();
        // first byte sent will be the varint (https://developers.google.com/protocol-buffers/docs/encoding#varints)
        // representing the length of the message following
        byte[] fullBinaryMessage = new byte[binaryMessage.length + 1]; // leave room for delimiter
        fullBinaryMessage[0] = (byte)binaryMessage.length;
        System.arraycopy(binaryMessage, 0, fullBinaryMessage, 1, binaryMessage.length);
        
        // should pass complete message as byte buffer to correct address
        verify(m_TransportLayer).send(ByteBuffer.wrap(fullBinaryMessage), m_Address);
        
        // verify logging is disabled
        verify(m_Logging, times(0)).debug(anyString());
    }

    /**
     * Verify the exception is thrown if inner exception is thrown by connection-less transport layer
     */
    @Test
    public void testTrySendMessageInnerException1() throws IOException, CCommException
    {
        // Connection-less channel that is available
        activateChannel(false, true, null);

        // construct a single base namespace message to verify sent to socket
        TerraHarvestMessage message = TerraHarvestMessageHelper.createBaseMessage();

        doThrow(new CCommException(FormatProblem.BUFFER_OVERFLOW))
            .when(m_TransportLayer).send(Mockito.any(ByteBuffer.class), Mockito.any(Address.class));

        // replay
        boolean sent = m_SUT.trySendMessage(message);
        assertThat("failed to send message as inner exception occurred", sent, is(false));
        
        //check that status is unavailable
        assertThat(m_SUT.getStatus(), is(ChannelStatus.Unavailable));
    }

    /**
     * Verify the exception is thrown if inner exception is thrown by connection-oriented transport layer
     */
    @Test
    public void testTrySendMessageInnerException2() throws IOException, CCommException
    {
        // Connection-oriented channel that is available
        activateChannel(true, true, null);

        // construct a single base namespace message to verify sent to socket
        TerraHarvestMessage message = TerraHarvestMessageHelper.createBaseMessage();

        doThrow(new CCommException(FormatProblem.BUFFER_OVERFLOW))
            .when(m_TransportLayer).send(Mockito.any(ByteBuffer.class));

        // replay
        boolean sent = m_SUT.trySendMessage(message);
        assertThat("failed to send message as inner exception occurred", sent, is(false));

        //check that status is unavailable
        assertThat(m_SUT.getStatus(), is(ChannelStatus.Unavailable));
    }

    /**
     * Verify the exception is thrown if inner exception is thrown when looking up transport layer
     */
    @Test
    public void testTrySendMessageInvalidTransport() throws IOException, CCommException
    {
        // Connection-less channel that is available
        activateChannel(false, true, null);
        
        // construct a single base namespace message to verify sent to socket
        TerraHarvestMessage message = TerraHarvestMessageHelper.createBaseMessage();
        
        // mock items need to send data
        when(m_CustomCommsService.getTransportLayer("tl1")).thenThrow(new IllegalArgumentException());
        
        // replay
        boolean sent = m_SUT.trySendMessage(message);
        assertThat("failed to send message as transport is invalid", sent, is(false));
        
        //check that status is unavailable
        assertThat(m_SUT.getStatus(), is(ChannelStatus.Unavailable));
    }
    
    /**
     * Verify the message is not sent if the endpoint is not available according to transport layer.
     */
    @Test
    public void testTrySendMessageAddressNotAvailable() throws IOException, CCommException
    {
        // Connection-less channel that is not available
        activateChannel(false, false, null);

        // construct a single base namespace message to verify sent to socket
        TerraHarvestMessage message = TerraHarvestMessageHelper.createBaseMessage();

        // replay
        boolean sent = m_SUT.trySendMessage(message);
        assertThat("message is not sent because endpont is not available", sent, is(false));

        //check that status is unavailable
        assertThat(m_SUT.getStatus(), is(ChannelStatus.Unavailable));
    }

    /**
     * Verify the message is sent using the given socket in binary form if no error sending message.
     */
    @Test
    public void testQueueMessage() throws IOException, CCommException
    {
        // must activate to setup queue
        activateChannel(false, true, null);
        
        // construct a single base namespace message to verify sent to socket
        TerraHarvestMessage message = TerraHarvestMessageHelper.createBaseMessage();
        
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
    public void testGetQueuedMessageCount() throws CCommException
    {
        // must activate to setup queue
        activateChannel(false, true, null);
        
        when(m_MessageSender.getQueuedMessageCount()).thenReturn(100);
        
        assertThat(m_SUT.getQueuedMessageCount(), is(100));
    }
    
    /**
     * Verify the queue can be requested to clear.
     */
    @Test
    public void testClearQueue() throws IOException, CCommException
    {
        // must activate to setup queue
        activateChannel(false, true, null);
        
        //request clear
        m_SUT.clearQueuedMessages();
        
        verify(m_MessageSender).clearQueue();
    }

    /**
     * Verify the exception is thrown if inner exception is thrown when looking up address
     */
    @Test
    public void testTrySendMessageInvalidAddress() throws IOException, CCommException
    {
        // Connection-less channel that is available
        activateChannel(false, true, null);

        // construct a single base namespace message to verify sent to socket
        TerraHarvestMessage message = TerraHarvestMessageHelper.createBaseMessage();

        when(m_AddressManagerService.getOrCreateAddress("remote-addr"))
            .thenThrow(new CCommException(FormatProblem.OTHER));

        // replay
        boolean sent = m_SUT.trySendMessage(message);
        assertThat("failed to send message as address is invalid", sent, is(false));

        //check that status is unavailable
        assertThat(m_SUT.getStatus(), is(ChannelStatus.Unavailable));
    }

    /**
     * Verify the matches method returns true if the transport layer and address properties match
     */
    @Test
    public void testMatches() throws CCommException
    {
        Map<String, Object> extraProps = new HashMap<String, Object>();
        extraProps.put(ComponentConstants.COMPONENT_ID, 23);
        extraProps.put(ComponentConstants.COMPONENT_NAME, TransportChannelImpl.class.getName());
        extraProps.put("some random value that might be there", "blah");
        
        // Connection-less channel that is available
        activateChannel(false, true, extraProps);

        // different props should not match
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(TransportChannelImpl.TRANSPORT_NAME_PROP_KEY, "tl2");
        props.put(TransportChannelImpl.LOCAL_ADDRESS_PROP_KEY, "local-addr2");
        props.put(TransportChannelImpl.REMOTE_ADDRESS_PROP_KEY, "remote-addr2");
        assertThat(m_SUT.matches(props), is(false));
        
        // same props for transport and address should match, should ignore default props of a component set by OSGi
        props.put(TransportChannelImpl.TRANSPORT_NAME_PROP_KEY, "tl1");
        props.put(TransportChannelImpl.LOCAL_ADDRESS_PROP_KEY, "local-addr");
        props.put(TransportChannelImpl.REMOTE_ADDRESS_PROP_KEY, "remote-addr");
        assertThat(m_SUT.matches(props), is(true));
    }
    
    /**
     * Verify that the channel type is set at initialization of the component.
     */
    @Test
    public void testGetChannelType() throws IOException, CCommException
    {
        // setup a channel
        activateChannel(false, true, null);
        
        assertThat(m_SUT.getChannelType(), is(RemoteChannelTypeEnum.TRANSPORT));
    }
    
    /**
     * Verify toString prints a short readable string.
     */
    @Test
    public void testToString() throws CCommException
    {
        // setup a channel
        activateChannel(false, true, null);
        
        assertThat("string rep is combination of layer and addresses",
            m_SUT.toString(), is("tl1;remote=remote-addr;local=local-addr;connectionOriented=false"));
    }
}
