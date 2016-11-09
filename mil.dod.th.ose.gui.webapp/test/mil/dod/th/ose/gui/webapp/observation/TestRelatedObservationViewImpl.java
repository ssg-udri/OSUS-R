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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

/**
 * Test class for {@link RelatedObservationViewImpl} class.
 * 
 * @author cweisenborn
 */
public class TestRelatedObservationViewImpl
{
    private static final UUID OBS_UUID_1 = UUID.randomUUID();
    private static final UUID OBS_UUID_2 = UUID.randomUUID();
    private static final UUID OBS_UUID_3 = UUID.randomUUID();
    
    private RelatedObservationViewImpl m_SUT;
    private ObservationMgr m_ObsMgr;
    
    private GuiObservation m_GuiObs1;
    private GuiObservation m_GuiObs2;
    private GuiObservation m_GuiObs3;
    
    /**
     * Setup dependencies.
     */
    @Before
    public void setup()
    {
        m_SUT = new RelatedObservationViewImpl();
        m_ObsMgr = mock(ObservationMgr.class);
        mockObservations();
        
        m_SUT.setObsMgr(m_ObsMgr);
    }
    
    /**
     * Verify that the initialized method function appropriately.
     */
    @Test
    public void testInitialize()
    {
        m_SUT.initialize(OBS_UUID_1);
        assertThat(m_SUT.getObservation(), is(m_GuiObs1));
        assertThat(m_SUT.canMoveBack(), is(false));
        assertThat(m_SUT.canMoveForward(), is(false));
    }
    
    /**
     * Verify that the initialize method clears a all stored information on previously viewed related observations.
     */
    @Test
    public void testInitializeClearData()
    {
        m_SUT.initialize(OBS_UUID_2);
        m_SUT.setCurrentNode(OBS_UUID_1);
        
        assertThat(m_SUT.getObservation(), is(m_GuiObs1));
        assertThat(m_SUT.canMoveBack(), is(true));
        assertThat(m_SUT.canMoveForward(), is(false));
        
        m_SUT.initialize(OBS_UUID_3);
        assertThat(m_SUT.getObservation(), is(m_GuiObs3));
        assertThat(m_SUT.canMoveBack(), is(false));
        assertThat(m_SUT.canMoveForward(), is(false));
    }
    
    /**
     * Verify the set current node method sets the current observation appropriately.
     */
    @Test
    public void testSetCurrentNode()
    {
        //Initialize the list of viewed related observations.
        m_SUT.initialize(OBS_UUID_1);
        assertThat(m_SUT.getObservation(), is(m_GuiObs1));
        
        //Set the current observations and verify that it is actually set.
        m_SUT.setCurrentNode(OBS_UUID_2);
        assertThat(m_SUT.getObservation(), is(m_GuiObs2));
        
        //Set the current observation, verify it is set, and that the history of the other observations is stored.
        m_SUT.setCurrentNode(OBS_UUID_3);
        assertThat(m_SUT.getObservation(), is(m_GuiObs3));
        assertThat(m_SUT.canMoveBack(), is(true));
        
        //Call the back method and verify that the previous observation is returned.
        m_SUT.back();
        assertThat(m_SUT.getObservation(), is(m_GuiObs2));
        
        m_SUT.back();
        assertThat(m_SUT.getObservation(), is(m_GuiObs1));
        assertThat(m_SUT.canMoveBack(), is(false));
        assertThat(m_SUT.canMoveForward(), is(true));
        
        m_SUT.setCurrentNode(OBS_UUID_3);
        assertThat(m_SUT.getObservation(), is(m_GuiObs3));
        assertThat(m_SUT.canMoveBack(), is(true));
        assertThat(m_SUT.canMoveForward(), is(false));
    }
    
    /**
     * Verify that the get observation method returns the appropriate observation.
     */
    @Test
    public void testGetObservation()
    {
        assertThat(m_SUT.getObservation(), is(nullValue()));
        
        m_SUT.initialize(OBS_UUID_1);
        assertThat(m_SUT.getObservation(), is(m_GuiObs1));
    }
    
    /**
     * Verify that the can move forward method returns the appropriate boolean value.
     */
    @Test
    public void testCanMoveForward()
    {
        assertThat(m_SUT.canMoveForward(), is(false));
        
        m_SUT.initialize(OBS_UUID_1);
        m_SUT.setCurrentNode(OBS_UUID_2);
        m_SUT.setCurrentNode(OBS_UUID_3);
        m_SUT.back();
        
        assertThat(m_SUT.canMoveForward(), is(true));
    }
    
    /**
     * Verify that the can move backward method returns the appropriate boolean value.
     */
    @Test
    public void testCanMoveBackward()
    {
        assertThat(m_SUT.canMoveBack(), is(false));
        
        m_SUT.initialize(OBS_UUID_1);
        m_SUT.setCurrentNode(OBS_UUID_2);
        m_SUT.setCurrentNode(OBS_UUID_3);
        m_SUT.back();
        
        assertThat(m_SUT.canMoveBack(), is(true));
    }
    
    /**
     * Verify that the back and forward method appropriately sets the current node to the correctly view related
     * observation.
     */
    @Test
    public void testBackForward()
    {
        m_SUT.initialize(OBS_UUID_1);
        m_SUT.setCurrentNode(OBS_UUID_2);
        assertThat(m_SUT.getObservation(), is(m_GuiObs2));
        assertThat(m_SUT.canMoveForward(), is(false));
        
        m_SUT.back();
        assertThat(m_SUT.getObservation(), is(m_GuiObs1));
        assertThat(m_SUT.canMoveForward(), is (true));
        
        m_SUT.forward();
        assertThat(m_SUT.getObservation(), is(m_GuiObs2));
        assertThat(m_SUT.canMoveForward(), is(false));
    }
    
    /**
     * Method that mocks the observations returned by the observation manager.
     */
    private void mockObservations()
    {
        m_GuiObs1 = mock(GuiObservation.class);
        m_GuiObs2 = mock(GuiObservation.class);
        m_GuiObs3 = mock(GuiObservation.class);
        
        when(m_ObsMgr.getObservation(OBS_UUID_1)).thenReturn(m_GuiObs1);
        when(m_ObsMgr.getObservation(OBS_UUID_2)).thenReturn(m_GuiObs2);
        when(m_ObsMgr.getObservation(OBS_UUID_3)).thenReturn(m_GuiObs3);
    }
}