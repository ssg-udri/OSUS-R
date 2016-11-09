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
package mil.dod.th.ose.gui.webapp.general;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import mil.dod.th.core.asset.capability.AssetCapabilities;
import mil.dod.th.core.ccomm.link.capability.LinkLayerCapabilities;
import mil.dod.th.core.ccomm.physical.capability.PhysicalLinkCapabilities;
import mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities;
import mil.dod.th.ose.gui.webapp.CapabilitiesFakeObjects;

import org.junit.Before;
import org.junit.Test;
import org.primefaces.model.TreeNode;

/**
 * Test class for the {@link CapabilitiesTree}.
 * @author jgold
 *
 */
public class TestCapabilitiesTree 
{
    private CapabilitiesTree m_SUT;

    /**
     * Setup for each test.
     * @throws java.lang.Exception the exception.
     */
    @Before
    public void setUp() throws Exception 
    {
        m_SUT = new CapabilitiesTree();      
    }

    /**
     * Test if null caps behavior.
     */
    @Test
    public void testNullCapsObject()
    {
        //pass null
        TreeNode node = m_SUT.getRoot(null);
        
        //should be an empty node value with the node name being message about 'no caps'
        assertThat(node.getChildCount(), is(1));
        assertThat(node.getChildren().get(0).getData().toString(), is("No Capabilities Document Found. / "));
        
        //verify no caps known
        assertThat(m_SUT.isCapsLoaded(), is(false));
    }
    
    /**
     * Test with AssetCapabilities data.
     */
    @Test
    public void testAssetCapabilities() 
    {
        CapabilitiesFakeObjects fakes = new CapabilitiesFakeObjects();
        AssetCapabilities ac = fakes.genAssetCapabilities();
      
        TreeNode root = m_SUT.getRoot(ac);
        assertThat(root.getChildCount(), is(13));
    }
    
    /**
     * Test building tree from comms capabilities.
     */
    @Test
    public void testCommsCapabilities()
    {
        CapabilitiesFakeObjects fakes = new CapabilitiesFakeObjects();
        PhysicalLinkCapabilities plc = fakes.genPhysLinkCaps();
        LinkLayerCapabilities lc = fakes.genLinkLayerCaps();
        TransportLayerCapabilities tlc = fakes.genTransportLayerCaps();
        
        TreeNode root = m_SUT.getRoot(plc);
        assertThat(root.getChildCount(), is(5));

        root = m_SUT.getRoot(lc);
        assertThat(root.getChildCount(), is(10));
        
        root = m_SUT.getRoot(tlc);
        assertThat(root.getChildCount(), is(7));
    }
}
