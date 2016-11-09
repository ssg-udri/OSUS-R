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
package mil.dod.th.ose.core.impl.ccomm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.UnmarshalException;

import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.core.impl.ccomm.link.LinkLayerInternal;
import mil.dod.th.ose.core.impl.ccomm.physical.PhysicalLinkInternal;
import mil.dod.th.ose.core.impl.ccomm.transport.TransportLayerInternal;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;

/**
 * This test class reflects the {@link CustomCommsServiceImpl} tests for System tasks.
 * 
 * @author callen
 *
 */
public class TestCustomCommsServiceImpl_System extends CustomCommsServiceImpl_TestCommon 
{
    @Before
    public void setUp() throws Exception
    {
        stubServices();
    }

    /**
     * Verify that the correct services are passed to the init of the registries for physical links.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testPhysicalLinkActivate() throws InvalidSyntaxException
    {
        verify(serviceContextFactory, times(3)).newInstance(null); // will be a call for each
        verify(m_PhysicalLinkServiceContext).initialize(m_Context, m_PhysicalLinkProxy, m_SUT);
    }
    
    /**
     * Verify that the correct services are passed to the init of the registries for link layers.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testLinkLayerActivate() throws InvalidSyntaxException
    {
        verify(serviceContextFactory, times(3)).newInstance(null); // will be a call for each
        verify(m_LinkLayerServiceContext).initialize(m_Context, m_LinkLayerProxy, m_SUT);

    }
    
    /**
     * Verify that the correct services are passed to the init of the registries for transport layers.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testTransportLayerActivate() throws InvalidSyntaxException
    {
        verify(serviceContextFactory, times(3)).newInstance(null); // will be a call for each
        verify(m_TransportLayerServiceContext).initialize(m_Context, m_TransportLayerProxy, m_SUT);

    }
    
    /**
     * Verify deactivate will dispose of all {@link mil.dod.th.ose.core.factory.api.FactoryServiceContext} components.
     */
    @Test
    public void testDeactivate()
    {
        m_SUT.deactivate();
        
        verify(m_PhysicalLinkComp).dispose();
        verify(m_LinkLayerComp).dispose();
        verify(m_TransportLayerComp).dispose();
    }
    
    /**
     * Tests correct output from print deep for all custom comms layer types.
     * 
     * NOTE: this test is whitespace sensitive.
     */
    @Test
    public void testPrintDeep() 
        throws CCommException, IOException, InterruptedException, IllegalArgumentException, IllegalStateException, 
            FactoryException, UnmarshalException
    {
        //physical link mocks
        String phys1 = "phys1";
        String phys2 = "phys2";
        Map<String, Object> physPropKeys = new HashMap<>();
        physPropKeys.put("someKey","someValue");
        PhysicalLinkInternal physInt1 = mock(PhysicalLinkInternal.class);
        when(physInt1.getName()).thenReturn(phys1);
        when(physInt1.getProperties()).thenReturn(physPropKeys);
        
        PhysicalLinkInternal physInt2 = mock(PhysicalLinkInternal.class);
        when(physInt2.getName()).thenReturn(phys2);
        Set<PhysicalLinkInternal> physLinks = new HashSet<>();
        physLinks.add(physInt1);
        physLinks.add(physInt2);
        when(m_PhysRegistry.getObjects()).thenReturn(physLinks);
        
        //link layer mocks
        LinkLayerInternal linkInt1 = mock(LinkLayerInternal.class);
        when(linkInt1.getName()).thenReturn("link1");
        LinkLayerInternal linkInt2 = mock(LinkLayerInternal.class);
        when(linkInt2.getName()).thenReturn("link2");
        when(linkInt2.getPhysicalLink()).thenReturn(physInt2);
        LinkLayerInternal linkInt3 = mock(LinkLayerInternal.class);
        when(linkInt3.getName()).thenReturn("link3");
        Set<LinkLayerInternal> links = new HashSet<>();
        links.add(linkInt1);
        links.add(linkInt2);
        links.add(linkInt3);
        when(m_LinkRegistry.getObjects()).thenReturn(links);
        
        //trans layer mocks
        TransportLayerInternal transInt1 = mock(TransportLayerInternal.class);
        when(transInt1.getName()).thenReturn("trans1");
        TransportLayerInternal transInt2 = mock(TransportLayerInternal.class);
        when(transInt2.getName()).thenReturn("trans2");
        when(transInt2.getLinkLayer()).thenReturn(linkInt2);
        TransportLayerInternal transInt3 = mock(TransportLayerInternal.class);
        when(transInt3.getName()).thenReturn("trans3");
        when(transInt3.getLinkLayer()).thenReturn(linkInt3);
        Set<TransportLayerInternal> transLinks = new HashSet<>();
        transLinks.add(transInt1);
        transLinks.add(transInt2);
        transLinks.add(transInt3);
        when(m_TransRegistry.getObjects()).thenReturn(transLinks);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);
        m_SUT.printDeep(printStream);
        
        //assert phys attrs
        assertThat(out.toString(), 
                containsString("phys1: Open: false, InUse: false, Properties:"));
        assertThat(out.toString(), 
                containsString("someKey = someValue"));
        assertThat(out.toString(), 
                containsString("phys2: Open: false, InUse: false, Properties:"));
        
        //assert link layer attrs
        assertThat(out.toString(), containsString("link1: Physical Link: <None>"));
        assertThat(out.toString(), containsString("link2: Physical Link: phys2"));
        assertThat(out.toString(), containsString("link3: Physical Link: <None>"));
        
        //assert trans attrs
        assertThat(out.toString(), 
                containsString("trans1: Link Layer: <None>, Physical Link: <None>"));
        assertThat(out.toString(), 
                containsString("trans2: Link Layer: link2, Physical Link: phys2"));
        assertThat(out.toString(), 
                containsString("trans3: Link Layer: link3, Physical Link: <None>"));
        
        //general header and footer
        assertThat(out.toString(), 
                containsString("****************Printing Deep:"));
        assertThat(out.toString(), 
                containsString("****************End of print deep"));
        assertThat(out.toString(), 
                containsString("------Transport Layer------"));
        assertThat(out.toString(), 
                containsString("------Link Layer-----------"));
        assertThat(out.toString(), 
                containsString("------PhysicalLink Layer-------"));
    }
}
