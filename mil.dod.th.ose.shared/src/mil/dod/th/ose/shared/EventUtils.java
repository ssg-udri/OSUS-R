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

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.event.Event;

/**
 * Utilities to make it easier to send/post/handle events from the EventAdmin service.
 * 
 * @author dhumeniuk
 *
 */
public final class EventUtils
{
    /**
     * Character separator used within a topic string.
     */
    private static final String TOPIC_SEPARATOR = "/";

    /**
     * Private constructor to prevent instantiation.
     */
    private EventUtils()
    {
        // do nothing
    }
    
    /**
     * Translates a class/action combination into a topic string based on the formatting 
     * fully/qualified/package/ClassName/ACTION.
     * 
     * @param clazz
     *      Class that the action pertains to
     * @param action
     *      Particular action for the event as scoped by the class
     * @return
     *      The event topic using the EventAdmin topic pattern
     */
    public static String getEventTopic(final Class<?> clazz, final String action)
    {
        return getClassPrefix(clazz) + TOPIC_SEPARATOR + action;
    }

    /**
     * Get the topic string based on the class only, get all actions for the class.
     * 
     * @param clazz
     *      Class that the actions pertains to
     * @return
     *      The event topic using the EventAdmin topic pattern
     */
    public static String getEventTopic(final Class<?> clazz)
    {
        return getEventTopic(clazz, "*");
    }

    /**
     * Check if the topic is for the give class.  Using the formatting, fully/qualified/package/ClassName/ACTION,
     * the topic will match if the package and class name are equal, where the ACTION is irrelevant.
     * 
     * @param topic
     *      Topic to check
     * @param clazz
     *      Class to match with the topic
     * @return
     *      true if topic is for the given class, false otherwise
     */
    public static boolean topicMatchesClass(final String topic, final Class<?> clazz)
    {
        return topic.startsWith(getClassPrefix(clazz));
    }
    
    /**
     * Convert a given class to what is the beginning of a topic string.
     * 
     * @param clazz
     *      Class to convert
     * @return
     *      Prefix for a topic string containing the class
     */
    private static String getClassPrefix(final Class<?> clazz)
    {
        return clazz.getName().replace(".", TOPIC_SEPARATOR);
    }

    /**
     * Generate a list of map entries for an event. Note that the topic will be removed from the properties.
     * 
     * @param event
     *      event to pull properties out of
     * @return
     *      Event properties converted into a map
     */
    public static Map<String, Object> getEventProps(final Event event)
    {
        final Map<String, Object> props = new HashMap<String, Object>();
        for (String key : event.getPropertyNames())
        {
            props.put(key, event.getProperty(key));
        }
        return props;
    }
}
