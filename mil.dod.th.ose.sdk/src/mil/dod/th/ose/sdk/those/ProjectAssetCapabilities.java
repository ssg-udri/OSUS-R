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
package mil.dod.th.ose.sdk.those;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mil.dod.th.core.asset.capability.ActionParameters;
import mil.dod.th.core.asset.capability.AssetCapabilities;
import mil.dod.th.core.asset.capability.AudioCapabilities;
import mil.dod.th.core.asset.capability.CameraSettingsParameters;
import mil.dod.th.core.asset.capability.CaptureImageParameters;
import mil.dod.th.core.asset.capability.CommandCapabilities;
import mil.dod.th.core.asset.capability.CreateActionListParameters;
import mil.dod.th.core.asset.capability.DetectTargetParameters;
import mil.dod.th.core.asset.capability.DetectionCapabilities;
import mil.dod.th.core.asset.capability.DigitalMediaCapabilities;
import mil.dod.th.core.asset.capability.ImageCapabilities;
import mil.dod.th.core.asset.capability.Lift;
import mil.dod.th.core.asset.capability.ObservationCapabilities;
import mil.dod.th.core.asset.capability.PanTilt;
import mil.dod.th.core.asset.capability.ReservedFieldDefinition;
import mil.dod.th.core.asset.capability.StatusCapabilities;
import mil.dod.th.core.asset.capability.TuneChannelParameters;
import mil.dod.th.core.asset.capability.TuneSettingsParameters;
import mil.dod.th.core.asset.capability.VideoCapabilities;
import mil.dod.th.core.asset.capability.ZeroizeCapabilities;
import mil.dod.th.core.types.ActionEnum;
import mil.dod.th.core.types.ComponentType;
import mil.dod.th.core.types.ComponentTypeEnum;
import mil.dod.th.core.types.DigitalMedia;
import mil.dod.th.core.types.FrequencyKhz;
import mil.dod.th.core.types.Mode;
import mil.dod.th.core.types.ModeEnum;
import mil.dod.th.core.types.SensingModality;
import mil.dod.th.core.types.SensingModalityEnum;
import mil.dod.th.core.types.audio.AudioRecorder;
import mil.dod.th.core.types.audio.AudioRecorderEnum;
import mil.dod.th.core.types.command.CommandTypeEnum;
import mil.dod.th.core.types.detection.DetectionTypeEnum;
import mil.dod.th.core.types.detection.TargetClassificationType;
import mil.dod.th.core.types.detection.TargetClassificationTypeEnum;
import mil.dod.th.core.types.factory.SpatialTypesFactory;
import mil.dod.th.core.types.image.Camera;
import mil.dod.th.core.types.image.CameraTypeEnum;
import mil.dod.th.core.types.image.PictureTypeEnum;
import mil.dod.th.core.types.image.PixelResolution;
import mil.dod.th.core.types.spatial.AzimuthDegrees;
import mil.dod.th.core.types.spatial.BankDegrees;
import mil.dod.th.core.types.spatial.DistanceMeters;
import mil.dod.th.core.types.spatial.ElevationDegrees;
import mil.dod.th.core.types.spatial.OrientationOffset;
import mil.dod.th.ose.utils.xml.XmlUtils;

/**
 * Utility class to create a capabilities xml byte array.
 * 
 * @author Tonia
 */
public final class ProjectAssetCapabilities //NOCHECKSTYLE: class fan-out complexity; 
                                            //needs all classes to create proper capabilities file
{
    /**
     * Description string to use when the type is OTHER.
     */
    private static final String DEFAULT_DESCRIPTION = "some description";
    
    private static final String OTHER_DESCRIPTION = "Must set description if value is OTHER";

    /**
     * Variable to populate the empty description parameters.
     */
    private static final String DESC_NONE = "none";

    /**
     * Variable to populate the MIME image type encoding.
     */
    private static final String MIME_IMAGE = "image/jpeg";
    
    /**
     * Variable to populate the MIME video type encoding.
     */
    private static final String MIME_VIDEO = "video/h.264";
    
    /**
     * Variable to populate the MIME unknown type encoding.
     */
    private static final String MIME_UNKNOWN = "Unknown";

    /**
     * Variable to populate a required action parameter.
     */
    private static final String ACTION_PARAM_REQ = "altitude";

    /**
     * Variable to populate an ignored action parameter.
     */
    private static final String ACTION_PARAM_IGNORE = "timeoutMs";

    /**
     * Default private constructor to prevent instantiation.
     */
    private ProjectAssetCapabilities()
    {       
    }
    
    /**
     * Creates a capabilities object and converts that object to xml in the form of a byte array.
     * @return The byte array containing the capabilities xml.
     */
    public static byte[] getCapabilities()
    {
        final AssetCapabilities cap = makeCapabilities();
        return XmlUtils.toXML(cap, true);
    }
  
    /**
     * Creates an object that is populated with placeholder values for a Capabilities object.
     * @return Capabilities object
     */
    private static AssetCapabilities makeCapabilities()
    {
        final List<SensingModality> type = makeSensingModalityList();
        
        final DigitalMedia primaryImage = ProjectBaseCapabilities.makePrimaryImage();
        final List<DigitalMedia> secondaryImages = ProjectBaseCapabilities.makeSecondaryImages();
       
        final DistanceMeters minRange = makeDistance();
        final DistanceMeters maxRange = makeDistance();
        final DistanceMeters nominalRange = makeDistance();
        final Double minFov = 0.0;
        final Double maxFov = 0.0;
        final Double nominalFov = 0.0;
        final List<Camera> supportedCameras = makeAvailableCameras();
        final StatusCapabilities statusCapabilities = makeStatusCapabilities();
        final DetectionCapabilities detectionCapabilities = makeDetectionCapabilities();
        final DigitalMediaCapabilities digitalMediaCapabilities = makeDigitalMediaCapabilities();
        final AudioCapabilities audioCapabilities = makeAudioCapabilities();
        final ImageCapabilities imageCapabilities = makeImageCapabilities();
        final VideoCapabilities videoCapabilities = makeVideoCapabilities();
        final CommandCapabilities commandCapabilities = makeCommandCapabilities();        
        final String productName = ProjectBaseCapabilities.getProductName();
        final String description = ProjectBaseCapabilities.getDescription();
        final String manufacturer = ProjectBaseCapabilities.getManufacturer();
        final ObservationCapabilities obsCapabilities = makeObsCapabilities();
        final ZeroizeCapabilities zeroizeCapabilities = makeZeroizeCapabilities();
        return new AssetCapabilities(primaryImage, secondaryImages, productName, description, manufacturer, type, 
                minRange, maxRange, nominalRange, minFov, maxFov, nominalFov, supportedCameras, statusCapabilities, 
                detectionCapabilities, digitalMediaCapabilities, audioCapabilities, imageCapabilities, 
                videoCapabilities, commandCapabilities, obsCapabilities, zeroizeCapabilities);
    }

    /**
     * Creates an list of SensingModality objects that are populated with placeholder values.
     * @return List of SensingModality objects
     */
    private static List<SensingModality> makeSensingModalityList()
    {
        final List<SensingModality> type = new ArrayList<SensingModality>();
        for (SensingModalityEnum modalityType : SensingModalityEnum.values())
        {                    
            type.add(makeSensingModality(modalityType));
        }
        return type;
    }
    
    /**
     * Creates a SensingModality object.
     * @param modalityType used to create the SensingModality object
     * @return SensingModality object
     */
    private static SensingModality makeSensingModality(final SensingModalityEnum modalityType)
    {
        // PMD thinks this is an invalid null assignment, it got confused by the statement
        final String description = (modalityType == SensingModalityEnum.OTHER) ? DEFAULT_DESCRIPTION : null; // NOPMD
        return new SensingModality(modalityType, description);                      
    }
    
    /**
     * Creates an object that is populated with placeholder values for a DistanceMeters object.
     * @return DistanceMeters object
     */
    private static DistanceMeters makeDistance()
    {
        final double value = 0;
        return new DistanceMeters(value, null, null, null, null);
    }
    
    /**
     * Creates an object that is populated with placeholder values for a FrequencyKhz object.
     * @return FrequencyKhz object
     */
    private static FrequencyKhz makeFrequency()
    {
        final double value = 0;
        return new FrequencyKhz(value, null, null, null, null);
    }
    
    /**
     * Creates an object that is populated with placeholder values for a StatusCapabilities object.
     * @return StatusCapabilities object
     */
    private static StatusCapabilities makeStatusCapabilities()
    {
        final List<ComponentType> availableComponentStatuses = makeComponentList();
        
        final Boolean sensorRangeAvailable = false;
        final Boolean sensorFovAvailable = false;
        final Boolean batteryChargeLevelAvailable = false;
        final Boolean batteryVoltageAvailable = false;
        final Boolean assetOnTimeAvailable = false;
        final Boolean temperatureAvailable = false;
        final Boolean powerConsumptionAvailable = false;
        final Boolean analogAnalogVoltageAvailable = false;
        final Boolean analogDigitalVoltargeAvailable = false;
        final Boolean analogMagVoltageAvailable = false;
        final Boolean internalArchiveStatusAvailable = false;
        final Boolean algorithmStatusAvailable = false;
        
        return new StatusCapabilities(availableComponentStatuses, sensorRangeAvailable, sensorFovAvailable,
                batteryChargeLevelAvailable, batteryVoltageAvailable, assetOnTimeAvailable, temperatureAvailable,
                powerConsumptionAvailable, analogAnalogVoltageAvailable, analogDigitalVoltargeAvailable,
                analogMagVoltageAvailable, internalArchiveStatusAvailable, algorithmStatusAvailable);
    }
    
    /**
     * Creates an list of Component objects that are populated with placeholder values.
     * @return
     *      complete list of component using all types
     */
    private static List<ComponentType> makeComponentList()
    {
        final List<ComponentType> components = new ArrayList<>();
        for (ComponentTypeEnum componentType : ComponentTypeEnum.values())
        {                    
            components.add(makeComponent(componentType));
        }
        return components;
    }
    
    /**
     * Creates a ComponentType object.
     * @param componentType 
     *      type to include, insert description if OTHER
     * @return ComponentType object
     */
    private static ComponentType makeComponent(final ComponentTypeEnum componentType)
    {
        // PMD thinks this is an invalid null assignment, it got confused by the statement
        final String description = (componentType == ComponentTypeEnum.OTHER) ? DEFAULT_DESCRIPTION : null; // NOPMD
        return new ComponentType(componentType, description);                      
    }
    
    /**
     * Creates an object that is populated with placeholder values for a DetectionCapabilities object.
     * @return DetectionCapabilities object
     */
    private static DetectionCapabilities makeDetectionCapabilities()
    {
        final List<DetectionTypeEnum> typesAvailable = Arrays.asList(DetectionTypeEnum.values());
        final List<TargetClassificationType> classifications = new ArrayList<TargetClassificationType>();
        
        for (TargetClassificationTypeEnum target: TargetClassificationTypeEnum.values())
        {
            final String description = (target == TargetClassificationTypeEnum.OTHER) ? OTHER_DESCRIPTION : DESC_NONE;
            final TargetClassificationType targ = new TargetClassificationType(target, description);
            classifications.add(targ);
        }
        
        final boolean targetLocation = false;
        final boolean targetSpeed = false;
        final boolean targetRange = false;
        final boolean targetOrientation = false;
        final boolean targetLOB = false;
        final boolean targetFrequency = false;
        final boolean trackHistory = false;
        final boolean directionOfTravel = false;
        final boolean targetId = false;
        final boolean algorithmId = false;
        final boolean targetCount = false;
        final boolean detectionProbability = false;
        final boolean targetLobGlobal = false;
        final boolean targetLobLocal = false;
        final boolean detectionLength = false;
        final boolean targetAngularVelocity = false;
        final boolean targetRadialVelocityNormalized = false;
        final boolean acousticSignature = false;
        final boolean seismicSignature = false;
        final boolean magneticSignature = false;
        final boolean imagerSignature = false;
        return new DetectionCapabilities(typesAvailable, classifications, null, targetLocation, targetSpeed,
                targetRange, targetOrientation, targetLOB, targetFrequency, trackHistory, directionOfTravel, targetId,
                algorithmId, targetCount, detectionProbability, targetLobGlobal, targetLobLocal, detectionLength,
                targetAngularVelocity, targetRadialVelocityNormalized, acousticSignature, seismicSignature,
                magneticSignature, imagerSignature);
    }
    
    /**
     * Creates an object that is populated with placeholder values for a DigitalMediaCapabilities object.
     * @return DigitalMediaCapabilities object
     */
    private static DigitalMediaCapabilities makeDigitalMediaCapabilities()
    {
        final List<String> encodings = new ArrayList<String>();
        encodings.add(MIME_IMAGE);
        encodings.add(MIME_VIDEO);
        encodings.add(MIME_UNKNOWN);
        return new DigitalMediaCapabilities(encodings);
    }
    
    /**
     * Creates an object that is populated with placeholder values for a AudioCapabilities object.
     * @return AudioCapabilities object
     */
    private static AudioCapabilities makeAudioCapabilities()
    {
        final List<AudioRecorder> recorders = new ArrayList<AudioRecorder>();
        final String description = DESC_NONE;
        final Integer id = 0;
        
        for (AudioRecorderEnum audio : AudioRecorderEnum.values())
        {
            final AudioRecorder aud = new AudioRecorder(audio, description, id);
            recorders.add(aud);
        }
        
        final float num1 = (float)88.2;
        final float num2 = (float)96.0;
        final List<Float> sampleRatesKHz = new ArrayList<Float>();
        sampleRatesKHz.add(num1);
        sampleRatesKHz.add(num2);
        sampleRatesKHz.add((float)0.0);
        return new AudioCapabilities(recorders, sampleRatesKHz);
    }
    
    /**
     * Creates an object that is populated with placeholder values for an ImageCapabilities object.
     * @return ImageCapabilities object
     */
    private static ImageCapabilities makeImageCapabilities()
    {
        final PixelResolution minResolution = new PixelResolution();
        final PixelResolution maxResolution = new PixelResolution();
                
        final List<Integer> availableCameraIDs = Arrays.asList(0, 1, 2);
        
        final boolean colorAvailable = false;
        return new ImageCapabilities(minResolution, maxResolution, availableCameraIDs, colorAvailable);
    }
    
    /**
     * Creates an object that is populated with placeholder values for a VideoCapabilities object.
     * @return VideoCapabilities object
     */
    private static VideoCapabilities makeVideoCapabilities()
    {
        final PixelResolution minResolution = new PixelResolution();
        final PixelResolution maxResolution = new PixelResolution();
        
        final float num1 = (float)1000.0;
        final float num2 = (float)30.0;
        final List<Float> framesPerSecond = new ArrayList<Float>();
        framesPerSecond.add(num1);
        framesPerSecond.add(num2);
        framesPerSecond.add((float)0.0);
        
        final List<Integer> availableCameraIDs = Arrays.asList(0, 1, 2);
        
        final boolean colorAvailable = false;
        return new VideoCapabilities(minResolution, maxResolution, availableCameraIDs, colorAvailable, framesPerSecond);
    }
    
    /**
     * Creates an object that is populated with placeholder values for a CommandCapabilities object.
     * @return CommandCapabilities object
     */
    private static CommandCapabilities makeCommandCapabilities()
    {
        final List<CommandTypeEnum> supportedCommands = makeSupportedCommands();
        final List<Mode> supportedModes = makeSupportedModes();
        final PanTilt panTilt = makePanTilt();
        final Lift lift = makeLift();
        final CameraSettingsParameters cameraSettings = makeCameraSettings();
        final CaptureImageParameters captureImage = makeCaptureImage();        
        final DetectTargetParameters detectTarget = makeDetectTarget();
        final TuneSettingsParameters tuneSettings = makeTuneSettings();
        final CreateActionListParameters createActionList = makeCreateActionList();
        final boolean captureData = false;
        final boolean performBIT = false;
        final boolean captureDataBySensor = false;
        return new CommandCapabilities(supportedCommands, supportedModes, panTilt, cameraSettings, captureImage,
                detectTarget, tuneSettings, createActionList, lift, captureData, performBIT, captureDataBySensor);
    }

    /**
     * Creates an object that is populated with placeholder values for a SupportedCommands object.
     * @return SupportedCommands object
     */
    private static List<CommandTypeEnum> makeSupportedCommands()
    {
        final List<CommandTypeEnum> supportedCommands = new ArrayList<CommandTypeEnum>();
        
        for (CommandTypeEnum value : CommandTypeEnum.values())
        {
            supportedCommands.add(value);
        }

        return supportedCommands;
        
    }
    
    /**
     * Creates an object that is populated with placeholder values for a SupportedModes object.
     * @return SupportedModes object
     */
    private static List<Mode> makeSupportedModes()
    {
        final List<Mode> supportedModes = new ArrayList<Mode>();
        
        for (ModeEnum mode : ModeEnum.values())
        {
            final Mode newMode = new Mode(mode, "Description goes here.");
            supportedModes.add(newMode);
        }
        
        return supportedModes;
    }

    /**
     * Creates an object that is populated with placeholder values for a PanTilt object.
     * @return PanTilt object
     */
    private static PanTilt makePanTilt()
    {
        final OrientationOffset minOffset = SpatialTypesFactory.newOrientationOffset(-50.0, 0.0, 0.0);
        final OrientationOffset maxOffset = SpatialTypesFactory.newOrientationOffset(50.0, 0.0, 0.0);

        final AzimuthDegrees minAzimuth = minOffset.getAzimuth();
        final AzimuthDegrees maxAzimuth = maxOffset.getAzimuth();
        final ElevationDegrees minElevation = minOffset.getElevation();
        final ElevationDegrees maxElevation = maxOffset.getElevation();
        final BankDegrees minBank = minOffset.getBank();
        final BankDegrees maxBank = maxOffset.getBank();
        final boolean azimuthSupported = true;
        final boolean elevationSupported = false;
        final boolean bankSupported = false;

        return new PanTilt(minAzimuth, maxAzimuth, minElevation, maxElevation, minBank, maxBank, azimuthSupported,
                           elevationSupported, bankSupported);
    }

    /**
     * Creates an object that is populated with placeholder values for a Lift object.
     * @return Lift object
     */
    private static Lift makeLift()
    {
        final DistanceMeters minHeight = makeDistance();
        final DistanceMeters maxHeight = makeDistance();
        final boolean isHeightSupported = false;
        final boolean isHeightOffsetSupported = false;
        final boolean isUpSupported = false;
        final boolean isDownSupported = false;
        final boolean isPositionFeedbackSupported = false;
        final boolean isHighLimitFeedbackSupported = false;
        final boolean isLowLimitFeedbackSupported = false;

        return new Lift(minHeight, maxHeight, isHeightSupported, isHeightOffsetSupported, isUpSupported,
                isDownSupported, isPositionFeedbackSupported, isHighLimitFeedbackSupported,
                isLowLimitFeedbackSupported);
    }

    /**
     * Creates an object that is populated with placeholder values for a CameraSettingsParameters object.
     * @return CameraSettingsParameters object
     */
    private static CameraSettingsParameters makeCameraSettings()
    {
        final Float minFocus = (float)0.0;
        final Float maxFocus = (float)0.0;
        final Integer maxNumAutoFocusWindow = 0;
        final Integer maxAutoFocusWindowArea = 0;
        final Integer minExposureTimeInMs = 0;
        final Integer maxExposureTimeInMs = 0;
        final Integer minExposureIndex = 0;
        final Integer maxExposureIndex = 0;
        final Double minAperture = 0.0;
        final Double maxAperture = 0.0;
        final boolean autoFocusSupported = false;
        final boolean exposureModeSupported = false;
        final boolean whiteBalanceSupported = false;
        return new CameraSettingsParameters(minFocus, maxFocus, maxNumAutoFocusWindow,
                maxAutoFocusWindowArea, minExposureTimeInMs, maxExposureTimeInMs, minExposureIndex, maxExposureIndex,
                minAperture, maxAperture, autoFocusSupported, exposureModeSupported, whiteBalanceSupported);
    }
    
    /**
     * Creates an object that is populated with placeholder values for a CaptureImageParameters object.
     * @return CaptureImageParameters object
     */
    private static CaptureImageParameters makeCaptureImage()
    {
        final List<Integer> availableCameraIDs = Arrays.asList(0, 1);
        final List<PictureTypeEnum> supportedPictureTypeEnums = makeSupportedPictureTypeEnums();
        final Integer minimumImageCompressionRatio = 0;
        final Integer maximumImageCompressionRatio = 0;
        final Integer minBurstIntervalInMs = 0;
        final Integer maxBurstIntervalInMs = 0;
        final Integer maximumBurstNumber = 0;
        return new CaptureImageParameters(availableCameraIDs, supportedPictureTypeEnums, minimumImageCompressionRatio,
                maximumImageCompressionRatio, minBurstIntervalInMs, maxBurstIntervalInMs, maximumBurstNumber);
    }
    
    /**
     * Creates an object that is populated with placeholder values for a DetectTargetParameters object.
     * @return DetectTargetParameters object
     */
    private static DetectTargetParameters makeDetectTarget()
    {
        final List<Integer> availableCameraIDs = Arrays.asList(1, 2);
        final List<PictureTypeEnum> supportedPictureTypeEnums = makeSupportedPictureTypeEnums();
        final Integer minimumDetectionDuration = 0;
        final Integer maximumDetectionDuration = 0;
        final Integer minimumCaptureInterval = 0;
        final Integer maximumCaptureInterval = 0;
        final Integer minimumImageCompressionRatio = 0;
        final Integer maximumImageCompressionRatio = 0;
        final Integer minBurstIntervalInMs = 0;
        final Integer maxBurstIntervalInMs = 0;
        final Integer maximumBurstNumber = 0;
        return new DetectTargetParameters(availableCameraIDs, supportedPictureTypeEnums, minimumDetectionDuration,
                maximumDetectionDuration, minimumCaptureInterval, maximumCaptureInterval, minimumImageCompressionRatio,
                maximumImageCompressionRatio, minBurstIntervalInMs, maxBurstIntervalInMs, maximumBurstNumber);
    }
    
    /**
     * Creates an object this is populated with placeholder values for a TuneSettingsParameters object.
     * @return
     *      a placeholder object
     */
    private static TuneSettingsParameters makeTuneSettings()
    {
        final List<TuneChannelParameters> channelParams = new ArrayList<TuneChannelParameters>();
        channelParams.add(makeTuneChannelParameters(1));
        channelParams.add(makeTuneChannelParameters(2));
        return new TuneSettingsParameters(channelParams);
    }

    /**
     * Create placeholder set of values for tune channel parameters.
     * 
     * @param channel
     *      channel number to use for the parameters
     * @return
     *      placeholder object
     */
    private static TuneChannelParameters makeTuneChannelParameters(final int channel)
    {
        final FrequencyKhz minFreq = makeFrequency();
        final FrequencyKhz maxFreq = makeFrequency();
        final FrequencyKhz minBand = makeFrequency();
        final FrequencyKhz maxBand = makeFrequency();
        final List<Mode> procModes = new ArrayList<Mode>();
        
        for (ModeEnum mode : ModeEnum.values())
        {
            if (mode.equals(ModeEnum.OTHER))
            {
                procModes.add(new Mode(mode, "describe other mode"));
            }
            else
            {
                procModes.add(new Mode(mode, null));
            }
        }
        
        return new TuneChannelParameters(minFreq, maxFreq, minBand, maxBand, procModes, channel);
    }

    /**
     * Creates a list populated with placeholder values for an ActionTypes
     * object.
     * 
     * @return ActionTypes object
     */
    private static CreateActionListParameters makeCreateActionList()
    {
        final List<ActionParameters> supportedActions = new ArrayList<ActionParameters>();
        final List<String> ignoredParams = new ArrayList<String>();
        final List<String> requiredParams = new ArrayList<String>();

        ignoredParams.add(ACTION_PARAM_IGNORE);
        requiredParams.add(ACTION_PARAM_REQ);

        for (ActionEnum action : ActionEnum.values())
        {
            final ActionParameters param = new ActionParameters(ignoredParams, requiredParams, action);
            supportedActions.add(param);
        }

        return new CreateActionListParameters(supportedActions, ActionEnum.TAKEOFF, ActionEnum.LAND);
    }

    /**
     * Creates an list of Camera objects that are populated with placeholder values.
     * @return List of Camera objects
     */
    private static List<Camera> makeAvailableCameras()
    {
        
        final List<Camera> availableCameras = new ArrayList<Camera>();
        
        int i = 0;
        for (CameraTypeEnum cameraType : CameraTypeEnum.values())
        {
            if (cameraType == CameraTypeEnum.OTHER)
            {
                availableCameras.add(new Camera(i, "set description if other", cameraType));
            }
            else
            {
                availableCameras.add(new Camera(i, null, cameraType));
            }
            i++;
        }
        
        return availableCameras;
    }
    
    /**
     * Creates a list of PictureTypeEnum objects that are populated with placeholder values.
     * @return List of PictureTypeEnum Objects
     */
    private static List<PictureTypeEnum> makeSupportedPictureTypeEnums()
    {
        final List<PictureTypeEnum> areaOfInterestSupported = new ArrayList<PictureTypeEnum>();
        
        for (PictureTypeEnum type : PictureTypeEnum.values())
        {
            areaOfInterestSupported.add(type);
        }
        
        return areaOfInterestSupported;
    }

    /**
     * Creates an object that is populated with placeholder values for a ObservationCapabilities object.
     * 
     * @return ObservationCapabilities object
     */
    private static ObservationCapabilities makeObsCapabilities()
    {
        final List<ReservedFieldDefinition> reservedFields = new ArrayList<>();
        reservedFields.add(new ReservedFieldDefinition("key", "string"));
        final ObservationCapabilities obsCaps = new ObservationCapabilities(reservedFields, false, false);
        return obsCaps;
    }

    /**
     * Creates an object that is populated with placeholder values for a ZeroizeCapabilities object.
     * 
     * @return ZeroizeCapabilities object
     */
    private static ZeroizeCapabilities makeZeroizeCapabilities()
    {
        final ZeroizeCapabilities zeroizeCaps = new ZeroizeCapabilities(false, false, false, false);
        return zeroizeCaps;
    }
}
