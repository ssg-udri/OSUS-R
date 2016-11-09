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
package mil.dod.th.ose.gui.webapp;

/**
 * Stores location data.  Does not store any error information, just the values. See {@link 
 * mil.dod.th.core.types.spatial.Coordinates} and {@link mil.dod.th.core.types.spatial.Orientation} for more details on
 * units and meanings.
 * 
 * @author dhumeniuk
 *
 */
public class LocationModel
{
    /**
     * Longitude for the location.
     */
    private Double m_Longitude;
    
    /**
     * Latitude for the location.
     */
    private Double m_Latitude;
    
    /**
     * Altitude for the location.
     */
    private Double m_Altitude;
    
    /**
     * Heading for the location.
     */
    private Double m_Heading;
    
    /**
     * Elevation for the location.
     */
    private Double m_Elevation;
    
    /**
     * Bank for the location.
     */
    private Double m_Bank;
    
    /**
     * Get the latitude of the asset.
     * 
     * @return
     *      the latitude of the asset, can be null
     */
    public Double getLatitude()
    {
        return m_Latitude;
    }
    
    /**
     * Set the latitude of the asset.
     * 
     * @param lat
     *      the latitude of the asset, can be null
     */
    public void setLatitude(final Double lat)
    {
        m_Latitude = lat;
    }
    
    /**
     * Get the longitude of the asset.
     * 
     * @return
     *      the longitude of the asset, can be null
     */
    public Double getLongitude()
    {
        return m_Longitude;
    }
    
    /**
     * Set the longitude of the asset.
     * 
     * @param lon
     *      the longitude of the asset, can be null
     */
    public void setLongitude(final Double lon)
    {
        m_Longitude = lon;
    }
    
    /**
     * Get the altitude of the asset.
     * 
     * @return
     *      the altitude of the asset, can be null
     */
    public Double getAltitude()
    {
        return m_Altitude;
    }
    
    /**
     * Set the altitude of the asset.
     * 
     * @param alt
     *      the altitude of the asset, can be null
     */
    public void setAltitude(final Double alt)
    {
        m_Altitude = alt;
    }
    
    /**
     * Get the bank of the asset.
     * 
     * @return
     *      the bank of the asset, can be null
     */
    public Double getBank()
    {
        return m_Bank;
    }
    
    /**
     * Set the bank of the asset.
     * 
     * @param bank
     *      the bank of the asset, can be null
     */
    public void setBank(final Double bank)
    {
        m_Bank = bank;
    }
    
    /**
     * Get the heading of the asset.
     * 
     * @return
     *      the heading of the asset, can be null
     */
    public Double getHeading()
    {
        return m_Heading;
    }
    
    /**
     * Set the heading of the asset.
     * 
     * @param head
     *      the heading of the asset, can be null
     */
    public void setHeading(final Double head)
    {
        m_Heading = head;
    }
    
    /**
     * Get the elevation of the asset.
     * 
     * @return
     *      the elevation of the asset, can be null
     */
    public Double getElevation()
    {
        return m_Elevation;
    }
    
    /**
     * Set the elevation of the asset.
     * 
     * @param ele
     *      the elevation of the asset, can be null
     */
    public void setElevation(final Double ele)
    {
        m_Elevation = ele;
    }
}
