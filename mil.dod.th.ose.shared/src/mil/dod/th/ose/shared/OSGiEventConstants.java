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
package mil.dod.th.ose.shared;

/**
 * This class hold the constants for OSGi related events, such as service and bundle events.
 * @author callen
 *
 */
public final class OSGiEventConstants
{
    /** Use for all topics within a package/class. */
    public final static String ALL_TOPIC_STR = "*";

    /** Event topic prefix for to use for all bundle events. */
    public final static String TOPIC_PREFIX_BUNDLE_EVENTS = "org/osgi/framework/BundleEvent/";
    
    /** Topic used to specify all bundle events. */
    public final static String TOPIC_BUNDLE_ALL_EVENTS = TOPIC_PREFIX_BUNDLE_EVENTS + ALL_TOPIC_STR;

    /** Topic used to specify a bundle install event. */
    public final static String TOPIC_BUNDLE_INSTALLED = TOPIC_PREFIX_BUNDLE_EVENTS + "INSTALLED";

    /** Topic used to specify a bundle resolved event. */
    public final static String TOPIC_BUNDLE_RESOLVED = TOPIC_PREFIX_BUNDLE_EVENTS + "RESOLVED";

    /** Topic used to specify a bundle uninstall event. */
    public final static String TOPIC_BUNDLE_UNINSTALLED = TOPIC_PREFIX_BUNDLE_EVENTS + "UNINSTALLED";
    
    /** Topic used to specify a bundle install event. */
    public final static String TOPIC_BUNDLE_UPDATED = TOPIC_PREFIX_BUNDLE_EVENTS + "UPDATED";
    
    /** Topic used to specify a bundle start event. */
    public final static String TOPIC_BUNDLE_STARTED = TOPIC_PREFIX_BUNDLE_EVENTS + "STARTED";
    
    /** Topic used to specify a bundle stop event. */
    public final static String TOPIC_BUNDLE_STOPPED = TOPIC_PREFIX_BUNDLE_EVENTS + "STOPPED";

    
    
    /** Event topic prefix used for all service events. */
    public final static String TOPIC_PREFIX_SERIVCE_EVENTS = "org/osgi/framework/ServiceEvent/";

    /** Event topic used when a service is registered. */
    public final static String TOPIC_SERVICE_REGISTERED = TOPIC_PREFIX_SERIVCE_EVENTS + "REGISTERED";

    
    
    /** Event topic prefix for to use for all logging events. */
    public final static String TOPIC_PREFIX_LOGGING_EVENTS = "org/osgi/service/log/LogEntry/";
    
    /** Topic used to specify all logging events. */
    public final static String TOPIC_LOGGING_ALL_EVENTS = TOPIC_PREFIX_LOGGING_EVENTS + ALL_TOPIC_STR;
    
    /** Event topic used when a debug message is logged. */
    public final static String TOPIC_LOG_DEBUG = TOPIC_PREFIX_LOGGING_EVENTS + "LOG_DEBUG";
 
    /** Event topic used when a debug message is logged. */
    public final static String TOPIC_LOG_INFO = TOPIC_PREFIX_LOGGING_EVENTS + "LOG_INFO";
 
    /** Event topic used when a debug message is logged. */
    public final static String TOPIC_LOG_WARNING = TOPIC_PREFIX_LOGGING_EVENTS + "LOG_WARNING";
 
    /** Event topic used when a debug message is logged. */
    public final static String TOPIC_LOG_ERROR = TOPIC_PREFIX_LOGGING_EVENTS + "LOG_ERROR";
 
    /**
     * Hidden constructor belonging to this utility class.
     */
    private OSGiEventConstants()
    {
    
    }
}
