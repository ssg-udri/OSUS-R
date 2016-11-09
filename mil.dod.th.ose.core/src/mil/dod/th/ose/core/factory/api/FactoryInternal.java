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

import mil.dod.th.core.asset.AssetFactory;
import mil.dod.th.core.ccomm.AddressFactory;
import mil.dod.th.core.ccomm.link.LinkLayerFactory;
import mil.dod.th.core.ccomm.physical.PhysicalLinkFactory;
import mil.dod.th.core.ccomm.transport.TransportLayerFactory;
import mil.dod.th.core.datastream.StreamProfileFactory;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryObjectProxy;

import org.osgi.service.metatype.AttributeDefinition;

/**
 * This interface tracks the OSGi component instances for a particular plug-in type.
 * 
 * @author jlatham
 *
 */
public interface FactoryInternal extends FactoryDescriptor, AssetFactory, StreamProfileFactory, AddressFactory, 
    PhysicalLinkFactory, LinkLayerFactory, TransportLayerFactory
{
    /**
     * Component factory name of the {@link org.osgi.service.component.ComponentFactory} that produces instances of this
     * interface. 
     */
    String FACTORY_NAME = "mil.dod.th.ose.core.factory.api.FactoryInternal";
    
    /**
     * Property must be supplied as a component property and set to the {@link FactoryServiceContext} for the service 
     * creating this component.
     */
    String KEY_SERVICE_CONTEXT = "factory.service.context";
    
    /**
     * Property must be supplied as a component property and set to the {@link org.osgi.framework.ServiceReference} 
     * instance for the {@link org.osgi.service.component.ComponentFactory} provided by the factory plug-in.
     */
    String KEY_SERVICE_REFERENCE = "factory.service.ref";
    
    /**
     * Property must be supplied as a component property and set to the 
     * {@link org.osgi.service.component.ComponentFactory} provided by the factory plug-in.
     */
    String KEY_COMPONENT_FACTORY = "plugin.component.factory";
    
    /**
     * Get the plug-in specific attribute definitions.
     * 
     * @return
     *      array of AttributeDefinition objects for the plug-in
     * @param filter
     *      whether to get required, optional, or all attributes
     */
    AttributeDefinition[] getPluginAttributeDefinitions(int filter);
    
    /**
     * Core will call this method when a factory plug-in is registered to allow configuration and metatype services
     * to access plug-in.
     */
    void registerServices();

    /**
     * Method called to register the {@link FactoryDescriptor} as an OSGi service, thus making the factory available to
     * other OSGi service components.
     */
    void makeAvailable();
    
    /**
     * Method called to unregister the {@link FactoryDescriptor} as an OSGi service, thus making the factory 
     * unavailable to other OSGi service components.
     */
    void makeUnavailable();
    
    /**
     * Unregisters the remaining of the factory's services from the the OSGi framework. 
     */
    void cleanup();
    
    /**
     * Create a proxy registered with this factory.
     * 
     * @return
     *      proxy instance for the plug-in
     */
    FactoryObjectProxy create();

    /**
     * Dispose of a previously created proxy.
     * 
     * @param obj
     *            the object that shall be deleted.
     */
    void dispose(FactoryObjectProxy obj);
    
    /**
     * Retrieve the PID for this factory.
     * 
     * @return pid
     *      the PID of the factory
     */
    String getPid();
}
