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
package edu.udayton.udri.asset.novatel.message;

import mil.dod.th.core.types.factory.SpatialTypesFactory;
import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.Orientation;

/**
 * Novatel Message instance which contains position information from an INSPVA message.
 * The time contained in this message is the GPS time from the INSPVA message with an offset to translate the time into
 * UTC. The time-offset is from the NovAtel TIMEA message.
 * @author allenchl
 *
 */
public class NovatelInsMessage
{
    /**
     * Coordinates object.
     */
    private final Coordinates m_Coordinates;
    
    /**
     * Orientation object.
     */
    private final Orientation m_Orientation;
    
    /**
     * The time when this position data was collected. The time reported here is the GPS
     * time with an offset applied to reflect the time in UTC. The offset is calculated from the most recent TIMEA
     * message received from the NovAtel instance.
     */
    private final long m_Time;
    
    /**
     * Constructor for the novatel message.
     * @param longitude
     *      the longitude value for the message
     * @param latitude
     *      the latitude value for the message
     * @param altitude
     *      the altitude value for the message
     * @param heading
     *      the heading value for the message
     * @param elevation
     *      the elevation value for the message
     * @param bank
     *      the bank value for the message
     * @param time
     *      the time from the message
     */
    public NovatelInsMessage(final double longitude, final double latitude, final double altitude, 
            final double heading, final double elevation, final double bank, final long time)
    {
        m_Time = time;
        m_Coordinates = SpatialTypesFactory.newCoordinates(longitude, latitude, altitude);
        m_Orientation = SpatialTypesFactory.newOrientation(heading, elevation, bank);
    }
    
    /**
     * Get the {@link Coordinates} object.
     * @return
     *      this message's coordinates object
     */
    public Coordinates getCoordinates()
    {
        return m_Coordinates;
    }
    
    /**
     * Get the {@link Orientation} object.
     * @return
     *      this message's orientation object
     */
    public Orientation getOrientation()
    {
        return m_Orientation;
    }
    
    /**
     * Get the time reported on the data message which contained the position data. The time reported here is the GPS
     * time with an offset applied to reflect the time in UTC. The offset is calculated from the most recent TIMEA
     * message received from the NovAtel instance.
     * @return
     *      this message's UTC time
     */
    public long getUtcTime()
    {
        return m_Time;
    }
}
