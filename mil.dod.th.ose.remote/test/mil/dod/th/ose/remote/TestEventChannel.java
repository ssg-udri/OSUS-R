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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;

import java.util.HashMap;

import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetRequestData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace
    .AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * 
 * 
 * @author cweisenborn
 */
public class TestEventChannel
{
    private EventChannel m_SUT;
    private int m_RemoteSystemId = 100;
    private int m_SourceId = 55;
    private TerraHarvestMessage m_ThMessage;
    
    @Mock private EventAdmin m_EventAdmin;
    
    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        
        m_ThMessage = createThMessage();
        
        m_SUT = new EventChannel(m_RemoteSystemId, m_EventAdmin);
    }

    /**
     * Verify that the appropriate remote system ID is returned.
     */
    @Test
    public void testGetRemoteSystemId()
    {
        assertThat(m_SUT.getRemoteSystemId(), equalTo(m_RemoteSystemId));
    }
    
    /**
     * Verify that try send message posts the appropriate event with proper values.
     */
    @Test
    public void testTrySendMessage()
    {
        assertThat(m_SUT.trySendMessage(m_ThMessage), equalTo(true));
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event unreachableEvent = eventCaptor.getValue();
        assertThat(unreachableEvent.getTopic(), equalTo(RemoteConstants.TOPIC_MESSAGE_SEND_UNREACHABLE_DEST));
        assertThat(unreachableEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), equalTo(m_RemoteSystemId));
        assertThat(unreachableEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), equalTo(m_SourceId));
        assertThat(unreachableEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), equalTo(m_ThMessage));
    }
    
    /**
     * Verify that queue message posts the appropriate event with proper values.
     */
    @Test
    public void testQueueMessage()
    {
        assertThat(m_SUT.queueMessage(m_ThMessage), equalTo(true));
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event unreachableEvent = eventCaptor.getValue();
        assertThat(unreachableEvent.getTopic(), equalTo(RemoteConstants.TOPIC_MESSAGE_SEND_UNREACHABLE_DEST));
        assertThat(unreachableEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), equalTo(m_RemoteSystemId));
        assertThat(unreachableEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), equalTo(m_SourceId));
        assertThat(unreachableEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), equalTo(m_ThMessage));
    }
    
    /**
     * Verify that matches returns false.
     */
    @Test
    public void testMatches()
    {
        assertThat(m_SUT.matches(new HashMap<String, Object>()), equalTo(false));
    }
    
    /**
     * Verify status returned is null.
     */
    @Test
    public void testGetStatus()
    {
        assertThat(m_SUT.getStatus(), nullValue());
    }
    
    /**
     * Verify channel type is null.
     */
    @Test
    public void testChannelType()
    {
        assertThat(m_SUT.getChannelType(), nullValue());
    }
    
    /**
     * Verify queued message count is 0.
     */
    @Test
    public void testGetQueuedMessageCount()
    {
        assertThat(m_SUT.getQueuedMessageCount(), equalTo(0));
    }

    /**
     * Verify bytes transmitted is 0.
     */
    @Test
    public void testGetBytesTransmitted()
    {
        assertThat(m_SUT.getBytesTransmitted(), equalTo(0L));
    }

    /**
     * Verify bytes received is 0.
     */
    @Test
    public void testGetBytesReceived()
    {
        assertThat(m_SUT.getBytesReceived(), equalTo(0L));
    }

    /**
     * Verify that clear queued messages method doesn't throw any errors. Method itself should do nothing.
     */
    @Test
    public void testClearQueuedMessages()
    {
        try
        {
            m_SUT.clearQueuedMessages();
        }
        catch (final Exception ex)
        {
            fail("No exception was expected.");
        }
    }
    
    /**
     * Creates a Terra Harvest message
     */
    private TerraHarvestMessage createThMessage()
    {
        final CreateAssetRequestData createAsset = CreateAssetRequestData.newBuilder().setProductType("test")
                .setName("bob").build();
        final AssetDirectoryServiceNamespace namespace = AssetDirectoryServiceNamespace.newBuilder().setType(
                AssetDirectoryServiceMessageType.CreateAssetRequest).setData(createAsset.toByteString()).build();
        final TerraHarvestPayload payload = TerraHarvestPayload.newBuilder()
                .setNamespace(Namespace.AssetDirectoryService)
                .setNamespaceMessage(namespace.toByteString()).build();
        final TerraHarvestMessage thMessage = TerraHarvestMessage.newBuilder().setDestId(m_RemoteSystemId)
                .setSourceId(m_SourceId).setMessageId(500).setTerraHarvestPayload(payload.toByteString())
                .setVersion(RemoteConstants.SPEC_VERSION).build();
        return thMessage;
    }
}
