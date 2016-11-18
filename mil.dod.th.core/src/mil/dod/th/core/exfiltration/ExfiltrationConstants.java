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
package mil.dod.th.core.exfiltration;

/**
 * Contains constants and event definitions related to exfiltration.
 * 
 * @author dlandoll
 */
public final class ExfiltrationConstants
{
    /** Event topic prefix to use for all topics in ExfiltrationConstants. */
    final public static String TOPIC_PREFIX = "mil/dod/th/core/exfiltration/ExfiltrationConstants/";

    /**
     * <p>
     * Event topic used when business logic or mission programming detects an observation that should be exfiltrated.
     * This allows for separation of functionality into different plug-ins that don't depend on each other. To preserve
     * this, an asset should never fire the this event on its own (as the asset would then be performing business
     * logic.)
     * </p>
     * 
     * <p>
     * A typical scenario should look like this:
     * </p>
     * <ol>
     * <li>Plug-in A (the asset) produces an observation, firing the
     *     {@link mil.dod.th.core.persistence.ObservationStore#TOPIC_OBSERVATION_PERSISTED OBSERVATION_PERSISTED} event.
     * <li>Plug-in B (the business logic or mission program) picks up the
     *     {@link mil.dod.th.core.persistence.ObservationStore#TOPIC_OBSERVATION_PERSISTED OBSERVATION_PERSISTED} event,
     *     examines the observation and decides whether it should be exfiltrated. If yes, it fires an
     *     INTERESTING_OBSERVATION event.
     * <li>Plug-in C (the exfiltration plug-in) picks up the INTERESTING_OBSERVATION event and sends out the
     *     observation.
     * </ol>
     * 
     * <p>
     * Contains the following fields:
     * </p>
     * <ul>
     * <li>{@link mil.dod.th.core.persistence.ObservationStore#EVENT_PROP_OBSERVATION_UUID} - the UUID of the
     *     interesting observation
     * <li>{@link #EVENT_PROP_SOURCE_ID} - identifier of the plug-in that is tagging the observation as interesting
     * </ul>
     */
    final public static String TOPIC_INTERESTING_OBSERVATION = TOPIC_PREFIX + "INTERESTING_OBSERVATION";

    /** Event property key for the class type or ID of the plug-in generating the event ({@link String}). */
    final public static String EVENT_PROP_SOURCE_ID = "source.id";

    /** Event property key for the exfil destination ({@link ExfiltrationDestination}). */
    final public static String EVENT_PROP_SEND_TO = "send.to";

    /**
     * Defined to prevent instantiation.
     */
    private ExfiltrationConstants()
    {

    }
}
