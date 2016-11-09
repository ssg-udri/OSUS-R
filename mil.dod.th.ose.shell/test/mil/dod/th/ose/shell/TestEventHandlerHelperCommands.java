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
package mil.dod.th.ose.shell;

import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;

import mil.dod.th.core.mp.EventHandlerHelper;

/**
 * @author cweisenborn
 *
 */
public class TestEventHandlerHelperCommands
{
    private EventHandlerHelperCommands m_SUT;
    private EventHandlerHelper m_EventHandlerHelper;
    
    @Before
    public void setup()
    {
        m_SUT = new EventHandlerHelperCommands();
        
        m_EventHandlerHelper = mock(EventHandlerHelper.class);
        
        m_SUT.setEventHandlerHelper(m_EventHandlerHelper);
    }
    
    @Test
    public void testUnregisterAllHandlers()
    {
        m_SUT.unregisterAllHandlers();
        
        verify(m_EventHandlerHelper, atLeastOnce()).unregisterAllHandlers();
    }
}
