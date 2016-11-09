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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.annotation.component.*;

import com.google.common.base.Objects;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteMetatypeConstants;
import mil.dod.th.core.types.StringMapEntry;
import mil.dod.th.model.config.PidConfig;
import mil.dod.th.ose.config.loading.OSGiConfigLoader;
import mil.dod.th.ose.config.loading.api.ConfigLoadingConstants;
import mil.dod.th.ose.utils.ConfigurationUtils;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * Implementation of the {@link OSGiConfigLoader} class.
 * 
 * @author cweisenborn
 */
@Component
public class OSGiConfigLoaderImpl implements OSGiConfigLoader
{
    /**
     * Reference to the logging service used to log information and errors.
     */
    private LoggingService m_LogService;
    
    /**
     * Reference to the OSGi configuration admin service.
     */
    private ConfigurationAdmin m_ConfigAdmin;
    
    /**
     * The event admin service used to send an event when processing of OSGi configurations have been completed.
     */
    private EventAdmin m_EventAdmin;

    /**
     * Reference to the OSGi meta type service.
     */
    private MetaTypeService m_MetaTypeService;
    
    /**
     * Bundle context used to retrieve all bundles on the system.
     */
    private BundleContext m_Context;
    
    /**
     * Event handler used to receive Metatype data available events.
     */
    private MetatypeEventHandler m_MetatypeEventHandler;
    
    /**
     * List of configurations that do not have metatype data available yet.
     */
    private List<PidConfig> m_PendingConfigList = new ArrayList<>();
    
    /**
     * Flag indicating whether the initial processing of configurations has completed or not. This should be true
     * when the metatype data available event is received for pending configurations.
     */
    private Boolean m_Processed = false;
    
    /**
     * Method used by OSGi to bind the logging service.
     * 
     * @param logService
     *      The log service to be set.
     */
    @Reference
    public void setLogService(final LoggingService logService)
    {
        m_LogService = logService;
    }
    
    /**
     * Method used by OSGi to bind the configuration admin service.
     * 
     * @param configAdmin
     *      The configuration admin service to be set.
     */
    @Reference
    public void setConfigAdmin(final ConfigurationAdmin configAdmin)
    {
        m_ConfigAdmin = configAdmin;
    }
    
    /**
     * Method used to assign the {@link EventAdmin} service to use.
     * 
     * @param eventAdmin
     *      The event admin service to be used
     */
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Method used by OSGi to bind the meta type service.
     * 
     * @param metaTypeService
     *      The meta type service to be set.
     */
    @Reference
    public void setMetaTypeService(final MetaTypeService metaTypeService)
    {
        m_MetaTypeService = metaTypeService;
    }
    
    /**
     * Activate method to save off bundle context for later use and register needed event handlers.
     * 
     * @param context
     *      context for this bundle
     */
    @Activate
    public void activate(final BundleContext context)
    {
        m_Context = context;
        m_MetatypeEventHandler = new MetatypeEventHandler();
        m_MetatypeEventHandler.registerHandler();
    }

    /**
     * Deactivate method used to unregister event handlers.
     */
    @Deactivate
    public void deactivate()
    {
        m_MetatypeEventHandler.unregisterHandler();
    }

    @Override
    public void process(final List<PidConfig> pidConfigs, final boolean firstRun)
    {
        if (firstRun && !pidConfigs.isEmpty())
        {
            synchronized (m_Processed)
            {
                for (PidConfig pidConfig : pidConfigs)
                {
                    processPidConfig(pidConfig);
                }

                m_Processed = true;

                m_LogService.debug("OSGi Configuration Pending list size=%d", m_PendingConfigList.size());
                checkForComplete();
            }
        }
        else
        {
            sendConfigurationsProcessedEvent();
        }
    }
    
    /**
     * Create the given configuration in OSGi ConfigAdmin. If metatype data is not yet available, queue it in the
     * pending list to be created when the associated bundle is installed.
     * 
     * @param pidConfig
     *      Configuration to be created or queued for later creation
     */
    private void processPidConfig(final PidConfig pidConfig)
    {
        if (pidConfig.isSetFactoryPid())
        {
            try
            {
                handleFactoryConfig(pidConfig);
                
                m_LogService.info("Successfully created factory configuration from factory with PID: %s", 
                        pidConfig.getFactoryPid());
            }
            catch (final IllegalStateException exception)
            {
                addPendingConfig(pidConfig);
            }
            catch (final IOException | IllegalArgumentException exception)
            {
                m_LogService.error(exception, "An error occurred trying to create factory "
                        + "configuration from factory with PID: %s", pidConfig.getFactoryPid());
            }
        }
        else if (pidConfig.isSetPid())
        {
            try
            {
                handleStandardConfig(pidConfig);
                
                m_LogService.info("Successfully created configuration with PID: %s", 
                        pidConfig.getPid());
            }
            catch (final IllegalStateException exception)
            {
                addPendingConfig(pidConfig);
            }
            catch (final IOException | IllegalArgumentException exception)
            {
                m_LogService.error(exception, 
                        "An error occurred trying to create configuration with PID: %s", 
                        pidConfig.getPid());
            }
        }
    }

    /**
     * Add a configuration to the pending list if called during the initial processing of configurations.
     * 
     * @param config
     *      Configuration to be queued up
     */
    private void addPendingConfig(final PidConfig config)
    {
        final String pid = config.isSetFactoryPid() ? config.getFactoryPid() : config.getPid();
        if (m_Processed)
        {
            m_LogService.error("Pending configuration with PID %s is missing metatype info", pid);
        }
        else
        {
            m_LogService.debug("Waiting on bundle for pending configuration with PID %s", pid);
            m_PendingConfigList.add(config);
        }
    }

    /**
     * Retrieve a pending configuration for the given pid.
     * 
     * @param pid
     *      PID value to search pending list for
     * @return
     *      Matching configuration or null if PID not found
     */
    private PidConfig getPendingConfig(final String pid)
    {
        for (final ListIterator<PidConfig> iter = m_PendingConfigList.listIterator(); iter.hasNext();)
        {
            final PidConfig pidConfig = iter.next();
            if (pidConfig.getPid().equals(pid))
            {
                iter.remove();
                return pidConfig;
            }
        }
        
        return null;
    }

    /**
     * Method called when handling a standard OSGi configuration.
     * 
     * @param config
     *      PidConfig that represents the configuration properties to be set for the specified OSGi configuration.
     * @throws IOException
     *      Thrown if the properties for the configuration cannot be created or updated.
     */
    private void handleStandardConfig(final PidConfig config) throws IOException
    {
        final String pid = config.getPid();
        final Map<String, Object> configProps = 
                ConfigurationUtils.convertDictionaryPropsToMap(createPropertyMap(pid, config.getProperties()));
        
        final Configuration standardConfig = m_ConfigAdmin.getConfiguration(pid, null);
        
        // get existing properties and add on new config properties over top
        final Dictionary<String, Object> newProps = 
                Objects.firstNonNull(standardConfig.getProperties(), new Hashtable<String, Object>());
        
        for (Entry<String, Object> configEntry : configProps.entrySet())
        {
            newProps.put(configEntry.getKey(), configEntry.getValue());
        }
        
        standardConfig.update(newProps);
    }
    
    /**
     * Checks to see if configurations have been processed and no pending items remain. If true, the configurations
     * processed event is sent.
     */
    private void checkForComplete()
    {
        if (m_Processed && m_PendingConfigList.isEmpty())
        {
            sendConfigurationsProcessedEvent();
        }
    }

    /**
     * Method called when handling a factory OSGi configuration.
     * 
     * @param config
     *      PidConfig that represents the factory configuration properties to be used in a creating a new factory
     *      configuration from the specified factory.
     * @throws IOException
     *      Thrown if the factory configuration with the specified properties cannot be created.
     */
    private void handleFactoryConfig(final PidConfig config) throws IOException
    {
        final String pid = config.getFactoryPid();
        final Dictionary<String, Object> propertyMap = createPropertyMap(pid, config.getProperties());
    
        final Configuration factoryConfig = m_ConfigAdmin.createFactoryConfiguration(pid, null);
        factoryConfig.update(propertyMap);
    }
    
    /**
     * Method that creates a dictionary of properties to be used when creating or updating a configuration.
     * 
     * @param pid
     *      PID of the configuration the properties pertain to.
     * @param properties
     *      StringMapEntry that represents the properties to create a dictionary for.
     * @return
     *      The dictionary that contains the key and value pairs that represent the properties for the specified
     *      configuration.
     */
    private Dictionary<String, Object> createPropertyMap(final String pid, final List<StringMapEntry> properties)
    {
        final List<AttributeDefinition> attributes = getAttributes(pid);
        final Dictionary<String, Object> propertyMap = new Hashtable<String, Object>();
        for (StringMapEntry prop: properties)
        {
            final AttributeDefinition definition = findDefinition(prop.getKey(), attributes);
            final Object actualValue = ConfigurationUtils.parseStringByType(definition.getType(), prop.getValue());
            propertyMap.put(prop.getKey(), actualValue);
        }
        return propertyMap;
    }
    
    /**
     * Method that finds the attribute definition for the property with the specified name.
     * 
     * @param propId
     *      The id of the property to find the attribute definition for.
     * @param attributes
     *      List of attribute definitions to be searched.
     * @return
     *      The attribute definition the corresponds to the property with the specified name.
     */
    private AttributeDefinition findDefinition(final String propId, final List<AttributeDefinition> attributes)
    {
        for (AttributeDefinition definition: attributes)
        {
            if (definition.getID().equals(propId))
            {
                return definition;
            }
        }
        throw new IllegalArgumentException(String.format("Property with ID %s does not exist.", propId));
    }
    
    /**
     * Method that returns the meta type attribute definitions for the configuration with the specified PID.
     * 
     * @param pid
     *      PID of the configuration to find meta type information for.
     * @return
     *      List of attribute definitions which define the properties for a configuration with the specified PID.
     */
    private List<AttributeDefinition> getAttributes(final String pid)
    {
        for (Bundle bundle: m_Context.getBundles())
        {
            final MetaTypeInformation metaTypeInfo = m_MetaTypeService.getMetaTypeInformation(bundle);
            if (metaTypeInfo != null)
            {
                final List<String> pidList = Arrays.asList(metaTypeInfo.getPids());
                final List<String> factoryPidList = Arrays.asList(metaTypeInfo.getFactoryPids());
                if (pidList.contains(pid) || factoryPidList.contains(pid))
                {
                    final ObjectClassDefinition ocd = metaTypeInfo.getObjectClassDefinition(pid, null);
                    final AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
                    return Arrays.asList(Objects.firstNonNull(ads, new AttributeDefinition[] {}));
                }
            }
        }
        throw new IllegalStateException(String.format("No meta type information could be found for configuration "
                + "with PID: %s", pid));
    }

    /**
     * Send event for notification of OSGi configurations being loaded.
     */
    private void sendConfigurationsProcessedEvent()
    {
        final String topic = ConfigLoadingConstants.TOPIC_CONFIG_OSGI_COMPLETE_EVENT;
        final Event configCompleteEvt = new Event(topic, new HashMap<String, Object>());
        m_EventAdmin.postEvent(configCompleteEvt);
        m_LogService.info("OSGi configurations loading completed.");
    }

    /**
     * Metatype data available event handler.
     */
    class MetatypeEventHandler implements EventHandler
    {
        /**
         * Service registration for the event handler.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;
        
        /**
         * Registers the event handler to listen for metatype information available events.
         */
        public void registerHandler()
        {
            // register to listen for bundles that have their metatype information registered
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, RemoteMetatypeConstants.TOPIC_METATYPE_INFORMATION_AVAILABLE);

            m_Registration = m_Context.registerService(EventHandler.class, this, props);
        }
        
        @Override
        public void handleEvent(final Event event)
        {
            final String topic = event.getTopic();
            if (topic.contains(RemoteMetatypeConstants.TOPIC_METATYPE_INFORMATION_AVAILABLE))
            {
                synchronized (m_Processed)
                {
                    if (m_Processed)
                    {
                        @SuppressWarnings("unchecked")
                        final List<String> pids =
                            (List<String>)event.getProperty(RemoteMetatypeConstants.EVENT_PROP_PIDS);
                        for (String pid : pids)
                        {
                            final PidConfig config = getPendingConfig(pid);
                            if (config != null)
                            {
                                m_LogService.debug("Event Pending config PID %s", pid);

                                processPidConfig(config);

                                m_LogService.debug("Event Pending list size=%d", m_PendingConfigList.size());

                                checkForComplete();
                            }
                        }
                    }
                }
            }
        }
        
        /**
         * Unregister the event handler.
         */
        public void unregisterHandler()
        {
            m_Registration.unregister();
        }
    }
}
