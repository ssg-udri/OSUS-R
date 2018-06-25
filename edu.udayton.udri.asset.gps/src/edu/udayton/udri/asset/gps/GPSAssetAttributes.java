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
package edu.udayton.udri.asset.gps;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

import mil.dod.th.core.ConfigurationConstants;
import mil.dod.th.core.asset.AssetAttributes;

/**
 * Interface which defines the configurable properties for GPSAsset.
 */
@OCD (description = ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION)
public interface GPSAssetAttributes extends AssetAttributes
{
    /**
     * The port constant of the GPS serial port.
     */
    String CONFIG_PROP_PHYS_PORT = "serial.port";
    
    /**
     * The baud rate constant of the GPS serial port.
     */
    String CONFIG_PROP_BAUD_RATE = "baud.rate";
    
    /**
     * The number of bits per transmission.
     */
    String CONFIG_PROP_DATA_BITS = "data.bits";
    
    /**
     * Name of the comm link.
     */
    String CONFIG_PROP_PHYS_LINK_NAME = "physical.link";
    
    /**
     * Configuration property for the baud rate to be applied to the physical port.
     * 
     * @return
     *      Integer that represents the baud rate to be applied.
     */
    @AD(id = CONFIG_PROP_BAUD_RATE, name = "Baud Rate", description = "The baud rate to be applied to the serial port "
            + "connection.", deflt = "4800", min = "0", max = "230400", required = false)
    int baudRate();
    
    /**
     * Configuration property for the data bits to be applied to the physical port.
     * 
     * @return
     *      Integer that represents the data bits to be applied.
     */
    @AD(id = CONFIG_PROP_DATA_BITS, name = "Data Bits", description = "Data bits"
            + "connection.", deflt = "8", min = "0", max = "8", required = false)
    int dataBits();
    
    /**
     * Configuration property for the comm port name to be applied physical link.
     * 
     * @return
     *      String that represents the name of the comm link.
     */
    @AD(id = CONFIG_PROP_PHYS_LINK_NAME, name = "Physical Link Name", description = "The name of the comm link.",
            deflt = "COM3", required = false)
    String physLinkName();
}
