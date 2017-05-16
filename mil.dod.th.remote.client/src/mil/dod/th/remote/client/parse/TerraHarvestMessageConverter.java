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
package mil.dod.th.remote.client.parse;

import com.google.protobuf.InvalidProtocolBufferException;

import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.remote.client.RemoteMessage;

/**
 * Message converter interface that defines a service for converting/parsing incoming {@link TerraHarvestMessage}s.
 * <p>
 * This is an OSGi service and may be obtained by getting an OSGi service reference or using declarative services.
 * 
 * @author dlandoll
 */
public interface TerraHarvestMessageConverter
{
    /**
     * Parse the raw {@link TerraHarvestMessage} and convert to a {@link RemoteMessage}.
     * 
     * @param message
     *      raw message
     * @return
     *      parsed remote message or null if parser not found
     * @throws InvalidProtocolBufferException
     *      if parsing error occurs
     */
    RemoteMessage<?> convertMessage(TerraHarvestMessage message) throws InvalidProtocolBufferException;

    /**
     * Returns whether the given namespace type is supported for message conversion.
     * 
     * @param namespace
     *      namespace type
     * @return
     *      true if the namespace type is supported, false otherwise
     */
    boolean isSupported(Namespace namespace);
}
