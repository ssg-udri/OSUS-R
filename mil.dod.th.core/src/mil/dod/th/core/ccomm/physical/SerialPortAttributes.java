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
package mil.dod.th.core.ccomm.physical;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

import mil.dod.th.core.ConfigurationConstants;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.types.ccomm.FlowControlEnum;
import mil.dod.th.core.types.ccomm.ParityEnum;
import mil.dod.th.core.types.ccomm.StopBitsEnum;

/**
 * Defines metadata of properties that are available to all {@link SerialPort}s. Retrieve properties using {@link 
 * SerialPort#getConfig()}.
 * 
 * @author dhumeniuk
 *
 */
@OCD (description = ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION)
public interface SerialPortAttributes extends PhysicalLinkAttributes
{
    /**
     * Property key for {@link #baudRate()}.
     */
    String CONFIG_PROP_BAUD_RATE = FactoryObject.TH_PROP_PREFIX + ".baud.rate";

    /**
     * Property key for {@link #parity()}.
     */
    String CONFIG_PROP_PARITY = FactoryObject.TH_PROP_PREFIX + ".parity";

    /**
     * Property key for {@link #stopBits()}.
     */
    String CONFIG_PROP_STOP_BITS = FactoryObject.TH_PROP_PREFIX + ".stop.bits";

    /**
     * Property key for {@link #flowControl()}.
     */
    String CONFIG_PROP_FLOW_CONTROL = FactoryObject.TH_PROP_PREFIX + ".flow.control";

    /** 
     * Configuration property for the baud rate of the serial port.
     * 
     * @return baud rate (bits per second)
     */
    @AD(required = false, deflt = "9600", id = CONFIG_PROP_BAUD_RATE,
        description = "How many bits of data in each byte read from the link")
    int baudRate();
    
    /** 
     * Configuration property for the parity to use for the serial port.
     * 
     * @return parity type
     */
    @AD(required = false, deflt = "NONE", id = CONFIG_PROP_PARITY,
        description = "Type of parity to use for the serial port")
    ParityEnum parity();
    
    /** 
     * Configuration property for the number of stop bits between each byte.
     * 
     * @return stop bits
     */
    @AD(required = false, deflt = "ONE_STOP_BIT", id = CONFIG_PROP_STOP_BITS,
        description = "Number of stop bits between each byte")
    StopBitsEnum stopBits();
    
    /** 
     * Configuration property for the flow control to use for the serial port.
     * 
     * @return stop bits
     */
    @AD(required = false, deflt = "NONE", id = CONFIG_PROP_FLOW_CONTROL,
        description = "Type of flow control to use for the serial port")
    FlowControlEnum flowControl();
}
