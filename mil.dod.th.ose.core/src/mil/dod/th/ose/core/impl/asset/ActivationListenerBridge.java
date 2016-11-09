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

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.log.LoggingService;

import org.osgi.service.log.LogService;

/**
 * Inner class for listening to activator and deactivator thread events and responding accordingly for the 
 * directory service.
 * 
 * @author dhumeniuk
 *
 */
final class ActivationListenerBridge implements AssetActivationListener
{
    /**
     * Logging service reference.
     */
    private final LoggingService m_Logging;

    /**
     * Constructor that requires the logging service that should be used.
     * @param logging
     *  logging service to use
     */
    ActivationListenerBridge(final LoggingService logging)
    {
        m_Logging = logging;
    }

    @Override
    public void assetActivationComplete(final AssetInternal asset)
    {
        asset.setActiveStatus(Asset.AssetActiveStatus.ACTIVATED);
        
        asset.postEvent(Asset.TOPIC_ACTIVATION_COMPLETE, null);
        
        m_Logging.log(LogService.LOG_INFO, "Activated Asset %s.", asset.getName());
    }

    @Override
    public void assetActivationFailed(final AssetInternal asset)
    {
        asset.setActiveStatus(Asset.AssetActiveStatus.DEACTIVATED);
        
        asset.postEvent(Asset.TOPIC_ACTIVATION_FAILED, null);
        
        m_Logging.log(LogService.LOG_WARNING, "Could not Activate Asset %s.", asset.getName());
    }

    @Override
    public void assetDeactivationComplete(final AssetInternal asset)
    {
        asset.setActiveStatus(Asset.AssetActiveStatus.DEACTIVATED);
        
        asset.postEvent(Asset.TOPIC_DEACTIVATION_COMPLETE, null);
        
        m_Logging.log(LogService.LOG_INFO, "Deactivated Asset %s.", asset.getName());
    }

    @Override
    public void assetDeactivationFailed(final AssetInternal asset)
    {
        asset.setActiveStatus(Asset.AssetActiveStatus.ACTIVATED);
        
        asset.postEvent(Asset.TOPIC_DEACTIVATION_FAILED, null);
        
        m_Logging.log(LogService.LOG_WARNING, "Could not deactivate asset %s.", asset.getName());
    }
}