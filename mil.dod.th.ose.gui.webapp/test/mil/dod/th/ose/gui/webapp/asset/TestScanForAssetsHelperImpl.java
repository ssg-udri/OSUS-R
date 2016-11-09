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
package mil.dod.th.ose.gui.webapp.asset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.faces.application.FacesMessage;

import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages
    .AssetDirectoryServiceNamespace.AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.ScanForNewAssetsRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage.Version;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.google.protobuf.Message;

/**
 * Test class for {@link ScanForAssetsHelperImpl}.
 * 
 * @author cweisenborn
 */
public class TestScanForAssetsHelperImpl
{
    private ScanForAssetsHelperImpl m_SUT;
    
    @Mock private MessageFactory m_MessageFactory;
    @Mock private MessageWrapper m_MessageWrapper;
    @Mock private GrowlMessageUtil m_GrowlUtil;
    
    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        
        m_SUT = new ScanForAssetsHelperImpl();
        
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setGrowlMessageUtil(m_GrowlUtil);
    }
    
    /**
     * Verify that the request scan for assets method sends the appropriate request message using the 
     * message factory.
     */
    @Test
    public void testRequestScanForAssets()
    {
        final int controllerId = 500;
        
        when(m_MessageFactory.createAssetDirectoryServiceMessage(AssetDirectoryServiceMessageType.GetAssetTypesRequest, 
                null)).thenReturn(m_MessageWrapper);
        
        m_SUT.requestScanForAssets(controllerId);
        
        verify(m_MessageFactory).createAssetDirectoryServiceMessage(
                AssetDirectoryServiceMessageType.GetAssetTypesRequest, null);
        verify(m_MessageWrapper).queue(controllerId, m_SUT);
    }
    
    /**
     * Verify that a scan for assets request is sent using the message factory when a get asset types response message
     * is received.
     */
    @Test
    public void testHandlerResponseTypesRequest()
    {
        when(m_MessageFactory.createAssetDirectoryServiceMessage(
                eq(AssetDirectoryServiceMessageType.ScanForNewAssetsRequest), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        
        AssetDirectoryServiceNamespace namespaceMsg = AssetDirectoryServiceNamespace.newBuilder()
                .setType(AssetDirectoryServiceMessageType.GetAssetTypesResponse).build();
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().setNamespace(Namespace.AssetDirectoryService)
                .setNamespaceMessage(namespaceMsg.toByteString()).build();
        Version version = Version.newBuilder().setMajor(0).setMinor(0).build();
        TerraHarvestMessage thMsg = TerraHarvestMessage.newBuilder().setDestId(0).setSourceId(500).setMessageId(5)
                    .setVersion(version).setTerraHarvestPayload(payload.toByteString()).build();
        
        m_SUT.handleResponse(thMsg, payload, namespaceMsg, null);
        
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        
        verify(m_MessageFactory).createAssetDirectoryServiceMessage(
                eq(AssetDirectoryServiceMessageType.ScanForNewAssetsRequest), messageCaptor.capture());
    
        ScanForNewAssetsRequestData request = (ScanForNewAssetsRequestData)messageCaptor.getValue();
        assertThat(request.getProductType(), equalTo(""));
    }
    
    /**
     * Verify that a growl message is posted when a get scan for asset response message
     * is received.
     */
    @Test
    public void testHandlerResponseScanRequest()
    {
        when(m_MessageFactory.createAssetDirectoryServiceMessage(
                eq(AssetDirectoryServiceMessageType.ScanForNewAssetsRequest), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        
        AssetDirectoryServiceNamespace namespaceMsg = AssetDirectoryServiceNamespace.newBuilder()
                .setType(AssetDirectoryServiceMessageType.ScanForNewAssetsResponse).build();
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().setNamespace(Namespace.AssetDirectoryService)
                .setNamespaceMessage(namespaceMsg.toByteString()).build();
        Version version = Version.newBuilder().setMajor(0).setMinor(0).build();
        TerraHarvestMessage thMsg = TerraHarvestMessage.newBuilder().setDestId(0).setSourceId(500).setMessageId(5)
                    .setVersion(version).setTerraHarvestPayload(payload.toByteString()).build();
        
        m_SUT.handleResponse(thMsg, payload, namespaceMsg, null);

        verify(m_GrowlUtil).createLocalFacesMessage(FacesMessage.SEVERITY_INFO, "Asset Scan Complete", 
                "Scan for new assets has completed.");
    }
}
