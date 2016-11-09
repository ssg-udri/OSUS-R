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
package mil.dod.th.ose.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Array; // NOCHECKSTYLE: use of reflection, rule is to discourage using reflection on the class
                                // under test, this is not the case here

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.component.ComponentInstance;

/**
 * Answer to any method that returns a {@link ComponentInstance}. Will return a new instance for each call up to the
 * instance count specified and {@link ComponentInstance#getInstance()} will return a mock of the underlying type.
 */
public class NewInstanceAnswer<T> implements Answer<ComponentInstance>
{
    /**
     * Instances to return.
     */
    private ComponentInstance[] m_Instances;
    
    private T[] m_Objects;
    
    /**
     * Used to keep track of what instance to return next.
     */
    private int m_Index;

    @SuppressWarnings("unchecked")
    public NewInstanceAnswer(Class<T> clazz, int instanceCount)
    {
        m_Instances = new ComponentInstance[instanceCount];
        m_Objects = (T[])Array.newInstance(clazz, instanceCount);
        for (int i=0; i < instanceCount; i++)
        {
            m_Instances[i] = mock(ComponentInstance.class);
            m_Objects[i] = mock(clazz); 
            when(m_Instances[i].getInstance()).thenReturn(m_Objects[i]);
        }
    }

    @Override
    public ComponentInstance answer(InvocationOnMock invocation) throws Throwable
    {
        return m_Instances[m_Index++];
    }

    /**
     * Get the instances that the factory mocker will return.
     * 
     * @return
     *      list of instances
     */
    public ComponentInstance[] getComponentInstances()
    {
        return m_Instances;
    }
    
    public T[] getObjects()
    {
        return m_Objects;
    }
}