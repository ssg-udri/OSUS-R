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
package mil.dod.th.ose.gui.webapp.channel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import mil.dod.th.ose.gui.webapp.channel.ChannelGuiHelper.ControllerOption;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the channel gui helper bean.
 * @author nickmarcucci
 *
 */
public class TestChannelGuiHelper
{
    private ChannelGuiHelperImpl m_SUT;
    
    @Before
    public void setUp()
    {
        m_SUT = new ChannelGuiHelperImpl();
    }
    
    /**
     * Verify that render option is CHANNEL by default.
     * 
     * Verify that render option can be changed.
     */
    @Test
    public void testRenderOption()
    {
        //default option
        assertThat(m_SUT.getRenderOption(), is(ControllerOption.CHANNEL));
        
        //change and verify 
        m_SUT.setRenderOption(ControllerOption.CONTROLLER);
        
        assertThat(m_SUT.getRenderOption(), is(ControllerOption.CONTROLLER));
    }
}
