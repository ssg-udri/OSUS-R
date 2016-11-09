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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace.
    AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetAssetTypesResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetAssetsResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetCapabilitiesRequestData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetScannableAssetTypesResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.ScanForNewAssetsRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace;
import mil.dod.th.core.remote.proto.AssetMessages.GetNameRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.GetNameResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace.AssetMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.SharedMessages.FactoryObjectInfo;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.ose.remote.integration.AssetNamespaceUtils;
import mil.dod.th.ose.remote.integration.MessageMatchers.BasicMessageMatcher;
import mil.dod.th.ose.remote.integration.MessageMatchers.EventMessageMatcher;
import mil.dod.th.ose.remote.integration.RemoteEventRegistration;
import mil.dod.th.ose.remote.integration.MessageListener;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import example.asset.ExampleAsset;
import example.asset.ExampleRequiredPropAsset;
import example.asset.ExampleRequiredPropAssetAttributes;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.Message;

/**
 * Tests the interaction of the remote interface with the {@link AssetDirectoryServiceNamespace}.  Specifically, 
 * the class tests that AssetDirectoryMessageService messages are properly sent and that appropriate responses are
 * received.    
 * @author bachmakm
 *
 */
public class TestAssetDirectoryServiceNamespace
{
    private Socket socket; //socket for remote interface communications
    
    /**
     * Creates an example asset to be used for each of the unit tests.  
     */
    @Before
    public void setup() throws UnknownHostException, IOException
    {
        socket = SocketHostHelper.connectToController();
    }
    
    /**
     * Ensure that the socket is closed.
     */
    @After
    public void tearDown() throws IOException
    {
        AssetNamespaceUtils.removeAllAssets(socket);
        MessageListener.unregisterEvent(socket);
        socket.close();
    }
    
    /**
     * Ensures that a valid UUID was returned after the create asset request sent in the 
     * setup method.  
     */
    @Test
    public final void testCreateAsset() throws UnknownHostException, IOException
    {          
        FactoryObjectInfo info =
            AssetNamespaceUtils.createAsset(socket, ExampleAsset.class.getName(), "createAsset", null).getInfo();
        assertThat(info.getUuid(), is(notNullValue()));
        assertThat("Pid should be empty initially", info.getPid(), is(""));
    }
    
    /**
     * Ensures that a valid UUID and PID is returned after the create asset request was sent with configuration 
     * properties specified.
     */
    @Test
    public final void testCreateAssetWithProps() throws UnknownHostException, IOException
    {
        final Map<String, Object> props = new HashMap<>();
        props.put(ExampleRequiredPropAssetAttributes.CONFIG_PROP_HOSTNAME, "127.0.0.1");
        props.put(ExampleRequiredPropAssetAttributes.CONFIG_PROP_PORT_NUM, 1337);
        FactoryObjectInfo info = AssetNamespaceUtils.createAsset(socket, ExampleRequiredPropAsset.class.getName(), 
                "createAsset", props).getInfo();
        assertThat(info.getUuid(), is(notNullValue()));
        assertThat("Pid should not be empty", info.getPid().isEmpty(), is(false));
    }
    
    /**
     * Verifies the ability of the system to handle a remote request to scan for any new assets.
     */
    @Test
    public final void testScanForNewAssets() throws UnknownHostException, IOException
    {                
        MessageListener listener = new MessageListener(socket);
        String[] topics = new String[]{AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS, 
            AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS_COMPLETE, FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED};
        
        int regId = RemoteEventRegistration.regRemoteEventMessages(socket, topics);
        
        //create new request to scan for assets of specific type
        ScanForNewAssetsRequestData request = ScanForNewAssetsRequestData.newBuilder().
                setProductType(ExampleAsset.class.getName()).build();      
              
        TerraHarvestMessage message = AssetNamespaceUtils.createAssetDirSvcNamespaceMessage(
                AssetDirectoryServiceMessageType.ScanForNewAssetsRequest, request);        
        message.writeDelimitedTo(socket.getOutputStream());

        listener.waitForMessages(15000, new EventMessageMatcher(AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS),
                new EventMessageMatcher(AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS_COMPLETE),
                new EventMessageMatcher(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED),
                new BasicMessageMatcher(Namespace.AssetDirectoryService, 
                        AssetDirectoryServiceMessageType.ScanForNewAssetsResponse));

        //create request to scan for ALL asset types
        request = ScanForNewAssetsRequestData.newBuilder().build(); 

        message = AssetNamespaceUtils.createAssetDirSvcNamespaceMessage(
                AssetDirectoryServiceMessageType.ScanForNewAssetsRequest, request);        
        message.writeDelimitedTo(socket.getOutputStream());
        
        // scanned again, but should not find obj
        listener.waitForMessages(15000, new EventMessageMatcher(AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS),
                new EventMessageMatcher(AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS_COMPLETE),
                new BasicMessageMatcher(Namespace.AssetDirectoryService, 
                        AssetDirectoryServiceMessageType.ScanForNewAssetsResponse));
        
        //clean up registrations and new asset
        MessageListener.unregisterEvent(regId, socket);
    }
    
    /**
     * Verify ability to get scannable asset types.
     */
    @Test
    public void testGetScannableAssetTypes() throws IOException
    {
        MessageListener listener = new MessageListener(socket);
        
        TerraHarvestMessage message = AssetNamespaceUtils.createAssetDirSvcNamespaceMessage(
                AssetDirectoryServiceMessageType.GetScannableAssetTypesRequest, null);        
        message.writeDelimitedTo(socket.getOutputStream());

        Message responseRcvd = listener.waitForMessage(Namespace.AssetDirectoryService, 
                AssetDirectoryServiceMessageType.GetScannableAssetTypesResponse, 500);
        
        AssetDirectoryServiceNamespace response = (AssetDirectoryServiceNamespace)responseRcvd;
        GetScannableAssetTypesResponseData dataResponse = 
                GetScannableAssetTypesResponseData.parseFrom(response.getData());
        
        assertThat(dataResponse.getScannableAssetTypeCount(), is(1));
        
        assertThat(dataResponse.getScannableAssetTypeList(), 
                hasItems(ExampleAsset.class.getName()));
    }
    
    /**
     * Verifies the ability to remotely get the name and type of the assets created in a bundle,
     * assuming 1 and only 1 asset factory has been registered upon execution of this test.
     */
    @Test
    public final void testGetAssetTypes() throws IOException
    {        
        MessageListener listener = new MessageListener(socket);
                  
        TerraHarvestMessage message = AssetNamespaceUtils.createAssetDirSvcNamespaceMessage(
                AssetDirectoryServiceMessageType.GetAssetTypesRequest, null);
        message.writeDelimitedTo(socket.getOutputStream());
        
        //listen for messages for a specific time interval
        Message responseRcvd = listener.waitForMessage(Namespace.AssetDirectoryService, 
                AssetDirectoryServiceMessageType.GetAssetTypesResponse, 500);  
        AssetDirectoryServiceNamespace response = (AssetDirectoryServiceNamespace)responseRcvd;
        GetAssetTypesResponseData dataResponse = GetAssetTypesResponseData.parseFrom(response.getData());
        
        //verify name and type of created asset is in list of all assets
        assertThat(dataResponse.getProductNameCount(), is(greaterThanOrEqualTo(1)));
        assertThat(dataResponse.getProductTypeCount(), is(greaterThanOrEqualTo(1)));
        assertThat(dataResponse.getProductNameList(), hasItem("ExampleAsset"));
        assertThat(dataResponse.getProductTypeList(), hasItem(ExampleAsset.class.getName()));        
    }

    /**
     * Verifies the ability of the system to handle a remote request to get all assets known to the service.   
     */
    @Test
    public final void testGetAssets() throws IOException
    {      
        SharedMessages.UUID asset1uuid = AssetNamespaceUtils.createAsset(socket, ExampleAsset.class.getName(), "asset1",
                null).getInfo().getUuid();
        SharedMessages.UUID asset2uuid = AssetNamespaceUtils.createAsset(socket, ExampleAsset.class.getName(), "asset2",
                null).getInfo().getUuid();
        MessageListener listener = new MessageListener(socket);        
        
        TerraHarvestMessage message = AssetNamespaceUtils.createAssetDirSvcNamespaceMessage(
                AssetDirectoryServiceMessageType.GetAssetsRequest, null);        
        message.writeDelimitedTo(socket.getOutputStream());
        
        //listen for messages for a specific time interval
        AssetDirectoryServiceNamespace response = (AssetDirectoryServiceNamespace)listener.waitForMessage(
                Namespace.AssetDirectoryService, AssetDirectoryServiceMessageType.GetAssetsResponse, 500);
        
        GetAssetsResponseData dataResponse = GetAssetsResponseData.
                parseFrom(response.getData()); 
        
        //Get assets by name and check for any that contain AssetNamespaceUtils.DO_NOT_REMOVE String. These
        //should be excluded
        List<SharedMessages.UUID> includedUuidList = new ArrayList<>();
        
        for(FactoryObjectInfo type: dataResponse.getAssetInfoList())
        {           
            MessageListener assetMsgListener = new MessageListener(socket);
            GetNameRequestData getNameRequest = GetNameRequestData.newBuilder().
                    setUuid(type.getUuid()).build();
                    
            TerraHarvestMessage assetMsg = AssetNamespaceUtils.createAssetMessage(
                    AssetMessageType.GetNameRequest, getNameRequest);
            assetMsg.writeDelimitedTo(socket.getOutputStream());
            
            AssetNamespace assetResponse = (AssetNamespace)assetMsgListener.waitForMessage(
                    Namespace.Asset, AssetMessageType.GetNameResponse, 500);
            
            GetNameResponseData nameResponse = GetNameResponseData.parseFrom(assetResponse.getData());
            
            if (!nameResponse.getAssetName().contains(AssetNamespaceUtils.DO_NOT_REMOVE))
            {
                includedUuidList.add(type.getUuid());
            }         
        }

        assertThat(includedUuidList.size(), is(2));
        List<SharedMessages.UUID> myUuids = new ArrayList<SharedMessages.UUID>();
        myUuids.add(includedUuidList.get(0));
        myUuids.add(includedUuidList.get(1));    

        assertThat(myUuids, hasItems(asset1uuid, asset2uuid));
    }

    /**
     * Test setting the name of an asset.
     * Verify that the name is the expected value.
     */
    @Test
    public void testSetAssetName() throws IOException
    {
        SharedMessages.UUID uuid = AssetNamespaceUtils.createAsset(socket, ExampleAsset.class.getName(), "setNameAsset",
                null).getInfo().getUuid();
        
        //change the asset's name, assertion error thrown from within this call
        AssetNamespaceUtils.changeAssetName(socket, uuid, "santa");
    }
    
    /**
     * Verifies ability of the system to get an asset's capabilities remotely. 
     */
    @Test
    public final void testGetCapabilities() throws Exception
    {
        MessageListener listener = new MessageListener(socket);
        
        GetCapabilitiesRequestData request = GetCapabilitiesRequestData.newBuilder().
                setProductType(ExampleAsset.class.getName()).build();        
        
        TerraHarvestMessage message = AssetNamespaceUtils.createAssetDirSvcNamespaceMessage(
                AssetDirectoryServiceMessageType.GetCapabilitiesRequest, request);         
        message.writeDelimitedTo(socket.getOutputStream());
        
        //listen for messages for a specific time interval
        Message responseRcvd = listener.waitForMessage(Namespace.AssetDirectoryService, 
                AssetDirectoryServiceMessageType.GetCapabilitiesResponse, 1200);  
        AssetDirectoryServiceNamespace response = (AssetDirectoryServiceNamespace)responseRcvd;
        GetCapabilitiesResponseData dataResponse = GetCapabilitiesResponseData.parseFrom(response.getData());
        
        //test various capabilities
        assertThat(dataResponse.getCapabilities(), is(notNullValue()));
        assertThat(dataResponse.getCapabilities().getVideoCapabilities().getBase().getColorAvailable(), is(false));
        assertThat(dataResponse.getCapabilities().getVideoCapabilities().getBase().getMinResolution().getHeight(),
                is(0));
        assertThat(dataResponse.getCapabilities().getVideoCapabilities().getFramesPerSecondCount(), is(1));
        assertThat(dataResponse.getCapabilities().getVideoCapabilities().getBase().getAvailableCameraIDsCount(), is(1));
        
        assertThat(dataResponse.getCapabilities().getCommandCapabilities().getCaptureData(), is(true));
        assertThat(dataResponse.getCapabilities().getCommandCapabilities().getPerformBIT(), is(true));
        assertThat(dataResponse.getCapabilities().getCommandCapabilities().getPanTilt().getMaxAzimuth().getValue(), 
                is(50.0));
        
        assertThat(dataResponse.getCapabilities().getAudioCapabilities().getRecordersCount(), is(1));
        assertThat(dataResponse.getCapabilities().getAudioCapabilities().getRecorders(0).getDescription(), is("none"));
        assertThat(dataResponse.getCapabilities().getAudioCapabilities().getSampleRatesKHzCount(), is(1));
        assertThat(dataResponse.getCapabilities().getAudioCapabilities().getSampleRatesKHz(0), is((float)14.4));
        
        assertThat(dataResponse.getCapabilities().getDetectionCapabilities().getDirectionOfTravel(), is(false));
        assertThat(dataResponse.getCapabilities().getDetectionCapabilities().getTargetFrequency(), is(true));
        assertThat(dataResponse.getCapabilities().getDetectionCapabilities().getTargetId(), is(true));
        assertThat(dataResponse.getCapabilities().getDetectionCapabilities().getTargetLOB(), is(true));
        assertThat(dataResponse.getCapabilities().getDetectionCapabilities().getTargetLocation(), is(true));
        assertThat(dataResponse.getCapabilities().getDetectionCapabilities().getTargetOrientation(), is(true));
        assertThat(dataResponse.getCapabilities().getDetectionCapabilities().getTargetRange(), is(false));
        assertThat(dataResponse.getCapabilities().getDetectionCapabilities().getTargetSpeed(), is(false));
        assertThat(dataResponse.getCapabilities().getDetectionCapabilities().getTrackHistory(), is(true)); 
    }
}
