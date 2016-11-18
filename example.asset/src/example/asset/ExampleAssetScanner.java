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
package example.asset;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService.ScanResults;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetScanner;
import mil.dod.th.core.factory.ProductType;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.core.pm.WakeLock;

import org.osgi.service.log.LogService;

/**
 * @author dlandoll
 */
@Component
@ProductType(ExampleAsset.class)
public class ExampleAssetScanner implements AssetScanner
{
    /**
     * Constant that represents the name of the asset found while scanning.
     */
    public static final String ASSET_NAME = "FoundExAsset";
    
    /**
     * If an asset has been created that contains this String in its name, then do not remove it.
     */
    public static final String DO_NOT_REMOVE = "doNotRemove";
    
    /**
     * Reference to the logging service.
     */
    private LoggingService m_Logging;
    
    private PowerManager m_PowerManager;
    
    private WakeLock m_WakeLock;

    @Reference(optional = true, dynamic = true)
    public void setPowerManager(final PowerManager powerManager)
    {
        m_PowerManager = powerManager;
    }
    
    public void unsetPowerManager(final PowerManager powerManager)
    {
        if (m_PowerManager != null && m_PowerManager.equals(powerManager))
        {
            m_PowerManager = null;
        }
    }
    
    @Activate
    public void activate()
    {
        if (m_PowerManager != null && m_WakeLock == null)
        {
            m_WakeLock = m_PowerManager.createWakeLock(this.getClass(), this.getClass().getSimpleName() + "WakeLock");
        }
    }
    
    @Deactivate
    public void deactivate()
    {
        if (powerManagementExists())
        {
            m_WakeLock.delete();
            m_WakeLock = null;
        }
    }
    
    /**
     * Binds the logging service to this component.
     * 
     * @param loggingService
     *            Logging service reference
     */
    @Reference
    public void setLoggingService(final LoggingService loggingService)
    {
        m_Logging = loggingService;
    }

    @Override
    public void scanForNewAssets(final ScanResults results, final Set<Asset> existing) throws AssetException
    {
        if  (powerManagementExists())
        {
            m_WakeLock.activate();
        }
        
        Set<Asset> filtered = new HashSet<>();
        //Ignore assets whose name contain a certain String
        for (Asset asset : existing)
        {
            if (!asset.getName().contains(DO_NOT_REMOVE))
            {
                filtered.add(asset);
            }
        }
        
        if (filtered.isEmpty())
        {
            Map<String, Object> props = new HashMap<>();
            props.put(ExampleAssetAttributes.CONFIG_PROP_DEVICE_POWER_NAME, "exampleB");
            results.found(ASSET_NAME, props);
        }
        else
        {
            m_Logging.log(LogService.LOG_INFO, "Nothing new found");
        }
        
        if (powerManagementExists())
        {
            m_WakeLock.cancel();
        }
    }
    
    private boolean powerManagementExists()
    {
        return m_PowerManager != null && m_WakeLock != null;       
    }
}
