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
package mil.dod.th.ose.core.impl.ccomm.transport;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.capability.BaseCapabilities;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.ccomm.transport.TransportLayerFactory;
import mil.dod.th.core.ccomm.transport.TransportLayerProxy;
import mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.factory.api.FactoryRegistryCallback;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.factory.api.FactoryServiceProxy;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectDataManager;
import mil.dod.th.ose.core.impl.ccomm.TransportLayerRegistryCallback;
import mil.dod.th.ose.core.impl.ccomm.transport.data.TransportLayerFactoryObjectDataManager;

import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.metatype.AttributeDefinition;

/**
 * Proxy to provide {@link mil.dod.th.core.ccomm.transport.TransportLayer} specific service functionality.
 * 
 * @author allenchl
 *
 */
@Component(properties = { TransportLayerInternal.SERVICE_TYPE_PAIR })
public class TransportLayerServiceProxy implements FactoryServiceProxy<TransportLayerInternal>
{
    /**
     * Component factory used to create {@link TransportLayerImpl}'s.
     */
    private ComponentFactory m_TransLayerComponentFactory;
    
    /**
     * Service for storing persisted data about transport layers.
     */
    private TransportLayerFactoryObjectDataManager m_TransportFactoryObjectDataManager;
    
    /**
     * Method to set the component factory that will be used to create 
     * instances of {@link TransportLayerImpl}.
     * @param factory
     *  the factory that will be used to create the instances
     */
    @Reference(target = "(component.factory=" + TransportLayerInternal.COMPONENT_FACTORY_REG_ID + ")")
    public void setTransportLayerFactory(final ComponentFactory factory)
    {
        m_TransLayerComponentFactory = factory;
    }

    /**
     * Bind the factory object data information manager for transport layers.
     * 
     * @param transFactObjMgr
     *      used to store the persisted data
     */
    @Reference
    public void setTransportLayerFactoryObjectDataManager(final TransportLayerFactoryObjectDataManager transFactObjMgr)
    {
        m_TransportFactoryObjectDataManager = transFactObjMgr;
    }
    
    @Override
    public void initializeProxy(final TransportLayerInternal object, final FactoryObjectProxy proxy, 
            final Map<String, Object> props) throws FactoryException
    {
        final TransportLayerProxy transProxy = (TransportLayerProxy)proxy;
        transProxy.initialize(object, props);
    }

    @Override
    public ComponentInstance createFactoryObjectInternal(final FactoryInternal factory)
    {
        return m_TransLayerComponentFactory.newInstance(new Hashtable<String, Object>());
    }

    @Override
    public AttributeDefinition[] getExtendedServiceAttributeDefinitions(
            final FactoryServiceContext<TransportLayerInternal> factoryServiceContext,
            final FactoryInternal factory, final int filter)
    {
        return new AttributeDefinition[]{};
    }

    @Override
    public Class<? extends BaseCapabilities> getCapabilityType()
    {
        return TransportLayerCapabilities.class;
    }

    @Override
    public Class<? extends FactoryObject> getBaseType()
    {
        return TransportLayer.class;
    }

    @Override
    public Dictionary<String, Object> getAdditionalFactoryRegistrationProps(final FactoryInternal factory)
    {
        final Dictionary<String, Object> transportFactProps = new Hashtable<>();
        transportFactProps.put(FactoryDescriptor.FACTORY_TYPE_SERVICE_PROPERTY, TransportLayerFactory.class);
        return transportFactProps;
    }

    @Override
    public void onRemoveFactory(final FactoryServiceContext<TransportLayerInternal> factoryServiceContext,
            final FactoryInternal factory)
    {
        //save away reg here so we don't have to cast below
        final FactoryRegistry<TransportLayerInternal> reg = factoryServiceContext.getRegistry();
        
        // Shutdown trans layers
        for (TransportLayerInternal transportLayer : reg.getObjectsByProductType(factory.getProductType()))
        {
            transportLayer.shutdown();
        }
    }

    @Override
    public void beforeAddFactory(final FactoryServiceContext<TransportLayerInternal> factoryServiceContext,
            final FactoryInternal factory)
    {
        // nothing to check
    }

    @Override
    public FactoryObjectDataManager getDataManager()
    {
        return m_TransportFactoryObjectDataManager;
    }

    @Override
    public FactoryRegistryCallback<TransportLayerInternal> createCallback(
            final FactoryServiceContext<TransportLayerInternal> factoryServiceContext)
    {
        return new TransportLayerRegistryCallback((CustomCommsService)factoryServiceContext.getDirectoryService());
    }
}
