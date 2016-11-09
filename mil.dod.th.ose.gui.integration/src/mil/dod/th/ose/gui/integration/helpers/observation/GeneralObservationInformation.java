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
package mil.dod.th.ose.gui.integration.helpers.observation;

import java.util.List;

import mil.dod.th.core.types.MapEntry;

/**
 * Class which defines the expected optional general observation information that will be displayed.
 * It is assumed that system id and observation uuid will always appear in the general information, 
 * and therefore is not represented by this class.
 * @author nickmarcucci
 *
 */
public class GeneralObservationInformation
{
    /**
     * Flag to indicate whether observation has an asset location.
     */
    private boolean m_HasAssetLocation;
    
    /**
     * Flag to indicate whether observation has an asset orientation.
     */
    private boolean m_HasAssetOrientation;
    
    /**
     * Flag to indicate whether observation has a platform orientation.
     */
    private boolean m_HasPlatformOrientation;
    
    /**
     * Flag to indicate whether observation has a pointing location.
     */
    private boolean m_HasPointingLocation;
    
    /**
     * The reserved field map.
     */
    private List<MapEntry> m_ReservedFields;
    
    /**
     * Default constructor.
     */
    public GeneralObservationInformation()
    {
        m_HasAssetLocation = false;
        m_HasAssetOrientation = false;
        m_HasPlatformOrientation = false;
        m_HasPointingLocation = false;
    }
    
    /**
     * Sets whether or not asset location should appear in the general information.
     * @param result
     *  true if asset location should appear; false otherwise
     */
    public void setHasAssetLocation(boolean result)
    {
        m_HasAssetLocation = result;
    }
    
    /**
     * Retrieve whether or not the asset location should appear in the general information.
     * @return
     *  true if information should appear; false otherwise
     */
    public boolean isSetAssetLocation()
    {
        return m_HasAssetLocation;
    }
    
    /**
     * Sets whether or not asset orientation should appear in the general information.
     * @param result
     *  true if asset orientation should appear; false otherwise
     */
    public void setHasAssetOrientation(boolean result)
    {
        m_HasAssetOrientation = result;
    }
    
    /**
     * Retrieve whether or not the asset orientation should appear in the general information.
     * @return
     *  true if information should appear; false otherwise
     */
    public boolean isSetAssetOrientation()
    {
        return m_HasAssetOrientation;
    }
    
    /**
     * Sets whether or not platform orientation should appear in the general information.
     * @param result
     *  true if platform orientation should appear; false otherwise
     */
    public void setHasPlatformOrientation(boolean result)
    {
        m_HasPlatformOrientation = result;
    }
    
    /**
     * Retrieve whether or not the platform orientation should appear in the general information.
     * @return
     *  true if information should appear; false otherwise
     */
    public boolean isSetPlatformOrientation()
    {
        return m_HasPlatformOrientation;
    }
    
    /**
     * Sets whether or not pointing location should appear in the general information.
     * @param result
     *  true if pointing location should appear; false otherwise
     */
    public void setHasPointingLocation(boolean result)
    {
        m_HasPointingLocation = result;
    }
    
    /**
     * Retrieve whether or not the pointing location should appear in the general information.
     * @return
     *  true if information should appear; false otherwise
     */
    public boolean isSetPointingLocation()
    {
        return m_HasPointingLocation;
    }
    
    /**
     * Set the expected reserved field values.
     */
    public GeneralObservationInformation withReservedFields(List<MapEntry> reservedFieldValues)
    {
        m_ReservedFields = reservedFieldValues;
        return this;
    }
    
    /**
     * Get the reserved field values.
     * @return
     * reserved field values
     */
    public List<MapEntry> getReservedFields()
    {
        return m_ReservedFields;
    }
    
    /**
     * Indicates if the observation has reserved field data.
     * @return
     *  true if the observation has reserved field data; false otherwise
     */
    public boolean hasReservedField()
    {
        if (m_ReservedFields == null)
        {
            return false;
        }
        
        return true;
    }
}
