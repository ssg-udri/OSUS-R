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
package mil.dod.th.ose.gui.webapp.advanced.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model that represents a service configuration.
 * 
 * @author cweisenborn
 */
public class ConfigAdminModel
{
    /**
     * Bundle location of the bundle containing the configuration.
     */
    private String m_BundleLocation;
    
    /**
     * PID of the configuration.
     */
    private String m_Pid;
    
    /**
     * Factory PID if the configuration is a factory configuration.
     */
    private String m_FactoryPid;
    
    /**
     * Boolean value that represents whether the configuration is a factory.
     */
    private boolean m_IsFactory;
    
    /**
     * Map of factory configurations associated with the factory. The key is a string which represents the PID of the 
     * factory configuration and the value is a model which represents the factory configuration.
     */
    private Map<String, ConfigAdminModel> m_FactoryConfigurations;
    
    /**
     * List of all properties associated with the configuration.
     */
    private List<ConfigAdminPropertyModel> m_Properties;
    
    /**
     * Sets the bundle location of the configuration.
     * 
     * @param bundleLocation
     *          Bundle location to be set.
     */
    public void setBundleLocation(final String bundleLocation)
    {
        m_BundleLocation = bundleLocation;
    }
    
    /**
     * Retrieves the bundle location of the configuration.
     * 
     * @return
     *          Bundle location of the configuration.
     */
    public String getBundleLocation()
    {
        return m_BundleLocation;
    }
    
    /**
     * Sets the PID of the configuration.
     * 
     * @param pid
     *          PID to be set.
     */
    public void setPid(final String pid)
    {
        m_Pid = pid;
    }
    
    /**
     * Retrieves the PID of the configuration.
     * 
     * @return
     *          PID of the configuration.
     */
    public String getPid()
    {
        return m_Pid;
    }
    
    /**
     * Sets the factory PID for the configuration.
     * 
     * @param factoryPid
     *          Factory PID to be set.
     */
    public void setFactoryPid(final String factoryPid)
    {
        m_FactoryPid = factoryPid;
    }
    
    /**
     * Retrieves the factory PID for the configuration. Will be null if the configuration is not a factory 
     * configuration.
     * 
     * @return
     *      Factory PID for the configuration.
     */
    public String getFactoryPid()
    {
        return m_FactoryPid;
    }
    
    /**
     * Sets whether or not the configuration is a factory.
     * 
     * @param isFactory
     *          Boolean to be set.
     */
    public void setIsFactory(final boolean isFactory)
    {
        m_IsFactory = isFactory;
    }
    
    /**
     * Used to determine whether or not the configuration is a factory.
     * 
     * @return
     *          Boolean for whether or not the configuration is a factory.
     */
    public boolean isFactory()
    {
        return m_IsFactory;
    }
    
    /**
     * Retrieves a list of all factory configurations for the factory. Will be empty if the configuration is not a 
     * factory.
     * 
     * @return
     *          List of {@link ConfigAdminModel}s which represent all factory configurations for the factory.
     */
    public Map<String, ConfigAdminModel> getFactoryConfigurations()
    {
        if (m_FactoryConfigurations == null)
        {
            m_FactoryConfigurations = new HashMap<String, ConfigAdminModel>();
        }
        return m_FactoryConfigurations;
    }
    
    /**
     * Retrieves a list of all properties for the configuration. Will be empty if the configuration is a factory.
     * 
     * @return
     *          List of {@link ConfigAdminPropertyModel}s which represent all properties for the configuration.
     */
    public List<ConfigAdminPropertyModel> getProperties()
    {
        if (m_Properties == null)
        {
            m_Properties = new ArrayList<ConfigAdminPropertyModel>();
        }
        return m_Properties;
    }
}
