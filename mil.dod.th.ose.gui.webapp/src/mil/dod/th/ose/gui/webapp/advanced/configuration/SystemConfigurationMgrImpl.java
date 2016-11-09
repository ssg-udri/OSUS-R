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
import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.inject.Inject;

import com.google.protobuf.Message;

import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace.ConfigAdminMessageType;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigurationInfoType;
import mil.dod.th.core.remote.proto.ConfigMessages.CreateFactoryConfigurationRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.CreateFactoryConfigurationResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.DeleteConfigurationRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetConfigurationInfoRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetConfigurationInfoResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.SetPropertyRequestData;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationResponseData;
import mil.dod.th.core.remote.proto.EventMessages.UnregisterEventRequestData;
import mil.dod.th.core.remote.proto.MapTypes.SimpleTypesMapEntry;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.ose.config.event.constants.ConfigurationEventConstants;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.webapp.controller.ControllerMgr;
import mil.dod.th.ose.gui.webapp.remote.RemoteEvents;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.ose.shared.SharedMessageUtils;

import org.glassfish.osgicdi.OSGiService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * Managed bean that is responsible for storing information for the configurations of all known controllers.
 * 
 * @author cweisenborn
 */
@ManagedBean(name = "configAdminMgr") //NOCHECKSTYLE:Fan out complexity, need to be able to handle different message
@ApplicationScoped                    //namespaces. As such need to include to handle logic for all the message types.
public class SystemConfigurationMgrImpl implements SystemConfigurationMgr
{
    /**
     * LDAP string used to filter the configuration admin service by a specific PID.
     */
    private static String m_ServiceFilter = "(service.pid=%s)";
    
    /**
     * Map that contains all known controllers and a map of configurations for each controller. The key is an integer
     * which is the ID of the controller. The value is another map with a key that is the PID of the configuration and a
     * value that is a model which represents the configuration.
     */
    private final Map<Integer, Map<String, ConfigAdminModel>> m_ControllerConfigList = 
            Collections.synchronizedMap(new HashMap<Integer, Map<String, ConfigAdminModel>>());
    
    /**
     * Map of the registered event IDs. The key is the controller ID and the value is the event registration ID that
     * corresponds to the registered events to listen for on the controller.
     */
    private final Map<Integer, Integer> m_EventRegistraionIds = 
            Collections.synchronizedMap(new HashMap<Integer, Integer>());
    
    /**
     * Utility class used to retrieve the bundle context.
     */
    @Inject
    private BundleContextUtil m_BundleUtil;
    
    /**
     * Utility class used to display growl messages.
     */
    @Inject
    private GrowlMessageUtil m_GrowlUtil;
    
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
     * Reference to a configuration response handler.
     */
    private ConfigurationResponseHandler m_ConfigResponseHandler;
    
    /**
     * Reference to a configuration event handler.
     */
    private ConfigurationEventHandler m_ConfigEventHandler;
    
    /**
     * Reference to a controller event handler.
     */
    private ControllerEventHandler m_ControllerEventHandler;
    
    /**
     * Reference to a register events response handler.
     */
    private RegisterEventsResponseHandler m_RegisterEventsResponseHandler;
    
    /**
     * Reference to a configuration admin event handler.
     */
    private ConfigurationAdminEventHandler m_ConfigurationAdminEventHandler;
    
    /**
     * Post construct method. Method instantiates all listeners and response handlers.
     */
    @PostConstruct
    public void setup()
    {   
        m_ConfigResponseHandler = new ConfigurationResponseHandler();
        m_ConfigEventHandler = new ConfigurationEventHandler();
        m_ControllerEventHandler = new ControllerEventHandler();
        m_ConfigurationAdminEventHandler = new ConfigurationAdminEventHandler();
        m_RegisterEventsResponseHandler = new RegisterEventsResponseHandler();
        m_ConfigEventHandler.registerConfigurationEvents();
        m_ControllerEventHandler.registerControllerEvents();
        m_ConfigurationAdminEventHandler.registerControllerEvents();
    }
    
    /**
     * Predestroy method. Method unregisters all registered listeners.
     */
    @PreDestroy
    public void cleanup()
    {
        m_ConfigEventHandler.unregisterListener();
        m_ControllerEventHandler.unregisterListener();
        m_ConfigurationAdminEventHandler.unregisterListener();
        
        for (int controllerId: m_EventRegistraionIds.keySet())
        {
            final UnregisterEventRequestData unregsiterMessage = 
                    UnregisterEventRequestData.newBuilder().setId(m_EventRegistraionIds.get(controllerId)).build();
            
            m_MessageFactory.createEventAdminMessage(EventAdminMessageType.UnregisterEventRequest, unregsiterMessage).
                queue(controllerId, null);
        }
    }
    
    /**
     * Sets the bundle context utility to use.
     * 
     * @param bundleUtil
     *          Bundle context utility to set.
     */
    public void setBundleContextUtil(final BundleContextUtil bundleUtil)
    {
        m_BundleUtil = bundleUtil;
    }
    
    /**
     * Sets the growl message utility to use.
     * 
     * @param growlUtil
     *          Growl message utility to set.
     */
    public void setGrowlMessageUtil(final GrowlMessageUtil growlUtil)
    {
        m_GrowlUtil = growlUtil;
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
    public synchronized Map<String, ConfigAdminModel> getConfigurationsAsync(final int controllerId)
    {
        if (!m_ControllerConfigList.containsKey(controllerId))
        {
            // TODO TH-1246: this assumes that the request below will actually come back and fill up the map. Need
            // to check for a failure response and try again.
            m_ControllerConfigList.put(controllerId, new HashMap<String, ConfigAdminModel>());
        
            final GetConfigurationInfoRequestData configInfoRequest = 
                    GetConfigurationInfoRequestData.newBuilder().setIncludeProperties(true).build();
            
            m_MessageFactory.createConfigAdminMessage(ConfigAdminMessageType.GetConfigurationInfoRequest, 
                    configInfoRequest).queue(controllerId, m_ConfigResponseHandler);
            
            registerRemoteEvents(controllerId);
        }
        return new HashMap<String, ConfigAdminModel>(m_ControllerConfigList.get(controllerId));   
    }
    
    @Override
    public synchronized ConfigAdminModel getConfigurationByPidAsync(final int controllerId, final String pid)
    {
        if (getConfigurationsAsync(controllerId).containsKey(pid))
        {
            return getConfigurationsAsync(controllerId).get(pid);
        }
        else 
        {
            for (ConfigAdminModel config: getConfigurationsAsync(controllerId).values())
            {
                if (config.isFactory() && config.getFactoryConfigurations().containsKey(pid))
                {
                    return config.getFactoryConfigurations().get(pid);
                }
            }
        }
        return null;
    }
    
    @Override
    public synchronized Map<String, ConfigAdminModel> getFactoryConfigurationsByFactoryPidAsync(
            final int controllerId, final String factoryPid)
    {
        if (getConfigurationsAsync(controllerId).containsKey(factoryPid))
        {
            final ConfigAdminModel factoryModel = getConfigurationsAsync(controllerId).get(factoryPid);
            if (factoryModel.isFactory())
            {
                return factoryModel.getFactoryConfigurations();
            }
        }
        return null;
    }
    
    @Override
    public synchronized List<ConfigAdminPropertyModel> getPropertiesByPidAsync(final int controllerId, final String pid)
    {
        if (getConfigurationByPidAsync(controllerId, pid) == null)
        {
            return null;
        }
        else
        {
            return getConfigurationByPidAsync(controllerId, pid).getProperties();
        }
    }
    
    @Override
    public void setConfigurationValueAsync(final int controllerId, final String pid, 
            final List<ModifiablePropertyModel> properties)
    {
        final SetPropertyRequestData.Builder setPropertyRequest = 
                SetPropertyRequestData.newBuilder().setPid(pid);
        for (ModifiablePropertyModel property: properties)
        {
            final Multitype configValue = SharedMessageUtils.convertObjectToMultitype(property.getValue());
            final SimpleTypesMapEntry configProp = SimpleTypesMapEntry.newBuilder().setKey(property.getKey()).setValue(
                    configValue).build();
            setPropertyRequest.addProperties(configProp);
        }
        
        m_MessageFactory.createConfigAdminMessage(ConfigAdminMessageType.SetPropertyRequest, 
                setPropertyRequest.build()).queue(controllerId, m_ConfigResponseHandler);
    }
    
    @Override
    public void createFactoryConfigurationAsync(final int controllerId, final String factoryPid)
    {
        final CreateFactoryConfigurationRequestData createRequest = 
                CreateFactoryConfigurationRequestData.newBuilder().setFactoryPid(factoryPid).build();
        
        m_MessageFactory.createConfigAdminMessage(ConfigAdminMessageType.CreateFactoryConfigurationRequest, 
            createRequest).queue(controllerId, null);
    }
    
    @Override
    public void removeConfigurationAsync(final int controllerId, final String pid)
    {
        final DeleteConfigurationRequestData deleteRequest = 
                DeleteConfigurationRequestData.newBuilder().setPid(pid).build();
        
        m_MessageFactory.createConfigAdminMessage(ConfigAdminMessageType.DeleteConfigurationRequest, deleteRequest).
            queue(controllerId, null);
    }
    
    /**
     * Method used to register all remote event admin events.
     * 
     * @param controllerId
     *          ID of the controller to register the events on.
     */
    private void registerRemoteEvents(final int controllerId)
    {
        //List of configuration events to register for.
        final List<String> topics = new ArrayList<String>();
        topics.add(ConfigurationEventConstants.TOPIC_ALL_CONFIGURATION_EVENTS);
             
        RemoteEvents.sendEventRegistration(m_MessageFactory, topics, null, true, controllerId, 
                m_RegisterEventsResponseHandler);
    }
    
    /**
     * Method that handles a configuration info response message.
     * 
     * @param controllerId
     *          ID of the controller the response pertains to.
     * @param dataMessage
     *          Message that contains the configuration info response data.
     */
    private void handleConfigInfoResponse(final int controllerId, final GetConfigurationInfoResponseData dataMessage)
    {
        final List<ConfigurationInfoType> configList = dataMessage.getConfigurationsList();
        
        for (ConfigurationInfoType config: configList)
        {            
            //Check if configuration has a factory PID.
            if (config.hasFactoryPid())
            {
                tryAddFactoryConfigModel(controllerId, config);
            }
            else
            {
                tryAddConfigModel(controllerId, config);
            } 
        }
        
        //Build the config model updated event.
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, controllerId);
        if (configList.size() == 1)
        {
            final ConfigurationInfoType config = configList.get(0);
            props.put(ConfigurationEventConstants.EVENT_PROP_PID, config.getPid());
            if (config.hasFactoryPid())
            {
                props.put(ConfigurationEventConstants.EVENT_PROP_FACTORY_PID, config.getFactoryPid());
            }
        }
        final Event configModelUpdated = new Event(TOPIC_CONFIG_MODEL_UPDATED, props);
        //Post the configuration model updated event.
        m_EventAdmin.postEvent(configModelUpdated);
    }
    
    /**
     * Attempts to add a configuration model to the map of controller configurations if it does not exist. If it does it
     * exist then it updates the location of the model.
     * 
     * @param controllerId
     *      ID of the controller where the configuration is located.
     * @param config
     *      The protocol buffer equivalent of the configuration to be created or updated.
     * @return
     *      The created or updated {@link ConfigAdminModel}.
     */
    private ConfigAdminModel tryAddConfigModel(final int controllerId, final ConfigurationInfoType config)
    {
        final ConfigAdminModel model;
        if (m_ControllerConfigList.get(controllerId).containsKey(config.getPid()))
        {
            model = m_ControllerConfigList.get(controllerId).get(config.getPid());
            model.setBundleLocation(config.getBundleLocation());
        }
        else
        {
            model = new ConfigAdminModel();
            model.setBundleLocation(config.getBundleLocation());
            model.setPid(config.getPid());
            m_ControllerConfigList.get(controllerId).put(config.getPid(), model);
        }
        
        //Set configuration properties
        for (SimpleTypesMapEntry prop: config.getPropertiesList())
        {
            tryAddConfigProp(model, prop);
        }
        
        return model;
    }
    
    /**
     * Attempts to add a factory configuration model to the map of controller configurations if it does not exist. Will
     * also create a model that represents the factory if it does not exists. If the factory configuration does exist
     * then it updates the location of the model.
     * 
     * @param controllerId
     *      ID of the controller where the configuration is located.
     * @param config
     *      The protocol buffer equivalent of the configuration to be created or updated.
     * @return
     *      The created or updated {@link ConfigAdminModel}.
     */
    private ConfigAdminModel tryAddFactoryConfigModel(final int controllerId, final ConfigurationInfoType config)
    {
        final ConfigAdminModel model;
        
        if (m_ControllerConfigList.get(controllerId).containsKey(config.getFactoryPid()))
        {
            final ConfigAdminModel factory = m_ControllerConfigList.get(controllerId).get(config.getFactoryPid());
            factory.setBundleLocation(config.getBundleLocation());
 
            if (factory.getFactoryConfigurations().containsKey(config.getPid()))
            {
                model = factory.getFactoryConfigurations().get(config.getPid());
                model.setBundleLocation(config.getBundleLocation());
            }
            else
            {
                model = new ConfigAdminModel();
                model.setBundleLocation(config.getBundleLocation());
                model.setPid(config.getPid());
                model.setFactoryPid(config.getFactoryPid());
                factory.getFactoryConfigurations().put(config.getPid(), model);
            }
        }
        else
        {
            model = new ConfigAdminModel();
            model.setBundleLocation(config.getBundleLocation());
            model.setPid(config.getPid());
            model.setFactoryPid(config.getFactoryPid());
            final ConfigAdminModel factory = new ConfigAdminModel();
            factory.setBundleLocation(config.getBundleLocation());
            factory.setPid(config.getFactoryPid());
            factory.setIsFactory(true);
            factory.getFactoryConfigurations().put(config.getPid(), model);
            m_ControllerConfigList.get(controllerId).put(config.getFactoryPid(), factory);
        }
        
        //Set configuration properties
        for (SimpleTypesMapEntry prop: config.getPropertiesList())
        {
            tryAddConfigProp(model, prop);
        }
        
        return model;
    }
    
    /**
     * Attempts to add the property to the specified configuration. If the property already exists then it updates the 
     * value and type of the property.
     * 
     * @param config
     *      The {@link ConfigAdminModel} that the property is to be added to or that contains the property to be 
     *      updated.
     * @param remoteProp
     *      The protocol equivalent of the property to be added or updated.
     */
    private void tryAddConfigProp(final ConfigAdminModel config, final SimpleTypesMapEntry remoteProp)
    {
        //Check to see if the configuration already contains the property.
        ConfigAdminPropertyModel localProp = null;
        for (ConfigAdminPropertyModel prop: config.getProperties())
        {
            if (prop.getKey().equals(remoteProp.getKey()))
            {
                localProp = prop;
                break;
            }
        }
        
        final Object value = SharedMessageUtils.convertMultitypeToObject(remoteProp.getValue());
        final Class<?> type = value.getClass();
        //If the configuration does not contain the property add it. Otherwise update the properties values.
        if (localProp == null)
        {
            final ConfigAdminPropertyModel newProp = new ConfigAdminPropertyModel();
            newProp.setKey(remoteProp.getKey());
            newProp.setType(type);
            newProp.setValue(value);
            config.getProperties().add(newProp);
        }
        else
        {
            localProp.setType(type);
            localProp.setValue(value);
        }
    }
    
    /**
     * Method that handles a configuration updated event.
     * 
     * @param controllerId
     *          ID of the controller the event pertains to.
     * @param pid
     *          PID of the configuration the update event pertains to.
     */
    private void handleUpdateConfigurationEvent(final int controllerId, final String pid)
    {   
        final GetConfigurationInfoRequestData.Builder request = 
                GetConfigurationInfoRequestData.newBuilder().setIncludeProperties(true);
        if (m_ControllerConfigList.containsKey(controllerId))
        {
            // already have info for all PIDs, just get the update for the one that was updated
            request.setFilter(String.format(m_ServiceFilter, pid));
        }
        else
        {
            // no info at all, don't filter
            m_ControllerConfigList.put(controllerId, new HashMap<String, ConfigAdminModel>());
            registerRemoteEvents(controllerId);
        }
        
        m_MessageFactory.createConfigAdminMessage(ConfigAdminMessageType.GetConfigurationInfoRequest, request.build()).
            queue(controllerId, m_ConfigResponseHandler);
    }
    
    /**
     * Method that handles a remove configuration event.
     * 
     * @param controllerId
     *          ID of the controller the event pertains to.
     * @param pid
     *          PID of the configuration the event pertains to.
     * @param factoryPid
     *          Factory PID of the configurations the event pertains to if it has a factory PID.
     */
    private void handleRemoveConfigurationEvent(final int controllerId, final String pid, 
            final String factoryPid)
    {   
        //Factory PID will be null when the configuration is not a factory configuration instance.
        if (factoryPid == null)
        {        
            m_ControllerConfigList.get(controllerId).remove(pid);
        }
        else
        {            
            if (m_ControllerConfigList.get(controllerId).containsKey(factoryPid)) 
            {
                m_ControllerConfigList.get(controllerId).get(factoryPid).getFactoryConfigurations().remove(pid);
            }
        }
        
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, controllerId);
        props.put(ConfigurationEventConstants.EVENT_PROP_PID, pid);
        props.put(ConfigurationEventConstants.EVENT_PROP_FACTORY_PID, factoryPid);
        final Event configModelUpdated = new Event(TOPIC_CONFIG_MODEL_UPDATED, props);
        //Post the configuration model updated event.
        m_EventAdmin.postEvent(configModelUpdated);
    }
    
    /**
     * Method that handles when a create factory configuration response has been received. This method
     * will send a message to retrieve further information the factory configuration that has been created.
     * 
     * @param controllerId
     *          ID of the controller where the factory configuration was created.
     * @param response
     *          Response message that contains information on the factory configuration that was created.
     */
    private void handleCreateFactoryConfigResponse(final int controllerId, 
            final CreateFactoryConfigurationResponseData response)
    {  
        final GetConfigurationInfoRequestData request = 
                GetConfigurationInfoRequestData.newBuilder().setFilter(String.format(m_ServiceFilter, 
                        response.getPid())).setIncludeProperties(true).build();
        
        m_MessageFactory.createConfigAdminMessage(ConfigAdminMessageType.GetConfigurationInfoRequest, request).
            queue(controllerId, m_ConfigResponseHandler);
    }
    
    /**
     * Response handler that handles all configuration response messages. These messages are used to retrieve and
     * store information about configurations on the specified controller.
     */
    class ConfigurationResponseHandler implements ResponseHandler
    {

        /* (non-Javadoc)
         * @see mil.dod.th.core.remote.ResponseHandler#handleResponse(mil.dod.th.core.remote.proto.RemoteBase.
         * TerraHarvestMessage, com.google.protobuf.Message, com.google.protobuf.Message)
         */
        @Override
        public void handleResponse(final TerraHarvestMessage message, final TerraHarvestPayload payload,
                final Message namespaceMessage, 
                final Message dataMessage)
        {
            final int controllerId = message.getSourceId();
            
            final ConfigAdminMessageType messageType = ((ConfigAdminNamespace)namespaceMessage).getType();

            if (messageType.equals(ConfigAdminMessageType.GetConfigurationInfoResponse))
            {
                handleConfigInfoResponse(controllerId, (GetConfigurationInfoResponseData)dataMessage);
            }
            else if (messageType.equals(ConfigAdminMessageType.SetPropertyResponse))
            {
                m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Properties Accepted:", 
                        String.format("Controller 0x%08x has accepted the list of updated properties.", 
                                message.getSourceId()));
            }
        }
    }
    
    /**
     * This response handler handles event registration response messages. This handler stores the event registration
     * ID and the controller the ID is associated with in a map. Later this is used to unregister events.
     */
    class RegisterEventsResponseHandler implements ResponseHandler
    {

        /* (non-Javadoc)
         * @see mil.dod.th.core.remote.ResponseHandler#handleResponse(mil.dod.th.core.remote.proto.RemoteBase.
         * TerraHarvestMessage, com.google.protobuf.Message, com.google.protobuf.Message)
         */
        @Override
        public void handleResponse(final TerraHarvestMessage message, final TerraHarvestPayload payload, 
            final Message namespaceMessage, 
                final Message dataMessage)
        {
            final EventRegistrationResponseData registrationResponse = 
                    (EventRegistrationResponseData)dataMessage;
            
            m_EventRegistraionIds.put(message.getSourceId(), registrationResponse.getId());
        }
    }
    
    /**
     * Event listener that listens for configurations events. Specifically this listener will only handle remote
     * configuration events. This handle will either add, update, or remove a configuration depending on the type of 
     * event received. A configuration updated event could mean that a configuration has just been created as well
     * as meaning a configuration was updated.
     */
    class ConfigurationEventHandler implements EventHandler
    {
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is 
         * destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;

        /**
         * Method that registers configuration events to listen for.
         */
        public void registerConfigurationEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            // register to listen for remote configuration events.
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            final List<String> topics = new ArrayList<String>();
            topics.add(TOPIC_CONFIGURATION_UPDATED_REMOTE);
            topics.add(TOPIC_CONFIGURATION_LOCATION_REMOTE);
            topics.add(TOPIC_CONFIGURATION_DELETED_REMOTE);
            props.put(EventConstants.EVENT_TOPIC, topics);       
            
            //register the event handler that listens for configuration events.
            m_Registration = context.registerService(EventHandler.class, this, props);
        }
        
        /* (non-Javadoc)
         * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
         */
        @Override
        public void handleEvent(final Event event)
        {
            final String topic = event.getTopic();
            final String pid = (String)event.getProperty(ConfigurationEventConstants.EVENT_PROP_PID);
            //Factory PID will be null if the configuration is not a factory configuration instance.
            final String factoryPid = (String)event.getProperty(ConfigurationEventConstants.EVENT_PROP_FACTORY_PID);
            final int controllerId = (Integer)event.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID);
            
            if (topic.equals(TOPIC_CONFIGURATION_UPDATED_REMOTE))
            {
                handleUpdateConfigurationEvent(controllerId, pid);
            }
            else if (topic.equals(TOPIC_CONFIGURATION_LOCATION_REMOTE))
            {
                handleUpdateConfigurationEvent(controllerId, pid);
            }
            else if (topic.equals(TOPIC_CONFIGURATION_DELETED_REMOTE))
            {
                handleRemoveConfigurationEvent(controllerId, pid, factoryPid);
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
     * Event listener that handles controller removed events. When a controller is removed then all configuration
     * information stored for that controller is removed from the map that contains configuration information for
     * known controllers.
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
         * Method that registers all controller events to listen for.
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

        /* (non-Javadoc)
         * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
         */
        @Override
        public void handleEvent(final Event event)
        {
            final int controllerId = (Integer)event.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID);
            m_ControllerConfigList.remove(controllerId);
            m_EventRegistraionIds.remove(controllerId);
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
     * Event handler that handles configuration admin events. This handler handles when a factory configuration is 
     * created and when a configuration is removed from a remote system.
     */
    class ConfigurationAdminEventHandler implements EventHandler
    {
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is 
         * destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;
        
        /**
         * Method that registers all configuration admin events to listen for.
         */
        public void registerControllerEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            // register to listen for controller removed events.
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, RemoteConstants.TOPIC_MESSAGE_RECEIVED);
            final String filterString = String.format("(&(%s=%s)(%s=%s))", 
                RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.ConfigAdmin.toString(),
                RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
                ConfigAdminMessageType.CreateFactoryConfigurationResponse.toString());
            props.put(EventConstants.EVENT_FILTER, filterString);
            
            //register the event handler that listens for controllers being removed.
            m_Registration = context.registerService(EventHandler.class, this, props);
        }
        
        /* (non-Javadoc)
         * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
         */
        @Override
        public void handleEvent(final Event event)
        {
            final TerraHarvestMessage thMessage = 
                    (TerraHarvestMessage)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE);
            final int controllerId = thMessage.getSourceId();
            final Message dataMessage = (Message)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE);
            
            handleCreateFactoryConfigResponse(controllerId, (CreateFactoryConfigurationResponseData)dataMessage);
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
