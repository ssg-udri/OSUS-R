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
package mil.dod.th.ose.core.impl.ccomm;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.capability.BaseCapabilities;
import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.AddressFactory;
import mil.dod.th.core.ccomm.AddressProxy;
import mil.dod.th.core.ccomm.capability.AddressCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistryCallback;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.factory.api.FactoryServiceProxy;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectDataManager;
import mil.dod.th.ose.core.impl.ccomm.data.AddressFactoryObjectDataManager;

import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.metatype.AttributeDefinition;

/**
 * Proxy to provide {@link mil.dod.th.core.ccomm.Address} specific service functionality.
 * 
 * @author dlandoll
 */
@Component(properties = { AddressInternal.SERVICE_TYPE_PAIR })
public class AddressServiceProxy implements FactoryServiceProxy<AddressInternal>
{
    /**
     * Component factory used to create {@link AddressImpl}'s.
     */
    private ComponentFactory m_AddressComponentFactory;
    
    /**
     * Service for storing persistent data about Addresses.
     */
    private AddressFactoryObjectDataManager m_DataManager;
    
    /**
     * Method to set the component factory that will be used to create 
     * instances of {@link AddressImpl}.
     * 
     * @param factory
     *   the factory that will be used to create the instances
     */
    @Reference(target = "(component.factory=" + AddressInternal.COMPONENT_FACTORY_REG_ID + ")")
    public void setAddressFactory(final ComponentFactory factory)
    {
        m_AddressComponentFactory = factory;
    }
    
    @Reference
    public void setAddressFactoryObjectDataManager(final AddressFactoryObjectDataManager factoryObjectDataManager)
    {
        m_DataManager = factoryObjectDataManager;
    }
    
    @Override
    public void initializeProxy(final AddressInternal object, final FactoryObjectProxy proxy, 
            final Map<String, Object> props) throws FactoryException
    {
        final AddressProxy addrProxy = (AddressProxy)proxy;
        addrProxy.initialize(object, props);
    }

    @Override
    public ComponentInstance createFactoryObjectInternal(final FactoryInternal factory)
    {
        return m_AddressComponentFactory.newInstance(new Hashtable<String, Object>());
    }

    @Override
    public AttributeDefinition[] getExtendedServiceAttributeDefinitions(
            final FactoryServiceContext<AddressInternal> factoryServiceContext, 
            final FactoryInternal factory, final int filter)
    {
        return new AttributeDefinition[]{};
    }

    @Override
    public Class<? extends BaseCapabilities> getCapabilityType()
    {
        return AddressCapabilities.class;
    }

    @Override
    public Class<? extends FactoryObject> getBaseType()
    {
        return Address.class;
    }
    
    @Override
    public FactoryObjectDataManager getDataManager()
    {
        return m_DataManager;
    }

    @Override
    public Dictionary<String, Object> getAdditionalFactoryRegistrationProps(final FactoryInternal factory)
    {
        final Dictionary<String, Object> addrFactProps = new Hashtable<>();
        addrFactProps.put(FactoryDescriptor.FACTORY_TYPE_SERVICE_PROPERTY, AddressFactory.class);
        addrFactProps.put(AddressFactory.ADDRESS_FACTORY_PREFIX_SERVICE_PROPERTY,
            factory.getAddressCapabilities().getPrefix());
        return addrFactProps;
    }
    
    @Override
    public void beforeAddFactory(final FactoryServiceContext<AddressInternal> factoryServiceContext, 
            final FactoryInternal factory) throws FactoryException
    {
        final AddressManagerServiceImpl addressMgrSvc = 
                (AddressManagerServiceImpl)factoryServiceContext.getDirectoryService();
        
        final String factoryPrefix = factory.getAddressCapabilities().getPrefix();
        final AddressFactory existingFactory = addressMgrSvc.getFactoryByPrefix(factoryPrefix);
      
        if (existingFactory != null)
        {
            throw new FactoryException(String.format(
                    "Attempted to add address plug-in [%s] with an existing prefix [%s]. Already registered with [%s].",
                    factory.getProductType(), factoryPrefix, existingFactory.getProductType()));
        }
    }

    @Override
    public void onRemoveFactory(final FactoryServiceContext<AddressInternal> factoryServiceContext, 
            final FactoryInternal factory)
    {
        // nothing to be done
    }

    @Override
    public FactoryRegistryCallback<AddressInternal> createCallback(
            final FactoryServiceContext<AddressInternal> factoryServiceContext)
    {
        return new AddressRegistryCallback();
    }
}
