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
package mil.dod.th.ose.datastream;

import java.net.URI;
import java.util.UUID;

import mil.dod.th.ose.core.factory.api.data.FactoryObjectDataManager;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectInformationException;

/**
 * StreamProfile-specific interface extension of the {@link FactoryObjectDataManager}.
 * 
 * @author jmiller
 *
 */
public interface StreamProfileFactoryObjectDataManager extends FactoryObjectDataManager
{
    
    /**
     * Get the stream port information (assigned by DataStreamService) for a StreamProfile.
     * 
     * @param uuid
     *      the uuid of the StreamProfile
     * @return
     *      the stream port as a URI
     * @throws FactoryObjectInformationException 
     *      if an error occurs that prevents the data from being retrieved
     */
    URI getStreamPort(UUID uuid) throws FactoryObjectInformationException;
    
    /**
     * Set the stream port information (assigned by DataStreamService) for a StreamProfile.
     * 
     * @param uuid
     *      the uuid of the StreamProfile
     * @param streamPort
     *      the stream port as a URI
     * @throws FactoryObjectInformationException 
     *      if an error occurs that prevents the data from being persisted
     */
    void setStreamPort(UUID uuid, URI streamPort) throws FactoryObjectInformationException;

}
