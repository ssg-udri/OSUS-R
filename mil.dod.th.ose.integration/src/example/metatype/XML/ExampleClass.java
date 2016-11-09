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
package example.metatype.XML;

import java.util.HashMap;
import java.util.Map;

import mil.dod.th.core.log.LoggingService;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Modified;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;

/**
 * Example class used to mimic a bundle that contains XML based metatype information.
 * @author callen
 *
 */
@Component(designate = ExampleClassConfig.class, configurationPolicy = ConfigurationPolicy.optional)
public class ExampleClass
{
    /**
     * Config PID will be the FQCN of this component since it uses {@link ExampleClassConfig} as a designate.
     */
    public final static String CONFIG_PID = ExampleClass.class.getName();

    /**
     * Event admin service.
     */
    private EventAdmin m_EventAdmin;
    
    /**
     * Example member field that holds the {@link ExampleClassConfig#exampleConfigValue()}.
     */
    private int m_ExampleValue;

    /**
     * Logging service used to log information.
     */
    private LoggingService m_Logging;

    /**
     * Bind the event admin service.
     * 
     * @param eventAdmin
     *      service for posting events
     */
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
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
     * Activate the component.
     * 
     * @param props
     *            Configuration Map
     */
    @Activate
    public void activate(final Map<String, Object> props)
    {
        updateProps(props);
    }

    /**
     * Updates/sets {@link ExampleClassConfig} properties. Generally THOSE classes that contain an @modified 
     * annotated class have an accompanying configuration interface associated with it. These configuration interfaces
     * define configuration properties. The framework is made aware of the interface by annotating the component header
     * with a designate tag like "designate = ExampleClassConfig.class". Configuration policy tags tell whether the
     * configuration is optional, required, or ignored.
     * 
     * @param props
     *     key, value pairs used to update the configuration value
     */
    @Modified
    public void modified(final Map<String, Object> props)
    {
        updateProps(props);
    }

    /**
     * Configurable object, created as a proxy object from the {@link ExampleClassConfig} interface is created
     * here and updated with new properties as values are changed through the @modified annotated method. The 
     * annotation denotes to the framework that this class contains configuration information that can be updated, and
     * that the method annotated should be called when those values are changed.
     * 
     * If the configuration value is also a framework property defined in a configuration property file 
     * the bundle context saved can be used to to fetch that value, generally this is done if there is a chance 
     * that the {@link org.osgi.service.cm.ConfigurationAdmin} may not be available.
     * @param props
     *     the properties assumed to contain the configuration values defined in the configuration interface belonging
     *     to this class. Note that there is a tag for default values in the event that the property map passed in
     *     does not have a value for the configuration.
     */
    private void updateProps(Map<String, Object> props)
    {
        //configurable object
        final ExampleClassConfig config = Configurable.createConfigurable(ExampleClassConfig.class, props);

        //logging
        m_Logging.debug("Example configuration class setting value from property map.");
        
        m_EventAdmin.postEvent(new Event("TOPIC_MAGICAL_UPDATE_EXAMPLECLASS", new HashMap<String, Object>()));
        
        //set that value to the member field
        setExampleConfigValue(config.exampleConfigValue());
    }

    /**
     * Setter for the member variable holding the configuration value within this example class.
     * @param value
     *     the value to set for the field
     */
    private void setExampleConfigValue(final int value)
    {
        m_ExampleValue = value;
        
        //log the value
        m_Logging.debug(
            "Example configuration class is using %d for the example configuration value.", m_ExampleValue);
    }
}
