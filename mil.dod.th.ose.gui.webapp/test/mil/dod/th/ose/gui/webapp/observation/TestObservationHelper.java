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
import static org.mockito.Mockito.*;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.faces.application.FacesMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.ObservationStoreNamespace.ObservationStoreMessageType;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.ose.remote.api.RemoteEventConstants;

/**
 * Class to test the ObservationHelper
 * @author nickmarcucci
 *
 */
public class TestObservationHelper
{
    private ObservationHelper m_SUT;
    private GrowlMessageUtil m_GrowlUtil;
    private EventAdmin  m_EventAdmin;
    private BundleContextUtil m_BundleContextUtil;
    private BundleContext m_BundleContext;
    private ServiceRegistration<?> m_ServiceRegistration;
    private EventHandler m_RemoteEventHandler;
    private EventHandler m_RemoteObsStoreHandler;
    
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Before
    public void init()
    {
        m_GrowlUtil = mock(GrowlMessageUtil.class);
        
        m_BundleContextUtil = mock(BundleContextUtil.class);
        m_BundleContext = mock(BundleContext.class);
        m_ServiceRegistration = mock(ServiceRegistration.class);
        
        when(m_BundleContextUtil.getBundleContext()).thenReturn(m_BundleContext);
        when(m_BundleContext.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class),
                Mockito.any(Dictionary.class))).thenReturn(m_ServiceRegistration);
        
        m_EventAdmin = mock(EventAdmin.class);
        
        m_SUT = new ObservationHelper();
        
        m_SUT.setBundleContextUtil(m_BundleContextUtil);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setGrowlMessageUtility(m_GrowlUtil);
        
        m_SUT.registerEventHelper();
        
        ArgumentCaptor<Dictionary> dictCaptor = ArgumentCaptor.forClass(Dictionary.class);
        ArgumentCaptor<EventHandler> eventCaptor = ArgumentCaptor.forClass(EventHandler.class);
        
        verify(m_BundleContext, times(2)).registerService(eq(EventHandler.class),
                eventCaptor.capture(), dictCaptor.capture());
        
        Dictionary<String, Object> dictionaryObsStore = dictCaptor.getAllValues().get(0);
        Dictionary<String, Object> dictionaryRemoteObs = dictCaptor.getAllValues().get(1);
        assertThat(dictionaryObsStore, notNullValue());
        assertThat(dictionaryRemoteObs, notNullValue());
        
        String topicFilter = (String)dictionaryObsStore.get(EventConstants.EVENT_FILTER);
        assertThat(topicFilter, containsString(ObservationStoreMessageType.RemoveObservationByUUIDResponse.toString()));
        assertThat(topicFilter, containsString(ObservationStoreMessageType.RemoveObservationResponse.toString()));
        
        m_RemoteObsStoreHandler = eventCaptor.getAllValues().get(0);
        m_RemoteEventHandler = eventCaptor.getAllValues().get(1);
        
        assertThat(m_RemoteEventHandler, notNullValue());
        assertThat(m_RemoteObsStoreHandler, notNullValue());
    }
    
    @After
    public void tearDown()
    {
        m_SUT.unregisterHelpers();
        verify(m_ServiceRegistration, times(2)).unregister();
    }
    
    /*
     * Verify GetObservationResponseData with observations produces an EventAdmin event.
     */
    @Test
    public void testHandleGetObsResponseWithObservations()
    {
        Event createdEvent = createARemoteObservationMessage(MessageType.GET);
        
        m_RemoteEventHandler.handleEvent(createdEvent);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(1)).postEvent(eventCaptor.capture());
        
        Event capturedEvent = eventCaptor.getValue();
        
        assertThat(capturedEvent, notNullValue());
        
        assertThat(capturedEvent.getTopic(), is(ObservationMgr.TOPIC_OBS_STORE_UPDATED));
        
        verify(m_GrowlUtil, times(1)).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_INFO), 
                eq("Observation Retrieval Complete"), eq("3 observations were retrieved."));
    }
    
    /*
     * Verify a RemoveObservationResponseData message produces only a growl message.
     */
    @Test
    public void testHandleRemoveObsResponse()
    {
        Event createdEvent = createARemoteObservationMessage(MessageType.REMOVE);
        
        m_RemoteObsStoreHandler.handleEvent(createdEvent);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(1)).postEvent(eventCaptor.capture());
        
        Event capturedEvent = eventCaptor.getValue();
        
        assertThat(capturedEvent, notNullValue());
        
        assertThat(capturedEvent.getTopic(), is(ObservationMgr.TOPIC_OBS_STORE_UPDATED));
        
        verify(m_GrowlUtil, times(1)).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_INFO), 
                eq("Observations Removed"), eq(String.format("Controller 0x%08x has remotely " +
                        "removed observations for selected assets.", 1)));
    }
    
    /**
     * Create an Event containing either a GetObservationResponseData or RemoveObservationResponseData
     * object.
     * @param type
     *  the type of observation response data to produce.
     * @return
     *  the event with the proper data and fields for the event.
     */
    private Event createARemoteObservationMessage(MessageType type)
    {
        Dictionary<String, Object> eventProps = new Hashtable<String, Object>();
        Event eventToReturn = null;
        if (type.equals(MessageType.GET))
        {            
            eventProps.put(RemoteEventConstants.EVENT_PROP_OBS_NUMBER_RETRIEVED, 3);
            eventToReturn = new Event(RemoteEventConstants.TOPIC_OBS_STORE_RETRIEVE_COMPLETE, eventProps);
        }
        else
        {
            eventProps.put(RemoteConstants.EVENT_PROP_SOURCE_ID, 1);
            
            eventProps.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
                    ObservationStoreMessageType.RemoveObservationResponse.toString());
            eventToReturn = new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, eventProps);
        }
        
        return eventToReturn;
    }
    
    /**
     * Enum which denotes which type of response message is needed.
     * @author nickmarcucci
     *
     */
    private enum MessageType
    {
        GET,
        REMOVE
    };
}
