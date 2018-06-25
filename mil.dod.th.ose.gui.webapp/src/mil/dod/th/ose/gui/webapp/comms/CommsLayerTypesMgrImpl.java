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

import mil.dod.th.core.capability.BaseCapabilities;
import mil.dod.th.core.ccomm.link.capability.LinkLayerCapabilities;
import mil.dod.th.core.ccomm.physical.capability.PhysicalLinkCapabilities;
import mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CommType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace.CustomCommsMessageType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetAvailableCommTypesRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetAvailableCommTypesResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetCapabilitiesRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;

import org.glassfish.osgicdi.OSGiService;

/**
 * Implementation of the {@link CommsLayerTypesMgr}.
 * @author bachmakm
 *
 */
@ManagedBean(name = "commsTypesMgr", eager = true)
@ApplicationScoped
public class CommsLayerTypesMgrImpl implements CommsLayerTypesMgr
{
    /**
     * Map of all available transport layer types corresponding to each registered controller.
     */
    private final Map<Integer, List<String>> m_TransportTypes;
    
    /**
     * Map of all available link layer types corresponding to each registered controller.
     */
    private final Map<Integer, List<String>> m_LinkTypes;
    
    /**
     * Map of capabilities objects. Integer key represents the system ID to
     * which the the capabilities belong.  Inner map contains the actual capabilities
     * object which is keyed by the FQCN of the comms layer type.  
     */
    private final Map<Integer, Map<String, Object>> m_Capabilities;
    
    /**
     * Map of all available physical link types corresponding to each registered controller.
     * Is only used when controller does not have physical links initially created.
     */
    private final Map<Integer, List<String>> m_PhysicalTypes;
    
    /**
     * Event handler helper class.  Listens for CustomComms namespace messages.
     */
    private EventHelperCommsTypes m_EventHelperComms;  
    
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
     * The {@link CommsImage} image display interface.
     */
    @Inject
    private CommsImage m_CommsImageInterface;
    
    /**
     * The {@link JaxbProtoObjectConverter} responsible for converting between JAXB and protocol buffer objects. 
     */
    @Inject @OSGiService
    private JaxbProtoObjectConverter m_Converter;
    
    /**
     * Inject the MessageFactory service for creating remote messages.
     */
    @Inject @OSGiService
    private MessageFactory m_MessageFactory;
    
    /**
     * Inject the EventAdmin service for posting events.
     */
    @Inject @OSGiService
    private EventAdmin m_EventAdmin;

    /**
     * Comms layer types constructor.
     */
    public CommsLayerTypesMgrImpl()
    {
        m_TransportTypes = Collections.synchronizedMap(new HashMap<Integer, List<String>>());
        m_LinkTypes = Collections.synchronizedMap(new HashMap<Integer, List<String>>());
        m_PhysicalTypes = Collections.synchronizedMap(new HashMap<Integer, List<String>>());
        
        m_Capabilities = Collections.synchronizedMap(new HashMap<Integer, Map<String, Object>>());
    }
    
    /**
     * Register event listeners and setup JAXB converters. 
     */
    @PostConstruct
    public void setupDependencies()
    {        
        //instantiate handlers
        m_EventHelperComms = new EventHelperCommsTypes();        
        m_EventHelperComms.registerForEvents();        
    }
    
    /**
     * Unregister handlers before destruction of the bean.
     */
    @PreDestroy
    public void unregisterHelper()
    {
        m_EventHelperComms.unregisterListener();
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
     * Set the Growl message utility service.
     * @param growlUtil
     *     the growl message utility service to use
     */
    public void setGrowlMessageUtility(final GrowlMessageUtil growlUtil)
    {
        m_GrowlMessageUtil = growlUtil;
    }
    
    /**
     * Set the {@link CommsImage} image display interface.
     * @param imgInterface
     *  the image display interface to use.
     */
    public void setCommsImageInterface(final CommsImage imgInterface)
    {
        m_CommsImageInterface = imgInterface;
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
     * Set the {@link JaxbProtoObjectConverter} service.
     * 
     * @param converter
     *      the service used to convert between JAXB and protocol buffer objects.
     */
    public void setConverter(final JaxbProtoObjectConverter converter)
    {
        m_Converter = converter;
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
    
    @Override
    public List<String> getTransportLayerTypes(final int systemId)
    {
        final List<String> types = m_TransportTypes.get(systemId);
        return types == null ? new ArrayList<String>() : types;
    }
    
    @Override
    public List<String> getLinkLayerTypes(final int systemId)
    {
        final List<String> types = m_LinkTypes.get(systemId);
        return types == null ? new ArrayList<String>() : types;
    }

    @Override
    public boolean getLinkLayerRequiresPhysical(final int systemId, final String clazzName)
    {
        for (String name : m_LinkTypes.get(systemId))
        {
            if (name.equals(clazzName))
            {
                final LinkLayerCapabilities linkCaps = (LinkLayerCapabilities)getCapabilities(systemId, name);
                return linkCaps.isPhysicalLinkRequired();
            }
        }

        return true;
    }

    @Override
    public List<String> getPhysicalLinkClasses(final int systemId)
    {
        final List<String> types = m_PhysicalTypes.get(systemId);
        return types == null ? new ArrayList<String>() : types;
    }
    
    /**
     * Method for handling GetAvailableCommTypes request.  
     * @param message
     *      Message containing the necessary data.
     * @param systemId
     *      ID of the controller from which the response originated
     */
    private synchronized void eventGetCommsTypesResponseData(final GetAvailableCommTypesResponseData message, 
            final int systemId)
    {
        final List<String> newTypesList = new ArrayList<String>(message.getProductTypeList());
        final CommType type = message.getCommType();
        
        List<String> oldComms = null;
        if (type.equals(CommType.TransportLayer))
        {
            oldComms = m_TransportTypes.get(systemId);
        }
        else if (type.equals(CommType.Linklayer))
        {
            oldComms = m_LinkTypes.get(systemId);
        }
        else if (type.equals(CommType.PhysicalLink))
        {
            oldComms = m_PhysicalTypes.get(systemId);
        }
        else
        {
            m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_WARN, "Unknown Layer Type", //NOCHECKSTYLE
                   String.format("Received information about %s, this type is not a known comms layer type", //multiple
                           type)); //string literals : used for consistency.
        }
        
        //used to maintain list of existing types
        final List<String> updatedList = new ArrayList<String>();
        
        if (oldComms != null)
        {
            for (String oldComm : oldComms)
            {
                //if item is in new list remove from new list
                if (newTypesList.contains(oldComm))
                {
                    newTypesList.remove(oldComm); //remove already known type from list of new types
                    updatedList.add(oldComm);
                }
                else //if type no longer exists 
                {
                    //remove the capabilties object corresponding to obsolete type
                    final Map<String, Object> capabilities = m_Capabilities.get(systemId);
                    capabilities.remove(oldComm);
                }
            }
        }
        
        //used to ensure that capabilities requests are only sent if type is new
        for (String newComms : newTypesList)
        {
            updatedList.add(newComms);
            final GetCapabilitiesRequestData requestMessage = GetCapabilitiesRequestData.newBuilder().setCommType(type).
                    setProductType(newComms).build();
            
            final CapabilitiesResponseHandler handler = new CapabilitiesResponseHandler(newComms);
            
            m_MessageFactory.createCustomCommsMessage(CustomCommsMessageType.GetCapabilitiesRequest,
                    requestMessage).queue(systemId, handler);
        }
        
        if (type.equals(CommType.TransportLayer))
        {
            m_TransportTypes.put(systemId, updatedList);
        }
        else if (type.equals(CommType.Linklayer))
        {
            m_LinkTypes.put(systemId, updatedList);
        }
        else if (type.equals(CommType.PhysicalLink))
        {
            m_PhysicalTypes.put(systemId, updatedList);
        }
        
        Logging.log(LogService.LOG_DEBUG, "Added comms types %s of type %s", updatedList.toString(), type.toString());
                
        //post notice
        m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Layer Types Updated", 
                String.format("Controller 0x%08x updated the list of available %ss", systemId, type.toString()));       
    }  
    
    
    /**
     * Function to add a new capabilities object to the set of known capabilties objects.
     * @param systemId
     *  the id of the system that the capabilties object pertains to
     * @param caps
     *  the capabilties object that is to be added
     * @param commsFQCN
     *  the fully qualified class name that the to be saved capabilties object refers to
     */
    private void addCapabilities(final int systemId, final BaseCapabilities caps, final String commsFQCN)
    {
        Map<String, Object> capabilities = new HashMap<String, Object>();
        
        synchronized (m_Capabilities)
        {
            if (m_Capabilities.containsKey(systemId))
            {
                capabilities = m_Capabilities.get(systemId);
            }
            else 
            {
                m_Capabilities.put(systemId, capabilities);
            }   
        }        
        capabilities.put(commsFQCN, caps);
    }
    
    @Override
    public Object getCapabilities(final int systemId, final String clazzName)
    {
        if (m_Capabilities.containsKey(systemId))
        {
            return m_Capabilities.get(systemId).get(clazzName);
        }        
        return null;
    }
    
    @Override
    public void requestLayerTypesUpdate(final int systemId)
    {        
        GetAvailableCommTypesRequestData typesRequest = GetAvailableCommTypesRequestData.newBuilder().
                setCommType(CommType.TransportLayer).build();
        m_MessageFactory.createCustomCommsMessage(CustomCommsMessageType.GetAvailableCommTypesRequest, 
                typesRequest).queue(systemId, null);
        
        typesRequest = GetAvailableCommTypesRequestData.newBuilder().
                setCommType(CommType.Linklayer).build();
        m_MessageFactory.createCustomCommsMessage(CustomCommsMessageType.GetAvailableCommTypesRequest, 
                typesRequest).queue(systemId, null);
        
        typesRequest = GetAvailableCommTypesRequestData.newBuilder().
                setCommType(CommType.PhysicalLink).build();
        m_MessageFactory.createCustomCommsMessage(CustomCommsMessageType.GetAvailableCommTypesRequest, 
                typesRequest).queue(systemId, null);
    }
    
    @Override
    public String getImage(final String commsType, final int controllerId)
    {
        if (m_TransportTypes.get(controllerId) != null && m_TransportTypes.get(controllerId).contains(commsType))
        {
            return m_CommsImageInterface.getTransportImage((TransportLayerCapabilities)getCapabilities(
                    controllerId, commsType));
        }
        
        if (m_LinkTypes.get(controllerId) != null && m_LinkTypes.get(controllerId).contains(commsType))
        {
            return m_CommsImageInterface.getLinkLayerImage((LinkLayerCapabilities)
                    getCapabilities(controllerId, commsType));
        }
        
        return m_CommsImageInterface.getPhysicalLinkImage();
    }
    
    @Override
    public String getPhysicalLinkClassByType(final int systemId, final PhysicalLinkTypeEnum type)
    {
        for (String className : getPhysicalLinkClasses(systemId))
        {
            if (tryGetPhysicalLinkType(systemId, className) == type)
            {
                return className;
            }
        }
        
        return null;
    }
    
    @Override
    public List<PhysicalLinkTypeEnum> getPhysicalLinkTypes(final int systemId)
    {
        final List<PhysicalLinkTypeEnum> linkTypes = new ArrayList<>();
        
        for (String className : getPhysicalLinkClasses(systemId))
        {
            final PhysicalLinkCapabilities physCaps = (PhysicalLinkCapabilities) getCapabilities(systemId, className);
            if (physCaps != null)
            {
                linkTypes.add(physCaps.getLinkType());
            }
        }
        
        return linkTypes;
    }
    
    /**
     * Get the physical link type for the physical link with the specified class.
     * @param systemId
     *      the id of the system that the physical link resides on
     * @param className
     *      the class name of the physical link
     * @return
     *      the string that represents the physical link type enumeration
     */
    private PhysicalLinkTypeEnum tryGetPhysicalLinkType(final int systemId, final String className) 
    {
        final PhysicalLinkCapabilities physCaps = (PhysicalLinkCapabilities) getCapabilities(systemId, className);
        if (physCaps != null)
        {
            return physCaps.getLinkType();
        }
        return null;
    }
    
    /**
     * 
     * Handles events related to getting available comms types for a controller. 
     *
     */
    class EventHelperCommsTypes implements EventHandler
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
            
            // register to listen to the getavailablecommstypes events
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            final String[] topics = {RemoteConstants.TOPIC_MESSAGE_RECEIVED};
            props.put(EventConstants.EVENT_TOPIC, topics);
            final String filterString = String.format("(&(%s=%s)(%s=%s))", 
                RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.CustomComms.toString(),
                RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
                    CustomCommsMessageType.GetAvailableCommTypesResponse.toString());
            props.put(EventConstants.EVENT_FILTER, filterString);
            
            //register the event handler that listens for asset directory responses
            m_Registration = context.registerService(EventHandler.class, this, props);
        } 
        
        /**
         * Unregister the event listener.
         */
        public void unregisterListener()
        {
            m_Registration.unregister();
        }
        
        @Override
        public void handleEvent(final Event event)
        {
            //pull out controller ID 
            final int systemId = (Integer)event.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID);
            
            eventGetCommsTypesResponseData((GetAvailableCommTypesResponseData)event.
                    getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), systemId);  
            
            final Map<String, Object> props = null;  
            m_EventAdmin.postEvent(new Event(CommsMgr.TOPIC_COMMS_LAYER_UPDATED, props));
        }
    }
    
    /**
     * ResponseHandler for a capabilities object request for a comms type.
     * @author nickmarcucci
     *
     */
    public class CapabilitiesResponseHandler implements ResponseHandler
    {
        /**
         * The fully qualified class name that this capabilities response is associated with.
         */
        final private String m_TypeName;
        
        /**
         * Constructor for the CapabilitiesResponseHandler.
         * @param type
         *      layer type to which the capabilities object corresponds
         */
        public CapabilitiesResponseHandler(final String type)
        {
            m_TypeName = type;
        }
        
        @Override
        public void handleResponse(final TerraHarvestMessage message, final TerraHarvestPayload payload, 
                final Message namespaceMessage, final Message dataMessage)
        {
            final int systemId = message.getSourceId();
            
            if (payload.getNamespace() == Namespace.Base)
            {
                final BaseNamespace baseMessage = (BaseNamespace)namespaceMessage;
                if (baseMessage.getType() == BaseMessageType.GenericErrorResponse)
                {
                    final GenericErrorResponseData data = (GenericErrorResponseData)dataMessage;
                    
                    m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Capabilities Retrieval Error", 
                        String.format("An error (%s) occurred trying to retrieve capabilities for comm type [%s]: %s", 
                                    data.getError(), m_TypeName, data.getErrorDescription()));
                }
            }
            else if (payload.getNamespace() == Namespace.CustomComms)
            {
                final GetCapabilitiesResponseData data = (GetCapabilitiesResponseData)dataMessage;
                final CommType type = data.getCommType();
                try
                {
                    if (type == CommType.Linklayer)
                    {
                        final LinkLayerCapabilities caps = 
                                (LinkLayerCapabilities) m_Converter.convertToJaxb(data.getLinkCapabilities());
                        addCapabilities(systemId, caps, m_TypeName);
                    }
                    else if (type == CommType.TransportLayer)
                    {
                        final TransportLayerCapabilities caps = 
                                (TransportLayerCapabilities) m_Converter.convertToJaxb(data.getTransportCapabilities());
                        addCapabilities(systemId, caps, m_TypeName);
                    }
                    else if (type == CommType.PhysicalLink)
                    {
                        final PhysicalLinkCapabilities caps = 
                                (PhysicalLinkCapabilities) m_Converter.convertToJaxb(data.getPhysicalCapabilities());
                        addCapabilities(systemId, caps, m_TypeName);
                    }
                    else
                    {
                        m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_WARN, "Unknown Layer Type", 
                               String.format("Received capabilities data about %s, this type is not a known comms layer"
                                       + " type", type));
                    }
                }
                catch (final ObjectConverterException exception)
                {
                    m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, 
                            "Object Conversion Exception", 
                            String.format("An error occurred trying to retrieve capabilities for comm type [%s]", 
                                    m_TypeName),
                            exception);
                }
            }
        }
    }
}
