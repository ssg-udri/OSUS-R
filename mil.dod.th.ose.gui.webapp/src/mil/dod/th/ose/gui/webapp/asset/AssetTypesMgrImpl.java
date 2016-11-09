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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.inject.Inject;

import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.Message;

import mil.dod.th.core.asset.capability.AssetCapabilities;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace.
    AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetAssetTypesResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetCapabilitiesRequestData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.webapp.controller.ControllerMgr;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.shared.OSGiEventConstants;

import org.glassfish.osgicdi.OSGiService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;


/**
 * Implementation of the {@link AssetTypesMgr}.
 * @author callen
 *
 */
@ManagedBean(name = "assetTypesMgr")
@ApplicationScoped
public class AssetTypesMgrImpl implements AssetTypesMgr 
{
    /**
     * Map that contains the system ID and a corresponding set of {@link AssetFactoryModel}s.
     */
    private final Map<Integer, Set<AssetFactoryModel>> m_AssetFactories;
    
    /**
     * Event handler helper class. Listens for Asset namespace messages.
     */
    private EventHelperAssetTypes m_EventHelperAsset;
    
    /**
     * Event handler helper class. Listens for the 'controller removed' event.
     */
    private EventHelperControllerRemovedEvent m_ControllerEventListener;
    
    /**
     * Event handler helper class.  Listens for remote bundle update events.
     */
    private EventHelperRemoteBundleEvent m_RemoteBundleUpdateEventListener;
    
    /**
     * Service that retrieves the bundle context of this bundle.
     */
    @Inject
    private BundleContextUtil m_BundleUtil;
    
    /**
     * Inject the MessageFactory service for creating remote messages.
     */
    @Inject @OSGiService
    private MessageFactory m_MessageFactory;
    
    /**
     * Reference to the OSGi event admin service.
     */
    @Inject @OSGiService
    private EventAdmin m_EventAdmin;
    
    /**
     * The {@link JaxbProtoObjectConverter} responsible converting between JAXB and protocol buffer objects. 
     */
    @Inject @OSGiService
    private JaxbProtoObjectConverter m_Converter;
    
    /**
     * The {@link AssetImage} image display interface to use.
     */
    @Inject
    private AssetImage m_AssetImageInterface;

    /**
     * Asset types helper constructor.
     */
    public AssetTypesMgrImpl()
    {
        m_AssetFactories = Collections.synchronizedMap(new HashMap<Integer, Set<AssetFactoryModel>>());
    }
    
    /**
     * Register event listener.
     */
    @PostConstruct
    public void registerEventHelper()
    {
        //instantiate handlers
        m_EventHelperAsset = new EventHelperAssetTypes();
        m_ControllerEventListener = new EventHelperControllerRemovedEvent();
        m_RemoteBundleUpdateEventListener = new EventHelperRemoteBundleEvent();
        
        //register to listen to delegated events
        m_EventHelperAsset.registerForEvents();
        m_ControllerEventListener.registerControllerEvents();
        m_RemoteBundleUpdateEventListener.registerRemoteBundleEvents();       
    }
    
    /**
     * Unregister handler before destruction of the bean.
     */
    @PreDestroy
    public void unregisterHelper()
    {
        //Unregister for events
        m_EventHelperAsset.unregisterListener();
        m_ControllerEventListener.unregisterListener();
        m_RemoteBundleUpdateEventListener.unregisterListener();
    }
    
    /**
     * Set the {@link BundleContextUtil} utility service.
     * @param bundleUtil
     *     the bundle context utility service to use
     */
    public void setBundleContextUtil(final BundleContextUtil bundleUtil)
    {
        m_BundleUtil = bundleUtil;
    }
    
    /**
     * Method that sets the MessageFactory service to use.
     * 
     * @param messageFactory
     *          MessageFactory service to set.
     */
    public void setMessageFactory(final MessageFactory messageFactory)
    {
        m_MessageFactory = messageFactory;
    }
    
    /**
     * Sets the event admin service to use.
     * 
     * @param eventAdmin
     *          Event admin service to be set.
     */
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Set the {@link JaxbProtoObjectConverter}.
     * 
     * @param converter
     *     the service responsible for converting between JAXB and protocol buffer objects.
     */
    @Reference
    public void setConverter(final JaxbProtoObjectConverter converter)
    {
        m_Converter = converter;        
    }
    
    /**
     * Set the {@link AssetImage} image display interface.
     * @param imgInterface
     *  the image interface to use.
     */
    public void setAssetImageInterface(final AssetImage imgInterface)
    {
        m_AssetImageInterface = imgInterface;
    }
    
    @Override
    public synchronized List<AssetFactoryModel> getAssetFactoriesForControllerAsync(final int controllerId)
    {
        //list to return
        final List<AssetFactoryModel> models = new ArrayList<AssetFactoryModel>();
       
        //check for mapping to the controller id
        if (!m_AssetFactories.containsKey(controllerId))
        {
            m_AssetFactories.put(controllerId, new HashSet<AssetFactoryModel>());
            
            //request for the controllers asset factories
            m_MessageFactory.createAssetDirectoryServiceMessage(AssetDirectoryServiceMessageType.GetAssetTypesRequest, 
                    null).queue(controllerId, null);
        }
        models.addAll(m_AssetFactories.get(controllerId));
        
        return models;
    }
    
    @Override
    public synchronized AssetFactoryModel getAssetFactoryForClassAsync(final int controllerId, final String className)
    {
        if (m_AssetFactories.containsKey(controllerId))
        {
            final Set<AssetFactoryModel> map = m_AssetFactories.get(controllerId);
            
            for (AssetFactoryModel model : map)
            {
                //found the model
                if (className.equals(model.getFullyQualifiedAssetType()))
                {
                    return model;
                }
            }
            
            Logging.log(LogService.LOG_WARNING, "No asset factory model found for classname [%s]", className);
        }
        else
        {
            //new controller
            m_AssetFactories.put(controllerId, new HashSet<AssetFactoryModel>());
            
            //send request for information
            m_MessageFactory.createAssetDirectoryServiceMessage(
                AssetDirectoryServiceMessageType.GetAssetTypesRequest, null).
                    queue(controllerId, null);
        }
        
        return null;
    }
    
    /**
     * Try to add an asset factory model.
     * @param assetFact
     *    the asset factory model to update/add
     *@param systemId
     *    the system id to which this factory belongs
     */
    private synchronized void tryAddAssetFactoryModel(final AssetFactoryModel assetFact, final int systemId)
    {
        final String productType = assetFact.getFullyQualifiedAssetType();
        final AssetFactoryModel model = findAssetFactoryModel(systemId, productType);
        if (model == null && m_AssetFactories.containsKey(systemId))
        {
            //add to respective system id
            final Set<AssetFactoryModel> contModels = m_AssetFactories.get(systemId);
            contModels.add(assetFact);
        }
        else if (model == null)
        {
            final Set<AssetFactoryModel> modelSet = new HashSet<AssetFactoryModel>();
            modelSet.add(assetFact);
            m_AssetFactories.put(systemId, modelSet);
        }
        sendCapabilitiesRequests(productType, systemId);
    }
    
    /**
     * Function to send a capabilities request to a certain system.
     * @param classType
     *  the class type for which a capabilities request is required
     * @param systemId
     *  the id of the system to send the request to
     */
    private void sendCapabilitiesRequests(final String classType, final int systemId)
    {
        //send out GetCapabilitiesRequest message
        final GetCapabilitiesRequestData requestCap = GetCapabilitiesRequestData.newBuilder().
                setProductType(classType).build();
        
        final GetCapabilitiesResponseHandler responseHandler = new GetCapabilitiesResponseHandler(systemId);
        
        m_MessageFactory.createAssetDirectoryServiceMessage(AssetDirectoryServiceMessageType.GetCapabilitiesRequest, 
                requestCap).
            queue(systemId, responseHandler);
    }
    
    /**
     * Event response handling for the get asset factories request.
     * @param message
     *     the message that contains the response information
     * @param systemId
     *     the system id from which the response message originated
     */
    private synchronized void eventGetAssetTypesResponseData(final GetAssetTypesResponseData message, 
        final int systemId)
    {
        final GetAssetTypesResponseData response = message;
        
        final List<String> productTypes = response.getProductTypeList();
        
        for (String productType : productTypes)
        {
            final AssetFactoryModel model = new AssetFactoryModel(productType, m_AssetImageInterface);
            tryAddAssetFactoryModel(model, systemId);
        }

        //check that the lookup of factories does not contain factories that are no longer associated with the 
        //given controller
        
        //list to hold models that need to be removed
        final List<AssetFactoryModel> models = new ArrayList<AssetFactoryModel>();
        for (AssetFactoryModel model : getAssetFactoriesForControllerAsync(systemId))
        {
            if (!productTypes.contains(model.getFullyQualifiedAssetType()))
            {
                models.add(model);
            }
        }

        //remove models that are no longer associated with the system id
        for (AssetFactoryModel model : models)
        {
            m_AssetFactories.get(systemId).remove(model);
        }
        
        //Build the asset types updated event.
        final Map<String, Object> props = new HashMap<String, Object>();
        final Event assetTypesUpdated = new Event(TOPIC_ASSET_TYPES_UPDATED, props);
        //Post the asset status updated event
        m_EventAdmin.postEvent(assetTypesUpdated);
    }
    
    /**
     * Event response handling for the get asset capabilities request.
     * @param data
     *  the message that contains the response information
     * @param systemId
     *  the system id from which the response message originated
     */
    private synchronized void eventGetAssetCapabilitiesResponseData(final GetCapabilitiesResponseData data, 
            final int systemId)
    {
        //get Capabilities
        try
        {
            final AssetCapabilities caps = (AssetCapabilities)m_Converter.convertToJaxb(data.getCapabilities());
            final String assetClassName = data.getProductType();
        
            final AssetFactoryModel model = findAssetFactoryModel(systemId, assetClassName);
            
            if (model != null)
            {
                model.setFactoryCaps(caps);
            }
        }
        catch (final ObjectConverterException exception)
        {
            Logging.log(LogService.LOG_ERROR, exception, 
                    "An error occurred trying to parse an asset capabilities object" 
                    + " of class type %s for system id 0x%08x ", data.getProductType(), systemId);
        }
    }

    /**
     * Find asset factory model in look up.
     * @param controllerId
     *    the controller id that the factory belongs to
     * @param productType
     *     the name of the product that the factory creates
     * @return
     *    the asset factory model that represents the request factory, or null if not found
     */
    private synchronized AssetFactoryModel findAssetFactoryModel(final int controllerId, final String productType)
    {
        final Set<AssetFactoryModel> models = m_AssetFactories.get(controllerId);
        if (models != null)
        {
            for (AssetFactoryModel model : models)
            {
                if (model.getFullyQualifiedAssetType().equals(productType))
                {
                    return model;
                }
            }
        }
        return null;
    }
    
    /**
     * Event handler for AssetDirectory namespace get asset types.
     * @author callen
     */
    public class EventHelperAssetTypes implements EventHandler
    {
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is 
         * destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;
        
        /**
         * Method to register this event handler for the message received topic.
         */
        public void registerForEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            // register to listen to the AssetDirectoryService events
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            final String[] topics = {RemoteConstants.TOPIC_MESSAGE_RECEIVED};
            props.put(EventConstants.EVENT_TOPIC, topics);
            final String filterString = String.format("(&(%s=%s)(%s=%s))", 
                RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.AssetDirectoryService.toString(),
                RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
                    AssetDirectoryServiceMessageType.GetAssetTypesResponse.toString());
            props.put(EventConstants.EVENT_FILTER, filterString);
            
            //register the event handler that listens for asset directory responses
            m_Registration = context.registerService(EventHandler.class, this, props);
        } 
        
        @Override
        public void handleEvent(final Event event)
        {
            //pull out event props
            final int systemId = (Integer)event.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID);
            
            eventGetAssetTypesResponseData(
                    (GetAssetTypesResponseData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), systemId);
        }
        
        /**
         * Unregister the event listener.
         */
        public void unregisterListener()
        {
            m_Registration.unregister();
        }
    }
    
    /**
     * Handles controller events and performs actions based on events received.
     */
    class EventHelperControllerRemovedEvent implements EventHandler
    {
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is
         * being destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;
        
        /**
         * Method to register this event handler for the controller removed event.
         */
        public void registerControllerEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            // register to listen for controller removed events.
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, ControllerMgr.TOPIC_CONTROLLER_REMOVED);
            
            //register the event handler that listens for controllers being removed.
            m_Registration = context.registerService(EventHandler.class, this, props);
        }

        @Override
        public void handleEvent(final Event event)
        {
            final int controllerId = (Integer)event.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID);
            m_AssetFactories.remove(controllerId);
        }
        
        /**
         * Unregister the event listener.
         */
        public void unregisterListener()
        {
            m_Registration.unregister();
        }
    }
    
    /**
     * Handles get capabilities response data events.
     */
    class GetCapabilitiesResponseHandler implements ResponseHandler
    {
        /**
         * Id of the system for which an asset is expected to return capabilities information.
         */
        private final int m_SystemId;
        
        /**
         * Constructor.
         * @param systemId
         *  the system id for which asset capabilities have been requested
         */
        GetCapabilitiesResponseHandler(final int systemId)
        {
            m_SystemId = systemId;
        }
        
        @Override
        public void handleResponse(final TerraHarvestMessage message, final TerraHarvestPayload payload,
                final Message namespaceMessage, final Message dataMessage)
        {
            eventGetAssetCapabilitiesResponseData((GetCapabilitiesResponseData)dataMessage, m_SystemId);
        } 
    }
    
    /**
     * Handles bundle events and performs actions based on the type of bundle event received.
     */
    class EventHelperRemoteBundleEvent implements EventHandler
    {
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is 
         * destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;
        
        /**
         * Method to register this event handler to listen for local bundle events.
         */
        public void registerRemoteBundleEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            // register to listen for bundle updated events.
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            final List<String> topics = new ArrayList<String>();
            topics.add(OSGiEventConstants.TOPIC_BUNDLE_UPDATED + RemoteConstants.REMOTE_TOPIC_SUFFIX);
            props.put(EventConstants.EVENT_TOPIC, topics);
            
            //register the event handler that listens for bundles being updated.
            m_Registration = context.registerService(EventHandler.class, this, props);
        }
        
        @Override
        public void handleEvent(final Event event)
        {
            final int controllerId = (Integer)event.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID);
            
            m_MessageFactory.createAssetDirectoryServiceMessage(AssetDirectoryServiceMessageType.GetAssetTypesRequest, 
                    null).queue(controllerId, null);
        }
        
        /**
         * Unregister the event listener.
         */
        public void unregisterListener()
        {
            m_Registration.unregister();
        }
    }  
}
