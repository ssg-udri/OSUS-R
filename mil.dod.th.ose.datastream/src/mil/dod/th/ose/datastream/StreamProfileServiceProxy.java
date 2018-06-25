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
package mil.dod.th.ose.datastream;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.capability.BaseCapabilities;
import mil.dod.th.core.datastream.StreamProfile;
import mil.dod.th.core.datastream.StreamProfileFactory;
import mil.dod.th.core.datastream.StreamProfileProxy;
import mil.dod.th.core.datastream.capability.StreamProfileCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistryCallback;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.factory.api.FactoryServiceProxy;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectDataManager;

import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.metatype.AttributeDefinition;

/**
 * Proxy to provide {@link StreamProfile} specific service functionality.
 * 
 * @author jmiller
 *
 */
@Component(properties = {StreamProfileInternal.SERVICE_TYPE_PAIR })
public class StreamProfileServiceProxy implements FactoryServiceProxy<StreamProfileInternal>
{
    
    /**
     * Component factory used to create {@link StreamProfileImpl}'s.
     */
    private ComponentFactory m_StreamProfileComponentFactory;
    
    /**
     * Logging service used to log messages.
     */
    private LoggingService m_Logging;
    
    /**
     * Stream profile factory object data manager service.
     */
    private StreamProfileFactoryObjectDataManager m_StreamProfileFactoryData;
    
    /**
     * Service used to register listeners for stream profile configuration updated events.
     */
    private StreamProfileConfigListener m_ConfigListener;
    
    /**
     * Method to set the component factory that will be used to create 
     * instances of {@link StreamProfileImpl}.
     * @param factory
     *  the factory that will be used to create the instances
     */
    @Reference(target = "(" + ComponentConstants.COMPONENT_FACTORY + "=" 
        + StreamProfileInternal.COMPONENT_FACTORY_REG_ID + ")")
    public void setStreamProfileFactory(final ComponentFactory factory)
    {
        m_StreamProfileComponentFactory = factory;
    }
    
    @Reference
    public void setConfigListener(final StreamProfileConfigListener configListener)
    {
        m_ConfigListener = configListener;
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
     * Method to set the {@link StreamProfileFactoryObjectDataManager} service to use.
     * @param manager
     *  the stream profile factory object data manager to use
     */
    @Reference
    public void setStreamProfileFactoryObjectDataManager(final StreamProfileFactoryObjectDataManager manager)
    {
        m_StreamProfileFactoryData = manager;
    }
 
    @Override
    public void initializeProxy(final StreamProfileInternal object, final FactoryObjectProxy proxy, 
            final Map<String, Object> props) throws FactoryException
    {
        final StreamProfileProxy streamProfileProxy = (StreamProfileProxy)proxy;
        streamProfileProxy.initialize(object, props);
        m_Logging.info("Initialized new StreamProfileProxy with properties: %s", props.toString());

    }

    @Override
    public ComponentInstance createFactoryObjectInternal(final FactoryInternal factory)
    {
        return m_StreamProfileComponentFactory.newInstance(new Hashtable<String, Object>());
    }

    @Override
    public Class<? extends BaseCapabilities> getCapabilityType()
    {
        return StreamProfileCapabilities.class;
    }

    @Override
    public Class<? extends FactoryObject> getBaseType()
    {
        return StreamProfile.class;
    }

    @Override
    public AttributeDefinition[] getExtendedServiceAttributeDefinitions(
            final FactoryServiceContext<StreamProfileInternal> factoryServiceContext, 
            final FactoryInternal factory, final int filter)
    {
        return new AttributeDefinition[]{};
    }

    @Override
    public Dictionary<String, Object> getAdditionalFactoryRegistrationProps(final FactoryInternal factory)
    {
        final Dictionary<String, Object> streamProfileFactoryProps = new Hashtable<>();
        streamProfileFactoryProps.put(FactoryDescriptor.FACTORY_TYPE_SERVICE_PROPERTY, StreamProfileFactory.class);
        return streamProfileFactoryProps;
        
    }

    @Override
    public void beforeAddFactory(final FactoryServiceContext<StreamProfileInternal> factoryServiceContext,
            final FactoryInternal factory) throws FactoryException
    {
        // no action needed
        m_ConfigListener.registerConfigListener(factory.getPid());
    }

    @Override
    public void onRemoveFactory(final FactoryServiceContext<StreamProfileInternal> factoryServiceContext,
            final FactoryInternal factory)
    {
        // TD: Determine if StreamProfile objects needs to be disabled.
        m_ConfigListener.unregisterConfigListener(factory.getPid());
    }

    @Override
    public FactoryObjectDataManager getDataManager()
    {
        return m_StreamProfileFactoryData;
    }

    @Override
    public FactoryRegistryCallback<StreamProfileInternal> createCallback(
            final FactoryServiceContext<StreamProfileInternal> factoryServiceContext)
    {
        return new StreamProfileRegistryCallback();
    }

}
