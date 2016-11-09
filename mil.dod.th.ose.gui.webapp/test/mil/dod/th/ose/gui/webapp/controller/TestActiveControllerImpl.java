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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;

import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * Test functionality of active controller class. 
 * @author callen
 *
 */
public class TestActiveControllerImpl 
{
    private ActiveControllerImpl m_SUT;
    private GrowlMessageUtil m_GrowlUtil;
    private ControllerMgr m_ControllerMgr;
    private EventAdmin m_EventAdmin;
    private ControllerImage m_ControllerImageInterface;
    
    @Before
    public void setUp()
    {
        //mock services
        m_GrowlUtil = mock(GrowlMessageUtil.class);
        m_ControllerMgr = mock(ControllerMgr.class);
        m_EventAdmin = mock(EventAdmin.class);
        m_ControllerImageInterface = mock(ControllerImage.class);

        //create bean
        m_SUT = new ActiveControllerImpl();
        
        //set services
        m_SUT.setGrowlMessageUtility(m_GrowlUtil);
        m_SUT.setControllerManager(m_ControllerMgr);
        m_SUT.setEventAdmin(m_EventAdmin);
    }

    /**
     * Test setting active controller.
     */
    @Test
    public void testSetActiveController()
    {        
        //get a controller to make the active controller
        ControllerModel model = new ControllerModel(123456, m_ControllerImageInterface);
        m_SUT.setActiveController(model);

        //verify controller does not get set as active controller due to model not being ready
        assertThat(m_SUT.getActiveController(), is(nullValue()));
        
        //reset model ready state and retest
        model.setIsReady(true);
        m_SUT.setActiveController(model);
        
        //verify event that the active controller changed
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getTopic(), is(ActiveController.TOPIC_ACTIVE_CONTROLLER_CHANGED));
        
        //mocking behavior
        when(m_ControllerMgr.getController(eq(123456))).thenReturn(model);        
        assertThat((m_SUT.getActiveController()).getId(), is(123456));
                        
        //set another controller as the active controller
        model = new ControllerModel(1478, m_ControllerImageInterface);
        model.setIsReady(true);
        when(m_ControllerMgr.getController(eq(1478))).thenReturn(model);
        m_SUT.setActiveController(model);
        
        //make sure that there is only ONE active controller and the id is not the id of the controller previously set 
        //as active
        assertThat((m_SUT.getActiveController()).getId(), is(not(123456)));
    }
    
    /**
     * Verify no event is posted if the current active controller
     * and new active controller are both null.  
     */
    @Test
    public void testSetNullController()
    {
        assertThat(m_SUT.getActiveController(), is(nullValue()));        
        m_SUT.setActiveController(null);
        
        //verify no event was posted due to active controller already being null
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, Mockito.never()).postEvent(eventCaptor.capture());
    }
    
    /**
     * Test getting the active controller.
     */
    @Test
    public void testGetActiveController()
    {        
        //get a controller to make the active controller
        ControllerModel model = new ControllerModel(123456, m_ControllerImageInterface);
        
        /**
         * Verify active controller is null, if desired active controller model is not ready. 
         * No ACTIVE_CONTROLLER_CHANGED event should be posted.
         */
        m_SUT.setActiveController(model); 
        when(m_ControllerMgr.getController(eq(123456))).thenReturn(model);
        assertThat(m_SUT.getActiveController(), is(nullValue()));
        
        /**
         * Verify active controller is set if model is not null and is in ready state.
         * 1 ACTIVE_CONTROLLER_CHANGED event should be posted.
         */
        model.setIsReady(true);
        m_SUT.setActiveController(model); //active controller changed
        when(m_ControllerMgr.getController(eq(123456))).thenReturn(model); 
        assertThat((m_SUT.getActiveController()).getId(), is(123456));

        /**
         * mock controller manager returning null, this is the behavior expected when the model is not found to return.
         * 1 ACTIVE_CONTROLLER_CHANGED event should be posted
         */
        ControllerModel modelNull = null;
        when(m_ControllerMgr.getController(eq(123456))).thenReturn(modelNull);        
        assertThat(m_SUT.getActiveController(), is(nullValue())); //active controller changed
        verify(m_GrowlUtil).createLocalFacesMessage(eq(FacesMessage.SEVERITY_INFO), Mockito.anyString(), 
            Mockito.anyString());
        
        /**
         * verify update active controller grabs a new ready controller
         * if controller mgr returns model that's not ready.
         * 2 ACTIVE_CONTROLLER_CHANGED events should be posted.
         */
        ControllerModel model2 = new ControllerModel(7890, m_ControllerImageInterface);
        model2.setIsReady(true);
        model2.setName("model2");
        
        List<ControllerModel> modelList = new ArrayList<ControllerModel>();
        modelList.add(model2);
        modelList.add(model);       
        
        m_SUT.setActiveController(model2); //active controller changed
        model2.setIsReady(false);
        when(m_ControllerMgr.getController(eq(7890))).thenReturn(model2);
        when(m_ControllerMgr.getAllControllers()).thenReturn(modelList);
        assertThat(m_SUT.getActiveController().getId(), is(123456)); //grab first ready controller, controller changed
        
        /**
         * Verify calling get active controller on unchanged active controller does not
         * cause an ACTIVE_CONTROLLER_CHANGED event to be posted. 
         */
        when(m_ControllerMgr.getController(eq(123456))).thenReturn(model);  
        assertThat(m_SUT.getActiveController().getId(), is(123456));
        
        //verify event that the active controller changed 4 times
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, Mockito.times(4)).postEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getTopic(), is(ActiveController.TOPIC_ACTIVE_CONTROLLER_CHANGED));
    }
    
    /**
     * Verify appropriate boolean value is returned based on the existence of an active controller. 
     */
    @Test
    public void testIsActiveControllerSet()
    {
        assertThat(m_SUT.isActiveControllerSet(), is(false));
        
        ControllerModel model = new ControllerModel(123456, m_ControllerImageInterface);
        model.setIsReady(true);
        
        m_SUT.setActiveController(model);
        
        when(m_ControllerMgr.getController(eq(123456))).thenReturn(model);
        
        assertThat(m_SUT.isActiveControllerSet(), is(true));
    }
    
    /**
     * Verify default setting of ready active controller if one is not set.
     */
    @Test
    public void testGetActiveControllerNotSet()
    {        
        //mock controller model
        ControllerModel model = new ControllerModel(123456, m_ControllerImageInterface);
        ControllerModel model2 = new ControllerModel(654321, m_ControllerImageInterface);
        
        model2.setIsReady(true);
        
        List<ControllerModel> models = new ArrayList<ControllerModel>();
        models.add(model);
        models.add(model2);
        
        when(m_ControllerMgr.getAllControllers()).thenReturn(models);
        
        //verify
        assertThat((m_SUT.getActiveController()).getId(), is(654321));
    }
}
