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

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.observation.types.AudioMetadata;
import mil.dod.th.core.observation.types.Detection;
import mil.dod.th.core.observation.types.ImageMetadata;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.ObservationRef;
import mil.dod.th.core.observation.types.Relationship;
import mil.dod.th.core.observation.types.RoadCondition;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.observation.types.VideoMetadata;
import mil.dod.th.core.observation.types.Weather;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.core.types.DigitalMedia;
import mil.dod.th.core.types.FrequencyKhz;
import mil.dod.th.core.types.SensingModality;
import mil.dod.th.core.types.SensingModalityEnum;
import mil.dod.th.core.types.SpeedMetersPerSecond;
import mil.dod.th.core.types.TemperatureCelsius;
import mil.dod.th.core.types.VoltageVolts;
import mil.dod.th.core.types.audio.AudioRecorder;
import mil.dod.th.core.types.audio.AudioRecorderEnum;
import mil.dod.th.core.types.audio.AudioSampleOfInterest;
import mil.dod.th.core.types.detection.DetectionTypeEnum;
import mil.dod.th.core.types.image.Camera;
import mil.dod.th.core.types.image.CameraTypeEnum;
import mil.dod.th.core.types.image.ImageCaptureReason;
import mil.dod.th.core.types.image.ImageCaptureReasonEnum;
import mil.dod.th.core.types.observation.ObservationSubTypeEnum;
import mil.dod.th.core.types.observation.RelationshipTypeEnum;
import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.ElevationDegrees;
import mil.dod.th.core.types.spatial.HeadingDegrees;
import mil.dod.th.core.types.spatial.LatitudeWgsDegrees;
import mil.dod.th.core.types.spatial.LongitudeWgsDegrees;
import mil.dod.th.core.types.spatial.Orientation;
import mil.dod.th.core.types.spatial.TrackElement;
import mil.dod.th.core.types.status.OperatingStatus;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.core.types.weather.RoadConditionType;
import mil.dod.th.core.types.weather.RoadConditionTypeEnum;
import mil.dod.th.core.validator.ValidationFailedException;

/**
 * Produces observations with related observations.
 * @author nickmarcucci
 *
 */
@Component(factory = Asset.FACTORY)
public class ExampleRelatedObsAsset implements AssetProxy
{
    private LoggingService m_Log;
    private AssetContext m_Context;
    private WakeLock m_WakeLock;
    
    @Reference
    public void setLogService(final LoggingService loggingService)
    {
        m_Log = loggingService;
    }
    
    @Override
    public void initialize(final AssetContext context, final Map<String, Object> props)
    {
        m_Log.info("Activating example related observation instance");
        m_Context = context;
        checkWakeLock();
    }
    
    @Deactivate
    public void deactivateInstance()
    {
        m_Log.info("Deactivating example related observation instance");
        if (m_WakeLock != null)
        {
            m_WakeLock.delete();
            m_WakeLock = null;
        }
    }
    
    @Override
    public void onActivate() throws AssetException
    {
        try
        {
            tryActivateWakeLock();        
            m_Context.setStatus(SummaryStatusEnum.GOOD, "Asset Activated");
        }
        finally
        {
            tryDeactivateWakeLock();
        }
    }

    @Override
    public void onDeactivate() throws AssetException
    {
        try
        {
            tryActivateWakeLock();
            m_Log.info("Example related observation deactivated");

            m_Context.setStatus(SummaryStatusEnum.OFF, "Asset Deactivated");
        }
        finally
        {
            tryDeactivateWakeLock();
        }
    }

    @Override
    public void updated(final Map<String, Object> props)
    {
        //no properties to update
    }

    @Override
    public Observation onCaptureData() throws AssetException
    {
        try
        {
            tryActivateWakeLock();
            m_Log.info("Example Related Observation Asset Data captured");

            final LatitudeWgsDegrees lat = new LatitudeWgsDegrees().withValue(50.0);
            final LongitudeWgsDegrees lng = new LongitudeWgsDegrees().withValue(70.0);
            final Coordinates coords = new Coordinates().withLatitude(lat).withLongitude(lng);
            final Coordinates coords2 = new Coordinates().withLatitude(lat).withLongitude(lng);

            final Observation obs = new Observation();
            obs.setUuid(UUID.randomUUID());
            obs.withModalities(new SensingModality().withValue(SensingModalityEnum.ACOUSTIC));

            final Detection detection = new Detection();
            detection.setType(DetectionTypeEnum.TEST);
            final HeadingDegrees heading = new HeadingDegrees().withValue(50);
            final ElevationDegrees elevation = new ElevationDegrees().withValue(90);
            detection.withTargetLocation(coords);
            detection.withTargetOrientation(new Orientation(heading, elevation, null));
            detection.withTrackHistories(new TrackElement(coords, 
                    new SpeedMetersPerSecond(205.5, null, null, null, null), 
                    new Orientation(heading, elevation, null), null, 2003L));
            detection.setTargetFrequency(new FrequencyKhz().withValue(30));
            detection.setTargetId("example-target-id");

            obs.setDetection(detection);
            obs.setSensorId("12345");

            obs.setAssetLocation(coords2);
            try 
            {
                createAndSetRelatedObservations(obs);
            } 
            catch (ValidationFailedException e) 
            {
                throw new AssetException(e);
            }

            return obs;
        }
        finally
        {
            tryDeactivateWakeLock();
        }
    }

    @Override
    public Observation onCaptureData(final String sensorId) throws AssetException
    {
        throw new AssetException(
            new UnsupportedOperationException("ExampleRelatedObsAsset does not support capturing data by sensorId."));
    }

    /**
     * Creates and sets randomly generated related observations on the pased in observation.
     * @param observation
     *  the observation that requires related observations
     */
    private void createAndSetRelatedObservations(Observation observation) throws ValidationFailedException
    {
        ObservationRef obsRefStatusChild = new ObservationRef();
        Observation childObs = createARelatedObservation("12346", ObservationSubTypeEnum.STATUS, 
                observation, RelationshipTypeEnum.PARENT);
        obsRefStatusChild.setUuid(childObs.getUuid());
        obsRefStatusChild.setRelationship(new Relationship(RelationshipTypeEnum.CHILD, 
                ""));
        
        ObservationRef obsRefAudioChild = new ObservationRef();
        Observation childObsAudio = createARelatedObservation("12347", ObservationSubTypeEnum.AUDIO_METADATA, 
                observation, RelationshipTypeEnum.PARENT);
        obsRefAudioChild.setUuid(childObsAudio.getUuid());
        obsRefAudioChild.setRelationship(new Relationship(RelationshipTypeEnum.CHILD, 
                "child uuid " + childObsAudio.getUuid().toString()));
        
        ObservationRef obsRefImageChild = new ObservationRef();
        Observation childObsImage = createARelatedObservation("12348", ObservationSubTypeEnum.IMAGE_METADATA, 
                observation, RelationshipTypeEnum.PARENT);
        obsRefImageChild.setUuid(childObsImage.getUuid());
        obsRefImageChild.setRelationship(new Relationship(RelationshipTypeEnum.CHILD, 
                "sub image"));
        
        ObservationRef obsRefVideoChild = new ObservationRef();
        Observation childObsVideo = createARelatedObservation("12349", ObservationSubTypeEnum.VIDEO_METADATA, 

                observation, RelationshipTypeEnum.PARENT);
        obsRefVideoChild.setUuid(childObsVideo.getUuid());
        obsRefVideoChild.setRelationship(new Relationship(RelationshipTypeEnum.CHILD, 
                "child uuid " + childObsVideo.getUuid().toString()));
        
        ObservationRef obsRefDetectionChild = new ObservationRef();
        Observation childObsDetection = createARelatedObservation("12350", ObservationSubTypeEnum.DETECTION, 
                observation, RelationshipTypeEnum.PARENT);
        obsRefDetectionChild.setUuid(childObsDetection.getUuid());
        obsRefDetectionChild.setRelationship(new Relationship(RelationshipTypeEnum.CHILD, 
                "child uuid " + childObsDetection.getUuid().toString()));
        
        ObservationRef obsRefOrientChild = new ObservationRef();
        Observation childObsOrient = createARelatedObservation("12351", null, 
                observation, RelationshipTypeEnum.PARENT);
        obsRefOrientChild.setUuid(childObsOrient.getUuid());
        obsRefOrientChild.setRelationship(new Relationship(RelationshipTypeEnum.CHILD, 
                "child uuid " + childObsOrient.getUuid().toString()));
        
        ObservationRef obsRefParent = new ObservationRef();
        Observation parentObs = createARelatedObservation("12352", ObservationSubTypeEnum.WEATHER, 
                observation, RelationshipTypeEnum.CHILD);
        obsRefParent.setUuid(parentObs.getUuid());
        obsRefParent.setRelationship(new Relationship(RelationshipTypeEnum.PARENT, 
                "parent uuid " + parentObs.getUuid().toString()));
        
        ObservationRef obsRefPeer = new ObservationRef();
        Observation peerObs = createARelatedObservation("12353", null, 
                observation, RelationshipTypeEnum.PEER);
        obsRefPeer.setUuid(peerObs.getUuid());
        obsRefPeer.setRelationship(new Relationship(RelationshipTypeEnum.PEER, "peer uuid " 
                + peerObs.getUuid().toString()));
        
        ObservationRef refNotFoundWithDesc = new ObservationRef();
        refNotFoundWithDesc.setRelationship(new Relationship(RelationshipTypeEnum.PEER, "sub image"));
        refNotFoundWithDesc.setUuid(UUID.randomUUID());
        
        ObservationRef refNotFoundNoDesc = new ObservationRef();
        refNotFoundNoDesc.setRelationship(new Relationship(RelationshipTypeEnum.PEER, null));
        refNotFoundNoDesc.setUuid(UUID.randomUUID());
        
        observation.withRelatedObservations(obsRefStatusChild, obsRefAudioChild, obsRefImageChild, 
                obsRefVideoChild, obsRefDetectionChild, obsRefOrientChild, 
                obsRefParent, obsRefPeer, refNotFoundWithDesc, refNotFoundNoDesc);
    }
    
    /**
     * Randomly generates an observation that is to be used as a related observation. Also, creates links 
     * back to the given observation.
     * 
     * @param sensorId
     *  the sensor id of the created observation
     * @param linkToObs
     *  the observation that the related observation is to be created for
     * @param linkToObsRelationship
     *  the observations relationship to the created related observation
     * @return
     *  the related observation for the given linkToObs 
     */
    private Observation createARelatedObservation(String sensorId, ObservationSubTypeEnum observationType,
            Observation linkToObs, RelationshipTypeEnum linkToObsRelationship) throws ValidationFailedException
    {
        final Observation observation = new Observation();
        observation.withSensorId(sensorId);
        
        ObservationRef relatedObsRef = new ObservationRef();
        relatedObsRef.setUuid(linkToObs.getUuid());
        relatedObsRef.setRelationship(new Relationship().withRelationshipType(linkToObsRelationship));
        observation.withRelatedObservations(relatedObsRef);
        
        if (observationType == null)
        {
            Coordinates assetLoc = new Coordinates()
                .withLatitude(new LatitudeWgsDegrees().withValue(50.0))
                .withLongitude(new LongitudeWgsDegrees().withValue(70.0));
            Coordinates pointLoc = new Coordinates().withLatitude(new LatitudeWgsDegrees().withValue(88.0))
                    .withLongitude(new LongitudeWgsDegrees().withValue(10.0));
            
            Orientation assetOrient = new Orientation().withHeading(
                    new HeadingDegrees().withPrecision(10.0).withStdev(1.0));
            Orientation pltOrient = new Orientation().withHeading(new HeadingDegrees()
                .withPrecision(2.0).withStdev(15.0));
            
            observation.withAssetLocation(assetLoc);
            observation.withAssetOrientation(assetOrient);
            
            observation.withPointingLocation(pointLoc);
            observation.withPlatformOrientation(pltOrient);
        }
        else
        {
            switch (observationType)
            {
                case DETECTION:
                    LatitudeWgsDegrees lat = new LatitudeWgsDegrees().withValue(50.0);
                    LongitudeWgsDegrees lng = new LongitudeWgsDegrees().withValue(70.0);
                    Coordinates coords = new Coordinates().withLatitude(lat).withLongitude(lng);
                    Detection detection = new Detection();
                    detection.setType(DetectionTypeEnum.TAMPER);
                    final HeadingDegrees heading = new HeadingDegrees().withValue(50);
                    final ElevationDegrees elevation = new ElevationDegrees().withValue(90);
                    detection.withTargetLocation(coords);
                    detection.withTargetOrientation(new Orientation(heading, elevation, null));
                    detection.withTrackHistories(new TrackElement(coords, new SpeedMetersPerSecond(205.5, null, null, 
                            null, null), new Orientation(heading, elevation, null), null, 2003L));
                    detection.setTargetFrequency(new FrequencyKhz().withValue(30));
                    detection.setTargetId("example-target-id");
                    
                    observation.withDetection(detection);
                    observation.withModalities(new SensingModality().withValue(SensingModalityEnum.ACOUSTIC));
                    break;
                case STATUS:
                    Status status = new Status().withBatteryVoltage(new VoltageVolts().withValue(3.4f))
                        .withSummaryStatus(new OperatingStatus().withSummary(SummaryStatusEnum.GOOD));
                    observation.setStatus(status);
                    break;
                case WEATHER:
                    Weather weather = new Weather().withRoadCondition(new RoadCondition()                    
                        .withType(new RoadConditionType(RoadConditionTypeEnum.BLACK_ICE, "Black Ice")))
                        .withTemperature(new TemperatureCelsius().withValue(19.0));
                    observation.setWeather(weather);
                    break;
                case AUDIO_METADATA:
                    DigitalMedia digitalAudio = new DigitalMedia().withEncoding("audio/mp3").withValue(new byte[100]);
                    AudioMetadata audio = new AudioMetadata().withStartTime(System.currentTimeMillis())
                        .withEndTime(System.currentTimeMillis() + 3000).withSampleRateKHz(52)
                        .withSampleOfInterest(new AudioSampleOfInterest()
                            .withStartTime(1L).withEndTime(2L))
                        .withRecorderType(new AudioRecorder().withValue(AudioRecorderEnum.MICROPHONE));
                    observation.withDigitalMedia(digitalAudio).withAudioMetadata(audio);
                    break;
                case IMAGE_METADATA:
                    DigitalMedia digitalImage = new DigitalMedia().withEncoding("image/jpeg").withValue(new byte[100]);
                    ImageMetadata image = new ImageMetadata().withImager(new Camera()
                        .withType(CameraTypeEnum.OTHER))
                            .withImageCaptureReason(new ImageCaptureReason().withValue(ImageCaptureReasonEnum.OTHER));
                    observation.withDigitalMedia(digitalImage).withImageMetadata(image);
                    break;
                case VIDEO_METADATA:
                    DigitalMedia digitalVideo = new DigitalMedia().withEncoding("video/wav").withValue(new byte[100]);
                    VideoMetadata videoData = new VideoMetadata().withRecorder(new Camera()
                        .withType(CameraTypeEnum.OTHER));
                    observation.withDigitalMedia(digitalVideo).withVideoMetadata(videoData);
                    break;
                default:
                    throw new IllegalArgumentException(String.format("No case exists for %s!"
                            + " Cannot create related observation.", observationType));
            }
        }
        
        m_Context.persistObservation(observation);
        
        return observation;
    }
    

    @Override
    public Status onPerformBit() throws AssetException
    {
        throw new AssetException("Function not supported.");
    }

    @Override
    public Response onExecuteCommand(Command command) throws CommandExecutionException, InterruptedException
    {
        throw new CommandExecutionException("Related observation asset does not support any commands");
    }

    @Override
    public Set<Extension<?>> getExtensions()
    {
        return null;
    }
    
    private void checkWakeLock()
    {
        if (m_WakeLock == null)
        {
            m_WakeLock = m_Context.createPowerManagerWakeLock(this.getClass().getSimpleName() + "WakeLock");
        }
    }
    
    private void tryActivateWakeLock()
    {
        if (m_WakeLock != null)
        {
            m_WakeLock.activate();
        }
    }
    
    private void tryDeactivateWakeLock()
    {
        if (m_WakeLock != null)
        {
            m_WakeLock.cancel();
        }
    }
}
