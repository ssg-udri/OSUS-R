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

/**
 * Model that represents a configuration property.
 * 
 * @author cweisenborn
 */
public class ConfigAdminPropertyModel
{
    /**
     * Key for the property.
     */
    private String m_Key;
    
    /**
     * Value of the property.
     */
    private Object m_Value;
    
    /**
     * Class type of the property value.
     */
    private Class<?> m_Type;
    
    /**
     * Sets the key of the property.
     * 
     * @param key
     *          Key to be set.
     */
    public void setKey(final String key)
    {
        m_Key = key;
    }
    
    /**
     * Retrieves the key for the property.
     * 
     * @return
     *          Key for the property.
     */
    public String getKey()
    {
        return m_Key;
    }
    
    /**
     * Sets the value for the property.
     * 
     * @param value
     *          Value to be set.
     */
    public void setValue(final Object value)
    {
        m_Value = value;
    }
    
    /**
     * Retrieves the value of the property.
     * 
     * @return
     *          Value of the property.
     */
    public Object getValue()
    {
        return m_Value;
    }
    
    /**
     * Sets the class type of the property value.
     * 
     * @param type
     *          Class type to be set.
     */
    public void setType(final Class<?> type)
    {
        m_Type = type;
    }
    
    /**
     * Retrieves the value class type.
     * 
     * @return
     *          Class type of the value.
     */
    public Class<?> getType()
    {
        return m_Type;
    }
}
