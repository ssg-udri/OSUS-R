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

import static org.mockito.Mockito.doAnswer;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * @author dhumeniuk
 *
 */
public class EventAdminSyncer extends GenericEventSyncer<Event>
{
    public EventAdminSyncer(EventAdmin eventAdmin, final String topic)
    {
        this(eventAdmin, topic, 1);
    }
    
    public EventAdminSyncer(EventAdmin eventAdmin, final String desiredTopic, int times)
    {
        assertThat(times, is(greaterThanOrEqualTo(0)));
        
        m_TotalTimes = times;
        m_TimesRemaining = times;
        m_DesiredTopic = desiredTopic;
        
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                Event event = (Event)invocation.getArguments()[0];
                if (event.getTopic().equals(desiredTopic))
                {
                    m_TimesRemaining--;
                    if (m_TimesRemaining <= 0)
                    {
                        m_FoundEvents.add(event);
                        m_WaitSem.release();                        
                    }
                }
                return null;
            }
        }).when(eventAdmin).postEvent(Mockito.any(Event.class));
    }
}
