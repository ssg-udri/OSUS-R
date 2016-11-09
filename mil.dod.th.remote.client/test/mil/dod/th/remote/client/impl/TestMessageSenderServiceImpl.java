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

import static org.mockito.Mockito.*;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;

import com.google.protobuf.ByteString;

import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.remote.client.ChannelStateCallback;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class TestMessageSenderServiceImpl
{
    private static int CLIENT_ID = 0;
    private static int DEST_ID = 1;

    private MessageSenderServiceImpl m_SUT;

    @Mock
    private OutputStream m_OutStream;
    @Mock
    private ChannelStateCallback m_ChannelCallback;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        m_SUT = new MessageSenderServiceImpl();
    }

    /**
     * Verify that the client ID must be set before sending messages.
     */
    @Test
    public void testSetClientId() throws IOException
    {
        m_SUT.addRemoteChannel(DEST_ID, m_OutStream, m_ChannelCallback);

        TerraHarvestPayload payload = createPayload();
        try
        {
            m_SUT.sendRequest(DEST_ID, payload);
            fail("sendRequest should throw IllegalStateException");
        }
        catch (IllegalStateException e)
        {
            // Expected
        }

        try
        {
            m_SUT.sendResponse(DEST_ID, payload);
            fail("sendResponse should throw IllegalStateException");
        }
        catch (IllegalStateException e)
        {
            // Expected
        }

        m_SUT.setClientId(CLIENT_ID);

        m_SUT.sendRequest(DEST_ID, payload);
        m_SUT.sendResponse(DEST_ID, payload);
    }

    /**
     * Verify that a remote channel must be added before sending messages.
     */
    @Test
    public void testAddRemoteChannel() throws IOException
    {
        m_SUT.setClientId(CLIENT_ID);

        TerraHarvestPayload payload = createPayload();
        try
        {
            m_SUT.sendRequest(DEST_ID, payload);
            fail("sendRequest should throw IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }

        try
        {
            m_SUT.sendResponse(DEST_ID, payload);
            fail("sendResponse should throw IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }

        m_SUT.addRemoteChannel(DEST_ID, m_OutStream, m_ChannelCallback);

        m_SUT.sendRequest(DEST_ID, payload);
        m_SUT.sendResponse(DEST_ID, payload);
    }

    /**
     * Verify that a remote channel can be added and removed with appropriate callbacks made.
     */
    @Test
    public void testAddRemoveRemoteChannel()
    {
        m_SUT.addRemoteChannel(DEST_ID, m_OutStream, m_ChannelCallback);

        m_SUT.removeRemoteChannel(DEST_ID);
        verify(m_ChannelCallback).onChannelRemoved(DEST_ID);

        // Should not be able to remove channel twice
        try
        {
            m_SUT.removeRemoteChannel(DEST_ID);
            fail("removeRemoteChannel should throw IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // Expected;
        }
    }

    /**
     * Verify that remote channel cannot be added if one already exists for the given destination ID.
     */
    @Test
    public void testAddDuplicateRemoteChannel()
    {
        m_SUT.addRemoteChannel(DEST_ID, m_OutStream, m_ChannelCallback);

        try
        {
            m_SUT.addRemoteChannel(DEST_ID, m_OutStream, m_ChannelCallback);
            fail("addRemoteChannel should throw IllegalStateException");
        }
        catch (IllegalStateException e)
        {
            // Expected
        }
    }

    /**
     * Verify that remote channel cannot be added with a null output stream.
     */
    @Test
    public void testAddRemoteChannelInvalidStream()
    {
        try
        {
            m_SUT.addRemoteChannel(DEST_ID, null, m_ChannelCallback);
            fail("addRemoteChannel should throw IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
    }

    /**
     * Verify that the channel callback can be null when adding and removing a channel.
     */
    @Test
    public void testAddRemoteChannelNullCallback()
    {
        m_SUT.addRemoteChannel(DEST_ID, m_OutStream, null);
        m_SUT.removeRemoteChannel(DEST_ID);
    }

    /**
     * Verify that sending a request writes the message to the appropriate output stream.
     */
    @Test
    public void testSendRequest() throws IOException
    {
        // Setup
        m_SUT.setClientId(CLIENT_ID);
        m_SUT.addRemoteChannel(DEST_ID, m_OutStream, m_ChannelCallback);
        TerraHarvestPayload payload = createPayload();

        // Replay
        m_SUT.sendRequest(DEST_ID, payload);

        // Verify
        verify(m_OutStream).write(Mockito.any(byte[].class), anyInt(), anyInt());
    }

    /**
     * Verify that sending a request message on a bad output stream is handled and the channel is removed.
     */
    @Test
    public void testSendRequestError() throws IOException
    {
        // Setup
        m_SUT.setClientId(CLIENT_ID);
        m_SUT.addRemoteChannel(DEST_ID, m_OutStream, m_ChannelCallback);
        TerraHarvestPayload payload = createPayload();

        // Mock
        doThrow(IOException.class).when(m_OutStream).write(Mockito.any(byte[].class), anyInt(), anyInt());

        // Replay
        try
        {
            m_SUT.sendRequest(DEST_ID, payload);
            fail("sendRequest should throw IOException");
        }
        catch (IOException e)
        {
            // Expected
        }

        // Verify
        verify(m_ChannelCallback).onChannelRemoved(eq(DEST_ID), Mockito.any(IOException.class));
    }

    /**
     * Verify that sending a request message on a bad output stream can handle a null callback.
     */
    @Test
    public void testSendRequestErrorNullCallback() throws IOException
    {
        // Setup
        m_SUT.setClientId(CLIENT_ID);
        m_SUT.addRemoteChannel(DEST_ID, m_OutStream, null);
        TerraHarvestPayload payload = createPayload();

        // Mock
        doThrow(IOException.class).when(m_OutStream).write(Mockito.any(byte[].class), anyInt(), anyInt());

        // Replay
        try
        {
            m_SUT.sendRequest(DEST_ID, payload);
            fail("sendRequest should throw IOException");
        }
        catch (IOException e)
        {
            // Expected
        }
    }

    /**
     * Verify that sending a response writes the message to the appropriate output stream.
     */
    @Test
    public void testSendResponse() throws IOException
    {
        // Setup
        m_SUT.setClientId(CLIENT_ID);
        m_SUT.addRemoteChannel(DEST_ID, m_OutStream, m_ChannelCallback);
        TerraHarvestPayload payload = createPayload();

        // Replay
        m_SUT.sendResponse(DEST_ID, payload);

        // Verify
        verify(m_OutStream).write(Mockito.any(byte[].class), anyInt(), anyInt());
    }

    /**
     * Verify that sending a response message on a bad output stream is handled and the channel is removed.
     */
    @Test
    public void testSendResponseError() throws IOException
    {
        // Setup
        m_SUT.setClientId(CLIENT_ID);
        m_SUT.addRemoteChannel(DEST_ID, m_OutStream, m_ChannelCallback);
        TerraHarvestPayload payload = createPayload();

        // Mock
        doThrow(IOException.class).when(m_OutStream).write(Mockito.any(byte[].class), anyInt(), anyInt());

        // Replay
        try
        {
            m_SUT.sendResponse(DEST_ID, payload);
            fail("sendResponse should throw IOException");
        }
        catch (IOException e)
        {
            // Expected
        }

        // Verify
        verify(m_ChannelCallback).onChannelRemoved(eq(DEST_ID), Mockito.any(IOException.class));
    }

    /**
     * Verify that sending a response message on a bad output stream can handle a null callback.
     */
    @Test
    public void testSendResponseErrorNullCallback() throws IOException
    {
        // Setup
        m_SUT.setClientId(CLIENT_ID);
        m_SUT.addRemoteChannel(DEST_ID, m_OutStream, null);
        TerraHarvestPayload payload = createPayload();

        // Mock
        doThrow(IOException.class).when(m_OutStream).write(Mockito.any(byte[].class), anyInt(), anyInt());

        // Replay
        try
        {
            m_SUT.sendResponse(DEST_ID, payload);
            fail("sendResponse should throw IOException");
        }
        catch (IOException e)
        {
            // Expected
        }
    }

    private TerraHarvestPayload createPayload()
    {
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder()
            .setNamespace(Namespace.Base)
            .setNamespaceMessage(ByteString.EMPTY)
            .build();

        return payload;
    }
}
