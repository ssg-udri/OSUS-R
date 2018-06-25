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
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.inject.Inject;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.Asset.AssetActiveStatus;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace
       .AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetAssetsResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace.AssetMessageType;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.GetActiveStatusRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.GetActiveStatusResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.GetLastStatusRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.GetLastStatusResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.GetLastStatusResponseData.StatusObservationCase;
import mil.dod.th.core.remote.proto.AssetMessages.GetNameRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.GetNameResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.PerformBitResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.SetPropertyRequestData;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.core.remote.proto.MapTypes.SimpleTypesMapEntry;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.core.remote.proto.SharedMessages.FactoryObjectInfo;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ConfigurationWrapper;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ModifiablePropertyModel;
import mil.dod.th.ose.gui.webapp.controller.ControllerMgr;
import mil.dod.th.ose.gui.webapp.factory.FactoryBaseModel;
import mil.dod.th.ose.gui.webapp.factory.FactoryObjMgr;
import mil.dod.th.ose.gui.webapp.general.RemoteEventRegistrationHandler;
import mil.dod.th.ose.gui.webapp.remote.RemoteEvents;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.ose.remote.api.EnumConverter;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.lexicon.asset.commands.BaseTypesGen.Command;
import mil.dod.th.remote.lexicon.asset.commands.GetPositionCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.GetPositionResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.SetPositionCommandGen;
import mil.dod.th.remote.lexicon.observation.types.ObservationGen;
import mil.dod.th.remote.lexicon.types.command.CommandTypesGen;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.BankDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.Coordinates;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.ElevationDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.HaeMeters;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.HeadingDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.LatitudeWgsDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.LongitudeWgsDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.Orientation;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;

import org.glassfish.osgicdi.OSGiService;

/**
 * Implementation of the {@link AssetMgr}.
 * @author callen
 *
 */
@ManagedBean(name = "assetMgr") //NOCHECKSTYLE:Fan out complexity, need to be able to handle different message 
@ApplicationScoped           //namespaces. As such need to include to handle logic for all the different messages.
public class AssetMgrImpl implements AssetMgr, FactoryObjMgr //NOPMD:Avoid really long classes. 
                                              // There are five inner classes that 
{                                             // listen to remote events concerning remote assets.
    /**
     * Map that contains the system id and a corresponding set of {@link AssetModel}s that represent assets on the
     * controller that the system id is associated with.
     */
    private final Map<Integer, Set<AssetModel>> m_Assets;

    /**
     * Remote event response handler that keeps track of remote send event registration IDs.
     */
    private RemoteEventRegistrationHandler m_RemoteHandler;
    
    /**
     * Event handler helper class. Listens for EventAdmin namespace messages.
     */
    private EventHelperEventAdminNamespace m_EventHelperEvent;
    
    /**
     * Event handler helper class. Listens for Asset namespace messages.
     */
    private EventHelperAssetNamespace m_EventHelperAsset;
    
    /**
     * Event handler helper class. Listens for AssetDirectory namespace messages.
     */
    private EventHelperAssetDirNamespace m_EventHelperAssetDir;
    
    /**
     * Event handler helper class. Listens for the controller has been removed events.
     */
    private EventHelperControllerEvent m_ControllerEventListener;

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
     * Growl message utility for creating growl messages.
     */
    @Inject
    private GrowlMessageUtil m_GrowlMessageUtil;
    
    /**
     * Service that retrieves the bundle context of this bundle.
     */
    @Inject
    private BundleContextUtil m_BundleUtil;
    
    /**
     * The image display interface to use.
     */
    @Inject
    private AssetImage m_AssetImageInterface;
    
    /**
     * The {@link JaxbProtoObjectConverter} responsible for converting between JAXB and protocol buffer objects. 
     */
    @Inject @OSGiService
    private JaxbProtoObjectConverter m_Converter;
    
    /**
     * Reference to the configuration wrapper bean.
     */
    @ManagedProperty(value = "#{configWrapper}")
    private ConfigurationWrapper configWrapper; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Reference to the asset types manager.
     */
    @ManagedProperty(value = "#{assetTypesMgr}")
    private AssetTypesMgr assetTypesMgr;  //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Asset manager's constructor.
     */
    public AssetMgrImpl()
    {
        super();
        m_Assets = Collections.synchronizedMap(new HashMap<Integer, Set<AssetModel>>());
    }
    
    /**
     * Register event listener.
     */
    @PostConstruct
    public void registerEventHelpers()
    {
        m_RemoteHandler = new RemoteEventRegistrationHandler(m_MessageFactory);

        //instantiate handlers
        m_EventHelperEvent = new EventHelperEventAdminNamespace();
        m_EventHelperAsset = new EventHelperAssetNamespace();
        m_EventHelperAssetDir = new EventHelperAssetDirNamespace();
        m_ControllerEventListener = new EventHelperControllerEvent();
        
        //register to listen to delegated events
        m_EventHelperEvent.registerForEvents();
        m_EventHelperAsset.registerForEvents();
        m_EventHelperAssetDir.registerForEvents();
        m_ControllerEventListener.registerControllerEvents();
    }
    
    /**
     * Unregister handlers before destruction of the bean.
     */
    @PreDestroy
    public void unregisterHelpers()
    {
        //Unregister for events
        m_EventHelperEvent.unregisterListener();
        m_EventHelperAsset.unregisterListener();
        m_EventHelperAssetDir.unregisterListener();
        m_RemoteHandler.unregisterRegistrations();
        m_ControllerEventListener.unregisterListener();
    }
    
    /**
     * Set the {@link ConfigurationWrapper} service.
     * @param config
     *      {@link ConfigurationWrapper} service to set
     */
    public void setConfigWrapper(final ConfigurationWrapper config)
    {
        configWrapper = config;
    }
    
    /**
     * Set the {@link AssetTypesMgr} service.
     * @param assetMgr
     *      {@link AssetTypesMgr} service to set
     */
    public void setAssetTypesMgr(final AssetTypesMgr assetMgr)
    {
        assetTypesMgr = assetMgr;
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
     * Set the {@link BundleContextUtil} utility service.
     * @param bundleUtil
     *     the bundle context utility service to use
     */
    public void setBundleContextUtil(final BundleContextUtil bundleUtil)
    {
        m_BundleUtil = bundleUtil;
    }
    
    /**
     * Set the {@link AssetImage} service.
     * @param imgInterface
     *  the asset image display interface.
     */
    public void setAssetImageInterface(final AssetImage imgInterface)
    {
        m_AssetImageInterface = imgInterface;
    }
    
    /**
     * Set the {@link JaxbProtoObjectConverter}.
     * 
     * @param converter
     *     the converter responsible for converting between JAXB and protocol buffer objects.
     */
    public void setConverter(final JaxbProtoObjectConverter converter)
    {
        m_Converter = converter;        
    }
    
    /**
     * Get the position of the asset and sync it to the asset model.
     * @param controllerId
     *      controller id that the asset is on
     * @param asset
     *      asset to get the position of
     */
    public void syncPosition(final int controllerId, final AssetModel asset)
    {
        final AssetExecuteErrorHandler handler = new AssetExecuteErrorHandler(asset.getName());
        
        final String sensorId = asset.getSensorId();
        final Command.Builder base = Command.newBuilder();
        if (sensorId != null && !sensorId.isEmpty())
        {
            sensorIdUpdate(asset);
            base.setSensorId(sensorId);
        }

        final GetPositionCommandGen.GetPositionCommand getPositionProto = GetPositionCommandGen.
                GetPositionCommand.newBuilder().setBase(base).build();
        
        final ExecuteCommandRequestData executeCommandRequest = ExecuteCommandRequestData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(asset.getUuid())).
                setCommand(getPositionProto.toByteString()).
                setCommandType(CommandTypesGen.CommandType.Enum.GET_POSITION_COMMAND).
                build();
        
        m_MessageFactory.createAssetMessage(AssetMessageType.ExecuteCommandRequest, executeCommandRequest).
            queue(controllerId, handler);
    }
    
    /**
     * Update the location of an asset, get all the various location objects from the asset model.
     * @param controllerId
     *      id of the controller the asset is on
     * @param asset
     *      the asset to update the location
     */
    public void locationUpdate(final int controllerId, final AssetModel asset)
    {
        final AssetExecuteErrorHandler handler = new AssetExecuteErrorHandler(asset.getName());
        
        //Create new coordinates objects
        final LongitudeWgsDegrees newLongitude = LongitudeWgsDegrees.newBuilder().
                setValue(Double.valueOf(asset.getLocation().getLongitude().toString())).
                build();
        
        final LatitudeWgsDegrees newLatitude = LatitudeWgsDegrees.newBuilder().
                setValue(Double.valueOf(asset.getLocation().getLatitude().toString())).
                build();
        
        final HaeMeters newHaeMeters = HaeMeters.newBuilder().
                setValue(Double.valueOf(asset.getLocation().getAltitude().toString())).
                build();
        
        final Coordinates newLocation = Coordinates.newBuilder().
                setAltitude(newHaeMeters).
                setLatitude(newLatitude).
                setLongitude(newLongitude).
                build();
        
        //Create new observation object
        final BankDegrees newBank = BankDegrees.newBuilder().
                setValue(Double.valueOf(asset.getLocation().getBank().toString())).
                build();
        
        final ElevationDegrees newElevation = ElevationDegrees.newBuilder().
                setValue(Double.valueOf(asset.getLocation().getElevation().toString())).
                build();
        
        final HeadingDegrees newHeading = HeadingDegrees.newBuilder().
                setValue(Double.valueOf(asset.getLocation().getHeading().toString())).
                build();
        
        final Orientation newOrientation = Orientation.newBuilder().
                setBank(newBank).
                setHeading(newHeading).
                setElevation(newElevation).
                build();
        
        //Build the base command required for every command message.
        final Command.Builder baseCommand = Command.newBuilder();

        final String sensorId = asset.getSensorId();
        if (sensorId != null && !sensorId.isEmpty())
        {
            sensorIdUpdate(asset);
            baseCommand.setSensorId(sensorId);
        }

        //Create a new command to set the position with the new orientation and location
        final SetPositionCommandGen.SetPositionCommand setPositionProto = SetPositionCommandGen.
                SetPositionCommand.newBuilder().
                setBase(baseCommand).
                setOrientation(newOrientation).
                setLocation(newLocation).
                build();
        
        final ExecuteCommandRequestData executeCommandRequest = ExecuteCommandRequestData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(asset.getUuid())).
                setCommand(setPositionProto.toByteString()).
                setCommandType(CommandTypesGen.CommandType.Enum.SET_POSITION_COMMAND).
                build();
        
        m_MessageFactory.createAssetMessage(AssetMessageType.ExecuteCommandRequest, executeCommandRequest).
            queue(controllerId, handler);
    }

    /**
     * Save the model's current sensor ID and generate event to send a push message and update in the browser.
     * 
     * @param asset
     *      the asset to update sensor IDs for
     */
    public void sensorIdUpdate(final AssetModel asset)
    {
        asset.updateSensorIds();
        createAssetEvent(TOPIC_ASSET_SENSOR_IDS_UPDATED, asset, new HashMap<String, Object>());
    }

    @Override
    public synchronized List<AssetModel> getAssetsForControllerAsync(final int controllerId)
    {
        //list to return
        final List<AssetModel> models = new ArrayList<AssetModel>();
        
        //check for mapping to the controller ID
        if (!m_Assets.containsKey(controllerId))
        {  
            //new system ID, request assets from that system.
            m_MessageFactory.createAssetDirectoryServiceMessage(AssetDirectoryServiceMessageType.GetAssetsRequest, 
                    null).queue(controllerId, null);

            //send request for asset and object factory events only if the system ID is new
            requestToListenForRemoteEvents(controllerId);
            
            m_Assets.put(controllerId, new HashSet<AssetModel>());
        }
        
        models.addAll(m_Assets.get(controllerId));
        return models;
    }
    
    @Override
    public void createConfiguration(final int systemId,
            final FactoryBaseModel model, final List<ModifiablePropertyModel> properties)
    {
        final SetPropertyRequestData.Builder builder = SetPropertyRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(model.getUuid()));
    
        for (ModifiablePropertyModel prop : properties)
        {
            final Multitype multi = SharedMessageUtils.convertObjectToMultitype(prop.getValue());
            
            final SimpleTypesMapEntry type = SimpleTypesMapEntry.newBuilder().setKey(
                    prop.getKey()).setValue(multi).build();
            
            builder.addProperties(type);
        }
        
        m_MessageFactory.createAssetMessage(AssetMessageType.SetPropertyRequest, builder.build()).
            queue(systemId, null);
    }
    
    /**
     * Try to add asset model.
     * @param asset
     *    the asset model to update/add
     * @return
     *    true if the asset was added, otherwise false
     */
    private synchronized boolean tryAddAssetModel(final AssetModel asset)
    {
        final int systemId = asset.getControllerId();
        final UUID uuid = asset.getUuid();
        final AssetModel model = getAssetModelByUuid(uuid, systemId);
        if (model == null && m_Assets.containsKey(systemId))
        {
            //add to respective system ID
            final Set<AssetModel> contModels = m_Assets.get(systemId);
            contModels.add(asset);
            Logging.log(LogService.LOG_DEBUG, "Asset model added with UUID: [%s] and of type: [%s]", asset.getUuid(), 
                    asset.getType());
            return true;
        }
        return false;
    }
    
    /**
     * Remove an asset model from lookup.
     * @param asset
     *     the asset model to remove
     * @return
     *     boolean value representing if the asset was removed successfully
     */
    private synchronized boolean removeAssetModel(final AssetModel asset)
    {
        //system ID
        final int systemId = asset.getControllerId();
        
        // remove the asset
        if (m_Assets.get(systemId).remove(asset))
        {
            Logging.log(LogService.LOG_DEBUG, "Asset model with name: [%s] and of type: [%s] has been removed", 
                    asset.getName(), asset.getType());
            return true;
        }
        return false;
    }
    
    @Override
    public synchronized AssetModel getAssetModelByUuid(final UUID uuid, final int systemId)
    {
        final List<AssetModel> models = retrieveAssetModelList(systemId);
        
        if (models != null)
        {
            for (AssetModel model : models)
            {
                if (model.getUuid().equals(uuid))
                {
                    return model;
                }
            }
        }
        return null;
    }
    
    /**
     * Method used to retrieve the list of current assets for the specified controller.
     * @param controllerId
     *          ID of the controller to retrieve a list of assets for.
     * @return
     *          List of {@link AssetModel}s.
     */
    private synchronized List<AssetModel> retrieveAssetModelList(final int controllerId)
    {
        final List<AssetModel> models = new ArrayList<AssetModel>();
        
        if (m_Assets.containsKey(controllerId))
        {
            models.addAll(m_Assets.get(controllerId));
        }
       
        return models;
    }
    
    /**
     * Process remote message that contains assets from a single controller.
     * @param assetMessage
     *    the asset message that contains assets from the specified asset
     * @param systemId
     *    the system ID that the message came from
     */
    private synchronized void processGetAssetResponse(final Message assetMessage, final int systemId)
    {
        final GetAssetsResponseData response = (GetAssetsResponseData) assetMessage;
        final List<UUID> convUuids = new ArrayList<UUID>();

        //first add any new assets
        for (FactoryObjectInfo info : response.getAssetInfoList())
        {
            final SharedMessages.UUID protoUUID = info.getUuid();
            final UUID convertedUuid = SharedMessageUtils.convertProtoUUIDtoUUID(protoUUID);
            convUuids.add(convertedUuid);
            final AssetModel model = new AssetModel(systemId, convertedUuid, info.getPid(), info.getProductType(), 
                    this, configWrapper, assetTypesMgr, m_AssetImageInterface);
            
            //request information only if the asset is new
            if (tryAddAssetModel(model))
            {
                //send requests for capabilities etc.
                sendRequestsForNewAssetInformation(protoUUID, systemId);
            }
        }

        //remove any assets that are no longer connected with the particular system
        for (AssetModel oldModel : retrieveAssetModelList(systemId))
        {
            if (!convUuids.contains(oldModel.getUuid()))
            {
                m_Assets.get(systemId).remove(oldModel);
            }
        }
    }
    
    /**
     * Method for handling retrieving information for new assets.
     * @param protoUUID
     *     the proto message typed UUID for the asset
     * @param systemId
     *     the system ID of the location to send requests to
     */
    private void sendRequestsForNewAssetInformation(final SharedMessages.UUID protoUUID, 
        final int systemId)
    {
        //send request for the asset's active status
        final GetActiveStatusRequestData requestActiveStatus = GetActiveStatusRequestData.newBuilder().
            setUuid(protoUUID).build();
        m_MessageFactory.createAssetMessage(AssetMessageType.GetActiveStatusRequest, 
                requestActiveStatus).queue(systemId, null);
        
        //send request for the asset's status
        final GetNameRequestData requestName = GetNameRequestData.newBuilder().
            setUuid(protoUUID).build();
        m_MessageFactory.createAssetMessage(AssetMessageType.GetNameRequest, 
                requestName).queue(systemId, null);
        
        //send request for asset status summary
        final GetLastStatusRequestData requestStatus = GetLastStatusRequestData.newBuilder().setUuid(protoUUID).build();
        m_MessageFactory.createAssetMessage(AssetMessageType.GetLastStatusRequest, requestStatus).queue(systemId, null);
    }
    
    /**
     * Event response handling for the asset namespace's GetStatusResponse.
     * @param message
     *     the message that contains the response information
     * @param systemId
     *     the system ID from which the response message originated
     */
    private synchronized void eventGetStatusResponse(final Message message, final int systemId)
    {
        final GetLastStatusResponseData data = (GetLastStatusResponseData) message;
        
        final UUID uuid = SharedMessageUtils.convertProtoUUIDtoUUID(data.getAssetUuid());
        //Retrieve model
        final AssetModel model = getAssetModelByUuid(uuid, systemId);

        if (model == null)
        {
            Logging.log(LogService.LOG_WARNING, "Status response for unknown asset [%s]", uuid);
            return;
        }
        
        if (data.getStatusObservationCase() != StatusObservationCase.STATUSOBSERVATIONNATIVE)
        {
            Logging.log(LogService.LOG_WARNING, "Asset [%s] status response in unsupported format [%s]", 
                    model.getName(), data.getStatusObservationCase());
            return;
        }
        
        try
        {
            convertApplyStatus(model, data.getStatusObservationNative());
        }
        catch (final ObjectConverterException exception)
        {
            Logging.log(LogService.LOG_ERROR, exception, 
                    "An error occurred trying to parse the status for asset with UUID [%s]" 
                    + " from system id 0x%08x ", model.getUuid().toString(), systemId);
            return;
        }
        
        postSummaryStatusUpdate(model);
    }
    
    /**
     * Event response handling for the asset namespace's GetStatusResponse.
     * @param message
     *     the message that contains the response information
     * @param systemId
     *     the system ID from which the response message originated
     * @throws InvalidProtocolBufferException
     *      proto was invalid
     */
    private synchronized void eventExecuteCommandResponse(final Message message, final int systemId) throws 
            InvalidProtocolBufferException
    {
        final ExecuteCommandResponseData commandData = (ExecuteCommandResponseData) message;
        
        final AssetModel model = getAssetModelByUuid(SharedMessageUtils.convertProtoUUIDtoUUID(
                commandData.getUuid()), systemId);
        
        if (commandData.getResponseType() == CommandTypesGen.CommandResponse.Enum.GET_POSITION_RESPONSE)
        {
            final GetPositionResponseGen.GetPositionResponse data = GetPositionResponseGen.GetPositionResponse.
                    parseFrom(commandData.getResponse());

            if (model == null)
            {
                return;
            }

            if (data.hasLocation())
            {
                model.setCoordinates(data.getLocation());
            }
            else
            {
                model.setCoordinates(null);
            }

            if (data.hasOrientation())
            {
                model.setOrientation(data.getOrientation());
            }
            else
            {
                model.setOrientation(null);
            }
            
            createAssetEvent(TOPIC_ASSET_LOCATION_UPDATED, model, new HashMap<String, Object>());
        }
    }
    
    /**
     * Event response handling for the asset namespace's PerformBITResponse.
     * @param message
     *     the message that contains the response information
     * @param systemId
     *     the system ID from which the response originated
     */
    private synchronized void eventPerformBITResponse(final Message message, final int systemId)
    {
        final PerformBitResponseData data = (PerformBitResponseData) message;

        final UUID uuid = SharedMessageUtils.convertProtoUUIDtoUUID(data.getAssetUuid());
        //Retrieve model
        final AssetModel model = getAssetModelByUuid(uuid, systemId);

        if (model == null)
        {
            Logging.log(LogService.LOG_WARNING, "Perform BIT response for unknown asset [%s]", uuid);
            return;
        }
        
        if (data.getStatusObservationCase() != PerformBitResponseData.StatusObservationCase.STATUSOBSERVATIONNATIVE)
        {
            Logging.log(LogService.LOG_WARNING, "Asset [%s] perform BIT response in unsupported format [%s]", 
                    model.getName(), data.getStatusObservationCase());
            return;
        }
    
        //set new value
        try
        {
            convertApplyStatus(model, data.getStatusObservationNative());
        }
        catch (final ObjectConverterException exception)
        {
            Logging.log(LogService.LOG_ERROR, exception, 
                    "An error occurred while trying to parse the post-built-in-test status of asset with UUID [%s]"
                    + " from system id 0x%08x.", model.getUuid().toString(), systemId);
            return;
        }

        //post notice
        m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Asset completed BIT test.", 
            String.format("Asset %s from controller 0x%08x, updated status to %s.", model.getName(), systemId,
                model.getSummaryStatus()));
        
        postSummaryStatusUpdate(model);
    }
        
    /**
     * Event response handling for the asset directory namespace's GetAssetStatusResponse.
     * @param message
     *     the message that contains the response information
     * @param systemId
     *     the system ID from which the response message originated
     */
    private synchronized void eventGetActiveStatusResponse(final GetActiveStatusResponseData message, 
            final int systemId)
    {
        //Retrieve model
        final AssetModel model = getAssetModelByUuid(SharedMessageUtils.
            convertProtoUUIDtoUUID(message.getUuid()), systemId);
        
        final AssetActiveStatus status = EnumConverter.convertProtoAssetActiveStatusToJava(message.getStatus());
        //set received values
        model.setActiveStatus(status);
        postAssetActiveStatusUpdate(model);
    }
    
    /**
     * Process the get asset name response message.
     * @param message
     *     the message that contains the name property value
     * @param systemId
     *     the system ID from which the response message originated
     */
    public void eventGetNameResponse(final GetNameResponseData message, final int systemId)
    {
        //Retrieve model
        final AssetModel model = getAssetModelByUuid(SharedMessageUtils.
            convertProtoUUIDtoUUID(message.getUuid()), systemId);
        
        //update the name of the asset
        model.updateName(message.getAssetName());
        Logging.log(LogService.LOG_DEBUG, "Received name: [%s] for asest with UUID: [%s]", model.getName(), 
                model.getUuid());
        createAssetEvent(TOPIC_ASSET_UPDATED, model, new HashMap<String, Object>());
    }
    
    /**
     * Process factory object event.
     * @param event
     *     the event properties to check
     * @param topic
     *     the event's topic
     * @param systemId
     *     the system ID from which the response message originated
     */
    private void processFactoryEvent(final Event event, final String topic, final int systemId)
    {
        //pull out UUID and asset type
        final UUID uuid = UUID.fromString((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_UUID));
        final String assetType = (String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_TYPE);
        
        //get the model to update
        final AssetModel model = getAssetModelByUuid(uuid, systemId);

        //not all actions require the model
        if (topic.equals(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED + RemoteConstants.REMOTE_TOPIC_SUFFIX))
        {
            //pull out PID
            final String pid = (String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_PID);
            final AssetModel newModel = new AssetModel(systemId, uuid, pid, assetType, this, configWrapper,
                    assetTypesMgr, m_AssetImageInterface);
            tryAddAssetModel(newModel);
            
            //send request for information
            sendRequestsForNewAssetInformation(SharedMessageUtils.convertUUIDToProtoUUID(uuid), systemId);
            
            m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Asset created.",
                String.format("An asset of type %s, was created for controller 0x%08x", 
                    event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_TYPE), systemId));
            
            createAssetEvent(TOPIC_ASSET_ADDED, newModel, new HashMap<String, Object>());
        }
        //all further actions require that the model is not null
        else if (model == null)
        {
            //nothing to update if the model isn't known to the system
            return;
        }

        //process other types of events
        if (topic.equals(FactoryDescriptor.TOPIC_FACTORY_OBJ_DELETED + RemoteConstants.REMOTE_TOPIC_SUFFIX))
        {
            if (removeAssetModel(model))
            {
                m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Asset removed.",
                    String.format("An asset from controller 0x%08x was removed.", systemId));
                
                createAssetEvent(TOPIC_ASSET_REMOVED, model, new HashMap<String, Object>());
            }
        }
        else if (topic.equals(FactoryDescriptor.TOPIC_FACTORY_OBJ_PID_CREATED + RemoteConstants.REMOTE_TOPIC_SUFFIX))
        {
            final String pid = (String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_PID);
            
            model.setPid(pid);
        }
        else if (topic.equals(FactoryDescriptor.TOPIC_FACTORY_OBJ_PID_REMOVED + RemoteConstants.REMOTE_TOPIC_SUFFIX))
        {
            model.setPid("");
        }
        else if (topic.equals(FactoryDescriptor.TOPIC_FACTORY_OBJ_NAME_UPDATED + RemoteConstants.REMOTE_TOPIC_SUFFIX))
        {
            final String name = (String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_NAME);
            if (name == null)
            {
                //null isn't a valid name value
                return;
            }
            m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Asset updated.",
                String.format("Asset formerly named %s changed to %s, from controller 0x%08x.", model.getName(),
                    name, systemId));
            //update the name
            model.updateName(name);
            createAssetEvent(TOPIC_ASSET_UPDATED, model, new HashMap<String, Object>());
        }
    }
    
    /**
     * Process an Asset namespace event.
     * @param event
     *      the event to process data from
     * @param topic
     *      the topic of the event
     * @param systemId
     *     the system ID from which the response message originated
     * @param model
     *      the model to update
     */
    private void processAssetNamespaceEvent(final Event event, final String topic, final int systemId, 
            final AssetModel model)
    {
        switch (topic)
        {
            case Asset.TOPIC_DATA_CAPTURED + RemoteConstants.REMOTE_TOPIC_SUFFIX:
            {
                createAssetEvent(TOPIC_ASSET_OBSERVATION_UPDATED, model, new HashMap<String, Object>());
                //capture data requests do not require an observation is sent
                if (event.containsProperty(Asset.EVENT_PROP_ASSET_OBSERVATION_UUID))
                {
                    //post notice
                    m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Asset captured data!", 
                        String.format("An asset from controller 0x%08x captured data. The observation UUID is %s.", 
                            systemId, ((UUID)event.getProperty(Asset.EVENT_PROP_ASSET_OBSERVATION_UUID)).toString()));
                }
                break;
            }
            case Asset.TOPIC_STATUS_CHANGED + RemoteConstants.REMOTE_TOPIC_SUFFIX:
            {
                final SummaryStatusEnum assetSum = 
                        SummaryStatusEnum.valueOf((String)event.getProperty(Asset.EVENT_PROP_ASSET_STATUS_SUMMARY));
                final String summaryDescription = (String)event.getProperty(
                        Asset.EVENT_PROP_ASSET_STATUS_DESCRIPTION);
                
                model.setSummary(assetSum);
                model.setSummaryDescription(summaryDescription);

                postSummaryStatusUpdate(model);
                break;
            }
            default:
            {
                processAssetActivatonEvents(topic, model);
            }
        }
    }
    
    /**
     * Process asset activation events.
     * @param topic
     *      the topic of the event
     * @param model
     *      the model to update
     */
    private void processAssetActivatonEvents(final String topic, final AssetModel model)
    {
        boolean doPost = true;

        //process events that require a model
        switch (topic)
        {
            case Asset.TOPIC_ACTIVATION_COMPLETE + RemoteConstants.REMOTE_TOPIC_SUFFIX:
            {
                model.setActiveStatus(AssetActiveStatus.ACTIVATED);
                break;
            }
            case Asset.TOPIC_DEACTIVATION_COMPLETE + RemoteConstants.REMOTE_TOPIC_SUFFIX:
            {
                model.setActiveStatus(AssetActiveStatus.DEACTIVATED);
                break;
            }
            case Asset.TOPIC_WILL_BE_ACTIVATED + RemoteConstants.REMOTE_TOPIC_SUFFIX:
            {
                model.setActiveStatus(AssetActiveStatus.ACTIVATING);
                break;
            }
            case Asset.TOPIC_WILL_BE_DEACTIVATED + RemoteConstants.REMOTE_TOPIC_SUFFIX:
            {
                model.setActiveStatus(AssetActiveStatus.DEACTIVATING);
                break;
            }
            case Asset.TOPIC_ACTIVATION_FAILED + RemoteConstants.REMOTE_TOPIC_SUFFIX:
            {
                model.setActiveStatus(AssetActiveStatus.DEACTIVATED);
                break;
            }
            case Asset.TOPIC_DEACTIVATION_FAILED + RemoteConstants.REMOTE_TOPIC_SUFFIX:
            {
                model.setActiveStatus(AssetActiveStatus.ACTIVATED);
                break;
            }
            default:
            {
                //different topic which is not presently handled by this bean
                doPost = false;
            }
        }

        if (doPost)
        {
            postAssetActiveStatusUpdate(model);
        }
    }
    
    /**
     * Create an asset activation status event and fill out the correct event properties.
     * @param model
     *  the model that the asset active status update was for and is used to retrieve the 
     *  asset's active status
     */
    private void postAssetActiveStatusUpdate(final AssetModel model)
    {
        final Map<String, Object> props = new HashMap<>();
        props.put(EVENT_PROP_ACTIVE_STATUS_SUMMARY, model.getActiveStatus().toString());
        
        createAssetEvent(TOPIC_ASSET_ACTIVATION_STATUS_UPDATED, model, props);
    }
    
    /**
     * Create an asset status event and fill out the correct event properties. 
     * @param model
     *  the model that the status update was for and is used to retrieve the 
     *  summary and the current status
     */
    private void postSummaryStatusUpdate(final AssetModel model)
    {
        final Map<String, Object> props = new HashMap<>();
        props.put(Asset.EVENT_PROP_ASSET_STATUS_SUMMARY, model.getSummaryStatus().toString());
        props.put(Asset.EVENT_PROP_ASSET_STATUS_DESCRIPTION, model.getSummaryDescription());
        
        createAssetEvent(TOPIC_ASSET_STATUS_UPDATED, model, props);
    }
    
    /**
     * Create an asset event using the specified topic.
     * @param assetTopic
     *  the topic of the event to create
     * @param model
     *  the asset model that the event is being created for
     * @param props
     *  the event properties that are to be associated with the topic
     */
    private void createAssetEvent(final String assetTopic, final AssetModel model, final Map<String, Object> props)
    {
        //Build the asset event.
        props.put(AssetModel.EVENT_PROP_UUID, model.getUuid().toString());
        final Event assetEvent = new Event(assetTopic, props);
        //Post the asset event
        m_EventAdmin.postEvent(assetEvent);
    }

    /**
     * Request to get asset events.
     * @param systemId
     *     the system to send the request to
     */
    public synchronized void requestToListenForRemoteEvents(final int systemId)
    {
        //List of topics
        final List<String> topics = new ArrayList<String>();
        topics.add("mil/dod/th/core/asset/*"); 
        topics.add("mil/dod/th/core/factory/FactoryDescriptor/*");
        topics.add(ObservationStore.TOPIC_OBSERVATION_MERGED_WITH_OBS);
        topics.add(ObservationStore.TOPIC_OBSERVATION_PERSISTED_WITH_OBS);
        
        //send request
        RemoteEvents.sendEventRegistration(m_MessageFactory, topics, null, true, systemId, m_RemoteHandler);
    }

    /**
     * Convert a {@link mil.dod.th.remote.lexicon.observation.types.StatusGen.Status} proto object to a 
     * {@link Status}.
     * @param model
     *      the model to apply the status to
     * @param protoStatus
     *      the status to be converted and applied to the model
     * @return
     *      the model passed with the status applied
     * @throws ObjectConverterException 
     *      if the status object passed cannot be converted
     */
    private AssetModel convertApplyStatus(final AssetModel model, final ObservationGen.Observation protoStatus)
            throws ObjectConverterException
    {
        final Observation obs = (Observation)m_Converter.convertToJaxb(protoStatus);

        //get the status
        final Status status = obs.getStatus();
        // set new value
        final SummaryStatusEnum sumStat = status.getSummaryStatus().getSummary();
        final String description = status.getSummaryStatus().getDescription();
        model.setSummary(sumStat);
        model.setSummaryDescription(description);
        return model;
    }
    
    /**
     * Create a remote asset handler which will send a request to change the asset's name to the desired 
     * value once the create asset response is received.
     *
     * @return
     *     the response handler created
     */
    public RemoteCreateAssetHandler createRemoteAssetHandler()
    {
        return new RemoteCreateAssetHandler();
    }
    
    /**
     * Create a remote asset name handler which update the user if the name change fails.
     * @param uuid
     *     the uuid of the asset
     * @return
     *     the response handler created
     */
    public RemoteSetAssetNameHandler createRemoteAssetNameHandler(final UUID uuid)
    {
        return new RemoteSetAssetNameHandler(uuid);
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Below are event handlers which handle processing particular event based on the event's namespace.         //
    //                                                                                                           //
    //                                                                                                           //
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
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
            final String all = "*";
            final BundleContext context = m_BundleUtil.getBundleContext();
            // register to listen for Event Admin send event messages
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            //All event topics of interest
            final String[] topics = 
            {
                Asset.TOPIC_DATA_CAPTURED + RemoteConstants.REMOTE_TOPIC_SUFFIX,
                Asset.TOPIC_STATUS_CHANGED + RemoteConstants.REMOTE_TOPIC_SUFFIX,
                Asset.TOPIC_WILL_BE_ACTIVATED + RemoteConstants.REMOTE_TOPIC_SUFFIX,
                Asset.TOPIC_ACTIVATION_COMPLETE + RemoteConstants.REMOTE_TOPIC_SUFFIX,
                Asset.TOPIC_ACTIVATION_FAILED + RemoteConstants.REMOTE_TOPIC_SUFFIX,
                Asset.TOPIC_WILL_BE_DEACTIVATED + RemoteConstants.REMOTE_TOPIC_SUFFIX,
                Asset.TOPIC_DEACTIVATION_COMPLETE + RemoteConstants.REMOTE_TOPIC_SUFFIX,
                Asset.TOPIC_DEACTIVATION_FAILED + RemoteConstants.REMOTE_TOPIC_SUFFIX,
                FactoryDescriptor.TOPIC_PREFIX + all
            };
            props.put(EventConstants.EVENT_TOPIC, topics);
            final String filter = String.format("(%s=*)", RemoteConstants.REMOTE_EVENT_PROP);
            props.put(EventConstants.EVENT_FILTER, filter);
            
            //register the event handler that listens for event admin responses
            m_Registration = context.registerService(EventHandler.class, this, props);
        }
        
        @Override 
        public void handleEvent(final Event event) 
        {
            //pull out event props
            final int systemId = (Integer)event.getProperty(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID);
            
            final String topic = event.getTopic();
            
            //check that this event is asset related
            if (topic.contains(Asset.TOPIC_PREFIX) || topic.contains(AssetDirectoryService.TOPIC_PREFIX))
            {
                //get the object UUID as a string
                final String uuid = (String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_UUID);
                
                if (uuid == null)
                {
                    //asset and asset dir updates require a model, and therefore the UUID
                    return;
                }
                
                final AssetModel model = getAssetModelByUuid(UUID.fromString(uuid), systemId);
                
                if (model == null)
                {
                    //asset and asset dir updates require a model                    
                    m_GrowlMessageUtil.createLocalFacesMessage(FacesMessage.SEVERITY_INFO, 
                        "Unknown Asset Data:", 
                        String.format("No Asset is known with UUID %s", uuid));
                    return;
                }
                
                processAssetNamespaceEvent(event, topic, systemId, model);
            }
            else if (topic.contains(FactoryDescriptor.TOPIC_PREFIX)
                    && ((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE)).
                        contains(Asset.class.getSimpleName()))
            {
                processFactoryEvent(event, topic, systemId);
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
     * Event handler for Asset namespace messages received.
     * @author callen
     */
    public class EventHelperAssetNamespace implements EventHandler
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
                RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Asset.toString());
            props.put(EventConstants.EVENT_FILTER, filterString);
            
            //register the event handler that listens for asset namespace responses
            m_Registration = context.registerService(EventHandler.class, this, props);
        } 
        
        @Override 
        public void handleEvent(final Event event) 
        {
            //pull out event props
            final int systemId = (Integer)event.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID);
            final String messageType = (String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE);
            
            if (messageType.equals(AssetMessageType.GetLastStatusResponse.toString()))
            {
                eventGetStatusResponse((GetLastStatusResponseData)event.
                    getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), systemId);
            }
            else if (messageType.equals(AssetMessageType.PerformBitResponse.toString()))
            {
                eventPerformBITResponse((PerformBitResponseData)event.
                    getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), systemId);
            }
            else if (messageType.equals(AssetMessageType.SetPropertyResponse.toString()))
            {
                m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, 
                        "Properties Accepted:", String.format("Controller 0x%08x has " 
                                + "accepted the list of properties.", systemId));
            }
            else if (messageType.equals(AssetMessageType.ExecuteCommandResponse.toString()))
            {
                try
                {
                    eventExecuteCommandResponse((ExecuteCommandResponseData)event.getProperty(
                            RemoteConstants.EVENT_PROP_DATA_MESSAGE), systemId);
                }
                catch (final InvalidProtocolBufferException e)
                {
                    //post notice
                    m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, 
                            "Could not parse execute command response!", e.getMessage());
                    return;
                }
            }
            else if (messageType.equals(AssetMessageType.GetActiveStatusResponse.toString()))
            {
                eventGetActiveStatusResponse((GetActiveStatusResponseData)event.
                    getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), systemId);
            }
            else if (messageType.equals(AssetMessageType.GetNameResponse.toString()))
            {
                eventGetNameResponse(
                     (GetNameResponseData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), systemId);
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
     * Event handler for AssetDirectory namespace messages received.
     * @author callen
     */
    public class EventHelperAssetDirNamespace implements EventHandler
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
            props.put(EventConstants.EVENT_TOPIC, RemoteConstants.TOPIC_MESSAGE_RECEIVED);
            final String filterString = String.format("(%s=%s)", 
                RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.AssetDirectoryService.toString());
            props.put(EventConstants.EVENT_FILTER, filterString);
            
            //register the event handler that listens for asset directory responses
            m_Registration = context.registerService(EventHandler.class, this, props);
        } 
        
        @Override
        public void handleEvent(final Event event)
        {
            //pull out event props
            final int systemId = (Integer)event.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID);
            final String eventType = (String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE);
            
            if (eventType.equals(AssetDirectoryServiceMessageType.GetAssetsResponse.toString()))
            {
                processGetAssetResponse((GetAssetsResponseData)event.
                    getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),  systemId);
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

            //remove controller mapping to assets
            m_Assets.remove(controllerId);
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
     * Response handler for create asset response.
     */
    public class RemoteCreateAssetHandler implements ResponseHandler
    {
        @Override
        public void handleResponse(final TerraHarvestMessage thMessage, final TerraHarvestPayload payload,
            final Message namespaceMessage, 
            final Message dataMessage)
        {
            if (dataMessage instanceof GenericErrorResponseData)
            {
                // post notice
                final GenericErrorResponseData errorResponse = (GenericErrorResponseData)dataMessage;
                m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Create Asset Error!",
                        errorResponse.getErrorDescription());
                return;
            }

            final CreateAssetResponseData assetResponse = (CreateAssetResponseData) dataMessage;
            final int systemId = thMessage.getSourceId();
            final FactoryObjectInfo info = assetResponse.getInfo();

            //create asset model
            final AssetModel model = new AssetModel(systemId, 
                SharedMessageUtils.convertProtoUUIDtoUUID(info.getUuid()), info.getPid(), info.getProductType(), 
                AssetMgrImpl.this, configWrapper, assetTypesMgr, m_AssetImageInterface);
            
            //add model
            tryAddAssetModel(model);
        }
    }
    
    /**
     * Response handler for create asset response.
     */
    public class RemoteSetAssetNameHandler implements ResponseHandler
    {
        /**
         * The asset's UUID used for notification purposes.
         */
        private final UUID m_AssetUUID;
        
        /**
         * Constructor that will take the UUID of the asset to update the name for and post a notification if the
         * the attempt at updating the name fails.
         * @param assetUUID
         *     the asset's uuid
         */
        RemoteSetAssetNameHandler(final UUID assetUUID)
        {
            m_AssetUUID = assetUUID;
        }

        @Override
        public void handleResponse(final TerraHarvestMessage thMessage, final TerraHarvestPayload payload, 
            final Message namespaceMessage, final Message dataMessage)
        {
            if (payload.getNamespace() == Namespace.Base)
            {
                final String summary = "Asset name change failed";
                
                final BaseNamespace baseMessage = (BaseNamespace)namespaceMessage;
                
                //check the base namespace message type
                if (baseMessage.getType() == BaseMessageType.GenericErrorResponse)
                {
                    final GenericErrorResponseData data = (GenericErrorResponseData)dataMessage;
                    
                    m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, summary, String.format(
                            "Asset with UUID %s from controller 0x%08x failed to update its name, because: %s - %s", 
                                m_AssetUUID.toString(), thMessage.getSourceId(), data.getError(), 
                                    data.getErrorDescription()));
                }
            }
        }
    }
    
    /**
     * Class used for handling and displaying errors that occur when performing actions on assets.
     * @author matt
     *
     */
    public class AssetExecuteErrorHandler implements ResponseHandler
    {
        /**
         * Name of the asset that this error handler has been setup for.
         */
        private final String m_AssetName;
        
        
        /**
         * Constructor to set the name of the asset.
         * @param assetName
         *      name of the asset to set
         */
        public AssetExecuteErrorHandler(final String assetName)
        {
            m_AssetName = assetName;
        }
        
        @Override
        public void handleResponse(final TerraHarvestMessage message, final TerraHarvestPayload payload,
                final Message namespaceMessage, final Message dataMessage)
        {
            if (payload.getNamespace() == Namespace.Base)
            {
                final String summary = "Asset Error Occurred";
                
                //parse the namespace message
                final BaseNamespace baseMessage = (BaseNamespace)namespaceMessage;
                
                //check the base namespace message type
                if (baseMessage.getType() == BaseMessageType.GenericErrorResponse)
                {
                  //parse generic error message
                    final GenericErrorResponseData data = (GenericErrorResponseData)dataMessage;
                    
                    //post notice
                    m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, summary, String.format(
                            "Performing action ExecuteCommand for asset %s has resulted in the following error %n "
                            + "%s - %s", m_AssetName, data.getError(), data.getErrorDescription()));
                }
            }
        }
    }
}
