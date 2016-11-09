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
package mil.dod.th.ose.core.xml;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.net.MalformedURLException;
import java.net.URL;

import mil.dod.th.core.asset.commands.SetPanTiltCommand;

import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * @author allenchl
 *
 */
public class TestXsdResourceFinder
{
    private BundleContext m_BundleContext = mock(BundleContext.class);

    /**
     * Verify looking up and returning of a URL resource given the bundle and class. 
     */
    @Test
    public void testGetXsdResource() throws MalformedURLException
    {
        Bundle bundle = mock(Bundle.class);
        URL schemaUrl = new URL("file:schema.xsd");
        when(m_BundleContext.getBundle()).thenReturn(bundle);
        when(bundle.getResource(Mockito.anyString())).thenReturn(schemaUrl);
        
        assertThat(XsdResourceFinder.getXsdResource(
                m_BundleContext, SetPanTiltCommand.class), is(schemaUrl));
    }
    
    /**
     * Verify that if the resource cannot be found that there is an exception. 
     */
    @Test
    public void testGetXsdResourceIllegalState() throws MalformedURLException
    {
        Bundle bundle = mock(Bundle.class);
        when(m_BundleContext.getBundle()).thenReturn(bundle);
        when(bundle.getResource(Mockito.anyString())).thenReturn(null);
        
        try
        {
            XsdResourceFinder.getXsdResource(m_BundleContext, SetPanTiltCommand.class);
            fail("Expected exception resource should be null per mocking.");
        }
        catch(IllegalStateException e)
        {
            
        }              
    }
}
