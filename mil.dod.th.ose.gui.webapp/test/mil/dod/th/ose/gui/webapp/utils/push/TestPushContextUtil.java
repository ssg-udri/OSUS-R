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
package mil.dod.th.ose.gui.webapp.utils.push;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.primefaces.push.PushContext;
import org.primefaces.push.PushContextFactory;

/**
 * Test class for {@link PushContextUtil}
 * @author nickmarcucci
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({PushContextFactory.class, PushContext.class})
public class TestPushContextUtil
{
    private PushContextUtil m_SUT;
    
    @Before
    public void init()
    {
        m_SUT = new PushContextUtil();
        
        PowerMockito.mockStatic(PushContextFactory.class);
        PowerMockito.mockStatic(PushContext.class);
    }
    
    /**
     * Test the getPushContext method.
     * Verify that the appropriate PushContext is returned.
     */
    @Test
    public void testGetPushContext()
    {
        PushContextFactory factory = mock(PushContextFactory.class);
        PushContext context = mock(PushContext.class);
        
        PowerMockito.when(PushContextFactory.getDefault()).thenReturn(factory);
        PowerMockito.when(PushContextFactory.getDefault().getPushContext()).thenReturn(context);
        
        assertThat(m_SUT.getPushContext(), is(context));
    }
}
