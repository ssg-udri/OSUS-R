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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import mil.dod.th.core.ccomm.link.capability.LinkLayerCapabilities;
import mil.dod.th.core.ccomm.physical.capability.PhysicalLinkCapabilities;
import mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.ose.gui.webapp.utils.FacesContextUtil;

import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.primefaces.context.RequestContext;

/**
 * Test the implementation capabilities of the {@link AddCommsDialogHelperImpl}.
 * @author bachmakm
 *
 */
@PrepareForTest({RequestContext.class})
public class TestAddCommsDialogHelperImpl 
{
    private AddCommsDialogHelperImpl m_SUT;
    private CommsLayerTypesMgr m_CommsLayerTypes;
    private FacesContextUtil m_FacesUtil;
    private CommsMgr m_CommsMgr;

    /**
     * Setup the injected services
     */
    @Before
    public void setUp()
    {
        m_SUT = new AddCommsDialogHelperImpl();
        
        m_CommsLayerTypes = mock(CommsLayerTypesMgr.class);
        m_CommsMgr = mock(CommsMgr.class);
        m_FacesUtil = mock(FacesContextUtil.class);
        m_SUT.setCommsTypeMgr(m_CommsLayerTypes);
        m_SUT.setCommsMgr(m_CommsMgr);
        m_SUT.setFacesContextUtil(m_FacesUtil);
        
        m_SUT.setup();
    }  
    
    /**
     * Test setting the ability for the helper to set and get selected link layer.
     * Verify correct type is returned when requested.
     */
    @Test
    public void testGetSetSelectedLinkLayer()
    {
        //type for link layer
        String type = "linklayer.type";
        
        CommsStackCreationModel model = m_SUT.getCommsCreationModel();

        //check that no type is set
        assertThat(model.getSelectedLinkLayerType(), is(nullValue()));

        //set a name
        model.setSelectedLinkLayerType(type);

        //verify
        assertThat(model.getSelectedLinkLayerType(), is(type));
        
        //verify dialog helper is keeping the updated model
        CommsStackCreationModel checkModel = m_SUT.getCommsCreationModel();
        
        //verify
        assertThat(checkModel.getSelectedLinkLayerType(), is(type));
    }
    
    /**
     * Test the validate value method.
     * Verify that method takes the appropriate action depending on whether value is valid or not.
     */
    @Test
    public void testValidateValue()
    {
        FacesContext context = mock(FacesContext.class);
        UIComponent component = mock(UIComponent.class);        
        Object value = -8675309;
        
        try
        {            
            m_SUT.validatePositiveTimeout(context, component, value);
            fail("Validation exception should be thrown.");
        }
        catch (ValidatorException exception)
        {
            assertThat(exception.getFacesMessage().getDetail(), is("Timeout value must be a positive integer."));
        }
        
        try
        {    
            value = 555;
            m_SUT.validatePositiveTimeout(context, component, value);   
        }
        catch (ValidatorException exception)
        {
            fail("Validation exception should not have been thrown.");
        }
    }
    
    /**
     * Verify all input values are reset to their default values. 
     */
    @Test
    public void testClearAllSelectedValues()
    {
        setAllSelectedValues();
        
        CommsStackCreationModel model = m_SUT.getCommsCreationModel();
        
        //verify values are set
        assertThat(model.getNewLinkName(), is("linkyLink"));
        assertThat(model.getNewTransportName(), is("transyTrans"));
        assertThat(model.isForceAdd(), is(true));
        assertThat(m_SUT.getActiveIndex(), is(1));
        assertThat(model.getSelectedPhysicalType(), is(PhysicalLinkTypeEnum.SPI));
        assertThat(model.getSelectedPhysicalLink(), is("fizzyPhys"));
        assertThat(model.getSelectedTransportLayerType(), is("example.trans.type"));
        assertThat(model.getSelectedLinkLayerType(), is("example.link.type"));
        assertThat(model.getNewPhysicalName(), is("tobyPhys"));     
        
        m_SUT.clearAllSelectedValues();
        
        CommsStackCreationModel modelAgain = m_SUT.getCommsCreationModel();
        
        //verify values are cleared
        assertThat(modelAgain.getNewLinkName(), is(nullValue()));
        assertThat(modelAgain.getNewTransportName(), is(nullValue()));
        assertThat(modelAgain.isForceAdd(), is(false));
        assertThat(m_SUT.getActiveIndex(), is(0));
        assertThat(modelAgain.getSelectedPhysicalType(), is(nullValue()));
        assertThat(modelAgain.getSelectedPhysicalLink(), is(nullValue()));
        assertThat(modelAgain.getSelectedTransportLayerType(), is(nullValue()));
        assertThat(modelAgain.getSelectedLinkLayerType(), is(nullValue()));
        assertThat(modelAgain.getNewPhysicalName(), is(nullValue())); 
    }
    
    /**
     * Verify call to reset state of add comms form is made.
     */
    @Test 
    public void testResetState()
    {
        RequestContext context = mock(RequestContext.class);
        when(m_FacesUtil.getRequestContext()).thenReturn(context);
        
        m_SUT.resetState();
        verify(context).reset("addCommsForm");
    }
       
    /**
     * Verify retrieval of Link Layer Capabilities Description.
     */
    @Test
    public void testGetDescription()
    {
        String className = "linklayertest";
        CommsStackCreationModel model = m_SUT.getCommsCreationModel();
        model.setSelectedLinkLayerType(className);
        
        //Test no capabilities
        when(m_CommsLayerTypes.getCapabilities(123, className)).thenReturn(null);
        assertThat(m_SUT.getLinkLayerDescription(123), is("No Description Found for " + className));       
        
        //Test invalid capabilities
        when(m_CommsLayerTypes.getCapabilities(123, className)).thenReturn("not a Capabilities object");
        assertThat(m_SUT.getLinkLayerDescription(123), is("Invalid Capabilities Object"));       
        
        //Test correct capabilities
        LinkLayerCapabilities llc = mock(LinkLayerCapabilities.class);
        when(llc.getDescription()).thenReturn("linklayerdesc");
        when(m_CommsLayerTypes.getCapabilities(123, className)).thenReturn(llc);
        assertThat(m_SUT.getLinkLayerDescription(123), is("linklayerdesc"));
        
        //Test Physical Link
        className = "physlinktest";
        String linkName = "test";
        model.setSelectedPhysicalLink(linkName);
        PhysicalLinkCapabilities plc = mock(PhysicalLinkCapabilities.class);
        when(plc.getDescription()).thenReturn("physlinkdesc");
        when(m_CommsMgr.getPhysicalClazzByName(linkName, 123)).thenReturn(className);
        when(m_CommsLayerTypes.getCapabilities(123, className)).thenReturn(plc);
        assertThat(m_SUT.getPhysLinkDescription(123), is("physlinkdesc"));
        
        //test physical type
        model.setForceAdd(true);
        className = "physlinktest2";
        model.setSelectedPhysicalType(PhysicalLinkTypeEnum.SERIAL_PORT);
        PhysicalLinkCapabilities plc2 = mock(PhysicalLinkCapabilities.class);
        when(plc2.getDescription()).thenReturn("physlinkdesc2");
        when(m_CommsLayerTypes.getCapabilities(123, className)).thenReturn(plc2);
        when(m_CommsLayerTypes.getPhysicalLinkClassByType(123, PhysicalLinkTypeEnum.SERIAL_PORT)).thenReturn(className);
        assertThat(m_SUT.getPhysLinkDescription(123), is("physlinkdesc2"));
    
        //Test Transport Layer
        className = "translayertest";
        model.setSelectedTransportLayerType(className);
        TransportLayerCapabilities tlc = mock(TransportLayerCapabilities.class);
        when(tlc.getDescription()).thenReturn("translayerdesc");
        when(m_CommsLayerTypes.getCapabilities(123, className)).thenReturn(tlc);
        assertThat(m_SUT.getTransLayerDescription(123), is("translayerdesc"));
        
        //test null layer
        model.setSelectedLinkLayerType(null);
        assertThat(m_SUT.getLinkLayerDescription(123), is("Select a Layer to see Description."));
    }
    
    /**
     * Verify the capabilities key is set and retrievable.
     */
    @Test
    public void testCapsKey()
    {
        m_SUT.setCapsKey("key");
        assertThat(m_SUT.getCapsKey(), is("key"));
    }
    
    /**
     * helper method for setting all input values
     */
    private void setAllSelectedValues()
    {
        CommsStackCreationModel model = m_SUT.getCommsCreationModel();
        model.setNewLinkName("linkyLink");
        model.setNewTransportName("transyTrans");
        model.setForceAdd(true);
        m_SUT.setActiveIndex(1);
        model.setSelectedPhysicalType(PhysicalLinkTypeEnum.SPI);
        model.setSelectedPhysicalLink("fizzyPhys");
        model.setSelectedTransportLayerType("example.trans.type");
        model.setSelectedLinkLayerType("example.link.type");
        model.setNewPhysicalName("tobyPhys");
    }
}
