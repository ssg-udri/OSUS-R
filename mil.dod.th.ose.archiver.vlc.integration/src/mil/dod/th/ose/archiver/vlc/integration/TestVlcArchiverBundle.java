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
package mil.dod.th.ose.archiver.vlc.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.net.URI;
import java.net.URISyntaxException;

import mil.dod.th.core.archiver.ArchiverException;
import mil.dod.th.core.archiver.ArchiverService;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import junit.framework.TestCase;

public class TestVlcArchiverBundle extends TestCase {
    
    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    /**
     * Verify that the VLC transcoder bundle is installed/active.
     */
    public void testVlcArchiverBundleLoaded()
    {
        for (Bundle bundle : m_Context.getBundles())
        {
            final String symName = bundle.getSymbolicName();
            if (symName.startsWith("mil.dod.th.ose.archiver.vlc") &&
                    !symName.contains("integration"))
            {
                assertThat(bundle.getState(), is(Bundle.ACTIVE));
                return;
            }
        }
        fail("VLC Archiver bundle is missing");
    }
    
    public void testArchiverAvailable() throws URISyntaxException, ArchiverException
    {
        ArchiverService archiverService = ServiceUtils.getService(m_Context, ArchiverService.class);
        assertThat(archiverService, is(notNullValue()));
        
        archiverService.start("test id", new URI("rtsp://127.0.0.1:6000"),  "/top/level/dir/file");
        
    }
    

}
