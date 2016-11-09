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
package mil.dod.th.ose.gui.webapp.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import mil.dod.th.core.controller.capability.ControllerCapabilities;
import mil.dod.th.core.types.DigitalMedia;

import org.junit.Before;
import org.junit.Test;

/**
 * Test the controller image class.
 * @author matt
 *
 */
public class TestControllerImage
{
    private static final String DEFUALT_CONTROLLER_IMAGE = "thoseIcons/default/controller.png";
    private ControllerImage m_SUT;
    
    @Before
    public void setUp()
    {        
        m_SUT = new ControllerImage();
    }
    
    /**
     * Verify the default controller image is returned.. since the primary
     * image functionality is not working currently.
     */
    @Test
    public void testGetImage()
    {
        ControllerCapabilities controllerCaps = new ControllerCapabilities();
        DigitalMedia digMedia = new DigitalMedia();
        
        controllerCaps.setPrimaryImage(digMedia);
        
        assertThat(m_SUT.getImage(controllerCaps), is(DEFUALT_CONTROLLER_IMAGE));
    }
    
    /**
     * Verify the default controller image is correct.
     */
    @Test
    public void testGetDefaultControllerImage()
    {
        assertThat(m_SUT.getImage(), is(DEFUALT_CONTROLLER_IMAGE));
    }
}
