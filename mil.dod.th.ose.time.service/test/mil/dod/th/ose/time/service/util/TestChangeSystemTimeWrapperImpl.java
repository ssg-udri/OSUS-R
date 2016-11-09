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
package mil.dod.th.ose.time.service.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test the system time utility.
 * @author nickmarcucci
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Runtime.class, ChangeSystemTimeWrapperImpl.class})
public class TestChangeSystemTimeWrapperImpl
{
    private ChangeSystemTimeWrapperImpl m_SUT;
    private Runtime m_Runtime;
    
    @Before
    public void init()
    {
        PowerMockito.mockStatic(Runtime.class);
        m_Runtime = mock(Runtime.class);
        
        when(Runtime.getRuntime()).thenReturn(m_Runtime);
        
        m_SUT = new ChangeSystemTimeWrapperImpl();
    }
    
    /**
     * Verify that the system time function calls date and time commands
     * with the correct values as specified by the long in milliseconds
     * which represents the date Nov 15, 2013 10:38:16.
     */
    @Test
    public void testSetSystemTime() throws IOException
    {
        //time: Nov 15, 2013 10:38:16
        long time = 1384529896856L;
        
        m_SUT.setSystemTime(time);
        
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(m_Runtime, times(2)).exec(captor.capture());
        
        assertThat(captor.getAllValues().get(0), is("cmd /C time 10:38:16"));
        assertThat(captor.getAllValues().get(1), is("cmd /C date 11/15/2013"));
    }
}
