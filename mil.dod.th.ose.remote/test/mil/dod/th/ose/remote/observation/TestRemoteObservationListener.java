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
package mil.dod.th.ose.remote.observation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.FindObservationByUUIDResponseData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.GetObservationResponseData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.ObservationStoreNamespace.ObservationStoreMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.ose.remote.TerraHarvestMessageHelper;
import mil.dod.th.ose.remote.api.RemoteEventConstants;
import mil.dod.th.ose.remote.observation.RemoteObservationListener.ObservationRemoteHandler;
import mil.dod.th.ose.remote.observation.RemoteObservationListener.ObservationStoreHandler;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.remote.lexicon.observation.types.ObservationGen;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen;

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

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

/**
 * @author callen
 *
 */
public class TestRemoteObservationListener 
{
    private RemoteObservationListener m_SUT;
    private LoggingService m_Logging;
    private JaxbProtoObjectConverter m_Converter;
    private ObservationStoreHandler m_ObservationHandler;
    private ObservationRemoteHandler m_RemoteObsListener;
    private BundleContext m_Context;
    private ServiceRegistration<EventHandler> m_HandlerReg;
    private ObservationStore m_ObservationStore;
    private EventAdmin m_EventAdmin;

    /**
     * Set up dependencies and services.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception
    {
        //set up
        m_SUT = new RemoteObservationListener();
        m_Logging = LoggingServiceMocker.createMock();
        m_Converter = mock(JaxbProtoObjectConverter.class);
        m_Context = mock(BundleContext.class);
        m_HandlerReg = mock(ServiceRegistration.class);
        m_ObservationStore = mock(ObservationStore.class);
        m_EventAdmin = mock(EventAdmin.class);

        //set services
        m_SUT.setLoggingService(m_Logging);
        m_SUT.setJaxbProtoObjectConverter(m_Converter);
        m_SUT.setObservationStore(m_ObservationStore);
        m_SUT.setEventAdmin(m_EventAdmin);
        
        //mock behavior for event listener
        when(m_Context.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
            Mockito.any(Dictionary.class))).thenReturn(m_HandlerReg);

        when(m_Context.getProperty(RemoteObservationListener.ENABLED_FRAMEWORK_PROPERTY)).thenReturn("true");

        //activate
        m_SUT.activate(m_Context, new HashMap<String, Object>());

        ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(m_Context, times(2)).registerService(eq(EventHandler.class), captor.capture(), 
            Mockito.any(Dictionary.class));

        //assign captured values
        m_ObservationHandler = (ObservationStoreHandler) captor.getAllValues().get(0);
        m_RemoteObsListener = (ObservationRemoteHandler) captor.getAllValues().get(1);
    }

    /**
     * Test deactivation.
     * Verify the unregistering of event handlers.
     */
    @Test
    public void testDeactivate() throws InterruptedException
    {
        //deactivate
        m_SUT.deactivate();

        //verify
        verify(m_HandlerReg, times(2)).unregister();
    }

    /**
     * Test loading get observation response data.
     * Verify that the observation loading interactions take place.
     */
    @Test
    public void testGetObservationResponse() throws ObjectConverterException, IllegalArgumentException, 
        PersistenceFailedException, ValidationFailedException
    {

        m_ObservationHandler.handleEvent(getObservationsEvent());

        verify(m_ObservationStore, timeout(100).times(3)).persist(Mockito.any(Observation.class));
        
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        //verify push event
        verify(m_EventAdmin).postEvent(captor.capture());
        assertThat(captor.getValue().getTopic(), is(RemoteEventConstants.TOPIC_OBS_STORE_RETRIEVE_COMPLETE));
        assertThat((Integer)captor.getValue().getProperty(RemoteEventConstants.EVENT_PROP_OBS_NUMBER_RETRIEVED), is(3));
    }
    
    /**
     * Verify that if zero observations are returned when retrieving that an event is still posted.
     */
    @Test
    public void testGetObservationResponse_ZeroObs() throws ValidationFailedException, IllegalArgumentException, 
        PersistenceFailedException
    {
        m_ObservationHandler.handleEvent(getEmptyObservationEvent());

        verify(m_ObservationStore, never()).persist(Mockito.any(Observation.class));
        
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        //verify push event
        verify(m_EventAdmin).postEvent(captor.capture());
        assertThat(captor.getValue().getTopic(), is(RemoteEventConstants.TOPIC_OBS_STORE_RETRIEVE_COMPLETE));
        assertThat((Integer)captor.getValue().getProperty(RemoteEventConstants.EVENT_PROP_OBS_NUMBER_RETRIEVED), is(0));
    }
    
    /**
     * Test loading get observation response data.
     * Verify that the observation loading interactions take place.
     */
    @Test
    public void testGetObservationResponse_InvalidFormat() throws Exception
    {
        GetObservationResponseData response = GetObservationResponseData.newBuilder()
                .setObservationFormat(RemoteTypesGen.LexiconFormat.Enum.XML)
                .addObservationXml(ByteString.copyFromUtf8("blah"))
                .addObservationXml(ByteString.copyFromUtf8("dee"))
                .addObservationXml(ByteString.copyFromUtf8("blah-blah")).build();
        
        m_ObservationHandler.handleEvent(createEvent(response, Namespace.ObservationStore, 
            ObservationStoreMessageType.GetObservationResponse.toString()));

        verify(m_ObservationStore, timeout(1000).never()).persist(Mockito.any(Observation.class));
        
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        //verify push event
        verify(m_EventAdmin).postEvent(captor.capture());
        assertThat(captor.getValue().getTopic(), is(RemoteEventConstants.TOPIC_OBS_STORE_RETRIEVE_COMPLETE));
        assertThat((Integer)captor.getValue().getProperty(RemoteEventConstants.EVENT_PROP_OBS_NUMBER_RETRIEVED), is(0));
    }

    /**
     * Test loading an observation namespace find template response.
     * Verify that the observation loading interactions take place.
     */
    @Test
    public void testFindObservationResponse() throws ObjectConverterException, IllegalArgumentException, 
        PersistenceFailedException, ValidationFailedException
    {
        //mocks for converter
        Observation obsJaxb = mock(Observation.class);

        //mock behavior
        when(m_Converter.convertToJaxb(Mockito.any(Message.class))).thenReturn(obsJaxb);

        m_ObservationHandler.handleEvent(findObservationsEvent());

        verify(m_ObservationStore, timeout(100).times(3)).persist(Mockito.any(Observation.class));
    }

    /**
     * Test loading an observation namespace find template response with a jaxb exception.
     * Verify that the observation is not persisted.
     * Verify other observations are persisted.
     */
    @Test
    public void testFindObservationResponsePersistException() throws ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, ValidationFailedException
    {
        m_ObservationHandler.handleEvent(findObservationsEventException());

        verify(m_ObservationStore, timeout(100).times(2)).persist(Mockito.any(Observation.class));
    }
    
    /**
     * Test loading an observation namespace message where the observation already exists.
     * Verify that the observations are removed.
     * Verify validated observation is persisted, replacing the already existent observation.
     */
    @Test
    public void testFindObservationResponsePersistMerge() throws ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, ValidationFailedException
    {
        //mocks for converter
        Observation obsJaxb = mock(Observation.class);
        when(obsJaxb.getUuid()).thenReturn(UUID.randomUUID());
        
        //mock behavior
        when(m_Converter.convertToJaxb(Mockito.any(Message.class))).thenReturn(obsJaxb);
        when(m_ObservationStore.find(Mockito.any(UUID.class))).thenReturn(obsJaxb);

        m_ObservationHandler.handleEvent(findObservationsEvent());

        verify(m_ObservationStore, timeout(100).times(3)).remove(obsJaxb);
        verify(m_ObservationStore, timeout(100).times(3)).persist(Mockito.any(Observation.class));
    }

    /**
     * Test loading an observation from a locally posted event from a remote system.
     */
    @Test
    public void testLoadObservationFromLocalEvent() throws ValidationFailedException, IllegalArgumentException, 
        PersistenceFailedException
    {
        //handle event
        m_RemoteObsListener.handleEvent(createObsEvent());

        verify(m_ObservationStore, timeout(100).atLeastOnce()).persist(Mockito.any(Observation.class));
    }

    /**
     * Test the handler registrations. 
     * Verify correct topic, and filters are being used.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testHandleRegistration()
    {
        //prop map simulating the enabled property is updated to false
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("enabled", false);
        m_SUT.modified(props);

        //verify
        verify(m_HandlerReg, times(2)).unregister();

        //reactivate
        props.put("enabled", true);
        m_SUT.modified(props);

        //verify, handlers registered again.
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Dictionary> dictCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(m_Context, times(4)).registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
            dictCaptor.capture());

        //map for filter and the topic value, not topic and then filter, because two of the registrations have the same
        //topic but different filters. 
        Map<Object, Object> topicFilters = new HashMap<Object, Object>();
        for (@SuppressWarnings("rawtypes") Dictionary dictionary : dictCaptor.getAllValues())
        {
            topicFilters.put(dictionary.get(EventConstants.EVENT_FILTER), 
                dictionary.get(EventConstants.EVENT_TOPIC));
        }
        
        assertThat(topicFilters, hasEntry((Object)String.format("(&(%s=%s)(|(%s=%s)(%s=%s)))", 
                RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.ObservationStore.toString(),
                RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
                ObservationStoreMessageType.FindObservationByUUIDResponse.toString(),
                RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
                ObservationStoreMessageType.GetObservationResponse.toString()),
                (Object)RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat(topicFilters, hasEntry((Object)null, (Object)(new
             String[] {ObservationStore.TOPIC_OBSERVATION_MERGED_WITH_OBS + RemoteConstants.REMOTE_TOPIC_SUFFIX,
            ObservationStore.TOPIC_OBSERVATION_PERSISTED_WITH_OBS + RemoteConstants.REMOTE_TOPIC_SUFFIX})));
    }

    /**
     * Test the ability for the component to be configured. 
     * Verify properties are updated.
     * Verify default of true if property is not set in config file.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConfiguration()
    {
        //prop map simulating the enabled property is updated to false
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("enabled", false);
        m_SUT.modified(props);

        //verify
        verify(m_HandlerReg, times(2)).unregister();

        //mock null property
        when(m_Context.getProperty(RemoteObservationListener.ENABLED_FRAMEWORK_PROPERTY)).thenReturn(null);

        //call update again, since the enable property isn't in the map should use framework property
        props = new HashMap<String, Object>();
        m_SUT.modified(props);

        //verify, handlers registered again.
        
        verify(m_Context, times(4)).registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
            Mockito.any(Dictionary.class));
    }
    
    /**
     * Verify that a message logged when the received observation queue is full.
     */
    @Test
    public void testRemoteObservationHandlerFullQueue() throws InterruptedException
    {
        // deactivate component so thread isn't running to read items off of queue
        m_SUT.deactivate();
        
        Observation obs = null;
        for (int i = 0; i < 102; i++)
        {
            obs = new Observation().withUuid(UUID.randomUUID());
            // properties for the event
            final Map<String, Object> props = new HashMap<String, Object>();
            props.put(ObservationStore.EVENT_PROP_OBSERVATION, obs);
            m_RemoteObsListener.handleEvent(new Event(ObservationStore.TOPIC_OBSERVATION_PERSISTED_WITH_OBS, props));
        }
        assertThat(obs, is(notNullValue()));

        verify(m_Logging).info("The remote observations with UUID %s could not be received, the received observation"
                + "queue is currently full.", obs.getUuid());
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////
    //  Support methods representing proto observations and jaxb observations                   //
    //////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a get observation event, this event contains the fields needed by the remote observation listener
     * that would be present in an actual remote message received event for a get observations response message.
     */
    private Event getObservationsEvent() throws ObjectConverterException
    {
        //Mock the JAXB observations that correlate to the protocol observations.
        final Observation jaxbObs1 = mock(Observation.class);
        final Observation jaxbObs2 = mock(Observation.class);
        final Observation jaxbObs3 = mock(Observation.class);
        
        //observations to return
        final ObservationGen.Observation obs1 = TerraHarvestMessageHelper.getProtoObs();
        final ObservationGen.Observation obs2 = TerraHarvestMessageHelper.getProtoObs();
        final ObservationGen.Observation obs3 = TerraHarvestMessageHelper.getProtoObs();
        
        when(jaxbObs1.getUuid()).thenReturn(SharedMessageUtils.convertProtoUUIDtoUUID(obs1.getUuid()));
        when(jaxbObs2.getUuid()).thenReturn(SharedMessageUtils.convertProtoUUIDtoUUID(obs2.getUuid()));
        when(jaxbObs3.getUuid()).thenReturn(SharedMessageUtils.convertProtoUUIDtoUUID(obs3.getUuid()));
        
        when(m_Converter.convertToJaxb(obs1)).thenReturn(jaxbObs1);
        when(m_Converter.convertToJaxb(obs2)).thenReturn(jaxbObs2);
        when(m_Converter.convertToJaxb(obs3)).thenReturn(jaxbObs3);

        //message
        GetObservationResponseData response = GetObservationResponseData.newBuilder().addObservationNative(obs3).
            addObservationNative(obs2).addObservationNative(obs1).build();

        return createEvent(response, Namespace.ObservationStore , 
            ObservationStoreMessageType.GetObservationResponse.toString());
    }
    
    /**
     * Create an observation event that contains zero observations.
     */
    private Event getEmptyObservationEvent()
    {
        return createEvent( GetObservationResponseData.newBuilder().build(), Namespace.ObservationStore , 
            ObservationStoreMessageType.GetObservationResponse.toString());
    }

    /**
     * Create a find observation event, this event contains the fields needed by the remote observation listener
     * that would be present in an actual remote message received event for a find observation response message.
     */
    private Event findObservationsEvent() throws ObjectConverterException
    {
        //Mock the JAXB observations that correlate to the protocol observations.
        final Observation jaxbObs1 = mock(Observation.class);
        final Observation jaxbObs2 = mock(Observation.class);
        final Observation jaxbObs3 = mock(Observation.class);
        
        //observations to return
        final ObservationGen.Observation obs1 = TerraHarvestMessageHelper.getProtoObs();
        final ObservationGen.Observation obs2 = TerraHarvestMessageHelper.getProtoObs();
        final ObservationGen.Observation obs3 = TerraHarvestMessageHelper.getProtoObs();
        
        when(jaxbObs1.getUuid()).thenReturn(SharedMessageUtils.convertProtoUUIDtoUUID(obs1.getUuid()));
        when(jaxbObs2.getUuid()).thenReturn(SharedMessageUtils.convertProtoUUIDtoUUID(obs2.getUuid()));
        when(jaxbObs3.getUuid()).thenReturn(SharedMessageUtils.convertProtoUUIDtoUUID(obs3.getUuid()));
        
        when(m_Converter.convertToJaxb(obs1)).thenReturn(jaxbObs1);
        when(m_Converter.convertToJaxb(obs2)).thenReturn(jaxbObs2);
        when(m_Converter.convertToJaxb(obs3)).thenReturn(jaxbObs3);

        //message
        FindObservationByUUIDResponseData response = FindObservationByUUIDResponseData.newBuilder().
            addObservationNative(obs3).
            addObservationNative(obs2).
            addObservationNative(obs1).build();

        return createEvent(response, Namespace.ObservationStore , 
            ObservationStoreMessageType.FindObservationByUUIDResponse.toString());
    }

    /**
     * Create a find observation event, this event contains the fields needed by the remote observation listener
     * that would be present in an actual remote message received event for a find observation response message.
     **THIS RENDITION INCLUDES EXCEPTION BEHAVIOR**
     */
    private Event findObservationsEventException() throws ObjectConverterException
    {
        //Mock the JAXB observations that correlate to the protocol observations.
        final Observation jaxbObs1 = mock(Observation.class);
        final Observation jaxbObs2 = mock(Observation.class);
        final Observation jaxbObs3 = mock(Observation.class);
        
        //observations to return
        final ObservationGen.Observation obs1 = TerraHarvestMessageHelper.getProtoObs();
        final ObservationGen.Observation obs2 = TerraHarvestMessageHelper.getProtoObs();
        final ObservationGen.Observation obs3 = TerraHarvestMessageHelper.getProtoObs();
        
        when(jaxbObs1.getUuid()).thenReturn(SharedMessageUtils.convertProtoUUIDtoUUID(obs1.getUuid()));
        when(jaxbObs2.getUuid()).thenReturn(SharedMessageUtils.convertProtoUUIDtoUUID(obs2.getUuid()));
        when(jaxbObs3.getUuid()).thenReturn(SharedMessageUtils.convertProtoUUIDtoUUID(obs3.getUuid()));
        
        when(m_Converter.convertToJaxb(obs1)).thenReturn(jaxbObs1);
        when(m_Converter.convertToJaxb(obs2)).thenReturn(jaxbObs2);

        //exception
        when(m_Converter.convertToJaxb(obs3)).thenThrow(new ObjectConverterException(""));

        //message
        FindObservationByUUIDResponseData response = FindObservationByUUIDResponseData.newBuilder().
            addObservationNative(obs3).
            addObservationNative(obs2).
            addObservationNative(obs1).build();

        return createEvent(response, Namespace.ObservationStore , 
            ObservationStoreMessageType.FindObservationByUUIDResponse.toString());
    }

    /**
     * Create an event with the specified fields assigned appropriately.
     */
    private Event createEvent(final Message message, Namespace namespace, final String messageType)
    {
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, message);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, 1);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, namespace.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            messageType);
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
    }

    /**
     * Create an event with an observation, this mimics the locally posted event that happens when a remote event is
     * received that contains an observation. 
     */
    private Event createObsEvent()
    {
        Observation obs = mock(Observation.class);
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(ObservationStore.EVENT_PROP_OBSERVATION, obs);

        //the event
        return new Event(ObservationStore.TOPIC_OBSERVATION_PERSISTED_WITH_OBS, props);
    }
}