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


/**
 * Representation of registry dependency where the dependency is on a object created by a {@link FactoryRegistry}.
 * 
 * @author dhumeniuk
 *
 */
public abstract class RegistryDependency
{
    /**
     * Property name for the object to look for.  Each object has a set of properties.  One property should be the name
     * of the other object it depends on.  This value is the property key that names the other object.
     */
    private final String m_Property;
    
    private final boolean m_Required;
    
    /**
     * Construct the abstract class.
     * 
     * @param objectNameProperty
     *      Property that defines the name of the object that is depended on
     * @param required
     *      Whether the dependency is required to be specified (should be equal to the {@link 
     *      aQute.bnd.annotation.metatype.Meta.AD} annotation required field)  
     */
    public RegistryDependency(final String objectNameProperty, final boolean required)
    {
        m_Property = objectNameProperty;
        m_Required = required;
    }
    
    /**
     * Get the object name property for the object that is depended on.
     * 
     * @return
     *      name of the property that defines the object name
     */
    public String getObjectNameProperty()
    {
        return m_Property;
    }
    
    /**
     * Get the object with the given name for this dependency.  Might look for an asset, link layer, etc.
     * 
     * @param objectName
     *      Name of the object to look for, where to look depends on implementation
     * @return
     *      Object with the given name or null if not found
     */
    abstract public Object findDependency(String objectName);

    /**
     * Retrieve whether the dependency is required to be specified.
     * 
     * @return
     *      True if required
     */
    public Boolean isRequired()
    {
        return m_Required;
    }
}

