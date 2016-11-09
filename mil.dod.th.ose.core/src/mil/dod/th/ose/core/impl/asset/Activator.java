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
 * Thread class to activate an Asset.  Notify listener of result.
 * 
 * @author mgleason
 */
public class Activator implements Runnable
{
    /** Asset to activate. */
    final private AssetInternal m_Asset;

    /** Listens to events from this class. */
    final private AssetActivationListener m_AssetActivationListener;

    /**
     * Default constructor.
     * 
     * @param asset
     *            asset to activate
     * @param assetActivationListener
     *            listener for events from this class
     */
    public Activator(final AssetInternal asset, final AssetActivationListener assetActivationListener)
    {
        m_Asset = asset;
        m_AssetActivationListener = assetActivationListener;
    }

    /**
     * Try and activate the given asset.
     */
    @Override
    public void run()
    {
        try
        {
            m_Asset.onActivate();
            m_AssetActivationListener.assetActivationComplete(m_Asset);
        }
        catch (final Exception e)
        {
            m_AssetActivationListener.assetActivationFailed(m_Asset);
            Logging.log(LogService.LOG_WARNING, e, "Exception during onActivate %s", m_Asset.getName());
        }
    }
}

