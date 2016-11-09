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
//    The purpose of this script is to allow for components on the channels page 
//    to be updated.
//==============================================================================

/*
 * GLOBALS
 * 
 * messageSocket - is located on the mainscreenTemplate and serves as the connection over 
 *               which messages are sent.
 *                 
 * updateChannels() - a function defined by a <p:remoteCommand> on the channels.xhtml 
 *                    page which specifies that when this function is called to update
 *                    the channels table via an AJAX call.
 */

/**
 * Function to handle update of the channels page's channels table.
 * @param message
 *  a web gui message which will contain data and an identifier as 
 *  to the type of the data.
 */
function handleMessage(message)
{
    if(message != null)
    {
         var topic = message.topic;
         
         //CHANNEL_UPDATED/CHANNEL_REMOVED/CONTROLLER_UPDATED Events
         if(topic == window.thTopic.channelRemoved || topic == window.thTopic.channelUpdated || topic == window.thTopic.controllerUpdated)
         {
             //see channels.xhtml rcUpdateChannels is an update command on a primefaces 
             //remote command specified by a <p:remoteCommand > tag
             rcUpdateChannels();
         }
    }
    
    return;
}

