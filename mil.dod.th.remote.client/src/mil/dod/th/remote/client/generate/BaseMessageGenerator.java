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
 * Provides helper methods to generate and send
 * {@link mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType} messages.
 * <p>
 * This is an OSGi service and may be obtained by getting an OSGi service reference or using declarative services.
 * 
 * @author dlandoll
 */
@ProviderType
public interface BaseMessageGenerator
{
    /**
     * Create a message builder for the get controller info request.
     * 
     * @return
     *      message builder object
     */
    MessageBuilder createGetControllerInfoRequest();
}
