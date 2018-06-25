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
package mil.dod.th.ose.gui.webapp.advanced.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.inject.Inject;

import com.google.protobuf.Message;

import mil.dod.th.core.ConfigurationConstants;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.RemoteMetatypeConstants;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.MetaTypeMessages.AttributeDefinitionType;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetMetaTypeInfoRequestData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetMetaTypeInfoResponseData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.MetaTypeInfoType;
import mil.dod.th.core.remote.proto.MetaTypeMessages.MetaTypeNamespace.MetaTypeMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.webapp.advanced.BundleMgr;
import mil.dod.th.ose.gui.webapp.controller.ControllerMgr;
import mil.dod.th.ose.gui.webapp.general.RemoteEventRegistrationHandler;
import mil.dod.th.ose.gui.webapp.remote.RemoteEvents;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;

import org.glassfish.osgicdi.OSGiService;

/**
 * This bean is responsible for maintaining all meta type information stored on known controllers.
 * 
 * @author cweisenborn
 */
@ManagedBean(name = "metatypeMgr")
@ApplicationScoped
public class SystemMetaTypeMgrImpl implements SystemMetaTypeMgr
{
    /**
     * Reference to the synchronized map that stores all meta type information for configurations. The key is the
     * controller ID and the value is a list of meta type models that represent meta type information stored on that
     * controller for configurations.
     */
    private final Map<Integer, List<MetaTypeModel>> m_MetatypeListConfigs = 
            Collections.synchronizedMap(new HashMap<Integer, List<MetaTypeModel>>());
    
    /**
     * Reference to the synchronized map that stores all meta type information for factories. The key is the controller
     * ID and the value is a list of meta type models that represent the meta type information stored on that controller
     * for factories.
     */
    private final Map<Integer, List<MetaTypeModel>> m_MetatypeListFactories = 
            Collections.synchronizedMap(new HashMap<Integer, List<MetaTypeModel>>());
    
    /**
     * Reference to the bundle context utility.
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
     * Reference to an event handler that handles bundle events.
     */
    private BundleEventHandler m_BundleEventHandler;
    
    /**
     * Reference to an event handler that handles controller events.
     */
    private ControllerEventHandler m_ControllerEventHandler;
    
    /**
     * Reference to an event handler that handles meta type events.
     */
    private MetaTypeEventHandler m_MetatypeEventHandler;
    
    /**
     * Remote event response handler that keeps track of remote send event registration IDs.
     */
    private RemoteEventRegistrationHandler m_RemoteHandler;
    
    /**
     * Post construct method that instantiates and registers all listeners and response handlers.
     */
    @PostConstruct
    public void setup()
    {
        m_RemoteHandler = new RemoteEventRegistrationHandler(m_MessageFactory);
        m_BundleEventHandler = new BundleEventHandler();
        m_ControllerEventHandler = new ControllerEventHandler();
        m_MetatypeEventHandler = new MetaTypeEventHandler();
        
        m_BundleEventHandler.registerBundleEvents();
        m_ControllerEventHandler.registerControllerEvents();
        m_MetatypeEventHandler.registerMetaTypeEvents();
    }
    
    /**
     * Predestroy method that unregisters all event listeners before the bean is destroyed.
     */
    @PreDestroy
    public void cleanup()
    {
        m_BundleEventHandler.unregisterListener();
        m_ControllerEventHandler.unregisterListener();
        m_RemoteHandler.unregisterRegistrations();
        m_MetatypeEventHandler.unregisterListener();
    }
    
    /**
     * Sets the bundle context utility to use.
     * 
     * @param bundleUtil
     *          Bundle context utility to be set.
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
    
    @Override
    public List<MetaTypeModel> getConfigurationsListAsync(final int controllerId)
    {
        if (m_MetatypeListConfigs.get(controllerId) == null)
        {
            retrieveMetaInformation(controllerId);
            return new ArrayList<MetaTypeModel>();
        }

        return new ArrayList<MetaTypeModel>(m_MetatypeListConfigs.get(controllerId));
    }
    
    @Override
    public List<MetaTypeModel> getFactoriesListAsync(final int controllerId)
    {
        if (m_MetatypeListFactories.get(controllerId) == null)
        {
            retrieveMetaInformation(controllerId);
            return new ArrayList<MetaTypeModel>();
        }
        return new ArrayList<MetaTypeModel>(m_MetatypeListFactories.get(controllerId));
    }
    
    @Override
    public MetaTypeModel getConfigInformationAsync(final int controllerId, final String pid)
    {
        for (MetaTypeModel service: getConfigurationsListAsync(controllerId))
        {
            if (service.getPid().equals(pid))
            {
                return service;
            }
        }
        return null;
    }
    
    @Override
    public MetaTypeModel getFactoryInformationAsync(final int controllerId, final String factoryPid)
    {
        for (MetaTypeModel service: getFactoriesListAsync(controllerId))
        {
            if (service.getPid().equals(factoryPid))
            {
                return service;
            }
        }
        return null;
    }
    
    /**
     * Method used to send out a request to retrieve meta type information when none exists.
     * 
     * @param controllerId
     *          ID of the controller to retrieve meta type data from.
     */
    private void retrieveMetaInformation(final int controllerId)
    {
        if (!m_MetatypeListConfigs.containsKey(controllerId))
        {
            m_MetatypeListConfigs.put(controllerId, new ArrayList<MetaTypeModel>());
        }
        
        if (!m_MetatypeListFactories.containsKey(controllerId))
        {
            m_MetatypeListFactories.put(controllerId, new ArrayList<MetaTypeModel>());
        }
        
        final GetMetaTypeInfoRequestData requestMetaInfo = GetMetaTypeInfoRequestData.newBuilder().build();
        m_MessageFactory.createMetaTypeMessage(MetaTypeMessageType.GetMetaTypeInfoRequest, requestMetaInfo).
            queue(controllerId, null);
        
        registerRemoteBundleEvents(controllerId);
    }
    
    /**
     * Method that registers bundle events with the specified controllers event administration service.
     *  
     * @param controllerId
     *          ID of the controller to register the events with.
     */
    private void registerRemoteBundleEvents(final int controllerId)
    {       
        //List of events to register for.
        final List<String> topics = new ArrayList<String>();
        topics.add(RemoteMetatypeConstants.TOPIC_METATYPE_INFORMATION_AVAILABLE);
        
        //Send registration message. 
        RemoteEvents.sendEventRegistration(m_MessageFactory, topics, null, true, controllerId, m_RemoteHandler);
    }
    
    /**
     * Method that handles a local bundle information received event being posted to the event admin service or when
     * bundle started or updated remote event is posted.
     * 
     * @param controllerId
     *          ID of the controller the bundle is located on.
     * @param bundleId
     *          ID of the bundle that the information pertains to.
     */
    private void retrieveMetaInfo(final int controllerId, final Long bundleId)
    {
        if (!m_MetatypeListConfigs.containsKey(controllerId))
        {
            m_MetatypeListConfigs.put(controllerId, new ArrayList<MetaTypeModel>());
        }
        
        if (!m_MetatypeListFactories.containsKey(controllerId))
        {
            m_MetatypeListFactories.put(controllerId, new ArrayList<MetaTypeModel>());
        }
        
        final GetMetaTypeInfoRequestData requestMetaInfo;
        if (bundleId == null)
        {
            requestMetaInfo = GetMetaTypeInfoRequestData.newBuilder().build();
        }
        else
        {
            requestMetaInfo = GetMetaTypeInfoRequestData.newBuilder().setBundleId(bundleId).build();
        }
        m_MessageFactory.createMetaTypeMessage(MetaTypeMessageType.GetMetaTypeInfoRequest, requestMetaInfo).
            queue(controllerId, null);
    }
    
    /**
     * Method that is called when a bundle removed event is received. This method finds all meta type models
     * with the bundle ID of the bundle that was removed. It then removes all models with the specified bundle ID from
     * list.
     * 
     * @param modelList
     *          List of services to be checked. 
     * @param bundleId
     *          ID of the bundle that was removed.
     */
    private void handleBundleRemoved(final List<MetaTypeModel> modelList, final long bundleId)
    {
        final List<MetaTypeModel> removeModelList = new ArrayList<MetaTypeModel>();
        //Check the list for all service models with the specified bundle ID and add the index to the removeIndexList.
        for (MetaTypeModel model: modelList)
        {
            if (model.getBundleId() == bundleId)
            {
                removeModelList.add(model);
            }
        }
        //Remove all service models in the list with the indexes found above.
        for (MetaTypeModel model: removeModelList)
        {
            modelList.remove(model);
        }
    }
    
    /**
     * Method that handles a meta  type information response message.
     * 
     * @param controllerId
     *      ID of the controller the response originated from.
     * @param response
     *      The {@link GetMetaTypeInfoResponseData} message that contains the data.
     */
    private void handleMetaInfoResponse(final int controllerId, final GetMetaTypeInfoResponseData response)
    {
        final List<MetaTypeInfoType> metaInfoList = response.getMetaTypeList();
        
        for (MetaTypeInfoType metaInfo: metaInfoList)
        {
            if (metaInfo.getOcd().getDescription().equals(ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION))
            {
                Logging.log(
                    LogService.LOG_INFO, "Ignoring partial ObjectClassDefinition with PID [%s] from system id 0x%08x.", 
                        metaInfo.getPid(), controllerId);
                continue;
            }
            final MetaTypeModel metaModel = tryAddMetaTypeModel(metaInfo.getBundleId(), metaInfo.getPid(), 
                    controllerId, metaInfo.getIsFactory());
            for (AttributeDefinitionType attributeInfo: metaInfo.getAttributesList())
            {
                final AttributeModel attribute = new AttributeModel(attributeInfo);
                tryAddAttribute(metaModel, attribute);
            }
            cleanupAttributes(metaModel, metaInfo.getAttributesList());
        }
        //Build meta type model updated event.
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, controllerId);
        final Event metaTypeModelUpdated = new Event(TOPIC_METATYPE_MODEL_UPDATED, props);
        //Post the meta type model updated event.
        m_EventAdmin.postEvent(metaTypeModelUpdated);
    }
    
    /**
     * Method tries to add a meta type model to the configuration for factories meta type maps. If the model already
     * exists then it just returns the already existent model.
     * 
     * @param bundleId
     *      ID of the bundle the meta type information belongs to.
     * @param pid
     *      PID of the service the meta type information belongs to.
     * @param controllerId
     *      ID of the controller the meta type information is located on.
     * @param isFactory
     *      Boolean used to determine if the meta type information pertains to a factory.
     * @return
     *      The {@link MetaTypeModel} that was created or already existed.
     */
    private MetaTypeModel tryAddMetaTypeModel(final long bundleId, final String pid, final int controllerId, 
            final boolean isFactory)
    {
        if (isFactory)
        {
            for (MetaTypeModel model: m_MetatypeListFactories.get(controllerId))
            {
                if (model.getPid().equals(pid))
                {
                    return model;
                }
            }
            final MetaTypeModel newModel = new MetaTypeModel(pid, bundleId);
            m_MetatypeListFactories.get(controllerId).add(newModel);
            return newModel;
        }
        
        for (MetaTypeModel model: m_MetatypeListConfigs.get(controllerId))
        {
            if (model.getPid().equals(pid))
            {
                return model;
            }
        }
        final MetaTypeModel newModel = new MetaTypeModel(pid, bundleId);
        m_MetatypeListConfigs.get(controllerId).add(newModel);
        return newModel;
    }
    
    /**
     * Method tries to add an attribute model to the specified meta type model. If the attribute already exists then
     * it is replaced with the new model.
     * 
     * @param model
     *          Meta type model to try to add an attribute to.
     * @param attribute
     *          The attribute to be added.
     */
    private void tryAddAttribute(final MetaTypeModel model, final AttributeModel attribute)
    {  
        final List<AttributeModel> attributeList = model.getAttributes();
        for (int i = 0; i < attributeList.size(); i++)
        {
            if (attributeList.get(i).getId().equals(attribute.getId()))
            {
                //Replacing definition if it already exists with updated definition.
                attributeList.set(i, attribute);
                return;
            }
        }
        
        //Adding definition if it does not exist.
        model.getAttributes().add(attribute);
    }
    
    /**
     * Method that checks the model for any attributes that no longer exist and removes them. Compares the local list of
     * attributes stored in the model against the list of the attributes received from the remote system.
     * 
     * @param model
     *      {@link MetaTypeModel} containing the local list of attributes to be checked.
     * @param attributeList
     *      The list of {@link AttributeDefinitionType} that represents the attributes on the remote system.
     */
    private void cleanupAttributes(final MetaTypeModel model, final List<AttributeDefinitionType> attributeList)
    {
        final List<Integer> indexList = new ArrayList<Integer>();
        for (AttributeModel localAttribute: model.getAttributes())
        {
            boolean exists = false;
            for (AttributeDefinitionType remoteAttribute: attributeList)
            {
                if (remoteAttribute.getId().equals(localAttribute.getId()))
                {
                    exists = true;
                    break;
                }
            }
            
            if (!exists)
            {
                indexList.add(model.getAttributes().indexOf(localAttribute));
            }
        }
        
        for (Integer index: indexList)
        {
            model.getAttributes().remove(index.intValue());
        }
    }
    
    /**
     * Event handler that handles bundle events posted to the event admin service. This event handles retrieves meta
     * type information from bundles that are added and also removes all information pertaining to bundles that are
     * removed.
     */
    class BundleEventHandler implements EventHandler
    {
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is 
         * destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;
        
        /**
         * Registers the listener to listen for all bundle events posted.
         */
        public void registerBundleEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            
            // register to listen for bundles removed/updated or have their metatype information updated
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            final String[] topics = new String[]
            {
                RemoteMetatypeConstants.TOPIC_METATYPE_INFORMATION_AVAILABLE + RemoteConstants.REMOTE_TOPIC_SUFFIX,
                BundleMgr.TOPIC_BUNDLE_INFO_REMOVED,
                BundleMgr.TOPIC_BUNDLE_INFO_RECEIVED
            };
            props.put(EventConstants.EVENT_TOPIC, topics);
            
            //register the event handler that listens for bundle events.
            m_Registration = context.registerService(EventHandler.class, this, props);
        }
        
        @Override
        public void handleEvent(final Event event)
        {           
            final String topic = event.getTopic();
            final int controllerId = (Integer)event.getProperty(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID);

            if (topic.contains(RemoteMetatypeConstants.TOPIC_METATYPE_INFORMATION_AVAILABLE))
            {
                final long bundleId = (Long)event.getProperty(RemoteMetatypeConstants.EVENT_PROP_BUNDLE_ID);
                retrieveMetaInfo(controllerId, bundleId);                
            }
            else if (topic.contains(BundleMgr.TOPIC_BUNDLE_INFO_RECEIVED))
            {
                final Long bundleId = (Long)event.getProperty(EventConstants.BUNDLE_ID);
                retrieveMetaInfo(controllerId, bundleId);                
            }
            else if (topic.equals(BundleMgr.TOPIC_BUNDLE_INFO_REMOVED))
            {
                final long bundleId = (Long)event.getProperty(EventConstants.BUNDLE_ID);
                handleBundleRemoved(m_MetatypeListConfigs.get(controllerId), bundleId);
                handleBundleRemoved(m_MetatypeListFactories.get(controllerId), bundleId);
            }
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
     * Event handler that handles remove controller events posted to the event admin service. This handler will remove
     * all meta type information stored for the controller specified in the remove controller event.
     */
    class ControllerEventHandler implements EventHandler
    {
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is 
         * destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;
        
        /**
         * Method that registers the listener to listen for controller events.
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
            m_MetatypeListConfigs.remove(controllerId);
            m_MetatypeListFactories.remove(controllerId);
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
     * Handles meta type data from Remote Interface, includes attributes and values.
     * @author jgold
     *
     */
    class MetaTypeEventHandler implements EventHandler
    {
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is 
         * destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;
        
        /**
         * Method that registers the listener to listen for meta type events.
         */
        public void registerMetaTypeEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            // register to listen for controller removed events.
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, RemoteConstants.TOPIC_MESSAGE_RECEIVED);
            final String filterString = String.format("(&(%s=%s)(%s=%s))", 
                RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.MetaType.toString(),
                RemoteConstants.EVENT_PROP_MESSAGE_TYPE, MetaTypeMessageType.GetMetaTypeInfoResponse.toString());
            props.put(EventConstants.EVENT_FILTER, filterString);
            
            //register the event handler that listens for controllers being removed.
            m_Registration = context.registerService(EventHandler.class, this, props);
        }
        
        @Override
        public void handleEvent(final Event event)
        {
            final int controllerId = 
                    ((TerraHarvestMessage)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE)).getSourceId();
            final Message dataMessage = (Message)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE);
        
            handleMetaInfoResponse(controllerId, (GetMetaTypeInfoResponseData)dataMessage);
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
