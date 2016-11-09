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

package mil.dod.th.ose.utils;

import java.util.Dictionary;

import com.google.common.base.Preconditions;

import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;

/**
 * Wraps a {@link ComponentFactory} for a single component instance. Makes it easier to work with so you don't have to
 * keep a field for the instance and factory.
 *  
 * @author dhumeniuk
 *
 * @param <T>
 *      type of instance held by this model
 */
public class SingleComponent<T>
{
    /**
     * actual instance of the component.
     */
    private T m_Object;
    
    /**
     * representation of the instance created by the factory.
     */
    private ComponentInstance m_Instance;
    
    /**
     * factory that created the instance.
     */
    private final ComponentFactory m_Factory;
    
    /**
     * Construct wrapper for the given factory.
     * 
     * @param factory
     *      factory that will be used to create a single instance
     */
    public SingleComponent(final ComponentFactory factory)
    {
        m_Factory = factory;
    }
    
    /**
     * Get the instance object.
     * 
     * @return
     *      object that was instantiated
     */
    public T getObject()
    {
        return m_Object;
    }
    
    /**
     * Get the instance representation created from the factory. Can be used to later dispose the instance.
     * 
     * @return
     *      representation of the created instance
     */
    public ComponentInstance getInstance()
    {
        return m_Instance;
    }
    
    /**
     * Factory that created the instance.
     * 
     * @return
     *      factory that creates instances
     */
    public ComponentFactory getFactory()
    {
        return m_Factory;
    }

    /**
     * Create a new {@link ComponentInstance} using the associated {@link ComponentFactory}.
     * 
     * @param properties
     *      properties to use for the component passing to {@link ComponentFactory#newInstance(Dictionary)}
     * @return
     *      the object associated with the instance, {@link ComponentInstance#getInstance()}
     */
    public T newInstance(final Dictionary<String, Object> properties)
    {
        // only allow a single instance to be created
        Preconditions.checkState(m_Instance == null);
        
        m_Instance = m_Factory.newInstance(properties);
        @SuppressWarnings("unchecked")
        final T object = (T)m_Instance.getInstance();
        m_Object = object;
        return m_Object;
    }
    
    /**
     * Try to dispose the previously created {@link ComponentInstance} if available.
     */
    public void tryDispose()
    {
        if (m_Instance != null)
        {
            m_Instance.dispose();
        }
        m_Instance = null; // NOPMD: assigning to null, clear value so object can't be used once disposed
        m_Object = null; // NOPMD: same
    }
}