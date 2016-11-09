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
package example.device.power;

import java.util.HashSet;
import java.util.Set;

import aQute.bnd.annotation.component.Component;

import mil.dod.th.core.pm.DevicePowerManager;
import mil.dod.th.core.pm.PowerManagerException;

/**
 * @author dhumeniuk
 *
 */
@Component
public class ExampleDevicePowerManager implements DevicePowerManager
{

    private static final Set<String> DEVICES = new HashSet<String>();
    static
    {
        DEVICES.add("exampleA");
        DEVICES.add("exampleB");
        DEVICES.add("exampleC");
    }

    @Override
    public void on(String name) throws PowerManagerException, IllegalArgumentException
    {
        if (!DEVICES.contains(name))
        {
            throw new IllegalArgumentException(name + " is not a valid device name");
        }
    }

    @Override
    public void off(String name) throws PowerManagerException, IllegalArgumentException
    {
        if (!DEVICES.contains(name))
        {
            throw new IllegalArgumentException(name + " is not a valid device");
        }
    }

    @Override
    public boolean isOn(String name) throws PowerManagerException, IllegalArgumentException
    {
        if (!DEVICES.contains(name))
        {
            throw new IllegalArgumentException(name + " is not a valid device");
        }
        
        return false;
    }

    @Override
    public Set<String> getDevices()
    {
        return DEVICES;
    }   
}
