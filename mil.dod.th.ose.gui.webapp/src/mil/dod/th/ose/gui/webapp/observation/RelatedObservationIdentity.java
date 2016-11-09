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
package mil.dod.th.ose.gui.webapp.observation;

import mil.dod.th.core.types.observation.ObservationSubTypeEnum;

/**
 * Class used to indicate the type of the related observation and whether or not that 
 * related observation can be found. 
 * 
 * @author nickmarcucci
 *
 */
public final class RelatedObservationIdentity
{
    /**
     * The type of observation that this related observation is.
     */
    private final ObservationSubTypeEnum m_ObservationSubType;
    
    /**
     * Indicates if related observation can be found in the observation store.
     */
    private final boolean m_FoundInObsStore; 
    
    /**
     * Constructor.
     * @param found
     *  Indicates whether the related observation that this structure can be found in the observation store.
     * @param type
     *  Indicates the type of this related observation.
     */
    public RelatedObservationIdentity(final boolean found, final ObservationSubTypeEnum type)
    {
        m_FoundInObsStore = found;
        m_ObservationSubType = type;
    }
    
    /**
     * Returns the type of the related observation.
     * @return
     *  the type of the related observation
     */
    public ObservationSubTypeEnum getObservationSubType()
    {
        return m_ObservationSubType;
    }
    
    /**
     * Returns whether or not the related observation can be found.
     * @return
     *  true if the observation can be found in the observation store; false otherwise
     */
    public boolean isFoundInObsStore()
    {
        return m_FoundInObsStore;
    }
}