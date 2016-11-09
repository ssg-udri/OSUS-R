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
package mil.dod.th.ose.remote.integration.base;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.UUID;

import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace.ConfigAdminMessageType;
import mil.dod.th.core.remote.proto.ConfigMessages.SetPropertyRequestData;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.EventMessages.SendEventData;
import mil.dod.th.core.remote.proto.MapTypes.ComplexTypesMapEntry;
import mil.dod.th.core.remote.proto.MapTypes.SimpleTypesMapEntry;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.FindObservationByUUIDRequestData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.FindObservationByUUIDResponseData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.GetObservationResponseData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.ObservationStoreNamespace;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.ObservationStoreNamespace.ObservationStoreMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype.Type;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.ose.remote.integration.MessageListener;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import mil.dod.th.ose.remote.integration.namespace.TestConfigAdminNamespace;
import mil.dod.th.ose.remote.integration.namespace.TestEventAdminNamespace;
import mil.dod.th.ose.remote.integration.namespace.TestObservationStoreMessageService;
import mil.dod.th.remote.lexicon.observation.types.ObservationGen;
import mil.dod.th.remote.lexicon.types.SharedTypesGen.Version;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.Message;

/**
 * Test class for the remote observation listener.
 * @author callen
 *
 */
public class TestRemoteObservationListener 
{
    private static Socket socket;
    
    /**
     * Make sure that the remote observation store listener is enabled.
     */
    @Before
    public void setup() throws UnknownHostException, IOException, InterruptedException
    {
        socket = SocketHostHelper.connectToController();
        enableRemoteObservationStore(true);
    }

    /**
     * Closes the socket. 
     */
    @After
    public void teardown() throws UnknownHostException, IOException
    {       
        socket.close();
    }

    /**
     * Test that if a remote observation message is remotely received that the observation is persisted on the 
     * controller.
     */
    @Test
    public void testRemoteObservation() throws IOException, InterruptedException
    {
        //observation message
        ObservationGen.Observation obs = createProtoObservation();

        //get observation responses
        GetObservationResponseData response = GetObservationResponseData.newBuilder().
            addObservationNative(obs).build();
        TerraHarvestMessage message = 
            TestObservationStoreMessageService.createObservationStoreMessage(
                ObservationStoreMessageType.GetObservationResponse, response);

        //send observation
        message.writeDelimitedTo(socket.getOutputStream());

        //wait for observation to be loaded
        Thread.sleep(1000);

        //check observation was added
        FindObservationByUUIDRequestData request = FindObservationByUUIDRequestData.newBuilder().
            addUuidOfObservation(obs.getUuid()).build();
        message = TestObservationStoreMessageService.createObservationStoreMessage(
            ObservationStoreMessageType.FindObservationByUUIDRequest, request);

        //message listener to get the find request message
        MessageListener listener = new MessageListener(socket);

        //send request to get observations
        message.writeDelimitedTo(socket.getOutputStream());

        Message responseRcvd = listener.waitForMessage(Namespace.ObservationStore, 
            ObservationStoreMessageType.FindObservationByUUIDResponse, 1000);

        //parse response
        ObservationStoreNamespace namespace = (ObservationStoreNamespace) responseRcvd;
        FindObservationByUUIDResponseData responseData = 
            FindObservationByUUIDResponseData.parseFrom(namespace.getData());

        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeCount(), is(1));
        assertThat(responseData.getObservationNativeList().get(0), is(obs));
    }

    /**
     * Test that if a remote event observation message is received that the observation is persisted locally 
     * on the controller.
     */
    @Test
    public void testRemoteObservationEvent() throws IOException, InterruptedException
    {
        enableRemoteObservationStore(true);

        //observation message
        ObservationGen.Observation obs = createProtoObservation();

        //Send event with observation
        ComplexTypesMapEntry entry = ComplexTypesMapEntry.newBuilder().
            setKey(ObservationStore.EVENT_PROP_OBSERVATION).
            setObservationNative(obs).build();
        SendEventData response = SendEventData.newBuilder().
            setTopic(ObservationStore.TOPIC_OBSERVATION_PERSISTED_WITH_OBS).
            addProperty(entry).build();
        TerraHarvestMessage message = 
            TestEventAdminNamespace.createEventAdminMessage(EventAdminMessageType.SendEvent, response);

        //send observation
        message.writeDelimitedTo(socket.getOutputStream());

        //wait for observation to be loaded
        Thread.sleep(1000);

        //check observation was added
        FindObservationByUUIDRequestData request = FindObservationByUUIDRequestData.newBuilder().
            addUuidOfObservation(obs.getUuid()).build();
        message = TestObservationStoreMessageService.createObservationStoreMessage(
            ObservationStoreMessageType.FindObservationByUUIDRequest, request);

        //message listener to get the find request message
        MessageListener listener = new MessageListener(socket);

        //send request to get observations
        message.writeDelimitedTo(socket.getOutputStream());

        Message responseRcvd = listener.waitForMessage(Namespace.ObservationStore, 
            ObservationStoreMessageType.FindObservationByUUIDResponse, 800);

        //parse response
        ObservationStoreNamespace namespace = (ObservationStoreNamespace) responseRcvd;
        FindObservationByUUIDResponseData responseData = 
            FindObservationByUUIDResponseData.parseFrom(namespace.getData());

        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeCount(), is(1));
        assertThat(responseData.getObservationNativeList().get(0), is(obs));
    }

    /**
     * Test that the listener can be disabled and that then observations are not added when remote observation
     * messages are received.
     */
    @Test
    public void testRemoteObservationListenerDisabled() throws IOException, InterruptedException
    {
        //disable the listener
        enableRemoteObservationStore(false);

        Thread.sleep(1000); //allow time for the component to unregister

        //get an observation 
        ObservationGen.Observation obs = createProtoObservation();
        
        //get observation responses
        GetObservationResponseData response = GetObservationResponseData.newBuilder().
            addObservationNative(obs).build();
        TerraHarvestMessage message = 
            TestObservationStoreMessageService.createObservationStoreMessage(
                ObservationStoreMessageType.GetObservationResponse, response);

        //send observation
        message.writeDelimitedTo(socket.getOutputStream());

        //wait for observation to be loaded
        Thread.sleep(1000);

        //check observation was added
        FindObservationByUUIDRequestData request = FindObservationByUUIDRequestData.newBuilder().
            addUuidOfObservation(obs.getUuid()).build();
        message = TestObservationStoreMessageService.createObservationStoreMessage(
            ObservationStoreMessageType.FindObservationByUUIDRequest, request);

        //message listener to get the find request message
        MessageListener listener = new MessageListener(socket);

        //send request to get observations
        message.writeDelimitedTo(socket.getOutputStream());

        Message responseRcvd = listener.waitForMessage(Namespace.ObservationStore, 
            ObservationStoreMessageType.FindObservationByUUIDResponse, 800);

        //parse response
        ObservationStoreNamespace namespace = (ObservationStoreNamespace) responseRcvd;
        FindObservationByUUIDResponseData responseData = 
            FindObservationByUUIDResponseData.parseFrom(namespace.getData());

        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeCount(), is(0));
    }

    /**
    * Create an new proto observation.
    */
    private ObservationGen.Observation createProtoObservation()
    {
        //uuid to use for the observation
        UUID obsUuid = UUID.randomUUID();
        SharedMessages.UUID obsUuidMsg = SharedMessages.UUID.newBuilder().
            setLeastSignificantBits(obsUuid.getLeastSignificantBits()).
            setMostSignificantBits(obsUuid.getMostSignificantBits()).build();
        UUID assetUuid = UUID.randomUUID();
        SharedMessages.UUID assetUuidMsg = SharedMessages.UUID.newBuilder().
            setLeastSignificantBits(assetUuid.getLeastSignificantBits()).
            setMostSignificantBits(assetUuid.getMostSignificantBits()).build();

        //observation message
        return ObservationGen.Observation.newBuilder().
            setAssetUuid(assetUuidMsg).
            setAssetName("I am an Asset").
            setAssetType("those.super.AmazingAsset").
            setCreatedTimestamp(Calendar.getInstance().getTimeInMillis()).
            setSystemInTestMode(true).
            setSystemId(321).
            setVersion(Version.newBuilder().setMajorNumber(1).setMinorNumber(2).build()).
            setUuid(obsUuidMsg).build();
    }

    /**
     * Enable remote observation store listener.
     */
    public static void enableRemoteObservationStore(final boolean enabled) throws IOException, InterruptedException
    {
        Multitype value = Multitype.newBuilder().setBoolValue(enabled).setType(Type.BOOL).build();
        SimpleTypesMapEntry prop = SimpleTypesMapEntry.newBuilder().setKey("enabled").setValue(value).build();
        SetPropertyRequestData request = SetPropertyRequestData.newBuilder().
            setPid("mil.dod.th.ose.remote.observation.RemoteObservationListener").addProperties(prop).build();
        TerraHarvestMessage message = TestConfigAdminNamespace.createConfigAdminMessage(
            ConfigAdminMessageType.SetPropertyRequest, request);

        message.writeDelimitedTo(socket.getOutputStream());
        
        MessageListener listener = new MessageListener(socket);
        listener.waitForMessage(Namespace.ConfigAdmin, ConfigAdminMessageType.SetPropertyResponse, 1000);
    }
}
