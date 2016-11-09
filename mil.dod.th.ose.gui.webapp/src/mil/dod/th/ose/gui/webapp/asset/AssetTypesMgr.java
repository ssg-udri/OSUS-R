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
package mil.dod.th.ose.gui.webapp.asset;

import java.util.List;

/**
 * Manages information on the types of assets available on a controller.
 * @author callen
 *
 */
public interface AssetTypesMgr 
{
    /** 
     * Event topic prefix for all {@link AssetMgr} events. 
     */
    String TOPIC_PREFIX = "mil/dod/th/ose/gui/webapp/asset/AssetTypesMgr/";
    
    /** 
     * Topic used for when a new asset is added.
     */
    String TOPIC_ASSET_TYPES_UPDATED = TOPIC_PREFIX + "ASSET_TYPES_UPDATED";
    
    /**
     * Get asset factories for a controller.
     * @param controllerId
     *    the controller id to retrieve asset factories for
     * @return
     *    list of asset factory models that belong to the specified controller
     */
    List<AssetFactoryModel> getAssetFactoriesForControllerAsync(int controllerId);
    
    /**
     * Function to get an AssetFactoryModel for a controller with the specified
     * class name.
     * @param controllerId
     *  the id of the controller to search for the associated class name
     * @param className
     *  the fully qualified class name to search for.
     * @return
     *  AssetFactoryModel which corresponds to the controller id and class name. If
     *  no match is found null is returned.
     */
    AssetFactoryModel getAssetFactoryForClassAsync(int controllerId, String className);
}
