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
package mil.dod.th.ose.gui.webapp.observation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.UUID;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.google.protobuf.Message;

import mil.dod.th.core.persistence.ObservationQuery;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.GetObservationRequestData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.Query;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.RemoveObservationByUUIDRequestData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.RemoveObservationRequestData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.TimeConstraintData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.ObservationStoreNamespace.ObservationStoreMessageType;
import mil.dod.th.ose.gui.webapp.asset.AssetModel;
import mil.dod.th.ose.gui.webapp.controller.ActiveController;
import mil.dod.th.ose.gui.webapp.controller.ControllerModel;
import mil.dod.th.ose.gui.webapp.utils.FacesContextUtil;
import mil.dod.th.ose.shared.SharedMessageUtils;

/**
 * @author allenchl
 *
 */
public class TestRetrieveDeleteObsHelperImpl
{
    private static final Integer CONTROLLER_ID = 1000;
    
    private RetrieveDeleteObsHelperImpl m_SUT;
    private MessageFactory m_MessageFactory;
    private ObservationStore m_ObservationStore;
    private ActiveController m_ActiveController;
    private MessageWrapper m_MessageWrapper;
    private FacesContextUtil m_FacesContextUtil;
    
    @Before
    public void setup()
    {
        m_SUT = new RetrieveDeleteObsHelperImpl();
        
        //deps
        m_MessageFactory = mock(MessageFactory.class);
        m_ObservationStore = mock(ObservationStore.class);
        m_ActiveController = mock(ActiveController.class);
        m_MessageWrapper = mock(MessageWrapper.class);
        m_FacesContextUtil = mock(FacesContextUtil.class);
        
        m_SUT.setObservationStore(m_ObservationStore);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setActiveController(m_ActiveController);
        m_SUT.setFacesContextUtil(m_FacesContextUtil);
        
        when(m_MessageFactory.createObservationStoreMessage(Mockito.any(ObservationStoreMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        ControllerModel controllerModel = mock(ControllerModel.class);
        when(controllerModel.getId()).thenReturn(CONTROLLER_ID);
        when(m_ActiveController.getActiveController()).thenReturn(controllerModel);
    }
    
    /**
     * Verify that the selected uuid can be set and removed.
     */
    @Test
    public void testSelectedUUID()
    {
        UUID uuid = UUID.randomUUID();
        m_SUT.setSelectedObsUuid(uuid);
        
        assertThat(m_SUT.getSelectedObsUuid(), is(uuid));
    }
    
    /**
     * Verify the ability to get observation number filter option.
     * Verify resetting to default.
     */
    @Test
    public void testGetObservationNumber()
    {
        //verify observation number is set to 0 by default
        assertThat(m_SUT.getMaxObservationNumber(), is(0));
        
        //set it to a value
        m_SUT.setMaxObservationNumber(6);
        
        //verify new value
        assertThat(m_SUT.getMaxObservationNumber(), is(6));
    }
    
    /**
     * Verify the ability to get filter by number option.
     */
    @Test
    public void testIsFilterByObservationNumber()
    {
        //verify property is false by deflt
        assertThat(m_SUT.isRetrieveByMaxObsNum(), is(false));
        
        //set it to a value
        m_SUT.setRetrieveByMaxObsNum(true);
        
        //verify new value
        assertThat(m_SUT.isRetrieveByMaxObsNum(), is(true));
    }
    
    /**
     * Verify can delete observation.
     */
    @Test
    public void testDeleteObservation()
    {
        UUID obsId = UUID.randomUUID();
        
        //set the selected obs UUID
        m_SUT.setSelectedObsUuid(obsId);
        m_SUT.deleteObservation();
        
        verify(m_ObservationStore).remove(obsId);
        
        ArgumentCaptor<RemoveObservationByUUIDRequestData> messageCaptor = 
                ArgumentCaptor.forClass(RemoveObservationByUUIDRequestData.class);
        
        verify(m_MessageFactory).
            createObservationStoreMessage(eq(ObservationStoreMessageType.RemoveObservationByUUIDRequest),
                    messageCaptor.capture());
        verify(m_MessageWrapper).queue(eq(CONTROLLER_ID), Mockito.any(ResponseHandler.class));
        
        RemoveObservationByUUIDRequestData data = messageCaptor.getValue();
        
        assertThat(data.getUuidOfObservationCount(), is(1));
        assertThat(data.getUuidOfObservation(0), is(SharedMessageUtils.convertUUIDToProtoUUID(obsId)));
    }
    
    /**
     * Verify no message sent if the UUID for the selected obs is null.
     */
    @Test
    public void testDeleteObservationNullUuid()
    {
        //set the selected obs UUID to null
        m_SUT.setSelectedObsUuid(null);
        m_SUT.deleteObservation();
        
        verify(m_ObservationStore, never()).remove(Mockito.any(UUID.class));
        verify(m_MessageFactory, never()).
            createObservationStoreMessage(eq(ObservationStoreMessageType.RemoveObservationByUUIDRequest),
                    Mockito.any(Message.class));
    }
    
    /**
     * Verify the remote request to retrieve Observations for all known assets.
     */
    @Test
    public void testRemoteRetrievalAllAssets()
    {
        //request for observations
        m_SUT.submitRetrieveObservationsRequest(null);
        
        //verify call to remote message sender for all asset observations
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        //request
        verify(m_MessageFactory).createObservationStoreMessage(
            eq(ObservationStoreMessageType.GetObservationRequest), messageCaptor.capture());
        verify(m_MessageWrapper).queue(eq(CONTROLLER_ID), eq((ResponseHandler)null));
        
        //verify data messages
        GetObservationRequestData requestData = (GetObservationRequestData)messageCaptor.getAllValues().get(0);
        Query query = requestData.getObsQuery();
        
        assertThat(query.hasMaxNumberOfObs(), is(false));
        assertThat(query.hasCreatedTimeRange(), is(false));
        assertThat(query.hasAssetType(), is(false));
    }
    
    /**
     * Verify the remote request to retrieve Observations for a single asset.
     */
    @Test
    public void testRemoteRetrievalOneAsset()
    {
        //asset uuid
        UUID assetUUID = UUID.randomUUID();
        
        //asset model mocks
        AssetModel asset1 = mock(AssetModel.class);
        when(asset1.getUuid()).thenReturn(assetUUID);
        
        //request for observations
        m_SUT.submitRetrieveObservationsRequest(asset1);
        
        //verify call to remote message sender
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        //should be one request
        verify(m_MessageFactory).createObservationStoreMessage(
            eq(ObservationStoreMessageType.GetObservationRequest), messageCaptor.capture());
        verify(m_MessageWrapper).queue(eq(CONTROLLER_ID), eq((ResponseHandler)null));
        
        //verify data messages
        GetObservationRequestData requestData = (GetObservationRequestData)messageCaptor.getAllValues().get(0);
        Query query = requestData.getObsQuery();
        assertThat(query.hasMaxNumberOfObs(), is(false));
        assertThat(query.hasCreatedTimeRange(), is(false));
        assertThat(query.getAssetUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(assetUUID)));
    }
    
    /**
     * Verify the remote request to retrieve Observations for all known assets with time constraint.
     */
    @Test
    public void testRemoteRetrievalAllAssetsTimeConstraint()
    {
        //date objs
        Date start = new Date(1000L);
        Date stop = new Date(5000L);
        
        //dates
        m_SUT.setRetrieveByMaxObsNum(false);
        m_SUT.setRetrieveDeleteByDate(true);
        m_SUT.setStartDate(start);
        m_SUT.setEndDate(stop);
        
        //request for observations
        m_SUT.submitRetrieveObservationsRequest(null);
        
        //verify call to remote message sender for all asset observations
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        //request
        verify(m_MessageFactory).createObservationStoreMessage(
            eq(ObservationStoreMessageType.GetObservationRequest), messageCaptor.capture());
        verify(m_MessageWrapper).queue(eq(CONTROLLER_ID), eq((ResponseHandler)null));
        
        //verify data messages
        GetObservationRequestData requestData = (GetObservationRequestData)messageCaptor.getAllValues().get(0);
        Query query = requestData.getObsQuery();
        assertThat(query.hasMaxNumberOfObs(), is(false));
        assertThat(query.hasCreatedTimeRange(), is(true));
        TimeConstraintData timeData = query.getCreatedTimeRange();
        assertThat(timeData.getStartTime(), is(start.getTime()));
        assertThat(timeData.getStopTime(), is(5999L));
        
        assertThat(query.hasAssetType(), is(false));
    }
    
    /**
     * Verify the remote request to retrieve Observations for all known assets with time constraint and max number.
     */
    @Test
    public void testRemoteRetrievalAllAssetsTimeConstraintAndNumber()
    {
        //date objs
        Date start = new Date(9000L);
        Date stop = new Date(11000L);
        
        //dates
        m_SUT.setRetrieveByMaxObsNum(true);
        m_SUT.setRetrieveDeleteByDate(true);
        m_SUT.setStartDate(start);
        m_SUT.setEndDate(stop);
        m_SUT.setMaxObservationNumber(4);
        
        //request for observations
        m_SUT.submitRetrieveObservationsRequest(null);
        
        //verify call to remote message sender for all asset observations
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        //should be two requests one for each asset type
        verify(m_MessageFactory).createObservationStoreMessage(
            eq(ObservationStoreMessageType.GetObservationRequest), messageCaptor.capture());
        verify(m_MessageWrapper).queue(eq(CONTROLLER_ID), eq((ResponseHandler)null));
        
        //verify data messages
        GetObservationRequestData requestData = (GetObservationRequestData)messageCaptor.getAllValues().get(0);
        Query query = requestData.getObsQuery();
        assertThat(query.hasMaxNumberOfObs(), is(true));
        assertThat(query.hasCreatedTimeRange(), is(true));
        TimeConstraintData timeData = query.getCreatedTimeRange();
        assertThat(timeData.getStartTime(), is(start.getTime()));
        assertThat(timeData.getStopTime(), is(11999L));
        
        assertThat(query.hasAssetType(), is(false));
        assertThat(query.getMaxNumberOfObs(), is(4));
    }
    
    /**
     * Verify the remote request to retrieve Observations for all known assets by number.
     */
    @Test
    public void testRemoteRetrievalAllAssetsByNumber()
    {
        //settings
        m_SUT.setRetrieveByMaxObsNum(true);
        m_SUT.setMaxObservationNumber(4);
        
        //request for observations
        m_SUT.submitRetrieveObservationsRequest(null);
        
        //verify call to remote message sender for all asset observations
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        //request
        verify(m_MessageFactory).createObservationStoreMessage(
            eq(ObservationStoreMessageType.GetObservationRequest), messageCaptor.capture());
        verify(m_MessageWrapper).queue(eq(CONTROLLER_ID), eq((ResponseHandler)null));
        
        //verify data messages
        GetObservationRequestData requestData = (GetObservationRequestData)messageCaptor.getAllValues().get(0);
        Query query = requestData.getObsQuery();
        assertThat(query.hasMaxNumberOfObs(), is(true));
        assertThat(query.hasCreatedTimeRange(), is(false));
        
        assertThat(query.hasAssetType(), is(false));
        assertThat(query.getMaxNumberOfObs(), is(4));
    }
    
    /**
     * Verify the remote request to retrieve Observations for all known assets and the obs number is not larger than 0.
     */
    @Test
    public void testRemoteRetrievalAllAssetsByNumberZero()
    {
        //settings
        m_SUT.setRetrieveByMaxObsNum(true);
        m_SUT.setMaxObservationNumber(0);
        
        //request for observations
        m_SUT.submitRetrieveObservationsRequest(null);
        
        //verify call to remote message sender for all asset observations
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        //request
        verify(m_MessageFactory).createObservationStoreMessage(
            eq(ObservationStoreMessageType.GetObservationRequest), messageCaptor.capture());
        verify(m_MessageWrapper).queue(eq(CONTROLLER_ID), eq((ResponseHandler)null));
        
        //verify data messages
        GetObservationRequestData requestData = (GetObservationRequestData)messageCaptor.getAllValues().get(0);
        Query query = requestData.getObsQuery();
        assertThat(query.hasMaxNumberOfObs(), is(false));
        assertThat(query.hasCreatedTimeRange(), is(false));
        
        assertThat(query.hasAssetType(), is(false));
    }
    
    /**
     * Verify the remote request to delete Observations for all known assets with time constraint.
     */
    @Test
    public void testRemoteDeletionAllAssetsTimeConstraint()
    {
        ObservationQuery query = mock(ObservationQuery.class);
        //mock obs store behavior
        when(m_ObservationStore.newQuery()).thenReturn(query);
        when(query.withAssetUuid(Mockito.any(UUID.class))).thenReturn(query);
        when(query.withTimeCreatedRange(Mockito.any(Date.class), Mockito.any(Date.class))).thenReturn(query);
        when(query.remove()).thenReturn(2000L);
        
        //date objs
        Date start = new Date(1000L);
        Date stop = new Date(5000L);
        
        //dates
        m_SUT.setRetrieveByMaxObsNum(false);
        m_SUT.setRetrieveDeleteByDate(true);
        m_SUT.setStartDate(start);
        m_SUT.setEndDate(stop);
        
        //request for observations
        m_SUT.submitDeleteObservationsRequest(null);
        
        //verify call to remote message sender for all asset observations
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        //request
        verify(m_MessageFactory).createObservationStoreMessage(
            eq(ObservationStoreMessageType.RemoveObservationRequest), messageCaptor.capture());
        verify(m_MessageWrapper).queue(eq(CONTROLLER_ID), eq((ResponseHandler)null));
        
        //verify data messages
        RemoveObservationRequestData requestData = (RemoveObservationRequestData)messageCaptor.getAllValues().get(0);
        Query queryData = requestData.getObsQuery();
        assertThat(queryData.hasCreatedTimeRange(), is(true));
        TimeConstraintData timeData = queryData.getCreatedTimeRange();
        assertThat(timeData.getStartTime(), is(start.getTime()));
        assertThat(timeData.getStopTime(), is(5999L));
        
        assertThat(queryData.hasAssetType(), is(false));
        
        //verify local remove call
        verify(query).remove();
    }
    
    /**
     * Verify the remote request to delete Observations for all known assets with time constraint and start time is 
     * null.
     */
    @Test
    public void testRemoteDeletionAllAssetsTimeConstraintNullStart()
    {
        ObservationQuery query = mock(ObservationQuery.class);
        //mock obs store behavior
        when(m_ObservationStore.newQuery()).thenReturn(query);
        when(query.withAssetUuid(Mockito.any(UUID.class))).thenReturn(query);
        when(query.withTimeCreatedRange(Mockito.any(Date.class), Mockito.any(Date.class))).thenReturn(query);
        when(query.remove()).thenReturn(2L);
        
        //date objs
        Date stop = new Date(5L);
        
        //dates
        m_SUT.setRetrieveByMaxObsNum(false);
        m_SUT.setRetrieveDeleteByDate(true);
        m_SUT.setStartDate(null);
        m_SUT.setEndDate(stop);
        
        //request for observations
        m_SUT.submitDeleteObservationsRequest(null);
        
        //verify call to remote message sender for all asset observations
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        //request
        verify(m_MessageFactory).createObservationStoreMessage(
            eq(ObservationStoreMessageType.RemoveObservationRequest), messageCaptor.capture());
        verify(m_MessageWrapper).queue(eq(CONTROLLER_ID), eq((ResponseHandler)null));
        
        //verify data messages
        RemoveObservationRequestData requestData = (RemoveObservationRequestData)messageCaptor.getAllValues().get(0);
        Query queryData = requestData.getObsQuery();
        assertThat(queryData.hasCreatedTimeRange(), is(false));
        
        assertThat(queryData.hasAssetType(), is(false));
        
        //verify local remove call
        verify(query).remove();
    }
    
    /**
     * Verify the remote request to delete Observations for all known assets.
     */
    @Test
    public void testRemoteDeleteAllAssets()
    {
        //obs store query
        ObservationQuery obsQuery = mock(ObservationQuery.class);
        when(m_ObservationStore.newQuery()).thenReturn(obsQuery);
        when(obsQuery.remove()).thenReturn(2L);
        
        //request to delete observations
        m_SUT.submitDeleteObservationsRequest(null);
        
        //verify call to remote message sender to delete observations
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        //request
        verify(m_MessageFactory).createObservationStoreMessage(
            eq(ObservationStoreMessageType.RemoveObservationRequest), messageCaptor.capture());
        verify(m_MessageWrapper).queue(eq(CONTROLLER_ID), eq((ResponseHandler)null));
        
        //verify data messages
        RemoveObservationRequestData requestData = (RemoveObservationRequestData)messageCaptor.getAllValues().get(0);
        Query query = requestData.getObsQuery();
        assertThat(query.hasCreatedTimeRange(), is(false));
        assertThat(query.hasAssetType(), is(false));
        
        //verify local remove call
        verify(obsQuery).remove();
    }
    
    /**
     * Verify the remote request to delete Observations for a known asset.
     */
    @Test
    public void testRemoteDeleteAnAsset()
    {
        //obs store query
        ObservationQuery obsQuery = mock(ObservationQuery.class);
        when(m_ObservationStore.newQuery()).thenReturn(obsQuery);
        when(obsQuery.remove()).thenReturn(2L);
        
        //asset UUID
        UUID asset2UUID = UUID.randomUUID();
        
        //asset model mock
        AssetModel asset2 = mock(AssetModel.class);
        when(asset2.getUuid()).thenReturn(asset2UUID);

        //request to delete observations
        m_SUT.submitDeleteObservationsRequest(asset2);
        
        //verify call to remote message sender to delete observations
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        //request
        verify(m_MessageFactory).createObservationStoreMessage(
            eq(ObservationStoreMessageType.RemoveObservationRequest), messageCaptor.capture());
        verify(m_MessageWrapper).queue(eq(CONTROLLER_ID), eq((ResponseHandler)null));
        
        RemoveObservationRequestData requestData = (RemoveObservationRequestData)messageCaptor.getValue();
        Query query = requestData.getObsQuery();
        assertThat(query.hasCreatedTimeRange(), is(false));
        assertThat(query.getAssetUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(asset2UUID)));
        
        //verify local remove call
        verify(obsQuery).withAssetUuid(asset2UUID);
        verify(obsQuery).remove();
    }
    
    /**
     * Verify the the validation of date objects for retrieve operations.
     */
    @Test
    public void testValidateDates()
    {
        // date objs, start time after end
        Date start = new Date(5L);
        Date stop = new Date(1L);

        // UIComponents
        ComponentSystemEvent event = mock(ComponentSystemEvent.class);
        UIComponent uiComp = mock(UIComponent.class);
        when(event.getComponent()).thenReturn(uiComp);
        UIInput uiInputStart = mock(UIInput.class);
        when(uiInputStart.getLocalValue()).thenReturn(start);
        when(uiInputStart.getClientId()).thenReturn("start");
        UIInput uiInputStop = mock(UIInput.class);
        when(uiInputStop.getLocalValue()).thenReturn(stop);
        when(uiInputStop.getClientId()).thenReturn("stop");

        // mock finding of start/stop inputs
        when(uiComp.findComponent("startDateRetrieve")).thenReturn(uiInputStart);
        when(uiComp.findComponent("endDateRetrieve")).thenReturn(uiInputStop);

        // mock the faces context
        FacesContext context = mock(FacesContext.class);
        when(m_FacesContextUtil.getFacesContext()).thenReturn(context);

        // validate
        m_SUT.validateDates(event);

        // verify growl messages
        verify(m_FacesContextUtil).getFacesContext();

        // stop message
        verify(context).addMessage(eq("start"), Mockito.any(FacesMessage.class));

        // verify validation failed
        verify(context).validationFailed();

        // verify rendering response
        verify(context).renderResponse();
    }
    
    /**
     * Verify successful validation of date objects which are equal.
     */
    @Test
    public void testValidateDatesEqualStartStop()
    {
        //date obj
        Date start = new Date(5L);
        Date stop = new Date(5L);
        
        //UIComponents
        ComponentSystemEvent event = mock(ComponentSystemEvent.class);
        UIComponent uiComp = mock(UIComponent.class);
        when(event.getComponent()).thenReturn(uiComp);
        UIInput uiInputStart = mock(UIInput.class);
        when(uiInputStart.getLocalValue()).thenReturn(start);
        when(uiInputStart.getClientId()).thenReturn("start");
        UIInput uiInputStop = mock(UIInput.class);
        when(uiInputStop.getLocalValue()).thenReturn(stop);
        when(uiInputStop.getClientId()).thenReturn("stop");
        
        //mock finding of start/stop inputs
        when(uiComp.findComponent("startDateRetrieve")).thenReturn(uiInputStart);
        when(uiComp.findComponent("endDateRetrieve")).thenReturn(uiInputStop);
        
        // mock the faces context
        FacesContext context = mock(FacesContext.class);
        when(m_FacesContextUtil.getFacesContext()).thenReturn(context);

        // validate
        m_SUT.validateDates(event);

        // verify validation failed methods are never called
        verify(m_FacesContextUtil, never()).getFacesContext();
        verify(context, never()).addMessage(eq("start"), Mockito.any(FacesMessage.class));
        verify(context, never()).validationFailed();
        verify(context, never()).renderResponse();
    }
    
    /**
     * Verify ignoring null start date.
     */
    @Test
    public void testValidateDatesNullStart()
    {
        //date obj
        Date stop = new Date(5L);
        
        //UIComponents
        ComponentSystemEvent event = mock(ComponentSystemEvent.class);
        UIComponent uiComp = mock(UIComponent.class);
        when(event.getComponent()).thenReturn(uiComp);
        UIInput uiInputStart = mock(UIInput.class);
        when(uiInputStart.getLocalValue()).thenReturn(null);
        when(uiInputStart.getClientId()).thenReturn("start");
        UIInput uiInputStop = mock(UIInput.class);
        when(uiInputStop.getLocalValue()).thenReturn(stop);
        when(uiInputStop.getClientId()).thenReturn("stop");
        
        //mock finding of start/stop inputs
        when(uiComp.findComponent("startDateRetrieve")).thenReturn(uiInputStart);
        when(uiComp.findComponent("endDateRetrieve")).thenReturn(uiInputStop);

        // validate
        m_SUT.validateDates(event);

        // verify growl messages
        verify(m_FacesContextUtil, never()).getFacesContext();
    }
    
    /**
     * Verify ignoring null stop date.
     */
    @Test
    public void testValidateDatesNullStop()
    {
        //date obj
        Date start = new Date(5L);
        
        //UIComponents
        ComponentSystemEvent event = mock(ComponentSystemEvent.class);
        UIComponent uiComp = mock(UIComponent.class);
        when(event.getComponent()).thenReturn(uiComp);
        UIInput uiInputStart = mock(UIInput.class);
        when(uiInputStart.getLocalValue()).thenReturn(start);
        when(uiInputStart.getClientId()).thenReturn("start");
        UIInput uiInputStop = mock(UIInput.class);
        when(uiInputStop.getLocalValue()).thenReturn(null);
        when(uiInputStop.getClientId()).thenReturn("stop");
        
        //mock finding of start/stop inputs
        when(uiComp.findComponent("startDateRetrieve")).thenReturn(uiInputStart);
        when(uiComp.findComponent("endDateRetrieve")).thenReturn(uiInputStop);

        // validate
        m_SUT.validateDates(event);

        // verify growl messages
        verify(m_FacesContextUtil, never()).getFacesContext();
    }
}
