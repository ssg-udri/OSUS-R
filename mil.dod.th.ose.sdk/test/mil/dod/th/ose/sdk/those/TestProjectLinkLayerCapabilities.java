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
 * Test class to test the LinkLayerCapabilities class.
 * 
 * @author m.elmo
 * 
 */
public class TestProjectLinkLayerCapabilities
{
    /**
     * Tests that the {@link ProjectLinkLayerCapabilities#getCapabilities()} returns an object containing the correct
     * XML tags.
     */
    @Test
    public void testGetCapabilities()
    {
        // get the byte array of the capabilities xml
        final byte[] capByte = ProjectLinkLayerCapabilities.getCapabilities();

        // read the byte array to a string object, used as the string to test against
        String lineAccum = new String(capByte);

        // assert that each main tag contains the correct sub-tags and/or fields
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:LinkLayerCapabilities", "</ns2:LinkLayerCapabilities>",
                "<primaryImage", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:LinkLayerCapabilities", "</ns2:LinkLayerCapabilities>",
                "<secondaryImages", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:LinkLayerCapabilities", "</ns2:LinkLayerCapabilities>",
                "productName", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:LinkLayerCapabilities", "</ns2:LinkLayerCapabilities>",
                "description", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:LinkLayerCapabilities", "</ns2:LinkLayerCapabilities>",
                "manufacturer", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:LinkLayerCapabilities", "</ns2:LinkLayerCapabilities>", 
                "staticMtu", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:LinkLayerCapabilities", "</ns2:LinkLayerCapabilities>", 
                "mtu", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:LinkLayerCapabilities", "</ns2:LinkLayerCapabilities>",
                "modality", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:LinkLayerCapabilities", "</ns2:LinkLayerCapabilities>",
                "physicalLinkRequired", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:LinkLayerCapabilities", "</ns2:LinkLayerCapabilities>",
                "performBITSupported", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:LinkLayerCapabilities", "</ns2:LinkLayerCapabilities>",
                "supportsAddressing", lineAccum);
        
        ProjectCapabilitiesUtils.assertTestPattern("<primaryImage", "/primaryImage>", "encoding", lineAccum);

        ProjectCapabilitiesUtils.assertTestPattern("<secondaryImages", "/secondaryImages>", "encoding", lineAccum);

        ProjectCapabilitiesUtils.assertTestPattern("<ns2:physicalLinksSupported>", "</ns2:physicalLinksSupported>", 
                "SerialPort", lineAccum);
    }
}
