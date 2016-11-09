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
package mil.dod.th.ose.core.impl.asset.data;

import java.util.UUID;

import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.Orientation;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectDataManager;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectInformationException;

/**
 * Describe asset specific service needed to persist asset information.
 * @author callen
 *
 */
public interface AssetFactoryObjectDataManager extends FactoryObjectDataManager
{
    /**
     * Get the coordinate information for an asset.
     * @param uuid
     *     the uuid of the asset
     * @return
     *     the coordinate information known for the asset or null if none is available
     * @throws IllegalArgumentException
     *     if a data message cannot be found for the given UUID
     * @throws FactoryObjectInformationException
     *     if an error occurs that prevents the data from being retrieved
     */
    Coordinates getCoordinates(UUID uuid) throws IllegalArgumentException, FactoryObjectInformationException;
    
    /**
     * Get the orientation information for an asset.
     * @param uuid
     *     the uuid of the asset
     * @return
     *     the orientation information known for the asset or null if none is available
     * @throws IllegalArgumentException
     *     if a data message cannot be found for the given UUID
     * @throws FactoryObjectInformationException
     *     if an error occurs that prevents the data from being retrieved
     */
    Orientation getOrientation(UUID uuid) throws IllegalArgumentException, FactoryObjectInformationException;
    
    /**
     * Get the coordinate information for an asset.
     * @param uuid
     *     the uuid of the asset
     * @param coords
     *     the coordinate values to persist for the asset
     * @throws IllegalArgumentException
     *     if a data message cannot be found for the given UUID
     * @throws FactoryObjectInformationException
     *     if an error occurs that prevents the data from being persisted
     */
    void setCoordinates(UUID uuid, Coordinates coords) throws IllegalArgumentException, 
            FactoryObjectInformationException;
    
    /**
     * Get the orientation information for an asset.
     * @param uuid
     *     the uuid of the asset
     * @param orien
     *     the orientation values to persist for the asset
     * @throws IllegalArgumentException
     *     if a data message cannot be found for the given UUID
     * @throws FactoryObjectInformationException
     *     if an error occurs that prevents the data from being persisted
     */
    void setOrientation(UUID uuid, Orientation orien) throws IllegalArgumentException, 
            FactoryObjectInformationException;
}
