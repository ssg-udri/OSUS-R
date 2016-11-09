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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.
    AssetDirectoryServiceNamespace.AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetAssetTypesResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace.AssetMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.webapp.asset.AssetTypesMgrImpl.EventHelperAssetTypes;
import mil.dod.th.ose.gui.webapp.asset.AssetTypesMgrImpl.EventHelperControllerRemovedEvent;
import mil.dod.th.ose.gui.webapp.asset.AssetTypesMgrImpl.EventHelperRemoteBundleEvent;
import mil.dod.th.ose.gui.webapp.asset.AssetTypesMgrImpl.GetCapabilitiesResponseHandler;
import mil.dod.th.ose.gui.webapp.controller.ControllerMgr;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.shared.OSGiEventConstants;
import mil.dod.th.remote.lexicon.asset.capability.AssetCapabilitiesGen.AssetCapabilities;
import mil.dod.th.remote.lexicon.capability.BaseCapabilitiesGen.BaseCapabilities;

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
 * Test implementation of the {@link AssetTypesMgr}.
 * @author callen
 *
 */
public class TestAssetTypesMgrImpl
{
    private AssetTypesMgrImpl m_SUT;
    private BundleContextUtil m_BundleUtil;
    private EventHelperAssetTypes m_AssetHelper;
    private EventHelperControllerRemovedEvent m_ControllerRemoved;
    private EventHelperRemoteBundleEvent m_RemoteBundleUpdated;
    private MessageFactory m_MessageFactory;
    @SuppressWarnings("rawtypes") //TH-534:unable to parameterize at the moment
    private ServiceRegistration m_HandlerReg = mock(ServiceRegistration.class);
    private EventAdmin m_EventAdmin = mock(EventAdmin.class);
    private MessageWrapper m_MessageWrapper;
    private JaxbProtoObjectConverter m_Converter;
    private AssetImage m_AssetImageInterface = mock(AssetImage.class);
    
    //these fields are at this level so they can be used in multiple messages
    //system ids
    private int systemId1 = 123;
    private int systemId2 = 1234;
    
    @SuppressWarnings("unchecked")//because of the use of the dictionary for the event helper
    @Before
    public void setUp() throws ObjectConverterException
    {
        //mock services
        m_BundleUtil = mock(BundleContextUtil.class);
        m_MessageFactory = mock(MessageFactory.class);
        BundleContext bundleContext = mock(BundleContext.class);
        m_MessageWrapper = mock(MessageWrapper.class);
        m_Converter = mock(JaxbProtoObjectConverter.class);
        
        //create asset types helper
        m_SUT = new AssetTypesMgrImpl();
        
        //set dependencies
        m_SUT.setBundleContextUtil(m_BundleUtil);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setAssetImageInterface(m_AssetImageInterface);
        
        //mock behavior for event listener
        when(m_BundleUtil.getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
            Mockito.any(Dictionary.class))).thenReturn(m_HandlerReg);
        
        m_SUT.setConverter(m_Converter);
        
        mil.dod.th.core.asset.capability.AssetCapabilities caps =
                mock(mil.dod.th.core.asset.capability.AssetCapabilities.class);
        when(m_Converter.convertToJaxb(Mockito.any(Message.class))).thenReturn(caps);
        
        when(m_MessageFactory.createAssetDirectoryServiceMessage(
            Mockito.any(AssetDirectoryServiceMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        when(m_MessageFactory.createAssetMessage(Mockito.any(AssetMessageType.class), 
            Mockito.any(Message.class))).thenReturn(m_MessageWrapper);        
        
        //register helper
        m_SUT.registerEventHelper();
        m_SUT.setEventAdmin(m_EventAdmin);
        
        //verify
        ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(bundleContext, times(3)).registerService(eq(EventHandler.class), captor.capture(), 
            Mockito.any(Dictionary.class));
        verify(m_BundleUtil, times(3)).getBundleContext();
        
        //enter classes
        m_AssetHelper = (EventHelperAssetTypes) captor.getAllValues().get(0);
        m_ControllerRemoved = (EventHelperControllerRemovedEvent) captor.getAllValues().get(1);
        m_RemoteBundleUpdated = (EventHelperRemoteBundleEvent) captor.getAllValues().get(2);
    }
    
    /**
     * Test the predestroy unregistering of event handlers.
     * Verify that all 3 are unregistered.
     */
    @Test
    public void testPreDestroy()
    {
        m_SUT.unregisterHelper();

        //verify listeners are unregistered, there are 3
        verify(m_HandlerReg, times(3)).unregister();
    }
    
    /**
     * Test the handling of the get asset types.
     * Verify that the product types are correctly added.
     */
    @Test
    public void testGetAssetTypes()
    {
        //Verify that there is no data initially returned for systemId1. Later verify a message was sent to retrieve
        //the asset types from the controller.
        assertThat(m_SUT.getAssetFactoriesForControllerAsync(systemId1).size(), is(0));
        
        //load assets for system id
        Event getTypes = mockMessageGetAssetTypesResponse(systemId1);
        
        //this should render two asset types for the system id
        m_AssetHelper.handleEvent(getTypes);

        //verify
        assertThat(m_SUT.getAssetFactoriesForControllerAsync(systemId1).size(), is(2));
        
        //collect all the names
        Set<String> types = new HashSet<String>();
        for (AssetFactoryModel model : m_SUT.getAssetFactoriesForControllerAsync(systemId1))
        {
            types.add(model.getFullyQualifiedAssetType());
        }
        //verify the FQCNs were found
        assertThat(types, hasItem("super.awesome.OutOfThisWorld"));
        assertThat(types, hasItem("sometimes.youfeellikeanut.SometimesYouDont"));
        
        //Verify that a message was sent out retrieve asset types.
        verify(m_MessageFactory).createAssetDirectoryServiceMessage(
                AssetDirectoryServiceMessageType.GetAssetTypesRequest, null);
        verify(m_MessageWrapper).queue(systemId1, null);
    }
    
    @Test
    public void testGetAssetCapabilitiesResponseData()
    {
        //make sure event helpers are registered
        m_SUT.registerEventHelper();
        
        //Verify that there is no data initially returned for systemId1. Later verify a message was sent to retrieve
        //the asset types from the controller.
        assertThat(m_SUT.getAssetFactoriesForControllerAsync(systemId1).size(), is(0));
        
        //load assets for system id
        Event getTypes = mockMessageGetAssetTypesResponse(systemId1);
        
        //this should render two asset types for the system id
        m_AssetHelper.handleEvent(getTypes);
        
        ArgumentCaptor<GetCapabilitiesResponseHandler> handlerCaptor = 
                ArgumentCaptor.forClass(GetCapabilitiesResponseHandler.class);
        
        verify(m_MessageWrapper, times(3)).queue(eq(systemId1), handlerCaptor.capture());
        
        assertThat(handlerCaptor.getAllValues().size(), is(3));
        
        //can't be the first because that is the initial request which does not have a handler.
        //however, the second and third items are the ones that make a request for the capabilities
        //for an asset.
        GetCapabilitiesResponseHandler capturedHandler = handlerCaptor.getAllValues().get(1);

        //create the necessary messages
        BaseCapabilities baseCaps = BaseCapabilities.newBuilder().setDescription("Fake Description. Barf!")
                .setProductName("Barf!").build();
        AssetCapabilities caps = AssetCapabilities.newBuilder().setBase(baseCaps).build();
        
        GetCapabilitiesResponseData data = GetCapabilitiesResponseData.newBuilder().
                setProductType("sometimes.youfeellikeanut.SometimesYouDont").setCapabilities(caps).build();
        
        // build a base namespace message
        final AssetDirectoryServiceNamespace assetMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(AssetDirectoryServiceMessageType.GetCapabilitiesResponse).
                setData(data.toByteString()).
                build();
        //build a TerraHarvestPayLoad message
        final TerraHarvestPayload payLoad = TerraHarvestPayload.newBuilder().
                setNamespace(Namespace.AssetDirectoryService).
                setNamespaceMessage(assetMessage.toByteString()).
                build();
        // build a terra harvest message
        final TerraHarvestMessage fullMessage = TerraHarvestMessage.newBuilder().
                setVersion(RemoteConstants.SPEC_VERSION).
                setSourceId(systemId1).
                // send message back to source
                setDestId(0).
                setMessageId(22).
                setTerraHarvestPayload(payLoad.toByteString()).
                build();
        
        //handle an assetcapabilities response
        capturedHandler.handleResponse(fullMessage, payLoad, assetMessage, data);
        
        AssetFactoryModel modelWithCaps = m_SUT.
                getAssetFactoryForClassAsync(systemId1, "sometimes.youfeellikeanut.SometimesYouDont");
        
        assertThat(modelWithCaps, notNullValue());
        assertThat(modelWithCaps.getFactoryCaps(), notNullValue());     
    }

    /**
     * Test the handling of the get asset types when types already existed.
     * Verify that the product types are correctly updated.
     */
    @Test
    public void testGetAssetTypesUpdate()
    {
        //load assets for system id
        Event getTypes = mockMessageGetAssetTypesResponse(systemId1);
        
        //this should render two asset types for the system id
        m_AssetHelper.handleEvent(getTypes);
        
        assertThat(m_SUT.getAssetFactoriesForControllerAsync(systemId1).size(), is(2));
        
        //collect all the names
        Set<String> types = new HashSet<String>();
        for (AssetFactoryModel model : m_SUT.getAssetFactoriesForControllerAsync(systemId1))
        {
            types.add(model.getFullyQualifiedAssetType());
        }
        //verify the FQCNs were found
        assertThat(types, hasItem("super.awesome.OutOfThisWorld"));
        assertThat(types, hasItem("sometimes.youfeellikeanut.SometimesYouDont"));
        
        //update
        getTypes = mockMessageGetAssetTypesUpdateResponse(systemId1);
        m_AssetHelper.handleEvent(getTypes);

        assertThat(m_SUT.getAssetFactoriesForControllerAsync(systemId1).size(), is(4));
        
        //collect all the names
        types = new HashSet<String>();
        for (AssetFactoryModel model : m_SUT.getAssetFactoriesForControllerAsync(systemId1))
        {
            types.add(model.getFullyQualifiedAssetType());
        }
        //verify the FQCNs were found
        assertThat(types, hasItem("super.amazing.Stuff"));
        assertThat(types, hasItem("bza.ret.plkjds"));
    }
    
    /**
     * Test the handling of the get asset types for multiple controllers.
     * Verify that the product types are correctly added.
     * Verify that the removed controller event removes the correct controller. 
     */
    @Test
    public void testGetAssetTypesNoNewFactories()
    {
        //load assets for system id
        Event getTypes = mockMessageGetAssetTypesResponse(systemId1);
        
        //this should render two asset types for the system id
        m_AssetHelper.handleEvent(getTypes);
        
        assertThat(m_SUT.getAssetFactoriesForControllerAsync(systemId1).size(), is(2));

        //collect all the names
        Set<String> types = new HashSet<String>();
        for (AssetFactoryModel model : m_SUT.getAssetFactoriesForControllerAsync(systemId1))
        {
            types.add(model.getFullyQualifiedAssetType());
        }
        //verify the FQCNs were found
        assertThat(types, hasItem("super.awesome.OutOfThisWorld"));
        assertThat(types, hasItem("sometimes.youfeellikeanut.SometimesYouDont"));
        
        //this add the asset types again, should not add additional models.
        m_AssetHelper.handleEvent(getTypes);
        
        assertThat(m_SUT.getAssetFactoriesForControllerAsync(systemId1).size(), is(2));
    }
    
    /**
     * Test the handling of the get asset types for a controller.
     * Verify that the product types are correctly updated to not include removed types.
     */
    @Test
    public void testGetAssetTypesRemovedFactory()
    {
        //load assets for system id
        Event getTypes = mockMessageGetAssetTypesResponse(systemId1);
        
        //this should render two asset types for the system id
        m_AssetHelper.handleEvent(getTypes);
        
        assertThat(m_SUT.getAssetFactoriesForControllerAsync(systemId1).size(), is(2));

        //collect all the names
        Set<String> types = new HashSet<String>();
        for (AssetFactoryModel model : m_SUT.getAssetFactoriesForControllerAsync(systemId1))
        {
            types.add(model.getFullyQualifiedAssetType());
        }
        //verify the FQCNs were found
        assertThat(types, hasItem("super.awesome.OutOfThisWorld"));
        assertThat(types, hasItem("sometimes.youfeellikeanut.SometimesYouDont"));
        
        //make sure the fetching of factories correctly represents the removed factory is gone
        getTypes = mockMessageGetAssetTypesResponseOneRemoved(systemId1);
        m_AssetHelper.handleEvent(getTypes);
        
        assertThat(m_SUT.getAssetFactoriesForControllerAsync(systemId1).size(), is(1));
    }

    /**
     * Test the handling of the get asset types for multiple controllers.
     * Verify that the product types are correctly added.
     * Verify that the removed controller event removes the correct controller. 
     */
    @Test
    public void testGetAssetTypesMultiControllers()
    {
        //load assets for system id
        Event getTypes = mockMessageGetAssetTypesResponse(systemId1);
        
        //this should render two asset types for the system id
        m_AssetHelper.handleEvent(getTypes);
        
        assertThat(m_SUT.getAssetFactoriesForControllerAsync(systemId1).size(), is(2));

        //collect all the names
        Set<String> types = new HashSet<String>();
        for (AssetFactoryModel model : m_SUT.getAssetFactoriesForControllerAsync(systemId1))
        {
            types.add(model.getFullyQualifiedAssetType());
        }
        //verify the FQCNs were found
        assertThat(types, hasItem("super.awesome.OutOfThisWorld"));
        assertThat(types, hasItem("sometimes.youfeellikeanut.SometimesYouDont"));
        
        //verify that the new system id to be used does not return any models
        assertThat(m_SUT.getAssetFactoriesForControllerAsync(systemId2).isEmpty(), is(true));
        
        //check using different controller id
        getTypes = mockMessageGetAssetTypesUpdateResponse(systemId2);
        
        //this should render two assets for the system id
        m_AssetHelper.handleEvent(getTypes);
        
        assertThat(m_SUT.getAssetFactoriesForControllerAsync(systemId2).size(), is(4));
        
        //collect all the names
        types = new HashSet<String>();
        for (AssetFactoryModel model : m_SUT.getAssetFactoriesForControllerAsync(systemId2))
        {
            types.add(model.getFullyQualifiedAssetType());
        }
        //verify the FQCNs were found
        assertThat(types, hasItem("super.amazing.Stuff"));
        assertThat(types, hasItem("bza.ret.plkjds"));
        
        //get controller removed event
        Event removedController = mockControllerRemovedEvent(systemId1);
        m_ControllerRemoved.handleEvent(removedController);
        
        //verify that the system id is not longer known
        assertThat(m_SUT.getAssetFactoriesForControllerAsync(systemId1).isEmpty(), is(true));
    }
    
    /**
     * Verify method returns factory model and sends info request.
     */
    @Test
    public void testGetAssetFactoryForClassAsync()
    {
        //verify first time I get null and message is sent out
        AssetFactoryModel modelDNE = m_SUT.getAssetFactoryForClassAsync(systemId1, "no.matter.this.class.name");
        assertThat(modelDNE, nullValue());
        
        verify(m_MessageFactory).createAssetDirectoryServiceMessage(
                eq(AssetDirectoryServiceMessageType.GetAssetTypesRequest), (Message)eq(null));
        
        //load assets for system id
        Event getTypes = mockMessageGetAssetTypesResponse(systemId1);
        
        //this should render two asset types for the system id
        m_AssetHelper.handleEvent(getTypes);
        
        AssetFactoryModel foundModel = m_SUT.getAssetFactoryForClassAsync(systemId1, 
                "sometimes.youfeellikeanut.SometimesYouDont");
        
        assertThat(foundModel, notNullValue());
        assertThat(foundModel.getSimpleType(), is("SometimesYouDont"));
        
        modelDNE = m_SUT.getAssetFactoryForClassAsync(systemId1, "class.not.even.there");
        assertThat(modelDNE, is(nullValue()));
        
        // should still be holding at one, should not send another request even if not found
        verify(m_MessageFactory, times(1)).createAssetDirectoryServiceMessage(
                eq(AssetDirectoryServiceMessageType.GetAssetTypesRequest), (Message)eq(null));
    }
    
    /**
     * Test the bundle event handler handling an update remote bundle event.
     */
    @Test
    public void testHandleBundleEventUpdate()
    {
        //create a remote bundle updated event
        Map<String, Object> props = new HashMap<>();
        props.put("bundle.id", 5L);
        props.put("controller.id", systemId1);
        Event bundleUpdatedEvent = new Event(OSGiEventConstants.TOPIC_BUNDLE_UPDATED + 
            RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
        
        m_RemoteBundleUpdated.handleEvent(bundleUpdatedEvent);
        
        //verify that a message was sent out to retrieve asset types
        verify(m_MessageFactory).createAssetDirectoryServiceMessage(
                AssetDirectoryServiceMessageType.GetAssetTypesRequest, null);
        verify(m_MessageWrapper).queue(systemId1, null);
    }
    
    /**
     * This method creates a GetAssetTypesResponse message.
     * @param systemId
     *     the system id to use
     * @return
     *     an event that represents the type of event expected to be posted when this type of message is received
     */
    private Event mockMessageGetAssetTypesResponse(final int systemId)
    {
        List<String> productTypes = new ArrayList<String>();
        productTypes.add("super.awesome.OutOfThisWorld");
        productTypes.add("sometimes.youfeellikeanut.SometimesYouDont");
        List<String> productNames = new ArrayList<String>();
        productNames.add("nothing really!");
        GetAssetTypesResponseData response = GetAssetTypesResponseData.newBuilder().
            addAllProductType(productTypes).addAllProductName(productNames).build();
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.AssetDirectoryService.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE,
            AssetDirectoryServiceMessageType.GetAssetTypesResponse.toString());
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
    }
    
    /**
     * This method creates a GetAssetTypesResponse message where there is only one factory type returned.
     * @param systemId
     *     the system id to use
     * @return
     *     an event that represents the type of event expected to be posted when this type of message is received
     */
    private Event mockMessageGetAssetTypesResponseOneRemoved(final int systemId)
    {
        List<String> productTypes = new ArrayList<String>();
        productTypes.add("super.awesome.OutOfThisWorld");
        List<String> productNames = new ArrayList<String>();
        productNames.add("nothing really!");
        GetAssetTypesResponseData response = GetAssetTypesResponseData.newBuilder().
            addAllProductType(productTypes).addAllProductName(productNames).build();
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.AssetDirectoryService.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            AssetDirectoryServiceMessageType.GetAssetTypesResponse.toString());
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
    }

    /**
     * This method creates a GetAssetTypesResponse message with addition assets type for a controller.
     * @param systemId
     *     the system id to use
     * @return
     *     an event that represents the type of event expected to be posted when this type of message is received
     */
    private Event mockMessageGetAssetTypesUpdateResponse(final int systemId)
    {
        List<String> productTypes = new ArrayList<String>();
        productTypes.add("super.amazing.Stuff");
        productTypes.add("bza.ret.plkjds");
        productTypes.add("super.awesome.OutOfThisWorld");
        productTypes.add("sometimes.youfeellikeanut.SometimesYouDont");
        List<String> productNames = new ArrayList<String>();
        productNames.add("nothing really!");
        GetAssetTypesResponseData response = GetAssetTypesResponseData.newBuilder().
            addAllProductType(productTypes).addAllProductName(productNames).build();
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.AssetDirectoryService.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            AssetDirectoryServiceMessageType.GetAssetTypesResponse.toString());
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
    }
    
    /**
     * Create an event that mimics a controller being removed.
     * @param systemId
     *     the system id to be used as the removed controller id
     * @return
     *     an event that is of the type and structure expected when a controller is removed
     */
    private Event mockControllerRemovedEvent(final int systemId)
    {
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, systemId);
        //the event
        return new Event(ControllerMgr.TOPIC_CONTROLLER_REMOVED, props);
    }
}
