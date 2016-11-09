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

import java.util.concurrent.Semaphore;


/**
 * This listener is used for detecting if all assets deactivating have completed or not. Does this by implementing 
 * {@link AssetActivationListener}.
 * 
 * @author nmarcucci
 *
 */
public class OnAssetDeactivateListener implements AssetActivationListener
{
    /**
     * The count of the number of assets that have failed to deactivate. 
     */
    private int m_FailedCount; 
    
    /**
     * Semaphore is released each time an asset completes deactivation (success or failure).
     */
    private final Semaphore m_Semaphore;

    /**
     * Default constructor.
     * 
     * @param semaphore
     *      semaphore that will be released for each assets that completes deactivation  
     */
    public OnAssetDeactivateListener(final Semaphore semaphore)
    {
        m_Semaphore = semaphore;
    }

    @Override
    public void assetActivationComplete(final AssetInternal asset)
    {
        // Not Needed
    }

    @Override
    public void assetActivationFailed(final AssetInternal asset)
    {
        // Not Needed
    }

    @Override
    public void assetDeactivationComplete(final AssetInternal asset)
    {
        m_Semaphore.release();
    }

    @Override
    public void assetDeactivationFailed(final AssetInternal asset)
    {
        m_FailedCount++;

        m_Semaphore.release();
    }
    
    /**
     * Indicates if there was any failures or not.  If no failures, then all expected assets have deactivated. 
     * 
     * @return
     *  True if successful.
     */
    public boolean wasSuccessful()
    {
        return m_FailedCount == 0;
    }
    
}
