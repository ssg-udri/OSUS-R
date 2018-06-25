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

import mil.dod.th.core.log.Logging;
import mil.dod.th.core.mp.MissionProgramManager;
import mil.dod.th.core.mp.MissionScript.TestResult;
import mil.dod.th.core.mp.Program;
import mil.dod.th.core.mp.Program.ProgramStatus;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.CancelProgramRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ExecuteRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ExecuteShutdownRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ExecuteTestRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetProgramInformationRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetProgramInformationResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace.MissionProgrammingMessageType;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ProgramInfo;
import mil.dod.th.core.remote.proto.MissionProgramMessages.RemoveMissionProgramRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.webapp.controller.ControllerMgr;
import mil.dod.th.ose.gui.webapp.general.RemoteEventRegistrationHandler;
import mil.dod.th.ose.gui.webapp.remote.RemoteEvents;
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
 * Class keeps track of all known missions that are loaded on connected controllers.
 * 
 * @author nickmarcucci
 *
 */
@ManagedBean(name = "currentMissionMgr")
@ApplicationScoped
public class CurrentMissionMgrImpl implements CurrentMissionMgr //NOPMD:Avoid really long classes. 
// There are multiple inner classes that 
// listen to remote events concerning remote comms layers.
{
    /**
     * Topic all string.
     */
    private static final String TOPIC_ALL = "*";
    
    /**
     * Summary text for growl messages for mission shutdown.
     */
    private static final String MISSION_SHUTDOWN_DIALOG_SUM = "Mission Shutdown";
    
    /**
     * List which holds all known missions for a controller.
     */
    private final Map<Integer, Set<CurrentMissionModel>> m_CurrentMissions;
    
    /**
     * Remote event response handler that keeps track of remote send event registration IDs.
     */
    private RemoteEventRegistrationHandler m_RemoteHandler;
    
    /**
     * Event handler for MissionProgramming namespace messages.
     */
    private EventHelperMissions m_MissionEventHelper;
    
    /**
     * Event handler for controller events.
     */
    private EventHelperControllerEvent m_ControllerEventHelper;
    
    /**
     * Event handler for remote events pertaining to missions.
     */
    private EventHelperRemoteEvents m_RemoteEventHelper;
    
    /**
     * Inject the MessageFactory service for creating remote messages.
     */
    @Inject @OSGiService
    private MessageFactory m_MessageFactory;
    
    /**
     * Service that retrieves the bundle context of this bundle.
     */
    @Inject
    private BundleContextUtil m_BundleUtil;
    
    /**
     * Inject the EventAdmin service for posting events.
     */
    @Inject @OSGiService
    private EventAdmin m_EventAdmin;
    
    /**
     * Growl message utility for creating growl messages.
     */
    @Inject
    private GrowlMessageUtil m_GrowlUtil;
    
    /**
     * Constructor.
     */
    public CurrentMissionMgrImpl()
    {
        m_CurrentMissions = Collections.synchronizedMap(new HashMap<Integer, Set<CurrentMissionModel>>());
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
     * Set the {@link EventAdmin} service.
     * @param eventAdmin
     *      the event admin service to use.
     */
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Set the growl message utility service.
     * @param growlUtil
     *     the growl message utility service to use
     */
    public void setGrowlMessageUtility(final GrowlMessageUtil growlUtil)
    {
        m_GrowlUtil = growlUtil;
    }
    
    /**
     * Register for mission controller and remote events.
     */
    @PostConstruct
    public void initAndRegisterEvents()
    {
        m_RemoteHandler = new RemoteEventRegistrationHandler(m_MessageFactory);
        
        m_MissionEventHelper = new EventHelperMissions();
        m_ControllerEventHelper = new EventHelperControllerEvent();
        m_RemoteEventHelper = new EventHelperRemoteEvents();
        
        m_MissionEventHelper.registerForEvents();
        m_ControllerEventHelper.registerControllerEvents();
        m_RemoteEventHelper.registerForEvents();
    }
    
    /**
     * Unregister for all previously registered events.
     */
    @PreDestroy
    public void cleanup()
    {
        m_RemoteHandler.unregisterRegistrations();
        
        m_MissionEventHelper.unregisterListener();
        m_ControllerEventHelper.unregisterListener();
        m_RemoteEventHelper.unregisterListener();
    }
    
    @Override
    public synchronized List<CurrentMissionModel> getCurrentMissionsForControllerAsync(final int systemId)
    {
        //list to return
        final List<CurrentMissionModel> models = new ArrayList<CurrentMissionModel>();
        
        if (!m_CurrentMissions.containsKey(systemId))
        {
            requestToListenForRemoteEvents(systemId);
            
            m_CurrentMissions.put(systemId, new HashSet<CurrentMissionModel>());
            
            final GetProgramInformationRequestData requestMessage = GetProgramInformationRequestData.
                    newBuilder().build();
            
            m_MessageFactory.createMissionProgrammingMessage(
                    MissionProgrammingMessageType.GetProgramInformationRequest, requestMessage).
                        queue(systemId, null);
        }
        
        models.addAll(m_CurrentMissions.get(systemId));
        
        return models;
    }
    

    @Override
    public void executeTestRequest(final String programName, final int controllerId)
    {
        final ExecuteTestRequestData requestMessage = ExecuteTestRequestData.newBuilder().
                setMissionName(programName).build();
        
        final MissionErrorHandler handler = new MissionErrorHandler(programName, MissionAction.TEST);
        
        m_MessageFactory.createMissionProgrammingMessage(MissionProgrammingMessageType.ExecuteTestRequest,
                requestMessage).queue(controllerId, handler);
    }
    

    @Override
    public void executeExecuteRequest(final String programName, final  int controllerId)
    {
        final ExecuteRequestData requestMessage = ExecuteRequestData.newBuilder().setMissionName(programName).build();
        
        final MissionErrorHandler handler = new MissionErrorHandler(programName, MissionAction.EXECUTE);
        
        m_MessageFactory.createMissionProgrammingMessage(MissionProgrammingMessageType.ExecuteRequest, 
                requestMessage).queue(controllerId, handler);
    }
    
    @Override
    public void executeShutdownRequest(final String programName, final int controllerId)
    {
        final ExecuteShutdownRequestData requestMessage = ExecuteShutdownRequestData.newBuilder().
                setMissionName(programName).build();
        
        final MissionErrorHandler handler = new MissionErrorHandler(programName, MissionAction.SHUTDOWN);
        
        m_MessageFactory.createMissionProgrammingMessage(MissionProgrammingMessageType.ExecuteShutdownRequest, 
                requestMessage).queue(controllerId, handler);
    }
    

    @Override
    public void executeCancelRequest(final String programName, final int controllerId)
    {
        final CancelProgramRequestData requestMessage = CancelProgramRequestData.newBuilder().
                setMissionName(programName).build();
        
        final MissionErrorHandler handler = new MissionErrorHandler(programName, MissionAction.CANCEL);
        
        m_MessageFactory.createMissionProgrammingMessage(MissionProgrammingMessageType.CancelProgramRequest, 
                requestMessage).queue(controllerId, handler);
    }
    
    @Override
    public void executeRemoveRequest(final String programName, final int controllerId)
    {
        final RemoveMissionProgramRequestData requestMessage = RemoveMissionProgramRequestData.newBuilder().
                setMissionName(programName).build();
        
        final MissionErrorHandler handler = new MissionErrorHandler(programName, MissionAction.REMOVE);
        
        m_MessageFactory.createMissionProgrammingMessage(MissionProgrammingMessageType.RemoveMissionProgramRequest,
                requestMessage).queue(controllerId, handler);
    }
    
    @Override
    public String translateProgramStatus(final ProgramStatus status)
    {
        if (status.equals(ProgramStatus.EXECUTED) || status.equals(ProgramStatus.EXECUTING)
                || status.equals(ProgramStatus.SHUTTING_DOWN))
        {
            return "RUNNING";
        }
        else if (status.equals(ProgramStatus.WAITING_UNINITIALIZED))
        {
            return "LOADING";
        }
        else if (status.equals(ProgramStatus.WAITING_INITIALIZED))
        {
            return "LOADED";
        }
        else if (status.equals(ProgramStatus.EXECUTING_TEST))
        {
            return "TESTING";
        }
        else if (status.equals(ProgramStatus.VARIABLE_ERROR) 
                || status.equals(ProgramStatus.INITIALIZATION_ERROR) 
                ||  status.equals(ProgramStatus.SCRIPT_ERROR))
        {
            return "ERROR";
        }
        
        return status.toString();
    }
    
    /**
     * Function requests to listen for remote events pertaining to a mission program
     * on a specified system.
     * @param systemId
     *  the id of the system on which events need to be listened for
     */
    private synchronized void requestToListenForRemoteEvents(final int systemId)
    {
        final List<String> topics = new ArrayList<String>();
        topics.add(Program.TOPIC_PREFIX + TOPIC_ALL);
        topics.add(MissionProgramManager.TOPIC_PREFIX + TOPIC_ALL);
        
        RemoteEvents.sendEventRegistration(m_MessageFactory, topics, null, true, systemId, m_RemoteHandler);        
    }
    
    /**
     * Function to try and add the program info into the list of recorded objects.
     * @param modelToAdd
     *  the mission model to add to the list of known programs
     * @param systemId
     *  the id of the system to which this program resides
     * @return
     *  true 
     */
    private synchronized boolean tryAddProgramInfo(final CurrentMissionModel modelToAdd, final int systemId)
    {
        final CurrentMissionModel info = findProgramInfo(modelToAdd.getMissionName(), systemId);
        
        if (info == null && m_CurrentMissions.containsKey(systemId))
        {
            final Set<CurrentMissionModel> setProgramInfo = m_CurrentMissions.get(systemId);
            setProgramInfo.add(modelToAdd);
            
            Logging.log(LogService.LOG_INFO, "Adding new current mission to map, template: %s, mission name: %s", 
                    modelToAdd.getTemplateName(), modelToAdd.getMissionName());
            
            return true;
        }
        
        return false;
        
    }
    
    /**
     * Function to remove a program from the list of known programs.
     * @param systemId
     *  the id of the system on which the to be removed program resides.
     * @param programName
     *  the name of the program that is to be removed.
     */
    private synchronized void tryRemoveProgramInfo(final int systemId, final String programName)
    {
        if (m_CurrentMissions.containsKey(systemId))
        {
            final Set<CurrentMissionModel> models = m_CurrentMissions.get(systemId);
            
            for (CurrentMissionModel model : models)
            {
                if (model.getMissionName().equals(programName))
                {
                    models.remove(model);
                    
                    Logging.log(LogService.LOG_INFO, 
                            "Removing current mission from map, template: %s, mission name: %s", 
                            model.getTemplateName(), model.getMissionName());
                    
                    break;
                }
            }
        }
    }
    
    
    /**
     * Function finds a specific mission model object based on the mission name and system id
     * passed in.
     * @param missionName
     *  the name of the mission to identify the correct program info object
     * @param systemId
     *  the id of the system to which the program info object should belong
     * @return
     *  returns a {@link CurrentMissionModel} object if found; otherwise null is returned
     */
    private synchronized CurrentMissionModel findProgramInfo(final String missionName, final int systemId)
    {
        if (m_CurrentMissions.get(systemId) != null)
        {
            for (CurrentMissionModel info : m_CurrentMissions.get(systemId))
            {
                if (info.getMissionName().equals(missionName))
                {
                    return info;
                }
            }
        }
            
        return null;
    }
    
    /**
     * Function to appropriately handle response for GetProgramInformationResponse.
     * 
     * @param systemId
     *  the id of the system from which this message originated
     * @param data
     *  the message data that was received
     */
    private synchronized void eventHandleGetProgramInformationResponse(final Message data, final int systemId)
    {
        final GetProgramInformationResponseData response = (GetProgramInformationResponseData)data;
        
        final List<ProgramInfo> missions = response.getMissionInfoList();
        
        for (ProgramInfo info : missions)
        {
            final ProgramStatus status = ProgramStatus.valueOf(info.getMissionStatus().toString());
            
            final CurrentMissionModel model = new CurrentMissionModel(info.getMissionName(), status,
                    info.getTemplateName(), info.getMissionSchedule().getStartInterval(), 
                    info.getMissionSchedule().getStopInterval());
            
            tryAddProgramInfo(model, systemId);
        }
        
        postMissionUpdateEvent();
        
    }
    
    
    /**
     * Function which handles a {@link Program#TOPIC_PROGRAM_STATUS_CHANGED} event.
     * The given program's status will be updated.
     * @param systemId
     *  the id of the system from which this program is running
     * @param programName
     *  the name of the program which has changed statuses
     * @param status
     *  the current status of the program
     */
    private synchronized void handleProgramStatusChangedEvent(final int systemId, 
            final String programName, final ProgramStatus status)
    {
        
        //set the mission status
        setMissionStatus(systemId, programName, status);
        
        //to avoid duplicate messages because of the translation function the EXECUTED and SHUTTING_DOWN
        //statuses are ignored because the message should already have been shown. Should the ProgramStatus
        //names ever be ironed out, this check needs to be removed.
        if (!status.equals(ProgramStatus.EXECUTED) && !status.equals(ProgramStatus.SHUTTING_DOWN))
        {
            //update with growl message
            m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, 
                "Mission Status Updated", 
                    String.format("The mission %s for system 0x%08x has been updated to the status %s.",
                        programName, systemId, translateProgramStatus(status)));
        }
        //update with push message
        postMissionUpdateEvent();
    }
    
    /**
     * Function that handles a {@link Program#TOPIC_PROGRAM_EXECUTED_FAILURE} event.
     * @param systemId
     *  the id of the system from which the program is running
     * @param programName
     *  the name of the program that has failed
     * @param status
     *  the status of the failed program
     * @param error
     *  the error that was caused when the program was executed
     */
    private synchronized void handleProgramExecutedFailureEvent(final int systemId, final String programName,
            final ProgramStatus status, final String error)
    {
        setMissionStatus(systemId, programName, status);
        
        //update with growl message
        m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "Mission Failed Execution!", 
                String.format("The mission %s for system 0x%08x has failed execution with status %s and the following" 
                        + " exception: \n %s", programName, systemId, translateProgramStatus(status), error));
        
        //update with push message
        postMissionUpdateEvent();
    }
    
    /**
     * Function that handles a {@link Program#TOPIC_PROGRAM_EXECUTED} event.
     * @param systemId
     *  the id of the system from which the program has been executed
     * @param programName
     *  the name of the program that has been executed on that system
     * @param status
     *  the status of the executed program
     */
    private synchronized void handleProgramExectutedEvent(final int systemId, final String programName,
            final ProgramStatus status)
    {
        setMissionStatus(systemId, programName, status);
        
        //update with growl message
        m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Mission Executed.", 
                String.format("The mission %s for system 0x%08x has been executed.", programName, systemId));
        
        //update with push message
        postMissionUpdateEvent();
    }
    
    /**
     * Function that handles a {@link MissionProgramManager#TOPIC_PROGRAM_REMOVED} event.
     * Function will remove the program from the list of known programs.
     * @param systemId
     *  the id of the system on which the program has been removed
     * @param programName
     *  the name of the removed program.
     * @param status 
     *  the status of the removed program.
     */
    private synchronized void handleProgramRemovedEvent(final int systemId, final String programName,
            final ProgramStatus status)
    {
        //remove a mission
        tryRemoveProgramInfo(systemId, programName);
        
        //update with growl message
        m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Mission Removed!",
                String.format("The mission %s for system 0x%08x has been removed with status %s.", programName, 
                        systemId, translateProgramStatus(status)));
        
        //update event
        postMissionUpdateEvent();
        
    }
    
    /**
     * Function that handles a {@link Program#TOPIC_PROGRAM_SHUTDOWN} event.
     * @param systemId
     *  the id of the system on which the program was shutdown.
     * @param programName
     *  the name of the program which has been shutdown
     * @param status
     *  the status of the program as a result of being shutdown.
     */
    private synchronized void handleProgramShutdownEvent(final int systemId, final String programName,
            final ProgramStatus status)
    {
        setMissionStatus(systemId, programName, status);
        
        //update with growl message
        m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, MISSION_SHUTDOWN_DIALOG_SUM,
                String.format("The mission %s for system 0x%08x has been shutdown.", programName, systemId));
        
        //push update event 
        postMissionUpdateEvent();
    }
    
    /**
     * Function that handles a {@link Program#TOPIC_PROGRAM_TESTED} event. 
     * @param systemId
     *  the id of the system on which the program was tested.
     * @param programName
     *  the name of the program that was tested
     * @param test
     *  the test result for this program
     * @param status
     *  the status of the tested program
     * @param exception
     *  if an exception has occurred then it will be the exception that was included
     *  in the mission. It will be the empty string otherwise.
     */
    private synchronized void handleProgramTestedEvent(final int systemId, final String programName,
            final TestResult test, final ProgramStatus status, final String exception)
    {
        setMissionStatus(systemId, programName, status);
        setTestResult(systemId, programName, test);

        final String messageSum = "Mission Test";

        if (exception == null || exception.isEmpty())
        {
            final String messageString = "The mission %s for system 0x%08x has been tested.";
            //update with growl message
            m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, messageSum,
                    String.format(messageString, programName, systemId));
        }
        else 
        {
            final String messageString = "The mission %s for system 0x%08x has been tested and" 
                    + " has resulted in the following error %s";
            //update with growl message
            m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_WARN, messageSum,
                    String.format(messageString, programName, systemId, exception));
        }
        
        //push update event
        postMissionUpdateEvent();
    }
    
    /**
     * Function that handles a {@link MissionProgramManager#TOPIC_PROGRAM_ADDED} event.
     * Function will request additional information from the controller by sending out 
     * a program information request.
     * @param systemId
     *  the id of the system on which the program was added.
     * @param programName
     *  the name of the mission that has been added.
     */
    public void handleProgramAddedEvent(final int systemId, final String programName)
    {
        final GetProgramInformationRequestData requestMessage = GetProgramInformationRequestData.
                newBuilder().addMissionName(programName).build();
        
        m_MessageFactory.createMissionProgrammingMessage(
                MissionProgrammingMessageType.GetProgramInformationRequest, requestMessage).
                    queue(systemId, null);
    }
    
    
    /**
     * Function alters the status of the mission identified by the mission id.
     * @param systemId
     *  the id of the system on which the mission is located
     * @param programName
     *  the name of the mission 
     * @param status
     *  the status that the mission is to be updated to
     */
    private synchronized void setMissionStatus(final int systemId, final String programName, 
            final ProgramStatus status)
    {
        final CurrentMissionModel info = findProgramInfo(programName, systemId);
        
        if (info == null)
        {
            Logging.log(LogService.LOG_ERROR, "An error has occurred trying to set the mission " 
                    + "status for mission program %s with status %s from system 0x%08x", programName, 
                    status.toString(), systemId);
        }
        else
        {
            info.updateMissionStatus(status);
        }
        
    }
    
    /**
     * Function sets the received test result for a mission.
     * @param systemId
     *  the id of the system on which the mission is located
     * @param programName
     *  the name of the mission
     * @param result
     *  the result of the test of the mission
     */
    private synchronized void setTestResult(final int systemId, final String programName, final TestResult result)
    {
        final CurrentMissionModel info = findProgramInfo(programName, systemId);
        
        if (info == null)
        {
            Logging.log(LogService.LOG_ERROR, "An error has occurred trying to set the mission test result for"
                    + " mission program %s with result %s from system 0x%08x", programName, 
                    result.toString(), systemId);
        }
        else
        {
            info.setLastTestResult(result);
        }
    }
    
    
    /**
     * Method to post a push update.
     */
    private void postMissionUpdateEvent()
    {
        final Event updated = new Event(CurrentMissionMgr.TOPIC_MISSION_UPDATED, new HashMap<String, Object>());
        m_EventAdmin.postEvent(updated);
    }
    
    /**
     * Event handler for EventAdmin namespace messages received. These messages represent remote event notices.
     */
    public class EventHelperRemoteEvents implements EventHandler
    {
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is 
         * destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;
        
        /**
         * Register for remote events which pertain to mission programs.
         */
        public void registerForEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            
            //register for all program topics
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            final String[] topics = {Program.TOPIC_PREFIX + TOPIC_ALL, MissionProgramManager.TOPIC_PREFIX + TOPIC_ALL};
            props.put(EventConstants.EVENT_TOPIC, topics);
            
            
            final String filterString = String.format("(%s=*)", RemoteConstants.REMOTE_EVENT_PROP);
            props.put(EventConstants.EVENT_FILTER, filterString);
            
            //register for the events
            m_Registration = context.registerService(EventHandler.class, this, props);
        }
        
        @Override
        public void handleEvent(final Event event)
        {
            final String topic = (String) event.getProperty(EventConstants.EVENT_TOPIC);
            final String programName = (String)event.getProperty(Program.EVENT_PROP_PROGRAM_NAME);
            final Integer systemId = (Integer)(event.getProperty(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID));
            
            if (topic.equals(Program.TOPIC_PROGRAM_STATUS_CHANGED + RemoteConstants.REMOTE_TOPIC_SUFFIX))
            {
                final String statusString = (String)event.getProperty(Program.EVENT_PROP_PROGRAM_STATUS);
                final ProgramStatus status = ProgramStatus.valueOf(statusString);
                handleProgramStatusChangedEvent(systemId, programName, status);
            }
            else if (topic.equals(Program.TOPIC_PROGRAM_EXECUTED_FAILURE + RemoteConstants.REMOTE_TOPIC_SUFFIX))
            {
                final String statusString = (String)event.getProperty(Program.EVENT_PROP_PROGRAM_STATUS);
                final ProgramStatus status = ProgramStatus.valueOf(statusString);
                final String error = (String)event.getProperty(Program.EVENT_PROP_PROGRAM_EXCEPTION);
                
                handleProgramExecutedFailureEvent(systemId, programName, status, error);
            }
            else if (topic.equals(Program.TOPIC_PROGRAM_EXECUTED + RemoteConstants.REMOTE_TOPIC_SUFFIX))
            {
                final String statusString = (String)event.getProperty(Program.EVENT_PROP_PROGRAM_STATUS);
                final ProgramStatus status = ProgramStatus.valueOf(statusString);
                handleProgramExectutedEvent(systemId, programName, status);
            }
            else if (topic.equals(Program.TOPIC_PROGRAM_SHUTDOWN + RemoteConstants.REMOTE_TOPIC_SUFFIX))
            {
                final String statusString = (String)event.getProperty(Program.EVENT_PROP_PROGRAM_STATUS);
                final ProgramStatus status = ProgramStatus.valueOf(statusString);
                handleProgramShutdownEvent(systemId, programName, status);
            }
            else if (topic.equals(Program.TOPIC_PROGRAM_TESTED + RemoteConstants.REMOTE_TOPIC_SUFFIX))
            {
                final String statusString = (String)event.getProperty(Program.EVENT_PROP_PROGRAM_STATUS);
                final ProgramStatus status = ProgramStatus.valueOf(statusString);
                final TestResult testResult = (TestResult)event.getProperty(Program.EVENT_PROP_PROGRAM_TEST_RESULT);
                
                final String exception = (String)event.getProperty(Program.EVENT_PROP_PROGRAM_TEST_RESULT_EXCEPTION);
                
                handleProgramTestedEvent(systemId, programName, testResult, status, exception);
            }
            else if (topic.equals(MissionProgramManager.TOPIC_PROGRAM_ADDED + RemoteConstants.REMOTE_TOPIC_SUFFIX))
            {
                //do i really need the status variable
                handleProgramAddedEvent(systemId, programName);
            }
            else if (topic.equals(MissionProgramManager.TOPIC_PROGRAM_REMOVED + RemoteConstants.REMOTE_TOPIC_SUFFIX))
            {
                final String statusString = (String)event.getProperty(Program.EVENT_PROP_PROGRAM_STATUS);
                final ProgramStatus status = ProgramStatus.valueOf(statusString);
                handleProgramRemovedEvent(systemId, programName, status);
            }
        }
        
        /**
         * Function to unregister for events.
         */
        public void unregisterListener()
        {
            m_Registration.unregister();
        }
    }

    /**
     * Class to handle controller removed events. 
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
            m_CurrentMissions.remove(controllerId);
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
     * Event handler for the MissionProgramming namespace messages.
     * @author nickmarcucci
     *
     */
    class EventHelperMissions implements EventHandler
    {
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is 
         * destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;
        
        /**
         * Function to register this event helper for mission programming namespace messages.
         */
        public void registerForEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            final String[] topics = {RemoteConstants.TOPIC_MESSAGE_RECEIVED};
            props.put(EventConstants.EVENT_TOPIC, topics);
            final String filterString = String.format("(%s=%s)", RemoteConstants.EVENT_PROP_NAMESPACE, 
                    Namespace.MissionProgramming.toString());
            props.put(EventConstants.EVENT_FILTER, filterString);
            
            //register the event handler that listens for the mission programming namespace responses
            m_Registration = context.registerService(EventHandler.class, this, props);
        }
        
        /* (non-Javadoc)
         * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
         */
        @Override
        public void handleEvent(final Event event)
        {
            //get the event properties 
            final int systemId = (Integer)event.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID);
            final String messageType = (String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE);
           
            //figure out the appropriate action to the received response 
            if (messageType.equals(MissionProgrammingMessageType.GetProgramInformationResponse.toString()))
            {
                eventHandleGetProgramInformationResponse((GetProgramInformationResponseData)event.
                        getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), systemId);
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
     * Class is in charge of handling/displaying errors that occur when performing mission program actions
     * such as executing, testing, shutting down, cancelling, and removing mission programs.
     * @author nickmarcucci
     *
     */
    public class MissionErrorHandler implements ResponseHandler
    {
        /**
         * The action that has been performed for this error handler.
         */
        private final MissionAction m_Action;
        
        /**
         * The name of the mission program that this error handler has been setip for.
         */
        private final String m_MissionName;
        
        /**
         * Constructor.
         * @param missionName
         *  the name of the mission that this handler represents.
         * @param action
         *  the action that has been performed for the mission.
         */
        public MissionErrorHandler(final String missionName, final MissionAction action)
        {
            m_MissionName = missionName;
            m_Action = action;
        }
        
        @Override
        public void handleResponse(final TerraHarvestMessage message, final TerraHarvestPayload payload,
                final Message namespaceMessage, final Message dataMessage)
        {
            if (payload.getNamespace() == Namespace.Base)
            {
                final String summary = "Mission Error Occurred";
                
                //parse the namespace message
                final BaseNamespace baseMessage = (BaseNamespace)namespaceMessage;
                
                //check the base namespace message type
                if (baseMessage.getType() == BaseMessageType.GenericErrorResponse)
                {
                  //parse generic error message
                    final GenericErrorResponseData data = (GenericErrorResponseData)dataMessage;
                    
                    //post notice
                    m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, summary, String.format(
                            "Performing action %s for mission %s has resulted in the following error \n %s - %s", 
                            m_Action.toString().toLowerCase(), m_MissionName, data.getError(), 
                                    data.getErrorDescription()));
                }
            }
        }
        
        
    }
    
    /**
     * The action that has been performed for a mission. 
     * @author nickmarcucci
     *
     */
    public enum MissionAction
    {
        /**
         * Mission has been executed.
         */
        EXECUTE,
        /**
         * Mission has been tested.
         */
        TEST,
        /**
         * Mission has been shutdown.
         */
        SHUTDOWN,
        /**
         * Mission has been cancelled.
         */
        CANCEL,
        /**
         * Mission has been removed.
         */
        REMOVE;
    }
}
