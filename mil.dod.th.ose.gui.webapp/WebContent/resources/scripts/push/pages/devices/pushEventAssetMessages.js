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
//    The purpose of this script is to allow for components on the assets page 
//    to be updated.
//==============================================================================

/*
 * GLOBALS
 * 
 * messageSocket - is located on the mainscreenTemplate and serves as the connection over 
 *               which messages are sent.
 *
 */

/**
 * Function to handle push events.
 * @param message
 *  a web gui message which will contain data and an identifier as 
 *  to the type of the data.
 */
function handleMessage(message)
{
     var topic = message.topic;
     
     if(topic == window.thTopic.assetAdded || topic == window.thTopic.assetUpdated)
     {
         rcUpdateAssetsSideList();
         rcUpdateAssetsList();
         rcUpdateAssetCommandControl();
     }
     else if (topic == window.thTopic.assetActivationStatusUpdated)
     {
         rcUpdateAssetsList();
     }
     else if(topic == window.thTopic.assetRemoved)
     {
         rcUpdateAssetsList();
         rcUpdateAssetsSideList();
         rcUpdateAssetCommandControl();
         rcUpdateObservations();
     }
     else if(topic == window.thTopic.assetLocationUpdated)
     {
         rcUpdateAssetsLocation();
     }
     else if(topic == window.thTopic.assetTypesUpdated)
     {
         rcUpdateAssetTypes();
     }
     else if(topic == window.thTopic.assetCommandSynced)
     {
         rcUpdateAssetCommandControl();
     }
     else if(topic == window.thTopic.obsStoreUpdated)
     {
         rcUpdateObservations();
     }
     else if(topic == window.thTopic.configModelUpdated || topic == window.thTopic.metaModelUpdated)
     {
         rcUpdateConfigDialog();
     }
     else if (topic == window.thTopic.assetGetCommandResponseReceived)
     {
         rcUpdateGetCommandWithResponse();
     }
}

//when the assets page is loaded update the asset lists
$(document).ready(function(){
    
    //see assets.xhtml rcUpdateAssetsList is an update command on a primefaces 
    //remote command specified by a <p:remoteCommand > tag
    rcUpdateAssetsList();
    
    //see assets.xhtml rcUpdateAssetsLocation is an update command on a primefaces 
    //remote command specified by a <p:remoteCommand > tag
    rcUpdateAssetsSideList();
});