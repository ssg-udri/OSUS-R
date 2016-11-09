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
package mil.dod.th.ose.gui.webapp.comms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;


import org.junit.Before;
import org.junit.Test;

/**
 * Test class for {@link CommsStackCreationModel}.
 * @author allenchl
 *
 */
public class TestCommsStackCreationModel 
{
    private CommsStackCreationModel m_SUT;

    /**
     * Setup the injected services
     */
    @Before
    public void setUp()
    {
        m_SUT = new CommsStackCreationModel();
    }  
    
    /**
     * Verify field value initialize at null, 0, or false depending on the type of the field.
     * Verify able to reset field values.
     */
    @Test
    public void testInit()
    {
        //name for layers
        String name = "James";
        
        //check that no name is set
        assertThat(m_SUT.getNewLinkName(), is(nullValue()));
        assertThat(m_SUT.getNewTransportName(), is(nullValue()));
        assertThat(m_SUT.getNewPhysicalName(), is(nullValue()));
        assertThat(m_SUT.isForceAdd(), is(false));

        //set a name
        m_SUT.setNewLinkName(name);
        m_SUT.setNewTransportName(name);
        m_SUT.setNewPhysicalName(name);
        m_SUT.setForceAdd(true);

        //verify
        assertThat(m_SUT.getNewLinkName(), is(name));
        assertThat(m_SUT.getNewTransportName(), is(name));
        assertThat(m_SUT.getNewPhysicalName(), is(name));
        assertThat(m_SUT.isForceAdd(), is(true));
        
        //set null name and verify empty
        m_SUT.setNewLinkName(null);
        m_SUT.setNewTransportName(null);
        m_SUT.setNewPhysicalName(null);
        
        assertThat(m_SUT.getNewLinkName(), is(""));
        assertThat(m_SUT.getNewTransportName(), is(""));
        assertThat(m_SUT.getNewPhysicalName(), is(""));
    }
    
    /**
     * Test setting the ability for the model to set and get selected link layer.
     * Verify correct type is returned when requested.
     */
    @Test
    public void testGetSetSelectedLinkLayer()
    {
        //type for link layer
        String type = "linklayer.type";

        //check that no type is set
        assertThat(m_SUT.getSelectedLinkLayerType(), is(nullValue()));

        //set a type
        m_SUT.setSelectedLinkLayerType(type);

        //verify
        assertThat(m_SUT.getSelectedLinkLayerType(), is(type));
    }
    
    /**
     * Test setting the ability for the model to set and get selected transport layer.
     * Verify correct type is returned when requested.
     */
    @Test
    public void testGetSetSelectedTransportLayer()
    {
        //type of layer
        String type = "transportlayer.type";

        //check that no type is set
        assertThat(m_SUT.getSelectedTransportLayerType(), is(nullValue()));

        //set a type
        m_SUT.setSelectedTransportLayerType(type);

        //verify
        assertThat(m_SUT.getSelectedTransportLayerType(), is(type));
    }
    
    /**
     * Test setting the ability for the model to set and get selected physical layer.
     * Verify correct type is returned when requested.
     */
    @Test
    public void testGetSetSelectedPhysicalLayer()
    {
        //name given to physical layer
        String name = "fizzyFizz";

        //check that no name is set
        assertThat(m_SUT.getSelectedPhysicalLink(), is(nullValue()));

        //set a name
        m_SUT.setSelectedPhysicalLink(name);

        //verify
        assertThat(m_SUT.getSelectedPhysicalLink(), is(name));
    }
    
    /**
     * Test setting the ability for the model to set and get selected physical layer type.
     * Verify correct type is returned when requested.
     */
    @Test
    public void testGetSetSelectedPhysicalType()
    {
        //check that no type is set
        assertThat(m_SUT.getSelectedPhysicalType(), is(nullValue()));

        //set a type
        m_SUT.setSelectedPhysicalType(PhysicalLinkTypeEnum.GPIO);

        //verify
        assertThat(m_SUT.getSelectedPhysicalType(), is(PhysicalLinkTypeEnum.GPIO));     
    }
}
