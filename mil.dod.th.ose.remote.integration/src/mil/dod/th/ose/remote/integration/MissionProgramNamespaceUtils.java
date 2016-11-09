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
package mil.dod.th.ose.remote.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.net.Socket;

import com.google.protobuf.Message;

import mil.dod.th.core.mp.Program;
import mil.dod.th.core.remote.proto.MissionProgramMessages;
import mil.dod.th.core.remote.proto.MissionProgramMessages.CancelProgramRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.CancelProgramResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ExecuteShutdownRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetProgramNamesResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.LoadParametersRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionStatus;
import mil.dod.th.core.remote.proto.MissionProgramMessages.RemoveMissionProgramRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace.MissionProgrammingMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype.Type;
import mil.dod.th.ose.remote.integration.namespace.TestMissionProgrammingNamespace;
import mil.dod.th.remote.lexicon.mp.model.MissionProgramParametersGen;
import mil.dod.th.remote.lexicon.types.SharedTypesGen;

/**
 * Utility class to assist with testing remote mission programming interactions.
 * @author callen
 *
 */
public class MissionProgramNamespaceUtils
{ 
    /**
     * Generate mission program parameters for a mission using the 
     * timed-data-capture-loop template.
     * @return
     *     the mission program parameters.
     */
    public static MissionProgramParametersGen.MissionProgramParameters 
        genTimedDataLoopParams(final MissionProgramParametersGen.MissionProgramSchedule schedule, 
         final String assetName, final String programName)
    {       
        MissionProgramParametersGen.MissionProgramParameters params =  MissionProgramParametersGen.
                MissionProgramParameters.newBuilder().
                setTemplateName("timed-data-capture-loop").
                setSchedule(schedule).
                addParameters(SharedTypesGen.MapEntry.newBuilder().setKey("asset").
                        setValue(Multitype.newBuilder().
                                setType(Type.STRING).setStringValue(assetName).build()).build()).
                addParameters(SharedTypesGen.MapEntry.newBuilder().setKey("initTimeDelay").
                        setValue(Multitype.newBuilder().
                                 setType(Type.INT32).setInt32Value(2).build()).build()).
                addParameters(SharedTypesGen.MapEntry.newBuilder().setKey("timeUnitStr").
                        setValue(Multitype.newBuilder().
                                 setType(Type.STRING).setStringValue("Millisecond").build()).build()).
                setProgramName(programName).build();
        return params;
    }
    
    /**
     * Return the name of a mission program known to the connected system.
     * @param socket
     *     the socket to use
     * @return name
     *     a mission program name
     */
    public static String getAProgramName(final Socket socket) throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        //create mission message
        TerraHarvestMessage message = TestMissionProgrammingNamespace.
                createMissionProgramMessage(MissionProgrammingMessageType.GetProgramNamesRequest, null);
        
        message.writeDelimitedTo(socket.getOutputStream());
        
        //get the response
        Message responseRcvd = listener.waitForMessage(
                Namespace.MissionProgramming, MissionProgrammingMessageType.GetProgramNamesResponse, 500);
        
        MissionProgrammingNamespace namespace = (MissionProgrammingNamespace)responseRcvd;
        GetProgramNamesResponseData response = GetProgramNamesResponseData.parseFrom(namespace.getData());
        
        assertThat(response.getMissionNameCount(), greaterThanOrEqualTo(1));
        return response.getMissionNameList().get(0);
    }
    
    /**
     * Load the given parameters to the remote system.
     * @param socket
     *     the socket to use
     * @param parameters
     *     the parameters to send
     *
     */
    public static void loadParameters(final Socket socket, 
            final MissionProgramParametersGen.MissionProgramParameters parameters) throws IOException, 
            InterruptedException
    {
        MessageListener listener = new MessageListener(socket);

        //create mission program parameters request
        LoadParametersRequestData programMessage = MissionProgramMessages.LoadParametersRequestData.newBuilder().
            setParameters(parameters).build();
        
        //create terra harvest message   
        TerraHarvestMessage message = TestMissionProgrammingNamespace.createMissionProgramMessage(
            MissionProgrammingMessageType.LoadParametersRequest, programMessage);
        
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());
        
        Message responseRcvd = listener.waitForMessage(
                Namespace.MissionProgramming, MissionProgrammingMessageType.LoadParametersResponse, 800);
        
        MissionProgrammingNamespace namespace = (MissionProgrammingNamespace)responseRcvd;
        assertThat(namespace.getType(), is(MissionProgrammingMessageType.LoadParametersResponse));
    }
    
    /**
     * Clean up a mission created. This assumes that the mission is in an executed state.
     * @param socket
     *     the socket to use
     * @return name
     *     a mission program name
     */
    public static boolean shutdownExecutedMission(final Socket socket, final String missionName) 
        throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        int regId = RemoteEventRegistration.regRemoteEventMessages(socket, Program.TOPIC_PROGRAM_SHUTDOWN, 
                String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, missionName));
        //create shutdown message
        ExecuteShutdownRequestData requestMessage = 
                ExecuteShutdownRequestData.newBuilder().setMissionName(missionName).build();
        
        //create shutdown message
        TerraHarvestMessage message = 
            TestMissionProgrammingNamespace.createMissionProgramMessage(
                    MissionProgrammingMessageType.ExecuteShutdownRequest, requestMessage);
        
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());
        
        //wait for execution and response, response is from the actual thread executing the mission program
        try
        {
            listener.waitForRemoteEvent(Program.TOPIC_PROGRAM_SHUTDOWN, 2000);
            return true;
        }
        finally
        {
            MessageListener.unregisterEvent(regId, socket);
        }
    }
    
    /**
     * Clean up a mission created. This assumes that the mission is in a scheduled state.
     * @param socket
     *     the socket to use
     * @return name
     *     a mission program name
     */
    public static boolean cancelScheduledMission(final Socket socket, final String missionName) 
        throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        //execution message
        CancelProgramRequestData requestMessage = CancelProgramRequestData.newBuilder().
                setMissionName(missionName).build();
        
        //create cancel message
        TerraHarvestMessage message = 
            TestMissionProgrammingNamespace.createMissionProgramMessage(
                    MissionProgrammingMessageType.CancelProgramRequest, requestMessage);
        
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());
        
        //wait for execution and response, response is from the actual thread executing the mission program
        Message responseRcvd = listener.waitForMessage(
            Namespace.MissionProgramming, MissionProgrammingMessageType.CancelProgramResponse, 1000);

        MissionProgrammingNamespace namespace = (MissionProgrammingNamespace)responseRcvd;
        CancelProgramResponseData responseData = CancelProgramResponseData.parseFrom(namespace.getData());

        //verify success
        assertThat(responseData.getMissionStatus(), isOneOf(MissionStatus.CANCELED));
        assertThat(responseData.getMissionName(), is(missionName));
        return true;
    }
    
    /**
     * Remove a mission program. Assumes that the mission is canceled or shutdown.
     * @param missionName 
     *      the name of the mission program
     * @param socket
     *      the socket to use for communication to the controller
     */
    public static void removeMissionProgram(final String missionName, final Socket socket) throws IOException, 
        InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        //removal message
        RemoveMissionProgramRequestData requestMessage = RemoveMissionProgramRequestData.newBuilder().
                setMissionName(missionName).build();
        
        //create execution message
        TerraHarvestMessage message = TestMissionProgrammingNamespace.
                createMissionProgramMessage(MissionProgrammingMessageType.RemoveMissionProgramRequest, requestMessage);
        
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());
        
        //wait for execution and response, response is from the actual thread executing the mission program
        listener.waitForMessage(Namespace.MissionProgramming, 
                MissionProgrammingMessageType.RemoveMissionProgramResponse, 1000);
    }
}