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
package mil.dod.th.ose.core.impl.pm;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.PlatformPowerManager;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.ose.core.pm.api.PowerManagerInternal;

import org.osgi.framework.BundleContext;

/**
 * Implementation of the {@link WakeLock}.
 */
@Component(factory = WakeLockImpl.COMPONENT_FACTORY)
public class WakeLockImpl implements WakeLock
{
    /**
     * Factory name used by the component factory.
     */
    public final static String COMPONENT_FACTORY = "mil.dod.th.ose.core.impl.pm.WakeLockImpl";

    /**
     * Name of the OSGi framework property that enables log tracing of wake locks.
     */
    public static final String WAKELOCK_TRACE_PROPERTY = "mil.dod.th.ose.core.pm.wakelock.trace";

    /**
     * Wake lock ID property passed in during activation.
     */
    public final static String COMPONENT_PROP_ID = "wakelock.id";

    /**
     * Wake lock context property passed in during activation.
     */
    public final static String COMPONENT_PROP_CONTEXT = "wakelock.context";

    /**
     * Wake lock source object property passed in during activation.
     */
    public final static String COMPONENT_PROP_SRC_OBJ = "wakelock.src.obj";
    
    /**
     * Reference to the power manager service.
     */
    public final static String COMPONENT_PROP_POWER_MGR = "power.manager";

    /**
     * Error/debug message used when {@link PlatformPowerManager} is not available to activate a wake lock.
     */
    private final static String ACTIVATE_WAKELOCK_ERR_MSG = 
            "PlatformPowerManager not available to activate WakeLock [%s]";

    /**
     * Debug message used when activating a wake lock with a start and end.
     */
    private final static String ACTIVATE_WAKELOCK_DURATION_DBG_MSG =
            "Activate WakeLock [%s] at %d ms and end at %d ms";

    /**
     * Wake lock ID.
     */
    private String m_Id;

    /**
     * Class context associated with this wake lock.
     */
    private Class<?> m_Context;

    /**
     * Factory object associated with this wake lock, if applicable.
     */
    private FactoryObject m_SourceObject;

    /**
     * Used to log messages.
     */
    private LoggingService m_Logging;

    /**
     * Platform specific power manager, delegate platform operations to this service.
     */
    private PlatformPowerManager m_PlatformPowerManager;
    
    /**
     * Reference to the internal power manager interface.
     */
    private PowerManagerInternal m_PowerManagerInternal;

    /**
     * Lock used for access to {@link #m_PlatformPowerManager}.
     */
    private final Object m_PPMLock = new Object();

    /**
     * Indicates whether all wake lock logging should be enabled or not.
     */
    private boolean m_EnableTrace;

    /**
     * Binds the logging service for logging messages.
     * 
     * @param logging
     *            Logging service object
     */
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }

    /**
     * Binds the platform's power manager.
     * 
     * This reference is optional and not required.
     * 
     * @param platformPowerManager
     *            power manager for the specific platform
     */
    @Reference(optional = true, dynamic = true)
    public void setPlatformPowerManager(final PlatformPowerManager platformPowerManager)
    {
        synchronized (m_PPMLock)
        {
            m_PlatformPowerManager = platformPowerManager;
        }
    }

    /**
     * Unbind the platform's power manager.
     * 
     * @param platformPowerManager
     *      parameter not used, must match binding method signature
     */
    public void unsetPlatformPowerManager(final PlatformPowerManager platformPowerManager)
    {
        synchronized (m_PPMLock)
        {
            m_PlatformPowerManager = null; // NOPMD: NullAssignment, Must assign to null, field is checked before using
        }
    }

    /**
     * Activate the component.
     * 
     * @param bundleContext
     *            OSGi bundle context
     * @param properties
     *            component properties
     */
    @Activate
    public void activateInstance(final BundleContext bundleContext, final Map<String, Object> properties)
    {
        m_Id = (String)properties.get(COMPONENT_PROP_ID);
        m_Context = (Class<?>)properties.get(COMPONENT_PROP_CONTEXT);
        m_SourceObject = (FactoryObject)properties.get(COMPONENT_PROP_SRC_OBJ);
        m_PowerManagerInternal = (PowerManagerInternal)properties.get(COMPONENT_PROP_POWER_MGR);

        final String traceProp = bundleContext.getProperty(WAKELOCK_TRACE_PROPERTY);
        if (traceProp != null && Boolean.TRUE.toString().equalsIgnoreCase(traceProp))
        {
            m_EnableTrace = true;
        }
    }

    @Override
    public String getId()
    {
        return m_Id;
    }

    @Override
    public Class<?> getContext()
    {
        return m_Context;
    }

    @Override
    public FactoryObject getSourceObject()
    {
        return m_SourceObject;
    }
    
    @Override
    public void delete() throws IllegalStateException
    {
        m_PowerManagerInternal.deleteWakeLock(this);
    }

    @Override
    public Date activate(final long lockAwakeDuration, final TimeUnit unit) throws IllegalArgumentException
    {
        if (lockAwakeDuration < 0)
        {
            throw new IllegalArgumentException(String.format("lockAwakeDuration [%d] is negative", lockAwakeDuration));
        }

        synchronized (m_PPMLock)
        {
            if (m_PlatformPowerManager == null)
            {
                traceLogging(ACTIVATE_WAKELOCK_ERR_MSG, this);
            }
            else
            {
                final long startLockTimeMs = System.currentTimeMillis();
                final long stopLockTimeMs = startLockTimeMs + unit.toMillis(lockAwakeDuration);
    
                traceLogging(ACTIVATE_WAKELOCK_DURATION_DBG_MSG, this, startLockTimeMs, stopLockTimeMs);

                return new Date(m_PlatformPowerManager.activateWakeLock(this, startLockTimeMs, stopLockTimeMs));
            }
        }

        return null;
    }

    @Override
    public Date activate(final Date lockAwakeTime)
    {
        synchronized (m_PPMLock)
        {
            if (m_PlatformPowerManager == null)
            {
                traceLogging(ACTIVATE_WAKELOCK_ERR_MSG, this);
            }
            else
            {
                final long startLockTimeMs = System.currentTimeMillis();
                final long stopLockTimeMs = lockAwakeTime.getTime();
    
                traceLogging(ACTIVATE_WAKELOCK_DURATION_DBG_MSG, this, startLockTimeMs, stopLockTimeMs);

                return new Date(m_PlatformPowerManager.activateWakeLock(this, startLockTimeMs, stopLockTimeMs));
            }
        }

        return null;
    }

    @Override
    public void activate()
    {
        synchronized (m_PPMLock)
        {
            if (m_PlatformPowerManager == null)
            {
                traceLogging(ACTIVATE_WAKELOCK_ERR_MSG, this);
            }
            else
            {
                traceLogging("Activate WakeLock [%s] indefinitely", this);

                m_PlatformPowerManager.activateWakeLock(this, System.currentTimeMillis(),
                        PlatformPowerManager.INDEFINITE);
            }
        }
    }

    @Override
    public void scheduleWakeTime(final Date wakeTime)
    {
        synchronized (m_PPMLock)
        {
            if (m_PlatformPowerManager == null)
            {
                traceLogging("PlatformPowerManager not available to schedule future WakeLock [%s]", this);
            }
            else
            {
                final long wakeTimeMs = wakeTime.getTime();

                traceLogging("Schedule WakeLock [%s] at %d ms", this, wakeTimeMs);

                // Use a minimum duration of 1 milliseconds and let PlatformPowerManager adjust to its
                // configured minimum
                m_PlatformPowerManager.activateWakeLock(this, wakeTimeMs, wakeTimeMs + 1);
            }
        }
    }

    @Override
    public void cancel() throws IllegalStateException
    {
        synchronized (m_PPMLock)
        {
            if (m_PlatformPowerManager == null)
            {
                traceLogging("PlatformPowerManager not available to cancel WakeLock [%s]", this);
            }
            else
            {
                traceLogging("Cancel WakeLock [%s]", this);

                m_PlatformPowerManager.cancelWakeLock(this);
            }
        }
    }

    @Override
    public String toString()
    {
        return String.format("%s:%s", getWakeLockName(), m_Id);
    }

    /**
     * Get the wake lock name/description based on whether using a source object or not.
     * 
     * @return Wake lock name
     */
    private String getWakeLockName()
    {
        return (m_SourceObject == null) ? m_Context.getName() : m_SourceObject.getName();
    }

    /**
     * Log a wake lock debug trace message through the log service, if trace logging is enabled via the
     * {@value #WAKELOCK_TRACE_PROPERTY} framework property.
     * 
     * @param format
     *      Format string, see {@link java.util.Formatter}.  The format string should be a constant and not a 
     *      concatenation.  If the log service is not available, the format string will not be evaluated, limiting
     *      processing.
     * @param args
     *      Argument list that will be inserted into the format string
     * 
     * @see String#format(String, Object...)
     */
    private void traceLogging(final String format, final Object... args)
    {
        if (m_EnableTrace)
        {
            m_Logging.debug(format, args);
        }
    }
}
