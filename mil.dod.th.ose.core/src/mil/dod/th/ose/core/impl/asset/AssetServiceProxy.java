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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.Asset.AssetActiveStatus;
import mil.dod.th.core.asset.AssetFactory;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.capability.AssetCapabilities;
import mil.dod.th.core.capability.BaseCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.factory.api.FactoryRegistryCallback;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.factory.api.FactoryServiceProxy;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectDataManager;
import mil.dod.th.ose.core.impl.asset.data.AssetFactoryObjectDataManager;

import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.AttributeDefinition;

/**
 * Proxy to provide {@link mil.dod.th.core.asset.Asset} specific service functionality.
 * 
 * @author nickmarcucci
 *
 */
@Component(properties = { AssetInternal.SERVICE_TYPE_PAIR })
public class AssetServiceProxy implements FactoryServiceProxy<AssetInternal>
{
    /**
     * Component factory used to create {@link AssetImpl}'s.
     */
    private ComponentFactory m_AssetComponentFactory;
    
    /**
     * Logging service used to log messages.
     */
    private LoggingService m_Logging;

    /**
     * Asset factory object data manager service.
     */
    private AssetFactoryObjectDataManager m_AssetFactoryData;

    /**
     * Method to set the component factory that will be used to create 
     * instances of {@link AssetImpl}.
     * @param factory
     *  the factory that will be used to create the instances
     */
    @Reference(target = "(" + ComponentConstants.COMPONENT_FACTORY + "=" + AssetInternal.COMPONENT_FACTORY_REG_ID + ")")
    public void setAssetFactory(final ComponentFactory factory)
    {
        m_AssetComponentFactory = factory;
    }
    
    /**
     * Bind the logging service to use.
     * @param logging
     *  logging service to be used for logging messages
     */
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }
    
    /**
     * Method to set the {@link AssetFactoryObjectDataManager} service to use.
     * @param manager
     *  the asset factory object data manager to use
     */
    @Reference
    public void setAssetFactoryObjectDataManager(final AssetFactoryObjectDataManager manager)
    {
        m_AssetFactoryData = manager;
    }
    
    @Override
    public void initializeProxy(final AssetInternal object, final FactoryObjectProxy proxy, 
            final Map<String, Object> props) throws FactoryException
    {
        final AssetProxy assetProxy = (AssetProxy)proxy;
        assetProxy.initialize(object, props);
    }

    @Override
    public ComponentInstance createFactoryObjectInternal(final FactoryInternal factory)
    {
        return m_AssetComponentFactory.newInstance(new Hashtable<String, Object>());
    }

    @Override
    public AttributeDefinition[] getExtendedServiceAttributeDefinitions(
            final FactoryServiceContext<AssetInternal> factoryServiceContext,
            final FactoryInternal factory, final int filter)
    {
        return new AttributeDefinition[]{};
    }

    @Override
    public Class<? extends BaseCapabilities> getCapabilityType()
    {
        return AssetCapabilities.class;
    }

    @Override
    public Dictionary<String, Object> getAdditionalFactoryRegistrationProps(final FactoryInternal factory)
    {
        final Dictionary<String, Object> assetFactProps = new Hashtable<>();
        assetFactProps.put(FactoryDescriptor.FACTORY_TYPE_SERVICE_PROPERTY, AssetFactory.class);
        return assetFactProps;
    }

    @Override
    public Class<? extends FactoryObject> getBaseType()
    {
        return Asset.class;
    }

    @Override
    public void onRemoveFactory(final FactoryServiceContext<AssetInternal> factoryServiceContext,
            final FactoryInternal factory)
    {
        final String productType = factory.getProductType();
        
        // initialize the semaphore and the listener that uses this semaphore.
        final Semaphore deactivateWaitSemaphore = new Semaphore(0);
        final OnAssetDeactivateListener listener = new OnAssetDeactivateListener(deactivateWaitSemaphore);

        final AssetActivationListener[] listeners = 
            new AssetActivationListener[] {new ActivationListenerBridge(m_Logging), listener};
        
        final List<Thread> deactivationThreads = new ArrayList<>();
        
        final FactoryRegistry<AssetInternal> reg = factoryServiceContext.getRegistry();
        
        // go through and begin the deactivation of assets
        for (AssetInternal asset : reg.getObjectsByProductType(productType))
        {
            if (asset.getActiveStatus() == AssetActiveStatus.ACTIVATED)
            {
                final Thread thread = asset.internalDeactivate(listeners);
                deactivationThreads.add(thread);
            }
        }
       
        final int activeCount = deactivationThreads.size();
        m_Logging.log(LogService.LOG_INFO, "Deactivating %d [%s] assets as factory is being removed", activeCount, 
                productType);

        // wait on assets to deactivate
        waitForAssetDeactivation(listener, deactivateWaitSemaphore, activeCount);
        
        for (Thread thread : deactivationThreads)
        {
            if (thread.isAlive())
            {
                thread.interrupt();
                m_Logging.log(LogService.LOG_WARNING, "Interrupted thread [%s] that was trying to Deactivate an Asset", 
                        thread.getName());
            }
        }
    }
    
    @Override
    public void beforeAddFactory(final FactoryServiceContext<AssetInternal> factoryServiceContext, 
            final FactoryInternal factory)
    {
        // nothing to check
    }

    @Override
    public FactoryObjectDataManager getDataManager()
    {
        return m_AssetFactoryData;
    }

    /**
     * Checks to see if all assets directed to deactivate are now deactivated. If they are then a message is logged 
     * indicating success.
     * 
     * @param dListener
     *      The on asset deactivate listener to listen for the deactivation of assets.
     * @param deactivateSemaphore
     *      The semaphore used for synchronization. Timeout while trying to acquire the semaphore
     *      indicates a problem with the deactivation of Assets and results in a warning in the fault log.
     * @param activeCount
     *      Number of active assets that were to be deactivated
     */
    private void waitForAssetDeactivation(final OnAssetDeactivateListener dListener,
            final Semaphore deactivateSemaphore, final int activeCount)
    {
        boolean acquiredSemaphore = false;

        // Wait for 10 seconds, until the dListener notifies that a Asset has failed to deactivate, or
        // all assets have been successfully deactivated.
        try
        {
            final int TIMEOUT = 10;
            acquiredSemaphore = deactivateSemaphore.tryAcquire(activeCount, TIMEOUT, TimeUnit.SECONDS);
        }
        catch (final InterruptedException e)
        {
            m_Logging.log(LogService.LOG_WARNING, e, "Interrupted while waiting for Assets to deactivate.");
        }

        // checks to see if semaphore was acquired or not.
        if (!acquiredSemaphore)
        {
            m_Logging.log(LogService.LOG_WARNING, "Timeout waiting for all Assets to DEACTIVATE");
        }

        // checks listener to see if it was successful
        if (dListener.wasSuccessful())
        {
            m_Logging.log(LogService.LOG_INFO, "All Assets successfully DEACTIVATED");
        }
        else
        {
            m_Logging.log(LogService.LOG_WARNING, "At least one Asset failed to deactivate.");
        }
    }

    @Override
    public FactoryRegistryCallback<AssetInternal> createCallback(
            final FactoryServiceContext<AssetInternal> factoryServiceContext)
    {
        return new AssetRegistryCallback(factoryServiceContext);
    }
}
