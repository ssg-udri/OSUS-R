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
package mil.dod.th.ose.gui.webapp.channel;

import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.ccomm.transport.TransportLayerFactory;
import mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.types.ccomm.LinkLayerTypeEnum;
import mil.dod.th.core.types.remote.RemoteChannelTypeEnum;
import mil.dod.th.ose.gui.webapp.channel.ChannelGuiDialogHelper.NewChannelChoice;
import mil.dod.th.ose.gui.webapp.utils.FacesContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

/**
 * @author nickmarcucci
 *
 */
public class TestChannelGuiDialogHelper
{
    private static final String COMMS_ICON_SOCKET = "thoseIcons/comms/socket.png";
    private static final String COMMS_ICON_GENERIC = "thoseIcons/comms/comms_generic.png";
    
    private ChannelGuiDialogHelperImpl m_SUT;
    private CustomCommsService m_CustomCommsService;
    private AddressManagerService m_AddressManagerService;
    private FacesContextUtil m_FacesUtil;
    private FacesContext m_FacesContext;
    private RemoteChannelLookup m_RemoteChannelLookup;
    private GrowlMessageUtil m_GrowlUtil;
    private final int m_SystemId = 1988;
    
    @Before
    public void setUp()
    {
        m_SUT = new ChannelGuiDialogHelperImpl();
        
        m_CustomCommsService = mock(CustomCommsService.class);
        m_AddressManagerService = mock(AddressManagerService.class);
        m_FacesUtil = mock(FacesContextUtil.class);
        m_FacesContext = mock(FacesContext.class);
        m_RemoteChannelLookup = mock(RemoteChannelLookup.class);
        m_GrowlUtil = mock(GrowlMessageUtil.class);
        
        when(m_FacesUtil.getFacesContext()).thenReturn(m_FacesContext);
        
        m_SUT.setCustomCommsService(m_CustomCommsService);
        m_SUT.setAddressManagerService(m_AddressManagerService);
        m_SUT.setFacesContextUtility(m_FacesUtil);
        m_SUT.setGrowlMessageUtility(m_GrowlUtil);
        m_SUT.setRemoteChannelLookup(m_RemoteChannelLookup);
        
        TransportLayer layer1 = mock(TransportLayer.class);
        TransportLayer layer2 = mock(TransportLayer.class);
        
        TransportLayerFactory transLayer1Factory = mock(TransportLayerFactory.class);
        doReturn(layer1.getClass().toString()).when(transLayer1Factory).getProductType();   
        TransportLayerFactory transLayer2Factory = mock(TransportLayerFactory.class);
        doReturn(layer2.getClass().toString()).when(transLayer2Factory).getProductType();   
        
        when(layer1.getName()).thenReturn("TransportLayer1");
        when(layer2.getName()).thenReturn("TransportLayer2");
        when(layer1.getFactory()).thenReturn(transLayer1Factory);
        when(layer2.getFactory()).thenReturn(transLayer2Factory);
        
        List<TransportLayer> layers = new ArrayList<TransportLayer>();
        layers.add(layer1);
        layers.add(layer2);
        when(m_CustomCommsService.getTransportLayers()).thenReturn(layers);
        
        final Set<TransportLayerFactory> factories = new HashSet<TransportLayerFactory> ();
        factories.add(transLayer1Factory);
        factories.add(transLayer2Factory);
        when(m_CustomCommsService.getTransportLayerFactories()).thenReturn(factories);
    }
    
    /**
     * Verify default value is set.
     * 
     * Verify value can be reset and retrieved
     */
    @Test
    public void testNewChannelControllerId()
    {
        //change and verify
        m_SUT.setNewChannelControllerId(m_SystemId);
        
        assertThat(m_SUT.getNewChannelControllerId(), is(m_SystemId));
    }
    
    /**
     * Verify selected new channel type sets selected channel type variable
     * 
     * Verify if set to transport then transport name is the name of the choice.
     */
    @Test
    public void testSetSelectedNewChannelType()
    {
        NewChannelChoice choice = mock(NewChannelChoice.class);
        when(choice.getChannelName()).thenReturn("Socket");
        when(choice.getChannelType()).thenReturn(RemoteChannelTypeEnum.SOCKET);
        
        m_SUT.setSelectedNewChannelType(choice);
        
        assertThat(m_SUT.getSelectedNewChannelType(), is(RemoteChannelTypeEnum.SOCKET));
        
        NewChannelChoice tchoice = mock(NewChannelChoice.class);
        when(tchoice.getChannelName()).thenReturn("ReallyAwesomeTransport");
        when(tchoice.getChannelType()).thenReturn(RemoteChannelTypeEnum.TRANSPORT);
        
        m_SUT.setSelectedNewChannelType(tchoice);
        
        assertThat(m_SUT.getSelectedNewChannelType(), is(RemoteChannelTypeEnum.TRANSPORT));
        assertThat(m_SUT.getNewTransportName(), is("ReallyAwesomeTransport"));
    }
    
    /**
     * Verify channel choices returns proper NewChannelChoices
     */
    @Test
    public void testGetChannelChoices()
    {
        List<NewChannelChoice> choices = m_SUT.getChannelChoices();
        
        assertThat(choices.size(), is(3));
        
        //Socket choice is always added by default. That is why it is the first one
        //in the list. 
        assertThat(choices.get(0).getChannelName(), is("Socket"));
        assertThat(choices.get(0).getChannelType(), is(RemoteChannelTypeEnum.SOCKET));
        
        assertThat(choices.get(1).getChannelName(), is("TransportLayer1"));
        assertThat(choices.get(1).getChannelType(), is(RemoteChannelTypeEnum.TRANSPORT));
        
        assertThat(choices.get(2).getChannelName(), is("TransportLayer2"));
        assertThat(choices.get(2).getChannelType(), is(RemoteChannelTypeEnum.TRANSPORT));
    }
    
    /**
     * Verify the get image works properly.. channel name of socket returns the socket image, channel with no 
     * capabilities or no list of link layer modalities supported returns the generic comms image, channel with 
     * capabilities and has a list of link layer modalities supported will return the first modality supported in the 
     * list and display the image for it.
     */
    @Test
    public void testChannelChoicesGetImage()
    {
        List<NewChannelChoice> choices = m_SUT.getChannelChoices();
        
        assertThat(choices.size(), is(3));
        
        for (int x = 0; x < choices.size(); x++)
        {
            if (choices.get(x).getChannelName().equals("Socket"))
            {
                assertThat(choices.get(x).getImage(), is(COMMS_ICON_SOCKET));
            }
            else if (choices.get(x).getChannelName().equals("TransportLayer1"))
            {
                assertThat(choices.get(x).getImage(), is(COMMS_ICON_GENERIC));
            }
            else if (choices.get(x).getChannelName().equals("TransportLayer2"))
            {
                TransportLayerCapabilities transCaps = mock(TransportLayerCapabilities.class);
                when(choices.get(x).getCapabilities()).thenReturn(transCaps);
                
                List<LinkLayerTypeEnum> linkLayerMods = new ArrayList<>();
                linkLayerMods.add(LinkLayerTypeEnum.CELLULAR);
                
                when(transCaps.getLinkLayerModalitiesSupported()).thenReturn(linkLayerMods);
                
                choices.get(x).setCapabilities(transCaps);
                
                assertThat(choices.get(x).getImage(), is("thoseIcons/comms/comms_" + 
                        choices.get(x).getCapabilities().getLinkLayerModalitiesSupported().
                            get(0).toString().toLowerCase() + ".png"));
            }
        }
    }
    
    /**
     * Verify that input is properly cleared
     */
    @Test
    public void testClearNewChannelInput()
    {
        m_SUT.setNewTransportName("SetNewTransportName");
        m_SUT.setNewTransportRemoteAddress("TransportRemoteAddress");
        m_SUT.setNewTransportLocalAddress("TransportLocalAddress");
        
        m_SUT.setNewSocketHost("NewSocketHost");
        m_SUT.setNewSocketPort(1);
        m_SUT.setNewSocketSsl(true);
        
        m_SUT.clearNewChannelInput();
       
        assertThat(m_SUT.getNewTransportName(), is(""));
        assertThat(m_SUT.getNewTransportRemoteAddress(), is(""));
        assertThat(m_SUT.getNewTransportLocalAddress(), is(""));
        
        assertThat(m_SUT.isNewSocketSsl(), is(false));
        assertThat(m_SUT.getNewSocketPort(), is(4000));
        assertThat(m_SUT.getNewSocketHost(), is("localhost"));
        assertThat(m_SUT.getNewChannelControllerId(), is(0));
        
        assertThat(m_SUT.getSelectedNewChannelType(), is(RemoteChannelTypeEnum.SOCKET));
    }
    
    /**
     * Verify that a list of transport names are returned.
     */
    @Test
    public void testGetAllKnownTransportLayers()
    {
        List<TransportLayer> received = m_SUT.getAllKnownTransportLayers();
        
        assertThat(received.size(), is(2));
        assertThat(received.get(0).getName().equals("TransportLayer1"), is(true));
        assertThat(received.get(1).getName().equals("TransportLayer2"), is(true));
    }
    
    /**
     * Verify that a list of addresses are returned.
     */
    @Test
    public void testGetAllAddresses()
    {
        List<String> addresses = new ArrayList<String>();
        addresses.add("Address1");
       
        when(m_AddressManagerService.getAddressDescriptiveStrings()).thenReturn(addresses);
       
        List<String> answer = m_SUT.getAllKnownAddresses();
       
        assertThat(answer.size(), is(1));
        assertThat(answer.get(0), is("Address1"));
       
    }
    
    /**
     * Verify that a duplicate socket can be found and indicated.
     */
    @Test
    public void testValidateSocketDoesNotExist()
    {
        UIComponent component = mock(UIComponent.class);
        
        UIInput hostName = mock(UIInput.class);
        UIInput port = mock(UIInput.class);
        when(hostName.getId()).thenReturn("hostName");
        when(hostName.getLocalValue()).thenReturn("localhost");
        
        when(port.getId()).thenReturn("hostPort");
        when(port.getLocalValue()).thenReturn(4000);
        
        when(component.findComponent("hostName")).thenReturn(hostName);
        when(component.findComponent("hostPort")).thenReturn(port);
        
        ComponentSystemEvent event = mock(ComponentSystemEvent.class);
        when(event.getComponent()).thenReturn(component);
        
        when(m_RemoteChannelLookup.checkChannelSocketExists("localhost", 4000)).thenReturn(false);
        m_SUT.validateSocketDoesNotExist(event);
        
        verify(m_GrowlUtil, times(0)).createLocalFacesMessage(eq(FacesMessage.SEVERITY_WARN), 
                eq("Socket Exists"), Mockito.anyString());
        verify(m_FacesUtil, times(0)).getFacesContext();
        verify(m_FacesContext, times(0)).renderResponse();
        
        when(m_RemoteChannelLookup.checkChannelSocketExists("localhost", 4000)).thenReturn(true);
        m_SUT.validateSocketDoesNotExist(event);
        
        verify(m_GrowlUtil, times(1)).createLocalFacesMessage(eq(FacesMessage.SEVERITY_WARN), eq("Socket Exists"), 
                eq("A socket with hostname localhost and port 4000 already exists."));
        verify(m_FacesUtil, times(1)).getFacesContext();
        verify(m_FacesContext, times(1)).renderResponse();
    }
    
    /**
     * Verify that a duplicate transport can be found.
     */
    @Test
    public void testValidateTransportDoesNotExist()
    {
        UIComponent component = mock(UIComponent.class);
        
        UIInput local = mock(UIInput.class);
        UIInput remote = mock(UIInput.class);
        
        when(local.getId()).thenReturn("localAddress");
        when(remote.getId()).thenReturn("remoteAddress");
        
        when(local.getLocalValue()).thenReturn("4000");
        when(remote.getLocalValue()).thenReturn("4000");
        
        when(component.findComponent("localAddress")).thenReturn(local);
        when(component.findComponent("remoteAddress")).thenReturn(remote);
        
        ComponentSystemEvent event = mock(ComponentSystemEvent.class);
        when(event.getComponent()).thenReturn(component);
        
        when(m_RemoteChannelLookup.checkChannelTransportExists("4000", "4000")).thenReturn(false);
        m_SUT.validateTransportDoesNotExist(event);
        
        verify(m_GrowlUtil, times(0)).createLocalFacesMessage(eq(FacesMessage.SEVERITY_WARN), 
                eq("Socket Exists"), Mockito.anyString());
        verify(m_FacesUtil, times(0)).getFacesContext();
        verify(m_FacesContext, times(0)).renderResponse();
        
        //test with a matching transport 
        when(m_RemoteChannelLookup.checkChannelTransportExists("4000", "4000")).thenReturn(true);
        m_SUT.validateTransportDoesNotExist(event);
        
        verify(m_GrowlUtil, times(1)).createLocalFacesMessage(eq(FacesMessage.SEVERITY_WARN), eq("Transport Exists"), 
                eq("A transport with local address 4000 and remote address 4000 already exists."));
        verify(m_FacesUtil, times(1)).getFacesContext();
        verify(m_FacesContext, times(1)).renderResponse();
    }
}
