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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.xml.bind.UnmarshalException;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Modified;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;

import com.google.common.base.Objects;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.xml.XmlUnmarshalService;
import mil.dod.th.model.config.Configurations;
import mil.dod.th.ose.config.loading.AddressLoader;
import mil.dod.th.ose.config.loading.FactoryObjectLoader;
import mil.dod.th.ose.config.loading.OSGiConfigLoader;
import mil.dod.th.ose.config.loading.RemoteChannelLoader;
import mil.dod.th.ose.config.loading.RemoteEventLoader;
import mil.dod.th.ose.config.loading.api.ConfigLoadingConstants;
import mil.dod.th.ose.shared.SystemConfigurationConstants;
import mil.dod.th.ose.utils.FileService;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;


/**
 * Class which handles reading from a configuration file and registers the objects to be created or updated.
 * 
 * @author nickmarcucci
 */
@Component(designate = ConfigurationMgrConfig.class, configurationPolicy = ConfigurationPolicy.optional)
public class ConfigurationMgr
{
    /**
     * Path to recommended configuration file.
     */
    private static final String CONFIGURATION_FILE = "configs.xml";
    
    /**
     * The unmarshaller that is used to read in the configuration file.
     */
    private XmlUnmarshalService m_Unmarshaller;
    
    /**
     * The configuration admin that is used to retrieve configuration properties.
     */
    private ConfigurationAdmin m_ConfigAdmin;

    /**
     * The event admin service used to send an event when processing of configs.xml has been completed.
     */
    private EventAdmin m_EventAdmin;

    /**
     * The factory object loader service that is to be used to create configurations.
     */
    private FactoryObjectLoader m_FactoryObjectLoader;
    
    /**
     * The OSGi configuration loader service that is to be used to create OSGi configurations.
     */
    private OSGiConfigLoader m_OSGiConfigLoader; 
    
    /**
     * The address loader service that is to be used to create address configurations.
     */
    private AddressLoader m_AddressLoader;
    
    /**
     * The file service that is to be used to perform file operations.
     */
    private FileService m_FileService;
    
    /**
     * Logging service used to log information.
     */
    private LoggingService m_Log;
    
    /**
     * The remote event loader service that is used to register remote events from configurations.
     */
    private RemoteEventLoader m_RemoteEventLoader;
    
    /**
     * The remote channel loader service that is used to load socket and transport channels from configurations. 
     */
    private RemoteChannelLoader m_RemoteChannelLoader;

    /**
     * Flag set during activation that indicates whether startup is occurring for the first time.
     */
    private boolean m_IsFirstRun;

    /**
     * Method used to assign the {@link XmlUnmarshalService} service to use.
     * @param unmarshaller
     *  the unmarshaller service to use
     */
    @Reference
    public void setXmlUnmarshaller(final XmlUnmarshalService unmarshaller)
    {
        m_Unmarshaller = unmarshaller;
    }
    
    /**
     * Method used to assign the {@link ConfigurationAdmin} service to use.
     * @param configAdmin
     *  the configuration admin service to be used
     */
    @Reference
    public void setConfigAdmin(final ConfigurationAdmin configAdmin)
    {
        m_ConfigAdmin = configAdmin;
    }
    
    /**
     * Method used to assign the {@link EventAdmin} service to use.
     * @param eventAdmin
     *  the event admin service to be used
     */
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Method used to assign the {@link FactoryObjectLoader} service to use.
     * @param factoryLoader
     *  the factory object configuration loader that is to be used
     */
    @Reference
    public void setFactoryObjectLoader(final FactoryObjectLoader factoryLoader)
    {
        m_FactoryObjectLoader = factoryLoader;
    }
    
    /**
     * Method used to assign the {@link OSGiConfigLoader} service to use.
     * @param osgiLoader
     *  the osgi configuration loader that is to be used
     */
    @Reference
    public void setOSGiConfigLoader(final OSGiConfigLoader osgiLoader)
    {
        m_OSGiConfigLoader = osgiLoader;
    }
    
    /**
     * Method used to assign the {@link AddressLoader} service to use.
     * @param addrLoader
     *  the address loader service that is to be used
     */
    @Reference
    public void setAddressLoader(final AddressLoader addrLoader)
    {
        m_AddressLoader = addrLoader;
    }
    
    /**
     * Bind the file service.
     * 
     * @param fileService
     *     the service used to access files
     */
    @Reference
    public void setFileService(final FileService fileService)
    {
        m_FileService = fileService;
    }
    
    /**
     * Method used to assign the {@link LoggingService} service to use.
     * @param logger
     *  the logging service to use
     */
    @Reference
    public void setLoggingService(final LoggingService logger)
    {
        m_Log = logger;
    }
    
    @Reference
    public void setRemoteEventLoader(final RemoteEventLoader remoteEventLoader)
    {
        m_RemoteEventLoader = remoteEventLoader;
    }
    
    /**
     * Bind the remote channel loader.
     * 
     * @param remoteChannelLoader
     *      the remote channel loader service that is to be used.
     */
    @Reference
    public void setRemoteChannelLoader(final RemoteChannelLoader remoteChannelLoader)
    {
        m_RemoteChannelLoader = remoteChannelLoader;
    }
    
    /**
     * Method called on activation of this component.
     * @param context
     *  this bundle's context
     * @throws UnmarshalException
     *  occurs if the file provided cannot be parsed into a JAXB object
     * @throws IOException 
     *  occurs if an error occurs while attempting to set configuration properties
     */
    @Activate
    public void activate(final BundleContext context) throws UnmarshalException, IOException
    {
        final String configDirectory = context.getProperty(SystemConfigurationConstants.DATA_DIR_PROPERTY);
        final File configParentFile = new File(configDirectory, "conf");
        final File configFile = new File(configParentFile, CONFIGURATION_FILE);
        final URL defaultConfigUrl = context.getBundle().getEntry(CONFIGURATION_FILE);

        final Configurations configurations;
        try
        {
            configurations = loadConfigurationFile(configFile, defaultConfigUrl);
        }
        catch (final UnmarshalException | IOException e)
        {
            m_Log.error(e, "Unable to load configuration file [%s]", configFile.getAbsolutePath());
            return;
        }

        if (configurations != null)
        {
            processConfigurations(configurations);
        }
    }

    /**
     * Method called when configuration properties are updated.
     * @param properties
     *      the properties that have been updated, argument required for method to be accepted by OSGi
     */
    @Modified
    public void modified(final Map<String, Object> properties)
    {
        //needed so that the activate method is not invoked again after setting configuration properties.
    }
    
    /**
     * Load the configuration file.
     * @param configFile
     *      reference to the configuration file
     * @param defaultConfigUrl
     *      reference to the default configuration file (from bundle)
     * @return
     *      loaded configurations object
     * @throws UnmarshalException
     *      if the configuration data cannot be unmarshalled
     * @throws MalformedURLException
     *      if the configuration file is not a valid URL
     * @throws IOException
     *      if unable to update the components configuration
     */
    private Configurations loadConfigurationFile(final File configFile, final URL defaultConfigUrl)
            throws UnmarshalException, MalformedURLException, IOException
    {
        Configurations configurations = null;
        
        final Configuration configuration = 
                m_ConfigAdmin.getConfiguration(ConfigurationMgr.class.getName());
        final Dictionary<String, Object> properties = Objects.firstNonNull(configuration.getProperties(), 
                new Hashtable<String, Object>());
        
        if (m_FileService.doesFileExist(configFile))
        {
            final ConfigurationMgrConfig mgrConfig =
                Configurable.createConfigurable(ConfigurationMgrConfig.class, properties);

            m_IsFirstRun = mgrConfig.isFirstRun();

            configurations = m_Unmarshaller.getXmlObject(Configurations.class, configFile.toURI().toURL());

            //update the property
            if (m_IsFirstRun)
            {
                properties.put(ConfigurationMgrConfig.FIRST_RUN_PROPERTY, false);
                configuration.update(properties);
            }
        }
        else
        {
            final InputStream inputStream = defaultConfigUrl.openConnection().getInputStream();
            final BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            final FileWriter out = new FileWriter(configFile);

            String inputLine;
            while ((inputLine = in.readLine()) != null)
            {
                out.write(inputLine);
                out.write(System.lineSeparator());
            }

            in.close();
            out.close();

            m_Log.info("%n"
                    + "       ########%n"
                    + "           Created %s, edit the file and restart controller%n"
                    + "       ########",
                    configFile.getPath());
        }

        return configurations;
    }

    /**
     * Process each of the configurations found during activation, if any.
     * 
     * @param configurations
     *      config to process
     */
    private void processConfigurations(final Configurations configurations)
    {
        final int pidSize = configurations.getOsgiConfigs().size();
        final int factConfigSize = configurations.getFactoryObjects().size();
        final int addrConfigsSize = configurations.getAddresses().size();
        final int remoteEventConfigsSize = configurations.getEventRegs().size();
        final int socketChannelConfigSize = configurations.getSocketChannels().size();
        final int transportChannelConfigSize = configurations.getTransportChannels().size();

        m_Log.info("Initializing configuration setup with firstRun property set to %s.", m_IsFirstRun);

        m_Log.info("%nProcessing %d Factory Configurations;%n"
                + "           %d Address Configurations;%n"
                + "           %d PID Configurations;%n"
                + "           %d Remote Event Configurations;%n"
                + "           %d Socket Channel Configurations;%n"
                + "           %d Transport Channel Configurations;", factConfigSize, addrConfigsSize, pidSize, 
                remoteEventConfigsSize, socketChannelConfigSize, transportChannelConfigSize); 

        m_OSGiConfigLoader.process(configurations.getOsgiConfigs(), m_IsFirstRun);
        m_FactoryObjectLoader.process(configurations.getFactoryObjects(), m_IsFirstRun);
        m_AddressLoader.process(configurations.getAddresses(), m_IsFirstRun);
        
        //these are always run, first run property means nothing
        m_RemoteEventLoader.process(configurations.getEventRegs());
        
        //Remote channels are always created. Remote channels created through the configuration loader 
        //are not persisted.
        m_RemoteChannelLoader.processSocketChannels(configurations.getSocketChannels());
        m_RemoteChannelLoader.processTransportChannels(configurations.getTransportChannels());

        sendConfigurationsProcessedEvent();
    }

    /**
     * Send event for notification of configurations being loaded.
     */
    private void sendConfigurationsProcessedEvent()
    {
        final String topic = ConfigLoadingConstants.TOPIC_CONFIG_PROCESSING_COMPLETE_EVENT;
        final Event configCompleteEvt = new Event(topic, new HashMap<String, Object>());
        m_EventAdmin.postEvent(configCompleteEvt);
        m_Log.info("Configurations have been processed from " + CONFIGURATION_FILE + ".");
    }
}
