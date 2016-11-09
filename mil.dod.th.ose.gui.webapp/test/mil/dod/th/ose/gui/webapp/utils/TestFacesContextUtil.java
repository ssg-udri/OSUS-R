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
package mil.dod.th.ose.gui.webapp.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import javax.faces.context.FacesContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.primefaces.context.RequestContext;

/**
 * Test class for {@link FacesContextUtil}.
 * 
 * @author cweisenborn
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({FacesContext.class, RequestContext.class})
public class TestFacesContextUtil
{
    private FacesContextUtil m_SUT;
    
    @Before
    public void setup()
    {
        m_SUT = new FacesContextUtil();
        
        PowerMockito.mockStatic(FacesContext.class);
        PowerMockito.mockStatic(RequestContext.class);
    }
    
    /**
     * Test the getFacesContext method.
     * Verify that the appropriate FacesContext is returned.
     */
    @Test
    public void testGetFacesContext()
    {
        
        FacesContext context = mock(FacesContext.class);
        PowerMockito.when(FacesContext.getCurrentInstance()).thenReturn(context);
        
        assertThat(m_SUT.getFacesContext(), is(context));
    }
    
    /**
     * Test the getRequestContext method.
     * Verify the appropriate RequestContext is returned.
     */
    @Test
    public void testGetRequestContext()
    {
        RequestContext context = mock(RequestContext.class);
        PowerMockito.when(RequestContext.getCurrentInstance()).thenReturn(context);
        
        assertThat(m_SUT.getRequestContext(), is(context));
    }
}
