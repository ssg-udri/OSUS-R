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

/**
 * Message builder for the event unregistration message.
 * 
 * @author dlandoll
 */
@ProviderType
public interface EventUnregistrationBuilder extends MessageBuilder
{
    /**
     * Set the event registration ID to be unregistered. This is a required parameter.
     * 
     * @param regId
     *      event registration ID to unregister
     * @return
     *      Builder instance
     */
    EventUnregistrationBuilder setId(int regId);
}
