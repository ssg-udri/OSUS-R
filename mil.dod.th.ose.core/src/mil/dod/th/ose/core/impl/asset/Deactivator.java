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

import mil.dod.th.core.log.Logging;

import org.osgi.service.log.LogService;


/**
 * Thread class to deactivate an Asset. Notify listeners of result.
 * 
 * @author mgleason
 */
public class Deactivator implements Runnable
{
    /** Asset to activate. */
    final private AssetInternal m_Asset;

    /** Listens to events from this class. */
    final private AssetActivationListener[] m_AssetActivationListeners;

    /**
     * Default constructor.
     * 
     * @param asset
     *            asset to deactivate
     * @param assetActivationListeners
     *            listeners for events from this class
     */
    public Deactivator(final AssetInternal asset, final AssetActivationListener[] assetActivationListeners)
    {
        m_Asset = asset;
        m_AssetActivationListeners = assetActivationListeners.clone();
    }

    /**
     * Try and deactivate the given asset. If unsuccessful set status back to
     * activated.
     */
    @Override
    public void run()
    {
        try
        {
            m_Asset.onDeactivate();
            for (AssetActivationListener listener : m_AssetActivationListeners)
            {
                listener.assetDeactivationComplete(m_Asset);
            }
        }
        catch (final Exception e)
        {
            Logging.log(LogService.LOG_WARNING, e, "Exception during onDeactivate %s", m_Asset.getName());
            for (AssetActivationListener listener : m_AssetActivationListeners)
            {
                listener.assetDeactivationFailed(m_Asset);
            }
        }
    }
}

