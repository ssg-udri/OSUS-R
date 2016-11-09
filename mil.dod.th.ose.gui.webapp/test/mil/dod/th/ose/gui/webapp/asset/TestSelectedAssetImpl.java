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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace.
            AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.AssetMessages.ActivateRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace.AssetMessageType;
import mil.dod.th.core.remote.proto.AssetMessages.CaptureDataRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.DeactivateRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.DeleteRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.PerformBitRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.SetNameRequestData;
import mil.dod.th.ose.gui.webapp.factory.AbstractFactoryBaseModel;
import mil.dod.th.ose.gui.webapp.utils.FacesContextUtil;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.primefaces.context.RequestContext;

import com.google.protobuf.Message;

/**
 * Test class for the asset selected implementation
 * @author callen
 *
 */
public class TestSelectedAssetImpl 
{
    private SelectedAssetImpl m_SUT;
    private MessageFactory m_MessageFactory;
    private AssetMgrImpl m_AssetMgr;
    private MessageWrapper m_MessageWrapper;
    
    private RequestContext m_RequestContext;
    private FacesContextUtil m_FacesUtil;
    
    //UUID to use
    private UUID uuid = UUID.randomUUID();
    //controller id to use
    private int systemId = 321;
    
    @Before
    public void setUp()
    {
        m_MessageFactory = mock(MessageFactory.class);
        m_AssetMgr = mock(AssetMgrImpl.class);
        m_MessageWrapper = mock(MessageWrapper.class);
        
        m_RequestContext = mock(RequestContext.class);
        m_FacesUtil = mock(FacesContextUtil.class);
        
        //create selected asset bean
        m_SUT = new SelectedAssetImpl();
        
        //set injected service
        m_SUT.setAssetMgr(m_AssetMgr);
        m_SUT.setMessageFactory(m_MessageFactory);
        
        m_SUT.setFacesContextUtil(m_FacesUtil);
        
        when(m_MessageFactory.createAssetDirectoryServiceMessage(Mockito.any(AssetDirectoryServiceMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        when(m_MessageFactory.createAssetMessage(Mockito.any(AssetMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        
        when(m_FacesUtil.getRequestContext()).thenReturn(m_RequestContext);
    }
    
    /**
     * Test setting an asset model.
     * Verify the correct asset is sent when getting the asset model.
     */
    @Test
    public void testSetAssetModel()
    {
        AssetModel asset = mock(AssetModel.class);
        m_SUT.setSelectedAssetForDialog(asset);
       
        assertThat(m_SUT.getSelectedAssetForDialog(), is((AbstractFactoryBaseModel)asset));
    }
    
    /**
     * Test requesting activation.
     * Verify message sent with correct uuid.
     */
    @Test
    public void testRequestActivation()
    {
        //create mock asset model
        AssetModel model = mock(AssetModel.class);
        when(model.getUuid()).thenReturn(uuid);
        when(model.getControllerId()).thenReturn(systemId);

        //handle request
        m_SUT.requestActivation(model);

        //verify message sent, should be activate since the model represents the asset is not active
        ArgumentCaptor<ActivateRequestData> activateReq = 
            ArgumentCaptor.forClass(ActivateRequestData.class);

        verify(m_MessageFactory).createAssetMessage(
            eq(AssetMessageType.ActivateRequest), activateReq.capture());
        verify(m_MessageWrapper).queue(eq(systemId), (ResponseHandler) eq(null));
        
        //get message
        ActivateRequestData requestActivate = activateReq.getValue();
        
        //verify
        assertThat(requestActivate.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(uuid)));
        
        //Verify that the selected asset is returned to null after the request.
        assertThat(m_SUT.getSelectedAssetForDialog(), is(nullValue()));
    }
    
    /**
     * Test the request to deactivate an asset.
     * Verify the correct request is sent.
     */
    @Test
    public void testDeactivationRequest()
    {
        //create mock asset model
        AssetModel model = mock(AssetModel.class);
        when(model.getUuid()).thenReturn(uuid);
        when(model.getControllerId()).thenReturn(systemId);
        
        //handle request
        m_SUT.requestDeactivation(model);
        
        //verify message sent, should be deactivate since the model represents the asset already being activated
        ArgumentCaptor<DeactivateRequestData> deactivateReq = 
            ArgumentCaptor.forClass(DeactivateRequestData.class);

        verify(m_MessageFactory).createAssetMessage(
            eq(AssetMessageType.DeactivateRequest), deactivateReq.capture());
        verify(m_MessageWrapper).queue(eq(systemId), (ResponseHandler) eq(null));
        
        //get message
        DeactivateRequestData request = deactivateReq.getValue();

        //verify
        assertThat(request.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(uuid)));
        
        //Verify that the selected asset is returned to null after the request.
        assertThat(m_SUT.getSelectedAssetForDialog(), is(nullValue()));
    }
    
    /**
     * Test the request for an asset to capture data.
     * Verify message sent.
     */
    @Test
    public void testRequestCaptureData()
    {
        //create mock asset model
        AssetModel model = mock(AssetModel.class);
        when(model.getUuid()).thenReturn(uuid);
        when(model.getControllerId()).thenReturn(systemId);
        
        m_SUT.requestCaptureData(model);
        
        //verify message sent to request the data capturing
        ArgumentCaptor<CaptureDataRequestData> request = 
            ArgumentCaptor.forClass(CaptureDataRequestData.class);

        verify(m_MessageFactory).createAssetMessage(eq(AssetMessageType.CaptureDataRequest), request.capture());
        verify(m_MessageWrapper).queue(eq(systemId), (ResponseHandler) eq(null));

        //get message
        CaptureDataRequestData requestCapture = request.getValue();
        
        //verify
        assertThat(requestCapture.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(uuid)));
        assertThat(requestCapture.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.UUID_ONLY));
        
        //Verify that the selected asset is returned to null after the request.
        assertThat(m_SUT.getSelectedAssetForDialog(), is(nullValue()));
    }
    
    /**
     * Test the request for an asset to perform it's BIT.
     * Verify message sent.
     */
    @Test
    public void testRequestPerformBIT()
    {
        //create mock asset model
        AssetModel model = mock(AssetModel.class);
        when(model.getUuid()).thenReturn(uuid);
        when(model.getControllerId()).thenReturn(systemId);
        
        m_SUT.requestBIT(model);
        
        //verify message sent to request the data capturing
        ArgumentCaptor<PerformBitRequestData> request = 
            ArgumentCaptor.forClass(PerformBitRequestData.class);

        verify(m_MessageFactory).createAssetMessage(eq(AssetMessageType.PerformBitRequest), request.capture());
        verify(m_MessageWrapper).queue(eq(systemId), (ResponseHandler) eq(null));

        //get message
        PerformBitRequestData requestCapture = request.getValue();
        
        //verify
        assertThat(requestCapture.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(uuid)));
        
        //Verify that the selected asset is returned to null after the request.
        assertThat(m_SUT.getSelectedAssetForDialog(), is(nullValue()));
    }
    
    /**
     * Test the request for an asset to be removed.
     * Verify message sent.
     */
    @Test
    public void testRequestRemoval()
    {
        //create mock asset model
        AssetModel model = mock(AssetModel.class);
        when(model.getUuid()).thenReturn(uuid);
        when(model.getControllerId()).thenReturn(systemId);
        
        m_SUT.requestRemoval(model);
        
        //verify message sent to remove the assets
        ArgumentCaptor<DeleteRequestData> request = 
            ArgumentCaptor.forClass(DeleteRequestData.class);

        verify(m_MessageFactory).createAssetMessage(
            eq(AssetMessageType.DeleteRequest), request.capture());
        verify(m_MessageWrapper).queue(eq(systemId), (ResponseHandler) eq(null));
        
        // verify
        verify(m_RequestContext).execute("unregisterComponents('" + model.getUuid() + "')");

        //get message
        DeleteRequestData requestCapture = request.getValue();
                
        //verify
        assertThat(requestCapture.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(uuid)));
        
        //Verify that the selected asset is returned to null after the request.
        assertThat(m_SUT.getSelectedAssetForDialog(), is(nullValue()));
    }
    
       
    /**
     * Test the request for an asset's name to be set/updated.
     * Verify message sent.
     */
    @Test
    public void testRequestNameUpdate()
    {
        //create mock asset model
        AssetModel model = mock(AssetModel.class);
        when(model.getControllerId()).thenReturn(systemId);
        when(model.getWorkingName()).thenReturn("AssetMcAsset");
        when(model.getUuid()).thenReturn(uuid);
        
        m_SUT.requestNameUpdate(model);
        
        //verify message sent to config admin
        ArgumentCaptor<SetNameRequestData> request = 
            ArgumentCaptor.forClass(SetNameRequestData.class);

        verify(m_MessageFactory).createAssetMessage(
            eq(AssetMessageType.SetNameRequest), request.capture());
        verify(m_MessageWrapper).queue(eq(systemId), (ResponseHandler) eq(null));

        //get message
        SetNameRequestData requestCapture = request.getValue();
        
        //verify
        assertThat(requestCapture.getAssetName(), is("AssetMcAsset"));
        assertThat(requestCapture.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(uuid)));
        
        //Verify that the selected asset is returned to null after the request.
        assertThat(m_SUT.getSelectedAssetForDialog(), is(nullValue()));
    }
}
