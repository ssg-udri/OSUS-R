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

import java.util.ArrayList;
import java.util.List;

import mil.dod.th.core.types.observation.RelationshipTypeEnum;

/**
 * Structure used to represent the possible relations that an observation could have.
 * @author nickmarcucci
 *
 */
public class RelatedObservation
{
    /**
     * List of parents that a related observation would have.
     */
    private List<RelatedObsStruct> m_Parents;
    
    /**
     * List of children that a related observation would have.
     */
    private List<RelatedObsStruct> m_Children;
    
    /**
     * List of peers that a related observation would have.
     */
    private List<RelatedObsStruct> m_Peers;
    
    /**
     * Constructor
     */
    public RelatedObservation()
    {
        m_Parents = new ArrayList<>();
        m_Children = new ArrayList<>();
        m_Peers = new ArrayList<>();
    }
    
    /**
     * Method adds parents to the list of parents.
     * @param obs
     *  the related observation structure that holds the parent observation
     *  and the description that is shown for the entry
     * @return
     *  the RelatedObservation object
     */
    public RelatedObservation withParents(RelatedObsStruct ... obs)
    {
        for (RelatedObsStruct parent : obs)
        {
            m_Parents.add(parent);
        }
        
        return this;
    }
    
    /**
     * Method adds children to the list of children.
     * @param obs
     *  the related observation structure that holds the child observation
     *  and the description that is shown for the entry
     * @return
     *  the RelatedObservation object
     */
    public RelatedObservation withChildren(RelatedObsStruct ... obs)
    {
        for (RelatedObsStruct child : obs)
        {
            m_Children.add(child);
        }
        
        return this;
    }
    
    /**
     * Method adds peers to the list of peers.
     * @param obs
     *  the related observation structure that holds the peer observation
     *  and the description that is shown for the entry
     * @return
     *  the RelatedObservation object
     */
    public RelatedObservation withPeers(RelatedObsStruct ... obs)
    {
        for (RelatedObsStruct peer : obs)
        {
            m_Peers.add(peer);
        }
        
        return this;
    }
    
    /**
     * Retrieve the list of known parents for this relation
     * @return
     *  the list of related observations
     */
    public List<RelatedObsStruct> getParents()
    {
        return m_Parents;
    }
    
    /**
     * Retrieve the list of known children for this relation
     * @return
     *  the list of related observations
     */
    public List<RelatedObsStruct> getChildren()
    {
        return m_Children;
    }
    
    /**
     * Retrieve the list of known peers for this relation
     * @return
     *  the list of related observations
     */
    public List<RelatedObsStruct> getPeers()
    {
        return m_Peers;
    }
    
    /**
     * Function to retrieve the list of relations for a given relationship type.
     * @param type
     *  the type that represents the desired list to retrieve
     * @return
     *  the list of relations that are of the type specified
     */
    public List<RelatedObsStruct> getListForRelation(RelationshipTypeEnum type)
    {
        switch (type)
        {
            case PARENT:
                return m_Parents;
            case CHILD:
                return m_Children;
            case PEER:
                return m_Peers;
            default:
                throw new IllegalArgumentException(
                        String.format("Invalid relationship type %s", type));
        }
    }
    
    /**
     * Method returns the number of sections that should be shown based on 
     * whether or not the lists of parents, children, and peers have items
     * in their relationship lists.
     * @return
     *  the count of the number of sections that should be displayed.
     */
    public int getNumberOfSections()
    {
        int count = 0;
        
        if (m_Parents.size() > 0)
        {
            count++;
        }
        
        if (m_Children.size() > 0)
        {
            count++;
        }
        
        if (m_Peers.size() > 0)
        {
            count++;
        }
        
        return count;
    }
}
