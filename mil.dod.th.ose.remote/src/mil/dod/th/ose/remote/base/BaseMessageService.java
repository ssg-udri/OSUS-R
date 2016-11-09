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
package mil.dod.th.ose.remote.base;

import java.io.IOException;
import java.util.List;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import mil.dod.th.core.controller.TerraHarvestController;
import mil.dod.th.core.controller.TerraHarvestController.OperationMode;
import mil.dod.th.core.controller.capability.ControllerCapabilities;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.BaseMessages;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.ControllerInfoData;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.GetControllerCapabilitiesRequestData;
import mil.dod.th.core.remote.proto.BaseMessages.GetControllerCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.GetOperationModeReponseData;
import mil.dod.th.core.remote.proto.BaseMessages.SetOperationModeRequestData;
import mil.dod.th.core.remote.proto.MapTypes.SimpleTypesMapEntry;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.system.TerraHarvestSystem;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.MessageService;
import mil.dod.th.ose.remote.api.EnumConverter;
import mil.dod.th.ose.remote.util.RemoteInterfaceUtilities;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.lexicon.controller.capability.ControllerCapabilitiesGen;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;


/**
 * This service handles all messages for the base namespace.  Basically any message that is part of the core remote
 * interface.
 * 
 * @author Dave Humeniuk
 *
 */
//service is not provided as this would create a cycle, instead this class registers with the message router
@Component(immediate = true, provide = { }) //NOCHECKSTYLE: Class-Fan-Out Complexity is 32 (max allowed is 30) 
                                            //needed to implement all base namespace messages

public class BaseMessageService implements MessageService
{
    /**
     * Service to use for posting events by this component.
     */
    private EventAdmin m_EventAdmin;
    
    /**
     * Service for creating messages to send through the remote interface.
     */
    private MessageFactory m_MessageFactory;

    /**
     * Service that describes the system (JVM/OSGi instance).
     */
    private TerraHarvestSystem m_TerraHarvestSystem;
    
    /**
     * TerraHarvest Controller.
     */
    private TerraHarvestController m_TerraHarvestController;
    
    /**
     * Service for logging messages.
     */
    private LoggingService m_Log;

    /**
     * Reference to the system bundle which can be used to stop the framework.
     */
    private Bundle m_SystemBundle;
    
    /**
     * Routes incoming messages.
     */
    private MessageRouterInternal m_MessageRouter;
    
    /**
     * Converter object used to convert java object to google protocol buffer.
     */
    private JaxbProtoObjectConverter m_Converter;

    /**
     * Binds the logging service for logging messages.
     * 
     * @param logging
     *            Logging service object
     */
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Log = logging;
    }
    
    /**
     * Bind the event admin service.
     * 
     * @param eventAdmin
     *      event admin service to use to post events
     */
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Bind to the service for creating remote messages.
     * 
     * @param messageFactory
     *      service that create messages
     */
    @Reference
    public void setMessageFactory(final MessageFactory messageFactory)
    {
        m_MessageFactory = messageFactory;
    }
    
    /**
     * Set the {@link mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter}.
     * 
     * @param converter
     *     the service responsible for converting between proto and JAXB objects.
     */
    @Reference
    public void setJaxbProtoObjectConverter(final JaxbProtoObjectConverter converter)
    {
        m_Converter = converter;        
    }
    
    /**
     * Set the {@link mil.dod.th.core.controller.TerraHarvestController}.
     * 
     * @param terraHarvestController
     *      service that describes the Terra Harvest controller
     *      
     */
    @Reference
    public void setTerraHarvestController(final TerraHarvestController terraHarvestController)
    {
        m_TerraHarvestController = terraHarvestController;
    }
    
    /**
     * Bind to the controller system service.
     * 
     * @param terraHarvestSystem
     *      service that describes the system
     */
    @Reference
    public void setTerraHarvestSystem(final TerraHarvestSystem terraHarvestSystem)
    {
        m_TerraHarvestSystem = terraHarvestSystem;
    }
    
    /**
     * Bind a message router to register.
     * 
     * @param messageRouter
     *      router that handles incoming messages
     */
    @Reference
    public void setMessageRouter(final MessageRouterInternal messageRouter)
    {
        m_MessageRouter = messageRouter;
    }
    
    /**
     * Activate method to save off system bundle for later use and bind this service to the message router.
     * 
     * @param context
     *      context for the bundle containing this class
     */
    @Activate
    public void activate(final BundleContext context)
    {
        m_SystemBundle = context.getBundle(0);
        
        m_MessageRouter.bindMessageService(this);
    }
    
    /**
     * Deactivate component by unbinding the service from the message router.
     */
    @Deactivate
    public void deactivate()
    {
        m_MessageRouter.unbindMessageService(this);
    }
    
    @Override
    public Namespace getNamespace()
    {
        return Namespace.Base;
    }

    @Override
    public void handleMessage(final TerraHarvestMessage message, final TerraHarvestPayload payload, 
        final RemoteChannel channel) throws IOException
    {
        final BaseNamespace baseMessage = BaseNamespace.parseFrom(payload.getNamespaceMessage());
        final Message dataMessage;
     
        // message specific handling
        switch (baseMessage.getType())
        {
            case RequestControllerInfo:
                dataMessage = null;
                handleControllerSystemInfo(message, channel);
                break;
            case ControllerInfo:
                dataMessage = ControllerInfoData.parseFrom(baseMessage.getData());
                break;
            case GenericErrorResponse:
                dataMessage = GenericErrorResponseData.parseFrom(baseMessage.getData());
                break;
            case ShutdownSystem:
                dataMessage = null;
                handleShutdownSystem(message, channel);
                break;
            case ReceivedShutdownRequest:
                dataMessage = null;
                break;
            case SetOperationModeRequest:
                dataMessage = setOperationMode(message, baseMessage, channel);
                break;
            case SetOperationModeResponse:
                dataMessage = null;
                break;
            case GetOperationModeRequest:
                dataMessage = null;
                getControllerMode(message, channel);
                break;
            case GetOperationModeResponse:
                dataMessage = GetOperationModeReponseData.parseFrom(baseMessage.getData());
                break;
            case GetControllerCapabilitiesRequest:
                dataMessage = getControllerCapabilities(baseMessage, message, channel);
                break;      
            case GetControllerCapabilitiesResponse:
                dataMessage = GetControllerCapabilitiesResponseData.parseFrom(baseMessage.getData());
                break; 
            default:
                throw new UnsupportedOperationException(
                        String.format("Message type: %s is not a supported type for"
                                + " the BaseMessageService namespace.", baseMessage.getType()));

        }
        
        // post event that message was received
        final Event event = RemoteInterfaceUtilities.createMessageReceivedEvent(message, payload, baseMessage, 
                baseMessage.getType(), dataMessage, channel);
        m_EventAdmin.postEvent(event);
    }

    /**
     * Send the controller info based on the given request.
     *  
     * @param request
     *      full request message
     * @param channel
     *      channel to use for sending a response
     * @throws IOException
     *      if there is a failure while sending the response message
     */
    private void handleControllerSystemInfo(final TerraHarvestMessage request, final RemoteChannel channel) 
            throws IOException
    {
        //construct the build info message
        final List<SimpleTypesMapEntry> buildInfoEntries = 
                SharedMessageUtils.convertStringMapToListSimpleTypesMapEntry(m_TerraHarvestController.getBuildInfo());

        final ControllerInfoData controllerInfoData = ControllerInfoData.newBuilder().
                setName(m_TerraHarvestController.getName()).
                setVersion(m_TerraHarvestController.getVersion()).
                addAllBuildInfo(buildInfoEntries).
                setCurrentSystemTime(System.currentTimeMillis()).
                build();
        
        m_MessageFactory.createBaseMessageResponse(request, BaseMessageType.ControllerInfo, 
                controllerInfoData).queue(channel);
    }
    
    /**
     * Shutdown the OSGi framework.
     * @param request
     *      request message
     * @param channel
     *      channel to use for sending a response
     * @throws IOException
     *      if there is a failure while sending the response message
     */
    private void handleShutdownSystem(final TerraHarvestMessage request, final RemoteChannel channel) 
            throws IOException
    {
        m_MessageFactory.createBaseMessageResponse(request, BaseMessageType.ReceivedShutdownRequest, null).
            queue(channel);
        
        try
        {
            m_SystemBundle.stop();
        }
        catch (final BundleException e)
        {
            m_Log.error("Bundle Error", e);
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.INTERNAL_ERROR, 
                    "Bundle exception when shutting down system: " + e.getMessage()).queue(channel);
        }
    }
    
    /**
     * Handle setting the controllers operation mode.
     * @param message
     *     the message containing the information needed to process the request
     * @param namespaceMessage
     *     the namespace message that contains the actual request message data
     * @param channel
     *     the channel that the request come through
     * @return
     *     the request message
     * @throws InvalidProtocolBufferException
     *      if there is a failure while sending the response message
     */
    private Message setOperationMode(final TerraHarvestMessage message, final BaseNamespace namespaceMessage, 
        final RemoteChannel channel) throws InvalidProtocolBufferException
    {
        //parse request 
        final SetOperationModeRequestData request = SetOperationModeRequestData.parseFrom(namespaceMessage.getData());
        
        //get the mode
        final OperationMode mode = EnumConverter.convertProtoOperationModeToJava(request.getMode());
        m_TerraHarvestController.setOperationMode(mode);

        m_MessageFactory.createBaseMessageResponse(message, BaseMessageType.SetOperationModeResponse, 
                null).queue(channel);

        //return request data
        return request;
    }
    
    /**
     * Handle getting generic controller capabilities.
     * 
     * @param baseMessage
     *      base namespace message
     * @param request
     *      the message containing the information needed to process the request
     * @param channel
     *      the channel that the request come through
     * @return
     *      Terra Harvest controller capabilities
     * @throws InvalidProtocolBufferException
     *      if data message is invalid
     */
    private Message getControllerCapabilities(final BaseNamespace baseMessage, final TerraHarvestMessage request, 
            final RemoteChannel channel) throws InvalidProtocolBufferException 
    {
        final GetControllerCapabilitiesRequestData requestData = 
                GetControllerCapabilitiesRequestData.parseFrom(baseMessage.getData());
        if (requestData.getControllerCapabilitiesFormat() != RemoteTypesGen.LexiconFormat.Enum.NATIVE)
        {
            throw new UnsupportedOperationException(
                    String.format("Lexicon format %s is not valid for controller capabilities", 
                            requestData.getControllerCapabilitiesFormat())); 
        }
        
        try
        {
            final ControllerCapabilities capabilities = m_TerraHarvestController.getCapabilities();
            final ControllerCapabilitiesGen.ControllerCapabilities protoCapabilities = 
                    (ControllerCapabilitiesGen.ControllerCapabilities)
                    m_Converter.convertToProto(capabilities);
            final GetControllerCapabilitiesResponseData capabilitiesResponse = 
                    GetControllerCapabilitiesResponseData.newBuilder().
                    setControllerCapabilitiesNative(protoCapabilities).build();
            m_MessageFactory.createBaseMessageResponse(request,
                    BaseMessageType.GetControllerCapabilitiesResponse, capabilitiesResponse).queue(channel);
            
            return capabilitiesResponse;
        }
        catch (final ObjectConverterException e)
        {
            m_Log.error(e, 
                    "Cannot convert java object to google protocol buffer message for system %s in get capabilities.",
                    m_TerraHarvestSystem.getId());
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.CONVERTER_ERROR, 
                    "Unable to convert from java object to google protocol buffer message. " + e.getMessage()).
                        queue(channel);
            return requestData;
        }
    }

    /**
     * Handle getting the controller's current operation mode.
     * @param message
     *     the message containing the request
     * @param channel
     *     the channel through which the request came through.
     */
    private void getControllerMode(final TerraHarvestMessage message, final RemoteChannel channel)
    {
        final GetOperationModeReponseData.Builder response = GetOperationModeReponseData.newBuilder();
        
        //get the system mode
        final BaseMessages.OperationMode mode = 
                EnumConverter.convertJavaOperationModeToProto(m_TerraHarvestController.getOperationMode());
        response.setMode(mode);
        
        //send response
        m_MessageFactory.createBaseMessageResponse(message, BaseMessageType.GetOperationModeResponse, response.build()).
            queue(channel);
    }
}
