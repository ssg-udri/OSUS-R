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
//    Common variables to be available/used by scripts which implement push 
//    capabilities.
//==============================================================================

/*
 * GLOBALS
 *  All variables in this file are considered to be global variables because they
 *  are being assigned to the window object for the page.
 */
//thTopic prototype, all topics related to THOSE should be properties of this.
window.thTopic || (window.thTopic = {});

window.thTopic.globalPrefixLocal = "mil/dod/th/ose/gui/webapp/";

//BUNDLE_INFO_RECEIVED
window.thTopic.bundleInfoReceived = window.thTopic.globalPrefixLocal
        + "advanced/BundleMgr/BUNDLE_INFO_RECEIVED";

//BUNDLE_INFO_REMOVED
window.thTopic.bundleInfoRemoved = window.thTopic.globalPrefixLocal
        + "advanced/BundleMgr/BUNDLE_INFO_REMOVED";

//BUNDLE_STATUS_UPDATED
window.thTopic.bundleStatusUpdated = window.thTopic.globalPrefixLocal
        + "advanced/BundleMgr/BUNDLE_STATUS_UPDATED";

//CONFIG_DISPLAY_MODELS_UPDATED (Posted by ViewScoped bean in charge of Sys. Config page. Event only posted
//if on the Sys. Config page).
window.thTopic.configDisplayModelsUpdated = window.thTopic.globalPrefixLocal
        + "advanced/configuration/ControllerConfigurationMgr/CONFIG_DISPLAY_MODELS_UPDATED";

//CONFIG_MODEL_UPDATED
window.thTopic.configModelUpdated = window.thTopic.globalPrefixLocal
        + "advanced/configuration/SystemConfigurationMgr/CONFIG_MODEL_UPDATED";

//METATYPE_MODEL_UPDATED
window.thTopic.metaModelUpdated = window.thTopic.globalPrefixLocal
        + "advanced/configuration/SystemMetaTypeMgr/METATYPE_MODEL_UPDATED";

//ASSET_COMMAND_SYNCED
window.thTopic.assetCommandSynced = window.thTopic.globalPrefixLocal
        + "asset/AssetCommandMgr/COMMAND_SYNCED";

//ASSET_GET_COMMAND_RESPONSE_RECEIVED
window.thTopic.assetGetCommandResponseReceived = window.thTopic.globalPrefixLocal
        + "asset/AssetGetCommandMgr/GET_RESPONSE_RECEIVED";

//ASSET_ADDED
window.thTopic.assetAdded = window.thTopic.globalPrefixLocal + "asset/AssetMgr/ASSET_ADDED";

//ASSET_LOCATION_UPDATED
window.thTopic.assetLocationUpdated = window.thTopic.globalPrefixLocal
        + "asset/AssetMgr/ASSET_LOCATION_UPDATED";

//ASSET_OBSERVATION_UPDATED
window.thTopic.assetObservationUpdated = window.thTopic.globalPrefixLocal
        + "asset/AssetMgr/ASSET_OBSERVATION_UPDATED";

//ASSET_REMOVED
window.thTopic.assetRemoved = window.thTopic.globalPrefixLocal + "asset/AssetMgr/ASSET_REMOVED";

//ASSET_STATUS_UPDATED
window.thTopic.assetStatusUpdated = window.thTopic.globalPrefixLocal
        + "asset/AssetMgr/ASSET_STATUS_UPDATED";

//ASSET_ACTIVATION_STATUS_UPDATED
window.thTopic.assetActivationStatusUpdated = window.thTopic.globalPrefixLocal
        + "asset/AssetMgr/ASSET_ACTIVATION_STATUS_UPDATED";

//ASSET_UPDATED
window.thTopic.assetUpdated = window.thTopic.globalPrefixLocal + "asset/AssetMgr/ASSET_UPDATED";

//ASSET_TYPES_UPDATED
window.thTopic.assetTypesUpdated = window.thTopic.globalPrefixLocal
        + "asset/AssetTypesMgr/ASSET_TYPES_UPDATED";

//CHANNEL_REMOVED
window.thTopic.channelRemoved = window.thTopic.globalPrefixLocal
        + "channels/ChannelConstants/CHANNEL_REMOVED";

//CHANNEL_UPDATED
window.thTopic.channelUpdated = window.thTopic.globalPrefixLocal
        + "channels/ChannelConstants/CHANNEL_UPDATED";

//COMMS_LAYER_UPDATED
window.thTopic.commsLayerUpdated = window.thTopic.globalPrefixLocal
        + "comms/CommsMgr/COMMS_LAYER_UPDATED";

//COMMS_TYPES_UPDATED
window.thTopic.commsTypesUpdated = window.thTopic.globalPrefixLocal
        + "comms/CommsLayerTypesMgr/COMMS_TYPES_UPDATED";

//ACTIVE_CONTROLLER_CHANGED
window.thTopic.activeControllerChanged = window.thTopic.globalPrefixLocal
        + "controller/ActiveController/ACTIVE_CONTROLLER_CHANGED";

//CONTROLLER_ADDED
window.thTopic.controllerAdded = window.thTopic.globalPrefixLocal
        + "controller/ControllerConstants/CONTROLLER_ADDED";

//CONTROLLER_REMOVED
window.thTopic.controllerRemoved = window.thTopic.globalPrefixLocal
        + "controller/ControllerConstants/CONTROLLER_REMOVED";

//CONTROLLER_UPDATED
window.thTopic.controllerUpdated = window.thTopic.globalPrefixLocal
        + "controller/ControllerConstants/CONTROLLER_UPDATED";

//OBSERVATION_COUNT_UPDATED
window.thTopic.observationCountUpdated = window.thTopic.globalPrefixLocal
        + "controller/ObservationCount/COUNT_UPDATED";

//MISSIONS_UPDATED
window.thTopic.missionUpdated = window.thTopic.globalPrefixLocal
        + "mp/CurrentMissionsConstants/MISSION_UPDATED";

//OBS_STORE_UPDATED
window.thTopic.obsStoreUpdated = window.thTopic.globalPrefixLocal
        + "observation/ObservationMgr/OBS_STORE_UPDATED";