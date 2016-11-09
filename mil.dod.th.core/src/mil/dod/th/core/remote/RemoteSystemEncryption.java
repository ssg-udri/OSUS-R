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
package mil.dod.th.core.remote;

import aQute.bnd.annotation.ProviderType;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;


/**
 * This interface is used to obtain the appropriate encryption type to be applied to messages
 * sent to a particular remote system. Implementations of this interface should provide the correct
 * encryption type based upon the {@link mil.dod.th.core.system.TerraHarvestSystem} Id presented. It is up
 * to the implementor to obtain the appropriate encryption information, and store this information. If this
 * interface is implemented, requests to create messages can be done using the 
 * {@link mil.dod.th.core.remote.messaging.MessageFactory} service and calling one of these send methods:
 * <p>
 * {@link mil.dod.th.core.remote.messaging.MessageWrapper#queue(int, mil.dod.th.core.remote.ResponseHandler)}
 * </p>
 * <p>
 * {@link mil.dod.th.core.remote.messaging.MessageWrapper#trySend(int)}
 * </p> 
 * When this interface is implemented it must be provided as an OSGi service.
 *
 * @author allenchl
 *
 */
@ProviderType
public interface RemoteSystemEncryption
{
    /**
     * Get the level of encryption that should be applied to messages for a given system ID.
     * @param systemId
     *     the {@link mil.dod.th.core.system.TerraHarvestSystem} ID of which to get the encryption
     *     type for
     * @return
     *     the proto version encryption type or null if the given ID is not a known system
     */
    EncryptType getEncryptType(int systemId);
    
    /**
     * Cleanup any system encryption types entries for which the {@link RemoteChannelLookup} 
     * no longer holds a reference to the system encryption type entry's system id.
     */
    void cleanupSystemEncryptionTypes();
    
    /**
     * Adds an encryption type to the list of known systems based on the given system ID.
     * @param systemId
     *  the system id for which the given {@link EncryptType} should be associated with
     * @param type
     *  the encryption type for the given system id
     */
    void addEncryptionTypeForSystem(int systemId, EncryptType type);
}
