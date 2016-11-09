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
package mil.dod.th.ose.remote.util;

import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.ControllerInfoData;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.TerraHarvestMessageHelper;

import org.junit.Test;
import org.osgi.service.event.Event;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * @author dhumeniuk
 *
 */
public class TestRemoteInterfaceUtilities
{
    @Test
    public final void testGetRemoteEventTopic()
    {
        assertThat(RemoteInterfaceUtilities.getRemoteEventTopic("blah"), is("blah_REMOTE"));
    }
    
    /**
     * Verify the event is created with the proper topic and properties.
     */
    @Test
    public final void testCreateMessageReceivedEvent() throws InvalidProtocolBufferException
    {
        // construct a single message containing data within a namespace message
        ControllerInfoData systemInfoData = ControllerInfoData.newBuilder()
                .setName("test-name")
                .build();
        
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .setData(systemInfoData.toByteString())
                .build();
        
        TerraHarvestMessage message = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.Base, 100, baseNamespaceMessage);
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().
                setNamespace(Namespace.Base).
                setNamespaceMessage(baseNamespaceMessage.toByteString()).
                build();
        RemoteChannel channel = mock(RemoteChannel.class);
        
        Event event = RemoteInterfaceUtilities.createMessageReceivedEvent(message, payload, baseNamespaceMessage,
                BaseMessageType.ControllerInfo, systemInfoData, channel);
        
        // now test all properties and the topic
        assertThat(event.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)event.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)event.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.Base.toString()));
        assertThat((BaseNamespace)event.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(baseNamespaceMessage));
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(BaseMessageType.ControllerInfo.toString()));
        assertThat((ControllerInfoData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), 
                is(systemInfoData));
        assertThat((Boolean)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_RESPONSE), 
                is(false));
        assertThat((RemoteChannel)event.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
    }
    
    /**
     * Verify the response event property is stored properly if the message contains a specific value.
     */
    @Test
    public final void testCreateMessageReceivedEventResponse() throws InvalidProtocolBufferException
    {
        // construct a single message containing data within a namespace message
        ControllerInfoData systemInfoData = ControllerInfoData.newBuilder()
                .setName("test-name")
                .build();
        
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .setData(systemInfoData.toByteString())
                .build();
        
        TerraHarvestMessage message = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.Base, 100, baseNamespaceMessage);
        TerraHarvestMessage responseMessage = message.toBuilder().setIsResponse(true).build();
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().
                setNamespace(Namespace.Base).
                setNamespaceMessage(baseNamespaceMessage.toByteString()).
                build();
        RemoteChannel channel = mock(RemoteChannel.class);
        
        Event event = RemoteInterfaceUtilities.createMessageReceivedEvent(responseMessage, payload, 
            baseNamespaceMessage, BaseMessageType.ControllerInfo, systemInfoData, channel);
        
        // now test all properties and the topic
        assertThat((Boolean)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_RESPONSE), 
                is(true));
    }
    
    /**
     * Verify the event is created with the proper topic and properties.
     */
    @Test
    public final void testCreateMessageUnreachableSendEvent()
    {
        // construct a single message containing data within a namespace message
        ControllerInfoData systemInfoData = ControllerInfoData.newBuilder()
                .setName("test-name")
                .build();
        
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .setData(systemInfoData.toByteString())
                .build();
        
        TerraHarvestMessage message = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.Base, 100, baseNamespaceMessage);
        
        Event event = RemoteInterfaceUtilities.createMessageUnreachableSendEvent(message);
        
        // now test all properties and the topic
        assertThat(event.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_SEND_UNREACHABLE_DEST));
        assertThat((TerraHarvestMessage)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)event.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)event.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
    }
    
    /**
     * Verify the event is created with the proper topic and properties.
     */
    @Test
    public final void testCreateMessageUnreachableReceivedEvent()
    {
        // construct a single message containing data within a namespace message
        ControllerInfoData systemInfoData = ControllerInfoData.newBuilder()
                .setName("test-name")
                .build();
        
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .setData(systemInfoData.toByteString())
                .build();
        
        TerraHarvestMessage message = 
                TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.Base, 100, baseNamespaceMessage);
        
        Event event = RemoteInterfaceUtilities.createMessageUnreachableReceivedEvent(message);
        
        // now test all properties and the topic
        assertThat(event.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED_UNREACHABLE_DEST));
        assertThat((TerraHarvestMessage)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)event.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)event.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
    }
}
