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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Dictionary;
import java.util.Map;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * @author dhumeniuk
 *
 */
public class BundleContextMocker
{
    /**
     * Just mock out the basic properties.
     */
    public static BundleContext createBasicMock()
    {
        BundleContext context = mock(BundleContext.class);
        
        Bundle bundle = mock(Bundle.class);
        when(context.getBundle()).thenReturn(bundle);
        when(bundle.getBundleContext()).thenReturn(context);
        
        return context;
    }
    
    @SuppressWarnings({ "rawtypes" })
    public static ServiceListenerAdaptor spyServiceListener(BundleContext context) throws InvalidSyntaxException
    {
        final ServiceListenerAdaptor adaptor = new ServiceListenerAdaptor(context);
        
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                Object[] args = invocation.getArguments();
                final ServiceListener listener = (ServiceListener)args[0];
                adaptor.bindListener(listener);
                return null;
            }
        }).when(context).addServiceListener(Mockito.any(ServiceListener.class), anyString());
        
        return adaptor;
    }
    
    /**
     * Used for creating Filters for use with EventAdmin.
     * 
     * Assumes filter string is (&amp;(event.topic)(event.filter))
     */
    public static BundleContext createBundleContextEventFilter()
    {
        BundleContext context = createBasicMock();
        try
        {
            when(context.createFilter(anyString())).thenAnswer(new Answer<Filter>()
            {
                @Override
                public Filter answer(InvocationOnMock invocation) throws Throwable
                {
                    final String filter = (String)invocation.getArguments()[0];
                    final String eventTopic = filter.contains("(&(")
                        ? filter.substring(3, filter.indexOf(")")) : filter.substring(1, filter.length() - 1);
                    final String eventFilter = filter.contains(")(") 
                        ? filter.substring(filter.indexOf(")(") + 2, filter.length() - 2) : null;
                    
                    return new Filter()
                    {
                        
                        @Override
                        public boolean matchCase(Dictionary<String, ?> dictionary)
                        {
                            if (dictionary.get(EventConstants.EVENT_TOPIC).equals(eventTopic))
                            {
                                if (eventFilter == null && dictionary.get(EventConstants.EVENT_FILTER) == null)
                                {
                                    return true;
                                }
                                else if (eventFilter != null && dictionary.get(EventConstants.EVENT_FILTER) != null)
                                {
                                    return dictionary.get(EventConstants.EVENT_FILTER).equals(eventFilter);
                                }
                                
                                return false;
                            }
                            
                            return false;
                        }
                        
                        @Override
                        public boolean match(Dictionary<String, ?> dictionary)
                        {
                            return false;
                        }
                        
                        @Override
                        public boolean match(ServiceReference<?> reference)
                        {
                            return false;
                        }

                        @Override
                        public boolean matches(Map<String, ?> arg0) 
                        {
                            return false;
                        }
                    };
                }
            });
        }
        catch (InvalidSyntaxException e)
        {
            // inside when, never happens
        }
        
        return context;
    }
    
    /**
     * Mock the {@link BundleContext#registerService(Class, Object, Dictionary)} method.
     * 
     * @param context
     *      context to stub
     * @param clazz
     *      type of service registration
     * @return
     *      registration for the service
     */
    @SuppressWarnings("unchecked")
    public static <T> ServiceRegistration<T> stubServiceRegistration(BundleContext context, Class<T> clazz)
    {
        ServiceRegistration<T> serviceRegistration = mock(ServiceRegistration.class);
        doReturn(serviceRegistration)
            .when(context).registerService(eq(clazz), notNull(clazz), notNull(Dictionary.class));
        
        return serviceRegistration;
    }

    /**
     * Asserts that the given context has an {@link EventHandler} register with the given topic.  Service that is 
     * registered is not checked.
     * 
     * Assume a single handler is registered.
     * 
     * @param context
     *      context to check with
     * @param topic
     *      topic that should be in the property map of the registration
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static EventHandler assertEventHandler(BundleContext context, String topic)
    {
        ArgumentCaptor<Dictionary> propsCap = ArgumentCaptor.forClass(Dictionary.class);
        ArgumentCaptor<EventHandler> handlerCap = ArgumentCaptor.forClass(EventHandler.class);
        verify(context).registerService(eq(EventHandler.class), handlerCap.capture(), propsCap.capture());
        
        EventHandler handler = handlerCap.getValue();
        assertThat("EventHandler exists", handler, is(notNullValue()));
        
        Dictionary props = propsCap.getValue();
        assertThat("Dictionary exists", props, is(notNullValue()));
        assertThat("Topic property", (String)props.get(EventConstants.EVENT_TOPIC), is(topic));
        
        return handler;
    }

    /**
     * Stub context to return the given filter.
     */
    public static Filter stubFilter(BundleContext context)
    {
        final Filter filter = mock(Filter.class);
        
        try
        {
            when(context.createFilter(anyString())).thenAnswer(new Answer<Filter>()
            {
                @Override
                public Filter answer(InvocationOnMock invocation) throws Throwable
                {
                    String filterStr = (String)invocation.getArguments()[0];
                    
                    when(filter.toString()).thenReturn(filterStr);
                    
                    return filter;
                }               
            });
        }
        catch (InvalidSyntaxException e)
        {
            throw new IllegalStateException(e);
        }
        
        return filter;
    }
}
