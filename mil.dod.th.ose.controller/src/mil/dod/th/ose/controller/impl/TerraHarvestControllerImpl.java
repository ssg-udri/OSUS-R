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
package mil.dod.th.ose.controller.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.UnmarshalException;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import com.google.common.base.Objects;

import mil.dod.th.core.controller.TerraHarvestController;
import mil.dod.th.core.controller.TerraHarvestControllerProxy;
import mil.dod.th.core.controller.capability.ControllerCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.system.TerraHarvestSystem;
import mil.dod.th.core.xml.XmlUnmarshalService;
import mil.dod.th.ose.controller.api.ThoseVersionProvider;
import mil.dod.th.ose.shared.SystemConfigurationConstants;
import mil.dod.th.ose.utils.FileService;
import mil.dod.th.ose.utils.numbers.Integers;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * 
 * Generic Terra Harvest Controller Component that has the generic Terra Harvest Controller specific methods.
 * 
 * @author frenchpd
 *
 */
@Component
public class TerraHarvestControllerImpl implements TerraHarvestController, TerraHarvestSystem
{
    /** String to store the key for the name property. */
    private static final String NAME_KEY = "name";

    /** String that holds the key for the id property. */
    private static final String ID_KEY = "id";
    
    /** String that holds the key for the controller operation property.*/
    private static final String OPERATION_MODE_KEY = "operation.mode";
    
    /** Name of the properties file. */
    private static final String PROP_FILE_NAME = "th.system.properties";
    
    /**
     * Version provider service.
     */
    private ThoseVersionProvider m_VersionProvider;

     /**
     * Reference to the file service used to perform operations of 
     * files.
     */
    private FileService m_FileService;
    
    /**
     * Properties map for the Terra Harvest System.
     */
    private Properties m_Prop = new Properties();
        
    /**
     * File to hold Terra Harvest (software) System properties.     
     */
    private File m_PropFile;
    
    /**
     * Reference to the event admin service used to post events.
     */
    private EventAdmin m_EventAdmin;
    
    /**
     * The XML unmarshal service.
     */
    private XmlUnmarshalService m_XMLUnmarshalService;

    /**
     * Cached value of the capabilities read in during activation.
     */
    private ControllerCapabilities m_Capabilities;

    /**
     * Current ID of the controller. -1 is the default to say that ID has not been set, a value of 0 is a valid ID.
     */
    private int m_Id = -1; 

    /**
     * Current name of the controller.
     */
    private String m_Name;

    /**
     * Current operation mode of the controller.
     */
    private OperationMode m_OperationMode;

    /**
     * Proxy class for custom controller behavior.
     */
    private TerraHarvestControllerProxy m_Proxy;

    /**
     * Service for logging events.
     */
    private LoggingService m_Logging;
    
    /**
     * Sets the event admin service to be used for posting events.
     * 
     * @param eventAdmin
     *          The {@link EventAdmin} service to be used.
     */
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Sets the file service to be used for posting events.
     * @param fileService
     *          The file service to be used.
     */
    @Reference
    public void setFileService(final FileService fileService)
    {
        m_FileService = fileService;
    }
    
    /**
     * Bind the service for get version info.
     * 
     * @param versionProvider
     *      version provider server
     */
    @Reference
    public void setThoseVersionProvider(final ThoseVersionProvider versionProvider)
    {
        m_VersionProvider = versionProvider;
    }
    
    /**
     * Binds the XML unmarshal service.
     * @param xmlUS
     *            the XML unmarshal service
     */
    @Reference
    public void setXMLUnmarshalService(final XmlUnmarshalService xmlUS)
    {
        m_XMLUnmarshalService = xmlUS;
    }
    
    /**
     * Bind the proxy class for custom behavior.
     * 
     * @param proxy
     *      proxy object for the specific controller
     */
    @Reference(optional = true, dynamic = false)
    public void setProxy(final TerraHarvestControllerProxy proxy)
    {
        m_Proxy = proxy;
    }
    
    /**
     * Unbind the proxy class for custom behavior.
     * 
     * @param proxy
     *      proxy object for the specific controller, argument required by OSGi to match with binding method
     */
    public void unsetProxy(final TerraHarvestControllerProxy proxy)
    {
        m_Proxy = null;
    }
    
    /**
     * Bind the service.
     * 
     * @param logging
     *      service to bind
     */
    @Reference
    public void setLogging(final LoggingService logging)
    {
        m_Logging = logging;
    }

    /**
     * This is the activation method for the TerraHarvestSystemImplementation.
     * This method loads the properties from the file and into memory. This method 
     * must be called by any class that overrides this class as the method will not be 
     * called otherwise.
     * 
     * @param context
     *      bundle that contains this component
     * @throws UnmarshalException
     *      if unable to read in the capabilities XML file 
     */
    @Activate
    public void activate(final BundleContext context) throws UnmarshalException
    {
        loadProperties(context);
        
        final ServiceReference<TerraHarvestControllerProxy> proxyServiceRef = 
                context.getServiceReference(TerraHarvestControllerProxy.class);
        final URL capabilitiesXmlUrl;
        // get capabilities XML from the bundle providing the proxy service if there is one, otherwise, use base
        // capabilities provided by the THOSE controller bundle
        if (proxyServiceRef == null)
        {
            capabilitiesXmlUrl = m_XMLUnmarshalService.getXmlResource(context.getBundle(), 
                    FactoryDescriptor.CAPABILITIES_XML_FOLDER_NAME, getClass().getName());
        }
        else
        {
            capabilitiesXmlUrl = m_XMLUnmarshalService.getXmlResource(proxyServiceRef.getBundle(), 
                    FactoryDescriptor.CAPABILITIES_XML_FOLDER_NAME, m_Proxy.getClass().getName());
        }
        m_Capabilities = m_XMLUnmarshalService.getXmlObject(ControllerCapabilities.class, capabilitiesXmlUrl);
    }

    @Override
    public String getName()
    {
        if (m_Proxy != null && Objects.firstNonNull(m_Capabilities.isNameOverridden(), false))
        {
            return m_Proxy.getName();
        }
        
        return m_Name;        
    }

    @Override
    public void setName(final String name)
    {
        if (m_Proxy != null && Objects.firstNonNull(m_Capabilities.isNameOverridden(), false))
        {
            m_Proxy.setName(name);
        }
        else
        {
            m_Prop.setProperty(NAME_KEY, name);
            try (FileOutputStream outputStream = m_FileService.createFileOutputStream(m_PropFile))
            {
                m_Prop.store(outputStream, "Terra Harvest Controller Properties, last property edit was 'name'");
            }
            catch (final IOException ex)
            {
                m_Logging.error(ex, "Unable to write 'name' to system file");
            } 
            
            m_Name = name;
            m_Logging.info("Controller's new name is %s", name);
        }
    }

    @Override
    public int getId() //NOPMD the method name is descriptive of what is being set, which is an ID
    {
        if (m_Proxy != null && Objects.firstNonNull(m_Capabilities.isIdOverridden(), false))
        {
            return m_Proxy.getId();
        }
        
        return m_Id;
    }

    @Override
    public void setId(final int id) //NOPMD the method name is descriptive of what is being set, which is an ID
    {
        if (m_Proxy != null && Objects.firstNonNull(m_Capabilities.isIdOverridden(), false))
        {
            m_Proxy.setId(id);
        }
        else
        {
            final String identification = String.format("0x%08x", id);
            m_Prop.setProperty(ID_KEY, identification);
            try (FileOutputStream outputStream = m_FileService.createFileOutputStream(m_PropFile))
            {
                m_Prop.store(outputStream, "Terra Harvest Controller Properties, last property edit was 'id'");
            }
            catch (final IOException ex)
            {
                m_Logging.error(ex, "Unable to write 'id' to system file");
            }
            
            m_Id = id;
            m_Logging.info("Controller's new id is %s", identification);
        }
    }
    
    @Override
    public OperationMode getOperationMode()
    {
        return m_OperationMode;
    }

    @Override
    public void setOperationMode(final OperationMode operationMode)
    {
        m_Prop.setProperty(OPERATION_MODE_KEY, operationMode.value());
        try (FileOutputStream outputStream = m_FileService.createFileOutputStream(m_PropFile))
        {
            m_Prop.store(outputStream, "Terra Harvest Controller Properties, last property edit was 'operation.mode'");
            
            final Map<String, Object> eventProps = new HashMap<>();
            eventProps.put(EVENT_PROP_SYSTEM_MODE, operationMode.value());
            final Event modeChangeEvent = new Event(TerraHarvestController.TOPIC_CONTROLLER_MODE_CHANGED, eventProps);
            m_EventAdmin.postEvent(modeChangeEvent);
           
        }
        catch (final IOException ex)
        {
            m_Logging.error(ex, "Unable to write 'OperationMode' to system file");
        }
        
        m_OperationMode = operationMode;
        m_Logging.info("Controller's new operation mode is %s", operationMode);
    }
    
    @Override
    public String getVersion()
    {
        if (m_Proxy == null || !Objects.firstNonNull(m_Capabilities.isVersionOverridden(), false))
        {
            return m_VersionProvider.getVersion();
        }
        else
        {
            return m_Proxy.getVersion();
        }
    }

    @Override
    public Map<String, String> getBuildInfo()
    {
        if (m_Proxy == null || !Objects.firstNonNull(m_Capabilities.isBuildInfoOverridden(), false))
        {
            return m_VersionProvider.getBuildInfo();
        }
        else
        {
            return m_Proxy.getBuildInfo();
        }
    }
    
    @Override
    public ControllerCapabilities getCapabilities()
    {
        return m_Capabilities;
    }
    
    /**
     * Load properties from the {@link #PROP_FILE_NAME} and initial fields with values. If values are not set in file,
     * initialize with default values.
     * 
     * @param context
     *      context for the bundle containing this component
     */
    private void loadProperties(final BundleContext context)
    {
        final String configDirectory = context.getProperty(SystemConfigurationConstants.DATA_DIR_PROPERTY);
        final File configParentFile = new File(configDirectory, "conf");
        m_PropFile = new File(configParentFile, PROP_FILE_NAME);
        
        m_FileService.mkdir(configParentFile);
        
        if (m_FileService.doesFileExist(m_PropFile))
        {
            try (FileInputStream inputStream = m_FileService.createFileInputStream(m_PropFile))
            {
                m_Prop.load(inputStream);
            }
            catch (final IOException ex)
            {
                m_Logging.error(ex, "Error in loading properties from file");
            }
        }
        
        // read in initial values into fields
        final String id = m_Prop.getProperty(ID_KEY);
        if (id == null)
        {
            m_Logging.warning("Controller id not set, initializing to default");
            setId(-1);
        }
        else
        {
            try
            {
                m_Id = Integers.parseHexOrDecimal(id);
            }
            catch (final NumberFormatException e)
            {
                m_Logging.warning("Unable to parse id from [%s]", id);
                setId(-1);
            }
        }
        
        m_Name = m_Prop.getProperty(NAME_KEY);
        if (m_Name == null)
        {
            m_Logging.warning("Controller name not set, initializing to default");
            setName("<undefined>");
        }
        
        final String mode = m_Prop.getProperty(OPERATION_MODE_KEY);
        if (mode == null)
        {
            m_Logging.warning("Controller operation mode not set, initializing to default");
            setOperationMode(OperationMode.TEST_MODE);
        }
        else
        {
            try
            {
                m_OperationMode = OperationMode.fromValue(mode);
            }
            catch (final IllegalArgumentException e)
            {
                m_Logging.warning("Unable to parse mode from [%s]", mode);
                setOperationMode(OperationMode.TEST_MODE);
            }
        }        
    }
}
