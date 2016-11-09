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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import mil.dod.th.core.controller.capability.ControllerCapabilities;

import org.junit.Before;
import org.junit.Test;

/**
 * @author nickmarcucci
 *
 */
public class TestControllerInfoDialogHelper
{
    private ControllerInfoDialogHelperImpl m_SUT;
    private ControllerModel m_ControllerModel;
    
    @Before
    public void setUp()
    {
        m_SUT = new ControllerInfoDialogHelperImpl();
        
        m_ControllerModel = mock(ControllerModel.class);
        when(m_ControllerModel.getId()).thenReturn(123);
        when(m_ControllerModel.getName()).thenReturn("Easy");
    }
    
    /**
     * Verify that a controller model for which information is being 
     * requested can be set and retrieved.
     */
    @Test
    public void testGetControllerInfo()
    {       
        m_SUT.setInfoController(m_ControllerModel);
        
        assertThat(m_SUT.getInfoController().getName(), is("Easy"));
        assertThat(m_SUT.getInfoController().getId(), is(123));
    }
    
    /**
     * Confirm Capabilities are correctly.
     */
    @Test
    public void testGetCtlrCaps()
    {
        assertThat(m_SUT.getCtlrCaps(), nullValue());
        
        m_SUT.setInfoController(m_ControllerModel);
        ControllerCapabilities cc = mock(ControllerCapabilities.class);
        when(m_ControllerModel.getCapabilities()).thenReturn(cc);
        assertThat(m_SUT.getCtlrCaps(), is(cc));
    }
}
