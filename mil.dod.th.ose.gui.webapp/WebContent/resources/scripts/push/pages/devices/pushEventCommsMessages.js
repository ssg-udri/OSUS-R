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
//    The purpose of this script is to allow for components on the comms page 
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
    
    //COMMS_LAYER_UPDATED Event
    if(topic == window.thTopic.commsLayerUpdated)
    {
         //see comms.xhtml rcUpdateComms is an update command on a primefaces 
         //remote command specified by a <p:remoteCommand > tag
         rcUpdateComms();
    }
    
    //COMMS_TYPES_UPDATED event
    if(topic == window.thTopic.commsTypesUpdated)
    {
        //see comms_add_layer_dialog.xhtml rcUpdateLayers is a remote update command
        //on a primefaces remote command specified by a <p:remoteCommand> tag
        rcUpdateLayers();
    }
    return;
}

// This function forces a push after a page has been completely loaded.  
// Needed as push updates may try to update page components before they are rendered,
// and therefore the updates will not be shown.  
$(document).ready(function(){
    rcUpdateComms(); 
});
