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
package mil.dod.th.remote.client.generate;

import aQute.bnd.annotation.ProviderType;

import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen.LexiconFormat;

/**
 * Message builder for the event registration message.
 * 
 * @author dlandoll
 */
@ProviderType
public interface EventRegistrationBuilder extends MessageBuilder
{
    /**
     * Set a variable number of event topics to register for.
     * 
     * @param topics
     *      event topics
     * @return
     *      Builder instance
     */
    EventRegistrationBuilder setTopics(String... topics);

    /**
     * Set an event filter to register with. This field is optional.
     * 
     * @param filter
     *      event filter, uses the LDAP-style filter specification
     * @return
     *      Builder instance
     */
    EventRegistrationBuilder setFilter(String filter);

    /**
     * Set the time, in hours, for the event registration to expire.
     * 
     * @param expirationTimeHours
     *      number of hours the event registration is valid for
     * @return
     *      Builder instance
     */
    EventRegistrationBuilder setExpirationTimeHours(int expirationTimeHours);

    /**
     * Set whether events matching this registration are queued on the controller when the link is down and resent
     * after the link is available again.
     * 
     * @param canQueueEvent
     *      true if events should be queued, false otherwise
     * @return
     *      Builder instance
     */
    EventRegistrationBuilder setCanQueueEvent(boolean canQueueEvent);

    /**
     * Set the object format lexicon based event properties should be sent as. This field optional and will default
     * to {@link mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen.LexiconFormat.Enum#NATIVE}.
     * 
     * @param objectFormat
     *      Format of lexicon based objects
     * @return
     *      Builder instance
     */
    EventRegistrationBuilder setObjectFormat(LexiconFormat.Enum objectFormat);
}
