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

import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetAssetsResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace.
    AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.AssetMessages;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace.AssetMessageType;
import mil.dod.th.core.remote.proto.AssetMessages.CaptureDataRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.CaptureDataResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.CaptureDataResponseData.ObservationCase;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.GetActiveStatusRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.GetActiveStatusResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.GetLastStatusRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.GetLastStatusResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.GetNameResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.PerformBitRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.PerformBitResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.core.types.command.CommandResponseEnum;
import mil.dod.th.core.types.command.CommandTypeEnum;
import mil.dod.th.ose.remote.integration.AssetNamespaceUtils;
import mil.dod.th.ose.remote.integration.MessageListener;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import mil.dod.th.remote.converter.CommandResponseEnumConverter;
import mil.dod.th.remote.converter.CommandTypeEnumConverter;
import mil.dod.th.remote.lexicon.asset.commands.BaseTypesGen;
import mil.dod.th.remote.lexicon.asset.commands.GetPanTiltCommandGen.GetPanTiltCommand;
import mil.dod.th.remote.lexicon.asset.commands.GetPanTiltResponseGen.GetPanTiltResponse;
import mil.dod.th.remote.lexicon.asset.commands.GetPositionCommandGen.GetPositionCommand;
import mil.dod.th.remote.lexicon.asset.commands.GetPositionResponseGen.GetPositionResponse;
import mil.dod.th.remote.lexicon.asset.commands.SetPanTiltCommandGen.SetPanTiltCommand;
import mil.dod.th.remote.lexicon.asset.commands.SetPanTiltResponseGen.SetPanTiltResponse;
import mil.dod.th.remote.lexicon.asset.commands.SetPositionCommandGen.SetPositionCommand;
import mil.dod.th.remote.lexicon.observation.types.ObservationGen;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.AzimuthDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.Coordinates;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.LatitudeWgsDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.LongitudeWgsDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.OrientationOffset;
import mil.dod.th.remote.lexicon.types.status.StatusTypesGen;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.Message;

import example.asset.ExampleAsset;
import example.asset.lexicon.ExampleCommandAsset;

/**
 * Tests the interaction of the remote interface with the {@link AssetNamespace}.  Specifically, 
 * the class tests that AssetMessageService messages are properly sent and that appropriate responses are
 * received.    
 * @author bachmakm
 *
 */
public class TestAssetNamespace
{
    private static SharedMessages.UUID testUuid;
    private static Socket socket;
    
    /**
     * Creates an example asset to be used for each of the unit tests.
     */
    @Before
    public void setup() throws UnknownHostException, IOException, InterruptedException
    {
        socket = SocketHostHelper.connectToController();
        CreateAssetResponseData createAssetResponse = AssetNamespaceUtils.createAsset(socket,
                ExampleAsset.class.getName(), null, null);
        testUuid = createAssetResponse.getInfo().getUuid();
    }
    
    /**
     * Removes the example asset used for each of the unit tests. 
     */
    @After
    public void teardown() throws UnknownHostException, IOException, InterruptedException
    {     
        try 
        {
            AssetNamespaceUtils.removeAsset(socket, testUuid);
        }
        finally
        {
            socket.close();
        }
    }
    
    /**
     * Verifies ability of system to remotely get the status of a single asset 
     */
    @Test
    public final void testGetStatus() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        //activate the asset
        AssetNamespaceUtils.activateAsset(socket, testUuid);
        
        //Build get status request message
        GetLastStatusRequestData request = GetLastStatusRequestData.newBuilder().
                setUuid(testUuid).build();    
        
        TerraHarvestMessage message = AssetNamespaceUtils.createAssetMessage(
                AssetMessageType.GetLastStatusRequest, request);        
        message.writeDelimitedTo(socket.getOutputStream());
        
        //listen for messages for a specific time interval
        AssetNamespace response;
        try
        {
            response = (AssetNamespace)listener.waitForMessage(Namespace.Asset, 
                    AssetMessageType.GetLastStatusResponse, 500);
            GetLastStatusResponseData getStatusResponse = GetLastStatusResponseData.parseFrom(response.getData());

            ObservationGen.Observation obs = getStatusResponse.getStatusObservationNative();
            assertThat(obs.getStatus().getSummaryStatus().getSummary(), 
                    is(StatusTypesGen.SummaryStatus.Enum.GOOD));
            assertThat(obs.getStatus().getSummaryStatus().getDescription(), 
                    is("Asset Activated"));
            assertThat(getStatusResponse.getAssetUuid(), is(testUuid));
        }
        finally
        {
            // cleanup
            AssetNamespaceUtils.deactivateAsset(socket, testUuid);
        }
    }
    
    /**
     * Verifies ability of system to remotely have an asset perform a built-in-test. 
     */
    @Test
    public final void testPerformBIT() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        //build request
        PerformBitRequestData request = PerformBitRequestData.newBuilder().
                setUuid(testUuid).build();        

        TerraHarvestMessage message = AssetNamespaceUtils.createAssetMessage(
                AssetMessageType.PerformBitRequest, request);
        message.writeDelimitedTo(socket.getOutputStream());

        //listen for messages for a specific time interval
        AssetNamespace response = (AssetNamespace)listener.waitForMessage(Namespace.Asset, 
                AssetMessageType.PerformBitResponse, 500); 
        PerformBitResponseData dataResponse = PerformBitResponseData.parseFrom(response.getData());

        assertThat(dataResponse.getAssetUuid(), is(testUuid));
        ObservationGen.Observation obs = dataResponse.getStatusObservationNative();
        assertThat(obs.getStatus().getSummaryStatus().getSummary(), 
                is(StatusTypesGen.SummaryStatus.Enum.GOOD));
        assertThat(obs.getStatus().getSummaryStatus().getDescription(), is("BIT Passed"));
    }
    
    /**
     * Verifies ability of the system to have an asset remotely capture data. 
     */
    @Test
    public final void testCaptureData_UuidOnlyResponse() throws IOException, AssetException, ObjectConverterException, 
        InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        // build request
        CaptureDataRequestData request = CaptureDataRequestData.newBuilder()
                .setUuid(testUuid)
                .setObservationFormat(RemoteTypesGen.LexiconFormat.Enum.UUID_ONLY)
                .build();        
        
        TerraHarvestMessage message = AssetNamespaceUtils.createAssetMessage(
                AssetMessageType.CaptureDataRequest, request);         
        message.writeDelimitedTo(socket.getOutputStream());
        
        //listen for messages for a specific time interval
        Message responseRcvd = listener.waitForMessage(Namespace.Asset, 
                AssetMessageType.CaptureDataResponse, 1200);  
        AssetNamespace response = (AssetNamespace)responseRcvd;
        CaptureDataResponseData dataResponse = CaptureDataResponseData.parseFrom(response.getData());
        
        //ensure that observation UUID is set
        assertThat(dataResponse.getObservationCase(), is(ObservationCase.OBSERVATIONUUID));
        assertThat(dataResponse.hasObservationUuid(), is(true));
    }

    /**
     * Verifies ability of the system to have an asset remotely capture data with the sensor ID parameter. 
     */
    @Test
    public final void testCaptureData_SensorId() throws IOException, AssetException, ObjectConverterException, 
        InterruptedException
    {
        CreateAssetResponseData createAssetResponse = AssetNamespaceUtils.createAsset(socket,
                ExampleCommandAsset.class.getName(), null, null);
        SharedMessages.UUID sensorAssetUuid = createAssetResponse.getInfo().getUuid();

        MessageListener listener = new MessageListener(socket);

        // build request
        CaptureDataRequestData request = CaptureDataRequestData.newBuilder()
                .setUuid(sensorAssetUuid)
                .setObservationFormat(RemoteTypesGen.LexiconFormat.Enum.UUID_ONLY)
                .setSensorId("sensor-id")
                .build();        
        
        TerraHarvestMessage message = AssetNamespaceUtils.createAssetMessage(
                AssetMessageType.CaptureDataRequest, request);         
        message.writeDelimitedTo(socket.getOutputStream());
        
        //listen for messages for a specific time interval
        Message responseRcvd = listener.waitForMessage(Namespace.Asset, 
                AssetMessageType.CaptureDataResponse, 1200);  
        AssetNamespace response = (AssetNamespace)responseRcvd;
        CaptureDataResponseData dataResponse = CaptureDataResponseData.parseFrom(response.getData());
        
        //ensure that observation UUID is set
        assertThat(dataResponse.getObservationCase(), is(ObservationCase.OBSERVATIONUUID));
        assertThat(dataResponse.hasObservationUuid(), is(true));
        assertThat(dataResponse.getSensorId(), is("sensor-id"));

        AssetNamespaceUtils.removeAsset(socket, sensorAssetUuid);
    }

    /**
     * Verifies ability of the system to have an asset remotely capture data. 
     */
    @Test
    public final void testCaptureData_ObsResponse() throws IOException, AssetException, ObjectConverterException, 
        InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        // build request
        CaptureDataRequestData request = CaptureDataRequestData.newBuilder().
                setUuid(testUuid).build();        
        
        TerraHarvestMessage message = AssetNamespaceUtils.createAssetMessage(
                AssetMessageType.CaptureDataRequest, request);
        message.writeDelimitedTo(socket.getOutputStream());
        
        //listen for messages for a specific time interval
        Message responseRcvd = listener.waitForMessage(Namespace.Asset, 
                AssetMessageType.CaptureDataResponse, 1200);  
        AssetNamespace response = (AssetNamespace)responseRcvd;
        CaptureDataResponseData dataResponse = CaptureDataResponseData.parseFrom(response.getData());
        
        //ensure that observation uuid is set
        assertThat(dataResponse.getObservationUuid(), is(notNullValue()));
        
        //ensure that observation data is set
        assertThat(dataResponse.getObservationCase(), is(ObservationCase.OBSERVATIONNATIVE)); 
        assertThat(dataResponse.getObservationNative().getAssetLocation().getLatitude().getValue(), is(74.0));
        assertThat(dataResponse.getObservationNative().getAssetLocation().getLongitude().getValue(), is(54.0));
        assertThat(dataResponse.getObservationNative().getDetection().getTargetId(), is("example-target-id"));
        assertThat(dataResponse.getObservationNative().getDetection().getTargetFrequency().getValue(), is((double)30));
        assertThat(dataResponse.getObservationNative().getDetection().getTargetLocation().getLatitude().getValue(), 
                is(50.0));
        assertThat(dataResponse.getObservationNative().getDetection().getTargetLocation().getLongitude().getValue(), 
                is(70.0));
    }
     
    /**
     * Verifies ability to execute a command remotely and get the response.
     * 
     * Test a set command.
     */
    @Test
    public final void testExecuteCommandSet() throws IOException, InterruptedException
    {
        SetPanTiltCommand command = SetPanTiltCommand.newBuilder().
                setBase(BaseTypesGen.Command.getDefaultInstance()).
                setPanTilt(OrientationOffset.newBuilder().
                        setAzimuth(AzimuthDegrees.newBuilder().setValue(20).build()).build()).build();
        
        //send the command
        ExecuteCommandResponseData executeCommandResponse = AssetNamespaceUtils.
                executeCommand(command, 
                        CommandTypeEnumConverter.convertJavaEnumToProto(CommandTypeEnum.SET_PAN_TILT_COMMAND), 
                        testUuid, 
                        socket);

        assertThat(executeCommandResponse.getResponseType(), is(
                CommandResponseEnumConverter.convertJavaEnumToProto(CommandResponseEnum.SET_PAN_TILT_RESPONSE)));
        assertThat(executeCommandResponse.getUuid(), is(testUuid));                
        
        SetPanTiltResponse commandResponse = SetPanTiltResponse.parseFrom(executeCommandResponse.getResponse());
        assertThat(commandResponse, is(notNullValue()));
    }
    
    /**
     * Verifies ability to execute a command remotely that set the position of an asset which does NOT have GPS 
     * support.
     * 
     * Test a set command.
     */
    @Test
    public final void testExecuteCommandSetPosition() throws IOException, InterruptedException
    {
        Coordinates coord = Coordinates.newBuilder().
                setLatitude(LatitudeWgsDegrees.newBuilder().setValue(70.0).build()).
                setLongitude(LongitudeWgsDegrees.newBuilder().setValue(95.6).build()).build();
        
        SetPositionCommand command =  SetPositionCommand.newBuilder().
                setBase(BaseTypesGen.Command.getDefaultInstance()).setLocation(coord).build();
        
        ExecuteCommandResponseData executeCommandResponse = AssetNamespaceUtils.
                executeCommand(command, 
                        CommandTypeEnumConverter.convertJavaEnumToProto(CommandTypeEnum.SET_POSITION_COMMAND), 
                        testUuid, 
                        socket);
        
        assertThat(executeCommandResponse.getResponseType(), is(
                CommandResponseEnumConverter.convertJavaEnumToProto(CommandResponseEnum.SET_POSITION_RESPONSE)));
        assertThat(executeCommandResponse.getUuid(), is(testUuid));
    }
    
    /**
     * Verifies ability to execute a command remotely and get the response.
     * 
     * Test a Get command.
     */
    @Test
    public final void testExecuteCommandGetPosition() throws IOException, InterruptedException
    {
        Coordinates coord = Coordinates.newBuilder().
                setLatitude(LatitudeWgsDegrees.newBuilder().setValue(70.0).build()).
                setLongitude(LongitudeWgsDegrees.newBuilder().setValue(95.6).build()).build();
        
        SetPositionCommand command =  SetPositionCommand.newBuilder().
                setBase(BaseTypesGen.Command.getDefaultInstance()).setLocation(coord).build();
        //execute set
        AssetNamespaceUtils.executeCommand(command, 
                CommandTypeEnumConverter.convertJavaEnumToProto(CommandTypeEnum.SET_POSITION_COMMAND), 
                testUuid, socket);
        
        //verify we are able to execute a get position
        ExecuteCommandResponseData executeCommandResponse = AssetNamespaceUtils.
                executeCommand(GetPositionCommand.newBuilder().setBase(
                        BaseTypesGen.Command.getDefaultInstance()).build(), 
                        CommandTypeEnumConverter.convertJavaEnumToProto(CommandTypeEnum.GET_POSITION_COMMAND),
                        testUuid, socket);
        
        assertThat(executeCommandResponse.getResponseType(), is(
                CommandResponseEnumConverter.convertJavaEnumToProto(CommandResponseEnum.GET_POSITION_RESPONSE)));
        assertThat(executeCommandResponse.getUuid(), is(testUuid));
        
        GetPositionResponse commandResponse = GetPositionResponse.parseFrom(executeCommandResponse.getResponse());
        assertThat(commandResponse, is(notNullValue()));
        
        Coordinates coords = commandResponse.getLocation();
        
        assertThat(coords.getLatitude().getValue(), is(70.0));
    }
    
    /**
     * Verifies ability to execute a command remotely and get the response.
     * 
     * Test a get command
     * 
     */
    @Test
    public final void testExecuteCommandGet() throws IOException, InterruptedException
    {
        //set the value first before retrieving
        SetPanTiltCommand command = SetPanTiltCommand.newBuilder().
                setBase(BaseTypesGen.Command.getDefaultInstance()).
                setPanTilt(OrientationOffset.newBuilder().
                        setAzimuth(AzimuthDegrees.newBuilder().setValue(20).build()).build()).build();
        
        //send the command
        AssetNamespaceUtils.executeCommand(command, 
                CommandTypeEnumConverter.convertJavaEnumToProto(CommandTypeEnum.SET_PAN_TILT_COMMAND), 
                testUuid, socket);
 
        ExecuteCommandResponseData executeCommandResponse = AssetNamespaceUtils.executeCommand(
            GetPanTiltCommand.newBuilder().setBase(BaseTypesGen.Command.getDefaultInstance()).build(), 
            CommandTypeEnumConverter.convertJavaEnumToProto(CommandTypeEnum.GET_PAN_TILT_COMMAND), testUuid, socket);
        
        assertThat(executeCommandResponse.getResponseType(), is(
                CommandResponseEnumConverter.convertJavaEnumToProto(CommandResponseEnum.GET_PAN_TILT_RESPONSE)));
        assertThat(executeCommandResponse.getUuid(), is(testUuid));              
        
        GetPanTiltResponse commandResponse = GetPanTiltResponse.parseFrom(executeCommandResponse.getResponse());
        assertThat(commandResponse.getPanTilt().getAzimuth().getValue(), is(20d)); // same as set in set command test
    }
    
    /**
     * Verifies ability to execute a command remotely and get the response.
     * 
     * Test a command that will send an error response.
     */
    @Test
    public final void testExecuteCommandErrorResponse() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        // set to value out of range
        SetPanTiltCommand command = SetPanTiltCommand.newBuilder().
                setBase(BaseTypesGen.Command.getDefaultInstance()).
                setPanTilt(OrientationOffset.newBuilder().
                        setAzimuth(AzimuthDegrees.newBuilder().setValue(90).build()).build()).build();
        
        //Build get status request message
        ExecuteCommandRequestData request = ExecuteCommandRequestData.newBuilder()
                .setUuid(testUuid)
                .setCommandType(CommandTypeEnumConverter.convertJavaEnumToProto(CommandTypeEnum.SET_PAN_TILT_COMMAND))
                .setCommand(command.toByteString())
                .build();    
        
        TerraHarvestMessage message = AssetNamespaceUtils.createAssetMessage(
                AssetMessageType.ExecuteCommandRequest, request);        
        message.writeDelimitedTo(socket.getOutputStream());
        
        //listen for messages for a specific time interval
        listener.waitForMessage(Namespace.Base, BaseMessageType.GenericErrorResponse, 600);  
    }

    /**
     * Verifies the ability of the system to handle a remote request to activate an asset. 
     * Verifies the ability of the system to handle a remote request to get the status of an asset.
     */
    @Test
    public final void testActivateAsset() throws IOException, InterruptedException
    {        
        MessageListener listener = new MessageListener(socket);

        AssetNamespaceUtils.activateAsset(socket, testUuid);
        
        GetActiveStatusRequestData request2 = GetActiveStatusRequestData.newBuilder().
                setUuid(testUuid).build(); 
        
        TerraHarvestMessage message = AssetNamespaceUtils.createAssetMessage(
                AssetMessageType.GetActiveStatusRequest, request2);        
        message.writeDelimitedTo(socket.getOutputStream());
        
        //listen for messages for a specific time interval
        Message responseRcvd = listener.waitForMessage(Namespace.Asset, 
                AssetMessageType.GetActiveStatusResponse, 500);   
        AssetNamespace response = (AssetNamespace)responseRcvd;
        GetActiveStatusResponseData dataResponse = GetActiveStatusResponseData.parseFrom(response.getData());     
        
        assertThat(dataResponse.getStatus(), is(AssetMessages.AssetActiveStatus.ACTIVATED));
        assertThat(dataResponse.getUuid(), is(testUuid));        
        
        AssetNamespaceUtils.deactivateAsset(socket, testUuid);
    }

    /**
     * Test getting the name of an Asset layer.
     * Verify that the name is the expected value.
     */
    @Test
    public void testGetAssetName() throws IOException, InterruptedException
    {
        //get asset name response
        GetNameResponseData dataResponse = AssetNamespaceUtils.getAssetName(socket, testUuid);

        assertThat(dataResponse.getAssetName(), is(ExampleAsset.class.getSimpleName()));
    }
    
    /**
     * Verifies the ability of the system to handle a remote request to deactivate an asset. 
     * Verifies the ability of the system to handle a remote request to get the status of an asset.
     */
    @Test
    public final void testDeactivateAsset() throws IOException, InterruptedException
    {        
        MessageListener listener = new MessageListener(socket);

        AssetNamespaceUtils.activateAsset(socket, testUuid);
        
        AssetNamespaceUtils.deactivateAsset(socket, testUuid);   
        
        GetActiveStatusRequestData request2 = GetActiveStatusRequestData.newBuilder().
                setUuid(testUuid).build(); 
        
        TerraHarvestMessage message = AssetNamespaceUtils.createAssetMessage(
                AssetMessageType.GetActiveStatusRequest, request2);        
        message.writeDelimitedTo(socket.getOutputStream());
        
        //listen for messages for a specific time interval
        Message responseRcvd = listener.waitForMessage(Namespace.Asset, 
                AssetMessageType.GetActiveStatusResponse, 500);   
        AssetNamespace response = (AssetNamespace)responseRcvd;

        GetActiveStatusResponseData dataResponse = GetActiveStatusResponseData.parseFrom(response.getData());     
        
        assertThat(dataResponse.getStatus(), is(AssetMessages.AssetActiveStatus.DEACTIVATED));
        assertThat(dataResponse.getUuid(), is(testUuid));        
    }
    
    /**
     * Verifies the ability of the system to handle a remote request to remove an asset. 
     */
    @Test
    public final void testRemoveAsset() throws IOException, InterruptedException
    {
        SharedMessages.UUID uuid = AssetNamespaceUtils.createAsset(socket, ExampleAsset.class.getName(), "removeAsset", 
                null).getInfo().getUuid();
        
        MessageListener listener = new MessageListener(socket);
        
        AssetNamespaceUtils.removeAsset(socket, uuid);
        
        TerraHarvestMessage message = AssetNamespaceUtils.createAssetDirSvcNamespaceMessage(
                AssetDirectoryServiceMessageType.GetAssetsRequest, null);        
        message.writeDelimitedTo(socket.getOutputStream());
        
        //listen for messages for a specific time interval
        Message responseRcvd = listener.waitForMessage(Namespace.AssetDirectoryService, 
                AssetDirectoryServiceMessageType.GetAssetsResponse, 500);   
        
        AssetDirectoryServiceNamespace response = (AssetDirectoryServiceNamespace)responseRcvd;
        GetAssetsResponseData dataResponse = GetAssetsResponseData.
                parseFrom(response.getData());   
        //verify the asset has been removed, and its UUID does not appear in the asset info list
        for (SharedMessages.FactoryObjectInfo info : dataResponse.getAssetInfoList())
        {
            assertThat(info.getUuid(), is(not(uuid)));
        }
    }
}
