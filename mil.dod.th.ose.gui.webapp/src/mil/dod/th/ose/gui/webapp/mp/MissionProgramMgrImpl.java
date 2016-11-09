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
package mil.dod.th.ose.gui.webapp.mp;

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
import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.inject.Inject;

import com.google.protobuf.Message;

import mil.dod.th.core.mp.MissionProgramManager;
import mil.dod.th.core.mp.Program;
import mil.dod.th.core.mp.TemplateProgramManager;
import mil.dod.th.core.mp.model.MissionProgramTemplate;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetTemplatesResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace.MissionProgrammingMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.webapp.controller.ControllerMgr;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.remote.lexicon.mp.model.MissionProgramTemplateGen;

import org.glassfish.osgicdi.OSGiService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;


/**
 * Implementation of the {@link MissionProgramMgr}.
 * @author callen
 *
 */
@ManagedBean(name = "missionProgMgr") 
@ApplicationScoped
public class MissionProgramMgrImpl implements MissionProgramMgr 
{
    /**
     * Map of mission program templates for all controllers known to this system.
     */
    private final Map<Integer, Set<MissionProgramTemplate>> m_RemoteTemplates;

    /**
     * Event handler helper class. Listens for the controller has been removed events.
     */
    private EventHelperControllerEvent m_ControllerEventListener;
    
    /**
     * Event handler helper class. Listens for MissionProgramming namespace messages.
     */
    private EventHelperMissionNamespace m_EventHelperMission;
    
    /**
     * Service that retrieves the bundle context of this bundle.
     */
    @Inject
    private BundleContextUtil m_BundleUtil;

    /**
     * Growl message utility for creating growl messages.
     */
    @Inject
    private GrowlMessageUtil m_GrowlMessageUtil;

    /**
     * Reference to the injected {@link TemplateProgramManager} service.
     */
    @Inject @OSGiService
    private MissionProgramManager m_MissionProgramManager;
    
    /**
     * Inject the MessageFactory service for creating remote messages.
     */
    @Inject @OSGiService
    private MessageFactory m_MessageFactory;

    /**
     * The {@link JaxbProtoObjectConverter} responsible for converting between JAXB and protocol buffer objects. 
     */
    @Inject @OSGiService
    private JaxbProtoObjectConverter m_Converter;

    /**
     * Reference to the injected {@link TemplateProgramManager} service.
     */
    @Inject @OSGiService
    private TemplateProgramManager m_TemplateProgramManager;

    /**
     * Initialize the map for mission program template collections.
     */
    public MissionProgramMgrImpl()
    {
        m_RemoteTemplates = Collections.synchronizedMap(new HashMap<Integer, Set<MissionProgramTemplate>>());
    }
    /**
     * Get the active controller.
     */
    @PostConstruct
    public void setupDependency()
    {
        //instantiate handlers
        m_ControllerEventListener = new EventHelperControllerEvent();
        m_EventHelperMission = new EventHelperMissionNamespace();

        //register to listen to delegated events
        m_ControllerEventListener.registerControllerEvents();
        m_EventHelperMission.registerForEvents();
    }
    
    /**
     * Unregister handlers before destruction of the bean.
     */
    @PreDestroy
    public void unregisterHelpers()
    {
        //Unregister for events
        m_ControllerEventListener.unregisterListener();
        m_EventHelperMission.unregisterListener();
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
     * Set the {@link JaxbProtoObjectConverter}.
     * 
     * @param converter
     *     the service responsible for converting between JAXB and protocol buffer objects.
     */
    public void setConverter(final JaxbProtoObjectConverter converter)
    {
        m_Converter = converter;
    }
 
    /**
     * Method for setting the {@link MissionProgramManager} service. This setter method is used only for testing.
     * 
     * @param mProgramManager
     *          The {@link MissionProgramManager} service to be set.
     */
    public void setProgramManager(final MissionProgramManager mProgramManager)
    {
        m_MissionProgramManager = mProgramManager;
    }

    /**
     * Set the Growl message utility service.
     * @param growlUtil
     *     the growl message utility service to use
     */
    public void setGrowlMessageUtility(final GrowlMessageUtil growlUtil)
    {
        m_GrowlMessageUtil = growlUtil;
    }

    /**
     * Method for setting the {@link MissionProgramTemplate} service.
     * 
     * @param templateManager
     *          The {@link MissionProgramTemplate} service to be set.
     */
    public void setTemplateProgramManager(final TemplateProgramManager templateManager)
    {
        m_TemplateProgramManager = templateManager;
    }

    @Override
    public synchronized List<String> getLocalMissionNames() 
    {
        final List<String> names = new ArrayList<String>();
        for (Program program : m_MissionProgramManager.getPrograms())
        {
            names.add(program.getProgramName());
        }
        
        //return list of names
        return names;
    }

    @Override
    public synchronized List<String> getRemoteTemplateNames(final int systemId) 
    {
        final List<String> names = new ArrayList<String>();
        
        //check that there is a mapping of templates for the controller id
        if (!m_RemoteTemplates.containsKey(systemId))
        {
            return names;
        }
        for (MissionProgramTemplate template : m_RemoteTemplates.get(systemId))
        {
            names.add(template.getName());
        }

        //return list of names
        return names;
    }

    @Override
    public synchronized void requestSyncingOfTemplates(final int systemId)
    {
        //send requests to get template information events
        requestToGetTemplates(systemId);
    }
    
    @Override
    public void queueMessage(final int controllerId, final Message message, 
            final MissionProgrammingMessageType type)
    {
        final MissionErrorHandler errorHandler = new MissionErrorHandler(controllerId);
        m_MessageFactory.createMissionProgrammingMessage(type, message).
            queue(controllerId, errorHandler);
    }

    /**
     * Request to get templates from the specified system.
     * @param systemId
     *     the system to send the request to 
     */
    private synchronized void requestToGetTemplates(final int systemId)
    {
        //request templates
        m_MessageFactory.createMissionProgrammingMessage(MissionProgrammingMessageType.GetTemplatesRequest, null).
            queue(systemId, null);
    }

    /**
     * Add a template to the lookup.
     * @param message
     *     the message that contains the mission program templates to add
     * @param systemId
     *     the ID of the system from which the template originated from
     */
    private void addTemplates(final GetTemplatesResponseData message, final int systemId)
    {
        //replace or add template collection for the given system id
        final Set<MissionProgramTemplate> templates = new HashSet<MissionProgramTemplate>();
        for (MissionProgramTemplateGen.MissionProgramTemplate protoTemplate : message.getTemplateList())
        {
            final MissionProgramTemplate template;
            try 
            {
                template = (MissionProgramTemplate) m_Converter.convertToJaxb(protoTemplate);
            } 
            catch (final ObjectConverterException exception) 
            {
                m_GrowlMessageUtil.createLocalFacesMessage(FacesMessage.SEVERITY_WARN, "Syncing Templates Warning", 
                    String.format("A template from controller 0x%08x wasn't able to be processed.", systemId), 
                        exception);
                continue;
            }
            
            templates.add(template);
            
            //load the template to the local store
            try
            {
                m_TemplateProgramManager.loadMissionTemplate(template);
            }
            catch (final PersistenceFailedException exception)
            {
                m_GrowlMessageUtil.createLocalFacesMessage(FacesMessage.SEVERITY_WARN, "Syncing Templates Info", 
                    String.format("A template from controller 0x%08x wasn't able to be stored locally.", systemId), 
                        exception);
            }
        }
        //add to the list of templates for the specified system
        m_RemoteTemplates.put(systemId, templates);
    }
    
    /**
     * Event handler for the Mission Programming namespace. Will load received templates into the local template
     * manager.
     * @author callen
     */
    public class EventHelperMissionNamespace implements EventHandler
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

            // register to listen to the mission programming get templates response
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, RemoteConstants.TOPIC_MESSAGE_RECEIVED);
            final String filterString = String.format("(&(%s=%s)(%s=%s))",
                RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.MissionProgramming.toString(),
                RemoteConstants.EVENT_PROP_MESSAGE_TYPE, MissionProgrammingMessageType.GetTemplatesResponse.toString());
            props.put(EventConstants.EVENT_FILTER, filterString);
            
            //register the event handler that listens for get templates responses
            m_Registration = context.registerService(EventHandler.class, this, props);
        } 
        
        @Override
        public void handleEvent(final Event event)
        {
            //pull out system ID
            final int systemId = (Integer)event.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID);
            final GetTemplatesResponseData data = (GetTemplatesResponseData)event.
                getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE);
            
            addTemplates(data, systemId);
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
    class EventHelperControllerEvent implements EventHandler
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

            m_RemoteTemplates.remove(controllerId);
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
     * Class to handle error messages for loading mission templates and parameters to 
     * a controller.
     * @author nickmarcucci
     *
     */
    public class MissionErrorHandler implements ResponseHandler
    {
        /**
         * The id of the controller that the error has occurred on.
         */
        private final int m_ControllerId;
        
        /**
         * Constructor.
         * @param controller
         *  the controller that this error has occurred on.
         */
        public MissionErrorHandler(final int controller)
        {
            m_ControllerId = controller;
        }
        
        @Override
        public void handleResponse(final TerraHarvestMessage message, final TerraHarvestPayload payload,
                final Message namespaceMessage, final Message dataMessage)
        {
            if (payload.getNamespace() == Namespace.Base)
            {
                final String summary = "Mission Loading Error Occurred";
                
                //parse the namespace message
                final BaseNamespace baseMessage = (BaseNamespace)namespaceMessage;
                 
                //check the base namespace message type
                if (baseMessage.getType() == BaseMessageType.GenericErrorResponse)
                {
                  //parse generic error message
                    final GenericErrorResponseData data = (GenericErrorResponseData)dataMessage;
                    
                    //post notice
                    m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, summary, String.format(
                            "An error has occurred trying to load a template to controller 0x%08x. " 
                            + " The following is the error: %n %s - %s", 
                             m_ControllerId, data.getError(), 
                                    data.getErrorDescription()));
                }
            }
        }
    }
}
