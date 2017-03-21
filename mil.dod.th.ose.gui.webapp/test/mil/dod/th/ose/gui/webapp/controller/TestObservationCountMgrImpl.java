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
package mil.dod.th.ose.gui.webapp.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field; // NOCHECKSTYLE: TD: illegal package, new warning, old code
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.webapp.asset.AssetMgrImpl;
import mil.dod.th.ose.gui.webapp.controller.ObservationCountMgrImpl.ControllerEventHandler;
import mil.dod.th.ose.gui.webapp.controller.ObservationCountMgrImpl.ObservationEventHandler;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

/**
 * Test class for the {@link ObservationCountMgrImpl} class.
 * 
 * @author cweisenborn
 */
public class TestObservationCountMgrImpl
{
    private static int CONTROLLER_ID = 5;
    
    private ObservationCountMgrImpl m_SUT;
    
    private EventAdmin m_EventAdmin;
    
    private ObservationStore m_ObsStore;
    
    private AssetMgrImpl m_AssetMgr;

    private BundleContextUtil m_BundleUtil;
    
    private ObservationEventHandler m_ObsEventHandler;
    
    private ControllerEventHandler m_ControllerEventHandler;
    
    private ServiceRegistration<?> m_Registration;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setup()
    {
        m_EventAdmin = mock(EventAdmin.class);
        m_ObsStore = mock(ObservationStore.class);
        m_AssetMgr = new AssetMgrImpl();
        m_BundleUtil = mock(BundleContextUtil.class);
        BundleContext bundleContext = mock(BundleContext.class);
        m_Registration = mock(ServiceRegistration.class);
        
        m_SUT = new ObservationCountMgrImpl();
        
        m_SUT.setBundleContextUtil(m_BundleUtil);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setObservationStore(m_ObsStore);
        m_SUT.setAssetMgr(m_AssetMgr);
        
        //mock behavior for event listener
        when(m_BundleUtil.getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
                Mockito.any(Dictionary.class))).thenReturn(m_Registration);
        
        //create and register event handlers.
        m_SUT.postConstruct();
        
        //verify
        ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(bundleContext, times(2)).registerService(eq(EventHandler.class), captor.capture(), 
            Mockito.any(Dictionary.class));
        verify(m_BundleUtil, times(2)).getBundleContext();
        
        m_ObsEventHandler = (ObservationEventHandler)captor.getAllValues().get(0);
        m_ControllerEventHandler = (ControllerEventHandler)captor.getAllValues().get(1);
    }
    
    /**
     * Test pre destroy method.
     * Verify the unregister method is called for all event handlers.
     */
    @Test
    public void testPreDestroy()
    {
        m_SUT.preDestroy();
        
        //Verify that unregister is called twice. Once for each event handler.
        verify(m_Registration, times(2)).unregister();
    }
    
    /**
     * Test retrieving the observation count for a controller.
     * Verify the observation count returned is correct.
     */
    @Test
    public void testGetObservationCount()
    {
        //Test initial retrieval for unknown controller adds controller and returns zero for obs count.
        assertThat(m_SUT.getObservationCount(CONTROLLER_ID), is(0));
        
        Event persistEvent = mockObsPersisted();
        m_ObsEventHandler.handleEvent(persistEvent);
        
        //Verify correct obs count is returned.
        assertThat(m_SUT.getObservationCount(CONTROLLER_ID), is(1));
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event pushEvent = eventCaptor.getValue();
        assertThat(pushEvent, notNullValue());
        
        assertThat(pushEvent.getTopic(), is(ObservationCountMgr.TOPIC_OBSERVATION_COUNT_UPDATED));
        assertThat((int)pushEvent.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID), is(CONTROLLER_ID));
        assertThat((int)pushEvent.getProperty(ObservationCountMgr.EVENT_PROP_OBS_COUNT), is(1));
    }
    
    /**
     * Test the method used to increment the observation count for a controller by one.
     * Verify that the observation count is incremented and that an obs count updated event is posted.
     */
    @Test
    public void testIncrementObsCount()
    {
        //Test initial increment for unknown controller adds the controller and sets obs count to one.
        Event persistEvent = mockObsPersisted();
        m_ObsEventHandler.handleEvent(persistEvent);
        assertThat(m_SUT.getObservationCount(CONTROLLER_ID), is(1));
        
        //Test incrementing the observation count for a known controller by one.
        m_ObsEventHandler.handleEvent(persistEvent);
        assertThat(m_SUT.getObservationCount(CONTROLLER_ID), is(2));
        
        //Verify that two observation count updated events were posted.
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        Event firstPush = eventCaptor.getAllValues().get(0);
        Event secondPush = eventCaptor.getAllValues().get(1);
        assertThat(firstPush.getTopic(), is(ObservationCountMgr.TOPIC_OBSERVATION_COUNT_UPDATED));
        assertThat((int)firstPush.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID), is(CONTROLLER_ID));
        assertThat((int)firstPush.getProperty(ObservationCountMgr.EVENT_PROP_OBS_COUNT), is(1));
       
        assertThat(secondPush.getTopic(), is(ObservationCountMgr.TOPIC_OBSERVATION_COUNT_UPDATED));
        assertThat((int)secondPush.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID), is(CONTROLLER_ID));
        assertThat((int)secondPush.getProperty(ObservationCountMgr.EVENT_PROP_OBS_COUNT), is(2));
    }
    
    /**
     * Test the method used to clear the observation count for a controller.
     * Verify that the observation count for a controller is set to zero once the clear method is called.
     */
    @Test
    public void testClearObsCount()
    {
        //Test calling clear obs count for unknown controller. Should not post an event.
        m_SUT.clearObsCount(5);
        
        //Increment obs count for controller.
        Event persistEvent = mockObsPersisted();
        m_ObsEventHandler.handleEvent(persistEvent);
        assertThat(m_SUT.getObservationCount(CONTROLLER_ID), is(1));
        
        //Clear obs count for controller and verify that it is zero.
        m_SUT.clearObsCount(CONTROLLER_ID);
        assertThat(m_SUT.getObservationCount(CONTROLLER_ID), is(0));
        
        //Verify that two observation count updated events were posted. One for incrementing and one for clearing.
        //Should not be three since the first clear obs count call should have done nothing.
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        Event firstPush = eventCaptor.getAllValues().get(0);
        Event secondPush = eventCaptor.getAllValues().get(1);
        assertThat(firstPush.getTopic(), is(ObservationCountMgr.TOPIC_OBSERVATION_COUNT_UPDATED));
        assertThat((int)firstPush.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID), is(CONTROLLER_ID));
        assertThat((int)firstPush.getProperty(ObservationCountMgr.EVENT_PROP_OBS_COUNT), is(1));
        
        assertThat(secondPush.getTopic(), is(ObservationCountMgr.TOPIC_OBSERVATION_COUNT_UPDATED));
        assertThat((int)secondPush.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID), is(CONTROLLER_ID));
        assertThat((int)secondPush.getProperty(ObservationCountMgr.EVENT_PROP_OBS_COUNT), is(0));
    }
    
    /**
     * Test the observation event handlers handle event method.
     * Verify that the specified controllers observation count is incremented by one and an obs count updated event
     * is posted.
     */
    @Test
    public void testObservationEventHandler()
    {   
        //Test handling observation persisted event.
        Event persistEvent = mockObsPersisted();
        m_ObsEventHandler.handleEvent(persistEvent);
        assertThat(m_SUT.getObservationCount(CONTROLLER_ID), is(1));
        
        //Test handling observation merged event.
        Event mergedEvent = mockObsMerged();
        m_ObsEventHandler.handleEvent(mergedEvent);
        assertThat(m_SUT.getObservationCount(CONTROLLER_ID), is(2));
        
        //Verify an observation count updated event was posted for the persisted and merged events.
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        Event firstPush = eventCaptor.getAllValues().get(0);
        Event secondPush = eventCaptor.getAllValues().get(1);
        assertThat(firstPush.getTopic(), is(ObservationCountMgr.TOPIC_OBSERVATION_COUNT_UPDATED));
        assertThat((int)firstPush.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID), is(CONTROLLER_ID));
        assertThat((int)firstPush.getProperty(ObservationCountMgr.EVENT_PROP_OBS_COUNT), is(1));
        
        assertThat(secondPush.getTopic(), is(ObservationCountMgr.TOPIC_OBSERVATION_COUNT_UPDATED));
        assertThat((int)secondPush.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID), is(CONTROLLER_ID));
        assertThat((int)secondPush.getProperty(ObservationCountMgr.EVENT_PROP_OBS_COUNT), is(2));
    }
    
    /**
     * Test the controller event handlers handle event method.
     * Verify that the specified controller is removed from the list know controllers. 
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testControllerEventHandler() throws SecurityException, NoSuchFieldException, IllegalArgumentException, 
        IllegalAccessException
    {
        //Add controller and verify existence.
        Event persistEvent = mockObsPersisted();
        m_ObsEventHandler.handleEvent(persistEvent);
        assertThat(m_SUT.getObservationCount(CONTROLLER_ID), is(1));
        
        //Setup event.
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, 5);
        Event controllerRemoved = new Event(ControllerMgr.TOPIC_CONTROLLER_REMOVED, props);
        
        //Handle event.
        m_ControllerEventHandler.handleEvent(controllerRemoved);
        
        //Verify nothing happens if a controller removed event is received for an unknown controller.
        m_ControllerEventHandler.handleEvent(controllerRemoved);
        
        //Retrieve map using reflections.
        Field mapField = m_SUT.getClass().getDeclaredField("m_ControllerObsCount");
        mapField.setAccessible(true);
        Map<Integer, Integer> obsCntMap = (Map<Integer, Integer>)mapField.get(m_SUT);
        
        //Verify controller ID is not contained within the map.
        assertThat(obsCntMap.containsKey(CONTROLLER_ID), is(false));
    }
    
    /**
     * Method used to mock an observation persisted event.
     * 
     * @return
     *          Returns a mocked observation persisted event.
     */
    private Event mockObsPersisted()
    {
        Observation obs = mock(Observation.class);
        UUID uuid = UUID.randomUUID();
        
        when(obs.getSystemId()).thenReturn(CONTROLLER_ID);
        when(m_ObsStore.find(uuid)).thenReturn(obs);
        
        //Properties map for both persisted and merged events.
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ObservationStore.EVENT_PROP_OBSERVATION_UUID, uuid);
        
        return new Event(ObservationStore.TOPIC_OBSERVATION_PERSISTED, props);
    }
    
    /**
     * Method used to mock an observation merged event.
     * 
     * @return
     *          Returns a mocked observation merged event.
     */
    private Event mockObsMerged()
    {
        Observation obs = mock(Observation.class);
        UUID uuid = UUID.randomUUID();
        
        when(obs.getSystemId()).thenReturn(CONTROLLER_ID);
        when(m_ObsStore.find(uuid)).thenReturn(obs);
        
        //Properties map for both persisted and merged events.
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ObservationStore.EVENT_PROP_OBSERVATION_UUID, uuid);
        
        return new Event(ObservationStore.TOPIC_OBSERVATION_MERGED, props);
    }
}
