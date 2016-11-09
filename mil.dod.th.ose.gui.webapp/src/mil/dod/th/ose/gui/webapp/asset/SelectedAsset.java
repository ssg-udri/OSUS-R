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

/**
 * This interface assists in operations on a single asset, such as activating/deactivation, configuring, and requesting
 * observations.
 * @author callen
 *
 */
public interface SelectedAsset 
{
    /**
     * Set the currently selected asset model. This is used for storing the asset model to 
     * display in a dialog.
     * @param assetModel
     *      asset model currently selected
     */
    void setSelectedAssetForDialog(AssetModel assetModel);
    
    /**
     * Get the asset model that is currently selected to be shown in a dialog.
     * @return
     *      asset model currently selected
     */
    AssetModel getSelectedAssetForDialog();
    
    /**
     * Request to activate the selected asset. 
     * @param assetModel
     *     the asset model that represents the asset selected 
     */
    void requestActivation(AssetModel assetModel);
    
    /**
     * Request to deactivate the selected asset.
     * @param assetModel
     *     the asset model that represents the asset selected 
     */
    void requestDeactivation(AssetModel assetModel);
    
    /**
     * Request that the selected asset performs the Built-In-Test.
     * @param assetModel
     *     the asset model that represents the asset selected
     */
    void requestBIT(AssetModel assetModel);
    
    /**
     * Request that the selected asset captures data.
     * @param assetModel
     *     the asset model that represents the asset selected
     */
    void requestCaptureData(AssetModel assetModel);
    
    /**
     * Remove the selected asset from its home system.
     * @param assetModel
     *     the asset model that represents the asset selected
     */
    void requestRemoval(AssetModel assetModel);
    
    /**
     * Request updating of the asset's name.
     * @param assetModel
     *     the asset model to update the name of
     */
    void requestNameUpdate(AssetModel assetModel);
}
