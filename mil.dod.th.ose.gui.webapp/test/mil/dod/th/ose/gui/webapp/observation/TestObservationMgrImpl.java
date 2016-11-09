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
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.validator.ValidatorException;

import mil.dod.th.core.types.SensingModality;
import mil.dod.th.core.types.SensingModalityEnum;
import mil.dod.th.core.types.detection.TargetClassificationType;
import mil.dod.th.core.types.detection.TargetClassificationTypeEnum;
import mil.dod.th.core.types.observation.ObservationSubTypeEnum;
import mil.dod.th.core.observation.types.Detection;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.ObservationRef;
import mil.dod.th.core.observation.types.Relationship;
import mil.dod.th.core.observation.types.TargetClassification;
import mil.dod.th.core.persistence.ObservationQuery;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.ose.gui.webapp.asset.AssetDisplayHelper;
import mil.dod.th.ose.gui.webapp.asset.AssetModel;
import mil.dod.th.ose.gui.webapp.controller.ActiveController;
import mil.dod.th.ose.gui.webapp.controller.ControllerModel;
import mil.dod.th.ose.gui.webapp.controller.ObservationCountMgr;
import mil.dod.th.ose.gui.webapp.utils.FacesContextUtil;
import mil.dod.th.ose.shared.JdoDataStore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.primefaces.context.RequestContext;

/**
 * Tests for the ObservationMgrImpl class.
 * @author nickmarcucci
 *
 */
public class TestObservationMgrImpl
{
    private static final Integer CONTROLLER_ID = 1000;
    private ObservationMgrImpl m_SUT;
    private AssetDisplayHelper m_AssetDisplayHelper;
    private ObservationStore m_ObservationStore;
    private ActiveController m_ActiveController;
    private ObservationCountMgr m_ObservationCountManager;
    private FacesContextUtil m_FacesContextUtil;
    @SuppressWarnings("rawtypes")
    private JdoDataStore m_JdoStore;
    private ObservationQuery m_ObsQuery;
    private ObservationQuery m_ObsCount;
    
    @Before
    public void setUp()
    {
        m_SUT = new ObservationMgrImpl();
        
        //mock services
        m_AssetDisplayHelper = mock(AssetDisplayHelper.class);
        m_ObservationStore = mock(ObservationStore.class);
        m_ActiveController = mock(ActiveController.class);
        m_ObservationCountManager = mock(ObservationCountMgr.class);
        m_FacesContextUtil = mock(FacesContextUtil.class);
        m_JdoStore = mock(JdoDataStore.class);
        //mock query object
        m_ObsQuery = mock(ObservationQuery.class);
        m_ObsCount = mock(ObservationQuery.class);
        javax.jdo.Query jdoQuery = mock(javax.jdo.Query.class);
        
        //set services
        m_SUT.setAssetDisplay(m_AssetDisplayHelper);
        m_SUT.setObservationStore(m_ObservationStore);
        m_SUT.setFacesContextUtil(m_FacesContextUtil);
        m_SUT.setActiveController(m_ActiveController);
        m_SUT.setObservationCountMgr(m_ObservationCountManager);
        m_SUT.setJdoDataStore(m_JdoStore);
        
        //mock obs store behavior
        when(m_ObservationStore.newQuery()).thenReturn(m_ObsQuery, m_ObsCount);
        when(m_JdoStore.newJdoQuery()).thenReturn(jdoQuery, mock(javax.jdo.Query.class));

        ControllerModel controllerModel = mock(ControllerModel.class);
        when(controllerModel.getId()).thenReturn(CONTROLLER_ID);
        when(m_ActiveController.getActiveController()).thenReturn(controllerModel);
        
        //query actions
        when(m_ObsQuery.withRange(Mockito.anyInt(), Mockito.anyInt())).thenReturn(m_ObsQuery);
        when(m_ObsQuery.withAssetUuid(Mockito.any(UUID.class))).thenReturn(m_ObsQuery);
        when(m_ObsQuery.withAssetType(Mockito.anyString())).thenReturn(m_ObsQuery);
        when(m_ObsQuery.withTimeCreatedRange(Mockito.any(Date.class), Mockito.any(Date.class))).thenReturn(m_ObsQuery);
        
        when(m_ObsCount.withRange(Mockito.anyInt(), Mockito.anyInt())).thenReturn(m_ObsCount);
        when(m_ObsCount.withAssetUuid(Mockito.any(UUID.class))).thenReturn(m_ObsCount);
        when(m_ObsCount.withAssetType(Mockito.anyString())).thenReturn(m_ObsCount);
        when(m_ObsCount.withTimeCreatedRange(Mockito.any(Date.class), Mockito.any(Date.class))).thenReturn(m_ObsCount);
        
        m_SUT.postConstruct();
    }
    
    /**
     * Verify getters and setter for date objects.
     */
    @Test
    public void testGetSetDate()
    {
        //date objs
        Date start = new Date(1L);
        Date stop = new Date(5L);
        
        m_SUT.setStartDate(start);
        m_SUT.setEndDate(stop);
        
        assertThat(m_SUT.getStartDate(), is(start));
        assertThat(m_SUT.getEndDate(), is(stop));
    }
    
    /**
     * Verify the ability to get filter by date option.
     */
    @Test
    public void testIsFilterByDate()
    {
        //verify property is false by deflt
        assertThat(m_SUT.isFilterByDate(), is(false));
        
        //set it to a value
        m_SUT.setFilterByDate(true);
        
        //verify new value
        assertThat(m_SUT.isFilterByDate(), is(true));
    }

    /**
     * Verify that target classifications can be retrieved from a 
     * observation if it contains a detection
     */
    @Test
    public void testGetTargetClassifications()
    {
        Observation obs = mock(Observation.class);
        Detection detection = mock(Detection.class);
        
        TargetClassification tgt1 = mock(TargetClassification.class);
        TargetClassification tgt2 = mock(TargetClassification.class);
        
        TargetClassificationType t1Type = mock(TargetClassificationType.class);
        TargetClassificationType t2Type = mock(TargetClassificationType.class);
        
        when(tgt1.getType()).thenReturn(t1Type);
        when(tgt2.getType()).thenReturn(t2Type);
        
        when(t1Type.getValue()).thenReturn(TargetClassificationTypeEnum.ANIMAL);
        when(t2Type.getValue()).thenReturn(TargetClassificationTypeEnum.BLAST);
        
        List<TargetClassification> list = new ArrayList<TargetClassification>();
        list.add(tgt1);
        list.add(tgt2);
        
        when(detection.getTargetClassifications()).thenReturn(list);
        
        when(obs.getDetection()).thenReturn(detection);
        
        List<String> answer = m_SUT.getTargetClassifications(obs);
        
        assertThat(answer.size(), is(2));
        assertThat(answer.get(0), is(TargetClassificationTypeEnum.ANIMAL.toString().toLowerCase()));
        assertThat(answer.get(1), is(TargetClassificationTypeEnum.BLAST.toString().toLowerCase()));
    }
    
    /**
     * Verify that sensing modalities can be retrieved from a observation if 
     * it contains a detection
     */
    @Test
    public void testGetModalities()
    {
        Observation obs = mock(Observation.class);
        Detection detection = mock(Detection.class);
        
        SensingModality md1 = mock(SensingModality.class);
        SensingModality md2 = mock(SensingModality.class);
        
        when(md1.getValue()).thenReturn(SensingModalityEnum.MAGNETIC);
        when(md2.getValue()).thenReturn(SensingModalityEnum.PIR);
        
        List<SensingModality> modalities = new ArrayList<SensingModality>();
        modalities.add(md1);
        modalities.add(md2);
        
        when(obs.getModalities()).thenReturn(modalities);
        when(obs.getDetection()).thenReturn(detection);
        
        List<String> answer = m_SUT.getModalities(obs);
        
        assertThat(answer.size(), is(2));
        assertThat(answer.get(0), is(SensingModalityEnum.MAGNETIC.toString().toLowerCase()));
        assertThat(answer.get(1), is(SensingModalityEnum.PIR.toString().toLowerCase()));
    }
    
    /**
     * Verify observation count is cleared each time load() is called.
     */
    @Test
    public void testObservationCountCleared()
    {
        m_SUT.getObservations().load(0, 10, null, null, null);
        
        // count is cleared each time observations are displayed
        verify(m_ObservationCountManager).clearObsCount(CONTROLLER_ID);
    }
    
    /**
     * Verify observation dates are seeded in the post construct.
     */
    @Test
    public void testObservationPostConstruct()
    {
        final Calendar date = Calendar.getInstance();
        m_SUT.postConstruct();
        
        assertThat(m_SUT.getStartDate().getTime(), lessThan(date.getTime().getTime()));
        assertThat(m_SUT.getEndDate().getTime(), is(not(lessThan(date.getTime().getTime()))));
    }
    
    /**
     * Verify that getting observations for models that have no observations
     * will return an empty list when argument is null or a model.
     */
    @Test
    public void testGetDisplayObservationsNoObservations()
    {
        AssetModel model1 = mock(AssetModel.class);
        
        List<GuiObservation> listObs = m_SUT.getObservations().load(0, 10, null, null, null);
        
        assertThat(listObs.size(), is(0));
        
        when(m_AssetDisplayHelper.getSelectedFactoryObject()).thenReturn(model1);
        listObs = m_SUT.getObservations().load(0, 10, null, null, null);
        
        assertThat(listObs.size(), is(0));
    }
    
    /**
     * Verify that if an observation is not found still return an observation type to
     * be placed in the gui observation.
     */
    @Test
    public void testGetDisplayObservationsUnableToFindObservation()
    {
        AssetModel model1 = mock(AssetModel.class);
        UUID uuid = UUID.randomUUID();
        when(model1.getUuid()).thenReturn(uuid);
        
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        
        List<Observation> expectedObs = new ArrayList<Observation>();
        expectedObs.add(createMockedObservation(uuid1, 4000L, false, ObservationSubTypeEnum.DETECTION));
        expectedObs.add(createMockedObservation(uuid2, 2000L, true, ObservationSubTypeEnum.STATUS));
        
        when(m_ObsQuery.execute()).thenReturn(expectedObs);
        
        List<GuiObservation> listObs = m_SUT.getObservations().load(0, 10, null, null, null);
        assertThat(listObs.size(), is(2));
        assertThat(listObs.get(0).getObservation().getUuid(), is(uuid1));
        assertThat(listObs.get(0).getRelatedObservationModels().size(), is(1));
        assertThat(listObs.get(0).getRelatedObservationModels().get(0).getObservationSubType(), nullValue());
        assertThat(listObs.get(0).getRelatedObservationModels().get(0).isFoundInObsStore(), is(false));
        
        assertThat(listObs.get(1).getObservation().getUuid(), is(uuid2));
        assertThat(listObs.get(1).getRelatedObservationModels().size(), is(1));
        assertThat(listObs.get(1).getRelatedObservationModels().get(0).getObservationSubType(), 
                is(ObservationSubTypeEnum.STATUS));
        assertThat(listObs.get(1).getRelatedObservationModels().get(0).isFoundInObsStore(), is(true));
    }
    
    /**
     * Verify that getting observations with specific model returns the correct observations.
     */
    @Test
    public void testObservationWithSelectedObject()
    {
        AssetModel model1 = mock(AssetModel.class);
        UUID uuid = UUID.randomUUID();
        when(model1.getUuid()).thenReturn(uuid);
        when(m_AssetDisplayHelper.getSelectedFactoryObject()).thenReturn(model1);
        
        //Returns list of uuids corresponding to the four observations
        //made. Therefore, position 0 in the list corresponds to o1 in
        //that method.
        List<Observation> expectedObs = new ArrayList<Observation>();
        
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        UUID uuid4 = UUID.randomUUID();

        expectedObs.add(createMockedObservation(uuid1, 4000L, true, ObservationSubTypeEnum.DETECTION));
        expectedObs.add(createMockedObservation(uuid2, 3000L, true, ObservationSubTypeEnum.WEATHER));
        expectedObs.add(createMockedObservation(uuid3, 2000L, true, ObservationSubTypeEnum.STATUS));
        expectedObs.add(createMockedObservation(uuid4, 2000L, true, ObservationSubTypeEnum.BIOLOGICAL));
        
        when(m_ObsQuery.execute()).thenReturn(expectedObs);
        
        when(m_AssetDisplayHelper.getSelectedFactoryObject()).thenReturn(model1);
        List<GuiObservation> listObs = m_SUT.getObservations().load(0, 10, null, null, null);
        
        //Since 2 has a newer timestamp than 0, it should be listed first
        assertThat(listObs.size(), is(4));
        assertThat(listObs.get(0).getObservation().getUuid(), is(uuid1));
        assertThat(listObs.get(0).getRelatedObservationModels().size(), is(1));

        assertThat(listObs.get(0).getRelatedObservationModels().get(0).getObservationSubType(), 
                is(ObservationSubTypeEnum.DETECTION));
        assertThat(listObs.get(1).getObservation().getUuid(), is(uuid2));
        assertThat(listObs.get(1).getRelatedObservationModels().size(), is(1));
        assertThat(listObs.get(1).getRelatedObservationModels().get(0).getObservationSubType(), 
                is(ObservationSubTypeEnum.WEATHER));
        assertThat(listObs.get(2).getObservation().getUuid(), is(uuid3));
        assertThat(listObs.get(2).getRelatedObservationModels().size(), is(1));
        assertThat(listObs.get(2).getRelatedObservationModels().get(0).getObservationSubType(), 
                is(ObservationSubTypeEnum.STATUS));
        assertThat(listObs.get(3).getObservation().getUuid(), is(uuid4));
        assertThat(listObs.get(3).getRelatedObservationModels().size(), is(1));
        assertThat(listObs.get(3).getRelatedObservationModels().get(0).getObservationSubType(), 
                is(ObservationSubTypeEnum.BIOLOGICAL));
        
        verify(m_ObsQuery).withAssetUuid(uuid);
        verify(m_ObsQuery).withRange(0, 10);
        verify(m_ObsQuery).execute();
        verify(m_ObsCount).getCount();
    }
    
    /**
     * Verify that getting observations with null input returns 
     * all observations
     */
    @Test
    public void testObservationWithNoSelectedObject()
    {
        List<Observation> expectedObs = new ArrayList<Observation>();
        
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        UUID uuid4 = UUID.randomUUID();
        UUID uuid5 = UUID.randomUUID();
        UUID uuid6 = UUID.randomUUID();
        
        expectedObs.add(createMockedObservation(uuid1, 1000L, true, ObservationSubTypeEnum.DETECTION));
        expectedObs.add(createMockedObservation(uuid2, 2000L, true, ObservationSubTypeEnum.VIDEO_METADATA));
        expectedObs.add(createMockedObservation(uuid3, 3000L, true, ObservationSubTypeEnum.WATER_QUALITY));
        expectedObs.add(createMockedObservation(uuid4, 4000L, true, ObservationSubTypeEnum.AUDIO_METADATA));
        expectedObs.add(createMockedObservation(uuid5, 5000L, true, ObservationSubTypeEnum.CHEMICAL));
        expectedObs.add(createMockedObservation(uuid6, 6000L, true, ObservationSubTypeEnum.POWER));
        
        when(m_ObsQuery.execute()).thenReturn(expectedObs);
        
        List<GuiObservation> listObs = m_SUT.getObservations().load(0, 10, null, null, null);
        
        assertThat(listObs.size(), is(6));
        
        assertThat(listObs.get(0).getObservation().getUuid(), is(expectedObs.get(0).getUuid()));
        assertThat(listObs.get(1).getObservation().getUuid(), is(expectedObs.get(1).getUuid()));
        assertThat(listObs.get(2).getObservation().getUuid(), is(expectedObs.get(2).getUuid()));
        assertThat(listObs.get(3).getObservation().getUuid(), is(expectedObs.get(3).getUuid()));
        assertThat(listObs.get(4).getObservation().getUuid(), is(expectedObs.get(4).getUuid()));
        assertThat(listObs.get(5).getObservation().getUuid(), is(expectedObs.get(5).getUuid()));
        
        verify(m_ObsQuery, never()).withAssetUuid(Mockito.any(UUID.class));
        
        verify(m_ObsCount).getCount();
        verify(m_ObsQuery).execute();
    }
    
    /**
     * Verify that only the appropriate observations are loaded based on the page settings
     */
    @Test
    public void testObservationLazyLoading()
    {
        List<Observation> obs = new ArrayList<>();
        for (int i = 0; i < 20; i++)
        {
            obs.add(createMockedObservation(UUID.randomUUID(), (long)i, true, ObservationSubTypeEnum.DETECTION));
        }
        
        ObservationQuery query = mock(ObservationQuery.class);
        when(m_ObservationStore.newQuery()).thenReturn(query);
        
        when(query.withRange(Mockito.anyInt(), Mockito.anyInt())).thenReturn(query);
        when(query.execute()).thenReturn(obs.subList(16, 20));

        // simple, start at 0
        List<GuiObservation> listObs = m_SUT.getObservations().load(0, 4, null, null, null);
        assertThat(listObs.size(), is(4));
        assertThat(listObs.get(3).getObservation().getUuid(), is(obs.get(19).getUuid()));
        assertThat(listObs.get(2).getObservation().getUuid(), is(obs.get(18).getUuid()));
        assertThat(listObs.get(1).getObservation().getUuid(), is(obs.get(17).getUuid()));
        assertThat(listObs.get(0).getObservation().getUuid(), is(obs.get(16).getUuid()));
        
        // start at the 3rd newest (0 based)
        when(query.execute()).thenReturn(obs.subList(14, 17));
        listObs = m_SUT.getObservations().load(3, 3, null, null, null);
        assertThat(listObs.size(), is(3));
        assertThat(listObs.get(2).getObservation().getUuid(), is(obs.get(16).getUuid()));
        assertThat(listObs.get(1).getObservation().getUuid(), is(obs.get(15).getUuid()));
        assertThat(listObs.get(0).getObservation().getUuid(), is(obs.get(14).getUuid()));
        
        // start at the 15th newest with 10 per page, but there is only 20 total
        when(query.execute()).thenReturn(obs.subList(0, 5));
        listObs = m_SUT.getObservations().load(15, 10, null, null, null);
        assertThat(listObs.size(), is(5));
        assertThat(listObs.get(4).getObservation().getUuid(), is(obs.get(4).getUuid()));
        assertThat(listObs.get(3).getObservation().getUuid(), is(obs.get(3).getUuid()));
        assertThat(listObs.get(2).getObservation().getUuid(), is(obs.get(2).getUuid()));
        assertThat(listObs.get(1).getObservation().getUuid(), is(obs.get(1).getUuid()));
        assertThat(listObs.get(0).getObservation().getUuid(), is(obs.get(0).getUuid()));
    }
    
    /**
     * Verify the the validation of date objects for filter operations.
     */
    @Test
    public void testValidateDatesFilter()
    {
        m_SUT.setFilterByDate(true);
        
        //date objs, start time after end
        Date start = new Date(5L);
        Date stop = new Date(1L);
        
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
        when(uiComp.findComponent("startDateFilter")).thenReturn(uiInputStart);
        when(uiComp.findComponent("endDateFilter")).thenReturn(uiInputStop);
        
        //mock the faces context
        FacesContext context = mock(FacesContext.class);
        when(m_FacesContextUtil.getFacesContext()).thenReturn(context);
        
        //validate
        m_SUT.validateDates(event);
        
        //verify growl messages
        verify(m_FacesContextUtil).getFacesContext();

        //stop message
        verify(context).addMessage(eq("start"), Mockito.any(FacesMessage.class));
        
        //verify validation failed
        verify(context).validationFailed();
        
        //verify rendering response
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
        when(uiComp.findComponent("startDateFilter")).thenReturn(uiInputStart);
        when(uiComp.findComponent("endDateFilter")).thenReturn(uiInputStop);
        
        // mock the faces context
        FacesContext context = mock(FacesContext.class);
        when(m_FacesContextUtil.getFacesContext()).thenReturn(context);

        // validate
        m_SUT.validateDates(event);

        // verify validation failed methods are never called.
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
        when(uiComp.findComponent("startDateFilter")).thenReturn(uiInputStart);
        when(uiComp.findComponent("endDateFilter")).thenReturn(uiInputStop);

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
        when(uiComp.findComponent("startDateFilter")).thenReturn(uiInputStart);
        when(uiComp.findComponent("endDateFilter")).thenReturn(uiInputStop);

        // validate
        m_SUT.validateDates(event);

        // verify growl messages
        verify(m_FacesContextUtil, never()).getFacesContext();
    }
    
    /**
     * Verify the filtering of Observations via a filter string.
     */
    @Test
    public void testFilterByExpression()
    {
        //settings
        m_SUT.setFilterByExpression(true);
        m_SUT.setFilter("filter");
        
        javax.jdo.Query queryObs = mock(javax.jdo.Query.class);
        javax.jdo.Query queryCount = mock(javax.jdo.Query.class);
        when(m_JdoStore.newJdoQuery()).thenReturn(queryObs, queryCount);
        
        //request for observations
        m_SUT.getObservations().load(0, 10, null, null, null);
        
        //verify call to jdo store
        verify(m_JdoStore, times(2)).newJdoQuery();
        //verify filter set
        verify(queryObs).setFilter("filter");
        verify(queryCount).setFilter("filter");
        verify(queryObs).setOrdering("createdTimestamp descending");
        verify(queryCount, never()).setOrdering("createdTimestamp descending");
        verify(m_JdoStore).executeJdoQuery(queryObs);
        verify(m_JdoStore).executeGetCount(queryCount);
    }
    
    /**
     * Verify filtering of Observations via a filter string and date if the start date is null.
     */
    @Test
    public void testFilterByExpressionAndDateNullStartDate()
    {
        //settings
        m_SUT.setFilterByExpression(true);
        m_SUT.setFilterByDate(true);
        m_SUT.setFilter("filter");
        m_SUT.setStartDate(null);
        m_SUT.setEndDate(new Date(7L));

        //request for observations
        List<GuiObservation> list = m_SUT.getObservations().load(0, 10, null, null, null);
        
        assertThat(list.size(), is(0));
        
        //verify call to jdo store
        verify(m_JdoStore, never()).executeJdoQuery(Mockito.any(javax.jdo.Query.class));
        verify(m_JdoStore, never()).executeGetCount(Mockito.any(javax.jdo.Query.class));
    }
    
    /**
     * Verify NO remote request to retrieve Observations via a filter string and date if the stop date is null.
     */
    @Test
    public void testFilterByExpressionAndDateNullStopDate()
    {
        //settings
        m_SUT.setFilterByExpression(true);
        m_SUT.setFilterByDate(true);
        m_SUT.setFilter("filter");
        m_SUT.setStartDate(new Date(7L));
        m_SUT.setEndDate(null);

        //request for observations
        List<GuiObservation> list = m_SUT.getObservations().load(0, 10, null, null, null);
        
        assertThat(list.size(), is(0));
        
        //verify call to jdo store
        verify(m_JdoStore, never()).executeJdoQuery(Mockito.any(javax.jdo.Query.class));
        verify(m_JdoStore, never()).executeGetCount(Mockito.any(javax.jdo.Query.class));
    }
    
    /**
     * Verify the remote request to retrieve Observations via a filter string and by Date.
     */
    @Test
    public void testFilterByExpressionWithDate()
    {
        //settings
        m_SUT.setFilterByExpression(true);
        m_SUT.setFilterByDate(true);
        m_SUT.setFilter("filter");
        m_SUT.setStartDate(new Date(9000L));
        m_SUT.setEndDate(new Date(11000L));
        
        javax.jdo.Query queryObs = mock(javax.jdo.Query.class);
        javax.jdo.Query queryCount = mock(javax.jdo.Query.class);
        when(m_JdoStore.newJdoQuery()).thenReturn(queryObs, queryCount);
        
        //request for observations
        m_SUT.getObservations().load(0, 10, null, null, null);
        
        //verify call to jdo store
        verify(m_JdoStore, times(2)).newJdoQuery();
        verify(queryObs).setFilter("filter && createdTimestamp >= 9000 && createdTimestamp <= 11999");
        verify(queryCount).setFilter("filter && createdTimestamp >= 9000 && createdTimestamp <= 11999");
        verify(m_JdoStore).executeJdoQuery(queryObs);
        verify(m_JdoStore).executeGetCount(queryCount);
    }
    
    /**
     * Verify the remote request to retrieve Observations via a filter string and by selected asset.
     */
    @Test
    public void testFilterByExpressionWithSelectedAsset()
    {
        AssetModel model1 = mock(AssetModel.class);
        UUID uuid = UUID.randomUUID();
        when(model1.getUuid()).thenReturn(uuid);
        when(m_AssetDisplayHelper.getSelectedFactoryObject()).thenReturn(model1);
        
        //settings
        m_SUT.setFilterByExpression(true);
        m_SUT.setFilter("filter");
        
        javax.jdo.Query query = mock(javax.jdo.Query.class);
        when(m_JdoStore.newJdoQuery()).thenReturn(query);
        
        //request for observations
        m_SUT.getObservations().load(0, 10, null, null, null);
        
        //verify calls to jdo store
        verify(m_JdoStore, times(2)).newJdoQuery();
        verify(query, times(2)).setFilter("filter " + "&& assetUuid == '"+ uuid.toString() + "'");
        verify(m_JdoStore).executeJdoQuery(query);
    }
    
    /**
     * Verify the remote request to retrieve Observations via a filter string and by Date and by selected asset.
     */
    @Test
    public void testFilterByExpressionWithDateSelectedAsset()
    {
        AssetModel model1 = mock(AssetModel.class);
        UUID uuid = UUID.randomUUID();
        when(model1.getUuid()).thenReturn(uuid);
        when(m_AssetDisplayHelper.getSelectedFactoryObject()).thenReturn(model1);
        
        //settings
        m_SUT.setFilterByExpression(true);
        m_SUT.setFilterByDate(true);
        m_SUT.setFilter("filter");
        m_SUT.setStartDate(new Date(9000L));
        m_SUT.setEndDate(new Date(11000L));
        
        javax.jdo.Query query = mock(javax.jdo.Query.class);
        when(m_JdoStore.newJdoQuery()).thenReturn(query);
        
        //request for observations
        m_SUT.getObservations().load(0, 10, null, null, null);
        
        //verify call to jdo store
        verify(m_JdoStore, times(2)).newJdoQuery();
        verify(query, times(2)).setFilter("filter " + "&& assetUuid == '" 
            + uuid.toString() + "' && createdTimestamp >= 9000 && createdTimestamp <= 11999");
        verify(m_JdoStore).executeJdoQuery(query);
    }
    
    /**
     * Verify filter of Observations by date, null start date returns no obs.
     */
    @Test
    public void testFilteringObservationsByDateNullStart()
    {
        //date objs
        Date stop = new Date(5L);
        
        //dates
        m_SUT.setFilterByDate(true);
        m_SUT.setStartDate(null);
        m_SUT.setEndDate(stop);
        
        javax.jdo.Query queryObs = mock(javax.jdo.Query.class);
        javax.jdo.Query queryCount = mock(javax.jdo.Query.class);
        when(m_JdoStore.newJdoQuery()).thenReturn(queryObs, queryCount);
        
        //request for observations
        List<GuiObservation> list = m_SUT.getObservations().load(0, 10, null, null, null);
        
        assertThat(list.size(), is(0));
        
        //verify no calls are made
        verify(m_JdoStore, never()).executeJdoQuery(queryObs);
        verify(m_JdoStore, never()).executeGetCount(queryCount);
    }
    
    /**
     * Verify filter of Observations by date, null stop date returns no obs.
     */
    @Test
    public void testFilteringObservationsByDateNullStop()
    {
        //date objs
        Date start = new Date(5L);
        
        //dates
        m_SUT.setFilterByDate(true);
        m_SUT.setStartDate(start);
        m_SUT.setEndDate(null);
        
        javax.jdo.Query queryObs = mock(javax.jdo.Query.class);
        javax.jdo.Query queryCount = mock(javax.jdo.Query.class);
        when(m_JdoStore.newJdoQuery()).thenReturn(queryObs, queryCount);
        
        //request for observations
        List<GuiObservation> list = m_SUT.getObservations().load(0, 10, null, null, null);
        
        assertThat(list.size(), is(0));
        
        //verify no calls are made
        verify(m_JdoStore, never()).executeJdoQuery(queryObs);
        verify(m_JdoStore, never()).executeJdoQuery(queryCount);
    }
    
    /**
     * Verify filtered retrieval of Observations by date.
     */
    @Test
    public void testFilteringObservationsByDateAllAssets()
    {
        //date objs
        Date start = new Date(1L);
        Date stop = new Date(5L);
        
        //dates
        m_SUT.setFilterByDate(true);
        m_SUT.setStartDate(start);
        m_SUT.setEndDate(stop);
        
        //request for observations
        m_SUT.getObservations().load(0, 10, null, null, null);
        
        //verify two calls are made to the observation store, one for getting the observations
        //in the range and one for getting the total count of observations.
        verify(m_ObservationStore, times(2)).newQuery();
        verify(m_ObsQuery).execute();
        verify(m_ObsCount).getCount();
    }
    
    /**
     * Verify request for obs when start date is after end date, does not blow up.
     */
    @Test
    public void testFilteringObservationsByBadDateAllAssets()
    {
        //date objs
        Date start = new Date(8L);
        Date stop = new Date(5L);
        
        //dates
        m_SUT.setFilterByDate(true);
        m_SUT.setStartDate(start);
        m_SUT.setEndDate(stop);
        
        //request for observations
        m_SUT.getObservations().load(0, 10, null, null, null);
        
        //verify two calls are made to the observation store, one for the query and one for the count
        verify(m_ObservationStore, times(2)).newQuery();
        verify(m_ObsQuery, never()).execute();
        verify(m_ObsCount, never()).getCount();
    }
    
    /**
     * Verify the remote request to retrieve Observations for all known assets because the filter string is null.
     */
    @Test
    public void testFilterByExpressionNullFilter()
    {
        //settings
        m_SUT.setFilterByExpression(true);
        m_SUT.setFilter(null);
        
        //request for observations
        m_SUT.getObservations().load(0, 10, null, null, null);
        
        //verify no calls
        verify(m_ObsQuery, never()).withAssetUuid(Mockito.any(UUID.class));
        verify(m_ObsQuery).execute();
        //verify jdo store NOT call
        verify(m_JdoStore, never()).newJdoQuery();
        verify(m_JdoStore, never()).executeJdoQuery(Mockito.any(javax.jdo.Query.class));
        verify(m_JdoStore, never()).executeGetCount(Mockito.any(javax.jdo.Query.class));
    }
    
    /**
     * Verify the remote request to retrieve Observations for all known assets because the filter string is empty.
     */
    @Test
    public void testFilterByExpressionEmptyFilter()
    {
        //settings
        m_SUT.setFilterByExpression(true);
        m_SUT.setFilter("");
        
        //request for observations
        m_SUT.getObservations().load(0, 10, null, null, null);
        
        //verify calls
        verify(m_ObsQuery, never()).withAssetUuid(Mockito.any(UUID.class));
        verify(m_ObsQuery).execute();
        //verify jdo store NOT call
        verify(m_JdoStore, never()).newJdoQuery();
        verify(m_JdoStore, never()).executeJdoQuery(Mockito.any(javax.jdo.Query.class));
        verify(m_JdoStore, never()).executeGetCount(Mockito.any(javax.jdo.Query.class));
    }
    
    /**
     * Verify that if the faces context says that items are not valid on the page that there
     * is not a call to update the obs table.
     */
    @Test
    public void testHandleManualFilterRequestValidationError()
    {
        FacesContext context = mock(FacesContext.class);
        when(m_FacesContextUtil.getFacesContext()).thenReturn(context);
        when(context.isValidationFailed()).thenReturn(true);
        
        m_SUT.handleManualFilterRequest();
        
        verify(m_FacesContextUtil, never()).getRequestContext();
    }
    
    /**
     * Verify that if the faces context says that all items are valid on the page, that there
     * is a call to update the obs table.
     */
    @Test
    public void testHandleManualFilterRequest()
    {
        FacesContext context = mock(FacesContext.class);
        when(m_FacesContextUtil.getFacesContext()).thenReturn(context);
        when(context.isValidationFailed()).thenReturn(false);
        RequestContext reContext = mock(RequestContext.class);
        when(m_FacesContextUtil.getRequestContext()).thenReturn(reContext);
        
        m_SUT.handleManualFilterRequest();
        
        verify(m_FacesContextUtil).getRequestContext();
        verify(reContext).update(":obsTable");
    }
    
    /**
     * Verify that submitting a filter string presents the the query to the obs store for validation. 
     */
    @Test
    public void testCheckFilter()
    {
        //mocks
        FacesContext context = mock(FacesContext.class);
        UIComponent component = mock(UIComponent.class);
        
        //settings
        String filter = "filter";
        m_SUT.setFilterByExpression(true);
        m_SUT.setFilter(filter);
        
        //mock jdo store query
        javax.jdo.Query jdoQuery = mock(javax.jdo.Query.class);
        when(m_JdoStore.newJdoQuery()).thenReturn(jdoQuery);
        
        //request check
        m_SUT.checkFilter(context, component, filter);
        
        //verify call to jdo store
        verify(m_JdoStore).newJdoQuery();
        verify(jdoQuery).compile();
    }
    
    /**
     * Verify validation exception if filter expression is invalid.
     */
    @Test
    public void testCheckFilterException()
    {
        javax.jdo.Query jdoQuery = mock(javax.jdo.Query.class);
        when(m_JdoStore.newJdoQuery()).thenReturn(jdoQuery);
        
        //mocks
        FacesContext context = mock(FacesContext.class);
        UIComponent component = mock(UIComponent.class);
        
        //settings
        String filter = "filter";
        m_SUT.setFilterByExpression(true);
        m_SUT.setFilter(filter);

        doThrow(new IllegalArgumentException("e")).when(jdoQuery).compile();
        
        //request check
        try
        {
            m_SUT.checkFilter(context, component, filter);
            fail("Expected exception from jdo object query.");
        }
        catch (ValidatorException e)
        {
            //expected
        }
    }
    
    /**
     * Verify that the get observation method returns the appropriate gui observation.
     */
    @Test
    public void testGetObservation()
    {
        UUID obsUuid = UUID.randomUUID();
        Observation mockedObs = createMockedObservation(obsUuid, 1000, true, ObservationSubTypeEnum.CBRNE_TRIGGER, 
                ObservationSubTypeEnum.STATUS);

        when(m_ObservationStore.find(obsUuid)).thenReturn(mockedObs);
        
        GuiObservation guiObs = m_SUT.getObservation(obsUuid);
        assertThat(guiObs.getObservation().getUuid(), is(obsUuid));
        assertThat(guiObs.getObservation().getCreatedTimestamp(), is(1000L));
        assertThat(guiObs.getRelatedObservationModels().get(0)
            .getObservationSubType(), is(ObservationSubTypeEnum.CBRNE_TRIGGER));
        assertThat(guiObs.getRelatedObservationModels().get(1).getObservationSubType(), 
                is(ObservationSubTypeEnum.STATUS));
    }
    
    /**
     * Verify that null is returned when trying to retrieve an observation that does not exist.
     */
    @Test
    public void testGetNonexistentObservation()
    {
        UUID obsUuid = UUID.randomUUID();
        
        when(m_ObservationStore.find(obsUuid)).thenReturn(null);
        
        assertThat(m_SUT.getObservation(obsUuid), is(nullValue()));
    }
    
    /**
     * Function creates a mocked observation which has the given uuid, timestamp, and related observations which 
     * are of the observation type specified.
     * The number of related observations depends on the number of observation types passed in. 
     * @param uuid
     *  the uuid of the observation that is to be created
     * @param timestamp
     *  the timestamp of the observation to be created
     * @param canFindRefObs
     *  if the referenced observation can be found in the observation store
     * @param refTypes
     *  the observation types of the observation references that the created observation is to contain
     * @return
     *  the mock observation with the given data
     */
    private Observation createMockedObservation(UUID uuid, long timestamp, final boolean canFindRefObs, 
            ObservationSubTypeEnum ... refTypes)
    {
        Observation observation = mock(Observation.class);
        
        when(observation.getCreatedTimestamp()).thenReturn(timestamp);
        when(observation.getUuid()).thenReturn(uuid);
        
        List<ObservationRef> obsRefs = new ArrayList<>();
        
        for (ObservationSubTypeEnum type : refTypes)
        {
            ObservationRef ref = mock(ObservationRef.class);
            
            UUID refUuid = UUID.randomUUID();
            when(ref.getUuid()).thenReturn(refUuid);
            
            Observation refObservation = mock (Observation.class);
            
            switch (type)
            {
                case DETECTION:
                    when(refObservation.isSetDetection()).thenReturn(true);
                    break;
                case STATUS:
                    when(refObservation.isSetStatus()).thenReturn(true);
                    break;
                case WEATHER:
                    when(refObservation.isSetWeather()).thenReturn(true);
                    break;
                case AUDIO_METADATA:
                    when(refObservation.isSetDigitalMedia()).thenReturn(true);
                    when(refObservation.isSetAudioMetadata()).thenReturn(true);
                    break;
                case IMAGE_METADATA:
                    when(refObservation.isSetDigitalMedia()).thenReturn(true);
                    when(refObservation.isSetImageMetadata()).thenReturn(true);
                    break;
                case VIDEO_METADATA:
                    when(refObservation.isSetDigitalMedia()).thenReturn(true);
                    when(refObservation.isSetVideoMetadata()).thenReturn(true);
                    break;
                case BIOLOGICAL:
                    when(refObservation.isSetBiological()).thenReturn(true);
                    break;
                case CBRNE_TRIGGER:
                    when(refObservation.isSetCbrneTrigger()).thenReturn(true);
                    break;
                case CHEMICAL:
                    when(refObservation.isSetChemical()).thenReturn(true);
                    break;
                case WATER_QUALITY:
                    when(refObservation.isSetWaterQuality()).thenReturn(true);
                    break;
                case POWER:
                    when(refObservation.isSetPower()).thenReturn(true);
                    break;
                default:
                    //do nothing other is desired type
                    break;
            }
            
            if (canFindRefObs)
            {
                when(m_ObservationStore.find(refUuid)).thenReturn(refObservation);
            }
            else
            {
                when(ref.getRelationship()).thenReturn(mock(Relationship.class));
                when(m_ObservationStore.find(refUuid)).thenReturn(null);
            }
            
            obsRefs.add(ref);
        }
        
        when(observation.getRelatedObservations()).thenReturn(obsRefs);
        
        return observation;
    }
}
