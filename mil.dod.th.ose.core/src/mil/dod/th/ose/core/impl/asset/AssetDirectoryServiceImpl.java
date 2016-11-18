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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.google.common.base.Preconditions;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetFactory;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.ose.core.factory.api.DirectoryService;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.factory.api.FactoryServiceProxy;
import mil.dod.th.ose.utils.SingleComponent;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.event.EventAdmin;

/**
 * The implementation of the {@link AssetDirectoryService}.
 * 
 * @see AssetDirectoryService
 * 
 * @author mgleason
 */
@Component
public class AssetDirectoryServiceImpl extends DirectoryService implements AssetDirectoryService
{
    ///////////////////////////////////////////////////////////////////////////
    // Private Fields
    ///////////////////////////////////////////////////////////////////////////
    
    /**
     * Reference to the service proxy for assets.
     */
    private FactoryServiceProxy<AssetInternal> m_FactoryServiceProxy; 

    /**
     * Factory service context.
     */
    private FactoryServiceContext<AssetInternal> m_FactoryContext;

    /**
     * Component wrapper for {@link FactoryServiceContext}s.
     */
    private SingleComponent<FactoryServiceContext<AssetInternal>> m_FactServiceContextComp;
    
    /**
     * Reference to the asset scanner manager.
     */
    private ScannerManager m_ScannerManager;

    /**
     * Wake lock used for asset directory service operations.
     */
    private WakeLock m_WakeLock;

    /**
     * Public Constructor to instantiate all of the maps for this class.
     */
    public AssetDirectoryServiceImpl()
    {
        super();
    }

    ///////////////////////////////////////////////////////////////////////////
    // OSGi binding methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        super.setLoggingService(logging);
    }
    
    @Override
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        super.setEventAdmin(eventAdmin);
    }
    
    @Override
    @Reference
    public void setPowerManager(final PowerManager powerManager)
    {
        super.setPowerManager(powerManager);
    }

    /**
     * Method used to set the {@link ComponentFactory} to be used for creating a {@link FactoryServiceContext}.
     * 
     * @param factory
     *  the factory to use
     */
    @Reference(target = "(" + ComponentConstants.COMPONENT_FACTORY + "=" + FactoryServiceContext.FACTORY_NAME + ")")
    public void setFactoryServiceContextFactory(final ComponentFactory factory)
    {
        m_FactServiceContextComp = new SingleComponent<FactoryServiceContext<AssetInternal>>(factory);
    }

    /**
     * Bind the service. Each service type will provide a service proxy, restrict to the asset one.
     * 
     * @param factoryServiceProxy
     *      service to bind
     */
    @Reference(target = "(" + AssetInternal.SERVICE_TYPE_PAIR + ")")
    public void setFactoryServiceProxy(final FactoryServiceProxy<AssetInternal> factoryServiceProxy)
    {
        m_FactoryServiceProxy = factoryServiceProxy;
    }
    
    /**
     * Binds the asset {@link ScannerManager} used to scan for new assets.
     * 
     * @param scannerManager
     *      Scanner manager object
     */
    @Reference
    public void setScannerManager(final ScannerManager scannerManager)
    {
        m_ScannerManager = scannerManager;
    }

    ///////////////////////////////////////////////////////////////////////////
    // OSGi Declarative Services activate/deactivate methods.
    ///////////////////////////////////////////////////////////////////////////

    /**
     * The service component activation method.
     * 
     * Registers factories that have been added before activation.
     * 
     * @param coreContext
     *      bundle context of the core bundle
     * @throws InvalidSyntaxException
     *      if {@link FactoryServiceContext} uses an invalid filter string
     */
    @Activate
    public void activate(final BundleContext coreContext) throws InvalidSyntaxException
    {
        m_FactoryContext = m_FactServiceContextComp.newInstance(null);
        m_FactoryContext.initialize(coreContext, m_FactoryServiceProxy, this);
        m_WakeLock = m_PowerManager.createWakeLock(getClass(), "coreAssetDirService");
    }

    /**
     * Deactivate this service and discard registry services.
     */
    @Deactivate
    public void deactivate()
    {
        //dispose of factory service component
        m_FactServiceContextComp.tryDispose();
        m_WakeLock.delete();
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // Inherited methods
    ///////////////////////////////////////////////////////////////////////////
    
    @Override
    public Set<AssetFactory> getAssetFactories()
    {
        final Set<FactoryInternal> set = new HashSet<>(m_FactoryContext.getFactories().values());
        @SuppressWarnings("unchecked")
        final Set<AssetFactory> toReturn = Collections.unmodifiableSet((Set<AssetFactory>)(Set<?>)set);
        return toReturn;
    }

    @Override
    public Set<Asset> getAssets()
    {
        return Collections.unmodifiableSet(new HashSet<Asset>(m_FactoryContext.getRegistry().getObjects()));
    }

    @Override
    public Set<Asset> getAssetsByType(final String productType) 
    {
        Preconditions.checkNotNull(productType);
        return Collections.unmodifiableSet(new HashSet<Asset>(
                m_FactoryContext.getRegistry().getObjectsByProductType(productType)));
    }

    @Override
    public Asset getAssetByUuid(final UUID uuid)
    {
        return m_FactoryContext.getRegistry().getObjectByUuid(uuid);
    }

    @Override
    public Asset getAssetByName(final String name) 
    {
        return m_FactoryContext.getRegistry().getObjectByName(name);
    }
    
    @Override
    public boolean isAssetAvailable(final String name)
    {
        Preconditions.checkNotNull(name);
        return m_FactoryContext.getRegistry().isObjectCreated(name);
    }
    
    @Override
    public Asset createAsset(final String productType) 
            throws AssetException, IllegalArgumentException
    {
        return createAsset(productType, null);
    }
    
    @Override
    public Asset createAsset(final String productType, final String name) throws AssetException,
            IllegalArgumentException
    {
        return createAsset(productType, name, new HashMap<String, Object>());
    }

    @Override
    public Asset createAsset(final String productType, final String name,
            final Map<String, Object> properties) throws AssetException, IllegalArgumentException
    {
        Preconditions.checkNotNull(productType);
        Preconditions.checkNotNull(properties);

        final FactoryInternal assetFactory = m_FactoryContext.getFactories().get(productType);
        if (assetFactory == null)
        {
            throw new IllegalArgumentException(
                    String.format("No Factory found that can create an Asset of [%s] type.", productType));
        }

        try
        {
            m_WakeLock.activate();

            // Create the Asset instance
            return m_FactoryContext.getRegistry().createNewObject(assetFactory, name, properties);
        }
        catch (final Exception e)
        {
            throw new AssetException("Unable to create Asset.", e);
        }
        finally
        {
            m_WakeLock.cancel();
        }
    }

    @Override
    public void scanForNewAssets()
    {
        m_ScannerManager.scanForAllAssets(this);
    }

    @Override
    public void scanForNewAssets(final String productType)
    {
        Preconditions.checkNotNull(productType);

        m_ScannerManager.scanForAssetsByType(this, productType);
    }

    @Override
    public Set<String> getScannableAssetTypes()
    {
        return m_ScannerManager.getScannableAssetTypes();
    }
}
