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

import java.io.IOException;
import java.io.PrintStream;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.pm.PlatformPowerManager;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.core.pm.WakeLockState;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Power manager commands.
 * 
 * @author cweisenborn
 *
 */
@Component(provide = PowerManagerCommands.class, properties = {"osgi.command.scope=thpower", 
        "osgi.command.function=enable|disable|getWakeLocks|getWakeLock"
        + "|getWakeLockContexts|getWakeLocksByContext|info|createWakeLock"
        + "|startwl|stopwl" })
public class PowerManagerCommands
{

    
    /** ID used when creating the shell wake lock. **/
    static final private String SHELL_LOCK_ID = "shellWakeLock";
    
    /**
     * Reference to power manager service.
     */
    private PowerManager m_PowerManager;
    
    /**
     * Reference to the platform power manager service.
     */
    private PlatformPowerManager m_PlatformPowerManager;
    
    /**
     * Reference to the configuration admin service.
     */
    private ConfigurationAdmin m_ConfigAdmin;
    
    /**
     * String name used when creating a WakeLock for the shell.
     */
    private WakeLock m_ShellLock;
    
    /**
     * Sets the power manager service.
     * 
     * @param powerManager
     *              service to be set
     */
    @Reference
    public void setPowerManager(final PowerManager powerManager)
    {
        m_PowerManager = powerManager;
    }
    
    /**
     * Sets the platform power manager service.
     * 
     * @param platformPowerManager
     *              service to be set
     */
    @Reference(optional = true)
    public void setPlatformPowerManager(final PlatformPowerManager platformPowerManager)
    {
        m_PlatformPowerManager = platformPowerManager;
    }
    
    /**
     * Sets the configuration admin service.
     * @param configAdmin
     *              the configuration service to be set
     */
    @Reference
    public void setConfigurationAdmin(final ConfigurationAdmin configAdmin)
    {
        m_ConfigAdmin = configAdmin;
    }
    
    /**
     * Activate the component, creates shell wake lock.
     */
    @Activate
    public void activate()
    {
        m_ShellLock = m_PowerManager.createWakeLock(this.getClass(), SHELL_LOCK_ID);
    }
    
    /**
     * Deactivates the component, deletes the shell wake lock.
     */
    @Deactivate
    public void deactivate()
    {
        m_ShellLock.delete();
    }
    
    /**
     * Method to get the set of {@link WakeLock}s that are of a given state.
     * @param state
     *  the given {@link WakeLockState} for which the {@link WakeLock}s should be in
     * @return
     *  the set of {@link WakeLock}s that are in the given state
     */
    @Descriptor("Retrieves a set of WakeLocks which are in a given state")
    public Set<WakeLock> getWakeLocks(
            @Descriptor("The desired WakeLockState. Valid inputs: Any, Scheduled, Inactive, Active")
            final String state)
    {
        final WakeLockState lockState = WakeLockState.valueOf(state);
        return m_PowerManager.getWakeLocks(lockState);
    }
    
    /**
     * Method to get the set of {@link WakeLock} that are of a given state and are associated with
     * the given {@link FactoryObject}.
     * @param sourceObj
     *  the {@link FactoryObject} for which all returned locks are associated with
     * @param state
     *  the given {@link WakeLockState} for which the {@link WakeLock}s should be in
     * @return
     *  the set of {@link WakeLock}s that are in the given state and are associated with 
     *  the given {@link FactoryObject} 
     */
    @Descriptor("Retrieves the set of WakeLocks which are associated with the "
            + "given factory object and are in the given state.")
    public Set<WakeLock> getWakeLocks(
            @Descriptor("The factory object which is to be used to retrieve "
                    + "all locks of a given state that are associated with it.")
            final FactoryObject sourceObj, 
            @Descriptor("The desired WakeLockState. Valid inputs: Any, Scheduled, Inactive, Active")
            final String state)
    {
        final WakeLockState lockState = WakeLockState.valueOf(state);
        return m_PowerManager.getWakeLocks(sourceObj, lockState);
    }
    
    /**
     * Method to get an individual {@link WakeLock} given a fully qualified class name 
     * in string form which is the context and an id.
     * @param session
     *  command session that is executing the command
     * @param className
     *  the class name (getName()) which is used as the context to identify a {@link WakeLock}
     * @param wakeLockId
     *  the id of the desired {@link WakeLock}
     * @return
     *  the {@link WakeLock}(s) that match(es) the context and the id or an empty set if no matches are found
     */
    @Descriptor("Retrieves a WakeLock given the class name (getName()) and a given WakeLock ID")
    public Set<WakeLock> getWakeLocksByContext(
            final CommandSession session,
            @Descriptor("The class name (getName()) which is used as the context for identifying a WakeLock.")
            final String className, 
            @Descriptor("The id of the desired WakeLock.")
            final String wakeLockId)
    {
        
        final Set<Class<?>> knownContexts = m_PowerManager.getWakeLockContexts();
        final Set<WakeLock> locksToReturn = new HashSet<>();
        final PrintStream out = session.getConsole();
        final String outString = "Cannot find a WakeLock with the given context: %s and id %s";
        
        for (Class<?> context : knownContexts)
        {
            if (className.equals(context.getName()))
            {
                final Set<WakeLock> locks = m_PowerManager.getWakeLocks(context, WakeLockState.Any);
                
                for (WakeLock lock : locks)
                {
                    if (lock.getId().equals(wakeLockId))
                    {
                        locksToReturn.add(lock);
                    }
                }
                
                if (locksToReturn.size() == 0)
                {
                    out.format(outString, className, wakeLockId);
                }
                
                return locksToReturn;
            }
        }
        
        out.format(outString, className, wakeLockId);
        
        return locksToReturn;
    }
    
    
    /**
     * Method used to retrieve a desired {@link WakeLock} which has the given lock ID and is associated with
     * the {@link FactoryObject}.
     * @param session
     *  command session that is executing the command
     * @param sourceObj
     *  the {@link FactoryObject} for which the desired lock is associated with
     * @param lockId
     *  the id of the desired {@link WakeLock}
     * @return
     *  the {@link WakeLock} that is associated with the given {@link FactoryObject} and has the given id
     */
    @Descriptor("Retrieves a WakeLock with the given id that is associated with the specified factory object")
    public WakeLock getWakeLock(final CommandSession session, 
            @Descriptor("The factory object which is to be used to retrieve "
                    + "all locks of a given state that are associated with it.")
            final FactoryObject sourceObj, 
            @Descriptor("The id of the desired WakeLock.")
            final String lockId)
    {
        final Set<WakeLock> knownLocks = m_PowerManager.getWakeLocks(sourceObj, WakeLockState.Any);
        
        for (WakeLock lock : knownLocks)
        {
            if (lock.getId().equals(lockId))
            {
                return lock;
            }
        }
        
        final PrintStream out = session.getConsole();
        out.format("Cannot find a WakeLock with the given factory object"
                + " %s and lock id %s", sourceObj.getName(), lockId);
        
        return null;
    }
    
    /**
     * Returns the set of all known contexts.
     * @return
     *  the set of all known contexts
     */
    @Descriptor("Retrieves all available WakeLock contexts")
    public Set<Class<?>> getWakeLockContexts()
    {
        return m_PowerManager.getWakeLockContexts();
    }
    
    /**
     * Displays platform specific power management information.
     * 
     * @param session
     *            provides access to the Gogo shell session
     * @throws IOException 
     *            if a configuration for the platform power manager cannot be retrieved
     */
    @Descriptor("Displays platform specific power management information")
    public void info(final CommandSession session) throws IOException
    {
        final PrintStream out = session.getConsole();

        final Configuration config = m_ConfigAdmin.getConfiguration(PlatformPowerManager.class.getName(), null);
        final Dictionary<String, Object> dict = config.getProperties();
        
        Boolean enabled = null; 
        String isEnabled = "No Property Exists";
        if (dict != null)
        {
            enabled = (Boolean)dict.get(PlatformPowerManager.CONFIG_PROP_ENABLED);
            
            if (enabled != null)
            {
                isEnabled = enabled.toString();
            }
        }
        
        String overallStatus = "Enabled";
        String pltfrmPres = "No";
        
        if (m_PlatformPowerManager != null)
        {
            pltfrmPres = "Yes";
        }
        
        if (m_PlatformPowerManager == null || (enabled != null && !enabled))
        {
            overallStatus = "Disabled";
        }
        
        out.format("Overall PlatformPowerManager status: %s%n%n\tEnabled: %s%n\tPlatformPowerManager Present: %s%n%n", 
                overallStatus, isEnabled, pltfrmPres);
        
        final String notAvailable = "N/A";
        String batteryRemValue = notAvailable;
        String batteryVoltValue = notAvailable;
        if (m_PlatformPowerManager != null)
        {
            try
            {
                batteryRemValue = String.format("%d amp-hours", m_PlatformPowerManager.getBatteryAmpHoursRem());
            }
            catch (final UnsupportedOperationException e)
            {
                batteryRemValue = e.toString();
            }
            
            try
            {
                batteryVoltValue = String.format("%d mV", m_PlatformPowerManager.getBatteryVoltage());
            }
            catch (final UnsupportedOperationException e)
            {
                batteryVoltValue = e.toString();
            }
        }
        
        out.format("Battery Remaining: %s%n%n", batteryRemValue);
        out.format("Battery Voltage: %s%n%n", batteryVoltValue);
    }
    
    /**
     * Method to enable the {@link PlatformPowerManager}.
     * @param session
     *  command session that is executing the command
     * @throws IOException 
     *  if a configuration for the platform power manager cannot be retrieved
     */
    @Descriptor("Enables the PlatformPowerManager")
    public void enable(final CommandSession session) throws IOException
    {
        final PrintStream out = session.getConsole();
        final Configuration config = m_ConfigAdmin.getConfiguration(PlatformPowerManager.class.getName(), null);
        Dictionary<String, Object> dict = config.getProperties();
        
        Boolean enabled = null;
        if (dict == null)
        {
            dict = new Hashtable<String, Object>();
        }
        else
        {
            enabled = (Boolean)dict.get(PlatformPowerManager.CONFIG_PROP_ENABLED);
        }
        
        if (enabled == null || !enabled)
        {
            dict.put(PlatformPowerManager.CONFIG_PROP_ENABLED, true);
            config.update(dict);
            
            if (m_PlatformPowerManager == null)
            {
                out.format("Warning!! The configuration property has been set to enabled "
                        + "but the PlatformPowerManager is not present%n");
            }
        }
        else
        {
            out.format("The PlatformPowerManager is already enabled.%n");
        }
    }
    
    /**
     * Method to disable the {@link PlatformPowerManager}.
     * @param session
     *  command session that is executing the command
     * @throws IOException 
     *  if a configuration for the platform power manager cannot be retrieved
     */
    @Descriptor("Disables the PlatformPowerManager")
    public void disable(final CommandSession session) throws IOException
    {
        final Configuration config = m_ConfigAdmin.getConfiguration(PlatformPowerManager.class.getName(), null);
        Dictionary<String, Object> dict = config.getProperties();
        final PrintStream out = session.getConsole();
        
        Boolean enabled = null;
        if (dict == null)
        {
            dict = new Hashtable<String, Object>();
        }
        else
        {
            enabled = (Boolean)dict.get(PlatformPowerManager.CONFIG_PROP_ENABLED);
        }
        
        if (enabled == null || enabled)
        {
            dict.put(PlatformPowerManager.CONFIG_PROP_ENABLED, false);
            config.update(dict);
            
            if (m_PlatformPowerManager == null)
            {
                out.format("Warning!! The configuration property has been set to disabled but "
                        + "the PlatformPowerManager is not present%n");
            }
        }
        else
        {
            out.format("The PlatformPowerManager is already disabled.%n");
        }
    }

    /**
     * Method that creates a wake lock with the specified ID.
     * 
     * @param lockId
     *      ID to given to the lock.
     * @return
     *      The newly created wake lock.
     */
    @Descriptor("Creates a wake lock which can be used to request a lock later.")
    public WakeLock createWakeLock(
            @Descriptor("ID that will be used for the wake lock.")
            final String lockId)
    {
        final WakeLock lock = m_PowerManager.createWakeLock(this.getClass(), lockId);
        return lock;
    }
    
    /**
     * Method to activate a WakeLock on the shell.
     */
    @Descriptor("Activates the wake lock so the shell doesn't sleep.")
    public void startwl()
    {
        m_ShellLock.activate();
    }
    
    /**
     * Method to deactivate a WakeLock on the shell.
     */
    @Descriptor("Cancels the wake lock so the shell is able to sleep.")
    public void stopwl()
    {
        m_ShellLock.cancel();
    }
}

