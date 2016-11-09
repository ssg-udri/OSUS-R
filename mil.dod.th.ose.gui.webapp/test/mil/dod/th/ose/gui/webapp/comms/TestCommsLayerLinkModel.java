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
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.link.LinkLayer.LinkStatus;
import mil.dod.th.core.ccomm.link.capability.LinkLayerCapabilities;
import mil.dod.th.core.types.ccomm.LinkLayerTypeEnum;

import mil.dod.th.ose.gui.webapp.advanced.configuration.ConfigurationWrapper;
import mil.dod.th.ose.gui.webapp.factory.FactoryObjMgr;

import org.junit.Before;
import org.junit.Test;

/**
 * Test the ability for the link layer model to correctly store and return proper values.
 * @author bachmakm
 *
 */
public class TestCommsLayerLinkModel 
{
    private static final String COMMS_ICON_GENERIC = "thoseIcons/comms/comms_generic.png";
    private static final String COMMS_ICON_CELLULAR = "thoseIcons/comms/comms_cellular.png";
    private static final String COMM_PID = "pid";
    
    private UUID m_Uuid = UUID.randomUUID();
    
    private ConfigurationWrapper m_ConfigWrapper = mock(ConfigurationWrapper.class); 
    private CommsLayerTypesMgr m_TypesMgr = mock(CommsLayerTypesMgr.class);
    private CommsLayerLinkModelImpl m_SUT;
    private CommsImage m_CommsImageInterface;
    private FactoryObjMgr m_FactoryMgr = mock(FactoryObjMgr.class);

    @Before
    public void init()
    {
        m_CommsImageInterface = new CommsImage();
        
        m_SUT = new CommsLayerLinkModelImpl(123, m_Uuid, COMM_PID, "clazz", 
                m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface);
    }
    
    /**
     * Test the default/initial values set for a model at creation.
     * 
     * Verify that all values are returned as documented.
     */
    @Test
    public void testDefaultValues()
    {        
        //verify default values
        assertThat(m_SUT.getUuid(), is(m_Uuid));
        assertThat(m_SUT.getControllerId(), is(123));
        assertThat(m_SUT.getPid(), is(COMM_PID));
        m_SUT.setPid(null);
        assertThat(m_SUT.getPid(), is(""));
        assertThat(m_SUT.getStatus(), is(nullValue()));
        assertThat(m_SUT.getStatusString(), is("Unknown"));
        assertThat(m_SUT.isActivated(), is(nullValue()));
        assertThat(m_SUT.isMetadataComplete(), is(false));
        assertThat(m_SUT.getCommsClazz(), is("clazz"));
    }
    
    /**
     * Test setter methods of the link layer model.
     * 
     * Verify that methods update appropriately to newly set values.
     */
    @Test
    public void testSetters()
    {
        //create link layer model
        CommsLayerLinkModelImpl model = new CommsLayerLinkModelImpl(123, m_Uuid, COMM_PID, "clazz", 
                m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface);

        model.setActivated(true);
        assertThat(model.isActivated(), is(true));
        
        model.setStatus(LinkLayer.LinkStatus.OK);
        assertThat(model.getStatus(), is(LinkLayer.LinkStatus.OK));
        assertThat(model.getStatusString(), is("OK"));
        
        model.setStatus(LinkLayer.LinkStatus.LOST);
        assertThat(model.getStatus(), is(LinkLayer.LinkStatus.LOST));
        assertThat(model.getStatusString(), is("LOST"));
        
        assertThat(model.isMetadataComplete(), is(true));
    }
    
    /**
     * Verify metadata is not completed until the model is activated and the status is set.
     */
    @Test
    public void testIsMetadataComplete()
    {
        //create link layer model
        CommsLayerLinkModelImpl model = new CommsLayerLinkModelImpl(123, m_Uuid, COMM_PID, "clazz", 
                m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface);
        
        assertThat(model.isMetadataComplete(), is(false));
        model.setActivated(true);
        
        assertThat(model.isMetadataComplete(), is(false));
        model.setStatus(LinkStatus.OK);
        
        assertThat(model.isMetadataComplete(), is(true));
    }
    
    /**
     * Verify correct image path is returned based on capabilities.
     */
    @Test
    public void testGetImage()
    {
        assertThat(m_SUT.getImage(), is(COMMS_ICON_GENERIC));
        
        LinkLayerCapabilities caps = mock(LinkLayerCapabilities.class);
        when(m_TypesMgr.getCapabilities(123, "clazz")).thenReturn(caps);
        
        assertThat(m_SUT.getImage(), is(COMMS_ICON_GENERIC));
        
        when(caps.getModality()).thenReturn(LinkLayerTypeEnum.CELLULAR);
        
        assertThat(m_SUT.getImage(), is(COMMS_ICON_CELLULAR));
    }
    
    /**
     * Verify the isComplete method returns true when all information for the link layer is available
     */
    @Test
    public void testIsComplete()
    {
        assertThat(m_SUT.isComplete(), is(false));
        m_SUT.setActivated(true);
        assertThat(m_SUT.isComplete(), is(false));
        m_SUT.setStatus(LinkStatus.OK);
        assertThat(m_SUT.isComplete(), is(false));
        
        LinkLayerCapabilities caps = mock(LinkLayerCapabilities.class);
        when(m_TypesMgr.getCapabilities(123, "clazz")).thenReturn(caps);
        
        m_SUT.updateName("newName");
        assertThat(m_SUT.getName(), is("newName"));
        
        assertThat(m_SUT.isComplete(), is(true));
    }
}
