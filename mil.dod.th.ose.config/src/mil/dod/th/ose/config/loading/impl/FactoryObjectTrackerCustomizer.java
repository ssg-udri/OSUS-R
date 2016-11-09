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
package mil.dod.th.ose.config.loading.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.types.StringMapEntry;
import mil.dod.th.model.config.AddressConfig;
import mil.dod.th.model.config.FactoryObjectConfig;
import mil.dod.th.ose.config.loading.api.ConfigLoadingConstants;
import mil.dod.th.ose.utils.ConfigurationUtils;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Base class for factory service trackers that implement the {@link org.osgi.util.tracker.ServiceTrackerCustomizer}
 * interface.
 */
public abstract class FactoryObjectTrackerCustomizer implements
        ServiceTrackerCustomizer<FactoryDescriptor, FactoryDescriptor>
{
    /**
     * Reference to the address configuration associated with this tracker.
     */
    private final AddressConfig m_AddressConfig;

    /**
     * Reference to the object configuration associated with this tracker.
     */
    private final FactoryObjectConfig m_ObjectConfig;

    /**
     * Service for logging events.
     */
    private LoggingService m_Logging;

    /**
     * Service for sending event admin events.
     */
    private EventAdmin m_EventAdmin;

    /**
     * Initialize the tracker customizer with the associated {@link mil.dod.th.model.config.AddressConfig}.
     * 
     * @param addressConfig
     *            Address configuration to track
     * @param loggingService
     *            Service for logging events
     * @param eventAdmin
     *            Service for sending OSGi events
     */
    public FactoryObjectTrackerCustomizer(final AddressConfig addressConfig,
            final LoggingService loggingService, final EventAdmin eventAdmin)
    {
        m_AddressConfig = addressConfig;
        m_ObjectConfig = null;
        m_Logging = loggingService;
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Initialize the tracker customizer with the associated {@link mil.dod.th.model.config.FactoryObjectConfig}.
     * 
     * @param objectConfig
     *            Factory object configuration to track
     * @param loggingService
     *            Service for logging events
     * @param eventAdmin
     *            Service for sending OSGi events
     */
    public FactoryObjectTrackerCustomizer(final FactoryObjectConfig objectConfig,
            final LoggingService loggingService, final EventAdmin eventAdmin)
    {
        m_AddressConfig = null;
        m_ObjectConfig = objectConfig;
        m_Logging = loggingService;
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Each implementor provides this method to handle creation of objects for the given factory.
     * 
     * @param factory
     *      factory instance that is now available
     * @throws FactoryException
     *      if there is an exception while creating the configured object
     */
    abstract void addingFactoryDescriptor(FactoryDescriptor factory) throws FactoryException;
    
    @Override
    public FactoryDescriptor addingService(final ServiceReference<FactoryDescriptor> reference)
    {
        final BundleContext context;
        final FactoryDescriptor factory;
        try
        {
            context = reference.getBundle().getBundleContext();
            factory = context.getService(reference);
        }
        catch (final Exception ex)
        {
            m_Logging.log(LogService.LOG_WARNING, ex, "Exception while attempting to acquire factory service for [%s]",
                    reference);
            return null;
        }

        try
        {
            addingFactoryDescriptor(factory);
        }
        catch (final Exception ex)
        {
            // catch all exceptions as this could be called from a component activation method
            m_Logging.log(LogService.LOG_WARNING, ex, "Exception while attempting to load configurations for [%s]", 
                    factory.getProductType());
        }
        finally
        {
            context.ungetService(reference);

            sendLoadedEvent(factory);
        }

        return null;
    }

    @Override
    public void modifiedService(final ServiceReference<FactoryDescriptor> reference, // NOPMD
            final FactoryDescriptor service) // Empty method in abstract class to simplify implementation
    {
        // No action required for a modified service
    }

    @Override
    public void removedService(final ServiceReference<FactoryDescriptor> reference, // NOPMD
            final FactoryDescriptor service) // Empty method in abstract class to simplify implementation
    {
        // No action required for a removed service
    }

    /**
     * Translates a list of {@link mil.dod.th.core.types.StringMapEntry} objects into a dictionary of properties
     * supported by all factory objects.
     * 
     * @param properties
     *            Properties provided by {@link mil.dod.th.model.config.FactoryObjectConfig}
     * @param factory
     *            Factory associated with the properties
     * @return Property values translated to the proper object types
     * @throws IllegalArgumentException
     *             When a property key is not found in the attribute definitions
     */
    protected Map<String, Object> translateStringMap(final List<StringMapEntry> properties,
            final FactoryDescriptor factory) throws IllegalArgumentException
    {
        final AttributeDefinition[] attrs = factory.getAttributeDefinitions(ObjectClassDefinition.ALL);
    
        final Map<String, Object> objectProperties = new HashMap<>();

        for (StringMapEntry prop : properties)
        {
            final String key = prop.getKey();
            objectProperties.put(key,
                    ConfigurationUtils.parseStringByType(getAttributeType(attrs, key), prop.getValue()));
        }

        return objectProperties;
    }

    /**
     * Returns the address configuration associated with this tracker.
     * 
     * @return the m_AddressConfig configuration object
     */
    protected AddressConfig getAddressConfig()
    {
        return m_AddressConfig;
    }

    /**
     * Returns the factory object configuration associated with this tracker.
     * 
     * @return the m_ObjectConfig factory configuration object
     */
    protected FactoryObjectConfig getObjectConfig()
    {
        return m_ObjectConfig;
    }

    /**
     * Returns a data type, defined in {@link org.osgi.service.metatype.AttributeDefinition} for the given property key.
     * 
     * @param attrs
     *            Attribute definitions for the factory being tracked
     * @param key
     *            Property key name
     * @return Data type for the given property key
     * @throws IllegalArgumentException
     *             When the key is not found in the attribute definitions
     */
    private int getAttributeType(final AttributeDefinition[] attrs, final String key) throws IllegalArgumentException
    {
        for (AttributeDefinition attr : attrs)
        {
            if (attr.getID().equals(key))
            {
                return attr.getType();
            }
        }

        throw new IllegalArgumentException("Unsupported product attribute: " + key);
    }

    /**
     * Send event for notification of the factory object being created.
     * 
     * @param factory
     *      factory instance that is now available
     */
    private void sendLoadedEvent(final FactoryDescriptor factory)
    {
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(ConfigLoadingConstants.EVENT_PROP_PRODUCT_TYPE, factory.getProductType());

        if (m_ObjectConfig == null)
        {
            props.put(ConfigLoadingConstants.EVENT_PROP_ADDR_DESC, m_AddressConfig.getAddressDescription());

            final Event addressCompleteEvt = new Event(ConfigLoadingConstants.TOPIC_CONFIG_ADDRESS_LOADED_EVENT,
                props);
            m_EventAdmin.postEvent(addressCompleteEvt);
            m_Logging.debug("Sent address loaded event for %s", m_AddressConfig.getAddressDescription());
        }
        else
        {
            props.put(ConfigLoadingConstants.EVENT_PROP_OBJ_NAME, m_ObjectConfig.getName());

            final Event factoryObjsCompleteEvt = new Event(ConfigLoadingConstants.TOPIC_CONFIG_FACTORY_OBJ_LOADED_EVENT,
                props);
            m_EventAdmin.postEvent(factoryObjsCompleteEvt);
            m_Logging.debug("Sent factory object loaded event for %s", m_ObjectConfig.getName());
        }
    }
}
