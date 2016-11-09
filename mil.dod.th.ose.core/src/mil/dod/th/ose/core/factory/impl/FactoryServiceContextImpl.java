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
package mil.dod.th.ose.core.factory.impl;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.capability.BaseCapabilities;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.ose.core.factory.api.DirectoryService;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryObjectInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.factory.api.FactoryRegistryCallback;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.factory.api.FactoryServiceProxy;
import mil.dod.th.ose.metatype.MetaTypeProviderBundle;
import mil.dod.th.ose.utils.BundleService;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Contains information about a {@link mil.dod.th.core.factory.FactoryObject} service context. The {@link 
 * FactoryServiceContext} is used to pass data from the factory object service implementation (AssetDirectoryService, 
 * AddressManagerService, etc) to the device factory.
 * 
 * @author Josh
 * 
 * @param <T>
 *  internal base type of objects created by the factories this service supports
 */
@Component(factory = FactoryServiceContext.FACTORY_NAME)
public class FactoryServiceContextImpl<T extends FactoryObjectInternal> implements FactoryServiceContext<T>
{
    /**
     * Reference to the {@link ComponentFactory} that is used to create a {@link FactoryRegistry} instance.
     */
    private ComponentFactory m_RegistryFactory;
    
    /**
     * Reference to the component instance that is created for a {@link FactoryRegistry} instance.
     */
    private ComponentInstance m_RegistryComponent;
    
    /**
     * List of factory objects created by this factory.
     */
    private FactoryRegistry<T> m_FactoryRegistry;
    
    /**
     * {@link BundleContext} Bundle context for this factory objects bundle.
     */
    private BundleContext m_CoreContext;
    
    /**
     * Map of factories which pertains to this factory service context where the string is value returned by {@link 
     * mil.dod.th.core.factory.FactoryDescriptor#getProductType()}.
     */
    private final Map<String, FactoryInternal> m_Factories = 
            Collections.synchronizedMap(new HashMap<String, FactoryInternal>());
    
    /**
     * Proxy that is implemented by each factory service type.
     */
    private FactoryServiceProxy<T> m_Proxy;

    /**
     * Reference to the logging service.
     */
    private LoggingService m_Logging;

    /**
     * Reference to the component factory responsible for instantiating instances of {@link FactoryInternal}.
     */
    private ComponentFactory m_FactoryInternalFactory;

    /**
     * Reference to the meta type service.
     */
    private MetaTypeService m_MetaTypeService;

    /**
     * Reference to the meta type provider bundle.
     */
    private MetaTypeProviderBundle m_MetaTypeProviderBundle;

    /**
     * Reference to the factory directory service.
     */
    private DirectoryService m_DirectoryService;
   
    /**
     * Reference to the plugin factory tracker.
     */
    private ServiceTracker<ComponentFactory, ComponentFactory> m_PluginFactoryTracker;
    
    private BundleService m_BundleService;
    
    /**
     * Method used by OSGi to bind the logging service.
     * 
     * @param loggingService
     *      {@link LoggingService} to be set.
     */
    @Reference
    public void setLoggingService(final LoggingService loggingService)
    {
        m_Logging = loggingService;
    }
    
    /**
     * Method used by OSGi to bind the meta type service.
     * 
     * @param metaTypeService
     *      {@link MetaTypeService} to be set.
     */
    @Reference
    public void setMetaTypeService(final MetaTypeService metaTypeService)
    {
        m_MetaTypeService = metaTypeService;
    }
    
    /**
     * Method used by OSGi to bind the meta type provider bundle.
     * 
     * @param metaTypeProviderBundle
     *      {@link MetaTypeProviderBundle} to be set.
     */
    @Reference
    public void setMetaTypeProviderBundle(final MetaTypeProviderBundle metaTypeProviderBundle)
    {
        m_MetaTypeProviderBundle = metaTypeProviderBundle;
    }
    
    /**
     * Method used to set the {@link ComponentFactory} to be used for creating the factory registry.
     * 
     * @param factory
     *  the factory to use
     */
    @Reference(target = "(" + ComponentConstants.COMPONENT_FACTORY + "=" 
            + FactoryRegistry.COMPONENT_FACTORY_REG_ID + ")")
    public void setFactoryRegistry(final ComponentFactory factory)
    {
        m_RegistryFactory = factory;
    }
    
    /**
     * Method used by OSGi to bind the factory internal factory.
     * 
     * @param factoryInternalFactory
     *      {@link ComponentFactory} that represents the factory internal factory.
     */
    @Reference(target = "(" + ComponentConstants.COMPONENT_FACTORY + "=" + FactoryInternal.FACTORY_NAME + ")")
    public void setFactoryInternalFactory(final ComponentFactory factoryInternalFactory)
    {
        m_FactoryInternalFactory = factoryInternalFactory;
    }
    
    @Reference
    public void setBundleService(final BundleService bundleService)
    {
        m_BundleService = bundleService;
    }

    @Override
    public void initialize(final BundleContext context, final FactoryServiceProxy<T> proxy, 
           final DirectoryService directoryService) throws InvalidSyntaxException
    {
        m_Proxy = proxy;
        m_DirectoryService = directoryService;
       
        m_CoreContext = context;
        
        m_RegistryComponent = m_RegistryFactory.newInstance(null);
        @SuppressWarnings("unchecked")
        final FactoryRegistry<T> factoryRegistry = (FactoryRegistry<T>)m_RegistryComponent.getInstance();
        m_FactoryRegistry = factoryRegistry;
        final FactoryRegistryCallback<T> callback = m_Proxy.createCallback(this);
        m_FactoryRegistry.initialize(m_DirectoryService, m_Proxy, callback);
        
        // set up tracker for factories
        final Filter filter = context.createFilter(String.format("(%s=%s)", 
                ComponentConstants.COMPONENT_FACTORY, m_Proxy.getBaseType().getName()));
        m_PluginFactoryTracker = new ServiceTracker<ComponentFactory, ComponentFactory>(m_CoreContext, filter, 
                new PluginFactoryTracker());
        m_PluginFactoryTracker.open();
    }
    
    /**
     * Deactivates the service and disposes of the plugin factory tracker and factory registry.
     */
    @Deactivate
    public void deactivate()
    {
        m_PluginFactoryTracker.close();
        if (m_RegistryComponent != null)
        {
            m_RegistryComponent.dispose();
        }
    }

    @Override
    public FactoryRegistry<T> getRegistry()
    {
        return m_FactoryRegistry;
    }
    
    @Override
    public DirectoryService getDirectoryService()
    {
        return m_DirectoryService;
    }

    @Override
    public BundleContext getCoreContext()
    {
        return m_CoreContext;
    }
    
    @Override
    public Map<String, FactoryInternal> getFactories()
    {
        return m_Factories;
    }
    
    @Override
    public MetaTypeService getMetaTypeService()
    {
        return m_MetaTypeService;
    }
    
    @Override
    public MetaTypeProviderBundle getMetaTypeProviderBundle()
    {
        return m_MetaTypeProviderBundle;
    }

    @Override
    public Class<? extends BaseCapabilities> getCapabilityType()
    {
        return m_Proxy.getCapabilityType();
    }

    @Override
    public AttributeDefinition[] getExtendedServiceAttributeDefinitions(final FactoryInternal factory, final int filter)
    {
        return m_Proxy.getExtendedServiceAttributeDefinitions(this, factory, filter);
    }

    @Override
    public Dictionary<String, Object> getAdditionalFactoryRegistrationProps(final FactoryInternal factory)
    {
        return m_Proxy.getAdditionalFactoryRegistrationProps(factory);
    }
    
    @Override
    public Class<? extends FactoryObject> getBaseType()
    {
        return m_Proxy.getBaseType();
    }
    
    @Override
    public Bundle getApiBundle()
    {
        return m_BundleService.getBundle(FactoryObject.class);
    }
    
    /**
     * Service tracker responsible for for handling the registration of factories.
     */
    public class PluginFactoryTracker implements ServiceTrackerCustomizer<ComponentFactory, ComponentFactory>
    {
        /**Executor used to queue and handle registering factories and making them available.*/
        final private ExecutorService m_ExecutorService = Executors.newFixedThreadPool(1);
        
        /**
         * Map that stores all instantiated factory component instances. The key is the product type of the factory and
         * value is it's component instance.
         */
        final private Map<String, ComponentInstance> m_FactoryComponents = 
                Collections.synchronizedMap(new HashMap<String, ComponentInstance>());
        
        /**
         * Create {@link FactoryInternal} instances and initialize the factory by restoring objects for the factory and 
         * registering services.
         * 
         * Call to {@link FactoryInternal#makeAvailable()} must come after the call to {@link 
         * FactoryInternal#registerServices()} as the {@link FactoryInternal#makeAvailable()} method is what 
         * signals to other services that the factory is wholly registered and ready. Therefore, it must also be in the 
         * {@link FactoryServiceContext} set of factories first. If the factory cannot be successfully registered then 
         * the factory will be removed from the list of active factories kept by the given 
         * {@link FactoryServiceContext}.
         * 
         * @param reference 
         *      The reference to the service being added to the ServiceTracker.
         * @return
         *      The service object to be tracked for the specified referenced service or null if the specified 
         *      referenced service should not be tracked.
         */
        @Override
        public ComponentFactory addingService(final ServiceReference<ComponentFactory> reference)
        {
            final String productType = (String)reference.getProperty(ComponentConstants.COMPONENT_NAME);
            
            //Verify that a factory hasn't already been registered for the given product type.
            if (m_Factories.containsKey(productType))
            {
                m_Logging.warning("Factory already registered with product type [%s], " 
                        + "will not register factory from bundle [%s]", productType, reference.getBundle());
                return null;
            }
            
            // register as service so other components can track this factory
            m_Logging.info("Registering factory for: %s", productType);
            try
            {
                final ComponentFactory proxyComponentFactory = m_CoreContext.getService(reference);
                final Dictionary<String, Object> factoryInternalProps = new Hashtable<>();
                factoryInternalProps.put(FactoryInternal.KEY_SERVICE_CONTEXT, FactoryServiceContextImpl.this);
                factoryInternalProps.put(FactoryInternal.KEY_SERVICE_REFERENCE, reference);
                factoryInternalProps.put(FactoryInternal.KEY_COMPONENT_FACTORY, proxyComponentFactory);
                
                final ComponentInstance factoryComponentInstance = 
                        m_FactoryInternalFactory.newInstance(factoryInternalProps);
                m_FactoryComponents.put(productType, factoryComponentInstance);
                
                final FactoryInternal factory = (FactoryInternal)factoryComponentInstance.getInstance();
                
                m_Proxy.beforeAddFactory(FactoryServiceContextImpl.this, factory);
                
                // must put factory in map after call to beforeAddFactory as proxy assumes factory is not in map yet
                m_Factories.put(productType, factory);
                
                m_ExecutorService.execute(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            factory.registerServices();
                            getRegistry().restoreAllObjects(factory);
                            factory.makeAvailable();
                        }
                        catch (final RuntimeException e)
                        {
                            handleRegistrationException(reference, productType, factory, e);
                        }
                    }
                });
                return proxyComponentFactory;
                    
            }
            catch (final Exception e)
            {
                handleRegistrationException(reference, productType, null, e);
            }

            return null;
        }

        @Override
        public void modifiedService(final ServiceReference<ComponentFactory> reference, final ComponentFactory service)
        {
            final String productType = (String)reference.getProperty(ComponentConstants.COMPONENT_NAME);
            
            m_Logging.warning("Updating factory plug-in properties is not supported. "
                    + "Ignoring modified plug-in for product type [%s] from bundle [%s]", 
                    productType, reference.getBundle());
        }

        /**
         * Unregister a factory as an OSGi service so other bundles that need the factory know it is no longer 
         * available.
         * 
         * @param service 
         *      The service object for the specified referenced service.
         * @param reference 
         *      The reference to the service that has been removed.
         */
        @Override
        public void removedService(final ServiceReference<ComponentFactory> reference, final ComponentFactory service)
        {
            final String productType = (String)reference.getProperty(ComponentConstants.COMPONENT_NAME);
            
            m_Logging.info("Unregistering factory for: %s", productType);
            
            final FactoryInternal factory = m_Factories.get(productType);
            
            //make unavailable via framework
            tryMakeFactoryUnavailable(factory);
            
            // allow proxy services to do clean up first
            tryProxyOnRemoveFactory(factory);
            
            //remove all associated factory objects
            tryRemoveAllFactoryObjects(factory);
            //unreg the rest of the services and other cleanup
            tryCleanupFactory(factory);
            
            //make unavailable to management services
            tryRemoveFactory(productType);
            
            //dispose of the factory instance
            tryDisposeFactory(productType);
            
            //remove reference to service
            tryUngetService(reference);
        }
        
        /**
         * Clean up registration for the failed factory.
         * 
         * @param reference
         *      reference to the {@link ComponentFactory} that was used to create the {@link FactoryInternal}
         * @param productType
         *      product type of the plug-in that failed to register
         * @param factory
         *      factory to unregister or null if the factory was never created, but other clean up is needed
         * @param cause
         *      cause of the exception
         */
        private void handleRegistrationException(final ServiceReference<ComponentFactory> reference,
                final String productType, final FactoryInternal factory, final Exception cause)
        {
            m_Logging.error(cause, "Unable to register plug-in for product type [%s] from bundle [%s]", 
                    productType, reference.getBundle());
            //Cleanup any actions taken to register a factory.
            if (factory != null)
            {
                tryMakeFactoryUnavailable(factory);
                tryRemoveAllFactoryObjects(factory);
                tryCleanupFactory(factory);
            }
            tryRemoveFactory(productType);
            tryDisposeFactory(productType);
            tryUngetService(reference);
        }
        
        /**
         * Attempts to make the specified factory unavailable. This method will catch and log any exception thrown.
         * 
         * @param factory
         *          Factory to be made unavailable.
         */
        private void tryMakeFactoryUnavailable(final FactoryInternal factory)
        {
            try
            {
                factory.makeUnavailable();
            }
            catch (final Exception e)
            {
                m_Logging.error(e, "Unable to make factory with product type [%s] unavailable", 
                        factory.getProductType());
            }
        }
        
        /**
         * Attempts to call the {@link FactoryServiceProxy}'s on remove factory method.
         * 
         * @param factory
         *      The factory being removed.
         */
        private void tryProxyOnRemoveFactory(final FactoryInternal factory)
        {
            try
            {
                m_Proxy.onRemoveFactory(FactoryServiceContextImpl.this, factory);
            }
            catch (final Exception e)
            {
                m_Logging.error(e, "Unable to complete call to factory service proxy's on remove factory method for "
                        + "factory with product type [%s]", factory.getProductType());
            }
        }
        
        /**
         * Attempts to remove all factory objects associate with the specified factory. This method will catch and log
         * any exception thrown.
         * 
         * @param factory
         *      The factory that should have all associated factory objects removed.
         */
        private void tryRemoveAllFactoryObjects(final FactoryInternal factory)
        {
            for (FactoryObjectInternal object: getRegistry().getObjectsByProductType(factory.getProductType()))
            {
                try
                {
                    getRegistry().removeObject(object.getUuid());
                    factory.dispose(object.getProxy()); 
                }
                catch (final Exception e)
                {
                    m_Logging.error(e, "Unable to remove all factory objects of product type [%s]", 
                            factory.getProductType());
                }
            }
        }
        
        /**
         * Attempts to call a factories cleanup method. This method will catch and log any exception thrown.
         * 
         * @param factory
         *      The factory who's cleanup method should be called.
         */
        private void tryCleanupFactory(final FactoryInternal factory)
        {
            try
            {
                factory.cleanup();
            }
            catch (final Exception e)
            {
                m_Logging.error(e, "Unable to cleanup factory with product type [%s]", factory.getProductType());
            }
        }
        
        /**
         * Attempts to remove the factory with the given product type from the map of factories which pertains to this 
         * factory service context. This method will catch and log any exception thrown.
         * 
         * @param productType
         *      String that represents the key for the factory that should be removed from the map of factories that
         *      pertains to this factory service context.
         */
        private void tryRemoveFactory(final String productType)
        {
            try
            {
                m_Factories.remove(productType);
            }
            catch (final Exception e)
            {
                m_Logging.error(e, "Unable to remove factory with product type [%s] from map of available factories", 
                        productType);
            }
        }
        
        /**
         * Attempts to dispose of the factory component instance for the factory with the specified product type. This
         * method will catch and log any exception thrown.
         * 
         * @param productType
         *      The product type of the factory who's factory component instance should be disposed of.
         */
        private void tryDisposeFactory(final String productType)
        {
            try
            {
                final ComponentInstance comp = m_FactoryComponents.remove(productType);
                if (comp != null)
                {
                    comp.dispose();
                }
            }
            catch (final Exception e)
            {
                m_Logging.error(e, "Unable to dispose of factory component instance with product type [%s]", 
                        productType);
            }
        }
        
        /**
         * Attempts to unregister the specified service. This method will catch and log any exception thrown.
         * 
         * @param reference
         *      Reference to the service to be unregistered.
         */
        private void tryUngetService(final ServiceReference<ComponentFactory> reference)
        {
            try
            {
                m_CoreContext.ungetService(reference);
            }
            catch (final Exception e)
            {
                m_Logging.error(e, "Unable to remove registration for plug-in with product type [%s] from bundle [%s]", 
                        reference.getProperty(ComponentConstants.COMPONENT_NAME), reference.getBundle());
            }
        }
    }
}
