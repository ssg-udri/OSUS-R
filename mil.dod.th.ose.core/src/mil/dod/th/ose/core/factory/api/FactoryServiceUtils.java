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
package mil.dod.th.ose.core.factory.api;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import aQute.bnd.annotation.component.*;

import com.google.common.base.Preconditions;

import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.metatype.MetaTypeProviderBundle;
import mil.dod.th.ose.utils.ConfigurationUtils;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * Utility class with methods to assist {@link mil.dod.th.ose.core.factory.api.DirectoryService} and its extensions.
 * 
 * @author cashioka
 *
 */
@Component(provide = FactoryServiceUtils.class)
public class FactoryServiceUtils
{
    /**
     * Bound service to manage meta type information.
     */
    private MetaTypeService m_MetaTypeService;
    
    /**
     * Service provides a bundle to be used by a {@link org.osgi.service.metatype.MetaTypeProvider}.
     */
    private MetaTypeProviderBundle m_MetaTypeProviderBundle;
    
    /**
     * Method to bind a service to manage meta type information.
     * 
     * @param metaTypeService
     *             the service to bind
     */
    @Reference
    public void setMetaTypeService(final MetaTypeService metaTypeService)
    {
        m_MetaTypeService = metaTypeService;
    }
    
    /**
     * Bind the OSGi service.
     * 
     * @param metaTypeProviderBundle
     *      service to bind
     */
    @Reference
    public void setMetaTypeProviderBundle(final MetaTypeProviderBundle metaTypeProviderBundle)
    {
        m_MetaTypeProviderBundle = metaTypeProviderBundle;
    }
    
    /**
     * Method to populate a base set of {@link FactoryObjectInternal} event properties.
     * @param object
     *  the object on which the event property map will be filled out.
     * @return
     *  the property map with the base event properties. The event properties included
     *  in this map will be:
     *  <ul>
     *  <li>{@link FactoryDescriptor#EVENT_PROP_OBJ}
     *  <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_NAME}
     *  <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_TYPE}
     *  <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_PID}
     *  <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_UUID}
     *  <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_BASE_TYPE}
     *  </ul>
     */
    public static Map<String, Object> getFactoryObjectBaseEventProps(final FactoryObjectInternal object)
    {
        Preconditions.checkNotNull(object);
        Preconditions.checkNotNull(object.getFactory());
        
        final Map<String, Object> props = new HashMap<String, Object>();
        
        props.put(FactoryDescriptor.EVENT_PROP_OBJ, object);
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, object.getName());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, object.getFactory().getProductType());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, object.getUuid().toString());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, object.getBaseType());       
        
        if (object.getPid() != null)
        {
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, object.getPid());
        }
        
        return props;
    }
    
    /**
     * Retrieve the default configuration properties from the metatype service for the given factory.
     * 
     * @param factory
     *             the factory to retrieve the metatype defaults from
     * @return set of default properties and values
     */
    public Dictionary<String, Object> getMetaTypeDefaults(final FactoryDescriptor factory)
    {
        final Dictionary<String, Object> properties = new Hashtable<String, Object>();

        final AttributeDefinition[] attrs = factory.getAttributeDefinitions(ObjectClassDefinition.ALL);

        for (AttributeDefinition attr : attrs)
        {
            if (attr.getDefaultValue() != null) // skip if no default exists
            {
                final Object value = ConfigurationUtils.parseStringsByType(attr.getType(), attr.getCardinality(), 
                        attr.getDefaultValue());
                properties.put(attr.getID(), value);
            }
        }
        
        return properties;
    }
    
    /**
     * Get the metatype service reference bound to this component. 
     * @return 
     *      metatype service reference
     */
    public MetaTypeService getMetaTypeService()
    {
        return m_MetaTypeService;
    }
    
    /**
     * Get the service that provides a bundle for a {@link org.osgi.service.metatype.MetaTypeProvider}.
     * 
     * @return
     *      service instance
     */
    public MetaTypeProviderBundle getMetaTypeProviderBundle()
    {
        return m_MetaTypeProviderBundle;
    }
    
    /**
     * Get a configuration for the given PID. If the PID is null, the returned value will be null as well.
     * 
     * @param configAdmin
     *      Service to retrieve configurations from
     * @param pid
     *      PID of the existing or about to be created {@link FactoryObjectInternal} 
     * @return
     *      null if no configuration for the PID or PID is null itself, otherwise, configuration with properties (safe 
     *      to call {@link Configuration#getProperties()}
     * @throws FactoryException
     *      if there is an exception will retrieving the configuration
     */
    public static Configuration getFactoryConfiguration(final ConfigurationAdmin configAdmin, final String pid)
            throws FactoryException
    {
        //fetch properties from the config admin
        if (pid != null)
        {
            final String filter = String.format("(%s=%s)", Constants.SERVICE_PID, pid);
            
            final Configuration[] configs;
            try
            {
                configs = configAdmin.listConfigurations(filter);
            }
            catch (final IOException | InvalidSyntaxException e)
            {
                throw new FactoryException(String.format("Unable to list configurations for [%s]", pid), e);
            }
            
            if (configs != null)
            {
                if (configs.length != 1)
                {
                    throw new IllegalStateException(
                            String.format("Expected exactly one configuration with PID [%s], but found [%d]", 
                                    pid, configs.length));
                }
                return configs[0];
            }
        }
        return null;
    }
}
