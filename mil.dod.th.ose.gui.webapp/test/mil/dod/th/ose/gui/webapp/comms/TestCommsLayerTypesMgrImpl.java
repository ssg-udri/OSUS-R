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

package mil.dod.th.ose.gui.webapp.comms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;

import mil.dod.th.core.ccomm.link.capability.LinkLayerCapabilities;
import mil.dod.th.core.ccomm.physical.capability.PhysicalLinkCapabilities;
import mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CommType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace.CustomCommsMessageType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetAvailableCommTypesResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.types.ccomm.LinkLayerTypeEnum;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.ose.gui.webapp.TerraHarvestMessageHelper;
import mil.dod.th.ose.gui.webapp.comms.CommsLayerTypesMgrImpl.CapabilitiesResponseHandler;
import mil.dod.th.ose.gui.webapp.comms.CommsLayerTypesMgrImpl.EventHelperCommsTypes;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.remote.converter.PhysicalLinkTypeEnumConverter;
import mil.dod.th.remote.lexicon.capability.BaseCapabilitiesGen.BaseCapabilities;
import mil.dod.th.remote.lexicon.ccomm.link.capability.LinkLayerCapabilitiesGen;
import mil.dod.th.remote.lexicon.ccomm.physical.capability.PhysicalLinkCapabilitiesGen;
import mil.dod.th.remote.lexicon.ccomm.transport.capability.TransportLayerCapabilitiesGen;
import mil.dod.th.remote.lexicon.types.ccomm.CustomCommTypesGen;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

import com.google.protobuf.Message;

/**
 * Test implementation of the {@link CommsLayerTypesMgr}.
 * @author bachmakm
 *
 */
public class TestCommsLayerTypesMgrImpl
{
    private CommsLayerTypesMgrImpl m_SUT;
    private BundleContextUtil m_BundleUtil;
    private EventHelperCommsTypes m_CommsHelper;
    private MessageFactory m_MessageFactory;
    private JaxbProtoObjectConverter m_Converter;
    private GrowlMessageUtil m_GrowlUtil;
    private EventAdmin m_EventAdmin;
    private MessageWrapper m_MessageWrapper;
    private CommsImage m_CommsImageInterface;

    @SuppressWarnings("rawtypes") //TH-534:unable to parameterize at the moment
    private ServiceRegistration m_HandlerReg = mock(ServiceRegistration.class);
    
    //these fields are at this level so they can be used in multiple messages
    //system ids
    private int systemId1 = 123;
    
    @SuppressWarnings("unchecked")//because of the use of the dictionary for the event helper
    @Before
    public void setUp()
    {
        //mock services
        m_BundleUtil = mock(BundleContextUtil.class);
        m_Converter = mock(JaxbProtoObjectConverter.class);
        m_MessageFactory = mock(MessageFactory.class);
        m_MessageWrapper = mock(MessageWrapper.class);
        m_GrowlUtil = mock(GrowlMessageUtil.class);
        m_EventAdmin = mock(EventAdmin.class);        
        
        BundleContext bundleContext = mock(BundleContext.class);
        
        //create asset types helper
        m_SUT = new CommsLayerTypesMgrImpl();
        
        //set dependencies
        m_SUT.setBundleContextUtil(m_BundleUtil);
        m_SUT.setConverter(m_Converter);
        m_SUT.setGrowlMessageUtility(m_GrowlUtil);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_CommsImageInterface = new CommsImage();
        m_SUT.setCommsImageInterface(m_CommsImageInterface);
        
        //mock behavior for event listener
        when(m_BundleUtil.getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
            Mockito.any(Dictionary.class))).thenReturn(m_HandlerReg);
        
        when(m_MessageFactory.createCustomCommsMessage(Mockito.any(CustomCommsMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        
        //register helper
        m_SUT.setupDependencies();
        
        //verify
        ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(bundleContext).registerService(eq(EventHandler.class), captor.capture(), 
            Mockito.any(Dictionary.class));
        verify(m_BundleUtil).getBundleContext();
        
        //enter classes
        m_CommsHelper = (EventHelperCommsTypes) captor.getAllValues().get(0);
    }
    
    /**
     * Test the predestroy unregistering of event handlers.
     * Verify that all are unregistered.
     */
    @Test
    public void testPreDestroy()
    {
        m_SUT.unregisterHelper();

        //verify listener is unregistered
        verify(m_HandlerReg).unregister();
    }
    
    /**
     * Test the handling of the get comms types (transport, link, and physical).
     * Verify that the product types are correctly added.
     */
    @Test
    public void testGetCommsTypes()
    {
        //test transport layer types
        Event getTypes = mockMessageGetCommsTypesResponse(systemId1, CommType.TransportLayer);
        m_CommsHelper.handleEvent(getTypes);

        List<String> transportTypes = m_SUT.getTransportLayerTypes(systemId1);
        //verify
        assertThat(transportTypes.size(), is(3));

        //verify the FQCNs were found
        assertThat(transportTypes, hasItem("class.name.one"));
        assertThat(transportTypes, hasItem("class.name.two"));
        assertThat(transportTypes, hasItem("class.name.three"));

        //test link layer types
        getTypes = mockMessageGetCommsTypesResponse(systemId1, CommType.Linklayer);
        m_CommsHelper.handleEvent(getTypes);

        List<String> linkTypes = m_SUT.getLinkLayerTypes(systemId1);
        //verify
        assertThat(linkTypes.size(), is(3));

        //verify the FQCNs were found
        assertThat(linkTypes, hasItem("class.name.one"));
        assertThat(linkTypes, hasItem("class.name.two"));
        assertThat(linkTypes, hasItem("class.name.three"));
        
        //test physical link types
        getTypes = mockMessageGetCommsTypesResponse(systemId1, CommType.PhysicalLink);
        m_CommsHelper.handleEvent(getTypes);

        List<String> physTypes = m_SUT.getPhysicalLinkClasses(systemId1);
        //verify
        assertThat(physTypes.size(), is(3));

        //verify the FQCNs were found
        assertThat(physTypes, hasItem("class.name.one"));
        assertThat(physTypes, hasItem("class.name.two"));
        assertThat(physTypes, hasItem("class.name.three"));
        
        //verify that handler will send get capabilities request
        verify(m_MessageFactory, times(9)).createCustomCommsMessage(
                eq(CustomCommsMessageType.GetCapabilitiesRequest), Mockito.any(Message.class));
        verify(m_MessageWrapper, times(9)).queue(eq(systemId1), Mockito.any(ResponseHandler.class));
        
        //verify an empty list is returned for each type
        assertThat(m_SUT.getLinkLayerTypes(8675309).size(), is(0));
        assertThat(m_SUT.getPhysicalLinkClasses(8675309).size(), is(0));
        assertThat(m_SUT.getTransportLayerTypes(8675309).size(), is(0));
    } 
    
    /**
     * Verify physical link types are returned.
     */
    @Test
    public void testGetPhysicalLinkTypes() throws ObjectConverterException
    {
        Event getTypes = mockMessageGetCommsTypesResponse(systemId1, CommType.PhysicalLink);
        m_CommsHelper.handleEvent(getTypes);
        
        handleTestCapabiltiesMessage("class.name.one", PhysicalLinkTypeEnum.SERIAL_PORT);
        handleTestCapabiltiesMessage("class.name.two", PhysicalLinkTypeEnum.GPIO);
        
        assertThat(m_SUT.getPhysicalLinkTypes(123),
            hasItems(PhysicalLinkTypeEnum.SERIAL_PORT, PhysicalLinkTypeEnum.GPIO));
    }
    
    /**
     * Verify comms layer types are properly updated and saved. 
     */
    @Test
    public void testUpdateCommsTypes() throws ObjectConverterException
    {   
        //load assets for system id
        Event getTypes = mockMessageGetCommsTypesResponse(systemId1, CommType.TransportLayer);

        //this should render two asset types for the system id
        m_CommsHelper.handleEvent(getTypes);

        List<String> transportTypes = m_SUT.getTransportLayerTypes(systemId1);
        //verify
        assertThat(transportTypes.size(), is(3));

        //verify the FQCNs were found
        assertThat(transportTypes, hasItem("class.name.one"));
        assertThat(transportTypes, hasItem("class.name.two"));
        assertThat(transportTypes, hasItem("class.name.three"));
        
        //fake capabilities for type class.name.three in transport layer
        CapabilitiesResponseHandler handler = m_SUT.new CapabilitiesResponseHandler("class.name.three");
        BaseCapabilities baseCaps = BaseCapabilities.newBuilder().
                setDescription("fancy description").setProductName("fancyName").build();
        TransportLayerCapabilitiesGen.TransportLayerCapabilities caps1 = 
                TransportLayerCapabilitiesGen.TransportLayerCapabilities.newBuilder().setBase(baseCaps).build();
        GetCapabilitiesResponseData response = GetCapabilitiesResponseData.newBuilder().
                setCommType(CommType.TransportLayer).setProductType("class.name.three").
                setTransportCapabilities(caps1).build();
        CustomCommsNamespace nameResponse = CustomCommsNamespace.newBuilder().
                setData(response.toByteString()).
                setType(CustomCommsMessageType.GetCapabilitiesResponse).build();
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().setNamespace(Namespace.CustomComms).
                setNamespaceMessage(response.toByteString()).build();             
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(systemId1, 0,
            Namespace.CustomComms, 123, nameResponse);        
        when(m_Converter.convertToJaxb(Mockito.any(Message.class))).thenReturn(new TransportLayerCapabilities());
        handler.handleResponse(thMessage, payload, nameResponse, response);
        
        //mock new layer types
        getTypes = mockMessageGetCommsTypesUpdateResponse(systemId1, CommType.TransportLayer);
        m_CommsHelper.handleEvent(getTypes);
        
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        //verify push event
        verify(m_EventAdmin, times(2)).postEvent(captor.capture());
        assertThat(captor.getValue().getTopic(), is(CommsMgr.TOPIC_COMMS_LAYER_UPDATED));
        assertThat(captor.getValue().getPropertyNames().length, is(1));
        assertThat(captor.getValue().getPropertyNames()[0], is("event.topics"));
        
        transportTypes = m_SUT.getTransportLayerTypes(systemId1);
       
        //verify
        assertThat(transportTypes.size(), is(3));

        //verify the FQCNs were found
        assertThat(transportTypes, hasItem("class.name.one"));
        assertThat(transportTypes, hasItem("class.name.two"));
        assertThat(transportTypes, hasItem("class.name.phil"));
    }
    
    /**
     * Verify capabilities handler handles get capabilities correctly. 
     */
    @Test
    public void testCapabilitiesHandler() throws ObjectConverterException
    {
        //new handlers
        CapabilitiesResponseHandler handler1 = m_SUT.new CapabilitiesResponseHandler("class.name.one");
        CapabilitiesResponseHandler handler2 = m_SUT.new CapabilitiesResponseHandler("class.name.two");
        CapabilitiesResponseHandler handler3 = m_SUT.new CapabilitiesResponseHandler("class.name.three");
        
        //fake capabilities for each layer
        BaseCapabilities transBaseCaps = BaseCapabilities.newBuilder()
                .setDescription("fancy description")
                .setProductName("fancyName")
                .build();
        TransportLayerCapabilitiesGen.TransportLayerCapabilities caps1 = 
                TransportLayerCapabilitiesGen.TransportLayerCapabilities.newBuilder()
                .setBase(transBaseCaps)
                .build();  
        
        BaseCapabilities linkBaseCaps = BaseCapabilities.newBuilder()
                .setDescription("fancy description")
                .setProductName("fancyName")
                .build();
        LinkLayerCapabilitiesGen.LinkLayerCapabilities caps2 = 
                LinkLayerCapabilitiesGen.LinkLayerCapabilities.newBuilder()
                .setBase(linkBaseCaps)
                .setStaticMtu(false)
                .setPhysicalLinkRequired(true)
                .addPhysicalLinksSupported(CustomCommTypesGen.PhysicalLinkType.Enum.SERIAL_PORT)
                .setPerformBITSupported(false)
                .setSupportsAddressing(false)
                .setModality(CustomCommTypesGen.LinkLayerType.Enum.LINE_OF_SIGHT)
                .build();   
                
        BaseCapabilities physBaseCaps = BaseCapabilities.newBuilder()
                .setDescription("fancy description")
                .setProductName("fancyName")
                .build();
        PhysicalLinkCapabilitiesGen.PhysicalLinkCapabilities caps3 = 
                PhysicalLinkCapabilitiesGen.PhysicalLinkCapabilities.newBuilder()
                .setBase(physBaseCaps)
                .setLinkType(CustomCommTypesGen.PhysicalLinkType.Enum.SERIAL_PORT)
                .build();
        
        //response messages to process
        GetCapabilitiesResponseData response1 = GetCapabilitiesResponseData.newBuilder().
                setCommType(CommType.TransportLayer).setProductType("class.name.one").
                setTransportCapabilities(caps1).build();
        GetCapabilitiesResponseData response2 = GetCapabilitiesResponseData.newBuilder().
                setCommType(CommType.Linklayer).setProductType("class.name.two").
                setLinkCapabilities(caps2).build();
        GetCapabilitiesResponseData response3 = GetCapabilitiesResponseData.newBuilder().
                setCommType(CommType.PhysicalLink).setProductType("class.name.three").
                setPhysicalCapabilities(caps3).build();

        CustomCommsNamespace nameResponse1 = CustomCommsNamespace.newBuilder().
            setData(response1.toByteString()).
            setType(CustomCommsMessageType.GetCapabilitiesResponse).build();
        CustomCommsNamespace nameResponse2 = CustomCommsNamespace.newBuilder().
                setData(response2.toByteString()).
                setType(CustomCommsMessageType.GetCapabilitiesResponse).build();
        CustomCommsNamespace nameResponse3 = CustomCommsNamespace.newBuilder().
                setData(response3.toByteString()).
                setType(CustomCommsMessageType.GetCapabilitiesResponse).build();

        TerraHarvestPayload payload1 = TerraHarvestPayload.newBuilder().setNamespace(Namespace.CustomComms).
            setNamespaceMessage(response1.toByteString()).build();  
        TerraHarvestPayload payload2 = TerraHarvestPayload.newBuilder().setNamespace(Namespace.CustomComms).
            setNamespaceMessage(response2.toByteString()).build(); 
        TerraHarvestPayload payload3 = TerraHarvestPayload.newBuilder().setNamespace(Namespace.CustomComms).
            setNamespaceMessage(response3.toByteString()).build(); 
            
        TerraHarvestMessage thMessage1 = TerraHarvestMessageHelper.createTerraHarvestMessage(systemId1, 0,
            Namespace.CustomComms, 123, nameResponse1);
        TerraHarvestMessage thMessage2 = TerraHarvestMessageHelper.createTerraHarvestMessage(systemId1, 0,
                Namespace.CustomComms, 123, nameResponse2);
        TerraHarvestMessage thMessage3 = TerraHarvestMessageHelper.createTerraHarvestMessage(systemId1, 0,
                Namespace.CustomComms, 123, nameResponse3);
        
        when(m_Converter.convertToJaxb(caps1)).thenReturn(new TransportLayerCapabilities());
        when(m_Converter.convertToJaxb(caps2)).thenReturn(new LinkLayerCapabilities());
        when(m_Converter.convertToJaxb(caps3)).thenReturn(new PhysicalLinkCapabilities());
        
        //handle fake response
        handler1.handleResponse(thMessage1, payload1, nameResponse1, response1);
        handler2.handleResponse(thMessage2, payload2, nameResponse2, response2);
        handler3.handleResponse(thMessage3, payload3, nameResponse3, response3);
        
        assertThat(m_SUT.getCapabilities(systemId1, "class.name.one"), is(notNullValue()));
        assertThat(m_SUT.getCapabilities(systemId1, "class.name.two"), is(notNullValue()));
        assertThat(m_SUT.getCapabilities(systemId1, "class.name.three"), is(notNullValue()));    
        
        assertThat(m_SUT.getCapabilities(8675309, "jennyJenny"), is(nullValue()));
    }
    
    /**
     * Verify exception is thrown if a capabilities objects cannot be converted from protobuf to JAXB.
     */
    @Test
    public void testCapabilitiesHandlerException() throws ObjectConverterException
    {
        CapabilitiesResponseHandler handler1 = m_SUT.new CapabilitiesResponseHandler("class.name.one");

        BaseCapabilities transBaseCaps = BaseCapabilities.newBuilder()
                .setDescription("fancy description")
                .setProductName("fancyName")
                .build();
        TransportLayerCapabilitiesGen.TransportLayerCapabilities caps1 = 
                TransportLayerCapabilitiesGen.TransportLayerCapabilities.newBuilder()
                .setBase(transBaseCaps)
                .build();
        
        //response message to process
        GetCapabilitiesResponseData response1 = GetCapabilitiesResponseData.newBuilder().
                setCommType(CommType.TransportLayer).setProductType("class.name.one").
                setTransportCapabilities(caps1).build();

        CustomCommsNamespace nameResponse1 = CustomCommsNamespace.newBuilder().
            setData(response1.toByteString()).
            setType(CustomCommsMessageType.GetCapabilitiesResponse).build();

        TerraHarvestPayload payload1 = TerraHarvestPayload.newBuilder().setNamespace(Namespace.CustomComms).
            setNamespaceMessage(response1.toByteString()).build();  
            
        TerraHarvestMessage thMessage1 = TerraHarvestMessageHelper.createTerraHarvestMessage(systemId1, 0,
            Namespace.CustomComms, 123, nameResponse1);
        
        when(m_Converter.convertToJaxb(Mockito.any(Message.class))).thenThrow(new ObjectConverterException(""));
        
        handler1.handleResponse(thMessage1, payload1, nameResponse1, response1);
        
        verify(m_GrowlUtil).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_ERROR), 
                eq("Object Conversion Exception"),
                eq(String.format("An error occurred trying to retrieve capabilities for comm type [class.name.one]")), 
                Mockito.any(ObjectConverterException.class));
    }
    
    /**
     * Verify error response is handled for a capabilities response handler.
     */
    @Test
    public void testCapabilitiesHandlerErrorResponse() throws ObjectConverterException
    {
        CapabilitiesResponseHandler handler = m_SUT.new CapabilitiesResponseHandler("class.name.one");
        
        //response message to process
        GenericErrorResponseData responseData = GenericErrorResponseData.newBuilder().
            setError(ErrorCode.ILLEGAL_STATE).setErrorDescription("some description").build();
                
        BaseNamespace nameResponse = BaseNamespace.newBuilder().
            setData(responseData.toByteString()).
            setType(BaseMessageType.GenericErrorResponse).build();

        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().setNamespace(Namespace.Base).
            setNamespaceMessage(responseData.toByteString()).build();  
            
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(systemId1, 0,
            Namespace.Base, 123, nameResponse);
        
        when(m_Converter.convertToJaxb(Mockito.any(Message.class))).thenThrow(new ObjectConverterException(""));
        
        handler.handleResponse(thMessage, payload, nameResponse, responseData);
        
        verify(m_GrowlUtil).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_ERROR), 
                eq("Capabilities Retrieval Error"),
                eq("An error (ILLEGAL_STATE) occurred trying to retrieve capabilities for comm type" + 
                   " [class.name.one]: some description"));
    }
    
    /**
     * Test the request for layer types types.
     * Verify message sent.
     */
    @Test
    public void testRequestLayerTypesUpdate()
    {
        m_SUT.requestLayerTypesUpdate(123);

        //verify that handler will send get available comms type request
        verify(m_MessageFactory, times(3)).createCustomCommsMessage(
                eq(CustomCommsMessageType.GetAvailableCommTypesRequest), Mockito.any(Message.class));
        verify(m_MessageWrapper, times(3)).queue(eq(systemId1), Mockito.any(ResponseHandler.class));
    }
    
    /**
     * Verify that an image is properly retrieved when no transports or link layers are known.
     */
    @Test
    public void testGetImagePhysical()
    {
        String expectingPhysImg = m_SUT.getImage("dont.matter.nothing.is.there", 111);
        
        assertThat(expectingPhysImg, is("thoseIcons/comms/comms_serial.png"));
    }
    
    /**
     * Verify that an image is properly retrieved when a transport is known.
     */
    @Test
    public void testGetImageTransport() throws ObjectConverterException
    {
        //put comms transport layers in
        Event getTypes = mockMessageGetCommsTypesResponse(systemId1, CommType.TransportLayer);
        m_CommsHelper.handleEvent(getTypes);
        
        //new handlers
        CapabilitiesResponseHandler handler1 = m_SUT.new CapabilitiesResponseHandler("class.name.one");
       
        //fake capabilities for each layer
        BaseCapabilities baseCaps = BaseCapabilities.newBuilder()
                .setDescription("fancy description")
                .setProductName("fancyName")
                .build();
        TransportLayerCapabilitiesGen.TransportLayerCapabilities caps1 = 
                TransportLayerCapabilitiesGen.TransportLayerCapabilities.newBuilder()
                .setBase(baseCaps)
                .addLinkLayerModalitiesSupported(CustomCommTypesGen.LinkLayerType.Enum.LINE_OF_SIGHT)
                .build();        
       
        //response messages to process
        GetCapabilitiesResponseData response1 = GetCapabilitiesResponseData.newBuilder().
                setCommType(CommType.TransportLayer).setProductType("class.name.one").
                setTransportCapabilities(caps1).build();
       
        CustomCommsNamespace nameResponse1 = CustomCommsNamespace.newBuilder().
            setData(response1.toByteString()).
            setType(CustomCommsMessageType.GetCapabilitiesResponse).build();
       
        TerraHarvestPayload payload1 = TerraHarvestPayload.newBuilder().setNamespace(Namespace.CustomComms).
            setNamespaceMessage(response1.toByteString()).build();  
       
        TerraHarvestMessage thMessage1 = TerraHarvestMessageHelper.createTerraHarvestMessage(systemId1, 0,
            Namespace.CustomComms, 123, nameResponse1);
       
        TransportLayerCapabilities tCaps = mock(TransportLayerCapabilities.class);
        List<LinkLayerTypeEnum> enumList = new ArrayList<LinkLayerTypeEnum>();
        enumList.add(LinkLayerTypeEnum.LINE_OF_SIGHT);
        when(tCaps.getLinkLayerModalitiesSupported()).thenReturn(enumList);
        when(m_Converter.convertToJaxb(Mockito.any(Message.class))).thenReturn(tCaps);
      
        //handle fake response
        handler1.handleResponse(thMessage1, payload1, nameResponse1, response1);
        
        String expectingTransportImg = m_SUT.getImage("class.name.one", systemId1);
        
        assertThat(expectingTransportImg, is("thoseIcons/comms/comms_line_of_sight.png"));

        when(tCaps.getLinkLayerModalitiesSupported()).thenReturn(null);
        String expectingGenericImg = m_SUT.getImage("class.name.one", systemId1);
        assertThat(expectingGenericImg, is("thoseIcons/comms/comms_generic.png"));
        
        enumList.clear();
        
        when(tCaps.getLinkLayerModalitiesSupported()).thenReturn(enumList);
        expectingGenericImg = "";
        expectingGenericImg = m_SUT.getImage("class.name.one", systemId1);
        assertThat(expectingGenericImg, is("thoseIcons/comms/comms_generic.png"));
    }
    
    /**
     * Verify that an image is properly retrieved when a link layer is known. 
     */
    @Test
    public void testGetImageLinkLayer() throws ObjectConverterException
    {
        Event getTypes = mockMessageGetCommsTypesResponse(systemId1, CommType.Linklayer);
        m_CommsHelper.handleEvent(getTypes);
        
        CapabilitiesResponseHandler capsHandler = m_SUT.new CapabilitiesResponseHandler("class.name.two");
        BaseCapabilities baseCaps = BaseCapabilities.newBuilder()
                .setDescription("fancy description")
                .setProductName("fancyName")
                .build();
        LinkLayerCapabilitiesGen.LinkLayerCapabilities caps = 
                LinkLayerCapabilitiesGen.LinkLayerCapabilities.newBuilder()
                .setBase(baseCaps)
                .setStaticMtu(false)
                .setPhysicalLinkRequired(true)
                .addPhysicalLinksSupported(CustomCommTypesGen.PhysicalLinkType.Enum.SERIAL_PORT)
                .setPerformBITSupported(false)
                .setModality(CustomCommTypesGen.LinkLayerType.Enum.LINE_OF_SIGHT)
                .setSupportsAddressing(false)
                .build();
        
        GetCapabilitiesResponseData response = GetCapabilitiesResponseData.newBuilder().
                setCommType(CommType.Linklayer).setProductType("class.name.two").
                setLinkCapabilities(caps).build();
        CustomCommsNamespace nameResponse = CustomCommsNamespace.newBuilder().
                setData(response.toByteString()).
                setType(CustomCommsMessageType.GetCapabilitiesResponse).build();
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().setNamespace(Namespace.CustomComms).
                setNamespaceMessage(response.toByteString()).build(); 
        
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(systemId1, 0,
                Namespace.CustomComms, 123, nameResponse);
        
        LinkLayerCapabilities lCaps = mock(LinkLayerCapabilities.class);
        when(lCaps.getModality()).thenReturn(LinkLayerTypeEnum.LINE_OF_SIGHT);
        when(m_Converter.convertToJaxb(Mockito.any(Message.class))).thenReturn(lCaps);
        
        capsHandler.handleResponse(thMessage, payload, nameResponse, response);
        
        String expectingLinkImg = m_SUT.getImage("class.name.two", systemId1);
        assertThat(expectingLinkImg, is("thoseIcons/comms/comms_line_of_sight.png"));
        
        when(lCaps.getModality()).thenReturn(null);
        String expectingGeneric = m_SUT.getImage("class.name.two", systemId1);
        assertThat(expectingGeneric, is("thoseIcons/comms/comms_generic.png"));
    }
    
    /**
     * Verify the correct class is returned given the type.
     */
    @Test
    public void testGetPhysicalLinkClassByType() throws ObjectConverterException
    {
        // just make sure it doesn't throw error in this case
        m_SUT.getPhysicalLinkClassByType(123, PhysicalLinkTypeEnum.SERIAL_PORT);
        
        Event getTypes = mockMessageGetCommsTypesResponse(systemId1, CommType.PhysicalLink);
        m_CommsHelper.handleEvent(getTypes);
        
        handleTestCapabiltiesMessage("class.name.one", PhysicalLinkTypeEnum.SERIAL_PORT);
        
        assertThat(m_SUT.getPhysicalLinkClassByType(123, PhysicalLinkTypeEnum.SERIAL_PORT), is("class.name.one"));
    }

    @Test
    public void testGetLinkLayerRequiresPhysical() throws ObjectConverterException
    {
        Event getTypes = mockMessageGetCommsTypesResponse(systemId1, CommType.Linklayer);
        m_CommsHelper.handleEvent(getTypes);

        CapabilitiesResponseHandler capsHandler = m_SUT.new CapabilitiesResponseHandler("class.name.two");
        BaseCapabilities baseCaps = BaseCapabilities.newBuilder()
                .setDescription("fancy description")
                .setProductName("fancyName")
                .build();
        LinkLayerCapabilitiesGen.LinkLayerCapabilities caps = 
                LinkLayerCapabilitiesGen.LinkLayerCapabilities.newBuilder()
                .setBase(baseCaps)
                .setStaticMtu(false)
                .setPhysicalLinkRequired(false)
                .setPerformBITSupported(false)
                .setModality(CustomCommTypesGen.LinkLayerType.Enum.LINE_OF_SIGHT)
                .setSupportsAddressing(false)
                .build();

        GetCapabilitiesResponseData response = GetCapabilitiesResponseData.newBuilder().
                setCommType(CommType.Linklayer).setProductType("class.name.two").
                setLinkCapabilities(caps).build();
        CustomCommsNamespace nameResponse = CustomCommsNamespace.newBuilder().
                setData(response.toByteString()).
                setType(CustomCommsMessageType.GetCapabilitiesResponse).build();
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().setNamespace(Namespace.CustomComms).
                setNamespaceMessage(response.toByteString()).build(); 

        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(systemId1, 0,
                Namespace.CustomComms, 123, nameResponse);

        LinkLayerCapabilities lCaps = mock(LinkLayerCapabilities.class);
        when(lCaps.isPhysicalLinkRequired()).thenReturn(false);
        when(m_Converter.convertToJaxb(Mockito.any(Message.class))).thenReturn(lCaps);

        capsHandler.handleResponse(thMessage, payload, nameResponse, response);

        boolean requiresPhy = m_SUT.getLinkLayerRequiresPhysical(systemId1, "class.name.two");
        assertThat(requiresPhy, is(false));

        requiresPhy = m_SUT.getLinkLayerRequiresPhysical(systemId1, null);
        assertThat(requiresPhy, is(true));
    }

    /**
     * Mock interaction with SUT to receive capabilities for the test physical link plug-in. 
     */
    private void handleTestCapabiltiesMessage(String className, PhysicalLinkTypeEnum type)
        throws ObjectConverterException
    {
        CapabilitiesResponseHandler handler = m_SUT.new CapabilitiesResponseHandler(className);
        BaseCapabilities baseCaps = BaseCapabilities.newBuilder()
                .setDescription("fancy description")
                .setProductName("fancyName")
                .build();
        PhysicalLinkCapabilitiesGen.PhysicalLinkCapabilities caps = 
                PhysicalLinkCapabilitiesGen.PhysicalLinkCapabilities.newBuilder()
                .setBase(baseCaps)
                .setLinkType(PhysicalLinkTypeEnumConverter.convertJavaEnumToProto(type)).build();

        //response messages to process
        GetCapabilitiesResponseData response = GetCapabilitiesResponseData.newBuilder().
                setCommType(CommType.PhysicalLink).setProductType(className).
                setPhysicalCapabilities(caps).build();
        
        CustomCommsNamespace nameResponse = CustomCommsNamespace.newBuilder().
                setData(response.toByteString()).
                setType(CustomCommsMessageType.GetCapabilitiesResponse).build();

        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().setNamespace(Namespace.CustomComms).
            setNamespaceMessage(response.toByteString()).build();  
            
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(systemId1, 0,
            Namespace.CustomComms, 123, nameResponse);

        when(m_Converter.convertToJaxb(Mockito.any(Message.class))).thenReturn(
                new PhysicalLinkCapabilities().withLinkType(type));
        
        //handle fake response
        handler.handleResponse(thMessage, payload, nameResponse, response);
    }
             
    /**
     * This method creates a GetAssetTypesResponse message.
     * @param systemId
     *     the system id to use
     * @return
     *     an event that represents the type of event expected to be posted when this type of message is received
     */
    private Event mockMessageGetCommsTypesUpdateResponse(final int systemId, CommType type)
    {
        List<String> classNames = new ArrayList<String>();
        classNames.add("class.name.one");
        classNames.add("class.name.two");
        classNames.add("class.name.phil");
        
        GetAvailableCommTypesResponseData response = GetAvailableCommTypesResponseData.newBuilder().
                addAllProductType(classNames).setCommType(type).build();
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.CustomComms.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            CustomCommsMessageType.GetAvailableCommTypesResponse.toString());
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
    }
    
    /**
     * This method creates a GetAssetTypesResponse message.
     * @param systemId
     *     the system id to use
     * @return
     *     an event that represents the type of event expected to be posted when this type of message is received
     */
    private Event mockMessageGetCommsTypesResponse(final int systemId, CommType type)
    {
        List<String> classNames = new ArrayList<String>();
        classNames.add("class.name.one");
        classNames.add("class.name.two");
        classNames.add("class.name.three");        
        
        GetAvailableCommTypesResponseData response = GetAvailableCommTypesResponseData.newBuilder().
                addAllProductType(classNames).setCommType(type).build();
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.CustomComms.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            CustomCommsMessageType.GetAvailableCommTypesResponse.toString());
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
    }
}
