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
package mil.dod.th.ose.config.event.constants;

/**
 * Contains constants for the configuration events posted to the event admin service through the configuration admin
 * event bridge.
 * 
 * @author cweisenborn
 */
public final class ConfigurationEventConstants
{
    /** Event topic prefix to use for all topics in ConfigurationEventConstants. */
    final public static String TOPIC_PREFIX = "org/osgi/service/cm/ConfigurationEvent/";

    /** Event topic used to receive all events posted by the configuration admin service. */
    final public static String TOPIC_ALL_CONFIGURATION_EVENTS = TOPIC_PREFIX + "*";
    
    /** Event topic used to receive configuration deleted events from the configuration admin service.*/
    final public static String TOPIC_CONFIGURATION_DELETED_EVENT = TOPIC_PREFIX + "CM_DELETED";
    
    /** Event topic used to receive configuration updated events from the configuration admin service. */
    final public static String TOPIC_CONFIGURATION_UPDATED_EVENT = TOPIC_PREFIX + "CM_UPDATED";
    
    /** Event topic used to receive configuration location changed events from the configuration admin service. */
    final public static String TOPIC_CONFIGURATION_LOCATION_EVENT = TOPIC_PREFIX + "CM_LOCATION_CHANGED";
    
    /** Event property key used to retrieve the pid of the service which the event pertains to. */
    final public static String EVENT_PROP_PID = "cm.pid";
    
    /** Event property key used to retrieve the factory pid of the service which the event pertains to. May be null
     * if event did not originate from a factory configuration service.
     */
    final public static String EVENT_PROP_FACTORY_PID = "cm.factoryPid";
    
    /** Event property key used to retrieve the service reference to the configuration admin service. */
    final public static String EVENT_PROP_SERVICE_REFERENCE = "service";
    
    /** Event property key used to retrieve the service id of the configuration admin service. */
    final public static String EVENT_PROP_SERVICE_ID = "service.id";
    
    /** Event property key used to retrieve the object class of the configuration admin service. */
    final public static String EVENT_PROP_SERVICE_OBJECT_CLASS = "service.objectClass";
    
    /** Event property key used to retrieve the service pid of the configuration admin service. */
    final public static String EVENT_PROP_SERVICE_PID = "service.pid";
    
    /**
     * Defined to prevent instantiation.
     */
    private ConfigurationEventConstants()
    {
        
    }
}
