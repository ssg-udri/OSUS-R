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
package mil.dod.th.ose.remote.mp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.Message;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.mp.EventHandlerHelper;
import mil.dod.th.core.mp.ManagedExecutors;
import mil.dod.th.core.mp.MissionProgramException;
import mil.dod.th.core.mp.MissionProgramManager;
import mil.dod.th.core.mp.MissionScript.TestResult;
import mil.dod.th.core.mp.Program;
import mil.dod.th.core.mp.Program.ProgramStatus;
import mil.dod.th.core.mp.TemplateProgramManager;
import mil.dod.th.core.mp.model.MissionProgramParameters;
import mil.dod.th.core.mp.model.MissionProgramSchedule;
import mil.dod.th.core.mp.model.MissionProgramTemplate;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.MissionProgramMessages.CancelProgramRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.CancelProgramResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ExecuteRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ExecuteResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ExecuteShutdownRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ExecuteShutdownResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ExecuteTestRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ExecuteTestResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetExecutionParametersRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetExecutionParametersResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetLastTestResultsRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetLastTestResultsResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetProgramInformationRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetProgramInformationResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetProgramNamesResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetProgramStatusRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetProgramStatusResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetTemplateNamesResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetTemplatesResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.LoadParametersRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.LoadTemplateRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ManagedExecutorsShutdownRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace.MissionProgrammingMessageType;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ProgramInfo;
import mil.dod.th.core.remote.proto.MissionProgramMessages.RemoveMissionProgramRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.RemoveMissionProgramResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.RemoveTemplateRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.MessageService;
import mil.dod.th.ose.remote.api.EnumConverter;
import mil.dod.th.ose.remote.util.RemoteInterfaceUtilities;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.lexicon.mp.model.MissionProgramParametersGen;
import mil.dod.th.remote.lexicon.mp.model.MissionProgramTemplateGen;
import mil.dod.th.remote.lexicon.types.SharedTypesGen.MapEntry;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * This service handles all messages for the MissionProgramming namespace. 
 * 
 * @author callen
 *
 */
//service is not provided as this would create a cycle, instead this class registers with the message router
@Component(immediate = true, provide = { }) //NOCHECKSTYLE: High class fan out complexity, need to be able to execute
//mission programs from a separate thread, therefore there is an inner class to assist with that
public class MissionProgrammingMessageService implements MessageService //NOPMD avoid really long classes: all mission
{ //programming service are handled by this message service therefore multiple classes and messages are interacted with
    /**
     * {@link LoggingService} logging service.
     */
    private LoggingService m_Logging;
    
    /**
     * Mission program manager service that manages {@link Program}s.
     */
    private MissionProgramManager m_Manager;
    
    /**
     * Service that manages mission program templates.
     */
    private TemplateProgramManager m_TemplateManager;
    
    /**
     * Service for creating messages to send through the remote interface.
     */
    private MessageFactory m_MessageFactory;
    
    /**
     * Service to use for posting events generated by this service.
     */
    private EventAdmin m_EventAdmin;
    
    /**
     * Service that assists in converting instances of proto messages to jaxb objects.
     */
    private JaxbProtoObjectConverter m_Converter;
    
    /**
     * Routes incoming messages.
     */
    private MessageRouterInternal m_MessageRouter;
    
    /**
     * Managed executor service.
     */
    private ManagedExecutors m_ManagedExecutor;
    
    /**
     * Event handler helper service.
     */
    private EventHandlerHelper m_EventHandlerHelper;

    /**
     * Bind the {@link MissionProgramManager} service.
     * 
     * @param missionProgramManager
     *     service for managing mission programs
     * 
     */
    @Reference
    public void setMissionProgramManager(final MissionProgramManager missionProgramManager)
    {
        m_Manager = missionProgramManager;
    }
    
    /**
     * Bind the {@link TemplateProgramManager} service.
     * 
     * @param templateProgramManager
     *     service the manages template mission programs
     */
    @Reference
    public void setTemplateProgramManager(final TemplateProgramManager templateProgramManager)
    {
        m_TemplateManager = templateProgramManager;
    }
    
    /**
     * Bind the {@link EventAdmin} service.
     * 
     * @param eventAdmin
     *      event admin service used to post events
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
     * Bind the managed executor service.
     * 
     * @param executors
     *      the executor service
     */
    @Reference
    public void setManagedExecutorService(final ManagedExecutors executors)
    {
        m_ManagedExecutor = executors;
    }
    
    /**
     * Bind the event handler helper service.
     * 
     * @param eventHelper
     *      the event handler helper service
     */
    @Reference
    public void setEventHelperService(final EventHandlerHelper eventHelper)
    {
        m_EventHandlerHelper = eventHelper;
    }
    
    /**
     * Activate method creates the {@link JaxbProtoObjectConverter} instances and bind this service to the message 
     * router.
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
        return Namespace.MissionProgramming;
    }

    @Override //NOCHECKSTYLE high cyclomatic complexity this is a switch statement that needs to be able to route
    //all mission program messages
    public void handleMessage(final TerraHarvestMessage message, final TerraHarvestPayload payload, //NOPMD long method
        final RemoteChannel channel) throws IOException,              //need to route all mission program messages
            IllegalArgumentException
    {
        
        final MissionProgrammingNamespace missionMessage = 
            MissionProgrammingNamespace.parseFrom(payload.getNamespaceMessage());
        final Message dataMessage;
        
        // handle according to the message sent
        switch (missionMessage.getType())
        {
            case LoadTemplateRequest:
                //load the mission program template
                dataMessage = loadTemplate(message, missionMessage, channel);
                break;
            case LoadTemplateResponse:
                dataMessage = null;
                break;
            case LoadParametersRequest:
                dataMessage = loadParameters(message, missionMessage, channel);
                break;
            case LoadParametersResponse:
                dataMessage = null;
                break;
            case RemoveTemplateRequest:
                dataMessage = removeTemplate(message, missionMessage, channel);
                break;
            case RemoveTemplateResponse:
                dataMessage = null;
                break;
            case GetTemplatesRequest:
                dataMessage = null;
                getTemplates(message, channel);
                break;
            case GetTemplatesResponse:
                dataMessage = GetTemplatesResponseData.parseFrom(missionMessage.getData());
                break;
            case ExecuteTestRequest:
                dataMessage = executeTest(message, missionMessage, channel);
                break;
            case ExecuteTestResponse:
                dataMessage = ExecuteTestResponseData.parseFrom(missionMessage.getData());
                break;
            case ExecuteRequest:
                dataMessage = execute(message, missionMessage, channel);
                break;
            case ExecuteResponse:
                dataMessage = ExecuteResponseData.parseFrom(missionMessage.getData());
                break;
            case ExecuteShutdownRequest:
                dataMessage = executeShutdown(message, missionMessage, channel);
                break;
            case ExecuteShutdownResponse:
                dataMessage = ExecuteShutdownResponseData.parseFrom(missionMessage.getData());
                break;
            case GetProgramNamesRequest:
                dataMessage = null;
                getProgramNames(message, channel);
                break;
            case GetProgramNamesResponse:
                dataMessage = GetProgramNamesResponseData.parseFrom(missionMessage.getData());
                break;
            case GetTemplateNamesRequest:
                dataMessage = null;
                getTemplateNames(message, channel);
                break;
            case GetTemplateNamesResponse:
                dataMessage = GetTemplateNamesResponseData.parseFrom(missionMessage.getData());
                break;
            case GetProgramStatusRequest:
                dataMessage = getProgramStatus(message, missionMessage, channel);
                break;
            case GetProgramStatusResponse:
                dataMessage = GetProgramStatusResponseData.parseFrom(missionMessage.getData());
                break;
            case GetLastTestResultsRequest:
                dataMessage = getLastTestResults(message, missionMessage, channel);
                break;
            case GetLastTestResultsResponse:
                dataMessage = GetLastTestResultsResponseData.parseFrom(missionMessage.getData());
                break;
            case GetExecutionParametersRequest:
                dataMessage = getExecutionParameters(message, missionMessage, channel);
                break;
            case GetExecutionParametersResponse:
                dataMessage = GetExecutionParametersResponseData.parseFrom(missionMessage.getData());
                break;
            case CancelProgramRequest:
                dataMessage = cancelProgram(message, missionMessage, channel);
                break;
            case CancelProgramResponse:
                dataMessage = CancelProgramResponseData.parseFrom(missionMessage.getData());
                break;            
            case GetProgramInformationRequest:
                dataMessage = getProgramData(message, missionMessage, channel);
                break;
            case GetProgramInformationResponse:
                dataMessage = GetProgramInformationResponseData.parseFrom(missionMessage.getData());
                break;
            case RemoveMissionProgramRequest:
                dataMessage = removeMissionProgram(message, missionMessage, channel);
                break;
            case RemoveMissionProgramResponse:
                dataMessage = RemoveMissionProgramResponseData.parseFrom(missionMessage.getData());
                break;
            case ManagedExecutorsShutdownRequest:
                dataMessage = shutdownExecutors(message, missionMessage, channel);
                break;
            case ManagedExecutorsShutdownResponse:
                dataMessage = null;
                break;
            case UnregisterEventHandlerRequest:
                dataMessage = null;
                unregisterHandler(message, channel);
                break;
            case UnregisterEventHandlerResponse:
                dataMessage = null;
                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Message type: %s is not a supported type for"
                                + " the MissionProgrammingMessageService namespace.", missionMessage.getType()));
        }
        
         // post event that MissionProgramming message was received
        final Event event = RemoteInterfaceUtilities.createMessageReceivedEvent(message, payload, missionMessage, 
                missionMessage.getType(), dataMessage, channel);
        m_EventAdmin.postEvent(event);        
    }

    /**
     * Add the passed template to the TemplateProgramManager.
     *
     * @param request
     *     full request message
     * @param missionMessage
     *     message containing a mission program message
     * @param channel
     *      channel to use for sending a response
     * @throws IOException
     *     if the response fails to send
     * @throws IllegalArgumentException
     *     thrown in the event that one of inputs is invalid    
     * @return
     *      the data message for this request 
     */
    private Message loadTemplate(final TerraHarvestMessage request, final MissionProgrammingNamespace missionMessage,
            final RemoteChannel channel) 
            throws IOException, IllegalArgumentException
    {
        //get data from the data field
        final LoadTemplateRequestData progTemplate = LoadTemplateRequestData.parseFrom(missionMessage.getData());
        MissionProgramTemplate template = null;
        try
        {
            //convert the message data to a mission program template
            template = (MissionProgramTemplate)m_Converter.convertToJaxb(progTemplate.getMission());
            m_TemplateManager.loadMissionTemplate(template);
            //send response
            m_MessageFactory.createMissionProgrammingResponseMessage(request, 
                    MissionProgrammingMessageType.LoadTemplateResponse, null).queue(channel);
        }
        catch (final ObjectConverterException exception) 
        {
            //if the jaxb converter was unable to create an object
            //send response
            final String errorDesc = "Failed to create template object from message. ";
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.CONVERTER_ERROR,
                    errorDesc + exception.getMessage()).queue(channel);
            m_Logging.error(exception, errorDesc);
        }
        catch (final PersistenceFailedException exception) 
        {
            //template failed to persist, send appropriate response
            final String errorDesc = "Failed persisting the mission template from message. ";
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.PERSIST_ERROR,
                    errorDesc + exception.getMessage()).queue(channel);
            m_Logging.error(exception, errorDesc);
        }
        
        return progTemplate;
    }
    
    /**
     * Add parameters and a schedule to {@link MissionProgramManager}.
     * 
     * @param request
     *     full request message
     * @param message
     *    message that includes parameters, a schedule, and the name of the template to use
     * @param channel
     *      channel to use for sending a response
     * @throws IOException
     *    if the response fails to be sent
     * @return
     *      the data message for this request 
     */
    private Message loadParameters(final TerraHarvestMessage request, final MissionProgrammingNamespace message,
            final RemoteChannel channel) 
            throws IOException
    {
        //get the parameter data
        final LoadParametersRequestData params = LoadParametersRequestData.parseFrom(message.getData());
        try
        {
            //convert the data to a MissionProgramTemplate            
            final MissionProgramParameters parameters = 
                (MissionProgramParameters)m_Converter.convertToJaxb(params.getParameters());
            m_Manager.loadParameters(parameters);
            //if all of the above is successful send response, message is null as there is no data
            m_MessageFactory.createMissionProgrammingResponseMessage(request, 
                    MissionProgrammingMessageType.LoadParametersResponse, null).queue(channel);
        }
        catch (final ObjectConverterException exception) 
        {
            //if the jaxb converter was unable to create an object
            final String errorDesc = "Failed to create parameters object from message. ";
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.CONVERTER_ERROR,
                errorDesc + exception.getMessage()).queue(channel);
            m_Logging.error(exception, errorDesc);
        }
        catch (final PersistenceFailedException exception)
        {
            //if the jaxb converter was unable to create an object
            final String errorDesc = "Failed to persist parameters from message. ";
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.PERSIST_ERROR,
                    errorDesc + exception.getMessage()).queue(channel);
            m_Logging.error(exception, errorDesc);
        }
        return params;
    }
    
    /**
     * Remove the {@link MissionProgramTemplate} with the specified name.
     * 
     * @param request
     *     full request message
     * @param message
     *    message that contains the name of the template to remove
     * @param channel
     *      channel to use for sending a response
     * @throws IOException
     *    thrown if an error occurs sending the response 
     * @return
     *      the data message for this request 
     */    
    private Message removeTemplate(final TerraHarvestMessage request, final MissionProgrammingNamespace message,
            final RemoteChannel channel) 
            throws IOException
    {
        final RemoveTemplateRequestData removeMessage = RemoveTemplateRequestData.parseFrom(message.getData());
        m_TemplateManager.removeTemplate(removeMessage.getNameOfTemplate());
        m_MessageFactory.createMissionProgrammingResponseMessage(request, 
                MissionProgrammingMessageType.RemoveTemplateResponse, null).queue(channel);
        
        return removeMessage;
    }
    
    /**
     * Handle the request to get all templates from this system.
     * 
     * @param request
     *     full request message
     * @param channel
     *      channel to use for sending a response
     * @throws IOException
     *     thrown in the event that an error is experienced while attempting to send the response message
     */
    private void getTemplates(final TerraHarvestMessage request, final RemoteChannel channel) throws IOException
    {
        //construct response
        final GetTemplatesResponseData.Builder responseBuilder = GetTemplatesResponseData.newBuilder();
        //list of templates from this system
        final Set<MissionProgramTemplate> templates = m_TemplateManager.getTemplates();
        //iterate over the set of templates transforming each template that is in the set, and adding it to the response
        for (MissionProgramTemplate template : templates)
        {
            //try to convert the template to a proto template
            try 
            {
                final MissionProgramTemplateGen.MissionProgramTemplate protoTemplate = (MissionProgramTemplateGen.
                    MissionProgramTemplate)m_Converter.convertToProto(template);
                //add to response, adding here to prevent null being placed into the message
                responseBuilder.addTemplate(protoTemplate);
            }
            catch (final ObjectConverterException exception)
            {
                m_Logging.error(exception, "Unable to convert template %s into a proto message", template.getName());
            }
        }
        //if there are no names in the fail list, then send templates
        m_MessageFactory.createMissionProgrammingResponseMessage(request, 
                MissionProgrammingMessageType.GetTemplatesResponse, responseBuilder.build()).queue(channel);
    }

    /**
     * Handle the request execute a test function on a program.
     * 
     * @param request
     *     full request message
     * @param missionMessage
     *     message containing a mission program message
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request 
     * @throws IOException
     *     thrown in the event that an error is experienced while attempting to send the response message
     */
    private Message executeTest(final TerraHarvestMessage request, final MissionProgrammingNamespace missionMessage, 
         final RemoteChannel channel) throws IOException
    {
        //parse request
        final ExecuteTestRequestData requestMessage = ExecuteTestRequestData.parseFrom(missionMessage.getData());

        //get the program
        final Program program = m_Manager.getProgram(requestMessage.getMissionName());

        final String errorStringPrefix = "Mission program [%s] was requested to execute its test function, "; 
        try
        {
            program.executeTest();
        }
        catch (final UnsupportedOperationException e)
        {
            //return error
            final String errorDesc = String.format(errorStringPrefix + "but it does not support this ability.", 
                    program.getProgramName());
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.INTERNAL_ERROR,
                errorDesc).queue(channel);
            m_Logging.error(e, errorDesc);
            return requestMessage;
        }
        catch (final IllegalStateException e)
        {
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.INTERNAL_ERROR,
                    e.getMessage()).queue(channel);
            m_Logging.error(e, "The program's dependencies are not fulfilled when the request was made.");
            return requestMessage;
        }
        catch (final MissionProgramException e) 
        { 
            throw new IllegalStateException(e);
        }
        
        final ExecuteTestResponseData response = ExecuteTestResponseData.newBuilder().
                setMissionName(requestMessage.getMissionName()).build();
        m_MessageFactory.createMissionProgrammingResponseMessage(
                request, MissionProgrammingMessageType.ExecuteTestResponse, response).queue(channel);

        return requestMessage;
    }

    /**
     * Handle the request to execute a mission program.
     * @param message
     *     original request message that contains all information needed to complete the request
     * @param missionMessage
     *     the mission program message that contains data about what program to execute
     * @param channel
     *     the channel from which the request was received
     * @return
     *     the parsed data message
     * @throws IOException
     *     thrown in the event that an error is experienced while attempting to send the response message 
     */
    private Message execute(final TerraHarvestMessage message, final MissionProgrammingNamespace missionMessage,
            final RemoteChannel channel) throws IOException
    {
        //parse the request message
        final ExecuteRequestData request = ExecuteRequestData.parseFrom(missionMessage.getData());
        
        //get the program name
        final String programName = request.getMissionName();
        
        //get the program
        final Program program = m_Manager.getProgram(programName);
        
        //try to execute the program, happens on a different thread
        try
        {
            program.execute();
        }
        catch (final IllegalStateException e)
        {
            m_MessageFactory.createBaseErrorMessage(message, ErrorCode.INTERNAL_ERROR,
                    e.getMessage()).queue(channel);
            m_Logging.error(e, "Remote mission shutdown error.");
            return request;
        }
        catch (final MissionProgramException e)
        { 
            throw new IllegalStateException(e);
        }
        
        final ExecuteResponseData response = ExecuteResponseData.newBuilder().setMissionName(programName).build();
        m_MessageFactory.createMissionProgrammingResponseMessage(
                message, MissionProgrammingMessageType.ExecuteResponse, response).queue(channel);
        return request;
    }

    /**
     * Handle request for a mission to be shutdown.
     * @param message
     *     the complete request
     * @param missionMessage
     *     the mission program specific message that contains the name of the program to shutdown
     * @param channel
     *     the channel from which the request was received
     * @return
     *     the parsed data message
     * @throws IOException
     *     thrown in the event that an error is experienced while attempting to send the response message 
     */
    private Message executeShutdown(final TerraHarvestMessage message, final MissionProgrammingNamespace missionMessage,
            final RemoteChannel channel) throws IOException
    {
        //parse the request message
        final ExecuteShutdownRequestData request = ExecuteShutdownRequestData.parseFrom(missionMessage.getData());
        
        //get the program name
        final String programName = request.getMissionName();
        
         //get the program
        final Program program = m_Manager.getProgram(programName);
        
        program.executeShutdown();
        
        final ExecuteShutdownResponseData response = ExecuteShutdownResponseData.newBuilder().
                setMissionName(programName).build();
        m_MessageFactory.createMissionProgrammingResponseMessage(
                message, MissionProgrammingMessageType.ExecuteShutdownResponse, response).queue(channel);
        
        return request;
    }
    
    /**
     * Handle the request to get all known program names.
     * @param message
     *     original request message that contains all information needed to complete the request
     * @param channel
     *     the channel from which the request was received
     */
    private void getProgramNames(final TerraHarvestMessage message, final RemoteChannel channel)
    {
        //get all the programs
        final Set<Program> programs = m_Manager.getPrograms();
        
        final GetProgramNamesResponseData.Builder responseBuilder = GetProgramNamesResponseData.newBuilder();
        for (Program program : programs)
        {
            responseBuilder.addMissionName(program.getProgramName());
        }
        //send response message
        m_MessageFactory.createMissionProgrammingResponseMessage(message, 
            MissionProgrammingMessageType.GetProgramNamesResponse, responseBuilder.build()).queue(channel);
    }
    
    /**
     * Handle the request to get all known template names.
     * @param message
     *     original request message that contains all information needed to complete the request
     * @param channel
     *     the channel from which the request was received
     */
    private void getTemplateNames(final TerraHarvestMessage message, final RemoteChannel channel)
    {
        //get all the programs
        final Set<String> templates = m_TemplateManager.getMissionTemplateNames();
        
        final GetTemplateNamesResponseData response = GetTemplateNamesResponseData.newBuilder().
                addAllTemplateName(templates).build();
        //send response message
        m_MessageFactory.createMissionProgrammingResponseMessage(message, 
            MissionProgrammingMessageType.GetTemplateNamesResponse, response).queue(channel);
    }
    
    /**
     * Handle the request to get a program's status.
     * @param message
     *     the complete request
     * @param missionMessage
     *     the mission program specific message that contains the name of the program to get the status of
     * @param channel
     *     the channel from which the request was received
     * @return
     *     the parsed data message
     * @throws IOException
     *     thrown in the event that an error is experienced while attempting to send the response message 
     */
    private Message getProgramStatus(final TerraHarvestMessage message, 
            final MissionProgrammingNamespace missionMessage, final RemoteChannel channel) throws IOException
    {
        //parse the request
        final GetProgramStatusRequestData request = GetProgramStatusRequestData.parseFrom(missionMessage.getData());
        final String programName = request.getMissionName();
        final Program program = m_Manager.getProgram(programName);
        final ProgramStatus status = program.getProgramStatus();
        
        //response message 
        final GetProgramStatusResponseData response = GetProgramStatusResponseData.newBuilder().
                setMissionName(programName).
                setMissionStatus(EnumConverter.convertProgramStatusToMissionStatus(status)).
                setException(program.getLastExecutionExceptionCause()).build();
        
        //send
        m_MessageFactory.createMissionProgrammingResponseMessage(message, 
                MissionProgrammingMessageType.GetProgramStatusResponse, response).queue(channel);
        return request;
    }
    
    /**
     * Handle request to get the last test results for the specified program.
     * @param message
     *     the complete request
     * @param missionMessage
     *     the mission program specific message that contains the name of the program get the last test results of
     * @param channel
     *     the channel from which the request was received
     * @return
     *     the parsed data message
     * @throws IOException
     *     thrown in the event that an error is experienced while attempting to send the response message 
     */
    private Message getLastTestResults(final TerraHarvestMessage message, 
            final MissionProgrammingNamespace missionMessage, final RemoteChannel channel) throws IOException
    {
        //parse the request
        final GetLastTestResultsRequestData request = GetLastTestResultsRequestData.parseFrom(missionMessage.getData());
        final String programName = request.getMissionName();
        final Program program = m_Manager.getProgram(programName);
        final TestResult results = program.getLastTestResult();
        
        //response message 
        final GetLastTestResultsResponseData.Builder response = GetLastTestResultsResponseData.newBuilder();
        response.setMissionName(programName);
        if (results != null)
        {
            response.setResult(EnumConverter.convertToMissionTestResult(results));
        }
        response.setException(program.getLastTestExceptionCause());
        //send
        m_MessageFactory.createMissionProgrammingResponseMessage(message, 
                MissionProgrammingMessageType.GetLastTestResultsResponse, response.build()).queue(channel);
        return request;
    }
    
    /**
     * Handle the request to get a mission program's execution parameters. 
     * @param message
     *     the complete request
     * @param missionMessage
     *     the mission program specific message that contains the name of the program get the last test results of
     * @param channel
     *     the channel from which the request was received
     * @return
     *     the parsed data message
     * @throws IOException
     *     thrown in the event that an error is experienced while attempting to send the response message 
     */
    private Message getExecutionParameters(final TerraHarvestMessage message, 
            final MissionProgrammingNamespace missionMessage, final RemoteChannel channel) throws IOException
    {
        //parse the request
        final GetExecutionParametersRequestData request = 
                GetExecutionParametersRequestData.parseFrom(missionMessage.getData());
        final String programName = request.getMissionName();
        final Program program = m_Manager.getProgram(programName);
        final Map<String, Object> params = program.getExecutionParameters();
        
        //response message 
        final GetExecutionParametersResponseData response = GetExecutionParametersResponseData.newBuilder().
                setMissionName(programName).
                addAllExecutionParam(translateParameters(params)).build();
        
        //send
        m_MessageFactory.createMissionProgrammingResponseMessage(message, 
                MissionProgrammingMessageType.GetExecutionParametersResponse, response).queue(channel);
        return request;
    }

    
    /**
     * Handle the request to cancel a program that is scheduled in the future.
     * @param message
     *     the complete request
     * @param missionMessage
     *     the mission program specific message that contains the name of the program to cancel
     * @param channel
     *     the channel from which the request was received
     * @return
     *     the parsed data message
     * @throws IOException
     *     thrown in the event that an error is experienced while attempting to send the response message 
     */
    private Message cancelProgram(final TerraHarvestMessage message, final MissionProgrammingNamespace missionMessage,
            final RemoteChannel channel) throws IOException
    {
        //parse the request
        final CancelProgramRequestData request = CancelProgramRequestData.parseFrom(missionMessage.getData());
        final String programName = request.getMissionName();
        final Program program = m_Manager.getProgram(programName);
        
        //cancel the program
        program.cancel();
        
        //response message
        final CancelProgramResponseData response = CancelProgramResponseData.newBuilder().
                setMissionName(programName).
                setMissionStatus(EnumConverter.convertProgramStatusToMissionStatus(
                        program.getProgramStatus())).build();
        m_MessageFactory.createMissionProgrammingResponseMessage(message, 
                MissionProgrammingMessageType.CancelProgramResponse, response).queue(channel);
        return request;
    }
    
    /**
     * Handle the request to get program information.
     * @param message
     *     the complete request
     * @param missionMessage
     *     the mission program specific message that contains the names of programs to retrieve information for
     * @param channel
     *     the channel from which the request was received
     * @return
     *     the parsed data message
     * @throws IOException
     *     thrown in the event that an error is experienced while attempting to send the response message 
     */
    private Message getProgramData(final TerraHarvestMessage message, final MissionProgrammingNamespace missionMessage,
            final RemoteChannel channel) throws IOException
    {
        //parse the request
        final GetProgramInformationRequestData request = 
            GetProgramInformationRequestData.parseFrom(missionMessage.getData());
        
        //get the list containing the names of programs to retrieve the profile for
        final List<String> programNames = request.getMissionNameList();
        
        //response message
        final GetProgramInformationResponseData.Builder response = GetProgramInformationResponseData.newBuilder();
        
        //if the list is empty get all information for all known programs
        if (programNames.isEmpty())
        {
            for (Program program : m_Manager.getPrograms())
            {
                //get the info message
                try
                {
                    final ProgramInfo info = createProgramInfoMessage(program);
                    response.addMissionInfo(info);
                }
                catch (final ObjectConverterException e)
                {
                    m_Logging.error(e, "Unable to translate the schedule for program %s", //NOCHECKSTYLE repeated
                        program.getProgramName());     //string literal, clearly describes the error that can occur.
                }
                catch (final IllegalArgumentException e)
                {
                    m_Logging.error(e, "Unable to translate the status for program %s", program.getProgramName());
                }
            }
        }
        else
        {
            for (String name : programNames)
            {
                try
                {
                    final Program program = m_Manager.getProgram(name);
                    final ProgramInfo info = createProgramInfoMessage(program);
                    response.addMissionInfo(info);
                }
                catch (final ObjectConverterException e)
                {
                    m_Logging.error(e, "Unable to translate the schedule for program %s", name);
                }
                catch (final IllegalArgumentException e)
                {
                    m_Logging.error(e, "Unable to translate the program's status or program %s is not known", name);
                }
                
            }
        }
        //send response
        m_MessageFactory.createMissionProgrammingResponseMessage(message, 
            MissionProgrammingMessageType.GetProgramInformationResponse, response.build()).queue(channel);
        
        //return the request data
        return request;
    }
    
    /**
     * Handle request to unregister event handlers known to the {@link EventHandlerHelper} service.
     * @param message
     *     original request message that contains all information needed to complete the request
     * @param channel
     *     the channel from which the request was received
     */
    private void unregisterHandler(final TerraHarvestMessage message, final RemoteChannel channel)
    {
        //process the request
        m_EventHandlerHelper.unregisterAllHandlers();
        
        //response
        m_MessageFactory.createMissionProgrammingResponseMessage(message, 
            MissionProgrammingMessageType.UnregisterEventHandlerResponse, null).queue(channel);
    }

    /**
     * Handle the request to shutdown {@link ManagedExecutors}.
     * @param message
     *     original request message that contains all information needed to complete the request
     * @param missionMessage
     *     the mission program message that contains data
     * @param channel
     *     the channel from which the request was received
     * @return
     *     the parsed data message
     * @throws IOException
     *     thrown in the event that an error is experienced while attempting to send the response message 
     */
    private Message shutdownExecutors(final TerraHarvestMessage message, 
            final MissionProgrammingNamespace missionMessage, final RemoteChannel channel) throws IOException
    {
        //parse request
        final ManagedExecutorsShutdownRequestData request = 
                ManagedExecutorsShutdownRequestData.parseFrom(missionMessage.getData());
        
        if (request.getShutdownNow())
        {
            m_ManagedExecutor.shutdownAllExecutorServicesNow();
        }
        else
        {
            m_ManagedExecutor.shutdownAllExecutorServices();
        }
        
        //response
        m_MessageFactory.createMissionProgrammingResponseMessage(message, 
                MissionProgrammingMessageType.ManagedExecutorsShutdownResponse, null).queue(channel);
        
        //return request
        return request;
    }

    /**
     * Handle the request to remove a mission program.
     * @param message
     *     original request message that contains all information needed to complete the request
     * @param missionMessage
     *     the mission program message that contains data
     * @param channel
     *     the channel from which the request was received
     * @return
     *     the parsed data message
     * @throws IOException
     *     thrown in the event that an error is experienced while attempting to send the response message 
     */
    private Message removeMissionProgram(final TerraHarvestMessage message, 
        final MissionProgrammingNamespace missionMessage, final RemoteChannel channel) throws IOException
    {
        //parse the request message
        final RemoveMissionProgramRequestData request = 
                RemoveMissionProgramRequestData.parseFrom(missionMessage.getData());
        
        //get the program name
        final String programName = request.getMissionName();
        
        //get the program
        final Program program = m_Manager.getProgram(programName);
        
        //request removal
        program.remove();
        
        //send response
        final RemoveMissionProgramResponseData response = RemoveMissionProgramResponseData.newBuilder().
                setMissionName(programName).build();
        m_MessageFactory.createMissionProgrammingResponseMessage(message, 
                MissionProgrammingMessageType.RemoveMissionProgramResponse, response).queue(channel);
        return request;
    }

    /**
     * Create a program information proto message for the given program.
     * @param program
     *     the program to create program information message about
     * @return
     *     program information message representing the specified program
     * @throws ObjectConverterException
     *     if the schedule is unable to be converted to a protocol message 
     * @throws IllegalArgumentException
     *     in the event that a status value is received that does not have a protocol buffer equivalent
     */
    private ProgramInfo createProgramInfoMessage(final Program program) throws IllegalArgumentException, 
            ObjectConverterException
    {
        //gather all the data
        final String name = program.getProgramName();
        final ProgramStatus status = program.getProgramStatus();
        final String templateName = program.getTemplateName();
        final MissionProgramSchedule schedule = program.getMissionSchedule();
        final Map<String, Object> params = program.getExecutionParameters();
        
        //the message to return
        return ProgramInfo.newBuilder().
            setMissionName(name).
            setMissionSchedule(
                    (MissionProgramParametersGen.MissionProgramSchedule)m_Converter.convertToProto(schedule)).
            setMissionStatus(EnumConverter.convertProgramStatusToMissionStatus(status)).
            setTemplateName(templateName).
            addAllExecutionParam(translateParameters(params)).build();
    }
    
    /**
     * Create a list of map entries from the passed map.
     * @param parameters
     *     the map to translate
     * @return
     *     a list of map entries that are a protocol buffer equivalent of the passed map
     */
    private List<MapEntry> translateParameters(final Map<String, Object> parameters)
    {
        //the list to return
        final List<MapEntry> entries = new ArrayList<MapEntry>();
        for (Entry<String, Object> entry : parameters.entrySet())
        {
            final MapEntry mapEntry = MapEntry.newBuilder().
                    setKey(entry.getKey()).
                    setValue(SharedMessageUtils.convertObjectToMultitype(entry.getValue())).build();
            entries.add(mapEntry);
        }
        return entries;
    }
}
