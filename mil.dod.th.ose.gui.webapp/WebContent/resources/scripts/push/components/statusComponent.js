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
//  This is the javascript function that is embedded in the status component that allows 
// for push updates to only update the status and not an entire component.
//==============================================================================
/**
 * Callback script for status components.
 * @param id
 *      the ID of the component to update
 * @param topic
 *      the event topic of the event which triggered this callback
 * @param prop
 *      the properties from the event
 * @param regProps
 *      additional properties used during the registration of the callback
 */
function statusCallback(id, topic, prop, regProps)
{
    var statusProp = regProps["statusProp"];
    var finalClass = "led-" + prop[statusProp];
    //append the styling needed for the unknown status
    if (prop[statusProp]=='UNKNOWN')
    {
        finalClass = finalClass + " ui-icon-help ui-icon";
    }
    $("div[id='" + id + "']").attr("class", finalClass);
}