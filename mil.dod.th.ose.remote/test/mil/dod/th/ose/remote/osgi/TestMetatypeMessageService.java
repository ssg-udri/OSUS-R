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
package mil.dod.th.ose.remote.osgi;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageResponseWrapper;
import mil.dod.th.core.remote.proto.MetaTypeMessages.AttributeDefinitionType;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetAttributeDefinitionRequestData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetAttributeDefinitionResponseData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetAttributeKeysRequestData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetAttributeKeysResponseData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetBundlePidsRequestData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetBundlePidsResponseData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetMetaTypeInfoRequestData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetMetaTypeInfoResponseData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.MetaTypeInfoType;
import mil.dod.th.core.remote.proto.MetaTypeMessages.MetaTypeNamespace;
import mil.dod.th.core.remote.proto.MetaTypeMessages.ObjectClassDefinitionType;
import mil.dod.th.core.remote.proto.MetaTypeMessages.MetaTypeNamespace.MetaTypeMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.TerraHarvestMessageHelper;
import mil.dod.th.ose.remote.messaging.TerraHarvestMessageUtil;
import mil.dod.th.ose.remote.util.MetatypeInformationListener;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

import com.google.protobuf.Message;

/**
 * Test that the MetaTypeMessage service correctly handles messages, and that the expected responses are sent.
 * @author callen
 *
 */
public class TestMetatypeMessageService 
{
    private MetatypeMessageService m_SUT;
    private MessageFactory m_MessageFactory;
    private EventAdmin m_EventAdmin;
    private MetaTypeService m_MetaTypeService;
    private BundleContext m_Context;
    private MessageRouterInternal m_MessageRouter;
    private MetatypeInformationListener m_MetaListener;
    private MessageResponseWrapper m_ResponseWrapper;
    
    @Before
    public void setUp() throws Exception
    {
        //initialization of members
        m_SUT = new MetatypeMessageService();
        m_EventAdmin = mock(EventAdmin.class);
        m_MetaTypeService = mock(MetaTypeService.class);
        m_MessageFactory = mock(MessageFactory.class);
        m_ResponseWrapper = mock(MessageResponseWrapper.class);
        m_Context = mock(BundleContext.class);
        m_MessageRouter = mock(MessageRouterInternal.class);
        m_MetaListener = mock(MetatypeInformationListener.class);
        
        //set services
        m_SUT.setMetaTypeService(m_MetaTypeService);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setMessageRouter(m_MessageRouter);
        m_SUT.setMetatypeInformationListener(m_MetaListener);
        
        //activate the component
        m_SUT.activate(m_Context);
        
        when(m_MessageFactory.createMetaTypeResponseMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(MetaTypeMessageType.class), Mockito.any(Message.class))).thenReturn(m_ResponseWrapper);
        when(m_MessageFactory.createBaseErrorMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(ErrorCode.class), Mockito.anyString())).thenReturn(m_ResponseWrapper);
    }
    
    /**
     * Verify message service is registered on activation and unregistered on deactivation.
     */
    @Test
    public void testActivateDeactivate()
    {
        // verify service is bound
        verify(m_MessageRouter).bindMessageService(m_SUT);
        
        m_SUT.deactivate();
        
        // verify service is unbound
        verify(m_MessageRouter).unbindMessageService(m_SUT);
    }

    /**
     * Test getting the namespace.
     * 
     * Verify the namespace is MetaType.
     */
    @Test
    public void testGetNamespace()
    {
        assertThat(m_SUT.getNamespace(), is(Namespace.MetaType));
    }
    
    /**
     * Test that the message service posts events in response to messages received.
     * 
     * Verify generic handling of a message.
     * 
     * Verify events are correctly posted for all response message types.
     */
    @Test
    public void testGenericHandleMessage() throws IOException
    {
        GetAttributeKeysResponseData responseMessage = GetAttributeKeysResponseData.newBuilder().setBundleId(5L).
                setPid("test-pid").build();
        
        //construct a single meta type namespace message to verify handling of meta type message service
        MetaTypeNamespace namespaceMessage = MetaTypeNamespace.newBuilder().
            setType(MetaTypeMessageType.GetAttributeKeyResponse).setData(responseMessage.toByteString()).build();
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().
                setNamespace(Namespace.MetaType).
                setNamespaceMessage(namespaceMessage.toByteString()).
                build();
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.MetaType, 100,
                namespaceMessage);

        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.MetaType.
            toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(namespaceMessage.getType().toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((MetaTypeNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(namespaceMessage));
        GetAttributeKeysResponseData dataMessage = 
                (GetAttributeKeysResponseData)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE);
        assertThat(dataMessage.getBundleId(), is(5L));
        assertThat(dataMessage.getPid(), is("test-pid"));
        
        //construct a single meta type namespace message to verify handling of meta type message service
        GetAttributeDefinitionResponseData responseDefintion = 
                GetAttributeDefinitionResponseData.newBuilder().setBundleId(5L).setPid("test-pid").build();
        namespaceMessage = MetaTypeNamespace.newBuilder().setType(MetaTypeMessageType.GetAttributeDefinitionResponse).
                setData(responseDefintion.toByteString()).build();
        payload = TerraHarvestPayload.newBuilder().
                setNamespace(Namespace.MetaType).
                setNamespaceMessage(namespaceMessage.toByteString()).
                build();
        message = TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.MetaType, 100, namespaceMessage);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.MetaType.
                toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(namespaceMessage.getType().toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((MetaTypeNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(namespaceMessage));
        GetAttributeDefinitionResponseData dataMessage2 = 
                (GetAttributeDefinitionResponseData)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE);
        assertThat(dataMessage2.getBundleId(), is(5L));
        assertThat(dataMessage2.getPid(), is("test-pid"));
        
        //construct a single meta type namespace message to verify handling of meta type message service
        GetAttributeKeysResponseData responseKeys = 
                GetAttributeKeysResponseData.newBuilder().setBundleId(5L).setPid("test-pid").build();
        namespaceMessage = MetaTypeNamespace.newBuilder().setType(MetaTypeMessageType.GetAttributeKeyResponse).
                setData(responseKeys.toByteString()).build();
        payload = TerraHarvestPayload.newBuilder().
                setNamespace(Namespace.MetaType).
                setNamespaceMessage(namespaceMessage.toByteString()).
                build();
        message = TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.MetaType, 100, namespaceMessage);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted
        verify(m_EventAdmin, times(3)).postEvent(eventCaptor.capture());
        postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.MetaType.
                toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(namespaceMessage.getType().toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((MetaTypeNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(namespaceMessage));
        GetAttributeKeysResponseData dataMessage3 = 
                (GetAttributeKeysResponseData)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE);
        assertThat(dataMessage3.getBundleId(), is(5L));
        assertThat(dataMessage3.getPid(), is("test-pid"));
        
        //Construct meta type info response message.
        AttributeDefinitionType attrDetType = AttributeDefinitionType.newBuilder().setAttributeType(5).
                setCardinality(1).setDescription("some description").setId("blah").setName("rawr!").build();
        MetaTypeInfoType metaTypeInfo = MetaTypeInfoType.newBuilder().setBundleId(5L).setIsFactory(false).
                setPid("some.pid").addAttributes(attrDetType).setOcd(
                        ObjectClassDefinitionType.newBuilder().setDescription("blah").setId("ID").setName("name")
                        .build())
                .build();
        GetMetaTypeInfoResponseData respnoseMetaInfo = GetMetaTypeInfoResponseData.newBuilder().
                addMetaType(metaTypeInfo).build();
        namespaceMessage = MetaTypeNamespace.newBuilder().setType(MetaTypeMessageType.GetMetaTypeInfoResponse).
                setData(respnoseMetaInfo.toByteString()).build();
        payload = TerraHarvestPayload.newBuilder().
                setNamespace(Namespace.MetaType).
                setNamespaceMessage(namespaceMessage.toByteString()).
                build();
        message = TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.MetaType, 100, namespaceMessage);
        
        // handle the message
        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted
        verify(m_EventAdmin, times(4)).postEvent(eventCaptor.capture());
        postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.MetaType.
                toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(namespaceMessage.getType().toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((MetaTypeNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(namespaceMessage));
        GetMetaTypeInfoResponseData dataMessage4 = 
                (GetMetaTypeInfoResponseData)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE);
        List<MetaTypeInfoType> metaTypeList = dataMessage4.getMetaTypeList();
        assertThat(metaTypeList.size(), is(1));
        assertThat(metaTypeList.get(0).getOcd().getName(), is("name"));
        assertThat(metaTypeList.get(0).getOcd().getId(), is("ID"));
        assertThat(metaTypeList.get(0).getOcd().getDescription(), is("blah"));
        assertThat(metaTypeList.get(0).getPid(), is("some.pid"));
        assertThat(metaTypeList.get(0).getBundleId(), is(5L));
        assertThat(metaTypeList.get(0).getIsFactory(), is(false));
        List<AttributeDefinitionType> attributeList = metaTypeList.get(0).getAttributesList();
        assertThat(attributeList.size(), is(1));
        assertThat(attributeList.get(0).getCardinality(), is(1));
        assertThat(attributeList.get(0).getAttributeType(), is(5));
        assertThat(attributeList.get(0).getDescription(), is("some description"));
        assertThat(attributeList.get(0).getId(), is("blah"));
        assertThat(attributeList.get(0).getName(), is("rawr!"));
    }

    /**
     * Test that if the bundle id is not found that the GetBundlePidsRequest is properly handled.
     * 
     * Verify INVALID_VALUE is sent with the response.
     * 
     * Verify event is posted with correct data message.
     */
    @Test
    public final void testGetBundlePidsRequestInvalidId() throws IOException
    {
        Bundle bundleA = mock(Bundle.class);
        when(bundleA.getBundleId()).thenReturn(1L);
        Bundle bundleB = mock(Bundle.class);
        when(bundleB.getBundleId()).thenReturn(2L);
        when(m_Context.getBundles()).thenReturn(new Bundle[] { bundleA, bundleB,});
        
        MetaTypeInformation infoA = mock(MetaTypeInformation.class);
        when(infoA.getPids()).thenReturn(new String[] {"configA", "configF"});
        MetaTypeInformation infoB = mock(MetaTypeInformation.class);
        when(infoB.getPids()).thenReturn(new String[] {});
        
        when(m_MetaTypeService.getMetaTypeInformation(bundleA)).thenReturn(infoA);
        when(m_MetaTypeService.getMetaTypeInformation(bundleB)).thenReturn(infoB);
        
        // replay/verify
        GetBundlePidsRequestData request = GetBundlePidsRequestData.newBuilder().setBundleId(0).build();
        TerraHarvestPayload payload = createPayload(request, MetaTypeMessageType.GetBundlePidsRequest);
        TerraHarvestMessage message = createTerraHarvestMessage(request, MetaTypeMessageType.GetBundlePidsRequest);

        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);

        //capture the message, verify correct error code
        verify(m_EventAdmin).postEvent(Mockito.any(Event.class));
        verify(m_MessageFactory).createBaseErrorMessage(eq(message), eq(ErrorCode.INVALID_VALUE), Mockito.anyString());
        verify(m_ResponseWrapper).queue(channel);

        // verify event is posted for pid request
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.MetaType.
                toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(MetaTypeMessageType.GetBundlePidsRequest.toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((MetaTypeNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(getNamespaceMessage(message)));
    }

    /**
     * Test that if the bundle id is found that the GetBundlePidsRequest is properly handled.
     * 
     * Verify lists of bundle Pids are returned with correct pids types.
     * 
     */
    @Test
    public void testGetBundlePidsRequestValid() throws IOException
    {
        //mock bundles
        Bundle bundleA = mock(Bundle.class);
        when(bundleA.getBundleId()).thenReturn(1L);
        Bundle bundleB = mock(Bundle.class);
        when(bundleB.getBundleId()).thenReturn(2L);
        Bundle bundleC = mock(Bundle.class);
        when(bundleC.getBundleId()).thenReturn(3L);
        Bundle bundleD = mock(Bundle.class);
        when(bundleD.getBundleId()).thenReturn(4L);
        when(m_Context.getBundles()).thenReturn(new Bundle[] { bundleA, bundleC, bundleB, bundleD});
        //mock meta type information for the bundles
        MetaTypeInformation infoA = mock(MetaTypeInformation.class);
        when(infoA.getPids()).thenReturn(new String[] {"configA", "configF"});
        when(infoA.getFactoryPids()).thenReturn(new String[] {"FactoryconfigA", "FactoryconfigF"});
        MetaTypeInformation infoB = mock(MetaTypeInformation.class);
        when(infoB.getPids()).thenReturn(new String[] {});
        when(infoB.getFactoryPids()).thenReturn(new String[] {"FactoryConfigG"});
        MetaTypeInformation infoC = mock(MetaTypeInformation.class);
        when(infoC.getPids()).thenReturn(new String[] {"configZ", "configD"});
        when(infoC.getFactoryPids()).thenReturn(new String[] {});
        //behavior for mocked meta information
        when(m_MetaTypeService.getMetaTypeInformation(bundleA)).thenReturn(infoA);
        when(m_MetaTypeService.getMetaTypeInformation(bundleB)).thenReturn(infoB);
        when(m_MetaTypeService.getMetaTypeInformation(bundleC)).thenReturn(infoC);
        
        //replay/verify
        GetBundlePidsRequestData request = GetBundlePidsRequestData.newBuilder().setBundleId(1).build();
        TerraHarvestPayload payload = createPayload(request, MetaTypeMessageType.GetBundlePidsRequest);
        TerraHarvestMessage message = createTerraHarvestMessage(request, MetaTypeMessageType.GetBundlePidsRequest);

        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);

        //capture the message
        ArgumentCaptor<GetBundlePidsResponseData> captor = ArgumentCaptor.forClass(GetBundlePidsResponseData.class);
        verify(m_MessageFactory).createMetaTypeResponseMessage(eq(message), 
            eq(MetaTypeMessageType.GetBundlePidsResponse), 
                captor.capture());
        verify(m_ResponseWrapper).queue(channel);

        GetBundlePidsResponseData response = captor.getValue();
        //verify that lists of pids is not empty        
        assertThat(response.getPidsCount(), is(2));
        assertThat(response.getPidsList(), hasItem("configA"));
        assertThat(response.getPidsList(), hasItem("configF"));
        assertThat(response.getBundleId(), is(1L));
        //verify that factory pids list is empty
        assertThat(response.getFactoryPidsCount(), is(2));
        assertThat(response.getFactoryPidsList(), hasItem("FactoryconfigA"));
        assertThat(response.getFactoryPidsList(), hasItem("FactoryconfigF"));

        //new request with different bundle id
        request = GetBundlePidsRequestData.newBuilder().setBundleId(2).build();
        payload = createPayload(request, MetaTypeMessageType.GetBundlePidsRequest);
        message = createTerraHarvestMessage(request, MetaTypeMessageType.GetBundlePidsRequest);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);

        //capture the message
        verify(m_MessageFactory).createMetaTypeResponseMessage(eq(message), 
            eq(MetaTypeMessageType.GetBundlePidsResponse), 
                captor.capture());
        //reused channel
        verify(m_ResponseWrapper, times(2)).queue(channel);

        //parse response
        response = captor.getValue();
        //verify that lists of pids is empty  
        assertThat(response.getBundleId(), is(2L));
        assertThat(response.getPidsCount(), is(0));
        //verify that factory pids list is NOT empty
        assertThat(response.getFactoryPidsCount(), is(1));
        assertThat(response.getFactoryPidsList(), hasItem("FactoryConfigG"));
        
        //new request with different bundle id
        request = GetBundlePidsRequestData.newBuilder().setBundleId(3).build();
        payload = createPayload(request, MetaTypeMessageType.GetBundlePidsRequest);
        message = createTerraHarvestMessage(request, MetaTypeMessageType.GetBundlePidsRequest);

        //handle message
        m_SUT.handleMessage(message, payload, channel);

        //capture the message
        verify(m_MessageFactory).createMetaTypeResponseMessage(eq(message), 
            eq(MetaTypeMessageType.GetBundlePidsResponse), 
                captor.capture());
        verify(m_ResponseWrapper, times(3)).queue(channel);
        response = captor.getValue();
        //verify that lists of pids is not empty
        assertThat(response.getBundleId(), is(3L));
        assertThat(response.getPidsCount(), is(2));
        assertThat(response.getPidsList(), hasItem("configZ"));
        assertThat(response.getPidsList(), hasItem("configD"));
        //verify that factory pids list is empty
        assertThat(response.getFactoryPidsCount(), is(0));
    }
    
    /**
     * Verify get bundle pids response sends an event, and that the messages in the event are correct.
     */
    @Test
    public void testGetBundlePidsResponse() throws IOException
    {
        GetBundlePidsResponseData getBungles = GetBundlePidsResponseData.newBuilder().
                setBundleId(1L)
                .build();
        
        MetaTypeNamespace message = MetaTypeNamespace.newBuilder().setType(MetaTypeMessageType.GetBundlePidsResponse).
                setData(getBungles.toByteString()).
                build();
        
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().
               setNamespace(Namespace.MetaType).
               setNamespaceMessage(message.toByteString()).
               build();
        
        TerraHarvestMessage thMessage = TerraHarvestMessageUtil.getPartialMessage().
                setSourceId(0).
                setDestId(1).
                setTerraHarvestPayload(payload.toByteString()).
                setMessageId(100).
                build();
        
        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // handle the message
        m_SUT.handleMessage(thMessage, payload, channel);

        //capture the message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(thMessage));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.MetaType.
                toString()));
        
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(MetaTypeMessageType.GetBundlePidsResponse.toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((MetaTypeNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(getNamespaceMessage(thMessage)));
    }

    /**
     * Verify that if bundle id/pid does not return attributes and no key value is set that the 
     * GetAttributeDefinitionRequest is properly handled.
     */
    @Test
    public void testGetAttributeDefinitionInvalidAndNoKey() throws IOException
    {
        Bundle bundleA = mock(Bundle.class);
        when(bundleA.getBundleId()).thenReturn(1L);
        when(m_Context.getBundles()).thenReturn(new Bundle[] { bundleA });
        
        //mock meta type information for the bundles
        MetaTypeInformation infoA = mock(MetaTypeInformation.class);
        when(infoA.getPids()).thenReturn(new String[] {"configA", "configF"});
        when(infoA.getFactoryPids()).thenReturn(new String[] {"FactoryconfigA", "FactoryconfigF"});
        
        //testing not found bundle/pid
        ObjectClassDefinition ocdBad  = mock(ObjectClassDefinition.class);
        when(infoA.getObjectClassDefinition("Swan", null)).thenReturn(ocdBad);
        
        //testing not found bundle/pid
        GetAttributeDefinitionRequestData request = GetAttributeDefinitionRequestData.newBuilder().
            setBundleId(900).
            setPid("Swan").build();
        TerraHarvestPayload payload = createPayload(request, MetaTypeMessageType.GetAttributeDefinitionRequest);
        TerraHarvestMessage message = createTerraHarvestMessage(request, MetaTypeMessageType.
            GetAttributeDefinitionRequest);

        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);

        //capture the message, verify error code
        verify(m_EventAdmin).postEvent(Mockito.any(Event.class));
        
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.INVALID_VALUE, 
                "The bundle id 900, and pid Swan did not return any attribute definitions.");
    }

    /**
     * Test that if the bundle id/pid does not return attributes and a key value is set that the 
     * GetAttributeDefinitionRequest is properly handled.
     * 
     * Verify error code INVALID_VALUE is sent when no attributes are returned from the MetaType service and a key is 
     * set.
     * 
     * Verify error code INVALID_VALUE is sent when attributes are returned, but key does not match an attribute id in  
     * the returned attributes from the MetaType service.
     * 
     * Verify event is posted with correct data message.
     */
    @Test
    public void testGetAttributeDefinitionInvalid() throws IOException
    {
      //mock bundles
        Bundle bundleA = mock(Bundle.class);
        when(bundleA.getBundleId()).thenReturn(1L);
        Bundle bundleB = mock(Bundle.class);
        when(bundleB.getBundleId()).thenReturn(2L);
        Bundle bundleC = mock(Bundle.class);
        when(bundleC.getBundleId()).thenReturn(3L);
        Bundle coreBundle = mock(Bundle.class);
        when(coreBundle.getBundleId()).thenReturn(0L);
        when(m_Context.getBundles()).thenReturn(new Bundle[] { bundleA, bundleC, bundleB, coreBundle});
        //mock meta type information for the bundles
        MetaTypeInformation infoA = mock(MetaTypeInformation.class);
        when(infoA.getPids()).thenReturn(new String[] {"configA", "configF"});
        when(infoA.getFactoryPids()).thenReturn(new String[] {"FactoryconfigA", "FactoryconfigF"});
        MetaTypeInformation infoB = mock(MetaTypeInformation.class);
        when(infoB.getPids()).thenReturn(new String[] {});
        when(infoB.getFactoryPids()).thenReturn(new String[] {"FactoryConfigG"});
        MetaTypeInformation infoC = mock(MetaTypeInformation.class);
        when(infoC.getPids()).thenReturn(new String[] {"configZ", "configD"});
        when(infoC.getFactoryPids()).thenReturn(new String[] {});
        //mock core meta information
        MetaTypeInformation infoCore = mock(MetaTypeInformation.class);
        when(infoCore.getPids()).thenReturn(new String[] {"core-pid", "configD"});
        when(infoCore.getFactoryPids()).thenReturn(new String[] {});
        when(m_MetaTypeService.getMetaTypeInformation(coreBundle)).thenReturn(infoCore);
        ObjectClassDefinition ocd = mock(ObjectClassDefinition.class);
        when(infoCore.getObjectClassDefinition("core-pid", null)).thenReturn(ocd);
        stubAttributeDefs(ocd);
        
        //testing not found bundle/pid
        ObjectClassDefinition ocdBad  = mock(ObjectClassDefinition.class);
        when(infoCore.getObjectClassDefinition("Swan", null)).thenReturn(ocdBad);
        GetAttributeDefinitionRequestData request = GetAttributeDefinitionRequestData.newBuilder().
            setBundleId(900).
            setPid("Swan").
            setKey("da key").build();
        TerraHarvestPayload payload = createPayload(request, MetaTypeMessageType.GetAttributeDefinitionRequest);
        TerraHarvestMessage message = createTerraHarvestMessage(request, MetaTypeMessageType.
            GetAttributeDefinitionRequest);

        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);

        //capture the message, verify error code
        verify(m_EventAdmin).postEvent(Mockito.any(Event.class));
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.INVALID_VALUE, 
                "The bundle id 900, and pid Swan did not return any attribute definitions, key da key was ignored.");
        verify(m_ResponseWrapper).queue(channel);

        //new request with attribute returned, but the key not found
        request = GetAttributeDefinitionRequestData.newBuilder().
            setBundleId(0).
            setPid("core-pid").
            setKey("da key").build();
        payload = createPayload(request, MetaTypeMessageType.GetAttributeDefinitionRequest);
        message = createTerraHarvestMessage(request, MetaTypeMessageType.
            GetAttributeDefinitionRequest);

        //handle message
        m_SUT.handleMessage(message, payload, channel);

        //capture the message, verify error code
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.INVALID_VALUE, 
                "Attribute definitions were found for the bundle id 0, and "
                        + "pid core-pid, but the attribute matching the key da key was not found.");
        //verify the queued to channel
        verify(m_ResponseWrapper, times(2)).queue(channel);

        // verify event is posted for get attribute definition request
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.MetaType.
                toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(MetaTypeMessageType.GetAttributeDefinitionRequest.toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((MetaTypeNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(getNamespaceMessage(message)));
    }

    /**
     * Test that if the bundle id/pid return attributes that GetAttributeDefinitionRequest is properly handled.
     * This test is without a key.
     * 
     * Verify list of all attributes fitting the bundle/pid pair are returned.
     */
    @Test
    public void testGetAttributeDefinitionValidWOKey() throws IOException
    {
        //mock bundles
        Bundle bundleA = mock(Bundle.class);
        when(bundleA.getBundleId()).thenReturn(1L);
        Bundle bundleB = mock(Bundle.class);
        when(bundleB.getBundleId()).thenReturn(2L);
        Bundle bundleC = mock(Bundle.class);
        when(bundleC.getBundleId()).thenReturn(3L);
        Bundle coreBundle = mock(Bundle.class);
        when(coreBundle.getBundleId()).thenReturn(0L);
        when(m_Context.getBundles()).thenReturn(new Bundle[] { bundleA, bundleC, bundleB, coreBundle});
        //mock meta type information for the bundles
        MetaTypeInformation infoA = mock(MetaTypeInformation.class);
        when(infoA.getPids()).thenReturn(new String[] {"configA", "configF"});
        when(infoA.getFactoryPids()).thenReturn(new String[] {"FactoryconfigA", "FactoryconfigF"});
        MetaTypeInformation infoB = mock(MetaTypeInformation.class);
        when(infoB.getPids()).thenReturn(new String[] {});
        when(infoB.getFactoryPids()).thenReturn(new String[] {"FactoryConfigG"});
        MetaTypeInformation infoC = mock(MetaTypeInformation.class);
        when(infoC.getPids()).thenReturn(new String[] {"configZ", "configD"});
        when(infoC.getFactoryPids()).thenReturn(new String[] {});
        //mock core meta information
        MetaTypeInformation infoCore = mock(MetaTypeInformation.class);
        when(infoCore.getPids()).thenReturn(new String[] {"core-pid", "configD"});
        when(infoCore.getFactoryPids()).thenReturn(new String[] {});
        when(m_MetaTypeService.getMetaTypeInformation(coreBundle)).thenReturn(infoCore);
        ObjectClassDefinition ocd = mock(ObjectClassDefinition.class);
        when(infoCore.getObjectClassDefinition("core-pid", null)).thenReturn(ocd);
        stubAttributeDefs(ocd);
        
        //testing not found bundle/pid
        ObjectClassDefinition ocdBad  = mock(ObjectClassDefinition.class);
        when(infoCore.getObjectClassDefinition("Swan", null)).thenReturn(ocdBad);
        GetAttributeDefinitionRequestData request = GetAttributeDefinitionRequestData.newBuilder().
            setBundleId(0).
            setPid("core-pid").build();
        TerraHarvestPayload payload = createPayload(request, MetaTypeMessageType.GetAttributeDefinitionRequest);

        TerraHarvestMessage message = createTerraHarvestMessage(request, MetaTypeMessageType.
            GetAttributeDefinitionRequest);

        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);

        //capture the message
        ArgumentCaptor<GetAttributeDefinitionResponseData> captor = 
                ArgumentCaptor.forClass(GetAttributeDefinitionResponseData.class);
        verify(m_EventAdmin).postEvent(Mockito.any(Event.class));
        verify(m_MessageFactory).createMetaTypeResponseMessage(eq(message), 
                eq(MetaTypeMessageType.GetAttributeDefinitionResponse), captor.capture());
        verify(m_ResponseWrapper).queue(channel);
        //parse response
        GetAttributeDefinitionResponseData response = captor.getValue();
        //verify that list of attributes is NOT empty
        assertThat(response.getAttributeDefinitionCount(), is(5));
        
        //verify return values
        List<AttributeDefinitionType> definitions = response.getAttributeDefinitionList();
        for (AttributeDefinitionType def : definitions)
        {
            if (def.getName().equals("Item1"))
            {
                assertThat(def.getId(), is("test"));
                assertThat(def.getDefaultValueCount(), is(1));
                assertThat(def.getDefaultValueList().get(0), is("default"));
                assertThat(def.getAttributeType(), is(AttributeDefinition.STRING));
                assertThat(def.getCardinality(), is(1));
                assertThat(def.getOptionLabelCount(), is(1));
                assertThat(def.getOptionLabelList().get(0), is ("Item1Label"));
                assertThat(def.getOptionValueList(), is(Arrays.asList("item1")));
            }
            else if (def.getName().equals("Item2"))
            {
                assertThat(def.getId(), is("not-set-prop"));
                assertThat(def.getDefaultValueCount(), is(1));
                assertThat(def.getDefaultValueList().get(0), is("not-set-default"));
                assertThat(def.getAttributeType(), is(AttributeDefinition.STRING));
                assertThat(def.getCardinality(), is(1));
                assertThat(def.getOptionLabelCount(), is(1));
                assertThat(def.getOptionLabelList().get(0), is ("Item2Label"));
                assertThat(def.getOptionValueList(), is(Arrays.asList("item2")));
            }
            else if (def.getName().equals("Item3"))
            {
                assertThat(def.getId(), is("unused"));
                assertThat(def.getCardinality(), is(0));
                assertThat(def.getAttributeType(), is(AttributeDefinition.FLOAT));
            }
            else if (def.getName().equals("Item4"))
            {
                assertThat(def.getId(), is("int-test"));
                assertThat(def.getDefaultValueCount(), is(1));
                assertThat(def.getDefaultValueList().get(0), is("84"));
                assertThat(def.getAttributeType(), is(AttributeDefinition.INTEGER));
                assertThat(def.getCardinality(), is(0));
                assertThat(def.getOptionLabelCount(), is(1));
                assertThat(def.getOptionLabelList().get(0), is ("Item4Label"));
                assertThat(def.getOptionValueList(), is(Arrays.asList("item4")));
            }
            else if (def.getName().equals("Item5"))
            {
                assertThat(def.getId(), is("nullString"));
                assertThat(def.getDefaultValueCount(), is(0));
                assertThat(def.getAttributeType(), is(AttributeDefinition.STRING));
                assertThat(def.getCardinality(), is(1));
                assertThat(def.getOptionLabelCount(), is(1));
                assertThat(def.getOptionLabelList().get(0), is ("Item5Label"));
                assertThat(def.getOptionValueList(), is(Arrays.asList("item5")));
            }
        }
    }

    /**
     * Test that if the bundle id/pid return attributes that GetAttributeDefinitionRequest is properly handled.
     * This test is with a key.
     * 
     * Verify attributes fitting the bundle/pid pair and key is returned.
     */
    @Test
    public void testGetAttributeDefinitionValidWithKey() throws IOException
    {
        //mock bundles
        Bundle bundleA = mock(Bundle.class);
        when(bundleA.getBundleId()).thenReturn(1L);
        Bundle bundleB = mock(Bundle.class);
        when(bundleB.getBundleId()).thenReturn(2L);
        Bundle bundleC = mock(Bundle.class);
        when(bundleC.getBundleId()).thenReturn(3L);
        Bundle coreBundle = mock(Bundle.class);
        when(coreBundle.getBundleId()).thenReturn(0L);
        when(m_Context.getBundles()).thenReturn(new Bundle[] { bundleA, bundleC, bundleB, coreBundle});
        //mock meta type information for the bundles
        MetaTypeInformation infoA = mock(MetaTypeInformation.class);
        when(infoA.getPids()).thenReturn(new String[] {"configA", "configF"});
        when(infoA.getFactoryPids()).thenReturn(new String[] {"FactoryconfigA", "FactoryconfigF"});
        MetaTypeInformation infoB = mock(MetaTypeInformation.class);
        when(infoB.getPids()).thenReturn(new String[] {});
        when(infoB.getFactoryPids()).thenReturn(new String[] {"FactoryConfigG"});
        MetaTypeInformation infoC = mock(MetaTypeInformation.class);
        when(infoC.getPids()).thenReturn(new String[] {"configZ", "configD"});
        when(infoC.getFactoryPids()).thenReturn(new String[] {});
        //mock core meta information
        MetaTypeInformation infoCore = mock(MetaTypeInformation.class);
        when(infoCore.getPids()).thenReturn(new String[] {"core-pid", "configD"});
        when(infoCore.getFactoryPids()).thenReturn(new String[] {});
        when(m_MetaTypeService.getMetaTypeInformation(coreBundle)).thenReturn(infoCore);
        ObjectClassDefinition ocd = mock(ObjectClassDefinition.class);
        when(infoCore.getObjectClassDefinition("core-pid", null)).thenReturn(ocd);
        stubAttributeDefs(ocd);
        
        //send message
        GetAttributeDefinitionRequestData request = GetAttributeDefinitionRequestData.newBuilder().
            setBundleId(0).
            setPid("core-pid").
            setKey("test").build();
        TerraHarvestPayload payload = createPayload(request, MetaTypeMessageType.GetAttributeDefinitionRequest);
        TerraHarvestMessage message = createTerraHarvestMessage(request, MetaTypeMessageType.
            GetAttributeDefinitionRequest);

        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);

        //capture the message
        ArgumentCaptor<GetAttributeDefinitionResponseData> captor = 
                ArgumentCaptor.forClass(GetAttributeDefinitionResponseData.class);
        verify(m_MessageFactory).createMetaTypeResponseMessage(eq(message), 
                eq(MetaTypeMessageType.GetAttributeDefinitionResponse), captor.capture());
        verify(m_ResponseWrapper).queue(channel);
        //parse response
        GetAttributeDefinitionResponseData response = captor.getValue();
        //verify that list of attribute is NOT empty
        assertThat(response.getBundleId(), is(0L));
        assertThat(response.getPid(), is("core-pid"));
        assertThat(response.getAttributeDefinitionCount(), is(1));
        
        //verify return values
        AttributeDefinitionType definition = response.getAttributeDefinitionList().get(0);
        assertThat(definition.getName(), is("Item1"));
        assertThat(definition.getId(), is("test"));
        assertThat(definition.getDescription(), is("description"));
        assertThat(definition.getDefaultValueCount(), is(1));
        assertThat(definition.getDefaultValueList().get(0), is("default"));
        assertThat(definition.getAttributeType(), is(AttributeDefinition.STRING));
        assertThat(definition.getCardinality(), is(1));
        assertThat(definition.getOptionLabelCount(), is(1));
        assertThat(definition.getOptionLabelList().get(0), is ("Item1Label"));
        assertThat(definition.getOptionValueList(), is(Arrays.asList("item1")));
    }

   /**
    * Test that if the bundle id/pid do not return attributes that the GetAttributeKeysRequest is handled properly.
    * 
    * Verify message with generic error code INVALID_VALUE is sent in response to the request.
    * 
    * Verify event is posted with correct data message.
    */
    @Test
    public void testGetAttributKeysInvalid() throws IOException
    {
        //mock bundles
        Bundle bundleA = mock(Bundle.class);
        when(bundleA.getBundleId()).thenReturn(1L);
        Bundle bundleB = mock(Bundle.class);
        when(bundleB.getBundleId()).thenReturn(2L);
        Bundle bundleC = mock(Bundle.class);
        when(bundleC.getBundleId()).thenReturn(3L);
        Bundle coreBundle = mock(Bundle.class);
        when(coreBundle.getBundleId()).thenReturn(0L);
        when(m_Context.getBundles()).thenReturn(new Bundle[] { bundleA, bundleC, bundleB, coreBundle});
       
        //send message
        GetAttributeKeysRequestData request = GetAttributeKeysRequestData.newBuilder().
            setBundleId(900).
            setPid("my pid").build();
        TerraHarvestPayload payload = createPayload(request, MetaTypeMessageType.GetAttributeKeyRequest);
        TerraHarvestMessage message = createTerraHarvestMessage(request, MetaTypeMessageType.GetAttributeKeyRequest);
 
        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);
 
        //capture the message, verify error code
        verify(m_MessageFactory).createBaseErrorMessage(eq(message), eq(ErrorCode.INVALID_VALUE), Mockito.anyString());
        verify(m_ResponseWrapper).queue(channel);
        
        // verify event is posted for get attribute key request
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.MetaType.
                toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(MetaTypeMessageType.GetAttributeKeyRequest.toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((MetaTypeNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(getNamespaceMessage(message)));
    }

    /**
     * Test that if the bundle id/pid do return attributes that the GetAttributeKeysRequest is handled properly.
     * 
     * Verify list of attribute key is sent in response to the request.
     */
    @Test
    public void testGetAttributKeysValid() throws IOException
    {
        //mock bundle
        Bundle coreBundle = mock(Bundle.class);
        when(coreBundle.getBundleId()).thenReturn(0L);
        when(m_Context.getBundles()).thenReturn(new Bundle[] { coreBundle});
        //mock core meta information
        MetaTypeInformation infoCore = mock(MetaTypeInformation.class);
        when(infoCore.getPids()).thenReturn(new String[] {"core-pid", "configD"});
        when(infoCore.getFactoryPids()).thenReturn(new String[] {});
        when(m_MetaTypeService.getMetaTypeInformation(coreBundle)).thenReturn(infoCore);
        ObjectClassDefinition ocd = mock(ObjectClassDefinition.class);
        when(infoCore.getObjectClassDefinition("core-pid", null)).thenReturn(ocd);
        stubAttributeDefs(ocd);
        
        //send message
        GetAttributeKeysRequestData request = GetAttributeKeysRequestData.newBuilder().
            setBundleId(0).
            setPid("core-pid").build();
        TerraHarvestPayload payload = createPayload(request, MetaTypeMessageType.GetAttributeKeyRequest);
        TerraHarvestMessage message = createTerraHarvestMessage(request, MetaTypeMessageType.GetAttributeKeyRequest);
 
        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);
 
        //capture the message
        ArgumentCaptor<GetAttributeKeysResponseData> captor = 
                ArgumentCaptor.forClass(GetAttributeKeysResponseData.class);
        verify(m_MessageFactory).createMetaTypeResponseMessage(eq(message), 
            eq(MetaTypeMessageType.GetAttributeKeyResponse), 
                captor.capture());
        verify(m_ResponseWrapper).queue(channel);
        //parse response
        GetAttributeKeysResponseData response = captor.getValue();
        //verify that list of keys is NOT empty
        assertThat(response.getBundleId(), is(0L));
        assertThat(response.getPid(), is("core-pid"));
        assertThat(response.getKeyCount(), is(5));

        //verify return values
        List<String> keys = response.getKeyList();
        assertThat(keys, hasItem("test"));
        assertThat(keys, hasItem("not-set-prop"));
        assertThat(keys, hasItem("unused"));
        assertThat(keys, hasItem("int-test"));
        assertThat(keys, hasItem("nullString"));
    }
    
    /**
     * Test that a get meta type information response is handled appropriately.
     * Verify that meta type information for all bundles is returned when no bundle id is specified.
     * Verify that only meta type information for a specific bundle is returned if a bundle id is specified.
     * Verify that a base error message is sent if information for a bundle that does not exist is requested.
     */
    @Test
    public void testGetMetatypeInformation() throws IOException
    {
        GetMetaTypeInfoRequestData request = GetMetaTypeInfoRequestData.newBuilder().build();
        TerraHarvestMessage thMessage = createTerraHarvestMessage(request, MetaTypeMessageType.GetMetaTypeInfoRequest);
        TerraHarvestPayload payLoad = TerraHarvestPayload.parseFrom(thMessage.getTerraHarvestPayload());
        
        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        //Mock bundles to be returned by context.
        Bundle bundle1 = mock(Bundle.class);
        Bundle bundle2 = mock(Bundle.class);
        Bundle bundle3 = mock(Bundle.class);
        Bundle bundle4 = mock(Bundle.class);
        Bundle bundle5 = mock(Bundle.class);
        Bundle[] bundles = new Bundle[] {bundle1, bundle2, bundle3, bundle4, bundle5};
        
        //Mock metatype information to be returned my metatype service.
        MetaTypeInformation infoBundle1 = mock(MetaTypeInformation.class);
        MetaTypeInformation infoBundle2 = mock(MetaTypeInformation.class);
        MetaTypeInformation infoBundle3 = mock(MetaTypeInformation.class);
        MetaTypeInformation infoBundle5 = mock(MetaTypeInformation.class);
        
        //Create list of PIDs to be associated with each bundle.
        String[] pidsForBundle1 = new String[] {"bundle1.test1", "bundle1.test2"};
        String[] pidsForBundle2 = new String[] {"bundle2.test1"};
        String[] pidsForBundle3 = new String[] {"bundle3.test1", "bundle3.test2"};
        String[] pidsForBundle5 = new String[] {"no.attr1"};
        
        //Create list of factory PIDs to be associated with each bundle.
        String[] factoryPidsForBundle1 = new String[] {"bundle1.factory1"};
        String[] factoryPidsForBundle2 = new String[] {"bundle2.factory1", "bundle2.factory2"};
        String[] factoryPidsForBundle3 = new String[] {};
        String[] factoryPidsForBundle5 = new String[] {"no.attr.factory1"};
        
        //For sake of sanity they all return the same set of attribute definitions.
        String ocdDesc1 = "blah";
        String ocdName1 = "name";
        String ocdId1 = "id";
        ObjectClassDefinition ocdForAll = mock(ObjectClassDefinition.class);
        when(ocdForAll.getDescription()).thenReturn(null, ocdDesc1);
        when(ocdForAll.getID()).thenReturn(ocdId1);
        when(ocdForAll.getName()).thenReturn(ocdName1);
        ObjectClassDefinition ocdBundle5 = mock(ObjectClassDefinition.class);
        String ocdDesc2 = "blah2";
        String ocdName2 = "name2";
        String ocdId2 = "id2";
        when(ocdBundle5.getDescription()).thenReturn(null, ocdDesc2);
        when(ocdBundle5.getID()).thenReturn(ocdId2);
        when(ocdBundle5.getName()).thenReturn(ocdName2);
        AttributeDefinition attrDef1 = mock(AttributeDefinition.class);
        AttributeDefinition attrDef2 = mock(AttributeDefinition.class);
        
        //Create list of attribute to be returned by object class definition.
        AttributeDefinition[] attribs = new AttributeDefinition[] {attrDef1, attrDef2};
        
        //Create list of default values.
        String[] defaults = new String[] {"default1", "default2", "default3"};
        
        //Create list of option labels.
        String[] optLabels = new String[] {"option1", "option2", "option3"};
        
        String[] optValues = new String[] {"value1", "value2", "value3"};
        
        //Stub all necessary methods.
        when(m_Context.getBundles()).thenReturn(bundles);
        when(bundle1.getBundleId()).thenReturn(5L);
        when(bundle2.getBundleId()).thenReturn(10L);
        when(bundle3.getBundleId()).thenReturn(15L);
        when(bundle4.getBundleId()).thenReturn(20L);
        when(bundle5.getBundleId()).thenReturn(25L);
        when(m_MetaTypeService.getMetaTypeInformation(bundle1)).thenReturn(infoBundle1);
        when(m_MetaTypeService.getMetaTypeInformation(bundle2)).thenReturn(infoBundle2);
        when(m_MetaTypeService.getMetaTypeInformation(bundle3)).thenReturn(infoBundle3);
        when(m_MetaTypeService.getMetaTypeInformation(bundle4)).thenReturn(null);
        when(m_MetaTypeService.getMetaTypeInformation(bundle5)).thenReturn(infoBundle5);
        when(infoBundle1.getPids()).thenReturn(pidsForBundle1);
        when(infoBundle2.getPids()).thenReturn(pidsForBundle2);
        when(infoBundle3.getPids()).thenReturn(pidsForBundle3);
        when(infoBundle5.getPids()).thenReturn(pidsForBundle5);
        when(infoBundle1.getFactoryPids()).thenReturn(factoryPidsForBundle1);
        when(infoBundle2.getFactoryPids()).thenReturn(factoryPidsForBundle2);
        when(infoBundle3.getFactoryPids()).thenReturn(factoryPidsForBundle3);
        when(infoBundle5.getFactoryPids()).thenReturn(factoryPidsForBundle5);
        when(infoBundle1.getObjectClassDefinition("bundle1.test1", null)).thenReturn(ocdForAll);
        when(infoBundle1.getObjectClassDefinition("bundle1.test2", null)).thenReturn(ocdForAll);
        when(infoBundle2.getObjectClassDefinition("bundle2.test1", null)).thenReturn(ocdForAll);
        when(infoBundle3.getObjectClassDefinition("bundle3.test1", null)).thenReturn(ocdForAll);
        when(infoBundle3.getObjectClassDefinition("bundle3.test2", null)).thenReturn(ocdForAll);
        when(infoBundle1.getObjectClassDefinition("bundle1.factory1", null)).thenReturn(ocdForAll);
        when(infoBundle2.getObjectClassDefinition("bundle2.factory1", null)).thenReturn(ocdForAll);
        when(infoBundle2.getObjectClassDefinition("bundle2.factory2", null)).thenReturn(ocdForAll);
        when(infoBundle5.getObjectClassDefinition("no.attr1", null)).thenReturn(ocdBundle5);
        when(infoBundle5.getObjectClassDefinition("no.attr.factory1", null)).thenReturn(ocdBundle5);
        when(ocdForAll.getAttributeDefinitions(ObjectClassDefinition.REQUIRED)).thenReturn(attribs);
        when(ocdBundle5.getAttributeDefinitions(ObjectClassDefinition.REQUIRED)).thenReturn(null);
        when(attrDef1.getCardinality()).thenReturn(5);
        when(attrDef1.getID()).thenReturn("key1");
        when(attrDef1.getDescription()).thenReturn("doesn't matter");
        when(attrDef1.getName()).thenReturn("rawr!");
        when(attrDef1.getType()).thenReturn(1);
        when(attrDef1.getDefaultValue()).thenReturn(null);
        when(attrDef1.getOptionLabels()).thenReturn(optLabels);
        when(attrDef1.getOptionValues()).thenReturn(optValues);
        when(attrDef2.getCardinality()).thenReturn(0);
        when(attrDef2.getID()).thenReturn("key2");
        when(attrDef2.getDescription()).thenReturn("some description");
        when(attrDef2.getName()).thenReturn("bob");
        when(attrDef2.getType()).thenReturn(2);
        when(attrDef2.getDefaultValue()).thenReturn(defaults);
        when(attrDef2.getOptionLabels()).thenReturn(optLabels);
        when(attrDef2.getOptionValues()).thenReturn(optValues);
        
        m_SUT.handleMessage(thMessage, payLoad, channel);
        
        //capture the message
        ArgumentCaptor<GetMetaTypeInfoResponseData> captor = ArgumentCaptor.forClass(GetMetaTypeInfoResponseData.class);
        verify(m_MessageFactory).createMetaTypeResponseMessage(eq(thMessage), 
            eq(MetaTypeMessageType.GetMetaTypeInfoResponse), captor.capture());
        verify(m_ResponseWrapper).queue(channel);
        //parse response
        GetMetaTypeInfoResponseData response = captor.getValue();
        List<MetaTypeInfoType> metaList = response.getMetaTypeList();
        assertThat(metaList.size(), is(10));
        
        //Verify data contained in response.
        MetaTypeInfoType metaInfo = metaList.get(0);
        assertThat(metaInfo.getOcd().getName(), is(ocdName1));
        assertThat(metaInfo.getOcd().getId(), is(ocdId1));
        //description can be null, therefore proto will return empty string
        assertThat(metaInfo.getOcd().getDescription(), is(""));
        assertThat(metaInfo.getBundleId(), is(5L));
        assertThat(metaInfo.getPid(), is("bundle1.test1"));
        assertThat(metaInfo.getIsFactory(), is(false));
        verifyAttributeDefitions(metaInfo.getAttributesList(), defaults, optLabels, optValues);
        
        metaInfo = metaList.get(1);
        assertThat(metaInfo.getOcd().getName(), is(ocdName1));
        assertThat(metaInfo.getOcd().getId(), is(ocdId1));
        assertThat(metaInfo.getOcd().getDescription(), is(ocdDesc1));
        assertThat(metaInfo.getBundleId(), is(5L));
        assertThat(metaInfo.getPid(), is("bundle1.test2"));
        assertThat(metaInfo.getIsFactory(), is(false));
        verifyAttributeDefitions(metaInfo.getAttributesList(), defaults, optLabels, optValues);
        
        metaInfo = metaList.get(2);
        assertThat(metaInfo.getOcd().getName(), is(ocdName1));
        assertThat(metaInfo.getOcd().getId(), is(ocdId1));
        assertThat(metaInfo.getOcd().getDescription(), is(ocdDesc1));
        assertThat(metaInfo.getBundleId(), is(5L));
        assertThat(metaInfo.getPid(), is("bundle1.factory1"));
        assertThat(metaInfo.getIsFactory(), is(true));
        verifyAttributeDefitions(metaInfo.getAttributesList(), defaults, optLabels, optValues);
        
        metaInfo = metaList.get(3);
        assertThat(metaInfo.getOcd().getName(), is(ocdName1));
        assertThat(metaInfo.getOcd().getId(), is(ocdId1));
        assertThat(metaInfo.getOcd().getDescription(), is(ocdDesc1));
        assertThat(metaInfo.getBundleId(), is(10L));
        assertThat(metaInfo.getPid(), is("bundle2.test1"));
        assertThat(metaInfo.getIsFactory(), is(false));
        verifyAttributeDefitions(metaInfo.getAttributesList(), defaults, optLabels, optValues);
        
        metaInfo = metaList.get(4);
        assertThat(metaInfo.getOcd().getName(), is(ocdName1));
        assertThat(metaInfo.getOcd().getId(), is(ocdId1));
        assertThat(metaInfo.getOcd().getDescription(), is(ocdDesc1));
        assertThat(metaInfo.getBundleId(), is(10L));
        assertThat(metaInfo.getPid(), is("bundle2.factory1"));
        assertThat(metaInfo.getIsFactory(), is(true));
        verifyAttributeDefitions(metaInfo.getAttributesList(), defaults, optLabels, optValues);
        
        metaInfo = metaList.get(5);
        assertThat(metaInfo.getOcd().getName(), is(ocdName1));
        assertThat(metaInfo.getOcd().getId(), is(ocdId1));
        assertThat(metaInfo.getOcd().getDescription(), is(ocdDesc1));
        assertThat(metaInfo.getBundleId(), is(10L));
        assertThat(metaInfo.getPid(), is("bundle2.factory2"));
        assertThat(metaInfo.getIsFactory(), is(true));
        verifyAttributeDefitions(metaInfo.getAttributesList(), defaults, optLabels, optValues);
        
        metaInfo = metaList.get(6);
        assertThat(metaInfo.getOcd().getName(), is(ocdName1));
        assertThat(metaInfo.getOcd().getId(), is(ocdId1));
        assertThat(metaInfo.getOcd().getDescription(), is(ocdDesc1));
        assertThat(metaInfo.getBundleId(), is(15L));
        assertThat(metaInfo.getPid(), is("bundle3.test1"));
        assertThat(metaInfo.getIsFactory(), is(false));
        verifyAttributeDefitions(metaInfo.getAttributesList(), defaults, optLabels, optValues);
        
        metaInfo = metaList.get(7);
        assertThat(metaInfo.getOcd().getName(), is(ocdName1));
        assertThat(metaInfo.getOcd().getId(), is(ocdId1));
        assertThat(metaInfo.getOcd().getDescription(), is(ocdDesc1));
        assertThat(metaInfo.getBundleId(), is(15L));
        assertThat(metaInfo.getPid(), is("bundle3.test2"));
        assertThat(metaInfo.getIsFactory(), is(false));
        verifyAttributeDefitions(metaInfo.getAttributesList(), defaults, optLabels, optValues);
        
        metaInfo = metaList.get(8);
        assertThat(metaInfo.getOcd().getName(), is(ocdName2));
        assertThat(metaInfo.getOcd().getId(), is(ocdId2));
        //descriptions can be null and therefore proto will return an empty string
        assertThat(metaInfo.getOcd().getDescription(), is(""));
        assertThat(metaInfo.getBundleId(), is(25L));
        assertThat(metaInfo.getPid(), is("no.attr1"));
        assertThat(metaInfo.getIsFactory(), is(false));
        assertThat(metaInfo.getAttributesCount(), is(0));
        
        metaInfo = metaList.get(9);
        assertThat(metaInfo.getOcd().getName(), is(ocdName2));
        assertThat(metaInfo.getOcd().getId(), is(ocdId2));
        assertThat(metaInfo.getOcd().getDescription(), is(ocdDesc2));
        assertThat(metaInfo.getBundleId(), is(25L));
        assertThat(metaInfo.getPid(), is("no.attr.factory1"));
        assertThat(metaInfo.getIsFactory(), is(true));
        assertThat(metaInfo.getAttributesCount(), is(0));
        
        //Build message to retrieve information for a single bundle.
        request = GetMetaTypeInfoRequestData.newBuilder().setBundleId(15L).build();
        thMessage = createTerraHarvestMessage(request, MetaTypeMessageType.GetMetaTypeInfoRequest);
        payLoad = TerraHarvestPayload.parseFrom(thMessage.getTerraHarvestPayload());
        
        m_SUT.handleMessage(thMessage, payLoad, channel);
        
        verify(m_MessageFactory).createMetaTypeResponseMessage(eq(thMessage), 
                eq(MetaTypeMessageType.GetMetaTypeInfoResponse), captor.capture());
        verify(m_ResponseWrapper, times(2)).queue(channel);
        //parse response
        response = captor.getValue();
        metaList = response.getMetaTypeList();
        assertThat(metaList.size(), is(2));
        
        //Verify data contained in response.
        metaInfo = metaList.get(0);
        assertThat(metaInfo.getOcd().getName(), is(ocdName1));
        assertThat(metaInfo.getOcd().getId(), is(ocdId1));
        assertThat(metaInfo.getOcd().getDescription(), is(ocdDesc1));
        assertThat(metaInfo.getBundleId(), is(15L));
        assertThat(metaInfo.getPid(), is("bundle3.test1"));
        assertThat(metaInfo.getIsFactory(), is(false));
        verifyAttributeDefitions(metaInfo.getAttributesList(), defaults, optLabels, optValues);
        
        metaInfo = metaList.get(1);
        assertThat(metaInfo.getOcd().getName(), is(ocdName1));
        assertThat(metaInfo.getOcd().getId(), is(ocdId1));
        assertThat(metaInfo.getOcd().getDescription(), is(ocdDesc1));
        assertThat(metaInfo.getBundleId(), is(15L));
        assertThat(metaInfo.getPid(), is("bundle3.test2"));
        assertThat(metaInfo.getIsFactory(), is(false));
        verifyAttributeDefitions(metaInfo.getAttributesList(), defaults, optLabels, optValues);
        
        //Build message to retrieve information for bundle that doesn't exist.
        request = GetMetaTypeInfoRequestData.newBuilder().setBundleId(777L).build();
        thMessage = createTerraHarvestMessage(request, MetaTypeMessageType.GetMetaTypeInfoRequest);
        payLoad = TerraHarvestPayload.parseFrom(thMessage.getTerraHarvestPayload());
        
        m_SUT.handleMessage(thMessage, payLoad, channel);
        
        //Verify base error response message is sent for request of non-existent bundle meta type information.
        verify(m_MessageFactory).createBaseErrorMessage(eq(thMessage), 
                eq(ErrorCode.INVALID_VALUE), eq("Metatype information for bundle with id 777 could not be retrieved " 
                        + "because the bundle could not be found."));
        verify(m_ResponseWrapper, times(3)).queue(channel);
    }
    
    /**
     * Used to verify a list of attribute definition types.
     */
    private void verifyAttributeDefitions(final List<AttributeDefinitionType> attrDefList, String[] defaults, 
            String[] optLabels, String[] optValues)
    {
        assertThat(attrDefList.size(), is(2));
        assertThat(attrDefList.get(0).getCardinality(), is(5));
        assertThat(attrDefList.get(0).getId(), is("key1"));
        assertThat(attrDefList.get(0).getName(), is("rawr!"));
        assertThat(attrDefList.get(0).getAttributeType(), is(1));
        assertThat(attrDefList.get(0).getDefaultValueList(), is(Arrays.asList(new String[0])));
        assertThat(attrDefList.get(0).getOptionLabelList(), is(Arrays.asList(optLabels)));
        assertThat(attrDefList.get(0).getOptionValueList(), is(Arrays.asList(optValues)));
        assertThat(attrDefList.get(0).hasRequired(), is(true));
        assertThat(attrDefList.get(1).getCardinality(), is(0));
        assertThat(attrDefList.get(1).getId(), is("key2"));
        assertThat(attrDefList.get(1).getName(), is("bob"));
        assertThat(attrDefList.get(1).getDescription(), is("some description"));
        assertThat(attrDefList.get(1).getAttributeType(), is(2));
        assertThat(attrDefList.get(1).getDefaultValueList(), is(Arrays.asList(defaults)));
        assertThat(attrDefList.get(1).getOptionLabelList(), is(Arrays.asList(optLabels)));
        assertThat(attrDefList.get(1).getOptionValueList(), is(Arrays.asList(optValues)));
        assertThat(attrDefList.get(1).hasRequired(), is(true));
    }
    
    /**
     * Construct a TerraHarvestMessage wrapping a Meta Type message.
     * @param metaTypeMessage
     *     meta type message
     * @param type
     *     the type of the message
     */
    private TerraHarvestMessage createTerraHarvestMessage(final Message metaTypeMessage, final MetaTypeMessageType type)
    {
        MetaTypeNamespace namespaceMessage = MetaTypeNamespace.newBuilder().
            setData(metaTypeMessage.toByteString()).
            setType(type).build();
        return TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.MetaType, 100, namespaceMessage);
    }
    
    /**
     * Pull out namespace message for checking events.
     */
    private MetaTypeNamespace getNamespaceMessage(final TerraHarvestMessage terraHarvestMessage) throws IOException
    {
        final TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(terraHarvestMessage.getTerraHarvestPayload());
        return MetaTypeNamespace.parseFrom(payload.getNamespaceMessage());
    }
    private TerraHarvestPayload createPayload(final Message metaTypeMessage, final MetaTypeMessageType type)
    {
        MetaTypeNamespace namespaceMessage = MetaTypeNamespace.newBuilder().
                setData(metaTypeMessage.toByteString()).
                setType(type).
                build();
        return TerraHarvestPayload.newBuilder().
               setNamespace(Namespace.MetaType).
               setNamespaceMessage(namespaceMessage.toByteString()).
               build();
    }
    
    private void stubAttributeDefs(ObjectClassDefinition ocd)
    {
        AttributeDefinition item1 = mock(AttributeDefinition.class);
        AttributeDefinition item2 = mock(AttributeDefinition.class);
        AttributeDefinition item3 = mock(AttributeDefinition.class);
        AttributeDefinition item4 = mock(AttributeDefinition.class);
        AttributeDefinition item5 = mock(AttributeDefinition.class);
        AttributeDefinition[] allAd = new AttributeDefinition[] { item1, item2, item3, item4, item5 };
        AttributeDefinition[] requiredAd = new AttributeDefinition[] { item1, item2, item3};
        AttributeDefinition[] optionalAd = new AttributeDefinition[] { item4, item5 };
        when(ocd.getAttributeDefinitions(ObjectClassDefinition.REQUIRED)).thenReturn(requiredAd);
        when(ocd.getAttributeDefinitions(ObjectClassDefinition.OPTIONAL)).thenReturn(optionalAd);
        when(ocd.getAttributeDefinitions(ObjectClassDefinition.ALL)).thenReturn(allAd);
        //mock behavior for attribute definitions
        when(item1.getID()).thenReturn("test");
        when(item1.getDefaultValue()).thenReturn(new String[] { "default" });
        when(item1.getType()).thenReturn(AttributeDefinition.STRING);
        when(item1.getCardinality()).thenReturn(1);
        when(item1.getName()).thenReturn("Item1");
        when(item1.getOptionLabels()).thenReturn(new String[] {"Item1Label"});
        when(item1.getOptionValues()).thenReturn(new String[] {"item1"});
        when(item1.getDescription()).thenReturn("description");
        
        when(item2.getID()).thenReturn("not-set-prop");
        when(item2.getDefaultValue()).thenReturn(new String[] { "not-set-default" });
        when(item2.getType()).thenReturn(AttributeDefinition.STRING);
        when(item2.getCardinality()).thenReturn(1);
        when(item2.getName()).thenReturn("Item2");
        when(item2.getOptionLabels()).thenReturn(new String[] {"Item2Label"});
        when(item2.getOptionValues()).thenReturn(new String[] {"item2"});
        
        when(item3.getID()).thenReturn("unused");
        when(item3.getName()).thenReturn("Item3");
        when(item3.getCardinality()).thenReturn(0);
        when(item3.getType()).thenReturn(AttributeDefinition.FLOAT);

        when(item4.getID()).thenReturn("int-test");
        when(item4.getDefaultValue()).thenReturn(new String[] { "84" });
        when(item4.getType()).thenReturn(AttributeDefinition.INTEGER);
        when(item4.getCardinality()).thenReturn(0);
        when(item4.getName()).thenReturn("Item4");
        when(item4.getOptionLabels()).thenReturn(new String[] {"Item4Label"});
        when(item4.getOptionValues()).thenReturn(new String[] {"item4"});
        
        when(item5.getID()).thenReturn("nullString");
        when(item5.getDefaultValue()).thenReturn(null);
        when(item5.getType()).thenReturn(AttributeDefinition.STRING);
        when(item5.getCardinality()).thenReturn(1);
        when(item5.getName()).thenReturn("Item5");
        when(item5.getOptionLabels()).thenReturn(new String[] {"Item5Label"});
        when(item5.getOptionValues()).thenReturn(new String[] {"item5"});
    }
}
