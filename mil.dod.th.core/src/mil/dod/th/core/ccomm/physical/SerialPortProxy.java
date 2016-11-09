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

/**
 * A serial port plug-in implements this interface providing any custom behavior of the plug-in that is needed to 
 * interact with the serial port instead of the base {@link PhysicalLinkProxy} interface.
 * 
 * @author dhumeniuk
 *
 */
public interface SerialPortProxy extends PhysicalLinkProxy
{
    /**
     * Set the data terminal ready (DTR) signal.  Setting the signal is only valid when the port is open.
     * 
     * @param high
     *      true if wanting to set the signal high, false for low
     * @throws IllegalStateException
     *      if method is called when the port is not open
     * @throws UnsupportedOperationException
     *      if the platform does not support this operation
     */
    void setDTR(boolean high) throws IllegalStateException, UnsupportedOperationException;
}
