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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.Dictionary;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

/**
 * Class used to mock event admin functionality.
 * 
 * @author nickmarcucci
 *
 */
public class EventAdminMocker
{
    /**
     * Method to mock the {@link BundleContext#registerService(Class, Object, Dictionary)}
     * and retrieve the event handler that was registered in that call.
     * @param context
     *  the bundle context to use
     * @param clazz
     *  the event handler class of the handler that is being registered
     * @param admin
     *  the event admin instance to use
     * 
     * @return
     *  the event handler that is returned
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static EventHandlerRegistrationAnswer stubHandlerOfType(
            final BundleContext context, Class<? extends EventHandler> clazz, EventAdmin admin)
    {
        ServiceRegistration reg = mock(ServiceRegistration.class);
        
        EventHandlerRegistrationAnswer answer = new EventHandlerRegistrationAnswer(reg, admin);
        
        doAnswer(answer).when(context).registerService(eq(EventHandler.class), 
                Mockito.any(clazz), Mockito.any(Dictionary.class));
        
        return answer;
    }
   
    /**
     * Class which represents the custom answer for a service registration.
     * 
     * @author nickmarcucci
     *
     */
    public static class EventHandlerRegistrationAnswer implements Answer<Object>
    {
        /**
         * Event handler that is to be captured.
         */
        private EventHandler m_HandlerToReturn;
        
        /**
         * Service registration to return.
         */
        @SuppressWarnings("rawtypes")
        private ServiceRegistration m_ServiceReg;
        
        /**
         * The event admin instance to use.
         */
        private EventAdmin m_EventAdmin;
        
        /**
         * Constructor.
         * @param reg
         *  the service registration to use
         */
        @SuppressWarnings("rawtypes")
        public EventHandlerRegistrationAnswer(final ServiceRegistration reg, final EventAdmin admin)
        {
            m_ServiceReg = reg;
            m_EventAdmin = admin;
        }
        
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable
        {
            m_HandlerToReturn = (EventHandler)invocation.getArguments()[1];
            registerHandler();
            
            return m_ServiceReg;
        }
        
        /**
         * Retrieves the event handler that has been captured.
         * @return
         *  the event handler to return
         */
        public EventHandler getHandler()
        {
            return m_HandlerToReturn;
        }
        
        /**
         * Retrieves the service registration that has been mocked
         * @return
         *  the service registration
         */
        @SuppressWarnings("rawtypes")
        public ServiceRegistration getRegistration()
        {
            return m_ServiceReg;
        }
        
        /**
         * Mocks the call to the given handler when the {@link EventAdmin#postEvent(Event)} method is 
         * invoked.
         */
        private void registerHandler()
        {
            doAnswer(new Answer<Object>()
            {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable
                {
                    m_HandlerToReturn.handleEvent((Event)invocation.getArguments()[0]);
                    return null;
                }
            }).when(m_EventAdmin).postEvent(Mockito.any(Event.class));
        }
    }    
}
