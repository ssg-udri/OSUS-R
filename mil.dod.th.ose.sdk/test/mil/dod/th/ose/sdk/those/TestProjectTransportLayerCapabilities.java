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
package mil.dod.th.ose.sdk.those;

import org.junit.Test;

/**
 * Test class to test the TransportLayerCapabilities class.
 * 
 * @author m.elmo
 * 
 */
public class TestProjectTransportLayerCapabilities
{
    /**
     * Tests that the {@link ProjectTransportLayerCapabilities#getCapabilities()} returns an object containing the
     * correct XML tags.
     */
    @Test
    public void testGetCapabilities()
    {
        // get the byte array of the capabilities xml
        final byte[] capByte = ProjectTransportLayerCapabilities.getCapabilities();

        // read the byte array to a string object, used as the string to test against
        String lineAccum = new String(capByte);

        // assert that each main tag contains the correct sub-tags and/or fields
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:TransportLayerCapabilities",
                "</ns2:TransportLayerCapabilities>", "<primaryImage", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:TransportLayerCapabilities",
                "</ns2:TransportLayerCapabilities>", "<secondaryImages", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:TransportLayerCapabilities",
                "</ns2:TransportLayerCapabilities>", "productName", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:TransportLayerCapabilities",
                "</ns2:TransportLayerCapabilities>", "description", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:TransportLayerCapabilities",
                "</ns2:TransportLayerCapabilities>", "manufacturer", lineAccum);

        ProjectCapabilitiesUtils.assertElement(lineAccum, "ns2:linkLayerModalitiesSupported", "LineOfSight");
        ProjectCapabilitiesUtils.assertElement(lineAccum, "ns2:linkLayerModalitiesSupported", "SATCOM");
        ProjectCapabilitiesUtils.assertElement(lineAccum, "ns2:linkLayerModalitiesSupported", "Cellular");

        ProjectCapabilitiesUtils.assertTestPattern("<primaryImage", "/primaryImage>", "encoding", lineAccum);

        ProjectCapabilitiesUtils.assertTestPattern("<secondaryImages", "/secondaryImages>", "encoding", lineAccum);
        
        // assume most plug-ins are not connection oriented
        ProjectCapabilitiesUtils.assertElement(lineAccum, "ns2:connectionOriented", "false");
    }
}
