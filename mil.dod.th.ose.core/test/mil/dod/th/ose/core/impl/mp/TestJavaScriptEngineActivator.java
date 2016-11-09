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

package mil.dod.th.ose.core.impl.mp;

import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.script.ScriptEngine;

import mil.dod.th.ose.mp.runtime.MissionProgramRuntime;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class TestJavaScriptEngineActivator
{

    private JavaScriptEngineActivator m_SUT;
    
    @Mock private MissionProgramRuntime m_MissionProgramRuntime;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        
        // must set to valid class loader, any will do since running in single class loader environment, if not mocked
        // class loader will be null and nothing can be loaded
        when(m_MissionProgramRuntime.getClassLoader()).thenReturn(getClass().getClassLoader());
        
        m_SUT = new JavaScriptEngineActivator();
        m_SUT.setMissionProgramRuntime(m_MissionProgramRuntime);
    }

    @Test
    public void testActivate()
    {
        BundleContext context = mock(BundleContext.class);
        m_SUT.activate(context);
        
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("name", "JavaScript");
        ArgumentCaptor<ScriptEngine> serviceCaptor = ArgumentCaptor.forClass(ScriptEngine.class);
        verify(context).registerService(eq(ScriptEngine.class), serviceCaptor.capture(), eq(properties));
        assertThat(serviceCaptor.getValue(), is(instanceOf(ScriptEngine.class)));
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })//Due to mocking and stubbing
    @Test
    public void testDeactivate()
    {
        BundleContext context = mock(BundleContext.class);
        ServiceRegistration reg = mock(ServiceRegistration.class);
        when(context.registerService(eq(ScriptEngine.class), Mockito.any(ScriptEngine.class), 
                Mockito.any(Dictionary.class))).
            thenReturn(reg);
        m_SUT.activate(context);
        
        m_SUT.deactivate();
        verify(reg).unregister();
    }
}
