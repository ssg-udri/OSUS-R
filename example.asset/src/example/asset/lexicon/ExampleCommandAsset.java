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
package example.asset.lexicon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.commands.CaptureImageCommand;
import mil.dod.th.core.asset.commands.CaptureImageResponse;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.asset.commands.ConfigureProfileCommand;
import mil.dod.th.core.asset.commands.ConfigureProfileResponse;
import mil.dod.th.core.asset.commands.CreateActionListCommand;
import mil.dod.th.core.asset.commands.CreateActionListResponse;
import mil.dod.th.core.asset.commands.DetectTargetCommand;
import mil.dod.th.core.asset.commands.DetectTargetResponse;
import mil.dod.th.core.asset.commands.GetCameraSettingsCommand;
import mil.dod.th.core.asset.commands.GetCameraSettingsResponse;
import mil.dod.th.core.asset.commands.GetLiftCommand;
import mil.dod.th.core.asset.commands.GetLiftResponse;
import mil.dod.th.core.asset.commands.GetModeCommand;
import mil.dod.th.core.asset.commands.GetModeResponse;
import mil.dod.th.core.asset.commands.GetPanTiltCommand;
import mil.dod.th.core.asset.commands.GetPanTiltResponse;
import mil.dod.th.core.asset.commands.GetPointingLocationCommand;
import mil.dod.th.core.asset.commands.GetPointingLocationResponse;
import mil.dod.th.core.asset.commands.GetPositionCommand;
import mil.dod.th.core.asset.commands.GetPositionResponse;
import mil.dod.th.core.asset.commands.GetProfilesCommand;
import mil.dod.th.core.asset.commands.GetProfilesResponse;
import mil.dod.th.core.asset.commands.GetTuneSettingsCommand;
import mil.dod.th.core.asset.commands.GetTuneSettingsResponse;
import mil.dod.th.core.asset.commands.GetVersionCommand;
import mil.dod.th.core.asset.commands.GetVersionResponse;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.asset.commands.SetCameraSettingsCommand;
import mil.dod.th.core.asset.commands.SetCameraSettingsResponse;
import mil.dod.th.core.asset.commands.SetLiftCommand;
import mil.dod.th.core.asset.commands.SetLiftResponse;
import mil.dod.th.core.asset.commands.SetModeCommand;
import mil.dod.th.core.asset.commands.SetModeResponse;
import mil.dod.th.core.asset.commands.SetPanTiltCommand;
import mil.dod.th.core.asset.commands.SetPanTiltResponse;
import mil.dod.th.core.asset.commands.SetPointingLocationCommand;
import mil.dod.th.core.asset.commands.SetPointingLocationResponse;
import mil.dod.th.core.asset.commands.SetTuneSettingsCommand;
import mil.dod.th.core.asset.commands.SetTuneSettingsResponse;
import mil.dod.th.core.asset.commands.StartRecordingCommand;
import mil.dod.th.core.asset.commands.StartRecordingResponse;
import mil.dod.th.core.asset.commands.StopRecordingCommand;
import mil.dod.th.core.asset.commands.StopRecordingResponse;
import mil.dod.th.core.asset.commands.TargetRefinementCommand;
import mil.dod.th.core.asset.commands.TargetRefinementResponse;
import mil.dod.th.core.asset.commands.ZeroizeCommand;
import mil.dod.th.core.asset.commands.ZeroizeResponse;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.observation.types.Detection;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.types.FrequencyKhz;
import mil.dod.th.core.types.Mode;
import mil.dod.th.core.types.ModeEnum;
import mil.dod.th.core.types.Profile;
import mil.dod.th.core.types.SensingModality;
import mil.dod.th.core.types.SensingModalityEnum;
import mil.dod.th.core.types.SpeedMetersPerSecond;
import mil.dod.th.core.types.detection.DetectionTypeEnum;
import mil.dod.th.core.types.factory.SpatialTypesFactory;
import mil.dod.th.core.types.image.WhiteBalanceEnum;
import mil.dod.th.core.types.spatial.HaeMeters;
import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.DistanceMeters;
import mil.dod.th.core.types.spatial.ElevationDegrees;
import mil.dod.th.core.types.spatial.Ellipse;
import mil.dod.th.core.types.spatial.HeadingDegrees;
import mil.dod.th.core.types.spatial.LatitudeWgsDegrees;
import mil.dod.th.core.types.spatial.LongitudeWgsDegrees;
import mil.dod.th.core.types.spatial.Orientation;
import mil.dod.th.core.types.spatial.TrackElement;
import mil.dod.th.core.types.status.OperatingStatus;
import mil.dod.th.core.types.status.SummaryStatusEnum;

/**
 * An Example Asset that supports all the known canned commands.
 * 
 * @author nickmarcucci
 */
@Component(factory = Asset.FACTORY)
public class ExampleCommandAsset implements AssetProxy
{
    private LoggingService m_Logging;
    private AssetContext m_Context;
    
    @Reference
    public void setLogService(final LoggingService logging)
    {
        m_Logging = logging;
    }
    
    @Override
    public void initialize(final AssetContext context, final Map<String, Object> props)
    {
        m_Context = context;
    }
    
    @Override
    public void onActivate() throws AssetException
    {
        m_Logging.info("Example command asset activated");
        m_Context.setStatus(SummaryStatusEnum.GOOD, "Asset Activated");
    }
    
    @Override
    public void onDeactivate() throws AssetException
    {
        m_Logging.info("Example command asset deactivated");
        m_Context.setStatus(SummaryStatusEnum.OFF, "Asset Deactivated");
    }

    @Override
    public Observation onCaptureData() throws AssetException
    {
        return createDefaultObservation();
    }

    @Override
    public Observation onCaptureData(final String sensorId) throws AssetException
    {
        return createDefaultObservation().withSensorId(sensorId);
    }

    @Override
    public Status onPerformBit() throws AssetException
    {
        m_Logging.info("Performing BIT");
        return new Status().withSummaryStatus(new OperatingStatus(SummaryStatusEnum.GOOD, "BIT Passed"));
    }
    
    @Override 
    public Response onExecuteCommand(final Command capabilityCommand)
        throws CommandExecutionException
    {
        m_Logging.info(capabilityCommand.getClass().getSimpleName());
        m_Logging.info(capabilityCommand.toString());

        if (capabilityCommand instanceof SetPanTiltCommand)
        {
            return new SetPanTiltResponse();
        }
        else if (capabilityCommand instanceof GetPanTiltCommand)
        {
            //if these inputs are altered you must update gui integration tests 
            //TestAssetCommands
            return new GetPanTiltResponse().withPanTilt(SpatialTypesFactory.newOrientationOffset(22, 10));
        }
        else if (capabilityCommand instanceof GetCameraSettingsCommand)
        {
            //if these inputs are altered you must update gui integration tests 
            //TestAssetCommands
            return new GetCameraSettingsResponse().withZoom(1).withFocus(1).
                    withWhiteBalance(WhiteBalanceEnum.CLOUDY);
        }
        else if (capabilityCommand instanceof SetCameraSettingsCommand)
        {
            return new SetCameraSettingsResponse();
        }
        else if (capabilityCommand instanceof DetectTargetCommand)
        {
            return new DetectTargetResponse();
        }
        else if (capabilityCommand instanceof CaptureImageCommand)
        {
            return new CaptureImageResponse();
        }
        else if (capabilityCommand instanceof SetPointingLocationCommand)
        {
            return new SetPointingLocationResponse();
        }
        else if (capabilityCommand instanceof GetPointingLocationCommand)
        {
            LongitudeWgsDegrees longitude = new LongitudeWgsDegrees(0.02, 0.02, 0.02, 0.02, 0.02);
            LatitudeWgsDegrees latitude = new LatitudeWgsDegrees(0.03, 0.03, 0.03, 0.03, 0.03);
            HaeMeters hae = new HaeMeters(0.04, 0.04, 0.04, 0.04, 0.04);
            DistanceMeters majorAxis = new DistanceMeters(0.05, 0.05, 0.05, 0.05, 0.05);
            DistanceMeters minorAxis = new DistanceMeters(0.06, 0.06, 0.06, 0.06, 0.06);
            HeadingDegrees rotation = new HeadingDegrees(0.07, 0.07, 0.07, 0.07, 0.07);
            Ellipse ellipseRegion = new Ellipse(majorAxis, minorAxis, rotation);
            Coordinates coordinates = new Coordinates(longitude, latitude, hae, ellipseRegion);

            return new GetPointingLocationResponse()
                    .withReserved(ExampleAssetUtils.buildReservedList())
                    .withLocation(coordinates);
        }
        else if (capabilityCommand instanceof SetTuneSettingsCommand)
        {
            return new SetTuneSettingsResponse();
        }
        else if (capabilityCommand instanceof GetTuneSettingsCommand)
        {
            return new GetTuneSettingsResponse();
        }
        else if (capabilityCommand instanceof GetPositionCommand)
        {
            return new GetPositionResponse();
        }
        else if (capabilityCommand instanceof GetProfilesCommand)
        {
            List<Profile> profile = new ArrayList<>();
            profile.add(new Profile(new Mode(ModeEnum.MANUAL, "manual"), 
                    "description 1", null, null, null, "id1", "profile 1"));
            profile.add(new Profile(new Mode(ModeEnum.AUTO, "auto"), 
                    "description 2", null, null, null, "id2", "profile 2"));
            profile.add(new Profile(new Mode(ModeEnum.OFF, "off"), 
                    "description 3", null, null, null, "id3", "profile 3"));
            return new GetProfilesResponse()
                    .withReserved(ExampleAssetUtils.buildReservedList())
                    .withProfiles(profile);
        }
        else if (capabilityCommand instanceof ConfigureProfileCommand)
        {
            return new ConfigureProfileResponse();
        }
        else if (capabilityCommand instanceof GetVersionCommand)
        {
            return new GetVersionResponse()
                    .withReserved(ExampleAssetUtils.buildReservedList())
                    .withCurrentVersion("V R.2.d.2");
        }
        else if (capabilityCommand instanceof GetModeCommand)
        {
            Mode mode = new Mode(ModeEnum.AUTO, "Example Mode");
            return new GetModeResponse()
                    .withReserved(ExampleAssetUtils.buildReservedList())
                    .withMode(mode);
        }
        else if (capabilityCommand instanceof SetModeCommand)
        {
            return new SetModeResponse();
        }
        else if (capabilityCommand instanceof CreateActionListCommand)
        {
            return new CreateActionListResponse();
        }
        else if (capabilityCommand instanceof TargetRefinementCommand)
        {
            return new TargetRefinementResponse();
        }
        else if (capabilityCommand instanceof StartRecordingCommand)
        {
            return new StartRecordingResponse();
        }
        else if (capabilityCommand instanceof StopRecordingCommand)
        {
            return new StopRecordingResponse();
        }
        else if (capabilityCommand instanceof GetLiftCommand)
        {
            return new GetLiftResponse().withHeight(new DistanceMeters().withValue(1.0));
        }
        else if (capabilityCommand instanceof SetLiftCommand)
        {
            return new SetLiftResponse();
        }
        else if (capabilityCommand instanceof ZeroizeCommand)
        {
            return new ZeroizeResponse();
        }
        else
        {
            throw new CommandExecutionException("Could not execute specified command.");
        }
    }

    @Override
    public void updated(final Map<String, Object> props)
    {
        //nothing to update currently.
    }
    
    /**
     * Create a default observation with a detection.
     * @return
     *  a validated observation.
     */
    private Observation createDefaultObservation()
    {
        final Observation obs = new Observation();
        obs.withModalities(new SensingModality().withValue(SensingModalityEnum.ACOUSTIC));
        
        final LatitudeWgsDegrees lat = new LatitudeWgsDegrees().withValue(50.0);
        final LongitudeWgsDegrees lng = new LongitudeWgsDegrees().withValue(70.0);
        final Coordinates coords = new Coordinates().withLatitude(lat).withLongitude(lng);
        
        final Detection detection = new Detection();
        detection.withType(DetectionTypeEnum.TEST);
        detection.withTargetLocation(coords);
        final HeadingDegrees heading = new HeadingDegrees().withValue(20);
        final ElevationDegrees elevation = new ElevationDegrees().withValue(90);
        detection.withTargetOrientation(new Orientation(heading, elevation, null));
        detection.withTrackHistories(new TrackElement(coords, new SpeedMetersPerSecond(205.5, null, null, null, null), 
                new Orientation(heading, elevation, null), null, 2003L));
        detection.setTargetFrequency(new FrequencyKhz().withValue(30));
        detection.setTargetId("example-target-id");
        
        obs.setDetection(detection);
        
        return obs;
    }

    @Override
    public Set<Extension<?>> getExtensions()
    {
        return null;
    }
}
