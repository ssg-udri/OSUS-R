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

import java.util.ArrayList;
import java.util.List;

import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.ObservationRef;
import mil.dod.th.core.types.observation.RelationshipTypeEnum;

/**
 * This model is a wrapper around {@link Observation}s that supports the types of related observations innately.
 * @author allenchl
 *
 */
public class GuiObservation
{
    /**
     * The observation which this model represents.
     */
    private final Observation m_Observation;
    
    /**
     * Related observation types represented by class which indicates if reference can be found in 
     * the observation store and whether or not it has an observation type. The order of these 
     * types is expected to be the same order of which the {@link mil.dod.th.core.observation.types.ObservationRef}
     * are listed within the represented Observation.
     */
    private final List<RelatedObservationIdentity> m_RelatedObsTypes;

    /**
     * Constructor for this model.
     * @param obs
     *      the observation which this model represents
     * @param relatedObsTypes
     *      related observation types, the order of these types is expected to be the same order of which the 
     *      {@link mil.dod.th.core.observation.types.ObservationRef}s are listed within the represented 
     *      Observation
     */
    public GuiObservation(final Observation obs, final List<RelatedObservationIdentity> relatedObsTypes)
    {
        m_Observation = obs;
        m_RelatedObsTypes = relatedObsTypes;
    }
    
    /**
     * Get the observation that this model is wrapping.
     * @return
     *      the observation which this model is wrapping
     */
    public Observation getObservation()
    { 
        return m_Observation; 
    }
    
    /**
     * Get the list of models representing related observations.
     * @return
     *      the list of related observation identity models
     */
    public List<RelatedObservationIdentity> getRelatedObservationModels()
    { 
        return m_RelatedObsTypes;
    }
    
    /**
     * Method to return all the indexes of observations that match a specific relation type.
     * @param relation
     *  the relationship type enum that is to be determined for
     * @return
     *  the list of indexes of all observation references that match the given relationship type
     */
    public List<Integer> findRelatedObservationOfRelation(final RelationshipTypeEnum relation)
    {
        final List<Integer> relatedIndexes = new ArrayList<>();
        final List<ObservationRef> listOfRefs = m_Observation.getRelatedObservations();
        
        for (int i = 0; i < listOfRefs.size(); i++)
        {
            if (listOfRefs.get(i).getRelationship().getRelationshipType() == relation)
            {
                relatedIndexes.add(i);
            }
        }
        
        return relatedIndexes;
    }
}
