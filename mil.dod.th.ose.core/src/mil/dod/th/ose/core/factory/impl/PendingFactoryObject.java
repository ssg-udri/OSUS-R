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
package mil.dod.th.ose.core.factory.impl;

import java.util.Dictionary;
import java.util.UUID;

import mil.dod.th.ose.core.factory.api.FactoryInternal;

/**
 * Represents a factory object that depends on one or more other factory objects that haven't been created yet.  The 
 * factory object data like the name and uuid will already exist for this object, but an instance will not be added 
 * the respective core service's registry until the dependencies are satisfied.
 * 
 * @author dlandoll
 */
public class PendingFactoryObject
{
    /** The UUID value of the pending object. */
    private final UUID m_Uuid;
    
    /** 
     * Properties associated with the pending object, will be used when creating the object.  Configuration properties
     * plus any default properties based on the meta type information.
     */
    private final Dictionary<String, Object> m_Properties;

    /**
     * Factory that can produce the pending object.
     */
    private final FactoryInternal m_Factory;
    
    /**
     * Creates a new pending object.
     * 
     * @param uuid
     *      intended UUID for the object
     * @param properties
     *      properties for the pending object
     * @param factory
     *      factory that produces the pending object 
     */
    public PendingFactoryObject(final UUID uuid, final Dictionary<String, Object> properties, 
            final FactoryInternal factory)
    {
        m_Uuid = uuid;
        m_Properties = properties;
        m_Factory = factory;
    }

    /**
     * Returns the UUID that will be associated with the object once it is created.
     * 
     * @return 
     *      the UUID value
     */
    public UUID getUuid()
    {
        return m_Uuid;
    }
    
    /**
     * Returns the properties for the object.
     * 
     * @return properties
     */
    public Dictionary<String, Object> getProperties()
    {
        return m_Properties;
    }

    /**
     * Get the factory the produces the pending factory.
     * 
     * @return a factory that produces the pending factory
     */
    public FactoryInternal getFactory()
    {
        return m_Factory;
    }
}
