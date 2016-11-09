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

import static org.mockito.Mockito.*;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * @author dhumeniuk
 *
 */
public class ServiceListenerAdaptor
{
    private ServiceListener m_Listener;
    private BiMap<ServiceReference<?>, Object> m_ServiceMap = HashBiMap.create(); 
    private BundleContext m_Context;

    ServiceListenerAdaptor(BundleContext context)
    {
        m_Context = context;
    }
    
    public void bindListener(ServiceListener listener)
    {
        m_Listener = listener;
    }
    
    @SuppressWarnings("rawtypes")
    public void addService(ServiceReference reference, Object service)
    {
        m_ServiceMap.put(reference, service);
        
        when(m_Context.getService(Mockito.any())).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                Object[] args = invocation.getArguments();
                ServiceReference ref = (ServiceReference)args[0];
                return m_ServiceMap.get(ref);
            }
        });
        
        m_Listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference));
    }
    
    public ServiceReference<?> removeService(Object service)
    {
        ServiceReference<?> reference = m_ServiceMap.inverse().get(service);
        Preconditions.checkNotNull(reference);
        m_Listener.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference));
        
        return reference;
    }
}
