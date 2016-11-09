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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.inject.Inject;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import mil.dod.th.core.asset.capability.CommandCapabilities;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.AssetMessages;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace.AssetMessageType;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandResponseData;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.types.command.CommandResponseEnum;
import mil.dod.th.core.types.command.CommandTypeEnum;
import mil.dod.th.ose.gui.webapp.utils.AssetCommandUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.ose.gui.webapp.utils.ReflectionsUtil;
import mil.dod.th.ose.gui.webapp.utils.ReflectionsUtilException;
import mil.dod.th.ose.remote.api.CommandConverter;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.converter.CommandResponseEnumConverter;
import mil.dod.th.remote.converter.CommandTypeEnumConverter;

import org.glassfish.osgicdi.OSGiService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * Session scoped bean responsible for storing asset command models and sending and receiving 
 * sync commands.
 * 
 * @author cweisenborn
 */
@ManagedBean(name = "assetCommandMgr") //NOCHECKSTYLE: Fan-out Complexity.  Due to relationship with event 
@SessionScoped                         // and remote handling.
public class AssetCommandMgrImpl implements AssetCommandMgr
{
    
    /**
     * String reference to the suffix for command object class names.
     */
    private static final String COMMAND_OBJECT_SUFFIX = "Command";
    
    /**
     * String reference to the suffix for response object class names.
     */
    private static final String RESPONSE_OBJECT_SUFFIX = "Response";
    
    /**
     * String reference to the title used for growl messages posted by this class.
     */
    private static final String EXCEPTION_TITLE = "Asset Command Error:";
    
    /**
     * Map that is used to store the {@link AssetSyncableCommandModel}s 
     * that represent commands supported by assets. The key is
     * an integer which represents the controller ID where the asset is contained. The value is another map with a key
     * that is the UUID of the asset and the value is the 
     * {@link AssetSyncableCommandModel} that represents the supported 
     * commands for the asset.
     */
    private final Map<Integer, Map<UUID, AssetSyncableCommandModel>> m_AssetCommands = 
            Collections.synchronizedMap(new HashMap<Integer, Map<UUID, AssetSyncableCommandModel>>());
    
    /**
     * Reference to the {@link AssetTypesMgr} bean.
     */
    @ManagedProperty(value = "#{assetTypesMgr}")
    private AssetTypesMgr assetTypesMgr; //NOCHECKSTYLE must match exactly with the bean name.
    
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
     * Reference to service which provides utilities to aid in command processing
     * operations.
     */
    @Inject @OSGiService
    private CommandConverter m_CommandConverterService;

    /**
     * Reference to the growl message utility.
     */
    @Inject
    private GrowlMessageUtil m_GrowlUtil;
    
    /**
     * Reference to the command response handler to be used.
     */
    private SyncCommandResponseHandler m_SyncResponseHandler;
    
        
    /**
     * Post construct method that instantiates the required response handler and data converters.
     */
    @PostConstruct
    public void postConstruct()
    {
        m_SyncResponseHandler = new SyncCommandResponseHandler();
    }
    
    /**
     * Method that sets the asset types manager to be used.
     * 
     * @param assetTypeMgr
     *          {@link AssetTypesMgr} to be set.
     */
    public void setAssetTypesMgr(final AssetTypesMgr assetTypeMgr)
    {
        this.assetTypesMgr = assetTypeMgr;
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
     * Method that sets the event admin service to be used.
     * 
     * @param eventAdmin
     *          The event admin service to be set.
     */
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Method that sets the command converter service to be used.
     * @param converter
     *          the command converter service to use
     */
    public void setCommandConverter(final CommandConverter converter)
    {
        m_CommandConverterService = converter;
    }
    
    /**
     * Method that sets the growl message utility to use.
     * 
     * @param growlUtil
     *          The growl message utility to be used.
     */
    public void setGrowlMessageUtil(final GrowlMessageUtil growlUtil)
    {
        m_GrowlUtil = growlUtil;
    }
    
    @Override
    public synchronized AssetSyncableCommandModel getAssetCommands(final AssetModel model)
    {
        final int controllerId = model.getControllerId();
        final UUID uuid = model.getUuid();
        final CommandCapabilities capabilities = assetTypesMgr.getAssetFactoryForClassAsync(controllerId, 
                model.getType()).getFactoryCaps().getCommandCapabilities();
        if (!m_AssetCommands.containsKey(controllerId))  //NOPMD: Confusing Ternary.  Check to create the hash map
        {                                                //if it doesn't exist. or add the model if there isn't one.
            m_AssetCommands.put(controllerId, new HashMap<UUID, AssetSyncableCommandModel>());
            tryAddOrUpdateAssetCommandModel(controllerId, uuid, capabilities);
        }
        else if (!m_AssetCommands.get(controllerId).containsKey(uuid))
        {
            tryAddOrUpdateAssetCommandModel(controllerId, uuid, capabilities);
        }
        else if (!m_AssetCommands.get(controllerId).get(uuid).getCapabilities().equals(capabilities))
        {
            tryAddOrUpdateAssetCommandModel(controllerId, uuid, capabilities);
        }
        return m_AssetCommands.get(controllerId).get(uuid);
    }
    
    /**
     * Method that attempts to add or update an {@link AssetSyncableCommandModel} to the map for the specified asset.
     * 
     * @param controllerId
     *          ID of the controller where the asset that the model represents is located.
     * @param uuid
     *          {@link UUID} of the asset the model represents.
     * @param capabilities
     *          {@link CommandCapabilities} for the asset the model represents.
     */
    private void tryAddOrUpdateAssetCommandModel(final int controllerId, final UUID uuid, 
            final CommandCapabilities capabilities)
    {
        final AssetSyncableCommandModel commandModel;
        final String exceptionMessage = String.format("Unable to display commands for asset with UUID: %s. See server " 
                + "log for further details.", uuid);
        try
        {
            if (m_AssetCommands.get(controllerId).containsKey(uuid))
            {
                //Attempt to update model if it already exists.
                m_AssetCommands.get(controllerId).get(uuid).updateCapabilities(capabilities);
            }
            else
            {
                commandModel = new AssetSyncableCommandModel(uuid, capabilities);
                m_AssetCommands.get(controllerId).put(uuid, commandModel);
            }
        }
        catch (final ClassNotFoundException exception)
        {
            m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, EXCEPTION_TITLE, exceptionMessage, 
                    exception);
            return;
        }
        catch (final InstantiationException exception)
        {
            m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, EXCEPTION_TITLE, exceptionMessage, 
                    exception);
            return;
        }
        catch (final IllegalAccessException exception)
        {
            m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, EXCEPTION_TITLE, exceptionMessage, 
                    exception);
            return;
        }
    }
    
    @Override
    public void syncCall(final int controllerId, final AssetSyncableCommandModel model)
    {
        for (CommandTypeEnum commandType: model.getSupportedCommands())
        {
            if (model.canSync(commandType))
            {
                doSync(controllerId, model, commandType);
            }
        }
    }
    
    @Override
    public void doSync(final int controllerId, final AssetSyncableCommandModel model, final CommandTypeEnum commandType)
    {
        final mil.dod.th.core.remote.proto.SharedMessages.UUID protoUuid = 
                SharedMessageUtils.convertUUIDToProtoUUID(model.getUuid());
        
        final CommandTypeEnum commandSyncType = model.getCommandSyncTypeByType(commandType);
        if (commandSyncType == null)
        {
            throw new IllegalArgumentException(String.format("The specified command %s does not support syncing.", 
                    commandType.toString()));
        }

        final String exceptionMessage = "Cannot send sync message for %s. See server logs for further details.";
        final String className = AssetCommandUtil.commandTypeToClassName(commandType);
        
        final Message.Builder commandMessage;
        try
        {
            commandMessage = AssetCommandUtil.createMessageBuilderFromCommandType(m_CommandConverterService
                    .retrieveProtoClass(commandSyncType));
        }
        catch (final SecurityException | NoSuchMethodException | IllegalArgumentException
                | IllegalAccessException | InvocationTargetException | ClassNotFoundException exception)
        {
            m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, EXCEPTION_TITLE, 
                    String.format(exceptionMessage, className), exception);
            
            return;
        }
        
        final ExecuteCommandRequestData executeCommandRequest = ExecuteCommandRequestData.newBuilder()
                .setUuid(protoUuid).setCommandType(CommandTypeEnumConverter.convertJavaEnumToProto(commandSyncType))
                .setCommand(commandMessage.build().toByteString()).build();
        
        m_MessageFactory.createAssetMessage(AssetMessageType.ExecuteCommandRequest, executeCommandRequest).
            queue(controllerId, m_SyncResponseHandler);
        
    }
    
    /**
     * Method that handles converting a get command response message to its local JAXB equivalent.
     * 
     * @param controllerId
     *          ID of the controller where the get command message response originated.
     * @param assetUuid
     *          UUID of the asset the get command was executed for.
     * @param responseType
     *          The {@link CommandResponseEnum} associated with the response message.
     * @param responseMessage
     *          The byte array that represents the response message containing data on the get command executed.
     */
    private void handleSyncResponse(final int controllerId, final UUID assetUuid, 
            final CommandResponseEnum responseType, final byte[] responseMessage)
    {
        final AssetSyncableCommandModel model = m_AssetCommands.get(controllerId).get(assetUuid);
        final CommandTypeEnum commandSyncType = CommandTypeEnum.valueOf(responseType.toString().replace(
                RESPONSE_OBJECT_SUFFIX.toUpperCase(), COMMAND_OBJECT_SUFFIX.toUpperCase()));
        final CommandTypeEnum commandType = model.getCommandTypeBySyncType(commandSyncType);
        final Command command = model.getCommandByType(commandType);
        if (command == null)
        {
            m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, EXCEPTION_TITLE, 
                    String.format("Command %s could not be synced for asset with UUID: %s because the command " 
                            + "does not exist or the command isn't supported.", 
                            model.getCommandDisplayName(commandSyncType), assetUuid));
            return;
        }
        
        try
        {
            final Object jaxbResponse = m_CommandConverterService.getJavaResponseType(responseMessage, responseType);
            
            for (Field field: jaxbResponse.getClass().getDeclaredFields())
            {
                if (ReflectionsUtil.hasJaxbAnnotation(field))
                {
                    try
                    {
                        handleField(field, jaxbResponse, command);
                    }
                    catch (final ReflectionsUtilException exception)
                    {
                        final String exceptionMessage = "The response %s was unable to " 
                                + "be converted for use in syncing. " 
                                + "See server log for further details.";
                        
                        m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, EXCEPTION_TITLE, 
                                String.format(exceptionMessage, jaxbResponse.getClass().getSimpleName(), exception));
                        return;
                    }
                }
            }  
            
            m_EventAdmin.postEvent(new Event(TOPIC_COMMAND_SYNCED, new HashMap<String, Object>()));
            m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Asset Command Synchronized:", 
                    String.format("%s for asset with UUID: %s synced.", model.getCommandDisplayName(commandType), 
                            assetUuid.toString()));
        }
        catch (final ObjectConverterException | InvalidProtocolBufferException exception)
        {
            m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, EXCEPTION_TITLE, 
                    String.format("Command %s could not be synced for asset with UUID: %s because the command" 
                            + " response could not be converted to an equivalent jaxb type.", 
                            model.getCommandDisplayName(commandSyncType), assetUuid));
        }
    }
    
    /**
     * Method used to handle setting the value of a field in the local command from the get command response received.
     * 
     * @param field
     *          Field in the response to be set in the local command.
     * @param jaxbResponse
     *          The get command response object.
     * @param localCommand
     *          The local command object.
     * @throws ReflectionsUtilException
     *          Thrown if an error occurs while using the reflections. 
     */
    @SuppressWarnings("unchecked")
    private void handleField(final Field field, final Object jaxbResponse, final Command localCommand) 
            throws ReflectionsUtilException
    {
        final String fieldName = field.getName();
        final Boolean isSet = ReflectionsUtil.isFieldSet(jaxbResponse, fieldName);

        if (isSet)
        {   
            if (field.getType().equals(List.class))
            {
                final List<Object> newList = (List<Object>)ReflectionsUtil.retrieveInnerObject(jaxbResponse, fieldName, 
                        field.getType());
                final List<Object> oldList = (List<Object>)ReflectionsUtil.retrieveInnerObject(localCommand, fieldName, 
                        field.getType());
                oldList.clear();
                oldList.addAll(newList);
            }
            else
            {
                final Object fieldValue = ReflectionsUtil.retrieveInnerObject(jaxbResponse, fieldName, field.getType());
                ReflectionsUtil.setInnerObject(localCommand, fieldName, fieldValue);
            }
        }
        else
        {
            ReflectionsUtil.unsetField(localCommand, fieldName);
        }
    }
    
    /**
     * Response handler that handles all execute command responses that pertain to retrieving the current values for a
     * particular command.
     */
    class SyncCommandResponseHandler implements ResponseHandler
    {
        @Override
        public void handleResponse(final TerraHarvestMessage message, final TerraHarvestPayload payload,
                final Message namespaceMessage, final Message dataMessage)
        {
            final AssetMessageType messageType = ((AssetMessages.AssetNamespace)namespaceMessage).getType();
            if (messageType.equals(AssetMessageType.ExecuteCommandResponse))
            {
                final ExecuteCommandResponseData executeResponse = (ExecuteCommandResponseData)dataMessage;
                final UUID assetUuid = SharedMessageUtils.convertProtoUUIDtoUUID(executeResponse.getUuid());
                final int controllerId = message.getSourceId();
                final CommandResponseEnum commandResponseType = CommandResponseEnumConverter.convertProtoEnumToJava(
                    executeResponse.getResponseType());

                handleSyncResponse(controllerId, assetUuid, commandResponseType, 
                        executeResponse.getResponse().toByteArray());
            }
        }
    }
}
