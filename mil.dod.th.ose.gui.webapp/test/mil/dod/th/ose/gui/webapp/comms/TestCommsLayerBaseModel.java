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
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mil.dod.th.core.types.ccomm.LinkLayerTypeEnum;

import mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CommType;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ConfigurationWrapper;
import mil.dod.th.ose.gui.webapp.factory.FactoryObjMgr;

import org.junit.Test;

/**
 * Test model properly maintains values for a layer.
 * @author bachmakm
 *
 */
public class TestCommsLayerBaseModel 
{
    private static final String COMMS_ICON_GENERIC = "thoseIcons/comms/comms_generic.png";
    private static final String COMMS_ICON_CELLULAR = "thoseIcons/comms/comms_cellular.png";
    private static final String COMMS_ICON_SERIAL = "thoseIcons/comms/comms_serial.png";
    private static final String COMM_PID = "pid";
    
    private ConfigurationWrapper m_ConfigWrapper = mock(ConfigurationWrapper.class);
    private CommsLayerTypesMgr m_TypesMgr = mock(CommsLayerTypesMgr.class);
    private CommsLayerBaseModel m_SUT;
    private UUID m_Uuid = UUID.randomUUID();
    private CommsImage m_CommsImageInterface = new CommsImage();
    private FactoryObjMgr m_FactoryMgr = mock(FactoryObjMgr.class);
    
    /**
     * Test the default/initial values set for a model at creation.
     * 
     * Verify that all values are returned as documented.
     */
    @Test
    public void testBaseValues()
    {
        //create comms base model
        m_SUT = new CommsLayerBaseModel(0, m_Uuid, COMM_PID, "clazzName",
                CommType.Linklayer, m_FactoryMgr, m_TypesMgr, 
                m_ConfigWrapper, m_CommsImageInterface);
        
        //verify getters values
        assertThat(m_SUT.getName(), is("Unknown (" + m_Uuid + ")"));
        assertThat(m_SUT.getPid(), is(COMM_PID));
        m_SUT.setPid(null);
        assertThat(m_SUT.getPid(), is(""));
        assertThat(m_SUT.getUuid(), is(m_Uuid));
        assertThat(m_SUT.getControllerId(), is(0));
        assertThat(m_SUT.getCommsClazz(), is("clazzName"));
        assertThat(m_SUT.getType(), is(CommType.Linklayer));
    }
    
    /**
     * Verify correct image path is returned based on capabilities.
     */
    @Test
    public void testGetImage()
    {
        //test transport layer
        m_SUT = new CommsLayerBaseModel(123, m_Uuid, COMM_PID, "clazzName",
                CommType.TransportLayer, m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface);   
        
        TransportLayerCapabilities caps = mock(TransportLayerCapabilities.class);
        
        List<LinkLayerTypeEnum> linkEnum = new ArrayList<LinkLayerTypeEnum>();
        linkEnum.add(LinkLayerTypeEnum.CELLULAR);
        when(m_TypesMgr.getCapabilities(123, "clazzName")).thenReturn(caps);
        when(caps.getLinkLayerModalitiesSupported()).thenReturn(linkEnum);
        
        assertThat(m_SUT.getImage(), is(COMMS_ICON_CELLULAR));
        
        //check empty layer enums case
        when(caps.getLinkLayerModalitiesSupported()).thenReturn(new ArrayList<LinkLayerTypeEnum>());        
        assertThat(m_SUT.getImage(), is(COMMS_ICON_GENERIC));
        
        //test null capabilities return
        when(m_TypesMgr.getCapabilities(123, "clazzName")).thenReturn(null);
        assertThat(m_SUT.getImage(), is(COMMS_ICON_GENERIC));
        
        //test physical link
        m_SUT = new CommsLayerBaseModel(0, m_Uuid, COMM_PID, "clazzName",
                CommType.PhysicalLink, m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface);  
        assertThat(m_SUT.getImage(), is(COMMS_ICON_SERIAL));
        
        //test other
        m_SUT = new CommsLayerBaseModel(0, m_Uuid, COMM_PID, "clazzName", 
                CommType.Linklayer, m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface);
        assertThat(m_SUT.getImage(), is(COMMS_ICON_GENERIC));
    }
}
