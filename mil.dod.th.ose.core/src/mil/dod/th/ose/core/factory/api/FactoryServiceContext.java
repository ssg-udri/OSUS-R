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
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.ose.metatype.MetaTypeProviderBundle;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeService;

/**
 * This interface contains accessors for information about a {@link mil.dod.th.core.factory.FactoryObject}'s supporting 
 * services. This context is used to pass data from the factory object's management service implementation 
 * (AssetDirectoryService, AddressManagerService, etc) to the corresponding device factory.
 * @author Josh
 * 
 * @param <T>
 *  internal base type of objects created by the factories this service supports
 */
public interface FactoryServiceContext<T extends FactoryObjectInternal>
{
    /**
     * ID of the component factory for this class.
     */
    String FACTORY_NAME = "mil.dod.th.ose.core.factory.api.FactoryServiceContext";
    
    /**
     * Method initializes the component. This method can initiate calls back to the {@link DirectoryService} for 
     * factory plug-ins that are already available. Therefore, callers must ensure the service is ready to receive calls
     * before initializing this component.
     * 
     * @param context
     *      Bundle context of the bundle which contains this component
     * @param proxy
     *      Proxy for this factory service
     * @param directoryService
     *      Directory service using this context
     * @throws InvalidSyntaxException
     *      Thrown if the filter string used to create the plugin factory tracker is invalid.
     */
    void initialize(BundleContext context, FactoryServiceProxy<T> proxy, DirectoryService directoryService) 
            throws InvalidSyntaxException;
    /**
     * Method that returns the map of all currently activated factories identified by the product type 
     * that each factory produces.
     * @return
     *  the synchronized map of factories where key is the value returned by 
     *  {@link mil.dod.th.core.factory.FactoryDescriptor#getProductType()}
     */
    Map<String, FactoryInternal> getFactories();
    
    /**
     * Gets the Factory Registry for this factory object.
     * 
     * @return
     *  the {@link FactoryRegistry} 
     */
    FactoryRegistry<T> getRegistry();
    
    /**
     * Get the directory service that has created this context.
     * 
     * @return
     *      directory service that can be cast to a specific service like 
     *      {@link mil.dod.th.core.asset.AssetDirectoryService}.
     */
    DirectoryService getDirectoryService();
    
    /**
     * Gets the bundle context of the <b>core</b> bundle.
     * 
     * @return
     *  the {@link BundleContext} for this factory object.
     */
    BundleContext getCoreContext();
    
    /**
     * Get the service that provides a separate bundle that {@link org.osgi.service.metatype.MetaTypeProvider} services 
     * can use.
     * 
     * @return
     *      bound service
     */
    MetaTypeProviderBundle getMetaTypeProviderBundle();
    
    /**
     * Gets a metatype service reference.
     * 
     * @return
     *  metatype service reference
     */
    MetaTypeService getMetaTypeService();
    
    /**
     * Method used to get Capabilities class for the particular {@link DirectoryService}.
     * @return 
     *      returns Capabilities class
     */
    Class<? extends BaseCapabilities> getCapabilityType();
    
    /**
     * Get additional service specific attributes. Used to return specific attributes for the service.
     * For example, serial ports have additional attribute definitions in comparison to most physical link types.
     * 
     * @param factory
     *      the factory to evaluate for additional attributes
     * @param filter
     *      whether the attributes are required or optional
     * @return
     *      the service specific attributes
     */
    AttributeDefinition[] getExtendedServiceAttributeDefinitions(FactoryInternal factory, int filter);
    
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
     * Get the base type of object supported by this service proxy, such as {@link mil.dod.th.core.asset.Asset}.
     * 
     * @return
     *      the base type
     */
    Class<? extends FactoryObject> getBaseType();
    
    /**
     * Get a reference to the bundle containing the core API.
     * 
     * @return
     *      bundle containing the core API
     */
    Bundle getApiBundle();
}
