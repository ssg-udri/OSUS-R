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

/**
 * Filter for updating callbacks after a controller is removed.
 * 
 * @param props
 *      message properties to check
 * @param topicCallback
 *      topic callback to filter based on properties
 * @returns {Boolean}
 *      true if the topic callback should be removed, false otherwise
 */
function controllerRemovedFilter(props, topicCallback)
{
    var controllerId = props[window.thEventProps.ControllerId];
    
    if (topicCallback.uniqueId == controllerId)
    {
        return true;
    }
    
    return false;
}

/**
 * Filter for updating callbacks after active controller change. 
 * 
 * @param props
 *      message properties to check
 * @param topicCallback
 *      topic callback to filter based on properties
 * @returns {Boolean}
 *      true if the topic callback should be removed, false otherwise
 */
function activeControllerCleanupFilter(props, topicCallback)
{
    var controllerId = props[window.thEventProps.ControllerId];
    var newComponentId = "activeControllerObsCnt_" + controllerId; 
    
    if (topicCallback.uniqueId != controllerId && topicCallback.componentId != newComponentId)
    {
        return true;
    }
    
    return false;
}