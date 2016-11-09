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
package mil.dod.th.ose.shell;

import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;

import org.apache.felix.service.command.CommandSession;
import org.junit.Before;
import org.junit.Test;

/**
 * @author cweisenborn
 */
public class TestCustomCommCommands
{
    private CustomCommCommands m_SUT;
    private CustomCommsService m_CustomCommService;
    private PhysicalLink m_PhysicalLink;
    private LinkLayer m_LinkLayer;
    private TransportLayer m_TransportLayer;
    
    @Before
    public void setup()
    {
        m_SUT = new CustomCommCommands();

        m_CustomCommService = mock(CustomCommsService.class);
        m_PhysicalLink = mock(PhysicalLink.class);
        m_LinkLayer = mock(LinkLayer.class);
        m_TransportLayer = mock(TransportLayer.class);
        
        m_SUT.setCustomCommsService(m_CustomCommService);
    }

    @Test
    public void testCreatePhysicalLink() throws CCommException, PersistenceFailedException, IllegalArgumentException, 
        IOException
    {
        final String testString = "test";
        
        when(m_CustomCommService.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, testString))
            .thenReturn(m_PhysicalLink);

        assertThat(m_SUT.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT.value(), testString), 
                equalTo(m_PhysicalLink));
    }
    
    @Test
    public void testDeletePhysicalLink() throws IllegalArgumentException, IllegalStateException, CCommException
    {
        final String physicalLinkName = "test";
        
        m_SUT.deletePhysicalLink(physicalLinkName);
        
        verify(m_CustomCommService).deletePhysicalLink(physicalLinkName);
    }
    
    @Test
    public void testCreateLinkLayer() throws CCommException
    {
        String llType = "test";
        
        when(m_CustomCommService.createLinkLayer(llType, m_PhysicalLink.getName())).thenReturn(m_LinkLayer);
        
        assertThat(m_SUT.createLinkLayer(llType, m_PhysicalLink.getName()),
                equalTo(m_LinkLayer));
    }
    
    @Test
    public void testCreateTransportLayer() throws CCommException, PersistenceFailedException
    {
        String tlType = "test";
        String name = "name";
        
        when(m_CustomCommService.createTransportLayer(tlType, name, "linkLayerName")).thenReturn(
                m_TransportLayer);
        
        assertThat(m_SUT.createTransportLayer(tlType, name, "linkLayerName"), equalTo(m_TransportLayer));
    }
    
    @Test
    public void testGetPhysicalLinks()
    {
        @SuppressWarnings("unchecked")
        List<String> stringList = mock(List.class);
        
        when(m_CustomCommService.getPhysicalLinkNames()).thenReturn(stringList);
        
        assertThat(m_SUT.getPhysicalLinkNames(), equalTo(stringList));
    }
    
    @Test
    public void testGetLinkLayer()
    {
        final String linkLayerName = "test";
        
        when(m_CustomCommService.getLinkLayer(linkLayerName)).thenReturn(m_LinkLayer);
        
        assertThat(m_SUT.getLinkLayer(linkLayerName), equalTo(m_LinkLayer));
    }
    
    @Test
    public void testGetLinkLayers()
    {
        @SuppressWarnings("unchecked")
        List<LinkLayer> linkLayerList = mock(List.class);
        
        when(m_CustomCommService.getLinkLayers()).thenReturn(linkLayerList);
        
        assertThat(m_SUT.getLinkLayers(), equalTo(linkLayerList));
    }
    
    @Test
    public void testGetTransportLayer()
    {
        final String transportLayerName = "test";
        
        when(m_CustomCommService.getTransportLayer(transportLayerName)).thenReturn(m_TransportLayer);
        
        assertThat(m_SUT.getTransportLayer(transportLayerName), equalTo(m_TransportLayer));
    }
    
    @Test
    public void testGetTransportLayers()
    {
        @SuppressWarnings("unchecked")
        List<TransportLayer> transportLayerList = mock(List.class);
        
        when(m_CustomCommService.getTransportLayers()).thenReturn(transportLayerList);
        
        assertThat(m_SUT.getTransportLayers(), equalTo(transportLayerList));
    } 
    
    @Test
    public void testPrintDeepSession()
    {
        PrintStream printStream = mock(PrintStream.class);
        CommandSession session = mock(CommandSession.class);
        
        when(session.getConsole()).thenReturn(printStream);
        
        m_SUT.printDeep(session);
        
        verify(m_CustomCommService).printDeep(printStream);
    }
}
