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

import org.junit.Before;
import org.junit.Test;

import mil.dod.th.core.controller.capability.ControllerCapabilities;
import mil.dod.th.core.types.DigitalMedia;
import mil.dod.th.ose.gui.webapp.controller.ControllerImage;
import mil.dod.th.ose.gui.webapp.controller.ControllerModel;

/**
 * Test class for the ControllerModel.
 * @author nickmarcucci
 *
 */
public class TestControllerModel
{
    private static final String CONTROLLER_ICON_DFLT = "thoseIcons/default/controller.png";
    
    private ControllerModel m_SUT;
    private ControllerImage m_ControllerImageInterface;
    
    @Before
    public void setUp()
    {
        m_ControllerImageInterface = new ControllerImage();
        m_SUT = new ControllerModel(123, m_ControllerImageInterface);
    }
    
    /**
     * Verify that controller comes back with correct 
     * hex conversion.
     */
    @Test
    public void testHexId()
    {
        assertThat(m_SUT.getHexId(), is("0x0000007B"));
    }
    
    /**
     * Verify get image returns the correct default controller image.
     */
    @Test
    public void testGetImage()
    {
        ControllerCapabilities caps = new ControllerCapabilities();
        DigitalMedia controllerPrimImage = new DigitalMedia();
        caps.setPrimaryImage(controllerPrimImage);
        
        m_SUT.setCapabilities(caps);
        assertThat(m_SUT.getImage(), is(CONTROLLER_ICON_DFLT));
    }
}
