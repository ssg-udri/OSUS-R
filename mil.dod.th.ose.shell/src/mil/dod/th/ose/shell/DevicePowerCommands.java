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
package mil.dod.th.ose.shell;

import java.io.PrintStream;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.pm.DevicePowerManager;
import mil.dod.th.core.pm.PowerManagerException;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;

/**
 * Implements a command to access power management functionality for the Felix Shell Service.
 * 
 * @author dlandoll
 */
@Component(provide = DevicePowerCommands.class, properties = { 
        "osgi.command.scope=thpower",
        "osgi.command.function=dev|devs" })
public class DevicePowerCommands
{
    /**
     * Device power manager service.
     */
    private DevicePowerManager m_DevicePowerManager;

    /**
     * Bind a device power manager service.
     * 
     * @param devicePowerManager
     *            service to manage device power
     */
    @Reference
    public void setDevicePowerManager(final DevicePowerManager devicePowerManager)
    {
        m_DevicePowerManager = devicePowerManager;
    }

    /**
     * Control/view device power toggle services.
     * 
     * @param session
     *            provides access to the Gogo shell session
     * @param turnOn
     *            indicates whether the given device should be turned on
     * @param turnOff
     *            indicates whether the given device should be turned off
     * @param name
     *            device to turn on/off or get status of
     * @throws PowerManagerException
     *             error thrown by power manager
     * @throws IllegalArgumentException
     *             if invalid name is given
     */
    @Descriptor("Manages device power state")
    public void dev(
            final CommandSession session,
            @Descriptor("Flag used to turn a device on")
            @Parameter(names = { "-o", "--on" }, presentValue = "true", absentValue = "false")
            final boolean turnOn,
            @Descriptor("Flag used to turn a device on")
            @Parameter(names = { "-f", "--off" }, presentValue = "true", absentValue = "false")
            final boolean turnOff,
            @Descriptor("Device name") final String name) throws IllegalArgumentException, PowerManagerException
    {
        final PrintStream out = session.getConsole();

        if (turnOn)
        {
            m_DevicePowerManager.on(name);
        }
        else if (turnOff)
        {
            m_DevicePowerManager.off(name);
        }
        else
        {
            out.format("Is on? %s%n", m_DevicePowerManager.isOn(name));
        }
    }

    /**
     * Displays the power management devices.
     * 
     * @param session
     *            provides access to the Gogo shell session
     * @throws PowerManagerException
     *             error thrown by power manager
     * @throws IllegalArgumentException
     *             if invalid name is given
     */
    @Descriptor("Displays all registered devices with the power manager")
    public void devs(final CommandSession session) throws IllegalArgumentException, PowerManagerException
    {
        final PrintStream out = session.getConsole();

        for (String name : m_DevicePowerManager.getDevices())
        {
            out.format("%s: is on? %s%n", name, m_DevicePowerManager.isOn(name));
        }
    }
}
