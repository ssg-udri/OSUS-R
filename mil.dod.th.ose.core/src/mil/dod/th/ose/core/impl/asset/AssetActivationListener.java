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


package mil.dod.th.ose.core.impl.asset;


/**
 * Used to listen to the {@link Activator} and {@link Deactivator} classes. 
 */
public interface AssetActivationListener
{
    /**
     * Called when an asset has completed activation.
     * 
     * @param asset 
     *      asset involved in event
     */
    void assetActivationComplete(AssetInternal asset); 

    /**
     * Called when an asset failed to activate.
     * 
     * @param asset 
     *      asset involved in event
     */
    void assetActivationFailed(AssetInternal asset);

    /**
     * Called when an asset has completed deactivation.
     * 
     * @param asset 
     *      asset involved in event
     */
    void assetDeactivationComplete(AssetInternal asset);

    /**
     * Called when an asset failed to deactivate.
     * 
     * @param asset 
     *      asset involved in event
     */
    void assetDeactivationFailed(AssetInternal asset);
}
