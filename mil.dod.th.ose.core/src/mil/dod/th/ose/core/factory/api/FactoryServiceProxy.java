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
package mil.dod.th.ose.core.factory.api;

import java.util.Dictionary;
import java.util.Map;

import mil.dod.th.core.capability.BaseCapabilities;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectDataManager;

import org.osgi.service.component.ComponentInstance;
import org.osgi.service.metatype.AttributeDefinition;

/**
 * Interface that will be implemented by each service type so that 
 * service type specific operations can be performed.
 * 
 * @param <T> 
 *      internal interface for the objects that this service proxy creates. This
 *      type should be a type like {@link mil.dod.th.ose.core.impl.asset.AssetInternal}.
 *      
 * 
 * @author nickmarcucci
 */
public interface FactoryServiceProxy<T extends FactoryObjectInternal>
{
    /**
     * Create the service specific proxy for the given object.
     * @param object
     *      the object for which the given {@link FactoryObjectProxy} should be initialized
     * @param proxy
     *      the proxy that is to be associated with this object
     * @param props
     *      the properties associated with the given object
     * @throws FactoryException
     *      thrown if the created proxy cannot be initialized
     */
    void initializeProxy(T object, FactoryObjectProxy proxy,
            Map<String, Object> props) throws FactoryException;
    
    /**
     * Method used to create {@link FactoryObjectInternal} for an object.
     * 
     * @param factory
     *      the factory describing the type of component instance that will be returned
     * @return
     *      the component instance that has been created for a new {@link FactoryObjectInternal} instance 
     */
    ComponentInstance createFactoryObjectInternal(FactoryInternal factory);
    
    /**
     * Method used to get Capabilities class for the particular {@link DirectoryService}.
     * @return 
     *      returns Capabilities class
     */
    Class<? extends BaseCapabilities> getCapabilityType();
    
    /**
     * Get the base type of object supported by this service proxy, such as {@link mil.dod.th.core.asset.Asset}.
     * 
     * @return
     *      the base type
     */
    Class<? extends FactoryObject> getBaseType();
    
    /**
     * Get additional service specific attributes. Used to return specific attributes for the service.
     * For example, serial ports have additional attribute definitions in comparison to most physical link types.
     * @param factory
     *      the factory to evaluate for additional attributes
     * @param factoryServiceContext
     *      the factory service context of the factory
     * @param filter
     *      whether the attributes are required or optional
     * @return
     *      the service specific attributes
     */
    AttributeDefinition[] getExtendedServiceAttributeDefinitions(FactoryServiceContext<T> factoryServiceContext,
            FactoryInternal factory, int filter);
    
    /**
     * Fetch any factory specific properties needed when registering {@link mil.dod.th.core.factory.FactoryDescriptor}.
     * 
     * @param factory
     *      the factory describing the type of component instance that will be returned
     * @return 
     *      returns {@link Dictionary} of additional properties to be added to the factory's registration properties 
     */
    Dictionary<String, Object> getAdditionalFactoryRegistrationProps(FactoryInternal factory);
    
    /**
     * Called when a factory is about to be added to a service. If a factory fails to be registered after this call is 
     * made, {@link #onRemoveFactory} will not be called. Method must be called before factory is available in {@link 
     * FactoryServiceContext#getFactories()}.
     * 
     * @param factoryServiceContext
     *      the factory service context of the factory is being added to
     * @param factory
     *      factory that is about to be added to the context service
     * @throws FactoryException
     *      proxy can reject the factory by throwing this exception
     */
    void beforeAddFactory(FactoryServiceContext<T> factoryServiceContext, FactoryInternal factory) 
            throws FactoryException;

    /**
     * Called when a factory is being removed from {@link FactoryServiceContext} service.
     * 
     * @param factoryServiceContext
     *      the factory service context the factory is being removed from
     * @param factory
     *      factory being removed
     */
    void onRemoveFactory(FactoryServiceContext<T> factoryServiceContext, FactoryInternal factory);

    /**
     * Get the data manager for this factory service.
     * 
     * @return
     *      data manager that applies to this factory service
     */
    FactoryObjectDataManager getDataManager();
    
    /**
     * Method used to construct a {@link FactoryRegistryCallback} to be called by the 
     * {@link mil.dod.th.ose.core.factory.api.FactoryRegistry}.
     * 
     * @param factoryServiceContext
     *      context for the type of factory service
     * @return
     *      the created factory registry callback
     */
    FactoryRegistryCallback<T> createCallback(FactoryServiceContext<T> factoryServiceContext);
}
