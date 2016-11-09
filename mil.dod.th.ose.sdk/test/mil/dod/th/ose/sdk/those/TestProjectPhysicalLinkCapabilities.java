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

import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;

import org.junit.Test;

/**
 * Test class to test the ProjectPhysicalLinkCapabilities class.
 * 
 * @author m.elmo
 * 
 */
public class TestProjectPhysicalLinkCapabilities
{
    /**
     * Tests that the {@link ProjectPhysicalLinkCapabilities#getCapabilities(PhysicalLinkTypeEnum)}
     * returns an object containing the correct XML tags.
     */
    @Test
    public void testGetCapabilities()
    {
        // get the byte array of the capabilities xml
        final byte[] capByte = ProjectPhysicalLinkCapabilities.getCapabilities(PhysicalLinkTypeEnum.SERIAL_PORT);

        // read the byte array to a string object, used as the string to test against
        String lineAccum = new String(capByte);

        // assert that each main tag contains the correct sub-tags and/or fields
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:PhysicalLinkCapabilities", "</ns2:PhysicalLinkCapabilities>",
                "<primaryImage", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:PhysicalLinkCapabilities", "</ns2:PhysicalLinkCapabilities>",
                "<secondaryImages", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:PhysicalLinkCapabilities", "</ns2:PhysicalLinkCapabilities>",
                "linkType", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:PhysicalLinkCapabilities", "</ns2:PhysicalLinkCapabilities>",
                "productName", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:PhysicalLinkCapabilities", "</ns2:PhysicalLinkCapabilities>",
                "description", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:PhysicalLinkCapabilities", "</ns2:PhysicalLinkCapabilities>",
                "manufacturer", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:PhysicalLinkCapabilities", "</ns2:PhysicalLinkCapabilities>",
                "<ns2:serialPort", lineAccum);

        ProjectCapabilitiesUtils.assertTestPattern("<primaryImage", "/primaryImage>", "encoding", lineAccum);

        ProjectCapabilitiesUtils.assertTestPattern("<secondaryImages", "/secondaryImages>", "encoding", lineAccum);

        ProjectCapabilitiesUtils.assertTestPattern("<ns2:serialPort", "</ns2:serialPort>", "baudRateSupported",
                lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:serialPort", "</ns2:serialPort>", "flowControl", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:serialPort", "</ns2:serialPort>", "parity", lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:serialPort", "</ns2:serialPort>", "stopBitsSupported",
                lineAccum);
        ProjectCapabilitiesUtils.assertTestPattern("<ns2:serialPort", "</ns2:serialPort>", "dataBitsSupported",
                lineAccum);
    }
}
