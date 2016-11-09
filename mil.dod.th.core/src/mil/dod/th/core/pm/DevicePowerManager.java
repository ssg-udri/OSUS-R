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

package mil.dod.th.core.pm;

import java.util.Set;

/**
 * Interface allows peripheral devices to be powered on/off. This interface is implemented by the platform and
 * registered as an OSGi service.
 * 
 * @author dhumeniuk
 */
public interface DevicePowerManager
{
    /**
     * Turn on the device.
     * 
     * @param name
     *      which device to power on
     * @throws PowerManagerException
     *      if unable to turn on device
     * @throws IllegalArgumentException
     *      if give device name isn't available
     */
    void on(String name) throws PowerManagerException, IllegalArgumentException; // NOPMD: short method name,
                                                                                 // name is a full word, 
                                                                                 // just happens to be short

    /**
     * Turn off the device.
     * 
     * @param name
     *      which device to power off
     * @throws PowerManagerException
     *      if unable to turn off device
     * @throws IllegalArgumentException
     *      if give device name isn't available
     */
    void off(String name) throws PowerManagerException, IllegalArgumentException;

    /**
     * Query to see if the device is on or off.
     * 
     * @param name
     *      which device to power off
     * @return
     *      true if the device is on, false if not
     * @throws PowerManagerException
     *      if unable to query power state
     * @throws IllegalArgumentException
     *      if give device name isn't available
     */
    boolean isOn(String name) throws PowerManagerException, IllegalArgumentException;
    
    /**
     * Get a list of all devices by their name.
     * 
     * @return
     *      set of device names
     */
    Set<String> getDevices();
}
