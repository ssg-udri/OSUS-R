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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doAnswer;
import mil.dod.th.core.factory.FactoryObjectContext;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * A generic syncer for factory objects when they post events. Class syncs on posted event topics.
 * 
 * @author nickmarcucci
 */
public class FactoryObjectEventSyncer extends GenericEventSyncer<String>
{    
    public FactoryObjectEventSyncer(FactoryObjectContext fObj, final String topic)
    {
        this(fObj, topic, 1);
    }
    
    @SuppressWarnings("unchecked")
    public FactoryObjectEventSyncer(FactoryObjectContext fObj, final String desiredTopic, final int times)
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
                String topic = (String)invocation.getArguments()[0];
                if (topic.equals(desiredTopic))
                {
                    m_TimesRemaining--;
                    if (m_TimesRemaining <= 0)
                    {
                        m_FoundEvents.add(topic);
                        m_WaitSem.release();                        
                    }
                }
                return null;
            }
        }).when(fObj).postEvent(Mockito.anyString(), Mockito.anyMap());
    }
}
