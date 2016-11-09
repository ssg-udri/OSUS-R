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
package example.metatype.configadmin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import mil.dod.th.core.log.LoggingService;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

/**
 * Example class used to mimic a class that keeps metatype information through defined in memory types that are 
 * persisted and managed by interactions with the {@link ConfigurationAdmin} and {@link 
 * org.osgi.service.metatype.MetaTypeService}.
 * @author callen
 *
 */
@Component(properties = {"service.pid" + "="  + "example.metatype.configadmin.ExampleInMemConfigClass"})
public class ExampleInMemConfigClass implements ManagedService, MetaTypeProvider
{
    /**
     * Used to log messages.
     */
    private LoggingService m_Logging;

    /**
     * Configuration admin service that manages configuration objects.
     */
    private ConfigurationAdmin m_ConfigAdmin;

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
     * Bind the {@link ConfigurationAdmin} service.
     * 
     * @param configurationAdmin
     *      service that handles persisting bundle configuration data
     */
    @Reference
    public void setConfigurationAdmin(final ConfigurationAdmin configurationAdmin)
    {
        m_ConfigAdmin = configurationAdmin;
    }

    /**
     * Update the configuration with the given values.
     * At activation if the configuration does not exist it will be created.
     * @param props
     *     map of properties to use. 
     */
    @Override
    public void updated(final Dictionary<String, ?> props)
    {
        //configuration object
        final Configuration configuration;
        try
        {
            configuration = m_ConfigAdmin.getConfiguration("example.metatype.configadmin.ExampleInMemConfigClass");
        }
        catch (final IOException e)
        {
            //log error
            m_Logging.debug(e.getMessage() + " Unable to get or create example configuration with PID [%s]", 
                "example.metatype.configadmin.ExampleInMemConfigClass");
            //error in the example class, nothing further can be done
            return;
        }

        //check properties in case this is an update versus calls made at activation.
        Dictionary<String, Object> existingProperties = configuration.getProperties();
        if (existingProperties ==  null)
        {
            existingProperties = new Hashtable<String, Object>();
        }

        if (props == null)
        {
            //add a property just as an example
            existingProperties.put("name.example.property", "example.example.bundle.example");
        }
        else
        {
            // add missing properties
            final Enumeration<String> keys = props.keys();
            while (keys.hasMoreElements())
            {
                final String key = keys.nextElement();
                existingProperties.put(key, props.get(key));
            }
        }
    }

    @Override
    public ObjectClassDefinition getObjectClassDefinition(String id, String locale)
    {
        final ObjectClassDefinition ocd = new ObjectClassDefinition()
        {
            
            @Override
            public String getName()
            {
                return "name";
            }
            
            @Override
            public InputStream getIcon(int size) throws IOException
            {
                return null;
            }
            
            @Override
            public String getID()
            {
                return "example.metatype.configadmin.ExampleInMemConfigClass";
            }
            
            @Override
            public String getDescription()
            {
                return "Example Object Class Definition (OCD)";
            }
            
            @Override
            public AttributeDefinition[] getAttributeDefinitions(int filter)
            {
                return null;
            }
        };
        return ocd;
    }

    @Override
    public String[] getLocales()
    {
        return null;
    }
}
