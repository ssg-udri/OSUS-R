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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.datastream.DataStreamService;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.model.config.CreatePolicyEnum;
import mil.dod.th.model.config.FactoryObjectConfig;
import mil.dod.th.model.config.FactoryTypeEnum;
import mil.dod.th.ose.config.loading.FactoryObjectLoader;
import mil.dod.th.ose.config.loading.api.ConfigLoadingConstants;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Loads {@link mil.dod.th.core.factory.FactoryObject}s based on a provided configuration.
 */
@Component
public class FactoryObjectLoaderImpl implements FactoryObjectLoader
{
    /**
     * List of service trackers used for each factory object configuration.
     */
    private List<ServiceTracker<FactoryDescriptor, FactoryDescriptor>> m_ServiceTrackerList;

    /**
     * Utility used to log messages.
     */
    private LoggingService m_Logging;

    /**
     * Reference to the asset directory service.
     */
    private AssetDirectoryService m_AssetDirectoryService;

    /**
     * Reference to custom comms service.
     */
    private CustomCommsService m_CustomCommsService;
    
    /**
     * Reference to datastream service.
     */
    private DataStreamService m_DataStreamService;

    /**
     * Reference to this bundle's OSGi context.
     */
    private BundleContext m_BundleContext;

    /**
     * Reference to the event admin service.
     */
    private EventAdmin m_EventAdmin;

    /**
     * Used to track the registration of the {@link EventHandler}.
     */
    private ServiceRegistration<EventHandler> m_EventHandlerReg;

    /**
     * The number of service trackers created based on the factory objects being processed.
     */
    private int m_NumServiceTrackersCreated;

    /**
     * List of stream profile configs used when the data stream service is not available during initial processing.
     */
    private List<FactoryObjectConfig> m_PendingStreamProfiles = new ArrayList<>();

    /**
     * Binds the logging service for logging messages.
     * 
     * @param logging
     *            Logging service object
     */
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }

    /**
     * Binds the event admin service.
     * 
     * @param eventAdmin
     *            Service for sending OSGi events
     */
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Set the asset directory service to use.
     * 
     * @param assetDirectoryService
     *            the m_AssetDirectoryService to set
     */
    @Reference
    public void setAssetDirectoryService(final AssetDirectoryService assetDirectoryService)
    {
        m_AssetDirectoryService = assetDirectoryService;
    }

    /**
     * Sets the custom comms service to be used.
     * 
     * @param customCommsService
     *            the m_CustomCommsService to set
     */
    @Reference
    public void setCustomCommsService(final CustomCommsService customCommsService)
    {
        m_CustomCommsService = customCommsService;
    }
    
    /**
     * Sets the datastream service to be used.
     * @param dataStreamService
     *      the m_DataStreamService to set
     */
    @Reference(optional = true, dynamic = true)
    public synchronized void setDataStreamService(final DataStreamService dataStreamService)
    {
        m_DataStreamService = dataStreamService;

        for (FactoryObjectConfig config : m_PendingStreamProfiles)
        {
            createTracker(config);
        }

        m_PendingStreamProfiles.clear();
    }
    
    /**
     * Unbind method to unset the datastream service.
     * 
     * @param dataStreamService
     *      the datastream service reference
     */
    @SuppressWarnings("PMD.NullAssignment")
    public synchronized void unsetDataStreamService(final DataStreamService dataStreamService)
    {
        m_DataStreamService = null;
    }

    /**
     * The service component activation method.
     * 
     * @param context
     *            context for the bundle
     */
    @Activate
    public void activate(final BundleContext context)
    {
        m_BundleContext = context;
        m_ServiceTrackerList = new ArrayList<ServiceTracker<FactoryDescriptor, FactoryDescriptor>>();

        // Create an event handler to wait for all factory objects to be loaded
        final EventHandler eventHandler = new EventHandler()
        {
            private int m_FactoryObjCount;

            @Override
            public void handleEvent(final Event event)
            {
                m_FactoryObjCount++;
                if (m_FactoryObjCount == m_NumServiceTrackersCreated)
                {
                    sendConfigFactoryObjCompleteEvent();
                }
            }
        };

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(EventConstants.EVENT_TOPIC, ConfigLoadingConstants.TOPIC_CONFIG_FACTORY_OBJ_LOADED_EVENT);
        m_EventHandlerReg = m_BundleContext.registerService(EventHandler.class, eventHandler, props);
    }

    /**
     * The service component deactivation method.
     * 
     * Any services being tracked by this component are removed.
     */
    @Deactivate
    public void deactivate()
    {
        for (ServiceTracker<FactoryDescriptor, FactoryDescriptor> tracker : m_ServiceTrackerList)
        {
            tracker.close();
        }

        m_EventHandlerReg.unregister();
    }

    @Override
    public synchronized void process(final List<FactoryObjectConfig> factoryObjectConfigs, final boolean firstRun)
    {
        // Determine how many service trackers will be created. This is needed to avoid race conditions that occur when
        // receiving factory object loaded events before all service trackers have been created.
        for (FactoryObjectConfig config : factoryObjectConfigs)
        {
            if ((config.getCreatePolicy() == CreatePolicyEnum.FIRST_RUN && firstRun)
                    || config.getCreatePolicy() == CreatePolicyEnum.IF_MISSING)
            {
                m_NumServiceTrackersCreated++;
            }
        }

        for (FactoryObjectConfig config : factoryObjectConfigs)
        {
            try
            {
                if ((config.getCreatePolicy() == CreatePolicyEnum.FIRST_RUN && firstRun)
                        || config.getCreatePolicy() == CreatePolicyEnum.IF_MISSING)
                {
                    createTracker(config);
                }
            }
            catch (final Exception ex)
            {
                m_Logging.error(ex, "Invalid factory object config: %s", config);
            }
        }

        if (m_ServiceTrackerList.isEmpty() && m_PendingStreamProfiles.isEmpty())
        {
            sendConfigFactoryObjCompleteEvent();
        }
    }

    /**
     * Send event for notification of all factory objects being loaded.
     */
    private void sendConfigFactoryObjCompleteEvent()
    {
        final Event factoryObjCompleteEvt = new Event(ConfigLoadingConstants.TOPIC_CONFIG_FACTORY_OBJS_COMPLETE_EVENT,
                new HashMap<String, Object>());
        m_EventAdmin.postEvent(factoryObjCompleteEvt);
        m_Logging.info("Factory object configuration loading completed.");
    }

    /**
     * Creates the service tracker customizer for a {@link mil.dod.th.model.config.FactoryObjectConfig}.
     * 
     * @param config
     *            Factory object configuration
     * @throws IllegalArgumentException
     *            If an unknown factory type or invalid product type is provided in the factory object config
     */
    private void createTracker(final FactoryObjectConfig config)
            throws IllegalArgumentException
    {
        final FactoryTypeEnum type = config.getFactoryType();
        final ServiceTrackerCustomizer<FactoryDescriptor, FactoryDescriptor> customizer;
        switch (type)
        {
            case ASSET:
                customizer = new AssetTrackerCustomizer(config, m_AssetDirectoryService, m_Logging, m_EventAdmin);
                break;
            case LINK_LAYER:
            case PHYSICAL_LINK:
            case TRANSPORT_LAYER:
                customizer = new CustomCommsTrackerCustomizer(config, m_CustomCommsService, m_Logging, m_EventAdmin);
                break;
            case STREAM_PROFILE:
                if (m_DataStreamService == null)
                {
                    m_PendingStreamProfiles.add(config);
                    return;
                }
                else
                {
                    customizer = new StreamProfileTrackerCustomizer(config, 
                            m_DataStreamService, m_Logging, m_EventAdmin);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown configuration factory type: " + type);
        }

        final Filter filter;
        if (type == FactoryTypeEnum.PHYSICAL_LINK)
        {
            filter = filterFromFilterString(getPhysicalLinkFilterString(config.getPhysicalLinkType()));
        }
        else
        {
            filter = filterFromKeyValuePair(FactoryDescriptor.PRODUCT_TYPE_SERVICE_PROPERTY, config.getProductType());
        }

        final ServiceTracker<FactoryDescriptor, FactoryDescriptor> tracker = 
                new ServiceTracker<FactoryDescriptor, FactoryDescriptor>(m_BundleContext, filter, customizer);
        tracker.open();

        m_ServiceTrackerList.add(tracker);
    }
    
    /**
     * Method to return a OSGi filter for a given physical link type.
     * @param type
     *  the {@link PhysicalLinkTypeEnum} 
     * @return
     *  the filter for the given type
     */
    private String getPhysicalLinkFilterString(final PhysicalLinkTypeEnum type)
    {
        switch (type)
        {
            case GPIO:
                return PhysicalLink.FILTER_GPIO;
            case I_2_C:
                return PhysicalLink.FILTER_I2C;
            case SERIAL_PORT:
                return PhysicalLink.FILTER_SERIAL_PORT;
            case SPI:
                return PhysicalLink.FILTER_SPI;
            default:
                throw new IllegalArgumentException(String.
                        format("Cannot create filter for link type %s", type));
        }
    }
    
    /**
     * Method to generate an OSGi filter from a key-value pair.
     * 
     * @param filterKey
     *  the key value of the filter that will be used to correctly bind the desired service reference instance
     * @param filterValue
     *  the value that is to be used for the filter
     * @return
     *  the {@link Filter} object that has the given key-value pair as its filter
     */
    private Filter filterFromKeyValuePair(final String filterKey, final String filterValue)
    {
        final String serviceMatch = String.format("(%s=%s)", filterKey, filterValue);
        
        return filterFromFilterString(serviceMatch);
    }
    
    /**
     * Method to create an OSGi filter used when creating a service tracker instance.
     * @param serviceMatch
     *  the filter string that is to be used when creating the OSGi {@link Filter}
     * @return
     *  the {@link Filter} object that has the given key-value pair as its filter
     */
    private Filter filterFromFilterString(final String serviceMatch)
    {
        final Filter filter;
        try
        {
            filter = m_BundleContext.createFilter(serviceMatch);
        }
        catch (final InvalidSyntaxException ex)
        {
            throw new IllegalArgumentException(String.format("Given filter [ %s ] cannot be parsed",
                     serviceMatch), ex);
        }
        
        return filter;
    }
}
