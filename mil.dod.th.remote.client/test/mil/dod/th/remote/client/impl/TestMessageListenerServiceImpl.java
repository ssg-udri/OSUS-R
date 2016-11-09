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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.ControllerInfoData;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.remote.client.ChannelStateCallback;
import mil.dod.th.remote.client.MessageListenerCallback;
import mil.dod.th.remote.client.RemoteMessage;
import mil.dod.th.remote.client.parse.TerraHarvestMessageConverter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class TestMessageListenerServiceImpl
{
    private static int SRC_ID = 1;

    private MessageListenerServiceImpl m_SUT;

    private Semaphore m_CallbackWaitRem;
    private Semaphore m_CallbackWaitRemEx;
    private PipedInputStream m_InputStream;
    private PipedOutputStream m_OutputStream;
    private Logging m_Logging = new Logging();

    @Mock
    private ChannelStateCallback m_ChannelCallback;
    @Mock
    private MessageListenerCallback<?> m_MessageCallback;
    @Mock
    private TerraHarvestMessageConverter m_Converter;
    
    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        
        m_SUT = new MessageListenerServiceImpl();
        m_SUT.setConverter(m_Converter);
        m_SUT.setLogging(m_Logging);
        m_CallbackWaitRem = new Semaphore(0);
        m_CallbackWaitRemEx = new Semaphore(0);
        m_InputStream = new PipedInputStream();
        m_OutputStream = new PipedOutputStream(m_InputStream);

        when(m_Converter.isSupported(Namespace.Base)).thenReturn(true);

        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                m_CallbackWaitRem.release();
                return null;
            }
        }).when(m_ChannelCallback).onChannelRemoved(eq(SRC_ID));

        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                m_CallbackWaitRemEx.release();
                return null;
            }
        }).when(m_ChannelCallback).onChannelRemoved(eq(SRC_ID), Mockito.any(Exception.class));
    }

    @After
    public void tearDown() throws Exception
    {
        // Make sure remote channel and its thread have been removed
        try
        {
            m_SUT.removeRemoteChannel(SRC_ID);
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    /**
     * Verify that a remote channel can be added and removed with appropriate callbacks made.
     */
    @Test
    public void testAddRemoveRemoteChannel() throws InterruptedException, IOException
    {
        final Semaphore readWait = new Semaphore(0);
        InputStream input = mock(InputStream.class);
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                readWait.acquire();
                throw new IOException("close");
            }
        }).when(input).read();
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                readWait.release();
                return null;
            }
        }).when(input).close();

        m_SUT.addRemoteChannel(SRC_ID, input, m_ChannelCallback);

        // Wait to make sure the background thread is running
        Thread.sleep(100);

        m_SUT.removeRemoteChannel(SRC_ID);

        // Wait for onChannelRemoved
        assertThat(m_CallbackWaitRem.tryAcquire(1, TimeUnit.SECONDS), is(true));

        // Should not be able to remove channel twice
        try
        {
            m_SUT.removeRemoteChannel(SRC_ID);
            fail("removeRemoteChannel should throw IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
    }

    /**
     * Verify that remote channel cannot be added if one already exists for the given source ID.
     */
    @Test
    public void testAddDuplicateRemoteChannel()
    {
        m_SUT.addRemoteChannel(SRC_ID, m_InputStream, m_ChannelCallback);

        try
        {
            m_SUT.addRemoteChannel(SRC_ID, m_InputStream, m_ChannelCallback);
            fail("addRemoteChannel should throw IllegalStateException");
        }
        catch (IllegalStateException e)
        {
            // Expected
        }
    }

    /**
     * Verify that a remote channel can be added and removed with a null callback.
     */
    @Test
    public void testAddRemoveRemoteChannelNullCallback() throws InterruptedException
    {
        m_SUT.addRemoteChannel(SRC_ID, m_InputStream, null);

        m_SUT.removeRemoteChannel(SRC_ID);

        Thread.sleep(100);

        // Should not be able to remove channel twice
        try
        {
            m_SUT.removeRemoteChannel(SRC_ID);
            fail("removeRemoteChannel should throw IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
    }

    /**
     * Verify that remote channel cannot be added with a null input stream.
     */
    @Test
    public void testAddRemoteChannelInvalidStream()
    {
        try
        {
            m_SUT.addRemoteChannel(SRC_ID, null, m_ChannelCallback);
            fail("addRemoteChannel should throw IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
    }

    /**
     * Verify that a remote channel is removed when there is an error with the input stream.
     */
    @Test
    public void testRemoveRemoteChannelFromError() throws IOException, InterruptedException
    {
        // replay
        m_SUT.addRemoteChannel(SRC_ID, m_InputStream, m_ChannelCallback);

        // Force channel to throw exception
        m_InputStream.close();

        // Wait for onChannelRemoved with exception
        assertThat(m_CallbackWaitRemEx.tryAcquire(1, TimeUnit.SECONDS), is(true));

        // Should not be able to remove channel after being removed from error
        try
        {
            m_SUT.removeRemoteChannel(SRC_ID);
            fail("removeRemoteChannel should throw IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
    }

    /**
     * Verify that a remote channel is removed when there is an error with the input stream and callback is null.
     */
    @Test
    public void testRemoveRemoteChannelFromErrorNullCallback() throws IOException, InterruptedException
    {
        // replay
        m_SUT.addRemoteChannel(SRC_ID, m_InputStream, null);

        // Force channel to throw exception
        m_InputStream.close();

        Thread.sleep(100);

        // Should not be able to remove channel after being removed from error
        try
        {
            m_SUT.removeRemoteChannel(SRC_ID);
            fail("removeRemoteChannel should throw IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testRegisterCallbackAll() throws InterruptedException, IOException
    {
        final Semaphore callbackWaitMsg = new Semaphore(0);
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                callbackWaitMsg.release();
                return null;
            }
        }).when(m_MessageCallback).handleMessage(Mockito.any(RemoteMessage.class));
        
        RemoteMessage<?> mockedMessage = mock(RemoteMessage.class);
        when(mockedMessage.getNamespace()).thenReturn(Namespace.Base);
        when(m_Converter.convertMessage(Mockito.any(TerraHarvestMessage.class)))
            .thenReturn((RemoteMessage)mockedMessage);

        m_SUT.addRemoteChannel(SRC_ID, m_InputStream, m_ChannelCallback);

        m_SUT.registerCallback(m_MessageCallback);

        // Send a message
        createTestMessage().writeDelimitedTo(m_OutputStream);

        // Wait for message callback
        assertThat(callbackWaitMsg.tryAcquire(2, TimeUnit.SECONDS), is(true));

        ArgumentCaptor<RemoteMessage> message = ArgumentCaptor.forClass(RemoteMessage.class);
        verify(m_MessageCallback).handleMessage(message.capture());

        RemoteMessage<?> remoteMessage = message.getValue();
        assertThat(remoteMessage, notNullValue());

        // Unregister callback and verify it is not called when another message is received
        m_SUT.unregisterCallback(m_MessageCallback);
        createTestMessage().writeDelimitedTo(m_OutputStream);
        assertThat(callbackWaitMsg.tryAcquire(2, TimeUnit.SECONDS), is(false));
    }

    @Test
    public void testRegisterCallbackByNamespace() throws InterruptedException, IOException
    {
        m_SUT.addRemoteChannel(SRC_ID, m_InputStream, m_ChannelCallback);

        for (Namespace namespace : Namespace.values())
        {
            verifyRegisterCallbackByNamespace(namespace);
        }        
    }

    /**
     * Verify that a null message callback throws exception when trying to unregister.
     */
    @Test
    public void testRegisterNullCallback()
    {
        try
        {
            m_SUT.registerCallback(Namespace.Base, null);
            fail("unregisterCallback should throw IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
    }

    @Test
    public void testUnregisterCallback()
    {
        // unregister where none exist
        m_SUT.unregisterCallback(m_MessageCallback);

        // unregister where different one exists
        m_SUT.registerCallback(Namespace.Base, mock(MessageListenerCallback.class));
        m_SUT.registerCallback(Namespace.Base, m_MessageCallback);
        m_SUT.unregisterCallback(m_MessageCallback);
    }

    /**
     * Verify that a null message callback throws exception when trying to unregister.
     */
    @Test
    public void testUnregisterNullCallback()
    {
        try
        {
            m_SUT.unregisterCallback(null);
            fail("unregisterCallback should throw IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
    }

    /**
     * Verify that exceptions thrown by callbacks do not cause the listening thread to crash.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testRecvWithCallbackException() throws IOException, InterruptedException
    {
        doThrow(new RuntimeException("callback error"))
            .when(m_MessageCallback).handleMessage(Mockito.any(RemoteMessage.class));

        RemoteMessage<?> mockedMessage = mock(RemoteMessage.class);
        when(mockedMessage.getNamespace()).thenReturn(Namespace.Base);
        when(m_Converter.convertMessage(Mockito.any(TerraHarvestMessage.class)))
            .thenReturn((RemoteMessage)mockedMessage);

        m_SUT.addRemoteChannel(SRC_ID, m_InputStream, m_ChannelCallback);

        m_SUT.registerCallback(Namespace.Base, m_MessageCallback);

        // Include second callback to verify exception from above doesn't crash receiving thread
        MessageListenerCallback<?> secondCallback = mock(MessageListenerCallback.class);
        m_SUT.registerCallback(Namespace.Base, secondCallback);

        // Send a message
        createTestMessage().writeDelimitedTo(m_OutputStream);

        // Wait for processing to complete
        verify(secondCallback, timeout(2000)).handleMessage((RemoteMessage)mockedMessage);
    }

    private TerraHarvestMessage createTestMessage()
    {
        ControllerInfoData controllerInfo = ControllerInfoData.newBuilder()
             .setName("test")
             .setVersion("1.0")
             .build();

        Namespace namespace = Namespace.Base;
        BaseNamespace namespaceMessage = BaseNamespace.newBuilder()
            .setType(BaseMessageType.ControllerInfo)
            .setData(controllerInfo.toByteString())
            .build();

        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder()
            .setNamespace(namespace)
            .setNamespaceMessage(namespaceMessage.toByteString())
            .build();

        TerraHarvestMessage thMessage = TerraHarvestMessage.newBuilder()
            .setSourceId(SRC_ID)
            .setDestId(0)
            .setEncryptType(EncryptType.NONE)
            .setIsResponse(true)
            .setMessageId(11)
            .setVersion(RemoteConstants.SPEC_VERSION)
            .setTerraHarvestPayload(payload.toByteString())
            .build();

        return thMessage;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void verifyRegisterCallbackByNamespace(Namespace type) throws InterruptedException, IOException
    {
        final Semaphore callbackWaitMsg = new Semaphore(0);
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                callbackWaitMsg.release();
                return null;
            }
        }).when(m_MessageCallback).handleMessage(Mockito.any(RemoteMessage.class));

        switch (type)
        {
            case AssetDirectoryService:
            case Base:
            case EventAdmin:
            {
                RemoteMessage<?> mockedMessage = mock(RemoteMessage.class);
                when(mockedMessage.getNamespace()).thenReturn(type);
                when(m_Converter.convertMessage(Mockito.any(TerraHarvestMessage.class)))
                    .thenReturn((RemoteMessage)mockedMessage);
                when(m_Converter.isSupported(type)).thenReturn(true);

                m_SUT.registerCallback(type, m_MessageCallback);

                // Include second callback to verify the one above is still called
                m_SUT.registerCallback(type, mock(MessageListenerCallback.class));

                // Send a message with callbacks
                createTestMessage().writeDelimitedTo(m_OutputStream);

                // Wait for message callback
                assertThat(callbackWaitMsg.tryAcquire(2, TimeUnit.SECONDS), is(true));

                ArgumentCaptor<RemoteMessage> message = ArgumentCaptor.forClass(RemoteMessage.class);
                verify(m_MessageCallback, atMost(3)).handleMessage(message.capture());

                RemoteMessage remoteMessage = message.getValue();
                assertThat(remoteMessage, notNullValue());
                assertThat(remoteMessage, is((RemoteMessage)mockedMessage));

                // Unregister callback and verify it is not called when another message is received
                m_SUT.unregisterCallback(m_MessageCallback);
                createTestMessage().writeDelimitedTo(m_OutputStream);
                assertThat(callbackWaitMsg.tryAcquire(1, TimeUnit.SECONDS), is(false));
                break;
            }
            case Asset:
            case Bundle:
            case ConfigAdmin:
            case CustomComms:
            case DataStreamService:
            case DataStreamStore:
            case EncryptionInfo:
            case LinkLayer:
            case MetaType:
            case MissionProgramming:
            case ObservationStore:
            case Persistence:
            case PhysicalLink:
            case PowerManagement:
            case RemoteChannelLookup:
            case TransportLayer:
            {
                try
                {
                    m_SUT.registerCallback(type, m_MessageCallback);
                    fail("registerCallback should throw IllegalArgumentException for unsupported namespace type "
                         + type.name());
                }
                catch (IllegalArgumentException e)
                {
                    // expected
                }

                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown namespace type " + type.name());
        }
    }
}
