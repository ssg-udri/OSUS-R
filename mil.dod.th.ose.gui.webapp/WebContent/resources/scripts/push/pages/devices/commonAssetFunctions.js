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
 * Filter for updating callbacks after removal of an asset.
 * 
 * @param props
 *      message properties to check
 * @param topicCallback
 *      topic callback to filter based on properties
 * @returns {Boolean}
 *      true if the topic callback should be removed, false otherwise
 */
function assetDescriptionCleanupFilter(props, topicCallback)
{
    var assetUuid = props[window.thEventProps.AssetUuid];
    
    if (topicCallback.uniqueId == assetUuid)
    {
        return true;
    }
    
    return false;
}

/**
 * Method that calls the save function for the primefaces inplace element with the specified widget variable name.
 * 
 * @param widgetVarName
 *      String that represents the widget variable name of the inplace widget to call the save function on.
 * @returns {Boolean}
 *      Always returns false as the form should not be submitted.
 */
function saveAssetName(widgetVarName)
{
    window[widgetVarName].save();
    return false;
}