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
package mil.dod.th.ose.remote.integration.secondrun;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetRequestData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace.
    AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.AssetMessages.CaptureDataRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace.AssetMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.ose.remote.integration.MessageListener;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import mil.dod.th.ose.remote.integration.TerraHarvestMessageHelper;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import example.asset.ExampleAsset;

/**
 * Test the remote event registrations are persisted.
 * @author callen
 *
 */
public class TestPersistedRemoteEventRegistrations
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
    public void tearDown() throws IOException
    {
        m_Socket.close();
    }
    
    /**
     * Test that an event registration for data capture was persisted through shutdown.
     */
    @Test
    public void testPersistedRegistration() throws UnknownHostException, IOException, InterruptedException
    {
        //used to register for events
        MessageListener listener = new MessageListener(m_Socket);

        CreateAssetRequestData request = CreateAssetRequestData.newBuilder().
                setProductType(ExampleAsset.class.getName()).build();
        AssetDirectoryServiceNamespace.Builder assetDirMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(AssetDirectoryServiceMessageType.CreateAssetRequest).
                setData(request.toByteString());
        TerraHarvestMessage message = TerraHarvestMessageHelper.
                createAdditionalSystemTerraHarvestMsg(Namespace.AssetDirectoryService, assetDirMessage);
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        AssetDirectoryServiceNamespace response = (AssetDirectoryServiceNamespace)listener.waitForMessage(
                Namespace.AssetDirectoryService, AssetDirectoryServiceMessageType.CreateAssetResponse, 2000);
        //create the asset
        CreateAssetResponseData assetCreation = 
                CreateAssetResponseData.parseFrom(response.getData());
        
        //will hold the create assets UUID
        final SharedMessages.UUID assetUuid = assetCreation.getInfo().getUuid();
        
        // build request
        CaptureDataRequestData requestCapture = CaptureDataRequestData.newBuilder()
                .setUuid(assetUuid)
                .setObservationFormat(RemoteTypesGen.LexiconFormat.Enum.UUID_ONLY).build();
        AssetNamespace.Builder assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.CaptureDataRequest).
                setData(requestCapture.toByteString());
        message = TerraHarvestMessageHelper.
                createAdditionalSystemTerraHarvestMsg(Namespace.Asset, assetMessage);
        
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        listener.waitForRemoteEvent(Asset.TOPIC_DATA_CAPTURED, 1000);
    }
}
