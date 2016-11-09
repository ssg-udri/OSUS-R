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
package mil.dod.th.ose.shared.protoconverter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mil.dod.th.core.asset.capability.AssetCapabilities;
import mil.dod.th.core.asset.capability.CameraSettingsParameters;
import mil.dod.th.core.asset.capability.CommandCapabilities;
import mil.dod.th.core.asset.capability.DetectionCapabilities;
import mil.dod.th.core.mp.model.MissionProgramParameters;
import mil.dod.th.core.mp.model.MissionProgramSchedule;
import mil.dod.th.core.mp.model.MissionProgramTemplate;
import mil.dod.th.core.mp.model.MissionVariableMetaData;
import mil.dod.th.core.mp.model.MissionVariableTypesEnum;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.types.ComponentType;
import mil.dod.th.core.types.ComponentTypeEnum;
import mil.dod.th.core.types.DigitalMedia;
import mil.dod.th.core.types.Direction;
import mil.dod.th.core.types.DirectionEnum;
import mil.dod.th.core.types.MapEntry;
import mil.dod.th.core.types.Mode;
import mil.dod.th.core.types.ModeEnum;
import mil.dod.th.core.types.PowerWatts;
import mil.dod.th.core.types.TemperatureCelsius;
import mil.dod.th.core.types.Version;
import mil.dod.th.core.types.VoltageVolts;
import mil.dod.th.core.types.command.CommandTypeEnum;
import mil.dod.th.core.types.factory.SpatialTypesFactory;
import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.DistanceMeters;
import mil.dod.th.core.types.spatial.Orientation;
import mil.dod.th.core.types.status.AmbientStatus;
import mil.dod.th.core.types.status.AmbientType;
import mil.dod.th.core.types.status.AmbientTypeEnum;
import mil.dod.th.core.types.status.BatteryChargeLevel;
import mil.dod.th.core.types.status.ChargeLevelEnum;
import mil.dod.th.core.types.status.ComponentStatus;
import mil.dod.th.core.types.status.InternalArchiveStatus;
import mil.dod.th.core.types.status.OperatingStatus;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.ose.test.matchers.JaxbUtil;
import mil.dod.th.remote.lexicon.asset.capability.AssetCapabilitiesGen;
import mil.dod.th.remote.lexicon.capability.BaseCapabilitiesGen.BaseCapabilities;
import mil.dod.th.remote.lexicon.types.SharedTypesGen;
import mil.dod.th.remote.lexicon.types.command.CommandTypesGen;

import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.Message;

/**
 * Unit test for the JAXB Object converter implementation.
 * 
 * @author cweisenborn
 */
public class TestJaxbProtoObjectConverterImpl
{
    private JaxbProtoObjectConverterImpl m_SUT;
    private AssetCapabilities m_TestJaxbObject;
    private Message m_TestMessage;
    
    @Before
    public void setUp() throws ObjectConverterException
    {      
        //Create the example jaxb object to be passed to the converter.
        m_TestJaxbObject = new AssetCapabilities();
        m_TestJaxbObject.setDescription("testobject");
        m_TestJaxbObject.setProductName("testname");
        m_TestJaxbObject.setMaxFov(1.2);

        CommandCapabilities cmd = new CommandCapabilities()
            .withCaptureData(false)
            .withPerformBIT(true)
            .withSupportedModes(new Mode(ModeEnum.DIAGNOSTIC, "diagmode"), new Mode(ModeEnum.OTHER, "othermode"))
            .withSupportedCommands(CommandTypeEnum.GET_MODE_COMMAND, CommandTypeEnum.SET_MODE_COMMAND)
            .withCameraSettings(new CameraSettingsParameters()
                .withMaxExposureIndex(2)
                .withAutoFocusSupported(false)
                .withWhiteBalanceSupported(false)
                .withExposureModeSupported(false));
        m_TestJaxbObject.setCommandCapabilities(cmd);

        DetectionCapabilities detect = new DetectionCapabilities();
        detect.setDirectionOfTravel(true);
        m_TestJaxbObject.setDetectionCapabilities(detect);
        
        //Create example Protocol message based off a generated message
        BaseCapabilities.Builder basCapsBuilder = BaseCapabilities.newBuilder();
        basCapsBuilder.setDescription("testobject");
        basCapsBuilder.setProductName("testname");
        
        AssetCapabilitiesGen.AssetCapabilities.Builder assetCapExtBuilder =
            AssetCapabilitiesGen.AssetCapabilities.newBuilder();
        assetCapExtBuilder.setMaxFov(1.2);
        assetCapExtBuilder.setCommandCapabilities(AssetCapabilitiesGen.CommandCapabilities.newBuilder()
            .setCaptureData(false)
            .setPerformBIT(true)
            .addSupportedModes(SharedTypesGen.Mode.newBuilder()
                .setDescription("diagmode")
                .setValue(SharedTypesGen.Mode.Enum.DIAGNOSTIC))
            .addSupportedModes(SharedTypesGen.Mode.newBuilder()
                .setDescription("othermode")
                .setValue(SharedTypesGen.Mode.Enum.OTHER))
            .addSupportedCommands(CommandTypesGen.CommandType.Enum.GET_MODE_COMMAND)
            .addSupportedCommands(CommandTypesGen.CommandType.Enum.SET_MODE_COMMAND)
            .setCameraSettings(AssetCapabilitiesGen.CameraSettingsParameters.newBuilder()
                .setMaxExposureIndex(2)
                .setAutoFocusSupported(false)
                .setWhiteBalanceSupported(false)
                .setExposureModeSupported(false)));
        assetCapExtBuilder.setDetectionCapabilities(AssetCapabilitiesGen.DetectionCapabilities.newBuilder().
            setDirectionOfTravel(true));
        assetCapExtBuilder.setBase(basCapsBuilder.build());
        m_TestMessage = assetCapExtBuilder.build();
        
        //Instantiate the converter
        m_SUT = new JaxbProtoObjectConverterImpl(); 
    }
    
    /**
     * Verify the converters ability to go from a jaxb object to a proto message.
     */
    @Test
    public void testConvertToProto() throws ObjectConverterException
    {
        //Convert the object to a protocol message.
        Message convertedMessage = m_SUT.convertToProto(m_TestJaxbObject);
        
        //Check that the converted message matches the the test message created.
        assertThat(convertedMessage, equalTo(m_TestMessage));
    }
    
    /**
     * Verify the converters ability to go from a proto message to a jaxb object.
     */
    @Test
    public void testConvertToJAXB() throws ObjectConverterException
    {
        //Convert the message to an object and cast it to the appropriate object type.
        AssetCapabilities convertedObject = (AssetCapabilities)m_SUT.convertToJaxb(m_TestMessage);

        //Check each field within the converted object to make sure it matches the test object created.
        JaxbUtil.assertEqualContent(convertedObject, m_TestJaxbObject);
    }
    
    /**
     * Verify the converters ability to go from a jaxb object to a proto message and back.
     * This tests ensures that nested types are properly handled and shared types. 
     */
    @Test
    public void testConvertToProtoObservation() throws ObjectConverterException
    {
        //obs for comparison
        Observation obsToGoThereAndBack = createStatusObservation();
        
        //Convert the object to a protocol message.
        Message convertedMessage = m_SUT.convertToProto(obsToGoThereAndBack);

        //Convert the message to an object and cast it to the appropriate object type.
        Observation convertedObject = (Observation)m_SUT.convertToJaxb(convertedMessage);

        //Check that the converted message matches the the test obs created.
        JaxbUtil.assertEqualContent(obsToGoThereAndBack, convertedObject);
    }

    /**
     * Test converting mission parameter data.
     * Verifies multitype data is handled.
     */
    @Test
    public void testMissionParameters() throws ObjectConverterException
    {
        MissionProgramParameters params = new MissionProgramParameters()
            .withProgramName("herald")
            .withTemplateName("harry")
            .withParameters(new MapEntry("tomato", 23), new MapEntry("onion", "Charlies"))
            .withSchedule(new MissionProgramSchedule(true, true, false, 1L, true, 2L));
        
        Message paramsMessage = m_SUT.convertToProto(params);
        MissionProgramParameters convertedParams = (MissionProgramParameters)m_SUT.convertToJaxb(paramsMessage);
        
        //Check that the converted message matches the original.
        JaxbUtil.assertEqualContent(convertedParams, params);
    }
    
    /**
     * Test converting mission schedule data.
     * Verifies standalone mission type can be found and converted.
     */
    @Test
    public void testMissionSchedule() throws ObjectConverterException
    {
        MissionProgramSchedule schedule = new MissionProgramSchedule(true, true, false, 1L, true, 2L);
        
        Message scheduleMessage = m_SUT.convertToProto(schedule);
        MissionProgramSchedule convertedSchedule = (MissionProgramSchedule)m_SUT.convertToJaxb(scheduleMessage);
        
        //Check that the converted message matches the original.
        JaxbUtil.assertEqualContent(convertedSchedule, schedule);
    }
    
    /**
     * Test converting mission template data.
     * Verify variable metadata extensions can be translated.
     */
    @Test
    public void testMissionTemplate() throws ObjectConverterException
    {
        List<MissionVariableMetaData> missionVarData = new ArrayList<>();
        MissionVariableMetaData data1 = new MissionVariableMetaData("one", "2", null, "1", "4", "numbers", 
                "more numbers", MissionVariableTypesEnum.INTEGER);
        missionVarData.add(data1);
        MissionVariableMetaData data2 = new MissionVariableMetaData("blob", "", null, null, null, "link", 
                "more links", MissionVariableTypesEnum.LINK_LAYER);
        missionVarData.add(data2);
        MissionProgramTemplate template = new MissionProgramTemplate(
                "me", "sourcearoo", "description", "j", new DigitalMedia(new byte[]{1, 2}, ".jpeg"), 
                null, true, true, false, true, false, false, missionVarData);
        
        Message templateMessage = m_SUT.convertToProto(template);
        MissionProgramTemplate convertedTemplate = (MissionProgramTemplate)m_SUT.convertToJaxb(templateMessage);
        
        //Check that the converted message matches the original.
        JaxbUtil.assertEqualContent(convertedTemplate, template);
    }
    
    /**
     * Test converting coordinates and orientation. 
     * Verifies that 'types' data can be converted standalone.
     */
    @Test
    public void testCoordAndOrien() throws ObjectConverterException
    {
        Coordinates coords = SpatialTypesFactory.newCoordinates(23.098, 45.5);
        Orientation orien = SpatialTypesFactory.newOrientation(33.3, 14.5677, .09);
        
        Message coordsMessage = m_SUT.convertToProto(coords);
        Message orinMessage = m_SUT.convertToProto(orien);
        Coordinates convertedCoords = (Coordinates)m_SUT.convertToJaxb(coordsMessage);
        Orientation convertedOrien = (Orientation)m_SUT.convertToJaxb(orinMessage);
        
        //Check that the converted messages match the original.
        JaxbUtil.assertEqualContent(convertedCoords, coords);
        JaxbUtil.assertEqualContent(convertedOrien, orien);
    }
    
    /**
     * Test converting a SharedTypes.xsd defined type. 
     * Verifies that 'shared types' data can be converted standalone.
     */
    @Test
    public void testDirection() throws ObjectConverterException
    {
        Direction direction = new Direction()
            .withValue(DirectionEnum.FN)
            .withDescription("left-right-left-up");
        
        Message directionMessage = m_SUT.convertToProto(direction);
        Direction convertedDirection = (Direction)m_SUT.convertToJaxb(directionMessage);
        
        //Check that the converted message matches the original.
        JaxbUtil.assertEqualContent(convertedDirection, direction);
    }
    
    /**
     * Create an observation.
     */
    private Observation createStatusObservation()
    {
        ComponentStatus status1 = new ComponentStatus(new ComponentType(ComponentTypeEnum.NAVIGATION, "ins"),
                new OperatingStatus(SummaryStatusEnum.BAD, "no satellites"));
        ComponentStatus status2 = new ComponentStatus(new ComponentType(ComponentTypeEnum.DSP, "yup"),
                new OperatingStatus(SummaryStatusEnum.GOOD, "stuff"));
        
        List<ComponentStatus> componentStatuses = new ArrayList<>();
        componentStatuses.add(status1);
        componentStatuses.add(status2);
        BatteryChargeLevel batteryChargeLevel = 
                new BatteryChargeLevel(ChargeLevelEnum.FULL, null);
        Double sensorFov = 300.00;
        VoltageVolts batteryVoltage = new VoltageVolts().withValue(15.00);
        TemperatureCelsius temperature = new TemperatureCelsius().withValue(10.00);
        PowerWatts powerConsumption = new PowerWatts().withValue(10.00);
        VoltageVolts analogAnalogVoltage = new VoltageVolts().withValue(10.00);
        VoltageVolts analogDigitalVoltage = new VoltageVolts().withValue(10.00);
        VoltageVolts analogMagVoltage = new VoltageVolts().withValue(10.00);
        
        DistanceMeters sensorRange =
                new DistanceMeters(100.0, 1.0, 1.0, 1.0, 0.0);
        int assetOnTime = 10;
        
        InternalArchiveStatus internalArchiveStatus = new InternalArchiveStatus();
        internalArchiveStatus.setArchivingInProgress(true);
        internalArchiveStatus.setArchiveTimeAvailable(1000L);
        internalArchiveStatus.setTotalArchiveTime(3000L);
        int nextStatusDurationMs = 12000;
        
        List<AmbientStatus> ambientStatus = new ArrayList<>();
        ambientStatus.add(new AmbientStatus(
                new AmbientType(AmbientTypeEnum.OCCLUSION, "occlusion"), 
                    new OperatingStatus(SummaryStatusEnum.BAD, "occlusions are bad")));
        
        Status status = new Status(new OperatingStatus(SummaryStatusEnum.GOOD, "stuff too"),
                componentStatuses,
                ambientStatus,
                batteryChargeLevel,
                sensorRange,
                sensorFov,
                internalArchiveStatus, 
                temperature,
                batteryVoltage,
                powerConsumption,
                analogAnalogVoltage,
                analogDigitalVoltage,
                analogMagVoltage,
                assetOnTime,
                nextStatusDurationMs);

        UUID uuid = UUID.randomUUID(); 
        Long createdTimestamp = 100L;
        String assetName = "asset-name";
        String assetType = "asset-type";
        String sensorId = "sensor-id";
        boolean systemInTestMode = false;
        Version version = new Version(1,2);
        int systemId = 0123;
        return new Observation(version, null, null, null, null, null, null, null, status, null, null, null, null, null,
                null, null, null, null, null, null, uuid, null, createdTimestamp, null, assetName, assetType, sensorId, 
                systemInTestMode, systemId).withAssetUuid(UUID.randomUUID());
    }
}
