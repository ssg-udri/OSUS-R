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
package mil.dod.th.ose.transcoder.vlc.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import junit.framework.TestCase;
import mil.dod.th.core.transcoder.TranscoderException;
import mil.dod.th.core.transcoder.TranscoderService;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class TestVlcBundle extends TestCase {

    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    /**
     * Verify that the VLC transcoder bundle is installed/active.
     */
    public void testVlcTranscoderBundleLoaded()
    {
        for (Bundle bundle : m_Context.getBundles())
        {
            final String symName = bundle.getSymbolicName();
            if (symName.startsWith("mil.dod.th.ose.transcoder.vlc") &&
                    !symName.contains("integration"))
            {
                assertThat(bundle.getState(), is(Bundle.ACTIVE));
                return;
            }
        }
        fail("VLC Transcoder bundle is missing");
    }
    
    public void testTranscoderAvailable() throws URISyntaxException, TranscoderException
    {
        TranscoderService transcoderService = ServiceUtils.getService(m_Context, TranscoderService.class);
        assertThat(transcoderService, is(notNullValue()));
        
        transcoderService.start("test id", new URI("rtsp://127.0.0.1:6000"),  new URI("rtp://127.0.0.0.1:8000"), 
                new HashMap<String, Object>());        
    }
}
