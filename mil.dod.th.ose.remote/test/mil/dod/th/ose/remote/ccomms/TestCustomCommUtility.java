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
package mil.dod.th.ose.remote.ccomms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.remote.proto.SharedMessages.UUID;
import mil.dod.th.ose.remote.comms.CustomCommUtility;
import mil.dod.th.ose.shared.SharedMessageUtils;

import org.junit.Test;

/**
 * Verify the get link layer and get transport layer by uuid takes in a proto uuid converts it to a java 
 * uuid and looks in the custom comms service for an object with that uuid.
 * @author matt
 */
public class TestCustomCommUtility
{
    private CustomCommsService m_CustomCommsService;
    
    /**
     * Verify using the utility the user can retrieve a link layer by the proto uuid that is in the custom
     * comms service.
     */
    @Test
    public void testGetLinkLayerByUuid()
    {
        m_CustomCommsService = mock(CustomCommsService.class);
        
        java.util.UUID javaUuid = java.util.UUID.randomUUID();
        UUID uuid = SharedMessageUtils.convertUUIDToProtoUUID(javaUuid);
        
        LinkLayer testLink = mock(LinkLayer.class);
        when(testLink.getUuid()).thenReturn(javaUuid);
        
        List<LinkLayer> linkList = new ArrayList<LinkLayer>();
        linkList.add(testLink);
        
        when(m_CustomCommsService.getLinkLayers()).thenReturn(linkList);
        
        assertThat(CustomCommUtility.getLinkLayerByUuid(m_CustomCommsService, uuid), is(testLink));
    }
    
    /**
     * Verify using the utility the user can retrieve a transport layer by the proto uuid that is in the custom
     * comms service.
     */
    @Test
    public void testGetTransportLayerByUuid()
    {
        m_CustomCommsService = mock(CustomCommsService.class);
        
        java.util.UUID javaUuid = java.util.UUID.randomUUID();
        UUID uuid = SharedMessageUtils.convertUUIDToProtoUUID(javaUuid);
        
        TransportLayer testTransport = mock(TransportLayer.class);
        when(testTransport.getUuid()).thenReturn(javaUuid);
        
        List<TransportLayer> transportList = new ArrayList<TransportLayer>();
        transportList.add(testTransport);
        
        when(m_CustomCommsService.getTransportLayers()).thenReturn(transportList);
        
        assertThat(CustomCommUtility.getTransportLayerByUuid(m_CustomCommsService, uuid), is(testTransport));
    }
}
