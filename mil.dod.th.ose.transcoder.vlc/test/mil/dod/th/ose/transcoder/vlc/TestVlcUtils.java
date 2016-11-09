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
package mil.dod.th.ose.transcoder.vlc;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import mil.dod.th.core.transcoder.TranscoderService;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


/**
 * Test the methods of the helper class {@link mil.dod.th.ose.transcoder.vlc.VlcUtils}.
 * 
 * @author jmiller
 *
 */
public class TestVlcUtils
{
    
    final private String m_MulticastHost = "225.1.2.3";
    
    final private int m_MulticastPort = 5000;
    
    final private double m_TranscodeBitrateKbps = 10.0;
    
    final private double m_NoTranscode = -1.0;
    
    final private String m_MimeTypeMp4 = "video/mp4";
    
    final private String m_MimeTypeMpeg = "video/mpeg";
    
    private URI m_MulticastUri;
        
    @Before
    public void setUp() throws URISyntaxException
    {
        m_MulticastUri = new URI(null, null, m_MulticastHost, m_MulticastPort, null, null, null);
    }
    
    /**
     * Verify that the proper configuration string is created when the bitrate is negative, which
     * implies no transcoding should be done.
     */
    @Test
    public void testCreateConfigStringNoTranscoding()
    {
        final Map<String, Object> configParams = new HashMap<>();
        configParams.put(TranscoderService.CONFIG_PROP_BITRATE_KBPS, m_NoTranscode);
        configParams.put(TranscoderService.CONFIG_PROP_FORMAT, m_MimeTypeMp4);
        assertThat(VlcUtils.createConfigString(m_MulticastUri, configParams), 
                is(":sout=#rtp{dst=225.1.2.3,port=5000,mux=ts,ttl=128}"));
    }
    
    /**
     * Verify that the proper configuration string is created for transcoding to multiple
     * output formats.
     */
    @Test
    public void testCreateConfigStringWithTranscoding()
    {
        final Map<String, Object> configParams1 = new HashMap<>();
        configParams1.put(TranscoderService.CONFIG_PROP_BITRATE_KBPS, m_TranscodeBitrateKbps);
        configParams1.put(TranscoderService.CONFIG_PROP_FORMAT, m_MimeTypeMp4);
        assertThat(VlcUtils.createConfigString(m_MulticastUri, configParams1), 
                is(":sout=#transcode{vcodec=mp4v,vb=10.0,acodec=none}:rtp{dst=225.1.2.3,port=5000,mux=ts,ttl=128}"));
        
        final Map<String, Object> configParams2 = new HashMap<>();
        configParams2.put(TranscoderService.CONFIG_PROP_BITRATE_KBPS, m_TranscodeBitrateKbps);
        configParams2.put(TranscoderService.CONFIG_PROP_FORMAT, m_MimeTypeMpeg);
        assertThat(VlcUtils.createConfigString(m_MulticastUri, configParams2), 
                is(":sout=#transcode{vcodec=mp2v,vb=10.0,acodec=none}:rtp{dst=225.1.2.3,port=5000,mux=ts,ttl=128}"));
    }
}
