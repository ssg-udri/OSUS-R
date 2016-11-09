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
//    The purpose of this script is to allow for components on the sys config page 
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
    if(message != null)
    {
         var topic = message.topic;
         
         if (topic == window.thTopic.bundleInfoReceived 
                 || topic == window.thTopic.bundleInfoRemoved
                 || topic == window.thTopic.bundleStatusUpdated)
         {
             //see systemconfig_bundles_tab.xhtml this call will activate a p:remoteCommand
             rcUpdateBundleTable(); 
         }
         else if (topic == window.thTopic.configDisplayModelsUpdated)
         {
             //see systemconfig_configuration_tab.xhtml these calls will activate a p:remoteCommand
             rcUpdateConfigTable();
             rcUpdateFactoryConfigTable();
         }
    }
}

//when the system configuration page is loaded update the both tabs
$(document).ready(function(){
    
    //see systemconfig_bundles_tab.xhtml this call will activate a p:remoteCommand
    rcUpdateBundleTable();
    //see systemconfig_configuration_tab.xhtml these calls will activate a p:remoteCommand
    rcUpdateConfigTable();
    rcUpdateFactoryConfigTable();
});