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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.persistence.ObservationQuery;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageResponseWrapper;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.FindObservationByUUIDRequestData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.FindObservationByUUIDResponseData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.GetObservationCountRequestData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.GetObservationCountResponseData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.GetObservationRequestData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.GetObservationResponseData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.ObservationStoreNamespace;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.ObservationStoreNamespace.ObservationStoreMessageType;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.Query;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.Range;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.RemoveObservationByUUIDRequestData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.RemoveObservationRequestData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.RemoveObservationResponseData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.SortField;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.SortOrder;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.TimeConstraintData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.types.observation.ObservationSubTypeEnum;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.TerraHarvestMessageHelper;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.remote.converter.ObservationSubTypeEnumConverter;
import mil.dod.th.remote.lexicon.observation.types.ObservationGen;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.google.protobuf.Message;

/**
 * Test class for the {@link ObservationMessageService}.
 * @author callen
 *
 */
public class TestObservationMessageService 
{
    private ObservationMessageService m_SUT;
    private LoggingService m_Logging;
    private JaxbProtoObjectConverter m_Converter;
    private ObservationStore m_ObservationStore;
    private EventAdmin m_EventAdmin;
    private MessageFactory m_MessageFactory;
    private MessageRouterInternal m_MessageRouter;
    private MessageResponseWrapper m_ResponseWrapper;

    //observation uuids
    private UUID uuidObs1 = UUID.randomUUID();
    private UUID uuidObs2 = UUID.randomUUID();

    //asset uuid
    private UUID uuid = UUID.randomUUID();

    /**
     * Set up dependencies and mock services.
     */
    @Before
    public void setUp()
    {
        //test class and mock services
        m_SUT = new ObservationMessageService();
        m_Logging = LoggingServiceMocker.createMock();
        m_Converter = mock(JaxbProtoObjectConverter.class);
        m_ObservationStore = mock(ObservationStore.class);
        m_EventAdmin = mock(EventAdmin.class);
        m_MessageFactory = mock(MessageFactory.class);
        m_ResponseWrapper = mock(MessageResponseWrapper.class);
        m_MessageRouter = mock(MessageRouterInternal.class);

        //set services
        m_SUT.setLoggingService(m_Logging);
        m_SUT.setJaxbProtoObjectConverter(m_Converter);
        m_SUT.setObservationStore(m_ObservationStore);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setMessageRouter(m_MessageRouter);

        when(m_MessageFactory.createObservationStoreResponseMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(ObservationStoreMessageType.class), Mockito.any(Message.class))).
                    thenReturn(m_ResponseWrapper);
        when(m_MessageFactory.createBaseErrorMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(ErrorCode.class), Mockito.anyString())).thenReturn(m_ResponseWrapper);

        m_SUT.activate();

        verify(m_MessageRouter).bindMessageService(m_SUT);
    }

    /**
     * Test deactivation.
     * 
     * Verify that message router is unbounded.
     */
    @Test
    public void testDeactivate()
    {
        m_SUT.deactivate();

        verify(m_MessageRouter).unbindMessageService(m_SUT);
    }

    /**
     * Verify the namespace is ObservationStore
     */
    @Test
    public void testGetNamespace()
    {
        assertThat(m_SUT.getNamespace(), is(Namespace.ObservationStore));
    }

    /**
     * Test generic handling of messages.
     * Verify that it posts an event locally.
     */
    @Test
    public void testGenericHandleMessage() throws IOException
    {
        FindObservationByUUIDResponseData message = FindObservationByUUIDResponseData.newBuilder()
            .addObservationNative(TerraHarvestMessageHelper.getProtoObs()).build();
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.FindObservationByUUIDResponse).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();

        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(thMessage));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(ObservationStoreMessageType.FindObservationByUUIDResponse.toString()));
        assertThat((ObservationStoreNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(namespace));
        assertThat((FindObservationByUUIDResponseData)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), 
                is(message));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
    }

    /**
     * Test handling of response messages.
     * Verify that they post events locally.
     */
    @Test
    public void testResponseMessages() throws IOException
    {
        //Get Observation
        GetObservationResponseData message = GetObservationResponseData.newBuilder()
            .addObservationNative(TerraHarvestMessageHelper.getProtoObs()).build();
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.GetObservationResponse).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();

        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(thMessage));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(ObservationStoreMessageType.GetObservationResponse.toString()));
        assertThat((ObservationStoreNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(namespace));
        assertThat((GetObservationResponseData)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), 
                is(message));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));

        //Remove observation response
        RemoveObservationResponseData removeData = RemoveObservationResponseData
                .newBuilder().setNumberOfObsRemoved(22L).build();
        namespace = ObservationStoreNamespace.newBuilder().
            setType(ObservationStoreMessageType.RemoveObservationResponse)
            .setData(removeData.toByteString()).build();
        payload = createPayload(namespace);
        thMessage = createMessageObservationStore(namespace);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        // verify event is posted
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        postedEvent = eventCaptor.getValue();

        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(thMessage));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(ObservationStoreMessageType.RemoveObservationResponse.toString()));
        
        RemoveObservationResponseData response = (RemoveObservationResponseData)
                postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE);
        assertThat(response.getNumberOfObsRemoved(), is(22L));
        
        assertThat((ObservationStoreNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
            is(namespace));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));

        //Remove observation response
        namespace = ObservationStoreNamespace.newBuilder().
            setType(ObservationStoreMessageType.RemoveObservationByUUIDResponse).build();
        payload = createPayload(namespace);
        thMessage = createMessageObservationStore(namespace);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        // verify event is posted
        verify(m_EventAdmin, times(3)).postEvent(eventCaptor.capture());
        postedEvent = eventCaptor.getValue();

        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(thMessage));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(ObservationStoreMessageType.RemoveObservationByUUIDResponse.toString()));
        assertThat((ObservationStoreNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
            is(namespace));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
    }
    
    /**
     * Verify that an observation count message is properly processed.
     */
    @Test
    public void testGetObservationCount() throws IOException
    {
        Query query = Query.getDefaultInstance();
        
        GetObservationCountRequestData message = GetObservationCountRequestData
                .newBuilder().setObsQuery(query).build();
        
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder()
                .setData(message.toByteString())
                .setType(ObservationStoreMessageType.GetObservationCountRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);
        
        RemoteChannel channel = mock(RemoteChannel.class);
        
        ObservationQuery oQuery = mock(ObservationQuery.class);
        when(m_ObservationStore.newQuery()).thenReturn(oQuery);
        when(oQuery.getCount()).thenReturn(44L);
        
        m_SUT.handleMessage(thMessage, payload, channel);
        
        ArgumentCaptor<GetObservationCountResponseData> response = ArgumentCaptor
                .forClass(GetObservationCountResponseData.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
                eq(ObservationStoreMessageType.GetObservationCountResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        GetObservationCountResponseData responseData = response.getValue();
        assertThat(responseData.getCount(), is(44L));   
    }
    
    /**
     * Verify that a GetObservationCountResponse message is properly handled.
     */
    @Test
    public void testGetObservationCountResponse() throws IOException
    {
        GetObservationCountResponseData message = GetObservationCountResponseData.newBuilder().setCount(33L).build();
        
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder()
                .setData(message.toByteString())
                .setType(ObservationStoreMessageType.GetObservationCountResponse).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);
        
        RemoteChannel channel = mock(RemoteChannel.class);
        
        m_SUT.handleMessage(thMessage, payload, channel);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event postedEvent = eventCaptor.getValue();
        
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(thMessage));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(ObservationStoreMessageType.GetObservationCountResponse.toString()));
        
        GetObservationCountResponseData response = (GetObservationCountResponseData)
                postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE);
        
        assertThat(response.getCount(), is(33L));
        assertThat((ObservationStoreNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
            is(namespace));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
    }
    
    /**
     * Verify a query with no parameters set still calls execute and returns all observations.
     * Verify response is sent.
     */
    @Test
    public void testGetObservationRequestAll() throws ObjectConverterException, IOException
    {
        Query query = Query.getDefaultInstance();
        
        GetObservationRequestData message = GetObservationRequestData.newBuilder().setObsQuery(query).build();
        
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.GetObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        Observation obs1 = mock(Observation.class);
        Observation obs2 = mock(Observation.class);
        List<Observation> observations = new ArrayList<Observation>();
        observations.add(obs1);
        observations.add(obs2);
        
        ObservationQuery oQuery = mockObservationQueryExecute(null, null, null, null, 
                null, null, null, null, null, null, null, null, observations);
        
        //proto observations to return
        ObservationGen.Observation obGen1 = TerraHarvestMessageHelper.getProtoObs();
        ObservationGen.Observation obGen2 = TerraHarvestMessageHelper.getProtoObs();

        //mock converter behavior
        when(m_Converter.convertToProto(obs1)).thenReturn(obGen1);
        when(m_Converter.convertToProto(obs2)).thenReturn(obGen2);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        //capture response
        ArgumentCaptor<GetObservationResponseData> response = ArgumentCaptor.forClass(GetObservationResponseData.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.GetObservationResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);

        //check response message for proto observations
        GetObservationResponseData responseData = response.getValue();
        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeCount(), is(2));
        assertThat(responseData.getObservationNativeList(), hasItems(obGen1, obGen2));
        
        verify(oQuery, never()).withAssetUuid(Mockito.any(UUID.class));
        verify(oQuery, never()).withAssetType(Mockito.anyString());
        verify(oQuery, never()).withMaxObservations(Mockito.anyInt());
        verify(oQuery, never()).withOrder(Mockito.any(ObservationQuery.SortField.class),
                                          Mockito.any(ObservationQuery.SortOrder.class));
        verify(oQuery, never()).withRange(Mockito.anyInt(), Mockito.anyInt());
        verify(oQuery, never()).withTimeCreatedRange(Mockito.any(Date.class), Mockito.any(Date.class));
        verify(oQuery, never()).withSubType(Mockito.any(ObservationSubTypeEnum.class));
        
        verify(oQuery).execute();
    }

    /**
     * Test GetObservationRequest with query of assetUuid.
     * Verify response is sent.
     */
    @Test
    public void testGetObservationRequestQueryAsset() throws IOException, ObjectConverterException
    {
        //necessary messages for request
        Query query = Query.newBuilder().setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid)).build();
        
        GetObservationRequestData message = GetObservationRequestData.newBuilder().setObsQuery(query).build();
        
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.GetObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        Observation obs1 = mock(Observation.class);
        Observation obs2 = mock(Observation.class);
        List<Observation> observations = new ArrayList<Observation>();
        observations.add(obs1);
        observations.add(obs2);
        
        ObservationQuery oQuery = mockObservationQueryExecute(uuid, null, null, null, 
                null, null, null, null, null, null, null, null, observations);

        //proto observations to return
        ObservationGen.Observation obGen1 = TerraHarvestMessageHelper.getProtoObs();
        ObservationGen.Observation obGen2 = TerraHarvestMessageHelper.getProtoObs();

        //mock converter behavior
        when(m_Converter.convertToProto(obs1)).thenReturn(obGen1);
        when(m_Converter.convertToProto(obs2)).thenReturn(obGen2);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        //capture response
        ArgumentCaptor<GetObservationResponseData> response = ArgumentCaptor.forClass(GetObservationResponseData.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.GetObservationResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);

        //check response message for proto observations
        GetObservationResponseData responseData = response.getValue();
        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeCount(), is(2));
        assertThat(responseData.getObservationNativeList(), hasItems(obGen1, obGen2));
        
        verify(oQuery).withAssetUuid(uuid);
        verify(oQuery).execute();
    }

    /**
     * Test GetObservationRequest with query of assetUuid with time constraints.
     * Verify response is sent.
     */
    @Test
    public void testGetObservationRequestQueryAssetTime() throws IOException, ObjectConverterException
    {
        //necessary messages for request
        TimeConstraintData time = getCreatedTimeConstraint();
        
        Query query = Query.newBuilder().setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid))
                .setCreatedTimeRange(time).build();
    
        GetObservationRequestData message = GetObservationRequestData.newBuilder()
                .setObsQuery(query).build();
        
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.GetObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        Observation obs1 = mock(Observation.class);
        Observation obs2 = mock(Observation.class);
        List<Observation> observations = new ArrayList<Observation>();
        observations.add(obs1);
        observations.add(obs2);
        
        Date timeStart = new Date(1L);
        Date timeEnd = new Date(3L);
        ObservationQuery oQuery = mockObservationQueryExecute(uuid, null, null, timeStart, timeEnd, 
                null, null, null, null, null, null, null, observations);
        
        //proto observations to return
        ObservationGen.Observation obGen1 = TerraHarvestMessageHelper.getProtoObs();
        ObservationGen.Observation obGen2 = TerraHarvestMessageHelper.getProtoObs();

        //mock converter behavior
        when(m_Converter.convertToProto(obs1)).thenReturn(obGen1);
        when(m_Converter.convertToProto(obs2)).thenReturn(obGen2);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        //capture response
        ArgumentCaptor<GetObservationResponseData> response = ArgumentCaptor.forClass(GetObservationResponseData.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.GetObservationResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);

        //check response message for proto observations
        GetObservationResponseData responseData = response.getValue();
        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeCount(), is(2));
        assertThat(responseData.getObservationNativeList(), hasItems(obGen1, obGen2));
        
        verify(oQuery).withAssetUuid(uuid);
        verify(oQuery).withTimeCreatedRange(timeStart, timeEnd);
        verify(oQuery).execute();
    }
    
    /**
     * Test GetObservationRequest with query of assetID, observed and created time constraints.
     * Verify response is sent.
     */
    @Test
    public void testGetObservationRequestQueryAssetCreatedTimeObservedTime() 
            throws IOException, ObjectConverterException
    {
        //necessary messages for request
        TimeConstraintData createdTime = getCreatedTimeConstraint();
        TimeConstraintData observedTime = getObservedTimeConstraint();
        
        Query query = Query.newBuilder().setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid))
                .setCreatedTimeRange(createdTime)
                .setObservedTimeRange(observedTime).build();

        GetObservationRequestData message = GetObservationRequestData.newBuilder()
                .setObsQuery(query).build();
        
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.GetObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        Observation obs1 = mock(Observation.class);
        Observation obs2 = mock(Observation.class);
        List<Observation> observations = new ArrayList<Observation>();
        observations.add(obs1);
        observations.add(obs2);
        
        Date createdTimeStart = new Date(1L);
        Date createdTimeEnd = new Date(3L);
        Date observedTimeStart = new Date(1L);
        Date observedTimeEnd = new Date(2L);
        ObservationQuery oQuery = mockObservationQueryExecute(uuid, null, null, createdTimeStart, 
                createdTimeEnd, observedTimeStart, observedTimeEnd, null, null, null, null, null, observations);
        
        //proto observations to return
        ObservationGen.Observation obGen1 = TerraHarvestMessageHelper.getProtoObs();
        ObservationGen.Observation obGen2 = TerraHarvestMessageHelper.getProtoObs();

        //mock converter behavior
        when(m_Converter.convertToProto(obs1)).thenReturn(obGen1);
        when(m_Converter.convertToProto(obs2)).thenReturn(obGen2);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        //capture response
        ArgumentCaptor<GetObservationResponseData> response = ArgumentCaptor.forClass(GetObservationResponseData.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.GetObservationResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);

        //check response message for proto observations
        GetObservationResponseData responseData = response.getValue();
        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeCount(), is(2));
        assertThat(responseData.getObservationNativeList(), hasItems(obGen1, obGen2));
        
        verify(oQuery).withAssetUuid(uuid);
        verify(oQuery).withTimeCreatedRange(createdTimeStart, createdTimeEnd);
        verify(oQuery).withTimeObservedRange(observedTimeStart, observedTimeEnd);
        verify(oQuery).execute();
    }
    
    /**
     * Test GetObservationRequest with query of assetUuid with maximum observation constraints
     */
    @Test
    public void testGetObservationRequestQueryAssetMaxObs() throws ObjectConverterException, IOException
    {
        //necessary messages for request
        Query query = Query.newBuilder().setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid))
                .setMaxNumberOfObs(1).build();
        
        GetObservationRequestData message = GetObservationRequestData.newBuilder()
                .setObsQuery(query).build();
        
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.GetObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        Observation obs1 = mock(Observation.class);
        Observation obs2 = mock(Observation.class);
        List<Observation> observations = new ArrayList<Observation>();
        observations.add(obs1);
        observations.add(obs2);
       
        ObservationQuery oQuery = mockObservationQueryExecute(uuid, null, null, 
                null, null, null, null, null, null, null, null, 1, observations.subList(0, 1));

        //proto observations to return
        ObservationGen.Observation obGen1 = TerraHarvestMessageHelper.getProtoObs();
        ObservationGen.Observation obGen2 = TerraHarvestMessageHelper.getProtoObs();

        //mock converter behavior
        when(m_Converter.convertToProto(obs1)).thenReturn(obGen1);
        when(m_Converter.convertToProto(obs2)).thenReturn(obGen2);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        //capture response
        ArgumentCaptor<GetObservationResponseData> response = ArgumentCaptor.forClass(GetObservationResponseData.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.GetObservationResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);

        //check response message for proto observations
        GetObservationResponseData responseData = response.getValue();
        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeCount(), is(1));
        assertThat(responseData.getObservationNativeList(), hasItems(obGen1));
        assertThat(responseData.getObservationNativeList(), not(hasItems(obGen2)));
        
        verify(oQuery).withAssetUuid(uuid);
        verify(oQuery).withMaxObservations(1);
        verify(oQuery).execute();
    }
    
    /**
     * Test GetObservationRequest with query of assetUuid with time constraints and maximum observations.
     * Verify response is sent.
     */
    @Test
    public void testGetObservationRequestQueryAssetTimeMaxObs() throws IOException, ObjectConverterException
    {
        //necessary messages for request
        TimeConstraintData time = getCreatedTimeConstraint();
        
        Query query = Query.newBuilder().setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid))
                .setCreatedTimeRange(time).setMaxNumberOfObs(1).build();
        
        GetObservationRequestData message = GetObservationRequestData.newBuilder().
            setObsQuery(query).build();
        
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.GetObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        Observation obs1 = mock(Observation.class);
        List<Observation> observations = new ArrayList<Observation>();
        observations.add(obs1);
        
        Date timeStart = new Date(1L);
        Date timeEnd = new Date(3L);
        ObservationQuery oQuery = mockObservationQueryExecute(uuid, null, null, 
                timeStart, timeEnd, null, null, null, null, null, null, null, observations);

        //proto observations to return
        ObservationGen.Observation obGen1 = TerraHarvestMessageHelper.getProtoObs();

        //mock converter behavior
        when(m_Converter.convertToProto(obs1)).thenReturn(obGen1);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        //capture response
        ArgumentCaptor<GetObservationResponseData> response = ArgumentCaptor.forClass(GetObservationResponseData.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.GetObservationResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);

        //check response message for proto observations
        GetObservationResponseData responseData = response.getValue();
        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeCount(), is(1));
        assertThat(responseData.getObservationNativeList(), hasItem(obGen1));
        
        verify(oQuery).withAssetUuid(uuid);
        verify(oQuery).withTimeCreatedRange(timeStart, timeEnd);
        verify(oQuery).withMaxObservations(1);
        verify(oQuery).execute();
    }
    /**
     * Test GetObservationRequest with query of assetType.
     * Verify response is sent.
     */
    @Test
    public void testGetObservationRequestQueryAssetType() throws IOException, ObjectConverterException
    {
        //necessary messages for request
        Query query = Query.newBuilder().setAssetType("a.new.Asset").build();

        GetObservationRequestData message = GetObservationRequestData.newBuilder()
                .setObsQuery(query).build();
        
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.GetObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        Observation obs1 = mock(Observation.class);
        Observation obs2 = mock(Observation.class);
        List<Observation> observations = new ArrayList<Observation>();
        observations.add(obs1);
        observations.add(obs2);
        
        ObservationQuery oQuery = mockObservationQueryExecute(null, "a.new.Asset", 
                null, null, null, null, null, null, null, null, null, null, observations);

        //proto observations to return
        ObservationGen.Observation obGen1 = TerraHarvestMessageHelper.getProtoObs();
        ObservationGen.Observation obGen2 = TerraHarvestMessageHelper.getProtoObs();

        //mock converter behavior
        when(m_Converter.convertToProto(obs1)).thenReturn(obGen1);
        when(m_Converter.convertToProto(obs2)).thenReturn(obGen2);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        //capture response
        ArgumentCaptor<GetObservationResponseData> response = ArgumentCaptor.forClass(GetObservationResponseData.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.GetObservationResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);

        //check response message for proto observations
        GetObservationResponseData responseData = response.getValue();
        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeCount(), is(2));
        assertThat(responseData.getObservationNativeList(), hasItems(obGen1, obGen2));
        
        verify(oQuery).withAssetType("a.new.Asset");
        verify(oQuery).execute();
    }

    /**
     * Test GetObservationRequest with query of assetType and time constraints.
     * Verify response is sent.
     */
    @Test
    public void testGetObservationRequestQueryAssetTypeTime() throws IOException, ObjectConverterException
    {
        //necessary messages for request
        TimeConstraintData time = getCreatedTimeConstraint();
        
        Query query = Query.newBuilder().setAssetType("a.new.Asset")
                .setCreatedTimeRange(time).build();

        GetObservationRequestData message = GetObservationRequestData.newBuilder()
                .setObsQuery(query).build();
        
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.GetObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        Observation obs1 = mock(Observation.class);
        Observation obs2 = mock(Observation.class);
        List<Observation> observations = new ArrayList<Observation>();
        observations.add(obs1);
        observations.add(obs2);
        
        Date timeStart = new Date(1L);
        Date timeEnd = new Date(3L);
        ObservationQuery oQuery = mockObservationQueryExecute(null, "a.new.Asset", null, timeStart,
                timeEnd, null, null, null, null, null, null, null, observations);

        //proto observations to return
        ObservationGen.Observation obGen1 = TerraHarvestMessageHelper.getProtoObs();
        ObservationGen.Observation obGen2 = TerraHarvestMessageHelper.getProtoObs();

        //mock converter behavior
        when(m_Converter.convertToProto(obs1)).thenReturn(obGen1);
        when(m_Converter.convertToProto(obs2)).thenReturn(obGen2);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        //capture response
        ArgumentCaptor<GetObservationResponseData> response = ArgumentCaptor.forClass(GetObservationResponseData.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.GetObservationResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);

        //check response message for proto observations
        GetObservationResponseData responseData = response.getValue();
        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeCount(), is(2));
        assertThat(responseData.getObservationNativeList(), hasItems(obGen1, obGen2));
        
        verify(oQuery).withAssetType("a.new.Asset");
        verify(oQuery).withTimeCreatedRange(timeStart, timeEnd);
        verify(oQuery).execute();
    }
    
    /**
     * Test GetObservationRequest with query of assetType and maximum number of observations.
     * Verify response is sent.
     */
    @Test
    public void testGetObservationRequestQueryAssetTypeMaxObs() throws IOException, ObjectConverterException
    {
        //necessary messages for request
        Query query = Query.newBuilder().setAssetType("a.new.Asset")
                .setMaxNumberOfObs(1).build();
        
        GetObservationRequestData message = GetObservationRequestData.newBuilder()
                .setObsQuery(query).build();
        
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.GetObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        Observation obs1 = mock(Observation.class);
        Observation obs2 = mock(Observation.class);
        List<Observation> observations = new ArrayList<Observation>();
        observations.add(obs1);
        observations.add(obs2);
        
        ObservationQuery oQuery = mockObservationQueryExecute(null, "a.new.Asset", 
                null, null, null, null, null, null, null, null, null, 1, observations.subList(0, 1));
        
        //proto observations to return
        ObservationGen.Observation obGen1 = TerraHarvestMessageHelper.getProtoObs();
        ObservationGen.Observation obGen2 = TerraHarvestMessageHelper.getProtoObs();

        //mock converter behavior
        when(m_Converter.convertToProto(obs1)).thenReturn(obGen1);
        when(m_Converter.convertToProto(obs2)).thenReturn(obGen2);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        //capture response
        ArgumentCaptor<GetObservationResponseData> response = ArgumentCaptor.forClass(GetObservationResponseData.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.GetObservationResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);

        //check response message for proto observations
        GetObservationResponseData responseData = response.getValue();
        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeList(), hasItems(obGen1));
        
        verify(oQuery).withAssetType("a.new.Asset");
        verify(oQuery).withMaxObservations(1);
        verify(oQuery).execute();
    }

    /**
     * Test GetObservationRequest with query of assetType and time constraints and maximum observations.
     * Verify response is sent.
     */
    @Test
    public void testGetObservationRequestQueryAssetTypeTimeMaxObs() throws IOException, ObjectConverterException
    {
        //necessary messages for request
        TimeConstraintData time = getCreatedTimeConstraint();
        
        Query query = Query.newBuilder().setAssetType("a.new.Asset")
                .setCreatedTimeRange(time).setMaxNumberOfObs(1).build();

        GetObservationRequestData message = GetObservationRequestData.newBuilder()
                .setObsQuery(query).build();
        
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.GetObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        Observation obs1 = mock(Observation.class);
        List<Observation> observations = new ArrayList<Observation>();
        observations.add(obs1);
        
        Date timeStart = new Date(1L);
        Date timeEnd = new Date(3L);
        ObservationQuery oQuery = mockObservationQueryExecute(null, "a.new.Asset", 
                null, timeStart, timeEnd, null, null, null, null, null, null, 1, observations);

        //proto observations to return
        ObservationGen.Observation obGen1 = TerraHarvestMessageHelper.getProtoObs();

        //mock converter behavior
        when(m_Converter.convertToProto(obs1)).thenReturn(obGen1);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        //capture response
        ArgumentCaptor<GetObservationResponseData> response = ArgumentCaptor.forClass(GetObservationResponseData.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.GetObservationResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);

        //check response message for proto observations
        GetObservationResponseData responseData = response.getValue();
        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeCount(), is(1));
        assertThat(responseData.getObservationNativeList(), hasItem(obGen1));
        
        verify(oQuery).withAssetType("a.new.Asset");
        verify(oQuery).withTimeCreatedRange(timeStart, timeEnd);
        verify(oQuery).withMaxObservations(1);
        verify(oQuery).execute();
    }
    /**
     * Test GetObservationRequest with query of observation type.
     * Verify response is sent.
     */
    @Test
    public void testGetObservationRequestQueryObsType() throws IOException, ObjectConverterException
    {
        //necessary messages for request
        Query query = Query.newBuilder().addObservationSubType(ObservationSubTypeEnumConverter
                .convertJavaEnumToProto(ObservationSubTypeEnum.AUDIO_METADATA)).build();
        
        GetObservationRequestData message = GetObservationRequestData.newBuilder().
            setObsQuery(query).build();
        
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.GetObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        Observation obs1 = mock(Observation.class);
        Observation obs2 = mock(Observation.class);
        List<Observation> observations = new ArrayList<Observation>();
        observations.add(obs1);
        observations.add(obs2);
        
        ObservationQuery oQuery = mockObservationQueryExecute(null, null, ObservationSubTypeEnum.AUDIO_METADATA, 
                null, null, null, null, null, null, null, null, null, observations);

        //proto observations to return
        ObservationGen.Observation obGen1 = TerraHarvestMessageHelper.getProtoObs();
        ObservationGen.Observation obGen2 = TerraHarvestMessageHelper.getProtoObs();

        //mock converter behavior
        when(m_Converter.convertToProto(obs1)).thenReturn(obGen1);
        when(m_Converter.convertToProto(obs2)).thenReturn(obGen2);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        //capture response
        ArgumentCaptor<GetObservationResponseData> response = ArgumentCaptor.forClass(GetObservationResponseData.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.GetObservationResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);

        //check response message for proto observations
        GetObservationResponseData responseData = response.getValue();
        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeCount(), is(2));
        assertThat(responseData.getObservationNativeList(), hasItems(obGen1, obGen2));
        
        verify(oQuery).withSubType(ObservationSubTypeEnum.AUDIO_METADATA);
        verify(oQuery).execute();
    }

    /**
     * Test GetObservationRequest with query of observation type and time constraints.
     * Verify response is sent.
     */
    @Test
    public void testGetObservationRequestQueryObsTypeTime() throws IOException, ObjectConverterException
    {
        //necessary messages for request
        TimeConstraintData time = getCreatedTimeConstraint();
        
        Query query = Query.newBuilder().addObservationSubType(
                ObservationSubTypeEnumConverter.convertJavaEnumToProto(ObservationSubTypeEnum.AUDIO_METADATA))
                .setCreatedTimeRange(time).build();
        
        GetObservationRequestData message = GetObservationRequestData.newBuilder().
            setObsQuery(query).build();
        
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.GetObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        Observation obs1 = mock(Observation.class);
        Observation obs2 = mock(Observation.class);
        List<Observation> observations = new ArrayList<Observation>();
        observations.add(obs1);
        observations.add(obs2);
        
        Date timeStart = new Date(1L);
        Date timeEnd = new Date(3L);
        ObservationQuery oQuery = mockObservationQueryExecute(null, null, ObservationSubTypeEnum.AUDIO_METADATA, 
                timeStart, timeEnd, null, null, null, null, null, null, null, observations);

        //proto observations to return
        ObservationGen.Observation obGen1 = TerraHarvestMessageHelper.getProtoObs();
        ObservationGen.Observation obGen2 = TerraHarvestMessageHelper.getProtoObs();

        //mock converter behavior
        when(m_Converter.convertToProto(obs1)).thenReturn(obGen1);
        when(m_Converter.convertToProto(obs2)).thenReturn(obGen2);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        //capture response
        ArgumentCaptor<GetObservationResponseData> response = ArgumentCaptor.forClass(GetObservationResponseData.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.GetObservationResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);

        //check response message for proto observations
        GetObservationResponseData responseData = response.getValue();
        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeCount(), is(2));
        assertThat(responseData.getObservationNativeList(), hasItems(obGen1, obGen2));
        
        verify(oQuery).withSubType(ObservationSubTypeEnum.AUDIO_METADATA);
        verify(oQuery).withTimeCreatedRange(timeStart, timeEnd);
        verify(oQuery).execute();
    }
    
    /**
     * Test GetObservationRequest with query of observation type and maximum number of observations.
     * Verify response is sent.
     */
    @Test
    public void testGetObservationRequestQueryObsTypeMaxObs() throws IOException, ObjectConverterException
    {
        //necessary messages for request
        Query query = Query.newBuilder().addObservationSubType(
                ObservationSubTypeEnumConverter.convertJavaEnumToProto(ObservationSubTypeEnum.AUDIO_METADATA))
                .setMaxNumberOfObs(1).build();
        
        GetObservationRequestData message = GetObservationRequestData.newBuilder().
            setObsQuery(query).build();
        
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.GetObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        Observation obs1 = mock(Observation.class);
        Observation obs2 = mock(Observation.class);
        List<Observation> observations = new ArrayList<Observation>();
        observations.add(obs1);
        observations.add(obs2);
        
        ObservationQuery oQuery = mockObservationQueryExecute(null, null, ObservationSubTypeEnum.AUDIO_METADATA, 
                null, null, null, null, null, null, null, null, 1, observations.subList(0, 1));

        //proto observations to return
        ObservationGen.Observation obGen1 = TerraHarvestMessageHelper.getProtoObs();
        ObservationGen.Observation obGen2 = TerraHarvestMessageHelper.getProtoObs();

        //mock converter behavior
        when(m_Converter.convertToProto(obs1)).thenReturn(obGen1);
        when(m_Converter.convertToProto(obs2)).thenReturn(obGen2);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        //capture response
        ArgumentCaptor<GetObservationResponseData> response = ArgumentCaptor.forClass(GetObservationResponseData.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.GetObservationResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);

        //check response message for proto observations
        GetObservationResponseData responseData = response.getValue();
        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeCount(), is(1));
        assertThat(responseData.getObservationNativeList(), hasItems(obGen1));
        assertThat(responseData.getObservationNativeList(), not(hasItems(obGen2)));
        
        verify(oQuery).withMaxObservations(1);
        verify(oQuery).withSubType(ObservationSubTypeEnum.AUDIO_METADATA);
        verify(oQuery).execute();
    }
    
    /**
     * Test GetObservationRequest with query of observation type and time constraints and maximum observation type.
     * Verify response is sent.
     */
    @Test
    public void testGetObservationRequestQueryObsTypeTimeMaxObs() throws IOException, ObjectConverterException
    {
        //necessary messages for request
        TimeConstraintData time = getCreatedTimeConstraint();
        Query query = Query.newBuilder().addObservationSubType(
                ObservationSubTypeEnumConverter.convertJavaEnumToProto(ObservationSubTypeEnum.AUDIO_METADATA))
                .setCreatedTimeRange(time)
                .setMaxNumberOfObs(1).build();
        
        GetObservationRequestData message = GetObservationRequestData.newBuilder().
            setObsQuery(query).build();
        
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.GetObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        Observation obs1 = mock(Observation.class);
        List<Observation> observations = new ArrayList<Observation>();
        observations.add(obs1);
        
        Date timeStart = new Date(1L);
        Date timeEnd = new Date(3L);
        ObservationQuery oQuery = mockObservationQueryExecute(null, null, ObservationSubTypeEnum.AUDIO_METADATA, 
                timeStart, timeEnd, null, null, null, null, null, null, 1, observations);

        //proto observations to return
        ObservationGen.Observation obGen1 = TerraHarvestMessageHelper.getProtoObs();

        //mock converter behavior
        when(m_Converter.convertToProto(obs1)).thenReturn(obGen1);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        //capture response
        ArgumentCaptor<GetObservationResponseData> response = ArgumentCaptor.forClass(GetObservationResponseData.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.GetObservationResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);

        //check response message for proto observations
        GetObservationResponseData responseData = response.getValue();
        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeCount(), is(1));
        assertThat(responseData.getObservationNativeList(), hasItems(obGen1));
        
        verify(oQuery).withSubType(ObservationSubTypeEnum.AUDIO_METADATA);
        verify(oQuery).withTimeCreatedRange(timeStart, timeEnd);
        verify(oQuery).withMaxObservations(1);
        verify(oQuery).execute();
    }
    
    /**
     * Verify that query with created timestamp sort order can be executed.
     * Verify response is sent.
     */
    @Test
    public void testGetObservationRequestQuerySortOrderCreated() throws IOException, ObjectConverterException
    {
        Query query = Query.newBuilder()
                .setSortField(SortField.CreatedTimestamp)
                .setSortOrder(SortOrder.Ascending)
                .build();
        
        GetObservationRequestData message = GetObservationRequestData.newBuilder().
            setObsQuery(query).build();
        
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.GetObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        Observation obs1 = mock(Observation.class);
        List<Observation> observations = new ArrayList<Observation>();
        observations.add(obs1);
        
        ObservationQuery oQuery = mockObservationQueryExecute(null, null, null, null, null, null, null,
                ObservationQuery.SortField.CreatedTimestamp, ObservationQuery.SortOrder.Ascending, null, null, null,
                observations);

        //proto observations to return
        ObservationGen.Observation obGen1 = TerraHarvestMessageHelper.getProtoObs();

        //mock converter behavior
        when(m_Converter.convertToProto(obs1)).thenReturn(obGen1);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);
        
        //capture response
        ArgumentCaptor<GetObservationResponseData> response = ArgumentCaptor.forClass(GetObservationResponseData.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.GetObservationResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);

        //check response message for proto observations
        GetObservationResponseData responseData = response.getValue();
        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeCount(), is(1));
        assertThat(responseData.getObservationNativeList(), hasItems(obGen1));
        
        verify(oQuery).withOrder(ObservationQuery.SortField.CreatedTimestamp, ObservationQuery.SortOrder.Ascending);
        verify(oQuery).execute();
    }
    
    /**
     * Verify that query with missing sort order is ignored.
     * Verify response is sent.
     */
    @Test
    public void testGetObservationRequestQuerySortOrderMissing() throws IOException, ObjectConverterException
    {
        // sort order is missing
        Query query = Query.newBuilder()
                .setSortField(SortField.CreatedTimestamp)
                .build();
        
        GetObservationRequestData message = GetObservationRequestData.newBuilder().
            setObsQuery(query).build();
        
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.GetObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        Observation obs1 = mock(Observation.class);
        List<Observation> observations = new ArrayList<Observation>();
        observations.add(obs1);
        
        ObservationQuery oQuery = mockObservationQueryExecute(null, null, null, null, null, null, null,
                ObservationQuery.SortField.CreatedTimestamp, null, null, null, null,
                observations);

        //proto observations to return
        ObservationGen.Observation obGen1 = TerraHarvestMessageHelper.getProtoObs();

        //mock converter behavior
        when(m_Converter.convertToProto(obs1)).thenReturn(obGen1);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);
        
        //capture response
        ArgumentCaptor<GetObservationResponseData> response = ArgumentCaptor.forClass(GetObservationResponseData.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.GetObservationResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);

        //check response message for proto observations
        GetObservationResponseData responseData = response.getValue();
        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeCount(), is(1));
        assertThat(responseData.getObservationNativeList(), hasItems(obGen1));
        
        verify(oQuery, never()).withOrder(Mockito.any(ObservationQuery.SortField.class),
                                          Mockito.any(ObservationQuery.SortOrder.class));
        verify(oQuery).execute();
    }
    
    /**
     * Verify that query with observed timestamp sort order can be executed.
     * Verify response is sent.
     */
    @Test
    public void testGetObservationRequestQuerySortOrderObserved() throws IOException, ObjectConverterException
    {
        Query query = Query.newBuilder()
                .setSortField(SortField.ObservedTimestamp)
                .setSortOrder(SortOrder.Descending)
                .build();
        
        GetObservationRequestData message = GetObservationRequestData.newBuilder().
            setObsQuery(query).build();
        
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.GetObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        Observation obs1 = mock(Observation.class);
        List<Observation> observations = new ArrayList<Observation>();
        observations.add(obs1);
        
        ObservationQuery oQuery = mockObservationQueryExecute(null, null, null, null, null, null, null,
                ObservationQuery.SortField.ObservedTimestamp, ObservationQuery.SortOrder.Descending, null, null, null,
                observations);

        //proto observations to return
        ObservationGen.Observation obGen1 = TerraHarvestMessageHelper.getProtoObs();

        //mock converter behavior
        when(m_Converter.convertToProto(obs1)).thenReturn(obGen1);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);
        
        //capture response
        ArgumentCaptor<GetObservationResponseData> response = ArgumentCaptor.forClass(GetObservationResponseData.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.GetObservationResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);

        //check response message for proto observations
        GetObservationResponseData responseData = response.getValue();
        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeCount(), is(1));
        assertThat(responseData.getObservationNativeList(), hasItems(obGen1));
        
        verify(oQuery).withOrder(ObservationQuery.SortField.ObservedTimestamp, ObservationQuery.SortOrder.Descending);
        verify(oQuery).execute();
    }
    
    /**
     * Verify that query with missing sort field is ignored.
     * Verify response is sent.
     */
    @Test
    public void testGetObservationRequestQuerySortFieldMissing() throws IOException, ObjectConverterException
    {
        // sort field is missing
        Query query = Query.newBuilder()
                .setSortOrder(SortOrder.Descending)
                .build();
        
        GetObservationRequestData message = GetObservationRequestData.newBuilder().
            setObsQuery(query).build();
        
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.GetObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        Observation obs1 = mock(Observation.class);
        List<Observation> observations = new ArrayList<Observation>();
        observations.add(obs1);
        
        ObservationQuery oQuery = mockObservationQueryExecute(null, null, null, null, null, null, null,
                null, ObservationQuery.SortOrder.Descending, null, null, null,
                observations);

        //proto observations to return
        ObservationGen.Observation obGen1 = TerraHarvestMessageHelper.getProtoObs();

        //mock converter behavior
        when(m_Converter.convertToProto(obs1)).thenReturn(obGen1);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);
        
        //capture response
        ArgumentCaptor<GetObservationResponseData> response = ArgumentCaptor.forClass(GetObservationResponseData.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.GetObservationResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);

        //check response message for proto observations
        GetObservationResponseData responseData = response.getValue();
        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeCount(), is(1));
        assertThat(responseData.getObservationNativeList(), hasItems(obGen1));
        
        verify(oQuery, never()).withOrder(Mockito.any(ObservationQuery.SortField.class),
                                          Mockito.any(ObservationQuery.SortOrder.class));
        verify(oQuery).execute();
    }
    
    /**
     * Verify that query for range is executed.
     * Verify that response is sent.
     */
    @Test
    public void testGetObservationRequestQueryRange() throws IOException, ObjectConverterException
    {
        Range range = Range.newBuilder().setFromInclusive(0).setToExclusive(1).build();
        Query query = Query.newBuilder().setRange(range).build();
        
        GetObservationRequestData message = GetObservationRequestData.newBuilder().
            setObsQuery(query).build();
        
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.GetObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        Observation obs1 = mock(Observation.class);
        List<Observation> observations = new ArrayList<Observation>();
        observations.add(obs1);
        
        ObservationQuery oQuery = mockObservationQueryExecute(null, null, null, 
                null, null, null, null, null, null, 0, 1, null, observations);
        
        //proto observations to return
        ObservationGen.Observation obGen1 = TerraHarvestMessageHelper.getProtoObs();

        //mock converter behavior
        when(m_Converter.convertToProto(obs1)).thenReturn(obGen1);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);
        
        //capture response
        ArgumentCaptor<GetObservationResponseData> response = ArgumentCaptor.forClass(GetObservationResponseData.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.GetObservationResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);

        //check response message for proto observations
        GetObservationResponseData responseData = response.getValue();
        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeCount(), is(1));
        assertThat(responseData.getObservationNativeList(), hasItems(obGen1));
        
        verify(oQuery).withRange(0, 1);
        verify(oQuery).execute();
    }

    /**
     * Test GetObservationRequest with query of observation type and a converter exception.
     * Verify response is sent with only one observation.
     */
    @Test
    public void testGetObservationRequest_QueryObsTypeObjException() throws IOException, ObjectConverterException
    {
        //necessary messages for request
        Query query = Query.newBuilder().addObservationSubType(
                ObservationSubTypeEnumConverter.convertJavaEnumToProto(ObservationSubTypeEnum.AUDIO_METADATA)).build();
      
        GetObservationRequestData message = GetObservationRequestData.newBuilder().
            setObsQuery(query).build();
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.GetObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        Observation obs1 = mock(Observation.class);
        Observation obs2 = mock(Observation.class);
        List<Observation> observations = new ArrayList<Observation>();
        observations.add(obs1);
        observations.add(obs2);
        
        ObservationQuery oQuery = mockObservationQueryExecute(null, null, 
                ObservationSubTypeEnum.AUDIO_METADATA, null, null, null, null,
                null, null, null, null, null, observations);

        //proto observations to return
        ObservationGen.Observation obGen = TerraHarvestMessageHelper.getProtoObs();

        //mock converter behavior
        when(m_Converter.convertToProto(obs1)).thenThrow(new ObjectConverterException("Converter exception"));
        when(m_Converter.convertToProto(obs2)).thenReturn(obGen);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        //capture response
        ArgumentCaptor<GetObservationResponseData> response = ArgumentCaptor.forClass(GetObservationResponseData.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.GetObservationResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);

        //check response message for proto observations
        GetObservationResponseData responseData = response.getValue();
        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeCount(), is(1));
        assertThat(responseData.getObservationNativeList(), hasItems(obGen));
        
        verify(oQuery).withSubType(ObservationSubTypeEnum.AUDIO_METADATA);
    }
    
    /**
     * Verify exception is thrown if requested format is invalid.
     */
    @Test
    public final void testGetObservationRequest_InvalidFormat() throws Exception
    {
        //build request
        Query query = Query.newBuilder().addObservationSubType(
                ObservationSubTypeEnumConverter.convertJavaEnumToProto(ObservationSubTypeEnum.AUDIO_METADATA)).build();
      
        GetObservationRequestData message = GetObservationRequestData.newBuilder()
                .setObservationFormat(RemoteTypesGen.LexiconFormat.Enum.XML)
                .setObsQuery(query).build();
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.GetObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        try
        {
            m_SUT.handleMessage(thMessage, payload, channel);
            fail("Expecting exception as requested format is not supported");
        }
        catch (UnsupportedOperationException e)
        {
            
        }
    }

    /**
     * Test remove observation.
     */
    @Test
    public void testRemoveObservation() throws IOException
    {
        //removal message
        Query query = Query.newBuilder().
            setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid)).build();
        RemoveObservationRequestData request = RemoveObservationRequestData.newBuilder().
            setObsQuery(query).build();
        ObservationStoreNamespace obsMessage = ObservationStoreNamespace.newBuilder().
            setData(request.toByteString()).
            setType(ObservationStoreMessageType.RemoveObservationRequest).build();
        TerraHarvestPayload payload = createPayload(obsMessage);
        TerraHarvestMessage thMessage = createMessageObservationStore(obsMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        ObservationQuery oQuery = mockObservationQueryRemove(uuid, null, null, null, null, null, null, null, 1L);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.RemoveObservationResponse), msgCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        assertThat(msgCaptor.getValue(), notNullValue());
        assertThat(((RemoveObservationResponseData)msgCaptor.getValue()).getNumberOfObsRemoved(), is(1L));
        
        verify(oQuery).withAssetUuid(uuid);
    }
    
    /**
     * Test remove observation with query with no parameters set. Verify observations still removed.
     * Verify response is sent.
     */
    @Test
    public void testRemoveObservationAll() throws IOException
    {
        //removal message
        Query query = Query.getDefaultInstance();
        RemoveObservationRequestData request = RemoveObservationRequestData.newBuilder().
            setObsQuery(query).build();
        ObservationStoreNamespace obsMessage = ObservationStoreNamespace.newBuilder().
            setData(request.toByteString()).
            setType(ObservationStoreMessageType.RemoveObservationRequest).build();
        TerraHarvestPayload payload = createPayload(obsMessage);
        TerraHarvestMessage thMessage = createMessageObservationStore(obsMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        ObservationQuery oQuery = mockObservationQueryRemove(null, null, null, null, null, null, null, null, 1L);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.RemoveObservationResponse), msgCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        assertThat(msgCaptor.getValue(), notNullValue());
        assertThat(((RemoveObservationResponseData)msgCaptor.getValue()).getNumberOfObsRemoved(), is(1L));
        
        verify(oQuery, never()).withAssetUuid(Mockito.any(UUID.class));
        verify(oQuery, never()).withAssetType(Mockito.anyString());
        verify(oQuery, never()).withMaxObservations(Mockito.anyInt());
        verify(oQuery, never()).withOrder(Mockito.any(ObservationQuery.SortField.class),
                                          Mockito.any(ObservationQuery.SortOrder.class));
        verify(oQuery, never()).withRange(Mockito.anyInt(), Mockito.anyInt());
        verify(oQuery, never()).withTimeCreatedRange(Mockito.any(Date.class), Mockito.any(Date.class));
        verify(oQuery, never()).withSubType(Mockito.any(ObservationSubTypeEnum.class));
    }

    /**
     * Test remove observation by UUID.
     */
    @Test

    public void testRemoveObservationByUUID() throws IOException
    {
        //removal message
        RemoveObservationByUUIDRequestData request = RemoveObservationByUUIDRequestData.newBuilder().
            addUuidOfObservation(SharedMessageUtils.convertUUIDToProtoUUID(uuidObs1)).
            addUuidOfObservation(SharedMessageUtils.convertUUIDToProtoUUID(uuidObs2)).build();
        ObservationStoreNamespace obsMessage = ObservationStoreNamespace.newBuilder().
            setData(request.toByteString()).
            setType(ObservationStoreMessageType.RemoveObservationByUUIDRequest).build();
        TerraHarvestPayload payload = createPayload(obsMessage);
        TerraHarvestMessage thMessage = createMessageObservationStore(obsMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.RemoveObservationByUUIDResponse), (Message) eq(null));
        verify(m_ResponseWrapper).queue(channel);

        //verify obs store calls
        verify(m_ObservationStore).remove(uuidObs1);
        verify(m_ObservationStore).remove(uuidObs2);
    }

    /**

     * Test find observation by UUID request.
     * 
     * Verify that observations are converted and set back in response.
     */
    @Test
    public void testFindByUUID() throws IOException, ObjectConverterException
    {
        //obs UUIDs
        UUID obsUuid1 = UUID.randomUUID();
        UUID obsUuid2 = UUID.randomUUID();

        //removal message
        FindObservationByUUIDRequestData request = FindObservationByUUIDRequestData.newBuilder().
            addUuidOfObservation(SharedMessageUtils.convertUUIDToProtoUUID(obsUuid1)).
            addUuidOfObservation(SharedMessageUtils.convertUUIDToProtoUUID(obsUuid2)).build();
        ObservationStoreNamespace obsMessage = ObservationStoreNamespace.newBuilder().
            setData(request.toByteString()).
            setType(ObservationStoreMessageType.FindObservationByUUIDRequest).build();
        TerraHarvestPayload payload = createPayload(obsMessage);
        TerraHarvestMessage thMessage = createMessageObservationStore(obsMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mocks
        Observation obsJaxb1 = mock(Observation.class);
        Observation obsJaxb2 = mock(Observation.class);

        ObservationGen.Observation obsGen1 = TerraHarvestMessageHelper.getProtoObs();
        ObservationGen.Observation obsGen2 = TerraHarvestMessageHelper.getProtoObs();

        //mock behavior
        when(m_ObservationStore.find(obsUuid1)).thenReturn(obsJaxb1);
        when(m_ObservationStore.find(obsUuid2)).thenReturn(obsJaxb2);
        when(m_Converter.convertToProto(obsJaxb1)).thenReturn(obsGen1);
        when(m_Converter.convertToProto(obsJaxb2)).thenReturn(obsGen2);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        //capture response
        ArgumentCaptor<FindObservationByUUIDResponseData> response = 
            ArgumentCaptor.forClass(FindObservationByUUIDResponseData.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.FindObservationByUUIDResponse), response.capture());
        verify(m_ResponseWrapper).queue(channel);

        //check response message for proto observations
        FindObservationByUUIDResponseData responseData = response.getValue();
        assertThat(responseData.getObservationFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(responseData.getObservationNativeList(), hasItems(obsGen1, obsGen2));
    }
    
    /**
     * Verify exception is thrown if requested format is invalid.
     */
    @Test
    public final void testFindByUUID_InvalidFormat() throws Exception
    {
        //build request
        FindObservationByUUIDRequestData request = FindObservationByUUIDRequestData.newBuilder()
                .setObservationFormat(RemoteTypesGen.LexiconFormat.Enum.XML).build();
        ObservationStoreNamespace obsMessage = ObservationStoreNamespace.newBuilder().
            setData(request.toByteString()).
            setType(ObservationStoreMessageType.FindObservationByUUIDRequest).build();
        TerraHarvestPayload payload = createPayload(obsMessage);
        TerraHarvestMessage thMessage = createMessageObservationStore(obsMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        // replay
        try
        {
            m_SUT.handleMessage(thMessage, payload, channel);
            fail("Expecting exception as requested format is not supported");
        }
        catch (UnsupportedOperationException e)
        {
            
        }
    }

    /**
     * Test remove observation request with query of asset UUID.
     * Verify response is sent.
     */
    @Test
    public void testRemoveObservationRequestQuery() throws IOException, ObjectConverterException
    {
        //necessary messages for request
        Query query = Query.newBuilder().
            setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid)).build();
        RemoveObservationRequestData message = RemoveObservationRequestData.newBuilder().
            setObsQuery(query).build();
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.RemoveObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        Observation obs1 = mock(Observation.class);
        when(obs1.getUuid()).thenReturn(uuidObs1);
        Observation obs2 = mock(Observation.class);
        when(obs2.getUuid()).thenReturn(uuidObs2);
        List<Observation> observations = new ArrayList<Observation>();
        observations.add(obs1);
        observations.add(obs2);
        
        ObservationQuery oQuery = mockObservationQueryRemove(uuid, null, null, null, null, null, null, null, 2L);
        
        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.RemoveObservationResponse), msgCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        assertThat(msgCaptor.getValue(), notNullValue());
        assertThat(((RemoveObservationResponseData)msgCaptor.getValue()).getNumberOfObsRemoved(), is(2L));
        
        verify(oQuery).withAssetUuid(uuid);
        verify(oQuery).remove();
    }

    /**
     * Test remove observation request by asset UUID with time constraints.
     * Verify response is sent.
     */
    @Test
    public void testRemoveObservationRequestAssetQueryTime() throws IOException, ObjectConverterException
    {
        //necessary messages for request
        TimeConstraintData constraint = getCreatedTimeConstraint();
        Query query = Query.newBuilder().setAssetUuid(
                SharedMessageUtils.convertUUIDToProtoUUID(uuid)).setCreatedTimeRange(constraint).build();
        
        RemoveObservationRequestData message = RemoveObservationRequestData.newBuilder().
            setObsQuery(query).build();
        
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.RemoveObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        Date timeStart = new Date(1L);
        Date timeEnd = new Date(3L);
        //mock observation that will be returned from obs store
        ObservationQuery oQuery = mockObservationQueryRemove(uuid, null, 
                null, timeStart, timeEnd, null, null, null, 2L);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.RemoveObservationResponse), msgCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        assertThat(msgCaptor.getValue(), notNullValue());
        assertThat(((RemoveObservationResponseData)msgCaptor.getValue()).getNumberOfObsRemoved(), is(2L));
        
        verify(oQuery).withAssetUuid(uuid);
        verify(oQuery).withTimeCreatedRange(timeStart, timeEnd);
        verify(oQuery).remove();
    }

    /**
     * Test remove observation request by asset type with time constraints.
     * Verify response is sent.
     */
    @Test
    public void testRemoveObservationRequestAssetTypeQueryTime() throws IOException, ObjectConverterException
    {
        //necessary messages for request
        TimeConstraintData time = getCreatedTimeConstraint();
        Query query = Query.newBuilder().setAssetType("a.new.Asset").setCreatedTimeRange(time).build();
        RemoveObservationRequestData message = RemoveObservationRequestData.newBuilder().
            setObsQuery(query).build();
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.RemoveObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        Date timeStart = new Date(1L);
        Date timeEnd = new Date(3L);
        ObservationQuery oQuery = mockObservationQueryRemove(null, "a.new.Asset", 
                null, timeStart, timeEnd, null, null, null, 2);
        
        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.RemoveObservationResponse), msgCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        assertThat(msgCaptor.getValue(), notNullValue());
        assertThat(((RemoveObservationResponseData)msgCaptor.getValue()).getNumberOfObsRemoved(), is(2L));
        
        verify(oQuery).withAssetType("a.new.Asset");
        verify(oQuery).withTimeCreatedRange(timeStart, timeEnd);
        verify(oQuery).remove();
    }

    /**
     * Test remove observation request with search criteria of asset type.
     * Verify response is sent.
     */
    @Test
    public void testRemoveObservationRequestAssetType() throws IOException, ObjectConverterException
    {
        //necessary messages for request
        Query query = Query.newBuilder().setAssetType("a.new.Asset").build();
        RemoveObservationRequestData message = RemoveObservationRequestData.newBuilder().
            setObsQuery(query).build();
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.RemoveObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        ObservationQuery oQuery = mockObservationQueryRemove(null, 
                "a.new.Asset", null, null, null, null, null, null, 2);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.RemoveObservationResponse), msgCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        assertThat(msgCaptor.getValue(), notNullValue());
        assertThat(((RemoveObservationResponseData)msgCaptor.getValue()).getNumberOfObsRemoved(), is(2L));
        
        verify(oQuery).withAssetType("a.new.Asset");
        verify(oQuery).remove();
    }

    /**
     * Test remove observation request by observation type.
     * Verify response is sent.
     */
    @Test
    public void testRemoveObservationRequestObservationType() throws IOException, ObjectConverterException
    {
        //necessary messages for request
        Query query = Query.newBuilder().
            addObservationSubType(ObservationSubTypeEnumConverter.convertJavaEnumToProto(
                    ObservationSubTypeEnum.DETECTION))
            .build();
        RemoveObservationRequestData message = RemoveObservationRequestData.newBuilder().
            setObsQuery(query).build();
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.RemoveObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        ObservationQuery oQuery = mockObservationQueryRemove(null, null, 
                ObservationSubTypeEnum.DETECTION, null, null, null, null, null, 2L);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.RemoveObservationResponse), msgCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        assertThat(msgCaptor.getValue(), notNullValue());
        assertThat(((RemoveObservationResponseData)msgCaptor.getValue()).getNumberOfObsRemoved(), is(2L));
        
        verify(oQuery).withSubType(ObservationSubTypeEnum.DETECTION);
        verify(oQuery).remove();
    }
    /**
     * Test remove observation request with action of observation type and time constraints.
     * Verify response is sent.
     */
    @Test
    public void testRemoveObservationRequestQueryObsTypeTime() throws IOException, ObjectConverterException
    {
        //necessary messages for request
        TimeConstraintData time = getCreatedTimeConstraint();
        Query query = Query.newBuilder().
                addObservationSubType(
                        ObservationSubTypeEnumConverter.convertJavaEnumToProto(ObservationSubTypeEnum.AUDIO_METADATA))
                .setCreatedTimeRange(time).build();
        
        RemoveObservationRequestData message = RemoveObservationRequestData.newBuilder().
            setObsQuery(query).build();
        
        ObservationStoreNamespace namespace = ObservationStoreNamespace.newBuilder().
            setData(message.toByteString()).
            setType(ObservationStoreMessageType.RemoveObservationRequest).build();
        TerraHarvestPayload payload = createPayload(namespace);
        TerraHarvestMessage thMessage = createMessageObservationStore(namespace);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //mock observation that will be returned from obs store
        Date timeStart = new Date(1L);
        Date timeEnd = new Date(3L);
        ObservationQuery oQuery = mockObservationQueryRemove(null, null, ObservationSubTypeEnum.AUDIO_METADATA, 
                timeStart, timeEnd, null, null, null, 2);

        // replay
        m_SUT.handleMessage(thMessage, payload, channel);

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(m_MessageFactory).createObservationStoreResponseMessage(eq(thMessage), 
            eq(ObservationStoreMessageType.RemoveObservationResponse), msgCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        assertThat(msgCaptor.getValue(), notNullValue());
        assertThat(((RemoveObservationResponseData)msgCaptor.getValue()).getNumberOfObsRemoved(), is(2L));
        
        verify(oQuery).withSubType(ObservationSubTypeEnum.AUDIO_METADATA);
        verify(oQuery).withTimeCreatedRange(timeStart, timeEnd);
        verify(oQuery).remove();
    }

    /**
     * Create time constraint message.
     */
    private TimeConstraintData getCreatedTimeConstraint()
    {
        return TimeConstraintData.newBuilder().setStartTime(1L).setStopTime(3L).build();
    }

    /**
     * Create different time constraint message than from above.
     */
    private TimeConstraintData getObservedTimeConstraint()
    {
        return TimeConstraintData.newBuilder().setStartTime(1L).setStopTime(2L).build();
    }
    
    /**
     * Create a TerraHarvestMessage with the observation namespace.
     */
    private TerraHarvestMessage createMessageObservationStore(ObservationStoreNamespace obsMessage)
    {
        return TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.ObservationStore, 3, obsMessage);
    }
    
    private TerraHarvestPayload createPayload(ObservationStoreNamespace obsMessage)
    {
        return TerraHarvestPayload.newBuilder().
               setNamespace(Namespace.ObservationStore).
               setNamespaceMessage(obsMessage.toByteString()).
               build();
    }
    
    /**
     * Mocks an ObservationQuery based on the inputs given and makes sure that the mocked object is returned
     * from the observation store when m_ObservationStore.newQuery() is called. Also ensures that the 
     * observation list passed in is returned when query.execute() is called. If a field is not to be set 
     * then null should be passed to the method. This method makes no checks on what are 
     * the valid combinations of query fields. 
     * 
     * @param assetUuid
     *  uuid of the asset; null if it is not to be set
     * @param type
     *  the type of the asset; null if it is not to be set
     * @param start
     *  the start date object for the created time range; null if not to be set
     * @param stop
     *  the stop date object for the created time range; null if not to be set
     * @param observedStart
     *  the start date object for the observed time range; null if not to be set
     * @param observedStop
     *  the stop date object for the observed time range; null if not to be set
     * @param field
     *  the sorting field of the query
     * @param order
     *  the sorting order of the query
     * @param startRange
     *  the start of the range of observations to return
     * @param endRange
     *  the end of the range of observations to return
     * @param maxObs
     *  the max number of observations; null if not to be set
     * @param obs
     *  the observations that should be returned when query.execute() is called.
     * @return
     *  the mocked observation query object
     */
    private ObservationQuery mockObservationQueryExecute(UUID assetUuid, String type, ObservationSubTypeEnum subtype,
            Date start, Date stop, Date observedStart, Date observedStop, ObservationQuery.SortField field,
            ObservationQuery.SortOrder order, Integer startRange, Integer endRange, Integer maxObs,
            List<Observation> obs)
    {
        ObservationQuery query = mockObservationQuery(assetUuid, type, subtype, start, stop, observedStart, 
                observedStop, maxObs);
        
        if (field != null && order != null)
        {
            when(query.withOrder(field, order)).thenReturn(query);
        }
        
        if (startRange != null && endRange != null)
        {
            when(query.withRange(startRange, endRange)).thenReturn(query);
        }
        
        when(query.execute()).thenReturn(obs);
        
        return query;
    }
    
    /**
     * Mocks an ObservationQuery based on the inputs given and makes sure that the mocked object is returned
     * from the observation store when m_ObservationStore.newQuery() is called. Also ensures that the 
     * number of obs removed passed in is returned when query.remove() is called. If a field is not to be set 
     * then null should be passed to the method. This method makes no checks on what are 
     * the valid combinations of query fields. 
     * 
     * @param assetUuid
     *  uuid of the asset; null if it is not to be set
     * @param type
     *  the type of the asset; null if it is not to be set
     * @param start
     *  the start date object for the created time range; null if not to be set
     * @param stop
     *  the stop date object for the created time range; null if not to be set
     * @param observedStart
     *  the start date object for the observed time range; null if not to be set
     * @param observedStop
     *  the stop date object for the observed time range; null if not to be set
     * @param maxObs
     *  the max number of observations; null if not to be set
     * @param numRemoved
     *  the number of observations that were removed when query.remove() is called
     * @return
     *  the observation query
     */
    private ObservationQuery mockObservationQueryRemove(UUID assetUuid, String type, ObservationSubTypeEnum subtype,  
            Date start, Date stop, Date observedStart, Date observedStop, Integer maxObs, long numRemoved)
    {
        ObservationQuery query = mockObservationQuery(assetUuid, type, subtype, start, stop, 
                observedStart, observedStop, maxObs);
        when(query.remove()).thenReturn(numRemoved);
        
        return query;
    }
    
    /**
     * Mocks an ObservationQuery based on the inputs given. 
     * uuid of the asset; null if it is not to be set
     * @param type
     *  the type of the asset; null if it is not to be set
     * @param start
     *  the start date object for a time range; null if not to be set
     * @param stop
     *  the stop date object for a time range; null if not to be set
     * @param observedStart
     *  the start date object for the observed time range; null if not to be set
     * @param observedStop
     *  the stop date object for the observed time range; null if not to be set
     * @param maxObs
     *  the max number of observations; null if not to be set
     * @return
     *  the observation query
     */
    private ObservationQuery mockObservationQuery(UUID assetUuid, String type, ObservationSubTypeEnum subtype,  
            Date start, Date stop, Date observedStart, Date observedStop, Integer maxObs)
    {
        ObservationQuery query = mock(ObservationQuery.class);
        
        when(m_ObservationStore.newQuery()).thenReturn(query);
        
        if (uuid != null)
        {
            when(query.withAssetUuid(assetUuid)).thenReturn(query);
        }
        
        if (type != null)
        {
            when(query.withAssetType(type)).thenReturn(query);
        }
        
        if (subtype != null)
        {
            when(query.withSubType(subtype)).thenReturn(query);
        }
        
        if (start != null && stop != null)
        {
            when(query.withTimeCreatedRange(start, stop)).thenReturn(query);
        }

        if (observedStart != null && observedStop != null)
        {
            when(query.withTimeObservedRange(observedStart, observedStop)).thenReturn(query);
        }
        
        if (maxObs != null)
        {
            when(query.withMaxObservations(maxObs)).thenReturn(query);
        }
        
        return query;
    }
}
