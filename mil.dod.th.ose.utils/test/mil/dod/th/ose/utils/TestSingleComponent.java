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

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;

public class TestSingleComponent
{
    private SingleComponent<Object> m_SUT;
    
    @Mock private ComponentFactory factory;
    @Mock private ComponentInstance instance;

    private Object m_Object = new Object();
    
    @SuppressWarnings("unchecked")
    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        
        when(factory.newInstance(Mockito.any(Dictionary.class))).thenReturn(instance);
        when(instance.getInstance()).thenReturn(m_Object);

        m_SUT = new SingleComponent<Object>(factory);
    }
    
    @Test
    public void testNewInstance()
    {
        Dictionary<String, Object> props = new Hashtable<>();
        props.put("key", "value");
        
        m_SUT.newInstance(props);
        
        verify(factory).newInstance(props);
        assertThat(m_SUT.getInstance(), is(instance));
        assertThat(m_SUT.getObject(), is(m_Object));
    }
    
    /**
     * Verify only a single instance can be made, that a second call fails.
     */
    @Test
    public void testNewInstance_MultipleCalls()
    {
        Dictionary<String, Object> props = new Hashtable<>();
        props.put("key", "value");
        
        m_SUT.newInstance(props);
        
        try
        {
            m_SUT.newInstance(props);
            fail("Expecting exception");
        }
        catch (IllegalStateException e)
        {
            
        }
    }
    
    @Test
    public void testTryDispose()
    {
        Dictionary<String, Object> props = new Hashtable<>();
        props.put("key", "value");
        
        m_SUT.newInstance(props);
        
        m_SUT.tryDispose();
        
        verify(instance).dispose();
        assertThat(m_SUT.getInstance(), is(nullValue()));
        assertThat(m_SUT.getObject(), is(nullValue()));
    }

    /**
     * Verify can try to dispose before new instance is created and no exception occurs.
     */
    @Test
    public void testTryDispose_BeforeNew()
    {
        Dictionary<String, Object> props = new Hashtable<>();
        props.put("key", "value");
        
        m_SUT.tryDispose();
    }
    
    /**
     * Verify can try to dispose twice without error and underlying instance is only disposed of once.
     */
    @Test
    public void testTryDispose_AfterDispose()
    {
        Dictionary<String, Object> props = new Hashtable<>();
        props.put("key", "value");
        
        m_SUT.newInstance(props);
        
        m_SUT.tryDispose();
        
        m_SUT.tryDispose();
        
        verify(instance).dispose();
    }
}
