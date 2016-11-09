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
package mil.dod.th.ose.remote.integration.namespace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.AssertionFailedError;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.mp.Program;
import mil.dod.th.core.mp.Program.ProgramStatus;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ExecuteTestRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetLastTestResultsRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetLastTestResultsResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetProgramInformationRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetProgramInformationResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetProgramStatusRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetProgramStatusResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetTemplateNamesResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetTemplatesResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.LoadTemplateRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ManagedExecutorsShutdownRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace.MissionProgrammingMessageType;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionStatus;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionTestResult;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ProgramInfo;
import mil.dod.th.core.remote.proto.MissionProgramMessages.RemoveTemplateRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.SharedMessages.FactoryObjectInfo;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.core.remote.proto.SharedMessages.UUID;
import mil.dod.th.ose.remote.integration.AssetNamespaceUtils;
import mil.dod.th.ose.remote.integration.MessageMatchers;
import mil.dod.th.ose.remote.integration.MessageMatchers.BasicMessageMatcher;
import mil.dod.th.ose.remote.integration.RemoteEventRegistration;
import mil.dod.th.ose.remote.integration.MessageListener;
import mil.dod.th.ose.remote.integration.MissionProgramNamespaceUtils;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import mil.dod.th.ose.remote.integration.TerraHarvestMessageHelper;
import mil.dod.th.remote.lexicon.mp.model.MissionProgramParametersGen;
import mil.dod.th.remote.lexicon.mp.model.MissionProgramTemplateGen;
import mil.dod.th.remote.lexicon.mp.model.MissionProgramTemplateGen.MissionVariableTypes;
import mil.dod.th.remote.lexicon.types.SharedTypesGen.VariableMetaData;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.log.LogService;

import com.google.protobuf.Message;

import example.asset.ExampleAsset;

/**
 * Tests routing of messages to the {@link MissionProgrammingNamespace}.
 * @author callen
 *
 */
public class TestMissionProgrammingNamespace 
{
    /**
     * The asset name created for programs that have an asset dep.
     */
    private static final String ASSET_NAME = "mission_asset";
    
    /**
     * The socket to use.
     */
    private Socket m_Socket;
    
    /**
     * The asset name created for programs that have an asset dep.
     */
    private SharedMessages.UUID m_AssetUuid;
    
    /**
     * The name of the mission created.
     */
    private String m_MissionName;
    
    /**
     * Sets up the socket that connects remote interface to the controller
     */
    @Before
    public void setUp() throws Exception
    {
        m_Socket = SocketHostHelper.connectToController();
        //create the asset to be bound to the script
        CreateAssetResponseData assetData = AssetNamespaceUtils.createAsset(m_Socket, ExampleAsset.class.getName(),
                ASSET_NAME, null);
        //store for removal
        m_AssetUuid = assetData.getInfo().getUuid();
        assertThat("No asset UUID could be retrieved.", m_AssetUuid, is(notNullValue()));
    }

    /**
     * Closes the socket.  
     */
    @After
    public void tearDown() throws Exception
    {
        try 
        {
            boolean exceptionOccurred = false;
            if (m_MissionName != null)
            {
                try
                {
                    MissionProgramNamespaceUtils.removeMissionProgram(m_MissionName, m_Socket);
                }
                catch (AssertionFailedError | Exception e)
                {
                    Logging.log(LogService.LOG_ERROR, e, "Unable to remove mission: %s", m_MissionName);
                    exceptionOccurred = true;
                }
                m_MissionName = null;
            }
            
            if (m_AssetUuid == null)
            {
                // failed to create from before, probably still there from before
                FactoryObjectInfo info = AssetNamespaceUtils.tryGetAssetInfoByName(m_Socket, ASSET_NAME);
                if (info != null)
                {
                    // asset still here from before
                    m_AssetUuid = info.getUuid();
                }
            }
            
            if (m_AssetUuid != null)
            {
                try
                {
                    AssetNamespaceUtils.removeAsset(m_Socket, m_AssetUuid);
                }
                catch (AssertionFailedError | Exception e)
                {
                    Logging.log(LogService.LOG_ERROR, e, "Unable to remove mission asset.");
                    exceptionOccurred = true;
                }
            }
            
            try
            {
                MessageListener.unregisterEvent(m_Socket); 
            }
            catch (AssertionFailedError | Exception e)
            {
                Logging.log(LogService.LOG_ERROR, e, "Unable to unregister for registered events.");
            }
            
            if (exceptionOccurred)
            {
                throw new IllegalStateException("Test was unable to complete cleanup. Check stacktrace for further "
                        + "details.");
            }           
        }
        finally
        {
            m_Socket.close();
        }
    }
    
    /**
     * Test sending and receiving response for a load template request. 
     * Verify that a persistence error is thrown if the template already exists.
     */
    @Test
    public void testLoadTemplate() throws UnknownHostException, IOException, InterruptedException
    {
        loadProtoTestTemplate();
        //TODO: TH-805, support syncing of templates, need to be able to preserve unique names,
        //but also allow for templates to be update if wanted.
    }
    
    /**
     * Test sending and receiving a response for a load parameters request.
     */
    @Test
    public void testLoadParameters() throws UnknownHostException, IOException, InterruptedException
    {
        //load the test template
        loadProtoTestTemplate();
        
        m_MissionName = "nameroooo";
        
        //create mission program parameters
        MissionProgramParametersGen.MissionProgramSchedule schedule = MissionProgramParametersGen.
            MissionProgramSchedule.newBuilder().setActive(true).setImmediately(false).
            setIndefiniteInterval(true).build();
        MissionProgramParametersGen.MissionProgramParameters params =  MissionProgramParametersGen.
            MissionProgramParameters.newBuilder().setTemplateName("TEST_TEMPLATE_REMOTE").setSchedule(schedule).
                setProgramName(m_MissionName).build();
        MissionProgramNamespaceUtils.loadParameters(m_Socket, params);
    }
    
    /**
     * Test getting a list of templates.
     * 
     * Verify that the list is greater than one. 
     */
    @Test
    public void testGetTemplates() throws IOException, InterruptedException
    {
        //load the test template
        loadProtoTestTemplate();
        
        //construct the request
        TerraHarvestMessage request = createMissionProgramMessage(MissionProgrammingMessageType.GetTemplatesRequest,
            null);
        
        // send out message
        request.writeDelimitedTo(m_Socket.getOutputStream());
        
        //read in response
        TerraHarvestMessage response = TerraHarvestMessage.parseDelimitedFrom(m_Socket.getInputStream());
        TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload());
        assertThat(response, is(notNullValue()));
        assertThat(payLoadTest.getNamespace(), is(Namespace.MissionProgramming));
        //verify namespace and response
        MissionProgrammingNamespace namespaceResponse = MissionProgrammingNamespace.
            parseFrom(payLoadTest.getNamespaceMessage());
        assertThat(namespaceResponse.getType(), is(MissionProgrammingMessageType.GetTemplatesResponse));
        GetTemplatesResponseData responseData = GetTemplatesResponseData.parseFrom(namespaceResponse.getData());
        //should be greater than one, just loaded a template earlier in the tests
        assertThat(responseData.getTemplateCount(), greaterThan(1));
        
        //verify response data contains the template loaded earlier
        for (MissionProgramTemplateGen.MissionProgramTemplate template : responseData.getTemplateList())
        {
            if (template.getName().equals("TEST_TEMPLATE_REMOTE"))
            {
                assertThat(template.getDescription(), is("Something"));
                assertThat(template.getSource(), is("TheSource!@@#(*&$^%_@##$"));
                assertThat(template.getVariableMetaDataCount(), is(1));
                for (MissionProgramTemplateGen.MissionVariableMetaData varData : template.getVariableMetaDataList())
                {
                    if (varData.getBase().getName().equals("variable"))
                    {
                        assertThat(varData.getType(), is(MissionVariableTypes.Enum.ASSET));
                        assertThat(varData.getBase().getDescription(), is("I describe a variable"));
                        assertThat(varData.getBase().getHumanReadableName(), is("human readable name"));
                        assertThat(varData.getBase().getDefaultValue(), is("My Asset"));
                    }
                }
            }
        }
    }
    
    /**
     * Test requesting the removal of a template.
     */
    @Test
    public void testRemoveTemplate() throws UnknownHostException, IOException, InterruptedException
    {
        //load the test template
        loadProtoTestTemplate();
        
        MessageListener listener = new MessageListener(m_Socket);
        
        //remove template message
        RemoveTemplateRequestData removeMessage = MissionProgramMessages.RemoveTemplateRequestData.newBuilder().
                setNameOfTemplate("TEST_TEMPLATE_REMOTE").build();
        
        //construct the namespace message
        MissionProgrammingNamespace.Builder missionMessageBuilder = MissionProgrammingNamespace.newBuilder().
            setType(MissionProgrammingMessageType.RemoveTemplateRequest).setData(removeMessage.toByteString());
        
        //add the previous messages to a terra harvest message
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.MissionProgramming, 
                missionMessageBuilder);
        // send out message
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        //verify response
        listener.waitForMessage(Namespace.MissionProgramming, MissionProgrammingMessageType.RemoveTemplateResponse,
                    300);
    }
    
    /**
     * Test requesting the execution of a mission program and verifying the status change event is posted.
     */
    @Test
    public void testStatusChange() throws UnknownHostException, IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(m_Socket);
        
        m_MissionName = "programStatusTestRemote";
                
        //create schedule
        MissionProgramParametersGen.MissionProgramSchedule schedule = MissionProgramParametersGen.
                MissionProgramSchedule.newBuilder().setActive(true).setImmediately(false).
                setIndefiniteInterval(true).build();
        
        //create mission program parameters
        MissionProgramParametersGen.MissionProgramParameters params =  
            MissionProgramNamespaceUtils.genTimedDataLoopParams(schedule, ASSET_NAME, m_MissionName);

        //load the parameters
        MissionProgramNamespaceUtils.loadParameters(m_Socket, params);
        
        RemoteEventRegistration.regRemoteEventMessages(m_Socket, Program.TOPIC_PROGRAM_STATUS_CHANGED, 
                String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_STATUS, ProgramStatus.EXECUTING_TEST));
        RemoteEventRegistration.regRemoteEventMessages(m_Socket, Program.TOPIC_PROGRAM_TESTED, 
                String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, m_MissionName));
        
        //remove template message
        ExecuteTestRequestData requestMessage = ExecuteTestRequestData.newBuilder().
                setMissionName(m_MissionName).build();
        
        //create execution message
        TerraHarvestMessage message = 
                createMissionProgramMessage(MissionProgrammingMessageType.ExecuteTestRequest, requestMessage);
        
        // send out message
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        // listen for messages for a specific time interval
        listener.waitForMessages(1500,
              new BasicMessageMatcher(Namespace.MissionProgramming, MissionProgrammingMessageType.ExecuteTestResponse), 
              new MessageMatchers.EventMessageMatcher(Program.TOPIC_PROGRAM_STATUS_CHANGED), 
              new MessageMatchers.EventMessageMatcher(Program.TOPIC_PROGRAM_TESTED));       
    }

    /**
     * Test requesting the test execution of a mission program from the template directory on the controller.
     * Verify success of the execution.
     */
    @Test
    public void testExecuteTest() throws UnknownHostException, IOException, InterruptedException
    {
        //message listener
        MessageListener listener = new MessageListener(m_Socket);
        
        m_MissionName = "remoteExecuteTestProgram";
                
        RemoteEventRegistration.regRemoteEventMessages(m_Socket, Program.TOPIC_PROGRAM_TESTED, 
                String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, m_MissionName));  
        
        //create schedule
        MissionProgramParametersGen.MissionProgramSchedule schedule = MissionProgramParametersGen.
                MissionProgramSchedule.newBuilder().setActive(true).setImmediately(false).
                setIndefiniteInterval(false).build();
        
        //create mission program parameters
        MissionProgramParametersGen.MissionProgramParameters params =
            MissionProgramNamespaceUtils.genTimedDataLoopParams(schedule, ASSET_NAME, m_MissionName);
        
        //load the parameters
        MissionProgramNamespaceUtils.loadParameters(m_Socket, params);
        
        //remove template message
        ExecuteTestRequestData requestMessage = ExecuteTestRequestData.newBuilder().
                setMissionName(m_MissionName).build();
        
        //create execution message
        TerraHarvestMessage message = 
                createMissionProgramMessage(MissionProgrammingMessageType.ExecuteTestRequest, requestMessage);
        
        // send out message
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        //wait for event as well, response message for test request will come back immediately
        listener.waitForMessages(1500,
              new BasicMessageMatcher(Namespace.MissionProgramming, MissionProgrammingMessageType.ExecuteTestResponse), 
              new MessageMatchers.EventMessageMatcher(Program.TOPIC_PROGRAM_TESTED));
    }

    /**
     * Test requesting the execution of a mission program.
     * Verify success of the execution.
     */
    @Test
    public void testExecute() throws UnknownHostException, IOException, InterruptedException
    {
        //message listener
        MessageListener listener = new MessageListener(m_Socket);
        
        m_MissionName = "executeProgram";
                
        //register for the program executed event
        RemoteEventRegistration.regRemoteEventMessages(m_Socket, Program.TOPIC_PROGRAM_EXECUTED, 
                String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, m_MissionName));
        
        //create schedule
        MissionProgramParametersGen.MissionProgramSchedule schedule = MissionProgramParametersGen.
                MissionProgramSchedule.newBuilder().setActive(true).setImmediately(false).
                setIndefiniteInterval(false).build();

        //create mission program parameters
        MissionProgramParametersGen.MissionProgramParameters params = 
            MissionProgramNamespaceUtils.genTimedDataLoopParams(
                    schedule, ASSET_NAME, m_MissionName);
        
        //load the parameters
        MissionProgramNamespaceUtils.loadParameters(m_Socket, params);
        
        //execution message
        ExecuteTestRequestData requestMessage = ExecuteTestRequestData.newBuilder().
                setMissionName("executeProgram").build();
        
        //create execution message
        TerraHarvestMessage message = 
                createMissionProgramMessage(MissionProgrammingMessageType.ExecuteRequest, requestMessage);
        
        // send out message
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        try
        {
            //wait for execution and response, response is from the actual thread executing the mission program
            //remote reg ensures that the name matches that of this mission
            listener.waitForMessages(2500,
                new BasicMessageMatcher(Namespace.MissionProgramming, MissionProgrammingMessageType.ExecuteResponse),
                new MessageMatchers.EventMessageMatcher(Program.TOPIC_PROGRAM_EXECUTED));
        }
        finally
        {
            //cleanup
            MissionProgramNamespaceUtils.shutdownExecutedMission(m_Socket, m_MissionName);
        }
    }
    
    /**
     * Test requesting the canceling of a mission program.
     * Verify the mission is canceled.
     */
    @Test
    public void testCancel() throws UnknownHostException, IOException, InterruptedException
    {
        m_MissionName = "cancelProgram1";
        
        //create schedule
        MissionProgramParametersGen.MissionProgramSchedule schedule = MissionProgramParametersGen.
                MissionProgramSchedule.newBuilder().setActive(true).
                setStartInterval(System.currentTimeMillis() + 1000000).
                setStopInterval(System.currentTimeMillis() + 5000000).build();

        //create mission program parameters
        MissionProgramParametersGen.MissionProgramParameters params = 
            MissionProgramNamespaceUtils.genTimedDataLoopParams(schedule, ASSET_NAME, m_MissionName);

        //load the parameters
        MissionProgramNamespaceUtils.loadParameters(m_Socket, params);
        
        //cancel
        boolean cancelled = MissionProgramNamespaceUtils.cancelScheduledMission(m_Socket, m_MissionName);
        assertThat(cancelled, is(true));
    }
    
    /**
     * Test requesting the shutdown of a mission program.
     * Verify the mission is canceled.
     */
    @Test
    public void testShutdown() throws UnknownHostException, IOException, InterruptedException, InterruptedException
    {
        m_MissionName = "shutdownProgram";
        
        //create schedule
        MissionProgramParametersGen.MissionProgramSchedule schedule = MissionProgramParametersGen.
                MissionProgramSchedule.newBuilder().setActive(true).setImmediately(true).
                setStopInterval(System.currentTimeMillis() + 5000000).build();
         
        //need separate asset so it isn't attempted to remove twice
        UUID localAssetUuid = AssetNamespaceUtils.createAsset(m_Socket, ExampleAsset.class.getName(), "shutdownAsset1",
                null).getInfo().getUuid();
        
        //create mission program parameters
        MissionProgramParametersGen.MissionProgramParameters params = 
            MissionProgramNamespaceUtils.genTimedDataLoopParams(schedule, "shutdownAsset1", m_MissionName);

        //load the parameters
        MissionProgramNamespaceUtils.loadParameters(m_Socket, params);
        
        //execution shutdown
        MissionProgramNamespaceUtils.shutdownExecutedMission(m_Socket, m_MissionName);
        //cleanup
        AssetNamespaceUtils.removeAsset(m_Socket, localAssetUuid);
    }
    
    /**
     * Test requesting the status of a program.
     */
    @Test
    public void testGetProgramStatus() throws UnknownHostException, IOException, InterruptedException
    {
        //message listener
        MessageListener listener = new MessageListener(m_Socket);
        
        m_MissionName = "programStatusMission";
        
        //create schedule
        MissionProgramParametersGen.MissionProgramSchedule schedule = MissionProgramParametersGen.
                MissionProgramSchedule.newBuilder().setActive(true).setImmediately(false).
                setIndefiniteInterval(false).build();

        //create mission program parameters
        MissionProgramParametersGen.MissionProgramParameters params = 
            MissionProgramNamespaceUtils.genTimedDataLoopParams(
                    schedule, ASSET_NAME, m_MissionName);
        
        //load the parameters
        MissionProgramNamespaceUtils.loadParameters(m_Socket, params);
        
        //get the name of a program to query about
        String programName = MissionProgramNamespaceUtils.getAProgramName(m_Socket);
        
        //create mission program message
        GetProgramStatusRequestData request = GetProgramStatusRequestData.newBuilder().
                setMissionName(programName).build();
        
        //create terra harvest message   
        TerraHarvestMessage message = createMissionProgramMessage(MissionProgrammingMessageType.GetProgramStatusRequest,
                request);
        
        // send out message
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        //wait for execution and response, response is from the actual thread executing the mission program
        MissionProgrammingNamespace namespace = (MissionProgrammingNamespace)listener.waitForMessage(
                Namespace.MissionProgramming, MissionProgrammingMessageType.GetProgramStatusResponse, 1500);
        
        GetProgramStatusResponseData responseData = GetProgramStatusResponseData.parseFrom(namespace.getData());

        //verify success
        assertThat(responseData.getMissionStatus(), isOneOf(MissionStatus.EXECUTED, MissionStatus.EXECUTING, 
                MissionStatus.WAITING_INITIALIZED, MissionStatus.CANCELED, MissionStatus.EXECUTING_TEST, 
                MissionStatus.INITIALIZATION_ERROR, MissionStatus.SCHEDULED, MissionStatus.SCRIPT_ERROR, 
                MissionStatus.SHUTDOWN, MissionStatus.WAITING_UNINITIALIZED, MissionStatus.SHUTTING_DOWN, 
                MissionStatus.UNSATISFIED));
        assertThat(responseData.getMissionName(), is(programName));
    }

    /**
     * Test requesting the last test results of a program.
     */
    @Test
    public void testGetProgramLastTestResult() throws UnknownHostException, IOException, InterruptedException
    {
        //message listener
        MessageListener listener = new MessageListener(m_Socket);
        
        m_MissionName = "testResultProgram";
        
        RemoteEventRegistration.regRemoteEventMessages(m_Socket, Program.TOPIC_PROGRAM_TESTED, 
                String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, m_MissionName));
                
        //create schedule
        MissionProgramParametersGen.MissionProgramSchedule schedule = MissionProgramParametersGen.
                MissionProgramSchedule.newBuilder().setActive(true).setImmediately(false).
                setIndefiniteInterval(false).build();
        
        //create mission program parameters
        MissionProgramParametersGen.MissionProgramParameters params = 
            MissionProgramNamespaceUtils.genTimedDataLoopParams(schedule, ASSET_NAME, m_MissionName);
        
        //load the parameters
        MissionProgramNamespaceUtils.loadParameters(m_Socket, params);
        
        //create execution message
        ExecuteTestRequestData requestMessage = ExecuteTestRequestData.newBuilder().
                setMissionName(m_MissionName).build();

        TerraHarvestMessage message = 
                createMissionProgramMessage(MissionProgrammingMessageType.ExecuteTestRequest, requestMessage);
        
        // send out message
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        //wait for execution and response, response is from the actual thread executing the mission program
        listener.waitForMessages(2000, new BasicMessageMatcher(Namespace.MissionProgramming, 
                MissionProgrammingMessageType.ExecuteTestResponse), 
                new MessageMatchers.EventMessageMatcher(Program.TOPIC_PROGRAM_TESTED));
        
        //create mission program message
        GetLastTestResultsRequestData request = GetLastTestResultsRequestData.newBuilder().
                setMissionName(m_MissionName).build();
        
        //create terra harvest message   
        message = createMissionProgramMessage(
                MissionProgrammingMessageType.GetLastTestResultsRequest, request);
        
        // send out message
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        //wait for execution and response, response is from the actual thread executing the mission program
        MissionProgrammingNamespace namespace = (MissionProgrammingNamespace)listener.waitForMessage(
                Namespace.MissionProgramming,  MissionProgrammingMessageType.GetLastTestResultsResponse, 500);
        
        GetLastTestResultsResponseData responseDataResult = 
                GetLastTestResultsResponseData.parseFrom(namespace.getData());

        //verify success
        assertThat(responseDataResult.getResult(), isOneOf(MissionTestResult.PASSED)); 
        assertThat(responseDataResult.getMissionName(), is(m_MissionName));
    }
    
    /**
     * Test requesting all the program information known for a system.
     */
    @Test
    public void testGetProgramInformation() throws UnknownHostException, IOException, InterruptedException
    {
        //message listener
        MessageListener listener = new MessageListener(m_Socket);
        
        m_MissionName = "testProgramInfo";
                
        //create schedule
        MissionProgramParametersGen.MissionProgramSchedule schedule = MissionProgramParametersGen.
                MissionProgramSchedule.newBuilder().setActive(true).setImmediately(false).
                setIndefiniteInterval(false).build();
        
        //create mission program parameters
        MissionProgramParametersGen.MissionProgramParameters params = 
            MissionProgramNamespaceUtils.genTimedDataLoopParams(schedule, ASSET_NAME, m_MissionName);
        
        //load the parameters
        MissionProgramNamespaceUtils.loadParameters(m_Socket, params);
        
        //create get program Information for all programs
        GetProgramInformationRequestData requestMessage = GetProgramInformationRequestData.getDefaultInstance();

        TerraHarvestMessage message = 
                createMissionProgramMessage(MissionProgrammingMessageType.GetProgramInformationRequest, requestMessage);
        
        // send out message
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        //wait for execution and response, response is from the actual thread executing the mission program
        MissionProgrammingNamespace namespace = (MissionProgrammingNamespace)listener.waitForMessage(
                Namespace.MissionProgramming, MissionProgrammingMessageType.GetProgramInformationResponse, 2000);
        
        GetProgramInformationResponseData responseData = 
                GetProgramInformationResponseData.parseFrom(namespace.getData());
        
        //might be greater than one if the controller has ran other missions not related to the remote tests
        assertThat(responseData.getMissionInfoCount(), greaterThanOrEqualTo(1));
        
        Map<String, Object> programs = new HashMap<String, Object>();
        for (ProgramInfo info : responseData.getMissionInfoList())
        {
            programs.put(info.getMissionName(), info);
        }
        assertThat(programs.get(m_MissionName), is(notNullValue()));
        ProgramInfo info = (ProgramInfo)programs.get(m_MissionName);
        assertThat(info.getTemplateName(), is("timed-data-capture-loop"));
        //an asset name, a string, and an int
        assertThat(info.getExecutionParamCount(), is(3));
        assertThat(info.getMissionSchedule().getActive(), is(true));
        assertThat(info.getMissionSchedule().getIndefiniteInterval(), is(false));
        assertThat(info.getMissionSchedule().getImmediately(), is(false));
    }
    
    /**
     * Test Getting the names of all the templates known to a system.
     * Verify that default two names of the templates that are deployed with the system are returned. 
     */
    @Test
    public void testGetTemplateNames() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(m_Socket);
        
        //create mission message
        TerraHarvestMessage message = TestMissionProgrammingNamespace.
                createMissionProgramMessage(MissionProgrammingMessageType.GetTemplateNamesRequest, null);
        
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        MissionProgrammingNamespace namespace = (MissionProgrammingNamespace)listener.waitForMessage(
                Namespace.MissionProgramming, MissionProgrammingMessageType.GetTemplateNamesResponse, 500);
        
        GetTemplateNamesResponseData response = GetTemplateNamesResponseData.parseFrom(namespace.getData());
        
        assertThat(response.getTemplateNameCount(), greaterThan(1));
        assertThat(response.getTemplateNameList(), hasItems("timed-data-capture-loop", "triggered-data-captured"));
    }
    
    /**
     * Test requesting the removal of a mission program.
     * Verify the mission is removed.
     */
    @Test
    public void testRemoval() throws UnknownHostException, IOException, InterruptedException
    {
        m_MissionName =  "removalProgram";
        
        //create schedule
        MissionProgramParametersGen.MissionProgramSchedule schedule = MissionProgramParametersGen.
                MissionProgramSchedule.newBuilder().setActive(true).
                setStartInterval(System.currentTimeMillis() + 1000000).
                setStopInterval(System.currentTimeMillis() + 5000000).build();

        //create mission program parameters
        MissionProgramParametersGen.MissionProgramParameters params = 
            MissionProgramNamespaceUtils.genTimedDataLoopParams(schedule, ASSET_NAME, m_MissionName);
       
        //load the parameters
        MissionProgramNamespaceUtils.loadParameters(m_Socket, params);
        //cancel the mission
        MissionProgramNamespaceUtils.cancelScheduledMission(m_Socket, m_MissionName);
    }
    
    /**
     * Send a request to unregister all event handlers.
     * Verify response is sent back.
     */
    @Test
    public void testUnregisterEventHandlers() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(m_Socket);
        
        //request
        TerraHarvestMessage message = createMissionProgramMessage(
                MissionProgrammingMessageType.UnregisterEventHandlerRequest, null);
        
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        //there is no data message so only need to verify response
        listener.waitForMessage( Namespace.MissionProgramming, 
                MissionProgrammingMessageType.UnregisterEventHandlerResponse, 500);
    }
    
    /**
     * Send a request to stop all managed executors.
     * Verify response is sent back.
     */
    @Test
    public void testStopAllManagedExecutors() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(m_Socket);
        
        //request data
        ManagedExecutorsShutdownRequestData request = ManagedExecutorsShutdownRequestData.newBuilder().
                setShutdownNow(true).build();
        
        //request
        TerraHarvestMessage message = createMissionProgramMessage(
                MissionProgrammingMessageType.ManagedExecutorsShutdownRequest, request);
        
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        listener.waitForMessage(
                Namespace.MissionProgramming, MissionProgrammingMessageType.ManagedExecutorsShutdownResponse, 500);
    }
    
    /**
     * Helper method that constructs terra harvest messages.
     * @param type
     *     the mission programming type of the message
     * @param message
     *     the mission programming message
     * @return
     *     terra harvest message that hold the mission programming namespace message
     */
    public static TerraHarvestMessage createMissionProgramMessage(final MissionProgrammingMessageType type, 
        final Message message)
    {
        MissionProgrammingNamespace.Builder missionMessageBuilder = MissionProgrammingNamespace.newBuilder();
        missionMessageBuilder.setType(type);
        if (message != null)
        {
            missionMessageBuilder.setData(message.toByteString());
        }
        
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.MissionProgramming,
                missionMessageBuilder);
        return thMessage;
    }
    
    /**
     * Load proto created test template.
     */
    private void loadProtoTestTemplate() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(m_Socket);
        
        //construct template
        MissionProgramTemplateGen.MissionVariableMetaData varData = 
                MissionProgramTemplateGen.MissionVariableMetaData.newBuilder().
            setBase(VariableMetaData.newBuilder().
                setName("variable").
                setDescription("I describe a variable").
                setDefaultValue("My Asset").
                setHumanReadableName("human readable name")).
            setType(MissionVariableTypes.Enum.ASSET).
            build();
        MissionProgramTemplateGen.MissionProgramTemplate template = MissionProgramTemplateGen.
            MissionProgramTemplate.newBuilder().
            setName("TEST_TEMPLATE_REMOTE").
            setSource("TheSource!@@#(*&$^%_@##$").
            setDescription("Something").
            addVariableMetaData(varData).build();
        //construct template message        
        LoadTemplateRequestData programMessage = MissionProgramMessages.LoadTemplateRequestData.newBuilder().
            setMission(template).build();
        
        //create terra harvest message
        TerraHarvestMessage message = createMissionProgramMessage(MissionProgrammingMessageType.LoadTemplateRequest, 
            programMessage);
        
        // send out message
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        //verify correct namespace, and response type
        listener.waitForMessage(Namespace.MissionProgramming, MissionProgrammingMessageType.LoadTemplateResponse, 
                    500);
    }
}
