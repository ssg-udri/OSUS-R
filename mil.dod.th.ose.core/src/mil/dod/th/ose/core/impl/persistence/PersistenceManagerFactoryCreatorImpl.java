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
package mil.dod.th.ose.core.impl.persistence;

import java.sql.Driver;
import java.util.HashMap;
import java.util.Map;

import javax.jdo.PersistenceManagerFactory;

import aQute.bnd.annotation.component.*;

import com.google.common.base.Preconditions;

import mil.dod.th.ose.utils.BundleService;
import mil.dod.th.ose.utils.ClassService;

import org.osgi.framework.Bundle;

import org.datanucleus.PropertyNames;
import org.datanucleus.api.jdo.JDOPersistenceManagerFactory;
import org.datanucleus.plugin.OSGiPluginRegistry;

/**
 * Implementation of the {@link PersistenceManagerFactoryCreator}.
 * 
 * @author jconn
 */
@Component
public class PersistenceManagerFactoryCreatorImpl implements PersistenceManagerFactoryCreator
{
    /**
     * Base Persistence Manager Factory configuration properties.
     */
    private final Map<String, Object> m_BaseProperties = new HashMap<String, Object>();

    /**
     * The database {@link Driver} reference.
     */
    private Driver m_Driver;

    /**
     * Service to access {@link Class} information.
     */
    private ClassService m_ClassService;

    /**
     * Service to access {@link Bundle} information.
     */
    private BundleService m_BundleService;

    /**
     * Binds the database {@link Driver}.
     * 
     * @param driver
     *            the specified database driver
     */
    @Reference
    public void setDriver(final Driver driver)
    {
        m_Driver = driver;
    }

    /**
     * Bind the service.
     * 
     * @param classService
     *      service being bound to this component
     */
    @Reference
    public void setClassService(final ClassService classService)
    {
        m_ClassService = classService;
    }
    
    /**
     * Bind the service.
     * 
     * @param bundleService
     *      service being bound to this component
     */
    @Reference
    public void setBundleService(final BundleService bundleService)
    {
        m_BundleService = bundleService;
    }

    /**
     * The service component activation method.
     */
    @Activate
    public void activate()
    {
        m_BaseProperties.put("javax.jdo.PersistenceManagerFactoryClass",
                "org.datanucleus.api.jdo.JDOPersistenceManagerFactory");
        m_BaseProperties.put(PropertyNames.PROPERTY_CONNECTION_USER_NAME, "THOSEAdmin");
        m_BaseProperties.put(PropertyNames.PROPERTY_CONNECTION_DRIVER_NAME, m_Driver.getClass().getName());
        m_BaseProperties.put(PropertyNames.PROPERTY_MAPPING, "h2");
        m_BaseProperties.put(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_ALL, Boolean.TRUE.toString());
        m_BaseProperties.put(PropertyNames.PROPERTY_SCHEMA_VALIDATE_TABLES, Boolean.FALSE.toString());
        m_BaseProperties.put(PropertyNames.PROPERTY_SCHEMA_VALIDATE_CONSTRAINTS, Boolean.FALSE.toString());
        m_BaseProperties.put(PropertyNames.PROPERTY_CLASSLOADER_RESOLVER_NAME, "datanucleus");
        m_BaseProperties.put(PropertyNames.PROPERTY_STORE_MANAGER_TYPE, "rdbms");
        m_BaseProperties.put(PropertyNames.PROPERTY_PLUGIN_REGISTRY_CLASSNAME, OSGiPluginRegistry.class.getName());
    }

    @Override
    public PersistenceManagerFactory createPersistenceManagerFactory(final Class<?> extentClass, final String url)
    {
        final Bundle bundle = m_BundleService.getBundle(OSGiPluginRegistry.class);
        Preconditions.checkNotNull(bundle);
        
        if (bundle.getState() == Bundle.RESOLVED)
        {
            // new JDOPersistenceManagerFactory() call below will attempt to get the bundle context and will fail if the
            // DataNucleus core bundle is not in an active (and starting/stopping) state
            throw new IllegalStateException(
                    String.format("%s must be in an active state to retrieve the bundle context", bundle));
        }

        final Map<String, Object> props = new HashMap<String, Object>();
        props.putAll(m_BaseProperties);
        props.put(PropertyNames.PROPERTY_CONNECTION_URL, url);
        props.put(PropertyNames.PROPERTY_CLASSLOADER_PRIMARY, m_ClassService.getClassLoader(extentClass));
        
        return new JDOPersistenceManagerFactory(props);
    }
}
