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
package example.asset.exception;

import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.observation.types.Detection;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.types.FrequencyKhz;
import mil.dod.th.core.types.SensingModality;
import mil.dod.th.core.types.SensingModalityEnum;
import mil.dod.th.core.types.SpeedMetersPerSecond;
import mil.dod.th.core.types.detection.DetectionTypeEnum;
import mil.dod.th.core.types.factory.SpatialTypesFactory;
import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.Orientation;
import mil.dod.th.core.types.spatial.TrackElement;
import mil.dod.th.core.types.status.OperatingStatus;
import mil.dod.th.core.types.status.SummaryStatusEnum;

/**
 * Example observation asset that generates exceptions when observations are created with duplicate data.
 * 
 * @author dlandoll
 */
@Component(factory = Asset.FACTORY)
public class ExampleObsExAsset implements AssetProxy
{
    private LoggingService m_Log;
    private AssetContext m_Context;
    private Detection m_Detection;
    private Coordinates m_AssetLocation;
    private Orientation m_AssetOrientation;
    private Coordinates m_TargetLocation;
    private Orientation m_TargetOrientation;
    private Coordinates m_HistoryLocation;
    private Orientation m_HistoryOrientation;

    @Reference
    public void setLogService(final LoggingService loggingService)
    {
        m_Log = loggingService;
    }
    
    @Override
    public void initialize(AssetContext context, Map<String,Object> props)
    {
        m_Context = context;
    }
    
    @Override
    public void updated(final Map<String, Object> props)
    {
        // Do nothing
    }

    @Override
    public void onActivate() throws AssetException
    {
        m_Context.setStatus(SummaryStatusEnum.GOOD, "Asset Activated");

        // Upon activation initialize detection variable used to generate duplicate errors
        m_Detection = new Detection();
        m_Detection.withType(DetectionTypeEnum.TEST);
        m_Detection.withTargetLocation(m_TargetLocation);
        m_Detection.withTargetOrientation(m_TargetOrientation);
        m_Detection.withTrackHistories(new TrackElement(m_HistoryLocation,
                new SpeedMetersPerSecond(205.5, null, null, null, null), m_HistoryOrientation, null, 2003L));
        m_Detection.setTargetFrequency(new FrequencyKhz().withValue(30));
        m_Detection.setTargetId("example-target-id");

        // Duplicate location data is used to verify that Observations can still be deleted when used
        m_AssetLocation = SpatialTypesFactory.newCoordinates(54, 74);
        m_AssetOrientation = SpatialTypesFactory.newOrientation(180.0, -80.0, 45.0);
        m_TargetLocation = m_AssetLocation;
        m_TargetOrientation = m_AssetOrientation;
        m_HistoryLocation = SpatialTypesFactory.newCoordinates(50.0, 70.0);
        m_HistoryOrientation = SpatialTypesFactory.newOrientation(1.5, 30.55, 55.55);
    }

    @Override
    public Observation onCaptureData()
    {
        final Observation obs = new Observation();
        obs.withModalities(new SensingModality().withValue(SensingModalityEnum.ACOUSTIC));

        // Reuse the detected to cause persistence exceptions if multiple observations are captured
        obs.setDetection(m_Detection);

        obs.setAssetLocation(m_AssetLocation);
        obs.setAssetOrientation(m_AssetOrientation);

        obs.setPlatformOrientation(SpatialTypesFactory.newOrientation(90.0, 10.0, 45.0));
        obs.setPointingLocation(m_AssetLocation);

        return obs;
    }

    @Override
    public Observation onCaptureData(final String sensorId) throws AssetException
    {
        throw new AssetException(
            new UnsupportedOperationException("ExampleObsExAsset does not support capturing data by sensorId."));
    }

    @Override
    public void onDeactivate() throws AssetException
    {
        m_Context.setStatus(SummaryStatusEnum.OFF, "Asset Deactivated");
    }

    @Override
    public Status onPerformBit() throws AssetException
    {
        m_Log.info("Performing BIT");
        return new Status().withSummaryStatus(new OperatingStatus(SummaryStatusEnum.GOOD, "BIT Passed"));
    }

    @Override
    public Response onExecuteCommand(final Command capabilityCommand)
            throws CommandExecutionException
    {
        throw new CommandExecutionException("Could not execute specified command.");
    }

    @Override
    public Set<Extension<?>> getExtensions()
    {
        return null;
    }
}
