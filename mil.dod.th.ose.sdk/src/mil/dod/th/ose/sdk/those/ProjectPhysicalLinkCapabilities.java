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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mil.dod.th.core.ccomm.physical.capability.PhysicalLinkCapabilities;
import mil.dod.th.core.ccomm.physical.capability.SerialPort;
import mil.dod.th.core.types.DigitalMedia;
import mil.dod.th.core.types.ccomm.FlowControlEnum;
import mil.dod.th.core.types.ccomm.ParityEnum;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.core.types.ccomm.StopBitsEnum;
import mil.dod.th.ose.utils.xml.XmlUtils;

/**
 * Utility class to create a capabilities xml byte array for physical layer.
 * 
 * @author m.elmo
 */
public final class ProjectPhysicalLinkCapabilities
{
    /**
     * Default private constructor to prevent instantiation.
     */
    private ProjectPhysicalLinkCapabilities()
    {
    }

    /**
     * Creates a capabilities object and converts that object to xml in the form of a byte array.
     * @param physType
     *  the type of the physical link 
     * 
     * @return The byte array containing the capabilities xml.
     */
    public static byte[] getCapabilities(final PhysicalLinkTypeEnum physType)
    {
        final PhysicalLinkCapabilities cap = makeCapabilities(physType);
        return XmlUtils.toXML(cap, true);
    }

    /**
     * Creates an object that is populated with placeholder values for a Capabilities object.
     * @param physType
     *  the type of the physical link
     * 
     * @return Capabilities object
     */
    private static PhysicalLinkCapabilities makeCapabilities(final PhysicalLinkTypeEnum physType)
    {
        final DigitalMedia primaryImage = ProjectBaseCapabilities.makePrimaryImage();
        final List<DigitalMedia> secondaryImages = ProjectBaseCapabilities.makeSecondaryImages();

        final List<Integer> baudRatesSupported = makeBaudRatesSupported();
        final List<FlowControlEnum> flowControl = Arrays.asList(FlowControlEnum.values());
        final List<ParityEnum> parity = Arrays.asList(ParityEnum.values());
        final List<StopBitsEnum> stopBitsSupported = Arrays.asList(StopBitsEnum.values());
        final List<Integer> dataBitsSupported = makeDataBitsSupported();

        final SerialPort serialPort = new SerialPort(baudRatesSupported, flowControl, parity, stopBitsSupported,
                dataBitsSupported);

        final String productName = ProjectBaseCapabilities.getProductName();
        final String description = ProjectBaseCapabilities.getDescription();
        final String manufacturer = ProjectBaseCapabilities.getManufacturer();

        return new PhysicalLinkCapabilities(primaryImage, secondaryImages, productName, description, manufacturer,
                serialPort, physType);
    }

    /**
     * Creates an List that is populated with Integers representing the supported BaudRates.
     * 
     * @return List of Supported Baud Rate Integers
     */
    private static List<Integer> makeBaudRatesSupported()
    {
        final ArrayList<Integer> baudRatesSupported = new ArrayList<Integer>();
        baudRatesSupported.add(9600); // NOCHECKSTYLE (magic number): just some random baud rates, no special meaning
        baudRatesSupported.add(57600); // NOCHECKSTYLE
        baudRatesSupported.add(112500); // NOCHECKSTYLE
        return baudRatesSupported;
    }

    /**
     * Creates an List of Integers representing the supported data bits.
     * 
     * @return List of Integers.
     */
    private static List<Integer> makeDataBitsSupported()
    {
        final List<Integer> dataBitsSupported = new ArrayList<Integer>();
        dataBitsSupported.add(5); // NOCHECKSTYLE not a magic number. Acceptable data bit value.
        dataBitsSupported.add(6); // NOCHECKSTYLE not a magic number. Acceptable data bit value.
        dataBitsSupported.add(7); // NOCHECKSTYLE not a magic number. Acceptable data bit value.
        dataBitsSupported.add(8); // NOCHECKSTYLE not a magic number. Acceptable data bit value.
        dataBitsSupported.add(9); // NOCHECKSTYLE not a magic number. Acceptable data bit value.
        return dataBitsSupported;
    }

}
