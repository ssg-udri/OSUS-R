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

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Calendar;

import mil.dod.th.core.remote.proto.ObservationStoreMessages.FindObservationByUUIDRequestData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.FindObservationByUUIDResponseData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.GetObservationCountRequestData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.GetObservationCountResponseData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.GetObservationRequestData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.GetObservationResponseData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.ObservationStoreNamespace;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.ObservationStoreNamespace.ObservationStoreMessageType;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.Query;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.RemoveObservationByUUIDRequestData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.RemoveObservationRequestData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.TimeConstraintData;
import mil.dod.th.core.remote.proto.SharedMessages.FactoryObjectInfo;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.types.observation.ObservationSubTypeEnum;
import mil.dod.th.ose.remote.integration.AssetNamespaceUtils;
import mil.dod.th.ose.remote.integration.MessageListener;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import mil.dod.th.ose.remote.integration.TerraHarvestMessageHelper;
import mil.dod.th.remote.converter.ObservationSubTypeEnumConverter;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.Message;

import example.asset.lexicon.ExampleObservationsAsset;

/**
 * @author callen
 *
 */
public class TestObservationStoreMessageService 
{
    private static final String OBS_ASSET_NAME = "obsNamespaceAsset";
    private static final String ASSET_TYPE = ExampleObservationsAsset.class.getName();
    private static final int WAIT_TIME_MS = 2000;
    private SharedMessages.UUID assetUuid;
    private Socket socket;

     /**
     * Create an example asset to be used for creating observations. 
     */
    @Before
    public void setup() throws UnknownHostException, IOException, InterruptedException
    {
        socket = SocketHostHelper.connectToController();
        assetUuid = AssetNamespaceUtils.createAsset(socket, ExampleObservationsAsset.class.getName(), OBS_ASSET_NAME,
                null).getInfo().getUuid();
    }

    /**
     * Removes the example asset used for creating observations. 
     */
    @After
    public void teardown() throws UnknownHostException, IOException, InterruptedException
    {
        try
        {
            if (assetUuid == null)
            {
                // failed to create from before, probably still there from before
                FactoryObjectInfo info = AssetNamespaceUtils.tryGetAssetInfoByName(socket, OBS_ASSET_NAME);
                if (info != null)
                {
                    // asset still here from before
                    assetUuid = info.getUuid();
                    AssetNamespaceUtils.removeAsset(socket, assetUuid);
                }
            }
            else
            {
                AssetNamespaceUtils.removeAsset(socket, assetUuid);
            }
        }
        finally
        {
            socket.close();
        }
    }

    /**
     * Test getting observations from a remote system via asset ID. 
     * 
     * Verify observations are returned.
     */
    @Test
    public void testGetObservationAssetUuid() throws IOException, InterruptedException
    {
        //request that the created asset captures data
        SharedMessages.UUID observationUuid = AssetNamespaceUtils.requestDataCaptureReturnObsUuid(socket, assetUuid);

        //message listener that will wait for responses
        MessageListener listener = new MessageListener(socket);

        //create get observation request
        Query query = Query.newBuilder()
                .setAssetUuid(assetUuid)
                .addObservationSubType(ObservationSubTypeEnumConverter
                        .convertJavaEnumToProto(ObservationSubTypeEnum.DETECTION))
                .build();
        GetObservationRequestData obsResquest = GetObservationRequestData.newBuilder().
            setObsQuery(query).build();

        //send request
        TerraHarvestMessage message = createObservationStoreMessage(ObservationStoreMessageType.GetObservationRequest, 
            obsResquest);

        //send request
        message.writeDelimitedTo(socket.getOutputStream());

        //wait for response
        Message response = listener.waitForMessage(Namespace.ObservationStore, 
            ObservationStoreMessageType.GetObservationResponse, WAIT_TIME_MS);

        //check observation is the observation expected
        ObservationStoreNamespace obsNamespaceResponse = (ObservationStoreNamespace) response;
        GetObservationResponseData getObsResponse = GetObservationResponseData.parseFrom(
            obsNamespaceResponse.getData());

        assertThat(getObsResponse.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(getObsResponse.getObservationNativeCount(), is(1));
        assertThat(getObsResponse.getObservationNativeList().get(0).getAssetUuid().getLeastSignificantBits(), 
            is(assetUuid.getLeastSignificantBits()));
        assertThat(getObsResponse.getObservationNativeList().get(0).getAssetUuid().getMostSignificantBits(), 
                is(assetUuid.getMostSignificantBits()));
        assertThat(getObsResponse.getObservationNativeList().get(0).getUuid(), is(observationUuid));
    }

    /**
     * Test getting observations from a remote system via AssetType. 
     * 
     * Verify observations are returned.
     */
    @Test
    public void testGetObservationAssetType() throws IOException, InterruptedException
    {
        //create observation
        AssetNamespaceUtils.requestDataCapture(socket, assetUuid);

        //message listener that will wait for responses
        MessageListener listener = new MessageListener(socket);

        //create get observation request
        Query query = Query.newBuilder().setAssetType(ASSET_TYPE).build();
        GetObservationRequestData obsResquest = GetObservationRequestData.newBuilder().
            setObsQuery(query).build();

        //send request
        TerraHarvestMessage message = createObservationStoreMessage(ObservationStoreMessageType.GetObservationRequest,
            obsResquest);

        //send request
        message.writeDelimitedTo(socket.getOutputStream());

        //wait for response
        Message response = listener.waitForMessage(Namespace.ObservationStore, 
            ObservationStoreMessageType.GetObservationResponse, WAIT_TIME_MS);

        //check observation is the observation expected
        ObservationStoreNamespace obsNamespaceResponse = (ObservationStoreNamespace) response;
        GetObservationResponseData getObsResponse = GetObservationResponseData.parseFrom(
            obsNamespaceResponse.getData());

        //greater than 1 because if this is running on a clean controller or if before other
        //tests the number of observations in the observation store for the asset type can be different.
        assertThat(getObsResponse.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(getObsResponse.getObservationNativeCount(), greaterThan(0));
        assertThat(getObsResponse.getObservationNativeList().get(0).getAssetType(), is(ASSET_TYPE));
    }

    /**
     * Test getting observations from a remote system via ObservationSubType. 
     * 
     * Verify observations are returned.
     */
    @Test
    public void testGetObservationObservationSubType() throws IOException, InterruptedException
    {
        //message listener that will wait for responses
        MessageListener listener = new MessageListener(socket);

        //request the asset to capture data
        AssetNamespaceUtils.requestDataCapture(socket, assetUuid);

        //create get observation request
        Query query = Query.newBuilder().
            addObservationSubType(ObservationSubTypeEnumConverter
                    .convertJavaEnumToProto(ObservationSubTypeEnum.DETECTION)).build();

        GetObservationRequestData obsResquest = GetObservationRequestData.newBuilder().
            setObsQuery(query).build();

        //send request
        TerraHarvestMessage message = createObservationStoreMessage(ObservationStoreMessageType.GetObservationRequest,
            obsResquest);

        //send request
        message.writeDelimitedTo(socket.getOutputStream());

        //wait for response
        Message response = listener.waitForMessage(Namespace.ObservationStore, 
            ObservationStoreMessageType.GetObservationResponse, WAIT_TIME_MS);

        //check observation is the observation expected
        ObservationStoreNamespace obsNamespaceResponse = (ObservationStoreNamespace) response;
        GetObservationResponseData getObsResponse = GetObservationResponseData.parseFrom(
            obsNamespaceResponse.getData());

        assertThat(getObsResponse.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(getObsResponse.getObservationNativeCount(), greaterThan(0));
        assertThat(getObsResponse.getObservationNativeList().get(0).getAssetType(), is(ASSET_TYPE));
    }

    /**
     * Test finding observations from a remote system via UUID. 
     * 
     * Verify an observation is returned.
     */
    @Test
    public void testFindObservation() throws IOException, InterruptedException
    {
        //message listener that will wait for responses
        MessageListener listener = new MessageListener(socket);

        SharedMessages.UUID observationUuid1 = AssetNamespaceUtils.requestDataCaptureReturnObsUuid(socket, assetUuid);
        SharedMessages.UUID observationUuid2 = AssetNamespaceUtils.requestDataCaptureReturnObsUuid(socket, assetUuid);

        //create get observation request
        FindObservationByUUIDRequestData obsResquest = FindObservationByUUIDRequestData.newBuilder()
            .addUuidOfObservation(observationUuid1)
            .addUuidOfObservation(observationUuid2).build();

        //send request
        TerraHarvestMessage message = createObservationStoreMessage(
            ObservationStoreMessageType.FindObservationByUUIDRequest, obsResquest);

        //send request
        message.writeDelimitedTo(socket.getOutputStream());

        //wait for response
        Message response = listener.waitForMessage(Namespace.ObservationStore, 
            ObservationStoreMessageType.FindObservationByUUIDResponse, WAIT_TIME_MS);

        //check observation is the observation expected
        ObservationStoreNamespace obsNamespaceResponse = (ObservationStoreNamespace) response;
        FindObservationByUUIDResponseData getObsResponse = FindObservationByUUIDResponseData.parseFrom(
            obsNamespaceResponse.getData());

        assertThat(getObsResponse.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(getObsResponse.getObservationNativeCount(), is(2));
        assertThat(getObsResponse.getObservationNativeList().get(0).getUuid(), is(observationUuid1));
        assertThat(getObsResponse.getObservationNativeList().get(0).getAssetType(), is(ASSET_TYPE));
        assertThat(getObsResponse.getObservationNativeList().get(0).getAssetUuid(), is(assetUuid));
        assertThat(getObsResponse.getObservationNativeList().get(1).getUuid(), is(observationUuid2));
        assertThat(getObsResponse.getObservationNativeList().get(1).getAssetType(), is(ASSET_TYPE));
        assertThat(getObsResponse.getObservationNativeList().get(1).getAssetUuid(), is(assetUuid));
    }
    
    /**
     * Verify that the correct observation count can be retrieved based on the given query.
     * Given query contains the asset sub type.
     */
    @Test
    public void testGetObservationCountWithQueryParams() throws IOException, InterruptedException
    {
        //message listener that will wait for responses
        MessageListener listener = new MessageListener(socket);
        
        //create get observation request
        Query query = Query.newBuilder().
            addObservationSubType(ObservationSubTypeEnumConverter
                    .convertJavaEnumToProto(ObservationSubTypeEnum.DETECTION)).build();

        GetObservationRequestData obsResquest = GetObservationRequestData.newBuilder().
            setObsQuery(query).build();
        
        //send request
        TerraHarvestMessage message = createObservationStoreMessage(ObservationStoreMessageType.GetObservationRequest,
            obsResquest);

        //send request
        message.writeDelimitedTo(socket.getOutputStream());

        //wait for response
        Message response = listener.waitForMessage(Namespace.ObservationStore, 
            ObservationStoreMessageType.GetObservationResponse, WAIT_TIME_MS);

        //check observation is the observation expected
        ObservationStoreNamespace obsNamespaceResponse = (ObservationStoreNamespace) response;
        GetObservationResponseData getObsResponse = GetObservationResponseData.parseFrom(
            obsNamespaceResponse.getData());
        
        assertThat(getObsResponse.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        int initialCount = getObsResponse.getObservationNativeCount();
        
        // capture 5 observations
        for (int i=0; i < 5; i++)
        {
            AssetNamespaceUtils.requestDataCapture(socket, assetUuid);
        }
        
        message.writeDelimitedTo(socket.getOutputStream());

        //wait for response
        response = listener.waitForMessage(Namespace.ObservationStore, 
            ObservationStoreMessageType.GetObservationResponse, WAIT_TIME_MS);

        obsNamespaceResponse = (ObservationStoreNamespace)response;
        getObsResponse = GetObservationResponseData.parseFrom(obsNamespaceResponse.getData());
        
        // count should be 5 more than before
        assertThat(getObsResponse.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(getObsResponse.getObservationNativeCount(), is(initialCount + 5));
        
        //create the message
        Query queryCount = Query.newBuilder().
                addObservationSubType(ObservationSubTypeEnumConverter
                        .convertJavaEnumToProto(ObservationSubTypeEnum.DETECTION)).build();
        
        GetObservationCountRequestData request = GetObservationCountRequestData
                .newBuilder().setObsQuery(queryCount).build();
        
        TerraHarvestMessage messageCount = createObservationStoreMessage(
                ObservationStoreMessageType.GetObservationCountRequest,
                request);
        
        //send request
        messageCount.writeDelimitedTo(socket.getOutputStream());
        
        Message responseCount = listener.waitForMessage(Namespace.ObservationStore, 
                ObservationStoreMessageType.GetObservationCountResponse, WAIT_TIME_MS);
        
        obsNamespaceResponse = (ObservationStoreNamespace)responseCount;
        GetObservationCountResponseData getObsCountResponse = 
                GetObservationCountResponseData.parseFrom(obsNamespaceResponse.getData());
        
        assertThat((int)getObsCountResponse.getCount(), is(getObsResponse.getObservationNativeCount()));
    }
    
    /**
     * Verify that the correct observation count can be retrieved based on the given query.
     * Given query contains no set fields
     */
    @Test
    public void testGetObservationCountWithQueryDefault() throws IOException, InterruptedException
    {
        //message listener that will wait for responses
        MessageListener listener = new MessageListener(socket);
        
        //create get observation request
        Query query = Query.getDefaultInstance();
        GetObservationRequestData obsResquest = GetObservationRequestData.newBuilder().
            setObsQuery(query).build();
        
        //send request
        TerraHarvestMessage message = createObservationStoreMessage(ObservationStoreMessageType.GetObservationRequest,
            obsResquest);

        //send request
        message.writeDelimitedTo(socket.getOutputStream());

        //wait for response
        Message response = listener.waitForMessage(Namespace.ObservationStore, 
            ObservationStoreMessageType.GetObservationResponse, WAIT_TIME_MS);

        //check observation is the observation expected
        ObservationStoreNamespace obsNamespaceResponse = (ObservationStoreNamespace) response;
        GetObservationResponseData getObsResponse = GetObservationResponseData.parseFrom(
            obsNamespaceResponse.getData());
        
        assertThat(getObsResponse.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(getObsResponse.getObservationNativeCount(), greaterThan(0));
               
        //create the message
        Query queryCount = Query.getDefaultInstance();
        
        GetObservationCountRequestData request = GetObservationCountRequestData
                .newBuilder().setObsQuery(queryCount).build();
        
        TerraHarvestMessage messageCount = createObservationStoreMessage(
                ObservationStoreMessageType.GetObservationCountRequest,
                request);
        
        //send request
        messageCount.writeDelimitedTo(socket.getOutputStream());
        
        Message responseCount = listener.waitForMessage(Namespace.ObservationStore, 
                ObservationStoreMessageType.GetObservationCountResponse, WAIT_TIME_MS);
        
        obsNamespaceResponse = (ObservationStoreNamespace)responseCount;
        GetObservationCountResponseData getObsCountResponse = 
                GetObservationCountResponseData.parseFrom(obsNamespaceResponse.getData());
        
        assertThat((int)getObsCountResponse.getCount(), is(getObsResponse.getObservationNativeCount()));
    }
    
    /**
     * Test removing observations from a remote system via observation type. 
     * 
     * Verify an observations are removed.
     */
    @Test
    public void testRemoveObservationSubType() throws IOException, InterruptedException
    {
        //create observation
        AssetNamespaceUtils.requestDataCapture(socket, assetUuid);

        //message listener that will wait for responses
        MessageListener listener = new MessageListener(socket);

        //create get observation request
        Query query = Query.newBuilder().
            addObservationSubType(ObservationSubTypeEnumConverter
                    .convertJavaEnumToProto(ObservationSubTypeEnum.DETECTION)).build();

        RemoveObservationRequestData obsResquest = RemoveObservationRequestData.newBuilder().
            setObsQuery(query).build();

        //send request
        TerraHarvestMessage message = createObservationStoreMessage(
            ObservationStoreMessageType.RemoveObservationRequest, obsResquest);

        //send request
        message.writeDelimitedTo(socket.getOutputStream());

        //wait for response
        listener.waitForMessage(Namespace.ObservationStore, ObservationStoreMessageType.RemoveObservationResponse, 
                WAIT_TIME_MS);
    }

    /**
     * Test removing observations from a remote system via asset. 
     * 
     * Verify an observations are removed.
     */
    @Test
    public void testRemoveObservationAsset() throws IOException, InterruptedException
    {
        AssetNamespaceUtils.requestDataCapture(socket, assetUuid);

        //message listener that will wait for responses
        MessageListener listener = new MessageListener(socket);

        //create get observation request
        Query query = Query.newBuilder().setAssetUuid(assetUuid).build();
        RemoveObservationRequestData obsResquest = RemoveObservationRequestData.newBuilder().
            setObsQuery(query).build();

        //send request
        TerraHarvestMessage message = createObservationStoreMessage(
            ObservationStoreMessageType.RemoveObservationRequest, obsResquest);

        //send request
        message.writeDelimitedTo(socket.getOutputStream());

        //wait for response
        listener.waitForMessage(Namespace.ObservationStore, 
            ObservationStoreMessageType.RemoveObservationResponse, WAIT_TIME_MS);
    }

    /**
     * Test removing observations from a remote system via asset type. 
     * 
     * Verify an observations are removed.
     */
    @Test
    public void testRemoveObservationAssetType() throws IOException, InterruptedException
    {
        //create observation
        AssetNamespaceUtils.requestDataCapture(socket, assetUuid);

        //message listener that will wait for responses
        MessageListener listener = new MessageListener(socket);

        //create get observation request
        Query query = Query.newBuilder().setAssetType(ASSET_TYPE).build();
        RemoveObservationRequestData obsResquest = RemoveObservationRequestData.newBuilder().
            setObsQuery(query).build();

        //send request
        TerraHarvestMessage message = createObservationStoreMessage(
            ObservationStoreMessageType.RemoveObservationRequest, obsResquest);

        //send request
        message.writeDelimitedTo(socket.getOutputStream());

        //wait for response
        listener.waitForMessage(Namespace.ObservationStore, 
            ObservationStoreMessageType.RemoveObservationResponse, WAIT_TIME_MS);
    }

    /**
     * Test removing observations from a remote system via UUID. 
     * 
     * Verify an observation UUID is returned.
     */
    @Test
    public void testRemoveObservationUUID() throws IOException, InterruptedException
    {
        //message listener that will wait for responses
        MessageListener listener = new MessageListener(socket);

        //create observation
        SharedMessages.UUID observationUuid = AssetNamespaceUtils.requestDataCaptureReturnObsUuid(socket, assetUuid);

        //create remove request
        RemoveObservationByUUIDRequestData obsResquest = RemoveObservationByUUIDRequestData.newBuilder().
            addUuidOfObservation(observationUuid).build();

        //send request
        TerraHarvestMessage message = createObservationStoreMessage(
            ObservationStoreMessageType.RemoveObservationByUUIDRequest, obsResquest);

        //send request
        message.writeDelimitedTo(socket.getOutputStream());

        //wait for response
        listener.waitForMessage(Namespace.ObservationStore, 
            ObservationStoreMessageType.RemoveObservationByUUIDResponse, WAIT_TIME_MS);
    }

    /**
     * Test getting observations from a remote system via AssetType with time constraints. 
     * 
     * Verify observations are returned.
     */
    @Test
    public void testGetObservationAssetTypeTime() throws IOException, InterruptedException
    {
        //start time
        long startTime = Calendar.getInstance().getTimeInMillis();
        
        //stop time get some time in the future
        long stopTime = startTime + 5000;
        
        //create observation
        AssetNamespaceUtils.requestDataCapture(socket, assetUuid);

        //message listener that will wait for responses
        MessageListener listener = new MessageListener(socket);

        //create get observation request
        TimeConstraintData time = TimeConstraintData.newBuilder().
            setStartTime(startTime).setStopTime(stopTime).build();
        Query query = Query.newBuilder().
            setAssetType(ASSET_TYPE).setCreatedTimeRange(time).build();
        GetObservationRequestData obsResquest = GetObservationRequestData.newBuilder().
            setObsQuery(query).build();

        //send request
        TerraHarvestMessage message = createObservationStoreMessage(ObservationStoreMessageType.GetObservationRequest,
            obsResquest);

        //send request
        message.writeDelimitedTo(socket.getOutputStream());

        //wait for response
        Message response = listener.waitForMessage(Namespace.ObservationStore, 
            ObservationStoreMessageType.GetObservationResponse, WAIT_TIME_MS);

        //check observation is the observation expected
        ObservationStoreNamespace obsNamespaceResponse = (ObservationStoreNamespace) response;
        GetObservationResponseData getObsResponse = GetObservationResponseData.parseFrom(
            obsNamespaceResponse.getData());

        //greater than 1 because if this is running on a clean controller or if before other
        //tests the number of observations in the observation store for the asset type can be different.
        assertThat(getObsResponse.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(getObsResponse.getObservationNativeCount(), greaterThan(0));
        assertThat(getObsResponse.getObservationNativeList().get(0).getAssetType(), is(ASSET_TYPE));
    }
    
    /**
     * Test getting observations from a remote system via AssetType with time constraints. 
     * 
     * Verify observations are returned within the observed time constraints.
     */
    @Test
    public void testGetObservationAssetAndObservedTime() throws IOException, InterruptedException
    {
        //start time - observed timestamp is offset by 1 second
        long startTime = Calendar.getInstance().getTimeInMillis() - 1000;
        
        //stop time get some time in the future
        long stopTime = startTime + 5000;
        
        //create observation
        AssetNamespaceUtils.requestDataCapture(socket, assetUuid);

        //message listener that will wait for responses
        MessageListener listener = new MessageListener(socket);

        //create get observation request
        TimeConstraintData time = TimeConstraintData.newBuilder().
            setStartTime(startTime).setStopTime(stopTime).build();
        Query query = Query.newBuilder().
            setAssetType(ASSET_TYPE).setObservedTimeRange(time).build();
        GetObservationRequestData obsResquest = GetObservationRequestData.newBuilder().
            setObsQuery(query).build();

        //send request
        TerraHarvestMessage message = createObservationStoreMessage(ObservationStoreMessageType.GetObservationRequest,
            obsResquest);

        //send request
        message.writeDelimitedTo(socket.getOutputStream());

        //wait for response
        Message response = listener.waitForMessage(Namespace.ObservationStore, 
            ObservationStoreMessageType.GetObservationResponse, WAIT_TIME_MS);

        //check observation is the observation expected
        ObservationStoreNamespace obsNamespaceResponse = (ObservationStoreNamespace) response;
        GetObservationResponseData getObsResponse = GetObservationResponseData.parseFrom(
            obsNamespaceResponse.getData());

        //greater than 1 because if this is running on a clean controller or if before other
        //tests the number of observations in the observation store for the asset type can be different.
        assertThat(getObsResponse.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(getObsResponse.getObservationNativeCount(), greaterThan(0));
        assertThat(getObsResponse.getObservationNativeList().get(0).getAssetType(), is(ASSET_TYPE));
    }
    
    /**
     * Test getting observations from a remote system via AssetType with the maximum observation constraint. 
     * 
     * Verify observations are returned.
     */
    @Test
    public void testGetObservationAssetTypeMaxObs() throws IOException, InterruptedException
    {
        //create observation
        for (int i = 0; i < 5; i++)
        {
            AssetNamespaceUtils.requestDataCapture(socket, assetUuid);
        }

        //message listener that will wait for responses
        MessageListener listener = new MessageListener(socket);

        //create get observation request to retrieve all observations
        Query query = Query.newBuilder().setAssetType(ASSET_TYPE).build();
        GetObservationRequestData obsRequest = GetObservationRequestData.newBuilder().
            setObsQuery(query).build();
        
        //send request
        TerraHarvestMessage message = createObservationStoreMessage(ObservationStoreMessageType.GetObservationRequest,
            obsRequest);
        
        //send request
        message.writeDelimitedTo(socket.getOutputStream());
        
        //wait for response
        Message response = listener.waitForMessage(Namespace.ObservationStore, 
            ObservationStoreMessageType.GetObservationResponse, WAIT_TIME_MS);
        
        //check observation is the observation expected
        ObservationStoreNamespace obsNamespaceResponse = (ObservationStoreNamespace) response;
        GetObservationResponseData getObsResponse = GetObservationResponseData.parseFrom(
            obsNamespaceResponse.getData());

        //greater than 5 because if this is running on a clean controller or if before other
        //tests the number of observations in the observation store for the asset type can be different.
        assertThat(getObsResponse.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(getObsResponse.getObservationNativeCount(), greaterThanOrEqualTo(5));
        assertThat(getObsResponse.getObservationNativeList().get(0).getAssetType(), is(ASSET_TYPE));
        
        //create get observation request with limited observations
        Query maxQuery = Query.newBuilder().setAssetType(ASSET_TYPE).setMaxNumberOfObs(2).build();
        GetObservationRequestData maxObsRequest = GetObservationRequestData.newBuilder().
            setObsQuery(maxQuery).build();

        //send request
        message = createObservationStoreMessage(ObservationStoreMessageType.GetObservationRequest,
                maxObsRequest);

        //send request
        message.writeDelimitedTo(socket.getOutputStream());

        //wait for response
        response = listener.waitForMessage(Namespace.ObservationStore, 
            ObservationStoreMessageType.GetObservationResponse, WAIT_TIME_MS);

        //check observation is the observation expected
        obsNamespaceResponse = (ObservationStoreNamespace) response;
        GetObservationResponseData getMaxObsResponse = 
                GetObservationResponseData.parseFrom(obsNamespaceResponse.getData());

        //equal to 2 because at least 5 observations exist on the controller but a maximum of 2 were requested
        assertThat(getObsResponse.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(getMaxObsResponse.getObservationNativeCount(), is(2));
        assertThat(getMaxObsResponse.getObservationNativeList().get(0).getAssetType(), is(ASSET_TYPE));
    }

    /**
     * Helper method for creating observation store messages to be sent to controller. 
     * @param type
     *      type of message to be contained in the sent TerraHarvestMessage
     * @param message
     *      message data to be contained in the sent TerraHarvestMessage
     * @return
     *      TerraHarvestMessage to be sent to the controller
     */
    public static TerraHarvestMessage createObservationStoreMessage(final ObservationStoreMessageType type, 
            final Message message)
    {
        ObservationStoreNamespace.Builder obsMessageBuilder = ObservationStoreNamespace.newBuilder().
                setType(type).
                setData(message.toByteString());

        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.
                createTerraHarvestMsg(Namespace.ObservationStore, obsMessageBuilder);
        return thMessage;
    }
}
