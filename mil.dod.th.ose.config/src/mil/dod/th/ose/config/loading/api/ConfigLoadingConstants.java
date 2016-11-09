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
package mil.dod.th.ose.config.loading.api;

/**
 * Contains constants for the configuration loading events posted to the event admin service.
 * 
 * @author dlandoll
 */
public final class ConfigLoadingConstants
{
    /** Event topic prefix to use for all topics in ConfigLoadingConstants. */
    final public static String TOPIC_PREFIX = "mil/dod/th/ose/config/loading/impl/ConfigurationMgr/";

    /**
     * Event topic used to post factory object loading complete events. This event indicates that factory objects,
     * such as assets, have been created or updated per definitions found in the configs.xml file. If dependent services
     * for a particular factory object are never satisfied, this event will never be generated.
     */
    final public static String TOPIC_CONFIG_FACTORY_OBJS_COMPLETE_EVENT = TOPIC_PREFIX + "CONFIG_FACTORY_OBJS_COMPLETE";

    /**
     * Event topic used to post address loading complete events. This event indicates that address objects have been
     * created per definitions found in the configs.xml file.
     */
    final public static String TOPIC_CONFIG_ADDRESS_OBJS_COMPLETE_EVENT = TOPIC_PREFIX + "CONFIG_ADDRESS_OBJS_COMPLETE";

    /**
     * Event topic used to post OSGi configurations loading complete events. This event indicates that configurations
     * have been created per definitions found in the configs.xml file.
     */
    final public static String TOPIC_CONFIG_OSGI_COMPLETE_EVENT = TOPIC_PREFIX + "CONFIG_OSGI_COMPLETE";

    /**
     * Event topic used to post configuration processing complete events. This event indicates that
     * configurations have been processed per definitions found in the configs.xml file. 
     * @see #TOPIC_CONFIG_FACTORY_OBJS_COMPLETE_EVENT
     */
    final public static String TOPIC_CONFIG_PROCESSING_COMPLETE_EVENT = TOPIC_PREFIX + "CONFIG_PROCESSING_COMPLETE";

    /**
     * Event topic used to receive events for the loading of each individual factory object. This event indicates that
     * a single factory object has been created or updated for an entry found in the configs.xml file.
     * <br>
     * The following properties will be included in the event as applicable:
     * <ul>
     * <li>{@link ConfigLoadingConstants#EVENT_PROP_PRODUCT_TYPE} - fully qualified class type as a String
     * <li>{@link ConfigLoadingConstants#EVENT_PROP_OBJ_NAME} - name of the factory object
     * </ul>
     */
    final public static String TOPIC_CONFIG_FACTORY_OBJ_LOADED_EVENT = TOPIC_PREFIX + "CONFIG_FACTORY_OBJ_LOADED";

    /**
     * Event topic used to receive events for the loading of each individual address. This event indicates that
     * a single address has been created or updated for an entry found in the configs.xml file.
     * <br>
     * The following properties will be included in the event as applicable:
     * <ul>
     * <li>{@link ConfigLoadingConstants#EVENT_PROP_PRODUCT_TYPE} - fully qualified class type as a String
     * <li>{@link ConfigLoadingConstants#EVENT_PROP_ADDR_DESC} - address description
     * </ul>
     */
    final public static String TOPIC_CONFIG_ADDRESS_LOADED_EVENT = TOPIC_PREFIX + "CONFIG_ADDRESS_LOADED";

    /**
     * Event property containing the factory object product type as a fully qualified class name.
     */
    final public static String EVENT_PROP_PRODUCT_TYPE = "product.type";

    /**
     * Event property containing the factory object name.
     */
    final public static String EVENT_PROP_OBJ_NAME = "obj.name";

    /**
     * Event property containing the address description.
     */
    final public static String EVENT_PROP_ADDR_DESC = "addr.desc";

    /**
     * Defined to prevent instantiation.
     */
    private ConfigLoadingConstants()
    {
    }
}
