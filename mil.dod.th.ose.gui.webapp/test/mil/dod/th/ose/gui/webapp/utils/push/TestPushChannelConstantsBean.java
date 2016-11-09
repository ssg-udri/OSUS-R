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

import org.junit.Before;
import org.junit.Test;

/**
 * Tests that correct channel constants are returned.
 * @author nickmarcucci
 *
 */
public class TestPushChannelConstantsBean
{
    private PushChannelConstantsBean m_SUT; 
    
    @Before
    public void init()
    {
        m_SUT = new PushChannelConstantsBean();
    }
    
    /**
     * Verify that event channel can be correctly retrieved.
     */
    @Test
    public void testGetEventChannel()
    {
        assertThat(m_SUT.getMessageChannel(), is(PushChannelConstants.PUSH_CHANNEL_THOSE_MESSAGES));
    }
}
