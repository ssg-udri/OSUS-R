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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.inject.Inject;

import com.google.protobuf.Message;
import com.google.protobuf.UninitializedMessageException;

import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace.AssetMessageType;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.types.command.CommandResponseEnum;
import mil.dod.th.core.types.command.CommandTypeEnum;
import mil.dod.th.ose.gui.webapp.utils.AssetCommandUtil;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.ose.remote.api.CommandConverter;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.converter.CommandResponseEnumConverter;
import mil.dod.th.remote.converter.CommandTypeEnumConverter;

import org.glassfish.osgicdi.OSGiService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;


/**
 * Class which handles sending commands and listens for their responses. 
 * @author nickmarcucci
 *
 */
@ManagedBean(name = "commandHandlerMgr")
@ApplicationScoped
public class CommandHandlerMgrImpl implements CommandHandlerMgr, EventHandler
{
    /**
     * String reference to the title used for growl messages posted by this class.
     */
    private static final String EXCEPTION_TITLE = "Asset Command Error:";
    
    /**
     * Service registration for the listener service. Saved for unregistering the service when the bean is 
     * destroyed.
     */
    @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
    private ServiceRegistration m_Registration;
    
    /**
     * Reference to the bundle context utility.
     */
    @Inject
    private BundleContextUtil m_BundleUtil;
    
    /**
     * Reference to the growl message utility.
     */
    @Inject
    private GrowlMessageUtil m_GrowlUtil;
    
    /**
     * Reference to the error response handler to be used.
     */
    private ErrorResponseHandler m_ErrorResponseHandler;
    
    /**
     * Reference to service which provides utilities to aid in command processing
     * operations.
     */
    @Inject @OSGiService
    private CommandConverter m_CommandConverterService;
    
    /**
     * Reference to the service which converts between JAXB and protocol buffer objects.
     */
    @Inject @OSGiService
    private JaxbProtoObjectConverter m_Converter;
    
    /**
     * Inject the MessageFactory service for creating remote messages.
     */
    @Inject @OSGiService
    private MessageFactory m_MessageFactory;
    
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
     * Method that sets the converter service to be used.
     * @param converter
     *          The service used to convert between JAXB and protocol buffer objects.
     */
    public void setConverter(final JaxbProtoObjectConverter converter)
    {
        m_Converter = converter;
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
     * Method that sets the bundle context utility to use.
     * 
     * @param bundleUtil
     *          The bundle context utility to be used.
     */
    public void setBundleContextUtil(final BundleContextUtil bundleUtil)
    {
        m_BundleUtil = bundleUtil;
    }
    
    /**
     * Post construct method that instantiates the required response handler and data converters.
     */
    @PostConstruct
    public void postConstruct()
    {
        m_ErrorResponseHandler = new ErrorResponseHandler();
        registerCommandEvents();
    }
    
    /**
     * Predestroy method to handle cleaning up registered event handlers.
     */
    @PreDestroy
    public void cleanup()
    {
        m_Registration.unregister();
    }
    
    @Override
    public void handleEvent(final Event event)
    {
        final ExecuteCommandResponseData response = 
                (ExecuteCommandResponseData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE);
        
        final UUID assetUuid = SharedMessageUtils.convertProtoUUIDtoUUID(response.getUuid());
        
        final CommandResponseEnum responseType = CommandResponseEnumConverter.convertProtoEnumToJava(
            response.getResponseType());
        final CommandTypeEnum commandType = m_CommandConverterService.getCommandTypeFromResponseType(responseType);
        
        final String commandName = AssetCommandUtil.commandTypeToClassName(commandType);
        final String summary = "Asset Command Executed:";
        final String description = String.format("%s executed for asset with uuid: %s", commandName, assetUuid);

        m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, summary, 
                String.format(description, commandName, assetUuid));
    }
    
    @Override
    public void sendCommand(final int controllerId, final UUID uuid, 
            final CommandTypeEnum commandType) throws ClassNotFoundException, 
            InstantiationException, IllegalAccessException
    {
        final Command command = AssetCommandUtil.instantiateCommandBasedOnType(commandType);
        
        sendCommand(controllerId, uuid, command, commandType);
    }

    @Override
    public void sendCommand(final int controllerId, final UUID uuid, 
            final Command command, final CommandTypeEnum commandType)
    {
        final Message commandMessage;
        
        try
        {
            commandMessage = m_Converter.convertToProto(command);
        }
        catch (final ObjectConverterException | UninitializedMessageException exception)
        {
            m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, EXCEPTION_TITLE, String.format(
                    "Unable to send %s command. See server log for futher details.", 
                    command.getClass().getSimpleName()), exception);
            return;
        }
        
        if (commandType != null)
        {
            final mil.dod.th.core.remote.proto.SharedMessages.UUID uuidProto = 
                    SharedMessageUtils.convertUUIDToProtoUUID(uuid);
            
            final ExecuteCommandRequestData executeCommand = ExecuteCommandRequestData.newBuilder().setUuid(uuidProto)
                    .setCommandType(CommandTypeEnumConverter.convertJavaEnumToProto(commandType))
                    .setCommand(commandMessage.toByteString()).build();
            m_MessageFactory.createAssetMessage(AssetMessageType.ExecuteCommandRequest, executeCommand).
                queue(controllerId, m_ErrorResponseHandler);
        }
        else 
        {
            throw new IllegalArgumentException(String.format("Cannot send %s command to system 0x%08x " 
                    + "because command type is null", command.getClass().getSimpleName(), controllerId));
        }
    }
    
    /**
     * Method that registers the event handler to listen for execute command response messages received.
     */
    private void registerCommandEvents()
    {
        final BundleContext context = m_BundleUtil.getBundleContext();
        // register to listen for message received events.
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(EventConstants.EVENT_TOPIC, RemoteConstants.TOPIC_MESSAGE_RECEIVED);
        final String filterString = String.format("(&(%s=%s)(%s=%s))", 
            RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Asset.toString(),
            RemoteConstants.EVENT_PROP_MESSAGE_TYPE, AssetMessageType.ExecuteCommandResponse.toString());
        props.put(EventConstants.EVENT_FILTER, filterString);
        
        //register the event handler that listens for messages received.
        m_Registration = context.registerService(EventHandler.class, this, props);    
    }
    
    /**
     * Response handler that handles error responses when sending a command to be executed.
     */
    class ErrorResponseHandler implements ResponseHandler
    {
        @Override
        public void handleResponse(final TerraHarvestMessage message, final TerraHarvestPayload payload,
                final Message namespaceMessage, final Message dataMessage)
        {
            if (payload.getNamespace() == Namespace.Base)
            {
                final BaseNamespace baseMessage = (BaseNamespace)namespaceMessage;
                
                if (baseMessage.getType() == BaseMessageType.GenericErrorResponse)
                {
                    final GenericErrorResponseData errorResponse = (GenericErrorResponseData)dataMessage;
                    final ErrorCode errorCode = errorResponse.getError();
                    m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "Could not execute command:", 
                                errorCode.toString() + ", " + errorResponse.getErrorDescription());
                }
            }
        }
    }
}
