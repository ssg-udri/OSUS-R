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
import static org.junit.Assert.fail;

import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

@SuppressWarnings({ "rawtypes", "unchecked" })//Due to mocking and ongoing stubbing
public class TestEventHandlerHelperImpl
{

    private EventHandlerHelperImpl m_SUT;
    private BundleContext m_Context;

    @Before
    public void setUp() throws Exception
    {
        m_SUT = new EventHandlerHelperImpl();
        m_Context = mock(BundleContext.class);
        m_SUT.activate(m_Context);
    }
       
    @Test
    public void testDeactivate()
    {
        // mock
        EventHandler handler = mock(EventHandler.class);
        ServiceRegistration expectedReg1 = mock(ServiceRegistration.class);
        ServiceRegistration expectedReg2 = mock(ServiceRegistration.class);
        when(m_Context.registerService(eq(EventHandler.class), eq(handler), Mockito.any(Dictionary.class)))
            .thenReturn(expectedReg1, expectedReg2);
        ServiceReference expectedRef1 = mock(ServiceReference.class);
        ServiceReference expectedRef2 = mock(ServiceReference.class);
        when(expectedReg1.getReference()).thenReturn(expectedRef1);
        when(expectedReg2.getReference()).thenReturn(expectedRef2);
        
        // replay
        m_SUT.registerHandler(handler, "blah");
        m_SUT.registerHandler(handler, "dee");
        m_SUT.deactivate();
        
        verify(expectedReg1).unregister();
        verify(expectedReg2).unregister();
        
        // replay
        m_SUT.deactivate();
        
        // verify not called again
        verify(expectedReg1, times(1)).unregister();
        verify(expectedReg2, times(1)).unregister();
    }

    @Test
    public void testRegisterHandlerTopicOnly()
    {
        // mock
        EventHandler handler = mock(EventHandler.class);
        ServiceRegistration expectedReg = mock(ServiceRegistration.class);
        when(m_Context.registerService(eq(EventHandler.class), eq(handler), Mockito.any(Dictionary.class)))
            .thenReturn(expectedReg);
        ServiceReference expectedRef = mock(ServiceReference.class);
        when(expectedReg.getReference()).thenReturn(expectedRef);
        
        // replay
        ServiceReference ref = m_SUT.registerHandler(handler, "blah");
        
        // verify
        assertThat(ref, is(expectedRef));
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(EventConstants.EVENT_TOPIC, "blah");
        verify(m_Context).registerService(EventHandler.class, handler, properties);
    }

    @Test
    public void testRegisterHandlerTopicAndFilter()
    {
        // mock
        EventHandler handler = mock(EventHandler.class);
        ServiceRegistration<EventHandler> expectedReg = mock(ServiceRegistration.class);
        when(m_Context.registerService(eq(EventHandler.class), eq(handler), Mockito.any(Dictionary.class)))
            .thenReturn(expectedReg);
        ServiceReference expectedRef = mock(ServiceReference.class);
        when(expectedReg.getReference()).thenReturn(expectedRef);
        
        // replay
        ServiceReference ref = m_SUT.registerHandler(handler, "blah", "(a=b)");
        
        // verify
        assertThat(ref, is(expectedRef));
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(EventConstants.EVENT_TOPIC, "blah");
        properties.put(EventConstants.EVENT_FILTER, "(a=b)");
        verify(m_Context).registerService(EventHandler.class, handler, properties);
    }
    
    @Test
    public void testUnregisterHandler()
    {
        // mock
        EventHandler handler = mock(EventHandler.class);
        ServiceRegistration reg = mock(ServiceRegistration.class);
        when(m_Context.registerService(eq(EventHandler.class), eq(handler), Mockito.any(Dictionary.class)))
            .thenReturn(reg);
        ServiceReference expectedRef = mock(ServiceReference.class);
        when(reg.getReference()).thenReturn(expectedRef);
        
        // replay
        ServiceReference ref = m_SUT.registerHandler(handler, "blah", "(a=b)");
        m_SUT.unregisterHandler(ref);
        
        verify(reg).unregister();
        
        try
        {
            m_SUT.unregisterHandler(ref);
            fail("expected exception");
        }
        catch (IllegalArgumentException e)
        {
            
        }
        
        // verify only called the first time
        verify(reg, times(1)).unregister();
    }
    
    @Test
    public void testUnregisterAllHandlers()
    {
        // mock
        EventHandler handler1 = mock(EventHandler.class);
        EventHandler handler2 = mock(EventHandler.class);
        EventHandler handler3 = mock(EventHandler.class);
        ServiceRegistration reg1 = mock(ServiceRegistration.class);
        ServiceRegistration reg2 = mock(ServiceRegistration.class);
        ServiceRegistration reg3 = mock(ServiceRegistration.class);
        when(m_Context.registerService(eq(EventHandler.class), eq(handler1), Mockito.any(Dictionary.class)))
            .thenReturn(reg1);
        when(m_Context.registerService(eq(EventHandler.class), eq(handler2), Mockito.any(Dictionary.class)))
            .thenReturn(reg2);
        when(m_Context.registerService(eq(EventHandler.class), eq(handler3), Mockito.any(Dictionary.class)))
            .thenReturn(reg3);
        ServiceReference ref1 = mock(ServiceReference.class);
        when(reg1.getReference()).thenReturn(ref1);
        ServiceReference ref2 = mock(ServiceReference.class);
        when(reg2.getReference()).thenReturn(ref2);
        ServiceReference ref3 = mock(ServiceReference.class);
        when(reg3.getReference()).thenReturn(ref3);
        
        // replay
        m_SUT.registerHandler(handler1, "blah", "(a=b)");
        m_SUT.unregisterHandler(m_SUT.registerHandler(handler2, "topic2", "(&(a=b)(c=d))"));
        m_SUT.registerHandler(handler3, "topic3");
        
        verify(reg2).unregister();
        
        m_SUT.unregisterAllHandlers();
        
        verify(reg1).unregister();
        verify(reg2, times(1)).unregister(); // verify not called again
        verify(reg3).unregister();
        
        m_SUT.unregisterAllHandlers();
        
        // verify all not called again
        verify(reg1, times(1)).unregister();
        verify(reg2, times(1)).unregister(); 
        verify(reg3, times(1)).unregister();
    }
}
