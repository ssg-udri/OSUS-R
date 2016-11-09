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
package mil.dod.th.ose.remote.comms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.Message;

import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.link.LinkLayerAttributes;
import mil.dod.th.core.ccomm.link.LinkLayerFactory;
import mil.dod.th.core.ccomm.link.capability.LinkLayerCapabilities;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.physical.PhysicalLinkFactory;
import mil.dod.th.core.ccomm.physical.capability.PhysicalLinkCapabilities;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.ccomm.transport.TransportLayerFactory;
import mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CommType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateLinkLayerRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateLinkLayerResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreatePhysicalLinkRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreatePhysicalLinkResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateTransportLayerRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateTransportLayerResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace.CustomCommsMessageType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetAvailableCommTypesRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetAvailableCommTypesResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetCapabilitiesRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetLayerNameRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetLayerNameResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetLayersRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetLayersResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.SetLayerNameRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.MessageService;
import mil.dod.th.ose.remote.util.RemoteInterfaceUtilities;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.converter.PhysicalLinkTypeEnumConverter;
import mil.dod.th.remote.lexicon.ccomm.link.capability.LinkLayerCapabilitiesGen;
import mil.dod.th.remote.lexicon.ccomm.physical.capability.PhysicalLinkCapabilitiesGen;
import mil.dod.th.remote.lexicon.ccomm.transport.capability.TransportLayerCapabilitiesGen;
import mil.dod.th.remote.lexicon.types.ccomm.CustomCommTypesGen.PhysicalLinkType;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * This class is responsible for managing Custom Comms messages received through the remote interface and sending 
 * proper responses according to different incoming Custom Comms request messages.  
 * @author matt
 */
//service is not provided as this would create a cycle, instead this class registers with the message router
@Component(immediate = true, provide = { }) //NOCHECKSTYLE - Reached max class fan out complexity
                                            //needed to implement all custom comms messages
public class CustomCommsMessageService implements MessageService //NOPMD: avoid really long classes. Need to process
//information for all comms layers. 
{
    /**
     * Error message sent remotely after the occurrence of a custom comms exception.
     */
    final private static String CUSTOM_COMMS_EXCEPTION = "Custom comms exception: ";
    
    /**
     * Error message sent remotely after an invalid comm type was given.
     */
    final private static String INVALID_COMM_TYPE = "Invalid comm type: ";
    
    /**
     * Constant error message sent remotely after the occurrence of an exception.
     */
    final private static String GENERIC_ERR_MSG = "Cannot complete request. "; 
    
    /**
     * Service for logging messages.
     */
    private LoggingService m_Logging;
    
    /**
     * Local service for managing {@link CustomCommsService}.
     */
    private CustomCommsService m_CommsService;
    
    /**
     * Reference to the event admin service.  Used for local messages within custom comms message service.
     */
    private EventAdmin m_EventAdmin;
    
    /**
     * Service for creating messages to send through the remote interface.
     */
    private MessageFactory m_MessageFactory;
    
    /**
     * Routes incoming messages.
     */
    private MessageRouterInternal m_MessageRouter;
    
    /**
     * Service that assists in converting instances of {@link LinkLayerCapabilities}, 
     * {@link TransportLayerCapabilities}, and {@link PhysicalLinkCapabilities} from 
     * JAXB objects to google protocol buffer messages.
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
        m_Logging = logging;
    }
    
    /**
     * Bind the event admin service.
     * 
     * @param eventAdmin
     *      service used to post events
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
     * Bind the {@link CustomCommsService}.
     * @param customCommsService
     *      service for managing custom comms locally
     */
    @Reference
    public void setCustomCommsService(final CustomCommsService customCommsService)
    {
        m_CommsService = customCommsService;
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
     * Activate method to bind this service to the message router.
     */
    @Activate
    public void activate()
    {
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
        return Namespace.CustomComms;
    }

    @Override //NOCHECKSTYLE - Reached max cyclomatic complexity - needed to implement all custom comms messages
    public void handleMessage(final TerraHarvestMessage message, final TerraHarvestPayload payload, //NOPMD Avoid 
        final RemoteChannel channel) throws IOException      //Really long methods - need to implement all messages
    {
        final CustomCommsNamespace ccommsMessage = CustomCommsNamespace.parseFrom(payload.getNamespaceMessage());
        final Message dataMessage;

        switch (ccommsMessage.getType())
        {
            case CreatePhysicalLinkRequest:
                dataMessage = createPhysicalLink(ccommsMessage, message, channel);
                break;
            case CreatePhysicalLinkResponse:
                dataMessage = CreatePhysicalLinkResponseData.parseFrom(ccommsMessage.getData());
                break;
            case CreateLinkLayerRequest:
                dataMessage = createLinkLayer(ccommsMessage, message, channel);
                break;
            case CreateLinkLayerResponse:
                dataMessage = CreateLinkLayerResponseData.parseFrom(ccommsMessage.getData());
                break;
            case CreateTransportLayerRequest:
                dataMessage = createTransportLayer(ccommsMessage, message, channel);
                break;
            case CreateTransportLayerResponse:
                dataMessage = CreateTransportLayerResponseData.parseFrom(ccommsMessage.getData());
                break;
            case GetAvailableCommTypesRequest:
                dataMessage = getAvailableCommTypes(ccommsMessage, message, channel);
                break;
            case GetAvailableCommTypesResponse:
                dataMessage = GetAvailableCommTypesResponseData.parseFrom(ccommsMessage.getData());
                break;
            case GetLayersRequest:
                dataMessage = getLayersRequest(ccommsMessage, message, channel);
                break;
            case GetLayersResponse:
                dataMessage = GetLayersResponseData.parseFrom(ccommsMessage.getData());
                break;
            case SetLayerNameRequest:
                dataMessage = setLayerName(ccommsMessage, message, channel);
                break;
            case SetLayerNameResponse:
                dataMessage = null;
                break;
            case GetLayerNameRequest:
                dataMessage = getLayerName(ccommsMessage, message, channel);
                break;
            case GetLayerNameResponse:
                dataMessage = GetLayerNameResponseData.parseFrom(ccommsMessage.getData());
                break;
            case GetCapabilitiesRequest:
                dataMessage = getCapabilities(ccommsMessage, message, channel);
                break;
            case GetCapabilitiesResponse:
                dataMessage = GetCapabilitiesResponseData.parseFrom(ccommsMessage.getData());
                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Message type: %s is not a supported type for"
                                + " the CustomCommsMessageService namespace.", ccommsMessage.getType()));
        }
        
        // locally post event that message was received
        final Event event = RemoteInterfaceUtilities.createMessageReceivedEvent(message, payload, ccommsMessage, 
                ccommsMessage.getType(), dataMessage, channel);
        
        m_EventAdmin.postEvent(event);
    }

    /**
     * Method responsible for handling a request to get a comms layer's capabilities based on 
     * the comms type and fully qualified class name.
     * @param ccommsMessage
     *      get capabilities request message containing the comms type and fully qualified class name of the
     *      comms layer to be queried.
     * @param message
     *      entire remote message for the request
     * @param channel
     *    the channel that was used to send request
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed or if response message cannot be sent
     */
    private Message getCapabilities(final CustomCommsNamespace ccommsMessage, final TerraHarvestMessage message, 
            final RemoteChannel channel) throws IOException 
    {
        final GetCapabilitiesRequestData capabilitiesRequest = 
                GetCapabilitiesRequestData.parseFrom(ccommsMessage.getData());
        final CommType ccommType = capabilitiesRequest.getCommType();
        switch (ccommType)
        {
            case Linklayer:
                return getLinkLayerCapabilities(ccommsMessage, message, channel);
            case TransportLayer:
                return getTransportLayerCapabilities(ccommsMessage, message, channel);
            case PhysicalLink:
                return getPhysicalLinkCapabilities(ccommsMessage, message, channel);
            default:
                m_Logging.error("[%s] is not a valid link type.", ccommType.toString());
                m_MessageFactory.createBaseErrorMessage(message, ErrorCode.INVALID_VALUE, 
                        INVALID_COMM_TYPE + "Cannot get capabilities for an invalid comm type.").queue(channel);
                return capabilitiesRequest;
        }
    }

    /**
     * Method responsible for handling a request to get a link layer's capabilities given the
     * link layer's type.
     * @param ccommsMessage
     *      get capabilities request message containing the link layer's type
     * @param message
     *      entire remote message for the request
     * @param channel
     *    the channel that was used to send request
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed or if response message cannot be sent
     */
    private Message getLinkLayerCapabilities(final CustomCommsNamespace ccommsMessage, 
            final TerraHarvestMessage message, final RemoteChannel channel) throws IOException 
    {
        final GetCapabilitiesRequestData capabilitiesRequest = 
                GetCapabilitiesRequestData.parseFrom(ccommsMessage.getData());
        final String linkLayerType = capabilitiesRequest.getProductType();
        final Set<LinkLayerFactory> factories = m_CommsService.getLinkLayerFactories();

        LinkLayerFactory factory = null;

        // get the factory by link layer FQCN
        for (LinkLayerFactory tempFactory : factories)
        {
            if (linkLayerType.equals(tempFactory.getProductType()))
            {
                factory = tempFactory;
                break;
            }
        }

        if (factory == null)
        {
            m_Logging.error("Cannot get capabilities, no factory found for linke layer [%s].", linkLayerType);
            m_MessageFactory.createBaseErrorMessage(message, ErrorCode.INVALID_VALUE, 
                    GENERIC_ERR_MSG + "Link layer factory not found.").queue(channel);                
            return capabilitiesRequest;
        }
        
        // get capabilities from the factory
        try
        {
            final LinkLayerCapabilities capabilities = factory.getLinkLayerCapabilities();
            final LinkLayerCapabilitiesGen.LinkLayerCapabilities capGen =
                    (LinkLayerCapabilitiesGen.LinkLayerCapabilities)m_Converter.convertToProto(capabilities);
            final GetCapabilitiesResponseData response = GetCapabilitiesResponseData.newBuilder().
                    setProductType(linkLayerType).
                    setCommType(CommType.Linklayer).
                    setLinkCapabilities(capGen).build();
            
            m_MessageFactory.createCustomCommsResponseMessage(message, CustomCommsMessageType.GetCapabilitiesResponse, 
                    response).queue(channel);
        }
        catch (final ObjectConverterException e)
        {
            m_Logging.error(e, "Cannot convert object to proto message for link layer %s in get capabilities.", 
                    linkLayerType);
            m_MessageFactory.createBaseErrorMessage(message,  ErrorCode.CONVERTER_ERROR, 
                    GENERIC_ERR_MSG + e.getMessage()).queue(channel);
        }
        return capabilitiesRequest;
    }

    /**
     * Method responsible for handling a request to get a transport layer's capabilities given the
     * transport layer's type.
     * @param ccommsMessage
     *      get capabilities request message containing the link layer's type
     * @param message
     *      entire remote message for the request
     * @param channel
     *    the channel that was used to send request
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed or if response message cannot be sent
     */
    private Message getTransportLayerCapabilities(final CustomCommsNamespace ccommsMessage, 
            final TerraHarvestMessage message, final RemoteChannel channel) throws IOException 
    {
        final GetCapabilitiesRequestData capabilitiesRequest = 
                GetCapabilitiesRequestData.parseFrom(ccommsMessage.getData());
        final String transportLayerType = capabilitiesRequest.getProductType();
        final Set<TransportLayerFactory> factories = m_CommsService.getTransportLayerFactories();

        TransportLayerFactory factory = null;

        // get the factory by transport layer FQCN
        for (TransportLayerFactory tempFactory : factories)
        {
            if (transportLayerType.equals(tempFactory.getProductType()))
            {
                factory = tempFactory;
                break;
            }
        }

        if (factory == null)
        {
            m_Logging.error("Cannot get capabilities, no factory found for transport layer [%s].", transportLayerType);
            m_MessageFactory.createBaseErrorMessage(message, ErrorCode.INVALID_VALUE, 
                    GENERIC_ERR_MSG + "Transport layer factory not found.").queue(channel);                
            return capabilitiesRequest;
        }
        
        // get capabilities from the factory
        try
        {
            final TransportLayerCapabilities capabilities = factory.getTransportLayerCapabilities();
            final TransportLayerCapabilitiesGen.TransportLayerCapabilities capGen =
                    (TransportLayerCapabilitiesGen.TransportLayerCapabilities)m_Converter.convertToProto(capabilities);
            final GetCapabilitiesResponseData response = GetCapabilitiesResponseData.newBuilder().
                    setProductType(transportLayerType).
                    setCommType(CommType.TransportLayer).
                    setTransportCapabilities(capGen).build();
            
            m_MessageFactory.createCustomCommsResponseMessage(message, CustomCommsMessageType.GetCapabilitiesResponse, 
                    response).queue(channel);
        }
        catch (final ObjectConverterException e)
        {
            m_Logging.error(e, "Cannot convert object to proto message for transport layer %s in get capabilities.", 
                    transportLayerType);
            m_MessageFactory.createBaseErrorMessage(message,  ErrorCode.CONVERTER_ERROR, 
                    GENERIC_ERR_MSG + e.getMessage()).queue(channel);
        }
        return capabilitiesRequest;
    }

    /**
     * Method responsible for handling a request to get a physical link's capabilities given the
     * physical link's type.
     * @param ccommsMessage
     *      get capabilities request message containing the physical link's type
     * @param message
     *      entire remote message for the request
     * @param channel
     *    the channel that was used to send request
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed or if response message cannot be sent
     */
    private Message getPhysicalLinkCapabilities(final CustomCommsNamespace ccommsMessage, 
            final TerraHarvestMessage message, final RemoteChannel channel) throws IOException 
    {
        final GetCapabilitiesRequestData capabilitiesRequest = 
                GetCapabilitiesRequestData.parseFrom(ccommsMessage.getData());
        final String physicalLinkType = capabilitiesRequest.getProductType();
        final Set<PhysicalLinkFactory> factories = m_CommsService.getPhysicalLinkFactories();

        PhysicalLinkFactory factory = null;

        // get the factory by FQCN
        for (PhysicalLinkFactory tempFactory : factories)
        {
            if (physicalLinkType.equals(tempFactory.getProductType()))
            {
                factory = tempFactory;
                break;
            }
        }

        if (factory == null)
        {
            m_Logging.error("Cannot get capabilities, no factory found for type %s.", physicalLinkType);
            m_MessageFactory.createBaseErrorMessage(message, ErrorCode.INVALID_VALUE, 
                    GENERIC_ERR_MSG + "Physical link factory not found.").queue(channel);                
            return capabilitiesRequest;
        }
        
        // get capabilities from the factory
        try
        {
            final PhysicalLinkCapabilities capabilities = factory.getPhysicalLinkCapabilities();
            final PhysicalLinkCapabilitiesGen.PhysicalLinkCapabilities capGen =
                    (PhysicalLinkCapabilitiesGen.PhysicalLinkCapabilities)m_Converter.convertToProto(capabilities);
            final GetCapabilitiesResponseData response = GetCapabilitiesResponseData.newBuilder().
                    setProductType(physicalLinkType).
                    setCommType(CommType.PhysicalLink).
                    setPhysicalCapabilities(capGen).build();
            
            m_MessageFactory.createCustomCommsResponseMessage(message, CustomCommsMessageType.GetCapabilitiesResponse, 
                    response).queue(channel);
        }
        catch (final ObjectConverterException e)
        {
            m_Logging.error(e, "Cannot convert object to proto message for physical link %s in get capabilities.", 
                    physicalLinkType);
            m_MessageFactory.createBaseErrorMessage(message,  ErrorCode.CONVERTER_ERROR, 
                    GENERIC_ERR_MSG + e.getMessage()).queue(channel);
        }
        return capabilitiesRequest;
    }

    /**
     * Method to get all custom comms service layers of the specified type from the request method.
     * @param ccommsMessage
     *      request message containing the specific comm layer to get the different layers for.
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed or response message cannot be sent
     */
    private Message getLayersRequest(final CustomCommsNamespace ccommsMessage, final TerraHarvestMessage request,
        final RemoteChannel channel) throws IOException
    {
        final GetLayersRequestData getLayersRequest = GetLayersRequestData.parseFrom(ccommsMessage.getData());
        
        final CommType type = getLayersRequest.getCommType();
        
        final GetLayersResponseData.Builder response = GetLayersResponseData.newBuilder();
        response.setCommType(type);
        
        //Get all link layer uuids known to the service
        if (type.equals(CommType.Linklayer))
        {
            for (LinkLayer linkLayer : m_CommsService.getLinkLayers())
            {
                response.addLayerInfo(SharedMessageUtils.createFactoryObjectInfoMessage(linkLayer));
            }
        }
        //Get all physical link uuids known to the service
        else if (type.equals(CommType.PhysicalLink))
        {
            for (UUID uuid : m_CommsService.getPhysicalLinkUuids())
            {
                response.addLayerInfo(SharedMessageUtils.createFactoryObjectInfoMessage(
                        m_CommsService.getPhysicalLinkPid(uuid), uuid, m_CommsService.getPhysicalLinkFactory(uuid)));
            }
        }
        //Get all transport layer uuids known to the service
        else if (type.equals(CommType.TransportLayer))
        {
            for (TransportLayer transportLayer : m_CommsService.getTransportLayers())
            {
                response.addLayerInfo(SharedMessageUtils.createFactoryObjectInfoMessage(transportLayer));
            }
        }
        else
        {
            m_Logging.error(INVALID_COMM_TYPE + type.toString(), 
                    "getLayersRequest: Cannot get layers for unknown comm type.");
                    
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.INVALID_VALUE, 
                    INVALID_COMM_TYPE).queue(channel);
            
            return getLayersRequest;
        }
        
        m_MessageFactory.createCustomCommsResponseMessage(request, CustomCommsMessageType.GetLayersResponse, 
                response.build()).queue(channel);
        
        return getLayersRequest;
    }

    /**
     * Method to get types of comm layers available by fully qualified class names.
     * @param ccommsMessage
     *      request message containing the type to find available comms on.
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed or response message cannot be sent
     */
    private Message getAvailableCommTypes(final CustomCommsNamespace ccommsMessage, final TerraHarvestMessage request,
        final RemoteChannel channel) throws IOException
    {
        final GetAvailableCommTypesRequestData getAvailableComm = 
                GetAvailableCommTypesRequestData.parseFrom(ccommsMessage.getData());
        
        final CommType type = getAvailableComm.getCommType();
        
        final GetAvailableCommTypesResponseData.Builder response = GetAvailableCommTypesResponseData.newBuilder().
            setCommType(type);
        
        final Set<FactoryDescriptor> factories = new HashSet<>();
        //Get all link layer class names known to the service
        if (type.equals(CommType.Linklayer))
        {
            factories.addAll(m_CommsService.getLinkLayerFactories());
        }
        //Get all physical link class names known to the service
        else if (type.equals(CommType.PhysicalLink))
        {
            factories.addAll(m_CommsService.getPhysicalLinkFactories());
        }
        //Get all transport layer class names known to the service
        else if (type.equals(CommType.TransportLayer))
        {
            factories.addAll(m_CommsService.getTransportLayerFactories());
        }
        else
        {
            m_Logging.error(INVALID_COMM_TYPE + type.toString(), 
                    "getAvailableCommTypes: Cannot get comm types for unknown comm type.");
                    
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.INVALID_VALUE, 
                    INVALID_COMM_TYPE).queue(channel);
            
            return getAvailableComm;
        }
        
        //list to hold the names
        final List<String> layerFQCNs = new ArrayList<String>();
        
        //gather the types
        for (FactoryDescriptor factory : factories)
        {
            layerFQCNs.add(factory.getProductType());
        }
        response.addAllProductType(layerFQCNs);
        m_MessageFactory.createCustomCommsResponseMessage(request, CustomCommsMessageType.GetAvailableCommTypesResponse,
                response.build()).queue(channel);
        
        return getAvailableComm;
    }

    /**
     * Method to create a new instance of a {@link TransportLayer} and add it to the 
     * service.
     * @param ccommsMessage
     *      request message containing the transport layer type, name, timeout value, and associated link layer UUID
     *      for creating a new transport layer.
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException 
     *      if message cannot be parsed or response message cannot be sent
     */
    private Message createTransportLayer(final CustomCommsNamespace ccommsMessage, final TerraHarvestMessage request,
        final RemoteChannel channel) throws IOException
    {
        final CreateTransportLayerRequestData createTransportLayer = CreateTransportLayerRequestData.
                parseFrom(ccommsMessage.getData());
        
        final String transportLayerType = createTransportLayer.getTransportLayerProductType();
        final String transportLayerName = createTransportLayer.getTransportLayerName();
        
        try
        {
            String linkLayerName = null;
            
            if (createTransportLayer.hasLinkLayerUuid()) 
            {
                final LinkLayer linkLayer = 
                        CustomCommUtility.getLinkLayerByUuid(m_CommsService, createTransportLayer.getLinkLayerUuid());
                linkLayerName = linkLayer.getName();
            }
            
            final TransportLayer createdTransportLayer = m_CommsService.createTransportLayer(transportLayerType,
                    transportLayerName, linkLayerName);

            final CreateTransportLayerResponseData response = CreateTransportLayerResponseData.newBuilder().
                    setInfo(SharedMessageUtils.createFactoryObjectInfoMessage(createdTransportLayer)).build();
            
            m_MessageFactory.createCustomCommsResponseMessage(request, 
                CustomCommsMessageType.CreateTransportLayerResponse, response).queue(channel);
        }
        catch (final CCommException e)
        {
            m_Logging.error(e, "CreateTransportLayer: Cannot create transport layer.");
            
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.CCOMM_ERROR, 
                    CUSTOM_COMMS_EXCEPTION + e.getMessage()).queue(channel);
        }
        return createTransportLayer;
    }

    /**
     * Method to create a new instance of a link layer and add it to the service.
     * @param ccommsMessage
     *      request message containing the link layer class name, associated physical link UUID, and name
     *      for creating a new link layer.
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed or response message cannot be sent
     */
    private Message createLinkLayer(final CustomCommsNamespace ccommsMessage, final TerraHarvestMessage request,
        final RemoteChannel channel) throws IOException
    {
        final CreateLinkLayerRequestData createLinkLayer = 
                CreateLinkLayerRequestData.parseFrom(ccommsMessage.getData());
        
        final String linkLayerClassName = createLinkLayer.getLinkLayerProductType();
        final String linkLayerName = createLinkLayer.getLinkLayerName();
        final Map<String, Object> properties = new HashMap<String, Object>();
        
        // may or may not have a physical link that the link layer uses
        if (createLinkLayer.hasPhysicalLinkUuid())
        {
            final UUID physicalLinkUuid = SharedMessageUtils.convertProtoUUIDtoUUID(
                    createLinkLayer.getPhysicalLinkUuid());
    
            final String physicalLinkName = m_CommsService.getPhysicalLinkName(physicalLinkUuid);
            properties.put(LinkLayerAttributes.CONFIG_PROP_PHYSICAL_LINK_NAME, physicalLinkName);
        }
        
        try
        {
            final LinkLayer createdLinkLayer = m_CommsService.createLinkLayer(linkLayerClassName, linkLayerName,
                    properties);

            final CreateLinkLayerResponseData response = CreateLinkLayerResponseData.newBuilder().
                    setInfo(SharedMessageUtils.createFactoryObjectInfoMessage(createdLinkLayer)).build();
            
            m_MessageFactory.createCustomCommsResponseMessage(request, CustomCommsMessageType.CreateLinkLayerResponse, 
                    response).queue(channel);
        }
        catch (final CCommException e)
        {
            m_Logging.error(e, "CreateLinkLayer: Cannot create link layer.");
            
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.CCOMM_ERROR, 
                            CUSTOM_COMMS_EXCEPTION + e.getMessage()).queue(channel);
        }
        return createLinkLayer;
    }

    /**
     * Method to create a new instance of a physical link and add it to the service.
     * @param ccommsMessage
     *      request message containing the physical link class name, and name for creating a new physical link.
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed or response message cannot be sent
     */
    private Message createPhysicalLink(final CustomCommsNamespace ccommsMessage, final TerraHarvestMessage request,
        final RemoteChannel channel) throws IOException
    {
        final CreatePhysicalLinkRequestData createPhysicalLink = 
                CreatePhysicalLinkRequestData.parseFrom(ccommsMessage.getData());
        
        final String physicalLinkName = createPhysicalLink.getPhysicalLinkName();
        final PhysicalLinkType.Enum physicalLinkType = createPhysicalLink.getPhysicalLinkType();
        
        try
        {
  
            final PhysicalLink plink = m_CommsService.createPhysicalLink(
                    PhysicalLinkTypeEnumConverter.convertProtoEnumToJava(physicalLinkType), physicalLinkName);
            final CreatePhysicalLinkResponseData.Builder responseBuilder = CreatePhysicalLinkResponseData.newBuilder().
                    setInfo(SharedMessageUtils.createFactoryObjectInfoMessage(plink));
             
            m_MessageFactory.createCustomCommsResponseMessage(request, 
                CustomCommsMessageType.CreatePhysicalLinkResponse, responseBuilder.build()).queue(channel);
        }
        catch (final CCommException e)
        {
            m_Logging.error(e, "CreatePhysicalLink: Cannot create physical link.");
            
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.CCOMM_ERROR, 
                            CUSTOM_COMMS_EXCEPTION + e.getMessage()).queue(channel);
        }
        return createPhysicalLink;
    }

    /**
     * Set the given layer's name.
     * @param ccommsMessage
     *      request message containing the request to set the name for a layer.
     * @param message
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed or response message cannot be sent
     */
    private Message setLayerName(final CustomCommsNamespace ccommsMessage, final TerraHarvestMessage message, 
        final RemoteChannel channel) throws IOException
    {
        final SetLayerNameRequestData request = SetLayerNameRequestData.parseFrom(ccommsMessage.getData());
        final SharedMessages.UUID layerUuid = request.getUuid();
        final CommType type = request.getCommType();
        final String name  = request.getLayerName();

        try
        {
            if (type.equals(CommType.TransportLayer))
            {
                final TransportLayer layer = CustomCommUtility.getTransportLayerByUuid(m_CommsService, layerUuid);
                layer.setName(name);
            }
            else if (type.equals(CommType.Linklayer))
            {
                final LinkLayer layer = CustomCommUtility.getLinkLayerByUuid(m_CommsService, layerUuid);
                layer.setName(name);
            }
            else if (type.equals(CommType.PhysicalLink))
            {
                m_CommsService.setPhysicalLinkName(SharedMessageUtils.convertProtoUUIDtoUUID(layerUuid), name);
            }
            else
            {
                final String errorDesc = String.format(INVALID_COMM_TYPE
                        + "Cannot get layer for unknown layer type [%s].", type);
                m_MessageFactory.createBaseErrorMessage(message, ErrorCode.INVALID_VALUE, errorDesc).queue(channel);
                m_Logging.error(errorDesc);
                return request;
            }
        }
        catch (final FactoryException e)
        {
            final String errorDesc = String.format("Unable to set the name for layer %s with UUID %s. %s", type, 
                SharedMessageUtils.convertProtoUUIDtoUUID(layerUuid).toString(), e.getMessage());
            m_MessageFactory.createBaseErrorMessage(message, ErrorCode.CCOMM_ERROR, errorDesc).queue(channel);
            return request;
        }
        m_MessageFactory.createCustomCommsResponseMessage(message, CustomCommsMessageType.SetLayerNameResponse, 
                null).queue(channel);
        return request;
    }

    /**
     * Get the given layer's name.
     * @param ccommsMessage
     *      request message containing the request to get the name for a layer.
     * @param message
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed or response message cannot be sent
     */
    private Message getLayerName(final CustomCommsNamespace ccommsMessage, final TerraHarvestMessage message, 
        final RemoteChannel channel) throws IOException
    {
        //parse request
        final GetLayerNameRequestData request = GetLayerNameRequestData.parseFrom(ccommsMessage.getData());
        final SharedMessages.UUID layerUuid = request.getUuid();
        final CommType type = request.getCommType();

        final String name;

        if (type.equals(CommType.TransportLayer))
        {
            final TransportLayer layer = CustomCommUtility.getTransportLayerByUuid(m_CommsService, layerUuid);
            name = layer.getName();
        }
        else if (type.equals(CommType.Linklayer))
        {
            final LinkLayer layer = CustomCommUtility.getLinkLayerByUuid(m_CommsService, layerUuid);
            name = layer.getName();
        }
        else if (type.equals(CommType.PhysicalLink))
        {

            name = m_CommsService.getPhysicalLinkName(SharedMessageUtils.convertProtoUUIDtoUUID(layerUuid));

        }
        else
        {
            final String errorDesc = String.format(INVALID_COMM_TYPE
                    + "Cannot get layer name for unknown layer type [%s].", type);
            m_MessageFactory.createBaseErrorMessage(message, ErrorCode.INVALID_VALUE, errorDesc).queue(channel);
            m_Logging.error(errorDesc);
            return request;
        }

        final GetLayerNameResponseData response = GetLayerNameResponseData.newBuilder().
             setLayerName(name).
             setUuid(layerUuid).
             setCommType(type).build();
        m_MessageFactory.createCustomCommsResponseMessage(message, CustomCommsMessageType.GetLayerNameResponse, 
                response).queue(channel);
        return request;
    }
}
