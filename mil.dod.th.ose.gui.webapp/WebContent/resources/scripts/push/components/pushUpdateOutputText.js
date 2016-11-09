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
//  This is the javascript function that is embedded in the pushUpdateOutputText
//  component. It allows for text contained in the component to be updated via
//  javascript.
//==============================================================================

/**
 * Callback script for pushUpdateOutputText components.
 * @param id
 *      the ID of the component to update
 * @param topic
 *      the event topic of the event which triggered this callback
 * @param prop
 *      the properties from the event
 * @param regProps
 *      additional properties used during the registration of the callback
 */
function outputTextCallback(id, topic, prop, regProps)
{
    var valueProp = regProps["valueProp"];
    
    $("[id='" + id + "']").text(prop[valueProp]);
}

/**
 * Callback script for the Active Controller observation count component.
 * @param id
 *      the ID of the component to update
 * @param topic
 *      the event topic of the event which triggered this callback
 * @param prop
 *      the properties from the event
 * @param regProps
 *      additional properties used during the registration of the callback
 */
function outputTextActiveControllerCallback(id, topic, prop, regProps)
{
    var valueProp = regProps["valueProp"];
    
    var idSplit = id.split("_");
    var activeControllerId = idSplit[1];
    
    if (prop["controller.id"] == activeControllerId)
    {
        $("[id='" + id + "']").text(prop[valueProp]);
    }
}