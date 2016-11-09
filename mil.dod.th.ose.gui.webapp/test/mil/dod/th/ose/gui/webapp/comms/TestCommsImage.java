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
package mil.dod.th.ose.gui.webapp.comms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import mil.dod.th.core.ccomm.link.capability.LinkLayerCapabilities;
import mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities;
import mil.dod.th.core.types.DigitalMedia;
import mil.dod.th.core.types.ccomm.LinkLayerTypeEnum;

import org.junit.Before;
import org.junit.Test;

/**
 * Test the comms image class.
 * @author matt
 *
 */
public class TestCommsImage
{
    private static final String COMMS_ICON_GENERIC = "thoseIcons/comms/comms_generic.png";
    private static final String COMMS_ICON_CELLULAR = "thoseIcons/comms/comms_cellular.png";
    private static final String COMMS_ICON_SERIAL = "thoseIcons/comms/comms_serial.png";
    private static final String COMMS_ICON_SOCKET = "thoseIcons/comms/socket.png";
    
    private CommsImage m_SUT;
    
    @Before
    public void setUp()
    {
        m_SUT = new CommsImage();
    }
    
    /**
     * Verify the get transport image works as expected.
     */
    @Test
    public void getTransportImage()
    {
        TransportLayerCapabilities transportCaps = new TransportLayerCapabilities();
        DigitalMedia digMedia = new DigitalMedia();
        transportCaps.setPrimaryImage(digMedia);
        
        assertThat(m_SUT.getTransportImage(transportCaps), is(COMMS_ICON_GENERIC));
        
        TransportLayerCapabilities transportCapsSupported = mock(TransportLayerCapabilities.class);
        List<LinkLayerTypeEnum> supportedModalities = new ArrayList<LinkLayerTypeEnum>();
        supportedModalities.add(LinkLayerTypeEnum.CELLULAR);
        
        assertThat(m_SUT.getTransportImage(transportCapsSupported), is(COMMS_ICON_GENERIC));
        when(transportCapsSupported.getLinkLayerModalitiesSupported()).thenReturn(supportedModalities);
        
        assertThat(m_SUT.getTransportImage(transportCapsSupported), is(COMMS_ICON_CELLULAR));
    }
    
    /**
     * Verify the get link layer image works as expected.
     */
    @Test
    public void getLinkLayerImage()
    {
        LinkLayerCapabilities linkLayerCaps = new LinkLayerCapabilities();
        DigitalMedia digMedia = new DigitalMedia();
        linkLayerCaps.setPrimaryImage(digMedia);
        
        assertThat(m_SUT.getLinkLayerImage(linkLayerCaps), is(COMMS_ICON_GENERIC));
        
        LinkLayerCapabilities linkLayerCapsMods = mock(LinkLayerCapabilities.class);
        when(linkLayerCapsMods.getModality()).thenReturn(LinkLayerTypeEnum.CELLULAR);
        
        assertThat(m_SUT.getLinkLayerImage(linkLayerCapsMods), is(COMMS_ICON_CELLULAR));
    }
    
    /**
     * Verify the get serial image returns the correct image.
     */
    @Test
    public void testGetSerialImage()
    {
        assertThat(m_SUT.getPhysicalLinkImage(), is(COMMS_ICON_SERIAL));
    }
    
    /**
     * Verify the get generic image returns the correct image.
     */
    @Test
    public void testGetGenericImage()
    {
        assertThat(m_SUT.getImage(), is(COMMS_ICON_GENERIC));
    }
    
    /**
     * Verify the get socket image returns the correct image.
     */
    @Test
    public void testGetSocketImage()
    {
        assertThat(m_SUT.getSocketImage(), is(COMMS_ICON_SOCKET));
    }
}
