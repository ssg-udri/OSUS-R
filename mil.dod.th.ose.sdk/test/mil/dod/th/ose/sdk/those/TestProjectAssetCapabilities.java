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

import org.junit.Test;

/**
 * Test class to test the ProjectAssetCapabilities class.
 * 
 * @author Tonia
 * 
 */
public class TestProjectAssetCapabilities
{
    /**
     * Tests that the {@link ProjectAssetCapabilities#getCapabilities()} returns an object containing the correct XML
     * tags.
     */
    @Test
    public void testGetCapabilities()
    {
        // get the byte array of the capabilities xml
        final byte[] capByte = ProjectAssetCapabilities.getCapabilities();

        // read the byte array to a string object, used as the string to test against
        String lineAccum = new String(capByte);

        // assert that each main tag contains the correct sub-tags and/or fields
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>",
                "<primaryImage", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>",
                "<secondaryImages", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>", "productName",
                lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>", "description",
                lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>",
                "manufacturer", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>", "<ns2:type",
                lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>",
                "<ns2:minRange", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>",
                "<ns2:maxRange", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>",
                "<ns2:nominalRange", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>", "minFov",
                lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>", "maxFov",
                lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>", "nominalFov",
                lineAccum);
        
        // verify camera id present
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>", 
                "<ns2:supportedCameras id=\"\\d+\"", lineAccum);
        // verify each camera type is displayed
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>",
                "<ns2:supportedCameras .* type=\"Unknown\"/>", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>", 
                "<ns2:supportedCameras .* type=\"Other\"/>", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>", 
                "<ns2:supportedCameras .* type=\"IR\"/>", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>", 
                "<ns2:supportedCameras .* type=\"Visible\"/>", lineAccum);
        
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>",
                "<ns2:statusCapabilities", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>",
                "<ns2:detectionCapabilities", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>",
                "<ns2:digitalMediaCapabilities", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>",
                "<ns2:audioCapabilities", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>",
                "<ns2:imageCapabilities", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>",
                "<ns2:videoCapabilities", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:AssetCapabilities", "</ns2:AssetCapabilities>",
                "<ns2:commandCapabilities", lineAccum);

        ProjectCapabilitiesUtils.assertTestPattern("<ns2:modalities", "/>", "value", lineAccum);
        
        ProjectCapabilitiesUtils.assertTestPattern("<primaryImage", "/primaryImage>", "encoding", lineAccum);

        ProjectCapabilitiesUtils.assertTestPattern("<secondaryImages", "/secondaryImages>", "encoding", lineAccum);
       
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:statusCapabilities", "</ns2:statusCapabilities>",
                "availableComponentStatuses", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:statusCapabilities", "</ns2:statusCapabilities>",
                "sensorRangeAvailable", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:statusCapabilities", "</ns2:statusCapabilities>",
                "sensorFovAvailable", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:statusCapabilities", "</ns2:statusCapabilities>",
                "batteryChargeLevelAvailable", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:statusCapabilities", "</ns2:statusCapabilities>",
                "batteryVoltageAvailable", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:statusCapabilities", "</ns2:statusCapabilities>",
                "assetOnTimeAvailable", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:statusCapabilities", "</ns2:statusCapabilities>",
                "temperatureAvailable", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:statusCapabilities", "</ns2:statusCapabilities>",
                "powerConsumptionAvailable", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:statusCapabilities", "</ns2:statusCapabilities>",
                "analogAnalogVoltageAvailable", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:statusCapabilities", "</ns2:statusCapabilities>",
                "analogDigitalVoltargeAvailable", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:statusCapabilities", "</ns2:statusCapabilities>",
                "analogMagVoltageAvailable", lineAccum);
        
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectionCapabilities", "</ns2:detectionCapabilities>",
                "targetId", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectionCapabilities", "</ns2:detectionCapabilities>",
                "directionOfTravel", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectionCapabilities", "</ns2:detectionCapabilities>",
                "trackHistory", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectionCapabilities", "</ns2:detectionCapabilities>",
                "targetFrequency", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectionCapabilities", "</ns2:detectionCapabilities>",
                "targetLOB", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectionCapabilities", "</ns2:detectionCapabilities>",
                "targetOrientation", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectionCapabilities", "</ns2:detectionCapabilities>",
                "targetRange", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectionCapabilities", "</ns2:detectionCapabilities>",
                "targetSpeed", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectionCapabilities", "</ns2:detectionCapabilities>",
                "targetLocation", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectionCapabilities", "</ns2:detectionCapabilities>",
                "<ns2:typesAvailable>", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectionCapabilities", "</ns2:detectionCapabilities>",
                "</ns2:typesAvailable>", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectionCapabilities", "</ns2:detectionCapabilities>",
                "<ns2:classifications", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectionCapabilities", "</ns2:detectionCapabilities>",
                "value", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectionCapabilities", "</ns2:detectionCapabilities>",
                "description", lineAccum);

        ProjectCapabilitiesUtils.assertTestPattern("<ns2:digitalMediaCapabilities>", "</ns2:digitalMediaCapabilities>",
                "<ns2:encodings>", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:digitalMediaCapabilities>", "</ns2:digitalMediaCapabilities>",
                "</ns2:encodings>", lineAccum);

        ProjectCapabilitiesUtils.assertTestPattern("<ns2:audioCapabilities>", "</ns2:audioCapabilities>",
                "<ns2:recorders", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:audioCapabilities>", "</ns2:audioCapabilities>",
                "description", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:audioCapabilities>", "</ns2:audioCapabilities>",
                "value", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:audioCapabilities>", "</ns2:audioCapabilities>",
                "<ns2:sampleRatesKHz>", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:audioCapabilities>", "</ns2:audioCapabilities>",
                "</ns2:sampleRatesKHz>", lineAccum);

        ProjectCapabilitiesUtils.assertTestPattern("<ns2:imageCapabilities", "</ns2:imageCapabilities>",
                "colorAvailable", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:imageCapabilities", "</ns2:imageCapabilities>",
                "<ns2:minResolution", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:imageCapabilities", "</ns2:imageCapabilities>", "height",
                lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:imageCapabilities", "</ns2:imageCapabilities>", "width",
                lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:imageCapabilities", "</ns2:imageCapabilities>",
                "<ns2:maxResolution", lineAccum);
        
        // verify available camera IDs
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:imageCapabilities", "</ns2:imageCapabilities>",
                "<ns2:availableCameraIDs>0</ns2:availableCameraIDs>", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:imageCapabilities", "</ns2:imageCapabilities>",
                "<ns2:availableCameraIDs>1</ns2:availableCameraIDs>", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:imageCapabilities", "</ns2:imageCapabilities>",
                "<ns2:availableCameraIDs>2</ns2:availableCameraIDs>", lineAccum);

        ProjectCapabilitiesUtils.assertTestPattern("<ns2:videoCapabilities", "</ns2:videoCapabilities>",
                "colorAvailable", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:videoCapabilities", "</ns2:videoCapabilities>",
                "<ns2:minResolution", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:videoCapabilities", "</ns2:videoCapabilities>", "height",
                lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:videoCapabilities", "</ns2:videoCapabilities>", "width",
                lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:videoCapabilities", "</ns2:videoCapabilities>",
                "<ns2:maxResolution", lineAccum);
        
        // verify available camera IDs
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:videoCapabilities", "</ns2:videoCapabilities>",
                "<ns2:availableCameraIDs>0</ns2:availableCameraIDs>", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:videoCapabilities", "</ns2:videoCapabilities>",
                "<ns2:availableCameraIDs>2</ns2:availableCameraIDs>", lineAccum);
        
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:videoCapabilities", "</ns2:videoCapabilities>",
                "<ns2:framesPerSecond>", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:videoCapabilities", "</ns2:videoCapabilities>",
                "</ns2:framesPerSecond>", lineAccum);

        ProjectCapabilitiesUtils.assertTestPattern("<ns2:commandCapabilities", "</ns2:commandCapabilities>",
                "performBIT", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:commandCapabilities", "</ns2:commandCapabilities>",
                "captureData", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:commandCapabilities", "</ns2:commandCapabilities>",
                "<ns2:supportedCommands", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:commandCapabilities", "</ns2:commandCapabilities>",
                "<ns2:panTilt", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:commandCapabilities", "</ns2:commandCapabilities>",
                "<ns2:cameraSettings", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:commandCapabilities", "</ns2:commandCapabilities>",
                "<ns2:captureImage", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:commandCapabilities", "</ns2:commandCapabilities>",
                "<ns2:detectTarget", lineAccum);
        
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:commandCapabilities", "</ns2:commandCapabilities>",
                "<ns2:supportedModes", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:commandCapabilities", "</ns2:commandCapabilities>", "mode",
                lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:commandCapabilities", "</ns2:commandCapabilities>", 
                "description", lineAccum);
        
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:supportedCommands", "</ns2:supportedCommands>",
                "CaptureImageCommand", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:supportedCommands", "</ns2:supportedCommands>",
                "DetectTargetCommand", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:supportedCommands", "</ns2:supportedCommands>",
                "GetCameraSettingsCommand", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:supportedCommands", "</ns2:supportedCommands>",
                "GetPanTiltCommand", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:supportedCommands", "</ns2:supportedCommands>",
                "GetPositionCommand", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:supportedCommands", "</ns2:supportedCommands>",
                "GetVersionCommand", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:supportedCommands", "</ns2:supportedCommands>",
                "SetCameraSettingsCommand", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:supportedCommands", "</ns2:supportedCommands>",
                "SetPanTiltCommand", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:supportedCommands", "</ns2:supportedCommands>",
                "SetPositionCommand", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:supportedCommands", "</ns2:supportedCommands>",
                "SetPointingLocationCommand", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:supportedCommands", "</ns2:supportedCommands>",
                "GetPointingLocationCommand", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:supportedCommands", "</ns2:supportedCommands>",
                "SetTuneSettingsCommand", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:supportedCommands", "</ns2:supportedCommands>",
                "GetTuneSettingsCommand", lineAccum);
        
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:panTilt", "/>", "minAzimuth", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:panTilt", "/>", "maxAzimuth", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:panTilt", "/>", "minElevation", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:panTilt", "/>", "maxElevation", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:panTilt", "/>", "minBank", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:panTilt", "/>", "maxBank", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:panTilt", "/>", "azimuthSupported", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:panTilt", "/>", "elevationSupported", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:panTilt", "/>", "bankSupported", lineAccum);

        ProjectCapabilitiesUtils.assertTestPattern("<ns2:cameraSettings", "/>", "whiteBalanceSupported", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:cameraSettings", "/>", "exposureModeSupported", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:cameraSettings", "/>", "autoFocusSupported", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:cameraSettings", "/>", "maxAperture", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:cameraSettings", "/>", "minAperture", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:cameraSettings", "/>", "maxExposureIndex", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:cameraSettings", "/>", "minExposureIndex", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:cameraSettings", "/>", "maxExposureTimeInMs", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:cameraSettings", "/>", "minExposureTimeInMs", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:cameraSettings", "/>", "maxAutoFocusWindowArea", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:cameraSettings", "/>", "maxNumAutoFocusWindow", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:cameraSettings", "/>", "maxFocus", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:cameraSettings", "/>", "minFocus", lineAccum);

        ProjectCapabilitiesUtils.assertTestPattern("<ns2:captureImage", "</ns2:captureImage>", "maximumBurstNumber",
                lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:captureImage", "</ns2:captureImage>", "maxBurstIntervalInMs",
                lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:captureImage", "</ns2:captureImage>", "minBurstIntervalInMs",
                lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:captureImage", "</ns2:captureImage>",
                "maximumImageCompressionRatio", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:captureImage", "</ns2:captureImage>",
                "minimumImageCompressionRatio", lineAccum);

        // verify available camera IDs
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:captureImage", "</ns2:captureImage>",
                "<ns2:availableCameraIDs>1</ns2:availableCameraIDs>", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:captureImage", "</ns2:captureImage>",
                "<ns2:availableCameraIDs>0</ns2:availableCameraIDs>", lineAccum);
        
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:captureImage", "</ns2:captureImage>", 
                "supportedPictureTypes", lineAccum);

        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectTarget", "</ns2:detectTarget>", "maximumBurstNumber",
                lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectTarget", "</ns2:detectTarget>", "maxBurstIntervalInMs",
                lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectTarget", "</ns2:detectTarget>", "minBurstIntervalInMs",
                lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectTarget", "</ns2:detectTarget>",
                "maximumImageCompressionRatio", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectTarget", "</ns2:detectTarget>",
                "minimumImageCompressionRatio", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectTarget", "</ns2:detectTarget>",
                "maximumImageCaptureInterval", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectTarget", "</ns2:detectTarget>",
                "minimumImageCaptureInterval", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectTarget", "</ns2:detectTarget>",
                "maximumDetectionDuration", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectTarget", "</ns2:detectTarget>",
                "minimumDetectionDuration", lineAccum);
        
        // verify available camera IDs
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectTarget", "</ns2:detectTarget>",
                "<ns2:availableCameraIDs>1</ns2:availableCameraIDs>", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:detectTarget", "</ns2:detectTarget>",
                "<ns2:availableCameraIDs>2</ns2:availableCameraIDs>", lineAccum);
        
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:tuneSettings", "</ns2:tuneSettings>", "availableChannels",
                lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:availableChannels", "</ns2:availableChannels>", "channel",
                lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:availableChannels", "</ns2:availableChannels>", 
                "minimumFrequency", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:availableChannels", "</ns2:availableChannels>", 
                "maximumFrequency", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:availableChannels", "</ns2:availableChannels>", 
                "minimumBandwidth", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:availableChannels", "</ns2:availableChannels>", 
                "maximumBandwidth", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:availableChannels", "</ns2:availableChannels>", 
                "processingModesSupported", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:processingModesSupported", "/>", "value", lineAccum);
        // if mode is other, make sure description is filled out
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:processingModesSupported", "/>",
                "description.*value=\"Other\"", lineAccum);

        ProjectCapabilitiesUtils.assertTestPattern("<ns2:createActionList", "</ns2:createActionList>", 
                "supportedActionTypes", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:createActionList", "</ns2:createActionList>", "firstAction",
                lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:createActionList", "</ns2:createActionList>", "lastAction",
                lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:supportedActionTypes", "</ns2:supportedActionTypes>", 
                "Takeoff", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:supportedActionTypes", "</ns2:supportedActionTypes>", 
                "GoToWaypoint", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:supportedActionTypes", "</ns2:supportedActionTypes>", 
                "ChangeOrientation", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:supportedActionTypes", "</ns2:supportedActionTypes>", 
                "CollectData", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:supportedActionTypes", "</ns2:supportedActionTypes>",
                "ExfilData", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:supportedActionTypes", "</ns2:supportedActionTypes>", 
                "Wait", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:supportedActionTypes", "</ns2:supportedActionTypes>", 
                "Land", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:ignoredParameters", "</ns2:ignoredParameters>", 
                "timeoutMs", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:requiredParameters", "</ns2:requiredParameters>", 
                "altitude", lineAccum);
    }
}
