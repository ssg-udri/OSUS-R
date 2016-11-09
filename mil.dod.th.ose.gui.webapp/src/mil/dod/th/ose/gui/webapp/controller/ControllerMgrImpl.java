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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.inject.Inject;

import mil.dod.th.core.controller.TerraHarvestController;
import mil.dod.th.core.controller.TerraHarvestController.OperationMode;
import mil.dod.th.core.controller.capability.ControllerCapabilities;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.BaseMessages;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.ControllerInfoData;
import mil.dod.th.core.remote.proto.BaseMessages.GetControllerCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.GetControllerCapabilitiesResponseData.ControllerCapabilitiesCase;
import mil.dod.th.core.remote.proto.BaseMessages.GetOperationModeReponseData;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.ose.gui.api.ControllerEncryptionConstants;
import mil.dod.th.ose.gui.api.EncryptionTypeManager;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.webapp.remote.RemoteEvents;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.ose.remote.api.EnumConverter;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.lexicon.controller.capability.ControllerCapabilitiesGen;

import org.glassfish.osgicdi.OSGiService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;

/**
 * Implementation of the {@link ControllerMgr} interface.
 * @author callen
 *
 */
@ManagedBean(name = "controllerManager")
@ApplicationScoped
public class ControllerMgrImpl implements ControllerMgr
{
    /**
     * String to use as description for messages and logging.
     */
    private static final String LOG_MESSAGE_DESCRIPTOR_INFO = "Controller Info:";
    
    /**
     * An integer representing the maximum system ID.
     */
    private static final int MAX_SYSTEM_ID = java.lang.Integer.MAX_VALUE;
    
    /**
     * Set of all known controllers. 
     */
    private final Set<ControllerModel> m_Controllers;

    /**
     * Event handler helper class. Listens for controller info messages and updates the controller models.
     */
    private EventHelper m_EventHelper;
    
    /**
     * Event handler to listen for channel added and removed events.
     */
    private ChannelEventHelper m_ChannelHelper;
    
    /**
     * Event handler that listens for remote controller mode change events.
     */
    private ControllerModeEventHandler m_ModeEventHandler;
    
    /**
     * Event handler that listens for controller cleanup responses so that controller info requests can commence.
     */
    private CleanupEventHelper m_CleanupHelper;
    
    /**
     * Event handler that listens for encryption type updates for controller models.
     */
    private EncryptionEventHelper m_EncryptionEventHelper;
    
    /**
     * Inject the RemoteChannelLookup service for registering new remote channels.
     */
    @Inject @OSGiService
    private RemoteChannelLookup m_RemoteChannelLookup;
    
    /**
     * Inject the EventAdmin service for posting events.
     */
    @Inject @OSGiService
    private EventAdmin m_EventAdmin;
    
    /**
     * Inject the {@link MessageFactory} service for creating remote messages.
     */
    @Inject @OSGiService
    private MessageFactory m_MessageFactory;
    
    /**
     * The {@link JaxbProtoObjectConverter} responsible for converting between JAXB and protocol buffer objects. 
     */
    @Inject @OSGiService
    private JaxbProtoObjectConverter m_Converter;
    
    /**
     * Growl message utility for creating growl messages.
     */
    @Inject
    private GrowlMessageUtil m_GrowlUtil;
    
    /**
     * Service that retrieves the bundle context of this bundle.
     */
    @Inject
    private BundleContextUtil m_BundleUtil;
    
    /**
     * The {@link ControllerImage} image display interface.
     */
    @Inject
    private ControllerImage m_ControllerImageInterface;
    
    /**
     * The service responsible for handling controller encryption type information.
     */
    @Inject @OSGiService
    private EncryptionTypeManager m_EncryptionTypeManager;
        
    /**
     * Controller manager's public constructor.
     */
    public ControllerMgrImpl()
    {
        m_Controllers = Collections.synchronizedSet(new HashSet<ControllerModel>());
    }
    
    /**
     * Provides the maximum system ID.
     * @return
     *      the maximum system ID
     */
    public int getMaxId()
    {
        return MAX_SYSTEM_ID;
    }
    
    /**
     * Register event listener and add initial controllers.
     */
    @PostConstruct
    public void setup()
    {
        m_EventHelper = new EventHelper();
        m_ChannelHelper = new ChannelEventHelper();
        m_ModeEventHandler = new ControllerModeEventHandler();
        m_EncryptionEventHelper = new EncryptionEventHelper();
        m_CleanupHelper = new CleanupEventHelper();
        
        m_EventHelper.registerControllerInfoEvents();
        m_ChannelHelper.registerChannelAddedRemovedEvents();
        m_ModeEventHandler.registerControllerModeEvents();
        m_EncryptionEventHelper.registerEncryptionUpdateEvents();
        m_CleanupHelper.registerCleanupEvents();
        
        //Add all controllers initially known by the system. These controllers may be known through remote channels that
        //have been persisted or that were created at startup through the configs.xml.
        for (Entry<Integer, Set<RemoteChannel>> controllerEntry : m_RemoteChannelLookup.getAllChannels().entrySet())
        {
            //Verify that a channel exists for the controller before trying to add it.
            if (!controllerEntry.getValue().isEmpty())
            {
                addControllerIfNew(controllerEntry.getKey());
            }
        }
    }
    
    /**
     * Unregister handler before destruction of the bean.
     */
    @PreDestroy
    public void unregisterEventHelper()
    {
        m_EventHelper.unregisterListener();
        m_ChannelHelper.unregisterListener();
        m_ModeEventHandler.unregisterListener();
        m_EncryptionEventHelper.unregisterListener();
        m_CleanupHelper.unregisterListener();
    }
    
    /**
     * Set the {@link RemoteChannelLookup} service.
     * @param lookup
     *     the remote channel lookup service to use
     */
    public void setRemoteChannelLookup(final RemoteChannelLookup lookup)
    {
        m_RemoteChannelLookup = lookup;
    }
    
    /**
     * Set the {@link EventAdmin} service.
     * @param eventAdmin
     *      the event admin service to use.
     */
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Set the growl message utility service.
     * @param growlUtil
     *     the growl message utility service to use
     */
    public void setGrowlMessageUtility(final GrowlMessageUtil growlUtil)
    {
        m_GrowlUtil = growlUtil;
    }
     
    /**
     * Set the Bundle Context utility service.
     * @param bundleUtil
     *     the bundle context utility service to use
     */
    public void setBundleContextUtility(final BundleContextUtil bundleUtil)
    {
        m_BundleUtil = bundleUtil;
    }
    
    /**
     * Sets the {@link ControllerImage} image display interface to use.
     * @param imgInterface
     *  the image interface to use.
     */
    public void setControllerImage(final ControllerImage imgInterface)
    {
        m_ControllerImageInterface = imgInterface;
    }
    
    /**
     * Bind the service.
     * 
     * @param converter
     *      service to used to converter between JAXB and protocol buffer objects.
     */
    public void setConverter(final JaxbProtoObjectConverter converter)
    {
        m_Converter = converter;
    }
    
    /**
     * Method that sets the MessageFactory service to use.
     * 
     * @param messageFactory
     *      MessageFactory service to set.
     */
    public void setMessageFactory(final MessageFactory messageFactory)
    {
        m_MessageFactory = messageFactory;
    }
    
    /**
     * Method that sets the EncryptioinTypeManager to use.
     * 
     * @param encryptionTypeManager
     *      EncryptionTypeManager to be set.
     */
    public void setEncryptionTypeManager(final EncryptionTypeManager encryptionTypeManager)
    {
        m_EncryptionTypeManager = encryptionTypeManager;
    }
    
    /**
     * Get a controller base object represented by the controller's ID.
     * @param idOfController
     *     the id of the controller to fetch
     * @return
     *     the controller base object representing the requested controller or null if not found
     */
    @Override
    public ControllerModel getController(final int idOfController)
    {
        flushControllerList();
        
        //controllers are not required to have unique names, look for the id
        synchronized (m_Controllers)
        {
            for (ControllerModel controller : m_Controllers)
            {
                if (controller.getId() == idOfController)
                {
                    return controller;
                }
            }
        }
        return null;
    }
    
    /**
     * Get a list of all controller base objects representing different controllers known to the application.
     * @return
     *    list of controller base objects
     */
    @Override
    public List<ControllerModel> getAllControllers()
    {
        flushControllerList();
        
        return new ArrayList<ControllerModel>(m_Controllers);
    }

    /**
     * Remove any controller models from the list that do not exist in the channel lookup anymore.
     */
    private void flushControllerList()
    {
        final Map<Integer, Set<RemoteChannel>> channels = m_RemoteChannelLookup.getAllChannels();
                
        for (ControllerModel model : new ArrayList<ControllerModel>(m_Controllers))
        {
            if (!channels.containsKey(model.getId()) || channels.get(model.getId()).isEmpty())
            {
                // controller has no channels, so remove controller from list
                removeControllerAndPostEvent(model);
            }
        }
    }
    
    /**
     * Remove a controller from the controller manager. 
     * @param controllerId
     *     the id of the controller to remove
     */
    @Override
    public synchronized void removeController(final int controllerId)
    {
        //try to remove the controller from the list of known controllers
        final ControllerModel model = getController(controllerId);
        if (model == null)
        {
            m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_WARN, LOG_MESSAGE_DESCRIPTOR_INFO, 
                String.format("Controller 0x%08x is not known to the system!", controllerId));
            throw new IllegalArgumentException(String.format("Controller 0x%08x is already removed", controllerId));  
        }
        //remove model
        m_Controllers.remove(model);
        //remove channels from channel lookup
        final List<RemoteChannel> rChannels = m_RemoteChannelLookup.getChannels(controllerId);
        for (RemoteChannel channel : rChannels)
        {
            m_RemoteChannelLookup.removeChannel(channel);
        }
        //notify
        m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_INFO, LOG_MESSAGE_DESCRIPTOR_INFO, 
            String.format("Controller 0x%08x was removed from lookup!", controllerId));
    }
    
    /**
     * Add a controller to the controller manager if not already added.
     * 
     * @param controllerId
     *     the id of the controller to add
     * @return 
     *     returns true if the controller was new and therefore was added to the list of 
     *     known controllers; false otherwise
     */
    private synchronized boolean addControllerIfNew(final int controllerId)
    {
        ControllerModel model = getController(controllerId);
        if (model == null)
        {
            model = new ControllerModel(controllerId, m_ControllerImageInterface);
            m_Controllers.add(model);
            
            EncryptType encryptType = null;
            try
            {
                encryptType = m_EncryptionTypeManager.getEncryptTypeAsnyc(controllerId);
            }
            catch (final IllegalArgumentException ex)
            {
                m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "Request Error:", 
                        String.format("Unable to send encryption type request for controller ID: %s. See server log "
                                + "for more details.", controllerId), ex);
            }
            
            if (encryptType != null)
            {                
                //send cleanup request if controller is completely new
                sendCleanupRequestIfNeeded(model);
            }

            //Create a controller added event and post it to the event admin service.
            final Map<String, Object> props = new HashMap<String, Object>();
            props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, model.getId());
            m_EventAdmin.postEvent(new Event(ControllerMgr.TOPIC_CONTROLLER_ADDED, props));
            
            if (controllerId != MAX_SYSTEM_ID)
            {
                m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, LOG_MESSAGE_DESCRIPTOR_INFO, 
                        String.format("Added controller 0x%08x", controllerId));
            }
            
            return true;
        } 
        
        return false;
    }
    
    /**
     * Check that all controllers in the controller list have channels. If a controller does not have any channels the 
     * controller will be removed.
     */
    private synchronized void checkControllerChannels()
    {
        //check that the all ids in the controller list are in the remote channel lookup
        final Set<ControllerModel> models = new HashSet<ControllerModel>(m_Controllers);
        for (ControllerModel model : models)
        {
            if (m_RemoteChannelLookup.getChannels(model.getId()).isEmpty())
            {
                //Remove the controller and post an event to the event admin service.
                removeControllerAndPostEvent(model);
            }
        }
    }
    
    /**
     * Responsible for sending a cleanup request for a specific controller if the controller
     * still requires a cleanup request and is not in a ready state.  At this point, the encryption type of the 
     * controller should be known so that the message can be sent successfully.
     *      
     * @param model
     *      the controller model 
     */
    private void sendCleanupRequestIfNeeded(final ControllerModel model)
    {
        if (model.needsCleanupRequest() && !model.isReady())
        {
            //model no longer needs cleanup request
            model.setNeedsCleanupRequest(false);

            // clean up previous registrations
            m_MessageFactory.createEventAdminMessage(EventAdminMessageType.CleanupRequest, null).
                queue(model.getId(), null);
        } 
    }
    
    /**
     * Method that removes the specified controller from the local map of known controllers and posts a 
     * {@link ControllerMgr#TOPIC_CONTROLLER_REMOVED} event.
     * 
     * @param model
     *      model that represents the controller that to be removed
     */
    private void removeControllerAndPostEvent(final ControllerModel model)
    {
        //Remove the controller from the local map of known controllers.
        m_Controllers.remove(model);
        
        //Create a controller removed event and post it to the event admin service.
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, model.getId());
        props.put(ControllerMgr.EVENT_PROP_CONTROLLER_NAME, model.getName());
        props.put(ControllerMgr.EVENT_PROP_CONTROLLER_VERSION, model.getVersion());
        
        //post the controller removed event
        final Event controllerRemoved = new Event(ControllerMgr.TOPIC_CONTROLLER_REMOVED, props);
        m_EventAdmin.postEvent(controllerRemoved);
        
        Logging.log(LogService.LOG_DEBUG, "Removed controller model with ID: 0x%08x", model.getId());
    }
    
    /**
     * Method that sets the specified controller model to the ready state and sends a request for the controllers 
     * current mode.
     * 
     * @param model
     *      Controller model to be set to ready.
     */
    private void setModelReadyAndRequestInfo(final ControllerModel model)
    {
        //model is now ready for sending and receiving encrypted messages
        model.setIsReady(true);
        
        //send controller info request
        m_MessageFactory.createBaseMessage(BaseMessageType.RequestControllerInfo, null).
            queue(model.getId(), null);
        
        //send capabilities request
        m_MessageFactory.createBaseMessage(BaseMessageType.GetControllerCapabilitiesRequest,
            null).queue(model.getId(), null);
        
        m_MessageFactory.createBaseMessage(BaseMessageType.GetOperationModeRequest, null).
            queue(model.getId(), null);
    
        final List<String> topics = new ArrayList<String>();
        topics.add(TerraHarvestController.TOPIC_CONTROLLER_MODE_CHANGED);
        
        RemoteEvents.sendEventRegistration(m_MessageFactory, topics, null, true, model.getId(), null);
    }
    
    /**
     * Handles local events and performs actions based on events received.
     *
     */
    class EventHelper implements EventHandler
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
        public void registerControllerInfoEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            // register to listen to required remote responses
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, RemoteConstants.TOPIC_MESSAGE_RECEIVED);
            final String filterString = String.format("(&(%s=%s)(|(%s=%s)(%s=%s)(%s=%s)))", 
                RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Base.toString(), 
                RemoteConstants.EVENT_PROP_MESSAGE_TYPE, BaseMessageType.ControllerInfo.toString(),
                RemoteConstants.EVENT_PROP_MESSAGE_TYPE, BaseMessageType.GetOperationModeResponse.toString(),
                RemoteConstants.EVENT_PROP_MESSAGE_TYPE, BaseMessageType.GetControllerCapabilitiesResponse.toString());
            props.put(EventConstants.EVENT_FILTER, filterString);
            
            //register the event handler 
            m_Registration = context.registerService(EventHandler.class, this, props);
        } 
        
        @Override
        public void handleEvent(final Event event)
        {  
            //get the controller id from the message that triggered this event
            final int controllerId = (Integer)event.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID);
            final String messageType = (String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE);
            final ControllerModel model = getController(controllerId);

            if (model == null)
            {
                Logging.log(LogService.LOG_ERROR, "Cannot process response: Controller 0x%08x is not recognized",
                        controllerId);
                return;
            }
            
            if (messageType.equals(BaseMessageType.ControllerInfo.toString()))
            {      
                final ControllerInfoData infoMessage = 
                        (ControllerInfoData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE);

                //update model
                model.setName(infoMessage.getName());
                model.setVersion(infoMessage.getVersion());

                //Create a map of strings for setting build info properties
                final Map<String, String> props = 
                        SharedMessageUtils.convertListSimpleTypesMapEntrytoMap(infoMessage.getBuildInfoList());
                model.setBuildInfo(props);

                Logging.log(LogService.LOG_DEBUG, "Updated information for controller 0x%08x", controllerId);
               
                final Event eventUpdated = new Event(TOPIC_CONTROLLER_UPDATED, new HashMap<String, Object>());
                m_EventAdmin.postEvent(eventUpdated);
            }
            else if (messageType.equals(BaseMessageType.GetOperationModeResponse.toString()))
            {
                final GetOperationModeReponseData modeResponse = 
                        (GetOperationModeReponseData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE);
                final BaseMessages.OperationMode modeProto = modeResponse.getMode();
                final OperationMode mode = EnumConverter.convertProtoOperationModeToJava(modeProto);

                model.setOperatingMode(mode);
                Logging.log(LogService.LOG_DEBUG, "Update status for controller 0x%08x to %s", //NOCHECKSTYLE:
                        controllerId, mode.toString()); //multiple string literals, convey the same information, 
                                                        //but they are in different methods
                final Event controllerStatusEvent = 
                        new Event(ControllerMgr.TOPIC_CONTROLLER_UPDATED, new HashMap<String, Object>());
                m_EventAdmin.postEvent(controllerStatusEvent);

            }
            else if (messageType.equals(BaseMessageType.GetControllerCapabilitiesResponse.toString()))
            {
                final GetControllerCapabilitiesResponseData response = (GetControllerCapabilitiesResponseData)event.
                        getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE);
                
                if (response.getControllerCapabilitiesCase() != ControllerCapabilitiesCase.CONTROLLERCAPABILITIESNATIVE)
                {
                    Logging.log(LogService.LOG_WARNING, "Controller [0x%08x] capabilities in unsupported format [%s]", 
                            controllerId, response.getControllerCapabilitiesCase());
                    return;
                }

                final ControllerCapabilitiesGen.ControllerCapabilities caps = 
                        response.getControllerCapabilitiesNative();

                try
                {
                    final ControllerCapabilities controllerCaps = 
                            (ControllerCapabilities) m_Converter.convertToJaxb(caps);

                    model.setCapabilities(controllerCaps);

                }
                catch (final ObjectConverterException exception)
                {
                    Logging.log(LogService.LOG_ERROR, "Could not convert controller capabilities for system 0x%08x. \n" 
                            + " The following error occurred \n%s", controllerId, exception.getMessage());
                }
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
     * Class to listen for channel added and removed events so that the controller manager
     * can check that it has the controller for which the channel was created in its list.
     * @author nickmarcucci
     *
     */
    class ChannelEventHelper implements EventHandler
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
        public void registerChannelAddedRemovedEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            // register to listen to channel updated or removed events
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            final String[] channelAddRemove = {RemoteChannelLookup.TOPIC_CHANNEL_UPDATED, 
                RemoteChannelLookup.TOPIC_CHANNEL_REMOVED};
            props.put(EventConstants.EVENT_TOPIC, channelAddRemove);
            
            //register the event handler
            m_Registration = context.registerService(EventHandler.class, this, props);
        }

        @Override
        public void handleEvent(final Event event)
        {
            final String topic = event.getTopic();
            
            if (topic.equals(RemoteChannelLookup.TOPIC_CHANNEL_UPDATED))
            {
                final int controllerId = (Integer)event.getProperty(RemoteConstants.EVENT_PROP_SYS_ID);
                
                //check if controller is new and therefore we should send a cleanup request first; 
                addControllerIfNew(controllerId);
            }
            else if (topic.equals(RemoteChannelLookup.TOPIC_CHANNEL_REMOVED))
            {
                //channel has been removed so check to see if the controller should still exists. if it is the 
                //last channel for the controller then the controller should be removed.
                checkControllerChannels();
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
     * Event handler that listens for remote controller status changed events and then handles updating the controller 
     * model to reflect the change.
     */
    class ControllerModeEventHandler implements EventHandler
    {
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is 
         * destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;
        
        /**
         * Method that registers to listen for remote controller mode changed events.
         */
        public void registerControllerModeEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            // register to listen for remote controller mode changed events.
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, 
                    TerraHarvestController.TOPIC_CONTROLLER_MODE_CHANGED + RemoteConstants.REMOTE_TOPIC_SUFFIX);
            
            //register the event handler that listens for remote controller mode events.
            m_Registration = context.registerService(EventHandler.class, this, props);
        }
        
        @Override
        public void handleEvent(final Event event)
        {
            final int controllerId = (Integer)event.getProperty(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID);
            final String modeStr = (String)event.getProperty(TerraHarvestController.EVENT_PROP_SYSTEM_MODE);
            final OperationMode mode;
            try 
            {
                mode = OperationMode.fromValue(modeStr);
            }
            catch (final IllegalArgumentException ex)
            {
                m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "Controller Status Update Failed", 
                        String.format("The status for controller 0x%08x could not be updated to %s.", controllerId, 
                                modeStr), ex);
                return;
            }
            final ControllerModel model = getController(controllerId);
            if (model != null)
            {
                model.setOperatingMode(mode);
                Logging.log(LogService.LOG_DEBUG, "Update status for controller 0x%08x to %s", controllerId, 
                        mode.toString());
                final Event controllerStatusEvent = 
                        new Event(ControllerMgr.TOPIC_CONTROLLER_UPDATED, new HashMap<String, Object>());
                m_EventAdmin.postEvent(controllerStatusEvent);
                
                m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Controller Status Updated", 
                        String.format("The status for controller 0x%08x was updated to %s.", 
                                        controllerId, model.getOperatingModeDisplayText()));
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
     * Class for handling encryption type updated events.
     */
    class CleanupEventHelper implements EventHandler
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
        public void registerCleanupEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            // register to listen to required remote responses
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, RemoteConstants.TOPIC_MESSAGE_RECEIVED);
            final String filterString = String.format("(&(%s=%s)(%s=%s))", 
                RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.EventAdmin.toString(),
                RemoteConstants.EVENT_PROP_MESSAGE_TYPE, EventAdminMessageType.CleanupResponse.toString());
            props.put(EventConstants.EVENT_FILTER, filterString);
            
            //register the event handler 
            m_Registration = context.registerService(EventHandler.class, this, props);
        }

        @Override
        public void handleEvent(final Event event)
        {
            //get the controller id from the message that triggered this event
            final int controllerId = (Integer)event.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID);
            final ControllerModel model = getController(controllerId);

            if (model == null)
            {
                Logging.log(LogService.LOG_ERROR, "Cannot process cleanup: Controller 0x%08x is not recognized",
                        controllerId);
                return;
            }
            
            setModelReadyAndRequestInfo(model);
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
    * Class for handling encryption type updated events.
    */
    class EncryptionEventHelper implements EventHandler
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
        public void registerEncryptionUpdateEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            // register to listen to encryption update events
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, 
                ControllerEncryptionConstants.TOPIC_CONTROLLER_ENCRYPTION_TYPE_UPDATED);
           
            //register the event handler 
            m_Registration = context.registerService(EventHandler.class, this, props);
        }

        @Override
        public void handleEvent(final Event event)
        {
            final Integer controllerId = (Integer)event.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID);
           
            final ControllerModel model = getController(controllerId);
            if (model == null)
            {
                Logging.log(LogService.LOG_ERROR, 
                    "Cannot process encryption type updated event: Controller 0x%08x is not recognized",
                    controllerId);
            }
            else
            {   
                //send cleanup request if controller is completely new
                sendCleanupRequestIfNeeded(model);
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
}
