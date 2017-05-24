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
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.inject.Inject;

import com.google.protobuf.Message;

import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.link.LinkLayer.LinkStatus;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CommType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateLinkLayerResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateTransportLayerRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace.CustomCommsMessageType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetAvailableCommTypesRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetLayerNameRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetLayerNameResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetLayersRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetLayersResponseData;
import mil.dod.th.core.remote.proto.LinkLayerMessages;
import mil.dod.th.core.remote.proto.LinkLayerMessages.GetStatusRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.GetStatusResponseData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.IsActivatedRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.IsActivatedResponseData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.LinkLayerNamespace.LinkLayerMessageType;
import mil.dod.th.core.remote.proto.MapTypes.SimpleTypesMapEntry;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.PhysicalLinkNamespace.PhysicalLinkMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.core.remote.proto.SharedMessages.FactoryObjectInfo;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.core.remote.proto.TransportLayerMessages;
import mil.dod.th.core.remote.proto.TransportLayerMessages.TransportLayerNamespace.TransportLayerMessageType;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ConfigurationWrapper;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ModifiablePropertyModel;
import mil.dod.th.ose.gui.webapp.controller.ControllerMgr;
import mil.dod.th.ose.gui.webapp.factory.AbstractFactoryBaseModel;
import mil.dod.th.ose.gui.webapp.factory.FactoryBaseModel;
import mil.dod.th.ose.gui.webapp.factory.FactoryObjMgr;
import mil.dod.th.ose.gui.webapp.general.RemoteEventRegistrationHandler;
import mil.dod.th.ose.gui.webapp.remote.RemoteEvents;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.ose.remote.api.EnumConverter;
import mil.dod.th.ose.shared.SharedMessageUtils;

import org.glassfish.osgicdi.OSGiService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;


/**
 * Implementation class for the comms manager interface.  
 * @author bachmakm
 *
 */
@ManagedBean(name = "commsMgr") //NOCHECKSTYLE - max fan out complexity - classes needed for operation
@ApplicationScoped
public class CommsMgrImpl implements CommsMgr, FactoryObjMgr //NOPMD - excessive class length, too many fields
                                                            //Current implementation requires all fields given here.
                                                          //TD look into possibility that this class could be broken up.
{    
    /**
     * String used to register for remote events.
     */
    private final static String ALL_STRING = "*";
    
    /**
     * Remote event response handler that keeps track of remote send event registration IDs.
     */
    private RemoteEventRegistrationHandler m_RemoteHandler; 
      
    /**
     * Map of all available transport layers corresponding to each registered controller.
     */
    private final Map<Integer, List<CommsLayerBaseModel>> m_TransportLayers;
    
    /**
     * Map of all available link layers corresponding to each registered controller.
     */
    private final Map<Integer, List<CommsLayerLinkModelImpl>> m_LinkLayers;
    
    /**
     * Map of all available physical links corresponding to each registered controller.
     */
    private final Map<Integer, List<CommsLayerBaseModel>> m_PhysicalLayers;
    
    /**
     * Map of all unused physical links on a particular controller.  Used
     * to build comms stacks. 
     */
    private final Map<Integer, List<CommsLayerBaseModel>> m_UnusedPhysicalLayers;
    
    /**
     * Event handler helper class.  Listens for CustomComms namespace messages.
     */
    private EventHelperCustomCommsNamespace m_EventHelperComms;
    
    /**
     * Event handler helper class.  Listens for LinkLayer namespace messages.
     */
    private EventHelperLinkLayerNamespace m_EventHelperLink;
    
    /**
     * Event handler helper class.  Listens for the addition or removal of controllers.
     */
    private EventHelperControllerEvent m_ControllerEventListener;
    
    /**
     * Event handler helper class. Listens for EventAdmin namespace messages.
     */
    private EventHelperEventAdminNamespace m_EventHelperEvent;
   
    /**
     * Reference to the {@link CommsLayerTypesMgr}.
     */
    @ManagedProperty(value = "#{commsTypesMgr}")
    private CommsLayerTypesMgr commsTypesMgr; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
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
     * The image display interface that is to be used by all comms models.
     */
    @Inject
    private CommsImage m_CommsImageInterface;
    
    /**
     * Inject the MessageFactory service for creating remote messages.
     */
    @Inject @OSGiService
    private MessageFactory m_MessageFactory;
    
    /**
     * Reference to the configuration wrapper bean.
     */
    @ManagedProperty(value = "#{configWrapper}")
    private ConfigurationWrapper configWrapper; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Inject the EventAdmin service for posting events.
     */
    @Inject @OSGiService
    private EventAdmin m_EventAdmin;

    /**
     * Comms manager constructor.
     */
    public CommsMgrImpl()
    {        
        super();
        m_TransportLayers = Collections.synchronizedMap(new HashMap<Integer, List<CommsLayerBaseModel>>());
        m_LinkLayers = Collections.synchronizedMap(new HashMap<Integer, List<CommsLayerLinkModelImpl>>());
        m_PhysicalLayers = Collections.synchronizedMap(new HashMap<Integer, List<CommsLayerBaseModel>>());
        m_UnusedPhysicalLayers = Collections.synchronizedMap(new HashMap<Integer, List<CommsLayerBaseModel>>());
    }
    
    /**
     * Register event listeners.
     */
    @PostConstruct
    public void registerEventHelpers()
    {
        m_RemoteHandler = new RemoteEventRegistrationHandler(m_MessageFactory);

        //instantiate handlers
        m_EventHelperComms = new EventHelperCustomCommsNamespace();
        m_EventHelperLink = new EventHelperLinkLayerNamespace();
        m_ControllerEventListener = new EventHelperControllerEvent();
        m_EventHelperEvent = new EventHelperEventAdminNamespace();
        
        //register to listen to delegated events
        m_ControllerEventListener.registerControllerEvents();
        m_EventHelperLink.registerForEvents();
        m_EventHelperComms.registerForEvents();
        m_EventHelperEvent.registerForEvents();
    }
    
    /**
     * Unregister handlers before destruction of the bean.
     */
    @PreDestroy
    public void unregisterHelpers()
    {
        //Unregister for events
        m_ControllerEventListener.unregisterListener();
        m_EventHelperLink.unregisterListener();
        m_EventHelperComms.unregisterListener();
        m_EventHelperEvent.unregisterListener();
        m_RemoteHandler.unregisterRegistrations();
    }
    
    /**
     * Set the comms layer types manager service.
     * @param commsTypesManager
     *      comms types manager service to set
     */
    public void setCommsTypesMgr(final CommsLayerTypesMgr commsTypesManager)
    {
        commsTypesMgr = commsTypesManager;
    } 
    
    /**
     * Set the {@link ConfigurationWrapper} service.
     * @param wrapper
     *      {@link ConfigurationWrapper} service to set
     */
    public void setConfigWrapper(final ConfigurationWrapper wrapper)
    {
        configWrapper = wrapper;
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
     * Set the {@link EventAdmin} service.
     * @param eventAdmin
     *      the event admin service to use.
     */
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Set the {@link CommsImage} image display interface to use.
     * @param imgInterface
     *  the image display interface to use.
     */
    public void setCommsImageInterface(final CommsImage imgInterface)
    {
        m_CommsImageInterface = imgInterface;
    }
    
    /**
     * Method for handling requests to get the generic status (OK or LOST) and activation status of a 
     * link having a specific UUID and belonging to a particular controller. 
     * Method is only called when a new link layer is created.
     * @param protoUUID
     *      UUID of the link layer being queried
     * @param systemId
     *      the system ID of the location to send requests
     */
    private void sendRequestsForLinkMetadata(final SharedMessages.UUID protoUUID, final int systemId)
    {
        final GetStatusRequestData statusRequest = GetStatusRequestData.newBuilder().setUuid(protoUUID).build();
        m_MessageFactory.createLinkLayerMessage(LinkLayerMessageType.GetStatusRequest, 
                statusRequest).queue(systemId, null);
        
        final IsActivatedRequestData isActivatedRequest = IsActivatedRequestData.newBuilder().
                setUuid(protoUUID).build();
        m_MessageFactory.createLinkLayerMessage(LinkLayerMessageType.IsActivatedRequest, 
                isActivatedRequest).queue(systemId, null);
    }
    
     
    /**
     * Event response handling for the custom comms namespace's GetAllLayers response.  Each layer is added to its 
     * respective map of layers.  This method is called with the addition of a new controller or as a result
     * of receiving a gateway response.  
     * @param data
     *      the data message that contains the response information
     * @param systemId
     *      the system ID from which the response originated
     */
    private synchronized void eventGetAllLayersResponse(final GetLayersResponseData data, final int systemId)
    {
        final CommType type = data.getCommType(); 
        
        switch (type)
        {
            case TransportLayer:
                if (m_TransportLayers.containsKey(systemId))
                {
                    final List<UUID> newUuids = mergeLayersHelper(type, data.getLayerInfoList(), systemId);
                    removeObsoleteLayerHelper(newUuids, m_TransportLayers.get(systemId));                    
                }
                else
                {
                    m_TransportLayers.put(systemId, new ArrayList<CommsLayerBaseModel>());             
                    addAllLayersHelper(type, data.getLayerInfoList(), systemId);
                }
                break;
            case Linklayer:
                if (m_LinkLayers.containsKey(systemId))
                {
                    final List<UUID> newUuids = mergeLayersHelper(type, data.getLayerInfoList(), systemId);
                    removeObsoleteLayerHelper(newUuids, m_LinkLayers.get(systemId));                    
                }
                else
                {
                    m_LinkLayers.put(systemId, new ArrayList<CommsLayerLinkModelImpl>());  
                    addAllLayersHelper(type, data.getLayerInfoList(), systemId); 
                }
                break;
            case PhysicalLink:
                if (m_PhysicalLayers.containsKey(systemId))
                {
                    final List<UUID> newUuids = mergeLayersHelper(type, data.getLayerInfoList(), systemId);
                    removeObsoleteLayerHelper(newUuids, m_PhysicalLayers.get(systemId));                    
                }
                else
                {
                    m_PhysicalLayers.put(systemId, new ArrayList<CommsLayerBaseModel>());
                    addAllLayersHelper(type, data.getLayerInfoList(), systemId);
                }
                break;
            default:
                Logging.log(LogService.LOG_DEBUG, 
                        "Controller 0x%08x process add layers event. Given comm type is invalid.", systemId);
                
                throw new IllegalArgumentException(String.format("Controller 0x%08x cannot process " 
                        + "add layers event. Invalid comm type %s", systemId, type.toString()));                
        }
    }
    
    /**
     * Helper method which adds new layers to the layer map corresponding to the given type 
     * using the data contained in the given FactoryObjectInfo list.
     * @param type
     *      layer type to which the info list corresponds
     * @param infoList
     *      list of information needed to create a new layer object
     * @param systemId
     *      ID of the controller to which the layers belong. 
     */
    private void addAllLayersHelper(final CommType type, final List<FactoryObjectInfo> infoList, final int systemId)
    {
        for (FactoryObjectInfo info: infoList)
        {
            final UUID uuid = SharedMessageUtils.convertProtoUUIDtoUUID(info.getUuid());
            
            if (type.equals(CommType.TransportLayer))
            {
                processNewTransportLayer(systemId, uuid, info.getPid(), info.getProductType());
            }
            else if (type.equals(CommType.Linklayer))
            {
                processNewLinkLayer(systemId, uuid, info.getPid(), info.getProductType());
            }
            else if (type.equals(CommType.PhysicalLink))
            {
                processNewPhysicalLink(systemId, uuid, info.getPid(), info.getProductType());
            }
        }        
    }
    
    /**
     * Helper method for merging data from the given FactoryObjectInfo list with the existing data
     * in the layer map.  Method iterates through the given info list, and adds new layers if they
     * are not found in the current list contained in the layer map.  Method also maintains
     * a list of the UUIDs contained in the given infoList to be used to remove any models
     * that should no longer exist in the layer map.  
     * @param type
     *      type of layer to which the FactoryObjectInfo list corresponds.
     * @param infoList
     *      list of information needed to create a new layer object
     * @param systemId
     *      ID of the controller to which the layers belong.
     * @return
     *      List of all UUIDs in the new list of layers
     */
    private synchronized List<UUID> mergeLayersHelper(final CommType type, final List<FactoryObjectInfo> infoList, 
            final int systemId)
    {        
        final List<UUID> tmpUuidList = new ArrayList<UUID>();
        for (FactoryObjectInfo info: infoList)
        {
            final UUID uuid = SharedMessageUtils.convertProtoUUIDtoUUID(info.getUuid());
            final String pid = info.getPid();
            final String productType = info.getProductType();
            
            if (type.equals(CommType.TransportLayer))
            {
                final List<CommsLayerBaseModel> layerList = m_TransportLayers.get(systemId);
                if (findLayer(uuid, layerList) == null) //if layer does not exist
                {                    
                    processNewTransportLayer(systemId, uuid, pid, productType);
                }
                tmpUuidList.add(uuid);
            }
            else if (type.equals(CommType.Linklayer)) //if layer does not exist
            {
                final List<CommsLayerLinkModelImpl> layerList = m_LinkLayers.get(systemId);
                if (findLayer(uuid, layerList) == null)
                {
                    processNewLinkLayer(systemId, uuid, pid, productType);
                }
                tmpUuidList.add(uuid);
            }
            else if (type.equals(CommType.PhysicalLink))
            {
                final List<CommsLayerBaseModel> layerList = m_PhysicalLayers.get(systemId);
                if (findLayer(uuid, layerList) == null) //if layer does not exist
                {
                    processNewPhysicalLink(systemId, uuid, pid, productType);
                }
                tmpUuidList.add(uuid); 
            }
        }   
        return tmpUuidList;
    }
    
    
    /**
     * Helper method for removing obsolete layers from the given list of layers.  
     * The given list of UUIDs corresponds to the new list of layers received.  
     * @param newUuids
     *      list of UUIDs corresponding to the new list of layers received
     * @param layerList
     *      list of layers potentially containing obsolete layers
     *      
     */
    private synchronized void removeObsoleteLayerHelper(final List<UUID> newUuids, 
            final List<? extends FactoryBaseModel> layerList)    
    {
        if (newUuids.size() < layerList.size())
        { 
            //get all the current UUIDs in the given layers list
            final List<UUID> currentUuids = getAllLayerUuids(layerList);
            removeModel(newUuids, currentUuids, layerList);            
        }
    }
    
    /**
     * Helper method for removing a model based on a list of new layer UUIDs and the list of current UUIDs.
     * @param newUuids
     *      List of UUIDs corresponding to the new layers received
     * @param currentUuids
     *      List of UUIDs corresponding the current layers in a map
     * @param layersList
     *      list of layers potentially containing obsolete layers
     */
    private void removeModel(final List<UUID> newUuids, final List<UUID> currentUuids, 
            final List<? extends FactoryBaseModel> layersList)
    {

        if (newUuids.isEmpty()) //updated list says there are no layers of this type 
        {
            layersList.clear(); //clear list of layers
        }
        else
        {
            //get the UUIDs that are no longer in the updated list of layers
            currentUuids.removeAll(newUuids);
            for (UUID uuid : currentUuids)
            {
                //remove obsolete layers from the given list
                layersList.remove(findLayer(uuid, layersList));
            }
        }
    }
    
    /**
     * Helper method which returns a list of all the UUIDS known to a list of layers.
     * @param models
     *      list of layers to be iterated through.
     * @return
     *      List of all known UUIDS belonging to the given list of layers
     */
    private List<UUID> getAllLayerUuids(final List<? extends FactoryBaseModel> models)
    {
        final List<UUID> uuids = new ArrayList<UUID>();
        for (FactoryBaseModel model : models)
        {
            uuids.add(model.getUuid());
        }        
        return uuids;
    }

    /**
     * Helper method for sending a request to get a layer's name.  
     * @param type
     *      Type of the layer
     * @param uuid
     *      UUID of the layer
     * @param systemId
     *      ID to which the layer corresponds.
     */
    private void sendGetLayerNameRequest(final CommType type, final SharedMessages.UUID uuid, final int systemId)
    {
        //send name request
        final GetLayerNameRequestData nameRequest = GetLayerNameRequestData.newBuilder().
              setCommType(type).
              setUuid(uuid).build();
        m_MessageFactory.createCustomCommsMessage(CustomCommsMessageType.GetLayerNameRequest, 
              nameRequest).queue(systemId, null);
    }
 
    /**
     * Event response handling for the LinkLayer namespace's GetStatus response.
     * @param data
     *     the data message that contains the response information
     * @param systemId
     *     the system ID from which the response originated
     */
    private synchronized void eventGetLinkLayerStatusResponse(final GetStatusResponseData data, final int systemId)
    {        
        final CommsLayerLinkModel link = (CommsLayerLinkModel)findLayer(SharedMessageUtils.
                convertProtoUUIDtoUUID(data.getUuid()), m_LinkLayers.get(systemId));
        if (link == null)
        {
            Logging.log(LogService.LOG_DEBUG, 
                    "Cannot set link status - link layer does not exist on controller 0x%08x", systemId);
        }
        else
        {
            link.setStatus(EnumConverter.convertProtoLinkStatusToJava(data.getLinkStatus()));            
        }        
    }
    
    /**
     * Event response handling for the LinkLayer namespace's IsActivated response message.
     * @param data
     *     the data message that contains the response information
     * @param systemId
     *     the system ID from which the response originated
     */
    private synchronized void eventGetLinkLayerActivatedResponse(final IsActivatedResponseData data, 
            final int systemId)
    {
        
        final CommsLayerLinkModel link = (CommsLayerLinkModel)findLayer(SharedMessageUtils.
                convertProtoUUIDtoUUID(data.getUuid()), m_LinkLayers.get(systemId));
        if (link == null)
        {
            Logging.log(LogService.LOG_DEBUG, 
                    "Cannot set link activation - link layer does not exist on controller 0x%08x", systemId);
        }
        else
        {
            link.setActivated(data.getIsActivated());
        }
    }
    
    /**
     * Helper method for finding a comms layer in a given list of layers. Method returns 
     * <code>null</code> if layer does not exist.  
     * @param uuid
     *      UUID of the layer to be found
     * @param layerList
     *      list of layers to iterate through
     * @return
     *      the comms layer corresponding to the given UUID.  If comms layer does not exist,
     *      method will return <code>null</code>. 
     */
    private FactoryBaseModel findLayer(final java.util.UUID uuid, final List<? extends FactoryBaseModel> layerList)
    {        
        if (layerList != null)
        {
            synchronized (layerList)
            {
                for (FactoryBaseModel layer:layerList)
                {
                    if (layer.getUuid().equals(uuid))
                    {
                        return layer;
                    }
                }
            }
        }
        return null;
    }
        
    /**
     * Request to get comms events.
     * 
     * @param systemId
     *     the system to which to send the request
     */
    public synchronized void requestToListenForRemoteEvents(final int systemId)
    {
        //List of topics
        final List<String> topics = new ArrayList<String>();
        // listen to link layer events
        topics.add(LinkLayer.TOPIC_ACTIVATED); 
        topics.add(LinkLayer.TOPIC_DEACTIVATED); 
        topics.add(LinkLayer.TOPIC_STATUS_CHANGED); 
        
        //send request for link layer event registrations
        RemoteEvents.sendEventRegistration(m_MessageFactory, topics, null, false, systemId, m_RemoteHandler);
        
        topics.clear();
        
        // listen to factory events
        topics.add(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED);
        topics.add(FactoryDescriptor.TOPIC_FACTORY_OBJ_DELETED);
        topics.add(FactoryDescriptor.TOPIC_FACTORY_OBJ_NAME_UPDATED);
        topics.add(FactoryDescriptor.TOPIC_FACTORY_OBJ_PID_CREATED);
        topics.add(FactoryDescriptor.TOPIC_FACTORY_OBJ_PID_REMOVED);
        
        final String filterString = String.format("(|(%s=%s)(%s=%s)(%s=%s))", 
                FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, LinkLayer.class.getSimpleName(),
                FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, PhysicalLink.class.getSimpleName(),
                FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, TransportLayer.class.getSimpleName());     
        
        //send request for factory descriptor registrations
        RemoteEvents.sendEventRegistration(m_MessageFactory, topics, filterString, false, systemId, m_RemoteHandler);
    }
    
    /**
     * Method for adding/updating a comms stack based on remote factory event received.
     * Specifically, the method processes events about the addition or removal of specific
     * comms layers.    
     * @param event
     *      properties of the event
     */
    private synchronized void processFactoryEvent(final Event event) //NOCHECKSTYLE: cyclomatic complexity is 13. Need to 
                                                                   // account for all factory object events.
    {           
        //pull out event props
        final String topic = event.getTopic(); 
        final int systemId = (Integer)event.getProperty(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID);        
        final String baseType = (String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE);        
        
        // add new layer to a stack if the new layer does not already exist
        if (topic.contains(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED))
        {
            processNewLayerHelper(event);

            //post notice
            m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "New Layer Added", 
                    String.format("Controller 0x%08x added a new %s", systemId, baseType));
        }
        else if (topic.contains(FactoryDescriptor.TOPIC_FACTORY_OBJ_DELETED))
        {           
            processDeleteLayerHelper(event);

            //post notice
            m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Layer Deleted", 
                    String.format("Controller 0x%08x deleted a %s", systemId, baseType));
        }
        else if (topic.contains(FactoryDescriptor.TOPIC_FACTORY_OBJ_NAME_UPDATED))
        {
            //check the name
            final String name = (String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_NAME);

            //pull out the uuid and update layer name
            final UUID uuid = UUID.fromString((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_UUID)); 
            updateLayerName(convertBaseTypeToCommType(baseType), uuid, systemId, name); 
        }
        else if (topic.contains(FactoryDescriptor.TOPIC_FACTORY_OBJ_PID_CREATED))
        {
            final String pid = (String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_PID);
            final UUID uuid = UUID.fromString((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_UUID)); 
            findLayer(uuid, findListByType(convertBaseTypeToCommType(baseType), systemId)).setPid(pid);
        }
        else if (topic.contains(FactoryDescriptor.TOPIC_FACTORY_OBJ_PID_REMOVED))
        {
            final UUID uuid = UUID.fromString((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_UUID)); 
            findLayer(uuid, findListByType(convertBaseTypeToCommType(baseType), systemId)).setPid("");
        }
    }
    
    /**
     * Helper method for converting a given string base comm type
     * to an actual comm type object.
     * @param baseType
     *      string representation of a comm type
     * @return
     *      actual protobuf comm type
     */
    private CommType convertBaseTypeToCommType(final String baseType)
    {
        if (baseType.equals(TransportLayer.class.getSimpleName()))
        {
            return CommType.TransportLayer;
        }
        else if (baseType.equals(LinkLayer.class.getSimpleName()))
        {
            return CommType.Linklayer;
        }
        else if (baseType.equals(PhysicalLink.class.getSimpleName()))
        {
            return CommType.PhysicalLink;
        } 
        throw new IllegalArgumentException("Cannot convert " + baseType + " to a valid CommType."); 
    }
       
    /**
     * Helper method used to reduce the cyclomatic complexity of the caller method.  Specifically, method verifies
     * that layer does not already exist in map and proceeds to add it to the list of stacks.  If the type of the new
     * layer is either transport or link, a request for the child layer information is sent.
     * @param event
     *      properties of the remote event
     *      
     */
    private void processNewLayerHelper(final Event event)
    {
        // pull out necessary event properties
        final java.util.UUID uuid = java.util.UUID.fromString((String)event.
                getProperty(FactoryDescriptor.EVENT_PROP_OBJ_UUID));
        final String pid = (String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_PID);
        final int systemId = (Integer)event.getProperty(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID);        
        final String baseType = (String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE);
        final String clazz = (String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_TYPE);
        
        //check type of layer added and verify that the layer does not already exist
        if (baseType.contains(TransportLayer.class.getSimpleName()) 
                && findLayer(uuid, m_TransportLayers.get(systemId)) == null)
        {
            addToMapIfNew(systemId, CommType.TransportLayer);
            processNewTransportLayer(systemId, uuid, pid, clazz);
        }
        else if (baseType.contains(LinkLayer.class.getSimpleName()) 
                && findLayer(uuid, m_LinkLayers.get(systemId)) == null)
        {
            addToMapIfNew(systemId, CommType.Linklayer);
            processNewLinkLayer(systemId, uuid, pid, clazz);
        }
        else if (baseType.contains(PhysicalLink.class.getSimpleName()) 
                &&  findLayer(uuid, m_PhysicalLayers.get(systemId)) == null)
        {
            addToMapIfNew(systemId, CommType.PhysicalLink);
            processNewPhysicalLink(systemId, uuid, pid, clazz);
        }
    }
    
    /**
     * Helper method used to check if a map has been initialized with a key-value pair upon getting a remote 
     * request to add a layer.  If the map does not contain the given systemId, a new key-value pair is added
     * to the map using the systemId.
     * @param systemId
     *      ID of the controller.  Also acts as the key in the hash map of the different layers.
     * @param type
     *      type of layer attempting to be added.
     */
    private void addToMapIfNew(final int systemId, final CommType type)
    {
        switch (type)
        {
            case TransportLayer:
                if (!m_TransportLayers.containsKey(systemId))
                {
                    m_TransportLayers.put(systemId, new ArrayList<CommsLayerBaseModel>());                    
                }
                break;
            case Linklayer:
                if (!m_LinkLayers.containsKey(systemId))
                {
                    m_LinkLayers.put(systemId, new ArrayList<CommsLayerLinkModelImpl>());                    
                }
                break;
            case PhysicalLink:
                if (!m_PhysicalLayers.containsKey(systemId))
                {
                    m_PhysicalLayers.put(systemId, new ArrayList<CommsLayerBaseModel>());
                }
                break;
            default:
                break;               
        }
    }
    
    /**
     * Helper method for returning the list of layers based on given comm type and controller ID.
     * @param type
     *      Protobuf type of comm layer
     * @param systemId
     *      ID of the controller to which the list of layers belong
     * @return
     *      list of layers corresponding to given type and controller ID
     */
    private List<? extends FactoryBaseModel> findListByType(final CommType type, final int systemId)
    {
        if (type.equals(CommType.TransportLayer))
        {
            return m_TransportLayers.get(systemId);
        }
        else if (type.equals(CommType.Linklayer))
        {
            return m_LinkLayers.get(systemId);
        }
        else if (type.equals(CommType.PhysicalLink))
        {
            return m_PhysicalLayers.get(systemId);
        } 
        throw new IllegalArgumentException(String.format("Cannot find list on Controller 0x%08x " 
                + "given CommType %s is invalid.", systemId, type.toString())); 
    }
    
    /**
     * Helper method used to reduce the cyclomatic complexity of the caller method.  Specifically, method
     * finds model containing layer that was remotely deleted and effectively deletes the local layer 
     * instance by setting it to null.
     * @param event
     *      properties of the event
     */
    private void processDeleteLayerHelper(final Event event)
    {
        // pull out event properties
        final java.util.UUID uuid = java.util.UUID.fromString((String)event.
                getProperty(FactoryDescriptor.EVENT_PROP_OBJ_UUID));
        final int systemId = (Integer)event.getProperty(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID);        
        final String baseType = (String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE);        
        
        final List<? extends FactoryBaseModel> layerList = findListByType(convertBaseTypeToCommType(baseType), 
                systemId);
        final FactoryBaseModel model = findLayer(uuid, layerList);
        if (model != null)
        {
            layerList.remove(model);
        }
    } 
    
    /**
     * Method for updating a link layer based on the remote message received.  
     * @param event
     *      event properties
     */
    private void processRemoteLinkLayerEvent(final Event event)
    {
        //pull out event props
        final int systemId = (Integer)event.getProperty(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID);
        final String topic = event.getTopic(); 
        final java.util.UUID uuid = java.util.UUID.fromString((String)event.
                getProperty(FactoryDescriptor.EVENT_PROP_OBJ_UUID));
        
        final CommsLayerLinkModel link = (CommsLayerLinkModel)findLayer(uuid, m_LinkLayers.get(systemId));

        if (link == null)
        {
            Logging.log(LogService.LOG_DEBUG, 
                    "Cannot update link - link layer does not exist on controller 0x%08x", systemId);
        }
        else
        {
            if (topic.contains(LinkLayer.TOPIC_ACTIVATED))
            {
                link.setActivated(true);
                
                //post notice
                m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Link Activated", 
                        String.format("Controller 0x%08x activated link layer %s", systemId, link.getName()));
            }
            else if (topic.contains(LinkLayer.TOPIC_DEACTIVATED))
            {
                link.setActivated(false);
                
                //post notice
                m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Link Deactivated", 
                        String.format("Controller 0x%08x deactivated link layer %s", systemId, link.getName()));
            }
            else if (topic.contains(LinkLayer.TOPIC_STATUS_CHANGED))
            {                  
                final LinkStatus status = (LinkStatus)event.getProperty(LinkLayer.EVENT_PROP_LINK_STATUS);
                link.setStatus(status);                
                
                //post notice
                m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Link Status Changed", 
                        String.format("Controller 0x%08x updated link layer %s with new status", systemId, 
                            link.getName()));
            } 
        }
    }
    
    /**
     * Process the get layer name response message.
     * @param message
     *     the message that contains the name property value
     * @param systemId
     *     the system ID from which the response message originated
     */
    private void processGetLayerNameResponse(final GetLayerNameResponseData message, final int systemId)
    {         
        //update
        final UUID uuid = SharedMessageUtils.convertProtoUUIDtoUUID(message.getUuid());
        updateLayerName(message.getCommType(), uuid, systemId, message.getLayerName());
    }
    
    /**
     * Update the name of a layer.
     * @param type
     *     the type of the layer
     * @param uuid
     *     the UUID of the layer
     * @param systemId
     *     the ID of the system from which the update came from
     * @param name
     *     the name to update to
     */
    private void updateLayerName(final CommType type, final UUID uuid, final int systemId, final String name)
    {
        if (type != null)
        {
            ((AbstractFactoryBaseModel)findLayer(uuid, findListByType(type, systemId))).updateName(name);

            m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Layer updated.",
                    String.format("The %s comms layer with UUID %s from controller 0x%08x has had its name updated to "
                            + "[%s].", type, uuid.toString(), systemId, name));
        }
    }
    
    /**
     * Helper method used to get the available types of link and transport layers.
     * @param systemId
     *      ID of the controller to send requests
     */
    private void sendLayerTypesRequest(final int systemId)
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
    
    /**
     * Create a new model for the given transport layer.
     * 
     * @param systemId
     *      id of the system that has the comm layer
     * @param uuid
     *      UUID of the comm layer
     * @param pid
     *      PID of the comm layer
     * @param productType
     *      product type of the comm layer
     */
    private void processNewTransportLayer(final int systemId, final UUID uuid, final String pid, 
            final String productType)
    {
        m_TransportLayers.get(systemId).add(new CommsLayerBaseModel(systemId, uuid, pid, productType,
                CommType.TransportLayer, this, commsTypesMgr, configWrapper, m_CommsImageInterface));
        sendGetLayerNameRequest(CommType.TransportLayer, SharedMessageUtils.convertUUIDToProtoUUID(uuid), systemId);
        
        Logging.log(LogService.LOG_INFO, "Processed new transport layer [%s]: systemId=0x%08X, UUID=%s", 
                productType, systemId, uuid);
    }
    
    /**
     * Create a new model for the given link layer.
     * 
     * @param systemId
     *      id of the system that has the comm layer
     * @param uuid
     *      UUID of the comm layer
     * @param pid
     *      PID of the comm layer
     * @param productType
     *      product type of the comm layer
     */
    private void processNewLinkLayer(final int systemId, final UUID uuid, final String pid, 
            final String productType)
    {
        m_LinkLayers.get(systemId).add(new CommsLayerLinkModelImpl(systemId, uuid, pid, productType, this, 
                commsTypesMgr, configWrapper, m_CommsImageInterface));
        sendRequestsForLinkMetadata(SharedMessageUtils.convertUUIDToProtoUUID(uuid), systemId);
        sendGetLayerNameRequest(CommType.Linklayer, SharedMessageUtils.convertUUIDToProtoUUID(uuid), systemId);
        
        Logging.log(LogService.LOG_INFO, "Processed new link layer [%s]: systemId=0x%08X, UUID=%s", 
                productType, systemId, uuid);
    }
    
    /**
     * Create a new model for the given physical link.
     * 
     * @param systemId
     *      id of the system that has the comm layer
     * @param uuid
     *      UUID of the comm layer
     * @param pid
     *      PID of the comm layer
     * @param productType
     *      product type of the comm layer
     */
    private void processNewPhysicalLink(final int systemId, final UUID uuid, final String pid, 
            final String productType)
    {
        m_PhysicalLayers.get(systemId).add(new CommsLayerBaseModel(systemId, uuid, pid, productType, 
                CommType.PhysicalLink, this, commsTypesMgr, configWrapper, m_CommsImageInterface));    
        sendGetLayerNameRequest(CommType.PhysicalLink, SharedMessageUtils.convertUUIDToProtoUUID(uuid), systemId);
        
        Logging.log(LogService.LOG_INFO, "Processed new physical link [%s]: systemId=0x%08X, UUID=%s", 
                productType, systemId, uuid);
    }
    
    @Override
    public UUID getPhysicalUuidByName(final String name, final int systemId)
    {
        final List<CommsLayerBaseModel> physicals = m_PhysicalLayers.get(systemId);
        
        if (physicals != null)
        {
            synchronized (physicals)
            {
                for (FactoryBaseModel physical : physicals)
                {
                    if (physical.getName().equals(name))
                    {
                        return physical.getUuid();
                    }
                }
            }
        }        
        return null;
    }

    @Override
    public String getPhysicalClazzByName(final String name, final int systemId)
    {
        final List<CommsLayerBaseModel> physicals = m_PhysicalLayers.get(systemId);
        
        if (physicals != null)
        {
            synchronized (physicals)
            {
                for (FactoryBaseModel physical : physicals)
                {
                    if (physical.getName().equals(name))
                    {
                        final CommsLayerBaseModel cbm = (CommsLayerBaseModel) physical;
                        return cbm.getCommsClazz();
                    }
                }
            }
        }        
        return null;
    }
    
    @Override
    public void setUnusedPhysicalLinks(final int systemId, final List<CommsLayerBaseModel> unusedPhysicalLinks)
    {
        m_UnusedPhysicalLayers.put(systemId, unusedPhysicalLinks);
    }
    
    @Override
    public List<String> getUnusedPhysicalLinkNames(final int systemId)
    {
        final List<CommsLayerBaseModel> physicals = m_UnusedPhysicalLayers.get(systemId);
        final List<String> layerNames = new ArrayList<String>();

        if (physicals != null)
        {
            synchronized (physicals)
            {
                for (FactoryBaseModel physical : physicals)
                {
                    layerNames.add(physical.getName());                    
                }
            }
        }        
        return layerNames;
    }
    
    @Override
    public List<CommsLayerBaseModel> getTransportsAsync(final int systemId)
    {        
        if (!m_TransportLayers.containsKey(systemId))
        {
            m_TransportLayers.put(systemId, new ArrayList<CommsLayerBaseModel>());
            //send request for transport layers
            final GetLayersRequestData layerRequest = GetLayersRequestData.newBuilder().
                    setCommType(CommType.TransportLayer).build();        
            m_MessageFactory.createCustomCommsMessage(CustomCommsMessageType.GetLayersRequest, layerRequest).
                queue(systemId, null);

            //send request to listen to remote events 
            requestToListenForRemoteEvents(systemId);
            sendLayerTypesRequest(systemId);
        }        
        return new ArrayList<CommsLayerBaseModel>(m_TransportLayers.get(systemId));        
    }
        
    @Override
    public List<CommsLayerLinkModelImpl> getLinksAsync(final int systemId)
    {        
        if (!m_LinkLayers.containsKey(systemId))
        {
            m_LinkLayers.put(systemId, new ArrayList<CommsLayerLinkModelImpl>());
            //send request for link layers
            final GetLayersRequestData layerRequest = GetLayersRequestData.newBuilder().
                    setCommType(CommType.Linklayer).build();        
            m_MessageFactory.createCustomCommsMessage(CustomCommsMessageType.GetLayersRequest, layerRequest).
                queue(systemId, null);
        }
        return new ArrayList<CommsLayerLinkModelImpl>(m_LinkLayers.get(systemId));
    }
    
    @Override
    public List<CommsLayerBaseModel> getPhysicalsAsync(final int systemId)
    {        
        if (!m_PhysicalLayers.containsKey(systemId))
        {
            m_PhysicalLayers.put(systemId, new ArrayList<CommsLayerBaseModel>());
            //send request for physical layers
            final GetLayersRequestData layerRequest = GetLayersRequestData.newBuilder().
                    setCommType(CommType.PhysicalLink).build();        
            m_MessageFactory.createCustomCommsMessage(CustomCommsMessageType.GetLayersRequest, layerRequest).
                queue(systemId, null);
        }
        return new ArrayList<CommsLayerBaseModel>(m_PhysicalLayers.get(systemId));        
    } 
    
    @Override
    public RemoteCreateLinkLayerHandler createLinkLayerHandler(final CreateTransportLayerRequestData.Builder 
            transportBuild)
    {
        return new RemoteCreateLinkLayerHandler(transportBuild);
    }

    @Override
    public void createConfiguration(final int systemId, 
            final FactoryBaseModel model, final List<ModifiablePropertyModel> properties)
    {
        if (model instanceof CommsLayerLinkModelImpl)
        {
            final LinkLayerMessages.SetPropertyRequestData.Builder builder1 =
                LinkLayerMessages.SetPropertyRequestData.newBuilder()
                    .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(model.getUuid()));
            for (ModifiablePropertyModel prop : properties)
            {
                final Multitype multi = SharedMessageUtils.convertObjectToMultitype(prop.getValue());
                final SimpleTypesMapEntry type =
                        SimpleTypesMapEntry.newBuilder()
                        .setKey(prop.getKey()).setValue(multi)
                        .build();
                builder1.addProperties(type);
            }

            m_MessageFactory.createLinkLayerMessage(LinkLayerMessageType.SetPropertyRequest, builder1.build())
                    .queue(systemId, null);
            return;
        }

        final CommsLayerBaseModel commsModel = (CommsLayerBaseModel)model;
        switch (commsModel.getType())
        {
            case PhysicalLink:
                final PhysicalLinkMessages.SetPropertyRequestData.Builder builder2 =
                    PhysicalLinkMessages.SetPropertyRequestData.newBuilder()
                        .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(model.getUuid()));
                for (ModifiablePropertyModel prop : properties)
                {
                    final Multitype multi = SharedMessageUtils.convertObjectToMultitype(prop.getValue());
                    final SimpleTypesMapEntry type =
                            SimpleTypesMapEntry.newBuilder()
                            .setKey(prop.getKey()).setValue(multi)
                            .build();
                    builder2.addProperties(type);
                }
    
                m_MessageFactory.createPhysicalLinkMessage(PhysicalLinkMessageType.SetPropertyRequest, builder2.build())
                        .queue(systemId, null);
                break;
            case TransportLayer:
                final TransportLayerMessages.SetPropertyRequestData.Builder builder3 =
                    TransportLayerMessages.SetPropertyRequestData.newBuilder()
                        .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(model.getUuid()));
                for (ModifiablePropertyModel prop : properties)
                {
                    final Multitype multi = SharedMessageUtils.convertObjectToMultitype(prop.getValue());
                    final SimpleTypesMapEntry type =
                            SimpleTypesMapEntry.newBuilder()
                            .setKey(prop.getKey()).setValue(multi)
                            .build();
                    builder3.addProperties(type);
                }
    
                m_MessageFactory.createTransportLayerMessage(TransportLayerMessageType.SetPropertyRequest,
                        builder3.build()).queue(systemId, null);
                break;
            default:
                throw new UnsupportedOperationException("CommsMgr does not support creating configurations for "
                        + commsModel.getType());
        }
    }

    /*
     *!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     *!!! EVENT HELPERS and INNER CLASSES ARE BELOW !!!
     *!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     */

    /**
     * Handles controller events and performs actions based on events received.
     */
    public class EventHelperControllerEvent implements EventHandler
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
            final String[] topics = {ControllerMgr.TOPIC_CONTROLLER_REMOVED};
            props.put(EventConstants.EVENT_TOPIC, topics);
            
            //register the event handler that listens for controllers being removed.
            m_Registration = context.registerService(EventHandler.class, this, props);
        }

        @Override
        public void handleEvent(final Event event)
        {
            final int controllerId = (Integer)event.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID);

            //remove controller mapping to layer types
            m_TransportLayers.remove(controllerId);
            m_LinkLayers.remove(controllerId);
            m_PhysicalLayers.remove(controllerId);            
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
     * 
     * Handles events related to the custom comms namespace. 
     *
     */
    class EventHelperCustomCommsNamespace implements EventHandler
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
            // register to listen to the Asset events
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            
            props.put(EventConstants.EVENT_TOPIC, RemoteConstants.TOPIC_MESSAGE_RECEIVED);
            final String filterString = String.format("(%s=%s)", //NOCHECKSTYLE: multiple string literals, LDAP filter
                RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.CustomComms.toString());
            props.put(EventConstants.EVENT_FILTER, filterString);
            
            //register the event handler that listens for custom comms namespace responses
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
            //pull out event props
            final int systemId = (Integer)event.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID);
            final String messageType = (String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE);
            
            if (messageType.equals(CustomCommsMessageType.GetLayersResponse.toString()))
            {
                eventGetAllLayersResponse((GetLayersResponseData)event.
                        getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), systemId);
                
                final Map<String, Object> props = null; 
                m_EventAdmin.postEvent(new Event(CommsMgr.TOPIC_COMMS_LAYER_UPDATED, props));
            }
            else if (messageType.equals(CustomCommsMessageType.GetLayerNameResponse.toString()))
            {
                processGetLayerNameResponse((GetLayerNameResponseData)event.
                        getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), systemId);
                
                final Map<String, Object> props = null;  
                m_EventAdmin.postEvent(new Event(CommsMgr.TOPIC_COMMS_LAYER_UPDATED, props));
            }
        }        
    }
    
    /**
     * Handles events for the link layer namespace. 
     */
    class EventHelperLinkLayerNamespace implements EventHandler 
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
            // register to listen to the Asset events
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, RemoteConstants.TOPIC_MESSAGE_RECEIVED);
            final String filterString = String.format("(%s=%s)", //NOCHECKSTYLE: multiple string literals, LDAP filter
                RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.LinkLayer.toString());
            props.put(EventConstants.EVENT_FILTER, filterString);
            
            //register the event handler that listens for CustomComms namespace responses
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
            //pull out event props
            final int systemId = (Integer)event.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID);
            final String messageType = (String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE);
            
            //call appropriate method based on response type
            if (messageType.equals(LinkLayerMessageType.GetStatusResponse.toString()))
            {
                eventGetLinkLayerStatusResponse((GetStatusResponseData)event.
                        getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), systemId);
                
                final Map<String, Object> props = null;   
                m_EventAdmin.postEvent(new Event(CommsMgr.TOPIC_COMMS_LAYER_UPDATED, props));
                
            }
            else if (messageType.equals(LinkLayerMessageType.IsActivatedResponse.toString()))
            {
                eventGetLinkLayerActivatedResponse((IsActivatedResponseData)event.
                        getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), systemId);
                
                final Map<String, Object> props = null;  
                m_EventAdmin.postEvent(new Event(CommsMgr.TOPIC_COMMS_LAYER_UPDATED, props));              
            }
        }
    }
    
    /**
     * Event handler for EventAdmin namespace messages received. For this bean, these messages represent
     * remote event notices.
     * @author callen
     */
    public class EventHelperEventAdminNamespace implements EventHandler
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
            
            // register to listen for Event Admin send event messages
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            
            //All event topics of interest
            final String[] topics = {LinkLayer.TOPIC_PREFIX + ALL_STRING, 
                FactoryDescriptor.TOPIC_PREFIX + ALL_STRING};
            props.put(EventConstants.EVENT_TOPIC, topics);
            final String filter = String.format("(%s=*)", RemoteConstants.REMOTE_EVENT_PROP);
            props.put(EventConstants.EVENT_FILTER, filter);
            
            //register the event handler that listens for event admin responses
            m_Registration = context.registerService(EventHandler.class, this, props);
        } 
        
        @Override 
        public void handleEvent(final Event event) 
        {
            //pull out event topic
            final String topic = event.getTopic();
            
            //check that this event is related to the link layer
            if (topic.contains(LinkLayer.TOPIC_PREFIX))
            {
                processRemoteLinkLayerEvent(event);
                final Map<String, Object> props = null;   
                m_EventAdmin.postEvent(new Event(CommsMgr.TOPIC_COMMS_LAYER_UPDATED, props));
            }
            //other wise check if it's a factory event from one of the layers
            else if (topic.contains(FactoryDescriptor.TOPIC_PREFIX))
            {
                final String baseType = (String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE);
            
                if (baseType.equals(PhysicalLink.class.getSimpleName()) 
                        || baseType.equals(LinkLayer.class.getSimpleName()) 
                        || baseType.equals(TransportLayer.class.getSimpleName()))
                {
                    processFactoryEvent(event);
                    final Map<String, Object> props = null;  
                    m_EventAdmin.postEvent(new Event(CommsMgr.TOPIC_COMMS_LAYER_UPDATED, props));
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
     * Response handler for create layer response.
     */
    public class RemoteCreateLinkLayerHandler implements ResponseHandler
    {               
        /**
         * Instance variable used to hold partially built CreateTransportLayer request.
         */
        private final CreateTransportLayerRequestData.Builder m_TransportBuildRequest;
        
        /**
         * Constructor that will take a partially built CreateTransportLayer request and 
         * store it in an instance variable.  Upon receiving the UUID of the link layer
         * to which the TransportLayer request corresponds, the UUID will be added to the request
         * message and sent. 
         * @param transportBuildRequest
         *     partially built CreateTransportLayer request.
         */
        RemoteCreateLinkLayerHandler(final CreateTransportLayerRequestData.Builder transportBuildRequest)
        {
            m_TransportBuildRequest = transportBuildRequest;
        }

        @Override
        public void handleResponse(final TerraHarvestMessage thMessage, final TerraHarvestPayload payload,
                final Message namespaceMessage, final Message dataMessage)
        {

            final CreateLinkLayerResponseData linkResponse = (CreateLinkLayerResponseData) dataMessage;
            final int systemId = thMessage.getSourceId();

            //add link layer UUID to request
            final CreateTransportLayerRequestData request = m_TransportBuildRequest.
                    setLinkLayerUuid(linkResponse.getInfo().getUuid()).build();

            //send request
            m_MessageFactory.createCustomCommsMessage(CustomCommsMessageType.
                    CreateTransportLayerRequest, request).queue(systemId, null);
        }
    }
}
