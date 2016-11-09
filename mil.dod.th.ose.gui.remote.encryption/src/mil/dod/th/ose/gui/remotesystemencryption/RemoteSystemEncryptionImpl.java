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
package mil.dod.th.ose.gui.remotesystemencryption;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.RemoteSystemEncryption;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;

/**
 * Basic implementation of {@link RemoteSystemEncryption} service.
 * @author allenchl
 *
 */
@Component
public class RemoteSystemEncryptionImpl implements RemoteSystemEncryption
{
    /**
     * Map containing encryption types indexed by system ID.  Used to more efficiently
     * return encryption types for systems without querying the persistent data
     * store.  
     */
    private final Map<Integer, EncryptType> m_SystemEncryptionTypes = 
            Collections.synchronizedMap(new HashMap<Integer, EncryptType>());
    
    /**
     * Reference the {@link RemoteChannelLookup} service for removing obsolete controllers from the data store.
     */
    private RemoteChannelLookup m_RemoteChannelLookup;
    
    /**
     * Set the {@link RemoteChannelLookup} service.
     * @param lookup
     *     the remote channel lookup service to use
     */
    @Reference
    public void setRemoteChannelLookup(final RemoteChannelLookup lookup)
    {
        m_RemoteChannelLookup = lookup;
    }
    

    @Override
    public EncryptType getEncryptType(final int systemId)
    {
        return m_SystemEncryptionTypes.get(systemId);
    }

    @Override
    public void cleanupSystemEncryptionTypes()
    {
        synchronized (m_SystemEncryptionTypes)
        {
            final List<Integer> keySet = new ArrayList<Integer>(m_SystemEncryptionTypes.keySet());
            
            //check that the all keys in the map are in the remote channel lookup
            for (int key : keySet)
            {
                if (m_RemoteChannelLookup.getChannels(key).isEmpty())
                {
                    m_SystemEncryptionTypes.remove(key);
                }
            }
        }
    }

    @Override
    public void addEncryptionTypeForSystem(final int systemId, final EncryptType type)
    {
        m_SystemEncryptionTypes.put(systemId, type);
    }
}
