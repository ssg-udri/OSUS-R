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
package mil.dod.th.ose.remote.osgi;

import java.io.IOException;
import java.util.Arrays;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.Message;

import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.MetaTypeMessages.AttributeDefinitionType;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetAttributeDefinitionRequestData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetAttributeDefinitionResponseData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetAttributeKeysRequestData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetAttributeKeysResponseData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetBundlePidsRequestData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetBundlePidsResponseData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetMetaTypeInfoRequestData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetMetaTypeInfoResponseData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.MetaTypeInfoType;
import mil.dod.th.core.remote.proto.MetaTypeMessages.MetaTypeNamespace;
import mil.dod.th.core.remote.proto.MetaTypeMessages.MetaTypeNamespace.MetaTypeMessageType;
import mil.dod.th.core.remote.proto.MetaTypeMessages.ObjectClassDefinitionType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.MessageService;
import mil.dod.th.ose.remote.util.MetatypeInformationListener;
import mil.dod.th.ose.remote.util.RemoteInterfaceUtilities;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * Message service for the {@link MetaTypeNamespace} messages.
 * @author callen
 *
 */
//service is not provided as this would create a cycle, instead this class registers with the message router
@Component(immediate = true, provide = { }) //NOCHECKSTYLE: high fan out. 
                                            //Multiple proto message types are needed for this service.
public class MetatypeMessageService implements MessageService
{

    /**
     * Service for creating messages to send through the remote interface.
     */
    private MessageFactory m_MessageFactory;
    
    /**
     * Service that assists with getting information from bundles.
     */
    private MetaTypeService m_MetaTypeService;
    
    /**
     * Reference to the event admin service, used to post events that a message was received.
     */
    private EventAdmin m_EventAdmin;
    
    /**
     * Bundle context used to look up bundle symbolic names.
     */
    private BundleContext m_Context;
    
    /**
     * Routes incoming messages.
     */
    private MessageRouterInternal m_MessageRouter;

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
     * Bind the meta type service to use.
     * 
     * @param metaTypeService
     *      Meta type service that is now available
     */
    @Reference
    public void setMetaTypeService(final MetaTypeService metaTypeService)
    {
        m_MetaTypeService = metaTypeService;
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
     * Bind a metatype information listener service.
     * 
     * @param metatypeListener
     *      metatype information listener, argument required for OSGi binding method to define the service to bind
     */
    @Reference
    public void setMetatypeInformationListener(final MetatypeInformationListener metatypeListener)
    {
        //Only here as a way to ensure that the listener service is available before this service
        //begins operation.
    }
    
    /**
     * Activate method to save off system bundle for later use and bind this service to the message router.
     * 
     * @param context
     *      context for this bundle
     */
    @Activate
    public void activate(final BundleContext context)
    {
        m_Context = context;

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
        return Namespace.MetaType;
    }
    
    @Override
    public void handleMessage(final TerraHarvestMessage message, final TerraHarvestPayload payload, 
        final RemoteChannel channel) throws IOException  
    {
        //parse meta type message
        final MetaTypeNamespace metaMessage = MetaTypeNamespace.parseFrom(payload.getNamespaceMessage());
        //data message
        final Message dataMessage;

        switch (metaMessage.getType())
        {
            case GetBundlePidsRequest:
                dataMessage = getBundlePids(message, metaMessage, channel);
                break;
            case GetBundlePidsResponse:
                dataMessage = GetBundlePidsResponseData.parseFrom(metaMessage.getData());
                break;
            case GetAttributeDefinitionRequest:
                dataMessage = getAttributeDefinition(message, metaMessage, channel);
                break;            
            case GetAttributeDefinitionResponse:
                dataMessage = GetAttributeDefinitionResponseData.parseFrom(metaMessage.getData());
                break;
            case GetAttributeKeyRequest:
                dataMessage = getAttributeKeys(message, metaMessage, channel);
                break;
            case GetAttributeKeyResponse:
                dataMessage = GetAttributeKeysResponseData.parseFrom(metaMessage.getData());
                break;
            case GetMetaTypeInfoRequest:
                dataMessage = getMetatypeInformation(message, metaMessage, channel);
                break;
            case GetMetaTypeInfoResponse:
                dataMessage = GetMetaTypeInfoResponseData.parseFrom(metaMessage.getData());
                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Message type: %s is not a supported type for"
                                + " the MetaTypeMessageService namespace.", metaMessage.getType()));
        }
        
        //locally post event that message was received
        final Event event = RemoteInterfaceUtilities.createMessageReceivedEvent(message, payload, metaMessage, 
            metaMessage.getType(), dataMessage, channel);
        m_EventAdmin.postEvent(event);
    }
    
    /**
     * Handle the get bundle pids request. 
     * 
     * @param request
     *      entire remote message for the request
     * @param metaMessage
     *     the message that contains the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException
     *     thrown if the message cannot be parsed correctly
     */
    private Message getBundlePids(final TerraHarvestMessage request, final MetaTypeNamespace metaMessage, 
            final RemoteChannel channel) throws IOException
    {
        //parse request from message
        final GetBundlePidsRequestData requestData = GetBundlePidsRequestData.parseFrom(metaMessage.getData());
        
        //message to send as response
        final GetBundlePidsResponseData.Builder responseBuilder = 
                GetBundlePidsResponseData.newBuilder().setBundleId(requestData.getBundleId());
        
        //try to get the bundle with the name from the message
        final Bundle bundle = getBundleById(requestData.getBundleId());
        if (bundle == null)
        {
            //notify sender
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.INVALID_VALUE,
                String.format("The bundle with id %d was not found.", requestData.getBundleId())).queue(channel);
            return requestData;
        }
        final MetaTypeInformation info = m_MetaTypeService.getMetaTypeInformation(bundle);
        if (info != null)
        {
            responseBuilder.addAllPids(Arrays.asList(info.getPids()));
            responseBuilder.addAllFactoryPids(Arrays.asList(info.getFactoryPids()));
        }
        //send the response
        final GetBundlePidsResponseData response = responseBuilder.build();
        m_MessageFactory.createMetaTypeResponseMessage(request, MetaTypeMessageType.GetBundlePidsResponse, 
                response).queue(channel);
        return request;
    }
    
    /**
     * Handle the get attribute definition request.
     * 
     * @param request
     *      entire remote message for the request
     * @param metaMessage
     *     the message that contains the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException
     *     thrown if the message cannot be parsed correctly 
     */
    private Message getAttributeDefinition(final TerraHarvestMessage request, final MetaTypeNamespace metaMessage, 
            final RemoteChannel channel) throws IOException
    {
        final GetAttributeDefinitionRequestData requestData = GetAttributeDefinitionRequestData.parseFrom(
            metaMessage.getData());
       
        // key not in dictionary get default
        final AttributeDefinition[] attribs = getAttributes(requestData.getBundleId(), requestData.getPid(),
                ObjectClassDefinition.ALL);
        //check that attributes isn't null, if null and there is a key then send error
        if (attribs == null)
        {
            final StringBuilder builder = new StringBuilder(
                    "The bundle id %d, and pid %s did not return any attribute definitions");
            
            final String errMsg;
            if (requestData.hasKey())
            {
                builder.append(", key %s was ignored.");
                errMsg = String.format(builder.toString(), 
                        requestData.getBundleId(), requestData.getPid(), requestData.getKey());
            }
            else
            {
                builder.append(".");
                errMsg = String.format(builder.toString(), requestData.getBundleId(), requestData.getPid());
            }
            
            //send error
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.INVALID_VALUE, errMsg).queue(channel);

            //nothing left to do
            return requestData;
        }
        
         //builder for response message
        final GetAttributeDefinitionResponseData.Builder responseBuilder = GetAttributeDefinitionResponseData.
            newBuilder().setPid(requestData.getPid()).setBundleId(requestData.getBundleId());
        
        //iterate over required attributes and pull out the keys
        final AttributeDefinition[] attribsReq = getAttributes(requestData.getBundleId(), requestData.getPid(),
                ObjectClassDefinition.REQUIRED);
        if (attribsReq != null)
        {
            addAttributesForKey(responseBuilder, requestData.getKey(), attribsReq, ObjectClassDefinition.REQUIRED);
        }
        //iterate over optional attributes and pull out the keys
        final AttributeDefinition[] attribsOpt = getAttributes(requestData.getBundleId(), requestData.getPid(),
                ObjectClassDefinition.OPTIONAL);
        if (attribsOpt != null)
        {
            addAttributesForKey(responseBuilder, requestData.getKey(), attribsOpt, ObjectClassDefinition.OPTIONAL);
        }
        
        //construct response
        final GetAttributeDefinitionResponseData response;
        //check that if a single attribute was requested that it was returned, if it wasn't send error message
        if (requestData.hasKey() && responseBuilder.getAttributeDefinitionCount() == 0)
        {
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.INVALID_VALUE,
                String.format("Attribute definitions were found for the bundle id %d, and "
                    + "pid %s, but the attribute matching the key %s was not found.", requestData.getBundleId(), 
                    requestData.getPid(), requestData.getKey())).queue(channel);
        }
        else
        {
            response = responseBuilder.build();
            m_MessageFactory.createMetaTypeResponseMessage(request, MetaTypeMessageType.GetAttributeDefinitionResponse,
                    response).queue(channel);
        }
        return requestData;
    }
    
    /**
     * Adds attributes to the attribute definition response based on the key provided.
     * 
     * @param response
     *      The response builder the attributes should be added to.
     * @param key
     *      The key that represents the attributes that should be added. If the key is null all attributes in the array
     *      will be added to the response.
     * @param attributes
     *      The array of attributes to be iterated over.
     * @param filter
     *      The filter used to determine whether the attribute definitions are required or not. Valid values are 
     *      {@link ObjectClassDefinition#REQUIRED}, and {@link ObjectClassDefinition#OPTIONAL}.
     */
    private void addAttributesForKey(final GetAttributeDefinitionResponseData.Builder response, final String key, 
            final AttributeDefinition[] attributes, final int filter)
    {
        for (AttributeDefinition attrib : attributes)
        {
            //get the attribute definition type
            final AttributeDefinitionType attributeDef = convertToAttribDefType(attrib, filter);
            if (key == null || key.isEmpty())
            {
                //if there is not a key set add all definitions
                response.addAttributeDefinition(attributeDef);
            }
            else if (attrib.getID().equals(key))
            {
                //only one definition was being looked for
                response.addAttributeDefinition(attributeDef);
            }
        }
    }
    
    /**
     * Handle the get attribute keys request.
     * 
     * @param request
     *      entire remote message for the request
     * @param metaMessage
     *     the message that contains the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException
     *     thrown if the message cannot be parsed correctly    
     */
    private Message getAttributeKeys(final TerraHarvestMessage request, final MetaTypeNamespace metaMessage, 
            final RemoteChannel channel) throws IOException
    {
        //parse the message
        final GetAttributeKeysRequestData requestData = GetAttributeKeysRequestData.parseFrom(metaMessage.getData());
        
        //response
        final GetAttributeKeysResponseData.Builder responseBuilder = GetAttributeKeysResponseData.newBuilder().
                setPid(requestData.getPid()).setBundleId(requestData.getBundleId());
        
        //get the attributes
        final AttributeDefinition[] attributes = getAttributes(requestData.getBundleId(), requestData.getPid(), 
                ObjectClassDefinition.ALL);
        //check if attributes is null
        if (attributes == null)
        {
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.INVALID_VALUE, 
                String.format("Attribute definitions were NOT found for the bundle id %d, and pid %s.", 
                    requestData.getBundleId(), requestData.getPid())).queue(channel);
            return requestData;
        }
        //iterate over attributes and pull out the keys
        for (AttributeDefinition attrib : attributes)
        {
            responseBuilder.addKey(attrib.getID());   
        }
        //send response
        final GetAttributeKeysResponseData response = responseBuilder.build();
        m_MessageFactory.createMetaTypeResponseMessage(request, MetaTypeMessageType.GetAttributeKeyResponse, 
                response).queue(channel);
        return requestData;
    }
    
    /**
     * Handle the get metatype information request.
     * 
     * @param request
     *      entire remote message for the request
     * @param metaMessage
     *      the message that contains the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException
     *      thrown if the message cannot be parsed correctly
     */
    private Message getMetatypeInformation(final TerraHarvestMessage request, final MetaTypeNamespace metaMessage, 
            final RemoteChannel channel) throws IOException
    {
        //parse the message
        final GetMetaTypeInfoRequestData requestData = GetMetaTypeInfoRequestData.parseFrom(metaMessage.getData());
        
        final Bundle[] bundles;
        if (requestData.hasBundleId())
        {
            final Bundle specificBundle = getBundleById(requestData.getBundleId());
            if (specificBundle == null)
            {
                m_MessageFactory.createBaseErrorMessage(request, ErrorCode.INVALID_VALUE, 
                        String.format("Metatype information for bundle with id %d could not be retrieved because the " 
                                + "bundle could not be found.", requestData.getBundleId())).queue(channel);
                return requestData;
            }
            bundles = new Bundle[] {specificBundle};
        }
        else
        {
            bundles = m_Context.getBundles();
        }
        
        final GetMetaTypeInfoResponseData.Builder responseData = GetMetaTypeInfoResponseData.newBuilder();
        for (Bundle bundle: bundles)
        {
            final MetaTypeInformation info = m_MetaTypeService.getMetaTypeInformation(bundle);
            if (info != null)
            {
                final String[] pids = info.getPids();
                final String[] factoryPids = info.getFactoryPids();
                //Create meta type information for all standard PIDs.
                for (String pid: pids)
                {
                    final MetaTypeInfoType metaInfo = buildMetaTypeInfo(bundle, info, pid, false);
                    responseData.addMetaType(metaInfo);
                }
                //Create meta type information for all factory PIDs.
                for (String factoryPid: factoryPids)
                {
                    final MetaTypeInfoType metaInfo = buildMetaTypeInfo(bundle, info, factoryPid, true);
                    responseData.addMetaType(metaInfo);
                }
            }
        }
        m_MessageFactory.createMetaTypeResponseMessage(request, MetaTypeMessageType.GetMetaTypeInfoResponse, 
                responseData.build()).queue(channel);
        return requestData;
    }
    
    /**
     * Method that creates protocol buffer representation of meta type information for the specified PID.
     * 
     * @param bundle
     *      The bundle that contains meta type information for the specified PID.
     * @param info
     *      Meta type information for the specified PID.
     * @param pid
     *      PID to build protocol buffer meta type information for.
     * @param isFactory
     *      Boolean used to determine if the PID represents a factory.
     * @return
     *      Protocol buffer representation of the meta type information for the specified PID.
     */
    private MetaTypeInfoType buildMetaTypeInfo(final Bundle bundle, final MetaTypeInformation info, final String pid, 
            final boolean isFactory)
    {
        final MetaTypeInfoType.Builder metaInfo = MetaTypeInfoType.newBuilder();
        metaInfo.setBundleId(bundle.getBundleId()).setPid(pid).setIsFactory(isFactory);
        final ObjectClassDefinition ocd = info.getObjectClassDefinition(pid, null);
        metaInfo.setOcd(getOcdMessage(ocd));
        final AttributeDefinition[] attribsReq = ocd.getAttributeDefinitions(ObjectClassDefinition.REQUIRED);
        final AttributeDefinition[] attribsOpt = ocd.getAttributeDefinitions(ObjectClassDefinition.OPTIONAL);
        if (attribsReq != null)
        {
            //iterate over required attributes
            for (AttributeDefinition attribute : attribsReq)
            {
                //get the attribute definition type
                final AttributeDefinitionType attributeDef = 
                        convertToAttribDefType(attribute, ObjectClassDefinition.REQUIRED);
                metaInfo.addAttributes(attributeDef);
            }
        }
        if (attribsOpt != null)
        {
            //iterate over optional attributes
            for (AttributeDefinition attribute : attribsOpt)
            {
                //get the attribute definition type
                final AttributeDefinitionType attributeDef = 
                        convertToAttribDefType(attribute, ObjectClassDefinition.OPTIONAL);
                metaInfo.addAttributes(attributeDef);
            }
        }
        return metaInfo.build();
    }
    
    /**
     * Build an {@link ObjectClassDefinitionType} message from the given {@link ObjectClassDefinition}.
     * @param ocd
     *      the OCD to base the message on
     * @return
     *      complete message with applicable fields set based on the given OCD
     */
    private ObjectClassDefinitionType getOcdMessage(final ObjectClassDefinition ocd)
    {
        final ObjectClassDefinitionType.Builder ocdMessage = ObjectClassDefinitionType.newBuilder();
        ocdMessage.setName(ocd.getName());
        ocdMessage.setId(ocd.getID());
        if (ocd.getDescription() != null)
        {
            ocdMessage.setDescription(ocd.getDescription()).build();
        }
        return ocdMessage.build();
    }

    /**
     * Get a bundle by its bundle id.
     * 
     * @param bundleId
     *      id that uniquely identifies a bundle
     * @return
     *      Bundle object for the given bundle id
     */
    private Bundle getBundleById(final long bundleId)
    {
        for (Bundle bundle : m_Context.getBundles())
        {
            final Long bundleFromListId = bundle.getBundleId();
            if (bundleFromListId == bundleId)
            {
                return bundle;
            }
        }
        //receiver checks for null
        return null;
    }
    
    /**
     * Get the attribute definitions for the given PID.
     * 
     * @param bundleId
     *      id of the bundle containing meta data
     * @param pid
     *      persistent identifier associated with the attributes
     * @param filter
     *      filter used to determine the attribute definitions to be retrieved. Valid values are 
     *      {@link ObjectClassDefinition#ALL}, {@link ObjectClassDefinition#REQUIRED}, and 
     *      {@link ObjectClassDefinition#OPTIONAL}.
     * @return
     *      Attribute definitions for the given PID or null if not available
     */
    private AttributeDefinition[] getAttributes(final long bundleId, final String pid, final int filter) 
    {
        final Bundle bundle = getBundleById(bundleId);
        if (bundle == null)
        {
            //nothing to check
            return null;
        }
        final MetaTypeInformation info = m_MetaTypeService.getMetaTypeInformation(bundle);
        
        final ObjectClassDefinition ocd = info.getObjectClassDefinition(pid, null);
        if (ocd == null)
        {
            //receiver checks for null
            return null;
        }   
        return ocd.getAttributeDefinitions(filter);
    }
    
    /**
     * Get an attribute definition type proto message from the attribute values.
     * @param attribute
     *     The {@link AttributeDefinition} to be converted to an {@link AttributeDefinitionType}.
     * @param filter
     *     The filter used to determine whether the attribute definitions are required or not. Valid values are 
     *      {@link ObjectClassDefinition#REQUIRED}, and {@link ObjectClassDefinition#OPTIONAL}. 
     * @return
     *     a built {@link AttributeDefinitionType} message
     */
    private AttributeDefinitionType convertToAttribDefType(final AttributeDefinition attribute, final int filter)
    {
        //attribute definition message
        final AttributeDefinitionType.Builder attribBuilder = AttributeDefinitionType.newBuilder();
        //set values to the current attribute values
        attribBuilder.setCardinality(attribute.getCardinality());
        attribBuilder.setId(attribute.getID());
        attribBuilder.setName(attribute.getName());
        attribBuilder.setAttributeType(attribute.getType());
        
        //Description is not a required field so check for null.
        if (attribute.getDescription() != null)
        {
            attribBuilder.setDescription(attribute.getDescription());
        }
        
        //Set whether the field is required or optional.
        if (filter == ObjectClassDefinition.REQUIRED)
        {
            attribBuilder.setRequired(true);
        }
        else
        {
            attribBuilder.setRequired(false);
        }
        
        //Fill in repeated values.
        if (attribute.getDefaultValue() != null)
        {
            attribBuilder.addAllDefaultValue(Arrays.asList(attribute.getDefaultValue()));
        }        
        if (attribute.getOptionLabels() != null)
        {
            attribBuilder.addAllOptionLabel(Arrays.asList(attribute.getOptionLabels()));
        }
        if (attribute.getOptionValues() != null)
        {
            attribBuilder.addAllOptionValue(Arrays.asList(attribute.getOptionValues()));
        }
        return attribBuilder.build();
    }
}
