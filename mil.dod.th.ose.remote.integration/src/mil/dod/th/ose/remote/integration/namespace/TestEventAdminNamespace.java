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
package mil.dod.th.ose.remote.integration.namespace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetRequestData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace.
    AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationRequestData;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationResponseData;
import mil.dod.th.core.remote.proto.EventMessages.SendEventData;
import mil.dod.th.core.remote.proto.EventMessages.UnregisterEventRequestData;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.MapTypes.ComplexTypesMapEntry;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.core.types.Version;
import mil.dod.th.ose.remote.integration.AssetNamespaceUtils;
import mil.dod.th.ose.remote.integration.MessageListener.MessageDetails;
import mil.dod.th.ose.remote.integration.RemoteEventRegistration;
import mil.dod.th.ose.remote.integration.MessageDualListener;
import mil.dod.th.ose.remote.integration.MessageListener;
import mil.dod.th.ose.remote.integration.MessageMatchers.BasicMessageMatcher;
import mil.dod.th.ose.remote.integration.MessageMatchers.EventMessageMatcher;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import mil.dod.th.ose.remote.integration.TerraHarvestMessageHelper;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.ose.test.remote.RemoteUtils;
import mil.dod.th.remote.lexicon.observation.types.ObservationGen;
import mil.dod.th.remote.lexicon.types.SharedTypesGen;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import example.asset.ExampleAsset;

/**
 * Tests the interaction of the remote interface with the {@link EventAdminNamespace}.  Specifically, 
 * the class tests that EventAdmin messages are properly sent and that appropriate responses are
 * received.    
 * @author bachmakm
 *
 */
public class TestEventAdminNamespace 
{
    /**
     * Socket to use for tests.
     */
    private Socket m_Socket;

    /**
     * Setup socket. Connect socket.
     */
    @Before
    public void setup() throws IOException
    {
        m_Socket = SocketHostHelper.connectToController();
    }
    
    /**
     * Tear down, remove socket.
     */
    @After
    public void tearDown() throws IOException, InterruptedException
    {
        try
        {
            AssetNamespaceUtils.removeAllAssets(m_Socket);
        }
        finally
        {
            m_Socket.close();
        }
    }
    
    /**
     * Test sending and receiving a response for registering and unregistering logging events.
     */
    @Test
    public void testRegisterUnregisterEvent() throws UnknownHostException, IOException, InterruptedException
    {
        //open connection to controller
        MessageListener listener = new MessageListener(m_Socket);

        //construct new message for registering an event
        EventRegistrationRequestData.Builder requestMessage = EventRegistrationRequestData.newBuilder()
                .addTopic(RemoteConstants.TOPIC_MESSAGE_RECEIVED);
        
        // add required fields
        requestMessage = RemoteUtils.appendRequiredFields(requestMessage, false);

        //create terra harvest message   
        TerraHarvestMessage message = createEventAdminMessage(EventAdminMessageType.EventRegistrationRequest, 
                requestMessage.build());

        // send out message
        message.writeDelimitedTo(m_Socket.getOutputStream());

        //listen for messages for a specific time interval
        List<MessageDetails> details = listener.waitForMessages(1000,
                new BasicMessageMatcher(Namespace.EventAdmin, 
                        EventAdminMessageType.EventRegistrationResponse), 
                        new EventMessageMatcher(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        
        EventAdminNamespace response = null;
        for (MessageDetails detail : details)
        {
            if (detail.getNamespaceMessage() instanceof EventAdminNamespace)
            {
                response = (EventAdminNamespace)detail.getNamespaceMessage();
                break;
            }
        }
        
        assertThat(response, notNullValue());
       
        //ensure EventRegistrationResponse type was received, store message data for later use
        EventRegistrationResponseData regResponse = EventRegistrationResponseData.parseFrom(response.getData());
        
        /*Create request to unregister from receiving event notifications for specific remote events.
        Registered events are assigned a unique ID.  When the user no longer wants to receive notifications
        corresponding to a certain registered event, that event's unique ID is used to remove the event 
        from the list of registered events.*/
        UnregisterEventRequestData unregisterRequestMessage = UnregisterEventRequestData.newBuilder()
                .setId(regResponse.getId()).build();
        message = createEventAdminMessage(EventAdminMessageType.UnregisterEventRequest, 
                unregisterRequestMessage);
        // send out message
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        //listen for potential messages as a result of new request
        //assert unregister event response was received
        listener.waitForMessage (Namespace.EventAdmin, EventAdminMessageType.UnregisterEventResponse, 500);        

        // Register another event
        requestMessage = EventRegistrationRequestData.newBuilder()
                .addTopic("test-topic2");
        // add required fields
        requestMessage = RemoteUtils.appendRequiredFields(requestMessage, false);
        message = createEventAdminMessage(EventAdminMessageType.EventRegistrationRequest, requestMessage.build());
        // send out message
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        //listen for new messages of a specific time for a specific time interval
        //assert event registration response was received
        listener.waitForMessage(Namespace.EventAdmin, EventAdminMessageType.EventRegistrationResponse, 500);  
        
        //verify that log events are no longer being posted with registration of a new event
        try
        {
            TerraHarvestMessage.parseDelimitedFrom(m_Socket.getInputStream());
            fail("Log event notifications still being captured.");
        }
        catch(Exception e)
        {
            assertThat(e.getMessage(), is("Read timed out"));
        }
    }

    /**
     * Test sending and receiving a response for cleaning up registered events.  
     */
    @Test
    public void testCleanup() throws UnknownHostException, IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(m_Socket);

        // Register Event
        EventRegistrationRequestData.Builder requestMessage = EventRegistrationRequestData.newBuilder()
                .addTopic(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED);
        // add required fields
        requestMessage = RemoteUtils.appendRequiredFields(requestMessage, false);
        TerraHarvestMessage message = createEventAdminMessage(EventAdminMessageType.EventRegistrationRequest, 
                requestMessage.build());
        //send out message
        message.writeDelimitedTo(m_Socket.getOutputStream());

        // check for response
        listener.waitForMessage(Namespace.EventAdmin, EventAdminMessageType.EventRegistrationResponse, 500);
        
        // don't use util method as we need to listen for the event which may be swallowed up by the helper method
        CreateAssetRequestData request = CreateAssetRequestData.newBuilder().
                setProductType(ExampleAsset.class.getName()).setName("EventCleanupAsset").build();
        AssetDirectoryServiceNamespace.Builder assetDirMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(AssetDirectoryServiceMessageType.CreateAssetRequest).
                setData(request.toByteString());
        message = TerraHarvestMessageHelper.
                createTerraHarvestMsg(Namespace.AssetDirectoryService, assetDirMessage);
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        // verify registered event is received
        listener.waitForRemoteEvent(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED, 1000);
        
        AssetNamespaceUtils.removeAllAssets(m_Socket);
        
        //create cleanup request
        message = createEventAdminMessage(EventAdminMessageType.CleanupRequest, null);
        message.writeDelimitedTo(m_Socket.getOutputStream());

        //listen for messages
        //assert cleanup response received
        listener.waitForMessage(Namespace.EventAdmin, EventAdminMessageType.CleanupResponse, 500);
        
        //Register another event
        requestMessage = EventRegistrationRequestData.newBuilder()
                .addTopic("test-topic2");
        // add required fields
        requestMessage = RemoteUtils.appendRequiredFields(requestMessage, false);
        message = createEventAdminMessage(EventAdminMessageType.EventRegistrationRequest, 
                requestMessage.build());
        //send out message
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        //listen for messages
        //assert event registration response received
        listener.waitForMessage(Namespace.EventAdmin, EventAdminMessageType.EventRegistrationResponse, 500);
        
        // verify that log events are no longer being posted as a result of "cleaning up" the
        // log event registration
        try{
            TerraHarvestMessage.parseDelimitedFrom(m_Socket.getInputStream());
            fail("Log event notifications still being captured.");
        }
        catch(Exception e)
        {
            assertThat(e.getMessage(), is("Read timed out"));
        }
    }

    /**
     * Register to listen for remote events. This verifies both the SendEvent message is handled by the controller and
     * that registered events result in a SendEvent being sent.
     * 
     * This test will:
     *  1. register for an observation persisted event with the controller
     *  2. send a SendEvent message (with observation) to the controller
     *  3. the controller will receive the SendEvent message, convert the observation to a JAXB object and post a remote
     *     observation persisted event locally
     *  4. the controller will convert the JAXB observation object back to protocol buffer format and send a SendEvent 
     *     message when a remote observation persisted event is posted locally
     *  5. the test will receive the SendEvent back
     */
    @Test
    public void testSendEvent_NativeObservation() throws IOException, InterruptedException
    {
        // 1. register for remote event
        int regId = RemoteEventRegistration.regRemoteEventMessages(m_Socket, 
                ObservationStore.TOPIC_OBSERVATION_PERSISTED_WITH_OBS + RemoteConstants.REMOTE_TOPIC_SUFFIX, 
                RemoteTypesGen.LexiconFormat.Enum.NATIVE);

        //create a basic observation
        SharedMessages.UUID assetUuid = SharedMessageUtils.convertUUIDToProtoUUID(UUID.randomUUID());
        SharedMessages.UUID obsUuid = SharedMessageUtils.convertUUIDToProtoUUID(UUID.randomUUID());

        ObservationGen.Observation observation = ObservationGen.Observation.newBuilder().
            setSystemInTestMode(true).
            setAssetUuid(assetUuid).
            setAssetName("NAME").
            setAssetType(ExampleAsset.class.getName()).
            setCreatedTimestamp(Calendar.getInstance().getTimeInMillis()).
            setSystemId(321).
            setVersion(SharedTypesGen.Version.newBuilder().setMajorNumber(1).setMinorNumber(2).build()).
            setUuid(obsUuid).build();

        // property map of the event
        ComplexTypesMapEntry props = ComplexTypesMapEntry.newBuilder().
            setKey(ObservationStore.EVENT_PROP_OBSERVATION).setObservationNative(observation).build();

        // send event message
        SendEventData sendEvent = SendEventData.newBuilder()
            .setTopic(ObservationStore.TOPIC_OBSERVATION_PERSISTED_WITH_OBS)
            .addProperty(props).build();

        TerraHarvestMessage message = createEventAdminMessage(EventAdminMessageType.SendEvent, sendEvent);

        // 2. send message
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        // 3 and 4 happen on the controller

        // 5. listener
        MessageListener listener = new MessageListener(m_Socket);
        EventAdminNamespace namespace = (EventAdminNamespace)listener.waitForRemoteEvent(
                ObservationStore.TOPIC_OBSERVATION_PERSISTED_WITH_OBS + RemoteConstants.REMOTE_TOPIC_SUFFIX, 1500);

        MessageListener.unregisterEvent(regId, m_Socket);

        SendEventData response = SendEventData.parseFrom(namespace.getData());

        //verify property for the observation
        ImmutableMap<String, ComplexTypesMapEntry> responseProps = 
                Maps.uniqueIndex(response.getPropertyList(), new Function<ComplexTypesMapEntry, String>()
                {
                    @Override
                    public String apply(ComplexTypesMapEntry entry)
                    {
                        return entry.getKey();
                    }
                });
        assertThat(responseProps.get(ObservationStore.EVENT_PROP_OBSERVATION).getObservationNative(), is(observation));
    }
    
    /**
     * Verify observation event data can be sent back as XML.
     * 
     * This test will:
     *  1. register for an observation persisted event with the controller
     *  2. send a SendEvent message (with observation) to the controller
     *  3. the controller will receive the SendEvent message, convert the observation to a JAXB object and post a remote
     *     observation persisted event locally
     *  4. the controller will convert the JAXB observation object back to XML format and send a SendEvent message when 
     *     a remote observation persisted event is posted locally
     *  5. the test will receive the SendEvent back
     */
    @Test
    public void testSendEvent_ObservationXml() throws Exception
    {
        // 1. register for remote event
        int regId = RemoteEventRegistration.regRemoteEventMessages(m_Socket, 
                ObservationStore.TOPIC_OBSERVATION_PERSISTED_WITH_OBS + RemoteConstants.REMOTE_TOPIC_SUFFIX, 
                RemoteTypesGen.LexiconFormat.Enum.XML);

        //create a basic observation
        UUID assetUuid = UUID.randomUUID();
        UUID obsUuid = UUID.randomUUID();
        long createTime = System.currentTimeMillis();

        ObservationGen.Observation observation = ObservationGen.Observation.newBuilder().
            setSystemInTestMode(true).
            setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(assetUuid)).
            setAssetName("NAME").
            setAssetType(ExampleAsset.class.getName()).
            setCreatedTimestamp(createTime).
            setSystemId(321).
            setVersion(SharedTypesGen.Version.newBuilder().setMajorNumber(1).setMinorNumber(2).build()).
            setUuid(SharedMessageUtils.convertUUIDToProtoUUID(obsUuid)).build();
        
        Observation jaxbObservation = new Observation()
            .withSystemInTestMode(true)
            .withAssetUuid(assetUuid)
            .withAssetName("NAME")
            .withAssetType(ExampleAsset.class.getName())
            .withCreatedTimestamp(createTime)
            .withSystemId(321)
            .withVersion(new Version(1, 2))
            .withUuid(obsUuid);

        // property map of the event
        ComplexTypesMapEntry props = ComplexTypesMapEntry.newBuilder().
            setKey(ObservationStore.EVENT_PROP_OBSERVATION).setObservationNative(observation).build();

        // send event message
        SendEventData sendEvent = SendEventData.newBuilder()
            .setTopic(ObservationStore.TOPIC_OBSERVATION_PERSISTED_WITH_OBS)
            .addProperty(props).build();

        TerraHarvestMessage message = createEventAdminMessage(EventAdminMessageType.SendEvent, sendEvent);

        // 2. send message
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        // 3 and 4 happen on the controller

        // 5. listener
        MessageListener listener = new MessageListener(m_Socket);
        EventAdminNamespace namespace = (EventAdminNamespace)listener.waitForRemoteEvent(
                ObservationStore.TOPIC_OBSERVATION_PERSISTED_WITH_OBS + RemoteConstants.REMOTE_TOPIC_SUFFIX, 1500);

        MessageListener.unregisterEvent(regId, m_Socket);

        SendEventData response = SendEventData.parseFrom(namespace.getData());

        //verify property for the observation
        ImmutableMap<String, ComplexTypesMapEntry> responseProps = 
                Maps.uniqueIndex(response.getPropertyList(), new Function<ComplexTypesMapEntry, String>()
                {
                    @Override
                    public String apply(ComplexTypesMapEntry entry)
                    {
                        return entry.getKey();
                    }
                });
        
        JAXBContext jc = JAXBContext.newInstance(Observation.class);
        Marshaller m = jc.createMarshaller();
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
        m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        m.marshal(jaxbObservation, byteStream);
        
        assertThat(responseProps.get(ObservationStore.EVENT_PROP_OBSERVATION).getObservationXml(), 
                is(ByteString.copyFrom(byteStream.toByteArray())));
    }

    /**
     * Test sending and receiving a response from two represented systems.
     * This test is lengthy: First the test will connect to the controller with the default connection
     * properties used for most of the integration tests. Then an asset is created, the test will register to get
     * notifications when that asset captures data. Then an additional connection is setup NOT using the default 
     * system information. Then the asset will be requested to capture data, both connections will be listening
     * to the data captured event. Once verified that both connections received the event, the first connection will 
     * request cleanup, which should remove event registrations for that connection. After confirmation of the cleanup
     * the asset will again capture data and it will be verified that the connection that requested clean up DOES not
     * receive notifications. Meanwhile the other connection will be confirmed to have still gotten the notification.
     *
     */
    @Test
    public void testTwoConnectionCleanup() throws UnknownHostException, IOException, InterruptedException
    {
        /////////////////////////
        // Setup for the test. //
        ////////////////////////

        //will hold the create assets UUID
        final SharedMessages.UUID assetUuid;
        
        //create the asset
        CreateAssetResponseData assetCreation = AssetNamespaceUtils.createAsset(m_Socket, ExampleAsset.class.getName(),
                "event_Test_Asset", null);
        
        //get the UUID of the asset
        assetUuid = assetCreation.getInfo().getUuid();
        
        //used to register for events
        MessageListener listener = new MessageListener(m_Socket);
        
        //will only register to the default system credentials
        RemoteEventRegistration.regRemoteEventMessages(m_Socket, Asset.TOPIC_DATA_CAPTURED, 
            String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_NAME, "event_Test_Asset"));
        
        //create additional connection
        try (Socket additionalSystemSock = SocketHostHelper.connectToController())
        {
            //will only register for the additional system
            RemoteEventRegistration.regRemoteEventMessagesAlternavtiveSystem(
                additionalSystemSock, new String[]{Asset.TOPIC_DATA_CAPTURED}, 
                    String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_NAME, "event_Test_Asset"));
    
            ///////////////////////////////////
            // Actual testing of the system. //
            //////////////////////////////////
            
            //listen for messages in response to the asset capturing data
            MessageDualListener dualListener = new MessageDualListener(m_Socket, additionalSystemSock);
            
            //ask the asset to capture data
            AssetNamespaceUtils.requestDataCapture(m_Socket, assetUuid);
            
            dualListener.waitForMessagesTwoSockets(9000);
            
            //verify messages were received on both channels
            dualListener.assertMessagesReceivedByTypeSocketOne(Namespace.EventAdmin, EventAdminMessageType.SendEvent);
            dualListener.assertMessagesReceivedByTypeSocketTwo(Namespace.EventAdmin, EventAdminMessageType.SendEvent);
    
            //Clean up request on original channel
            TerraHarvestMessage message = createEventAdminMessage(EventAdminMessageType.CleanupRequest, null);
            
            //send out the request
            message.writeDelimitedTo(m_Socket.getOutputStream());
            
            //listen for response message 
            listener.waitForMessage(Namespace.EventAdmin, EventAdminMessageType.CleanupResponse, 1000);
            
            //ask the asset to capture data again
            AssetNamespaceUtils.requestDataCapture(m_Socket, assetUuid);
    
            //listen for messages in response to the asset capturing data
            dualListener.waitForMessages(2000);
            
            //verify a message was only received across the channel for the additional system
            dualListener.assertMessagesReceivedByTypeSocketTwo(Namespace.EventAdmin, EventAdminMessageType.SendEvent);
            //assertion should fail
            try
            {
                dualListener.assertMessagesReceivedByTypeSocketOne(Namespace.EventAdmin, 
                        EventAdminMessageType.SendEvent);
                fail("Expected to not get a send event because cleanup was previously requested.");
            }
            catch (AssertionError e)
            {
                //expected exception because there should NOT be messages of this type received
            }
        }
        finally
        {
            //clean up
            AssetNamespaceUtils.removeAsset(m_Socket, assetUuid);
        }
    }
    
    /**
     * Verify that duplicate event registrations are not created.
     */
    @Test
    public void testDuplicateEventRegs() throws IOException
    {
        MessageListener listener = new MessageListener(m_Socket);

        // Register event
        EventRegistrationRequestData.Builder requestMessage = EventRegistrationRequestData.newBuilder()
                .addTopic("test-topic1");
        // add required fields
        requestMessage = RemoteUtils.appendRequiredFields(requestMessage, false);
        TerraHarvestMessage message = createEventAdminMessage(EventAdminMessageType.EventRegistrationRequest, 
                requestMessage.build());
        // send out message
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        // listen for messages for a specific time interval
        List<MessageDetails> details = listener.waitForMessages(1000,
                new BasicMessageMatcher(Namespace.EventAdmin, 
                        EventAdminMessageType.EventRegistrationResponse));
        
        EventAdminNamespace response = null;
        for (MessageDetails detail : details)
        {
            if (detail.getNamespaceMessage() instanceof EventAdminNamespace)
            {
                response = (EventAdminNamespace)detail.getNamespaceMessage();
                break;
            }
        }
        
        assertThat(response, notNullValue());
        
        EventRegistrationResponseData regResponse = EventRegistrationResponseData.parseFrom(response.getData());
        int origRegId = regResponse.getId();

        // Register event again
        requestMessage = EventRegistrationRequestData.newBuilder().addTopic("test-topic1");
        // add required fields
        requestMessage = RemoteUtils.appendRequiredFields(requestMessage, false);
        message = createEventAdminMessage(EventAdminMessageType.EventRegistrationRequest, requestMessage.build());
        // send out message
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        // listen for messages for a specific time interval
        details = listener.waitForMessages(1000,
                new BasicMessageMatcher(Namespace.EventAdmin, 
                        EventAdminMessageType.EventRegistrationResponse));
        
        response = null;
        for (MessageDetails detail : details)
        {
            if (detail.getNamespaceMessage() instanceof EventAdminNamespace)
            {
                response = (EventAdminNamespace)detail.getNamespaceMessage();
                break;
            }
        }
        
        assertThat(response, notNullValue());
        
        regResponse = EventRegistrationResponseData.parseFrom(response.getData());
        assertThat(regResponse.getId(), equalTo(origRegId));
    }

    /**
     * Helper method for creating EventAdmin messages to be sent to controller. 
     * @param type
     *      type of message to be contained in the sent TerraHarvestMessage
     * @param message
     *      message data to be contained in the sent TerraHarvestMessage
     * @return
     *      TerraHarvestMessage to be sent to the controller
     */
    public static TerraHarvestMessage createEventAdminMessage(final EventAdminMessageType type, 
            final Message message)
    {
        EventAdminNamespace.Builder eventMessageBuilder = null;
        switch(type)
        {
            case EventRegistrationRequest:
            {               
                eventMessageBuilder = EventAdminNamespace.newBuilder().
                        setType(type).setData(message.toByteString());
                break;
            }
            case UnregisterEventRequest:
            {                
                eventMessageBuilder = EventAdminNamespace.newBuilder().
                        setType(type).setData(message.toByteString());
                break;
            }
            case CleanupRequest:
            {                
                eventMessageBuilder = EventAdminNamespace.newBuilder().
                        setType(EventAdminMessageType.CleanupRequest);
                break;
            }
            case SendEvent:
            {
                eventMessageBuilder = EventAdminNamespace.newBuilder().
                        setType(type).
                        setData(message.toByteString());
                break;
            }
            default:
                break;
        }

        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.
                createTerraHarvestMsg(Namespace.EventAdmin, eventMessageBuilder);
        return thMessage;
    }
}
