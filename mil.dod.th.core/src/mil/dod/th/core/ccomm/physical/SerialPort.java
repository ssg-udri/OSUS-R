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
//
// DESCRIPTION:
// This file contains the SerialPort class.  Generated from Enterprise 
// Architect.  The Enterprise Architect model should be updated for all 
// non-implementation changes (function names, arguments, notes, etc.) and 
// re-synced with the code.
//
//==============================================================================
package mil.dod.th.core.ccomm.physical;

import aQute.bnd.annotation.ProviderType;

import mil.dod.th.core.types.ccomm.FlowControlEnum;
import mil.dod.th.core.types.ccomm.ParityEnum;
import mil.dod.th.core.types.ccomm.StopBitsEnum;

/**
 * Class is a common interface to access a controller's serial port.
 * 
 * @author dhumeniuk
 */
@ProviderType
public interface SerialPort extends PhysicalLink
{
    /**
     * Set serial port specific properties. This method only updates the properties in ConfigurationAdmin and the new
     * values will not be applied to the physical serial port until it is opened. If the port is already open, the new
     * properties will be applied when ConfigurationAdmin completes the update process.
     * 
     * @param baudRate
     *            which baud rate to use for the serial port (e.g., 56k, would be 56000)
     * @param dataBits
     *            how may bits of data per transmission (typically 8)
     * @param parity
     *            what type of parity checking to perform
     * @param stopBits
     *            how many stop bits to use
     * @param flowControl
     *            what type of flow control to use
     * 
     * @throws PhysicalLinkException
     *             thrown if there is a problem setting the properties
     */
    void setSerialPortProperties(int baudRate, int dataBits, ParityEnum parity, StopBitsEnum stopBits, 
            FlowControlEnum flowControl) throws PhysicalLinkException;

    /**
     * Set the data terminal ready (DTR) signal.  Setting the signal is only valid when the port is open.
     * 
     * @param high
     *      true if wanting to set the signal high, false for low
     * @throws IllegalStateException
     *      if method is called when the port is not open
     */
    void setDTR(boolean high) throws IllegalStateException;
    
    @Override
    SerialPortAttributes getConfig();
}
