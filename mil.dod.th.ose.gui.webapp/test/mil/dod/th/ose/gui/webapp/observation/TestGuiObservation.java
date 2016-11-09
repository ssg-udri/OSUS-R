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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.ObservationRef;
import mil.dod.th.core.observation.types.Relationship;
import mil.dod.th.core.types.observation.ObservationSubTypeEnum;
import mil.dod.th.core.types.observation.RelationshipTypeEnum;

/**
 * Test class for the {@link GuiObservation}.
 * @author allenchl
 *
 */
public class TestGuiObservation
{
    private GuiObservation m_SUT;
    
    /**
     * Verify that once a GUI observation is created that the appropriate data
     * is able to be pulled back out of the object.
     */
    @Test
    public void testCreateGuiObs()
    {
        Observation obs = mock(Observation.class);
        
        List<Boolean> foundList = new ArrayList<>();
        foundList.add(true);
        foundList.add(true);
        foundList.add(false);

        List<ObservationSubTypeEnum> typeList = new ArrayList<>();
        typeList.add(ObservationSubTypeEnum.DIGITAL_MEDIA);
        typeList.add(ObservationSubTypeEnum.DETECTION);
        typeList.add(null);
        
        List<RelatedObservationIdentity> obsReferences = makeListOfRelatedObservation(foundList, typeList);
        
        m_SUT = new GuiObservation(obs, obsReferences);
        
        assertThat(m_SUT.getObservation(), is(obs));
        assertThat(m_SUT.getRelatedObservationModels().get(0).getObservationSubType(), 
                is(ObservationSubTypeEnum.DIGITAL_MEDIA));

        assertThat(m_SUT.getRelatedObservationModels().get(0).isFoundInObsStore(), is(true));
        
        assertThat(m_SUT.getRelatedObservationModels().get(1).getObservationSubType(), 
                is(ObservationSubTypeEnum.DETECTION));

        assertThat(m_SUT.getRelatedObservationModels().get(1).isFoundInObsStore(), is(true));
        
        assertThat(m_SUT.getRelatedObservationModels().get(2).getObservationSubType(), 
                nullValue());
        assertThat(m_SUT.getRelatedObservationModels().get(2).isFoundInObsStore(), is(false));
    }
    
    /**
     * Verify that observation references can be retrieved by type.
     */
    @Test
    public void testFindRelatedObservationOfRelation()
    {
        Observation obs = mock(Observation.class);
        
        ObservationRef obsRefChild = mock(ObservationRef.class);
        Relationship childRel = new Relationship();
        childRel.setRelationshipType(RelationshipTypeEnum.CHILD);
        when(obsRefChild.getRelationship()).thenReturn(childRel);
        
        ObservationRef obsRefPeer = mock(ObservationRef.class);
        Relationship peerRel = new Relationship();
        peerRel.setRelationshipType(RelationshipTypeEnum.PEER);
        when(obsRefPeer.getRelationship()).thenReturn(peerRel);
        
        ObservationRef obsRefParent = mock(ObservationRef.class);
        Relationship parentRel = new Relationship();
        parentRel.setRelationshipType(RelationshipTypeEnum.PARENT);
        when(obsRefParent.getRelationship()).thenReturn(parentRel);
        
        List<ObservationRef> ref = new ArrayList<>();
        ref.add(obsRefChild);
        when(obs.getRelatedObservations()).thenReturn(ref);
        
        List<Boolean> foundList = new ArrayList<>();
        foundList.add(true);
        foundList.add(true);

        List<ObservationSubTypeEnum> typeList = new ArrayList<>();
        typeList.add(ObservationSubTypeEnum.DIGITAL_MEDIA);
        typeList.add(ObservationSubTypeEnum.DETECTION);
        
        List<RelatedObservationIdentity> obsReferences = makeListOfRelatedObservation(foundList, typeList);
        
        m_SUT = new GuiObservation(obs, obsReferences);
        
        List<Integer> indexes = m_SUT.findRelatedObservationOfRelation(RelationshipTypeEnum.CHILD);
        assertThat(indexes.size(), is(1));
        assertThat(indexes.get(0), is(0));
        assertThat(m_SUT.findRelatedObservationOfRelation(RelationshipTypeEnum.PEER).size(), is(0));
        assertThat(m_SUT.findRelatedObservationOfRelation(RelationshipTypeEnum.PARENT).size(), is(0));
        
        ref.clear();
        ref.add(obsRefPeer);
        
        when(obs.getRelatedObservations()).thenReturn(ref);
        m_SUT = new GuiObservation(obs, obsReferences);
        
        indexes = m_SUT.findRelatedObservationOfRelation(RelationshipTypeEnum.PEER);
        assertThat(indexes.size(), is(1));
        assertThat(indexes.get(0), is(0));
        assertThat(m_SUT.findRelatedObservationOfRelation(RelationshipTypeEnum.CHILD).size(), is(0));
        assertThat(m_SUT.findRelatedObservationOfRelation(RelationshipTypeEnum.PARENT).size(), is(0));
        
        ref.clear();
        ref.add(obsRefParent);
        
        when(obs.getRelatedObservations()).thenReturn(ref);
        m_SUT = new GuiObservation(obs, obsReferences);
        
        indexes = m_SUT.findRelatedObservationOfRelation(RelationshipTypeEnum.PARENT);
        assertThat(indexes.size(), is(1));
        assertThat(indexes.get(0), is(0));
        assertThat(m_SUT.findRelatedObservationOfRelation(RelationshipTypeEnum.CHILD).size(), is(0));
        assertThat(m_SUT.findRelatedObservationOfRelation(RelationshipTypeEnum.PEER).size(), is(0));
        
        ref.clear();
        ref.add(obsRefPeer);
        ref.add(obsRefChild);
        ref.add(obsRefParent);
        
        when(obs.getRelatedObservations()).thenReturn(ref);
        m_SUT = new GuiObservation(obs, obsReferences);
        
        indexes = m_SUT.findRelatedObservationOfRelation(RelationshipTypeEnum.PARENT);
        assertThat(indexes.size(), is(1));
        assertThat(indexes.get(0), is(2));
        indexes = m_SUT.findRelatedObservationOfRelation(RelationshipTypeEnum.CHILD);
        assertThat(indexes.size(), is(1));
        assertThat(indexes.get(0), is(1));
        indexes = m_SUT.findRelatedObservationOfRelation(RelationshipTypeEnum.PEER);
        assertThat(indexes.size(), is(1));
        assertThat(indexes.get(0), is(0));
    }
    
    /**
     * Creates a list of related observation identity models. Assumes both lists are the same size and
     * that each ith position of each list relate to one related observation.
     * @param foundList
     *  whether or not the observation can be found in the observation store.
     * @param typeList
     *  the type of the related observation
     * @return
     *  a list of related observation identity elements
     */
    private List<RelatedObservationIdentity> makeListOfRelatedObservation(List<Boolean> foundList, 
            List<ObservationSubTypeEnum> typeList)
    {
        List<RelatedObservationIdentity> answer = new ArrayList<>();
        
        for (int i = 0; i < typeList.size(); i++)
        {
            RelatedObservationIdentity id = new RelatedObservationIdentity(foundList.get(i), typeList.get(i));
            answer.add(id);
        }
        
        return answer;
    }
}
