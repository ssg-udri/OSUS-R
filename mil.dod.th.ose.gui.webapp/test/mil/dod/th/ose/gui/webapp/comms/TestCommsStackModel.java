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
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import mil.dod.th.core.remote.proto.CustomCommsMessages.CommType;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ConfigurationWrapper;
import mil.dod.th.ose.gui.webapp.factory.FactoryObjMgr;
import mil.dod.th.ose.gui.webapp.factory.FactoryBaseModel;

import org.junit.Test;

/**
 * Test the ability for the comms stack model to correctly store and return proper values.
 * @author bachmakm
 *
 */
public class TestCommsStackModel 
{
    private static final String COMM_PID = "pid";
    
    private ConfigurationWrapper m_ConfigWrapper = mock(ConfigurationWrapper.class);
    private UUID m_Uuid = UUID.randomUUID();
    
    private CommsLayerTypesMgr m_TypesMgr = mock(CommsLayerTypesMgr.class);
    
    private CommsImage m_CommsImageInterface = new CommsImage();
    
    private FactoryObjMgr m_FactoryMgr = mock(FactoryObjMgr.class);
    
    /**
     * Test the default/initial values set for a model at creation.
     * 
     * Verify that all values are returned as documented.
     */
    @Test
    public void testDefaultValues()
    {
        //create comms stack model
        CommsStackModelImpl model = new CommsStackModelImpl(m_CommsImageInterface);

        //verify default values
        assertThat(model.getCommsTopLayerName(), is(""));
        assertThat(model.getLink(), is(nullValue()));
        assertThat(model.getPhysical(), is(nullValue()));
        assertThat(model.getTransport(), is(nullValue()));
        assertThat(model.isComplete(), is(false));
        assertThat(model.getStackLayers().size(), is(0));
    }
    
    /**
     * Test setter methods of the asset model.
     * 
     * Verify that isX methods update appropriately to newly set values.
     */
    @Test
    public void testSetters()
    {
      //create comms stack model
        CommsStackModelImpl model = new CommsStackModelImpl(m_CommsImageInterface);        
        //set transport
        CommsLayerBaseModel base = new CommsLayerBaseModel(0, m_Uuid, COMM_PID, "clazz", 
                CommType.TransportLayer, m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface);
        model.setTransport(base);
        //Ensure that data is the same since type returned is not same as type passed in setter
        assertThat(model.getTransport().getUuid(), is(base.getUuid()));
        assertThat(model.getTransport().getPid(), is(base.getPid()));
        assertThat(model.getTransport().getControllerId(), is(base.getControllerId()));
        
        //set physical layer
        CommsLayerBaseModel phys = new CommsLayerBaseModel(0, m_Uuid, COMM_PID, 
                "clazz", CommType.PhysicalLink, m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface);
        model.setPhysical(phys);
        //Ensure that data is the same since type returned is not same as type passed in setter
        assertThat(model.getPhysical().getUuid(), is(base.getUuid()));
        assertThat(model.getPhysical().getPid(), is(base.getPid()));
        assertThat(model.getPhysical().getControllerId(), is(base.getControllerId()));
        
        //set link layer
        CommsLayerLinkModelImpl link = new CommsLayerLinkModelImpl(0, m_Uuid, COMM_PID, "clazz", 
                m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface);
        model.setLink(link);
        //Ensure that data is the same since type returned is not same as type passed in setter
        assertThat(model.getLink().getPid(), is(COMM_PID));
        assertThat(model.getLink().getUuid(), is(m_Uuid));
        assertThat(model.getLink().getControllerId(), is(link.getControllerId()));

        //set the stack complete
        model.setStackComplete();
        assertThat(model.isComplete(), is(true));
        
        List<FactoryBaseModel> layersList = model.getStackLayers();
        assertThat(layersList.size(), is(3)); //all three layers are in stack        
    }
    
    /**
     * Test getCommsTopLayer method.  
     * Verify that top layer name is returned.
     */
    @Test
    public void testGetCommsTopLayerName()
    {
        //create comms stack model
        CommsStackModelImpl model = new CommsStackModelImpl(m_CommsImageInterface);
        CommsLayerBaseModel base = new CommsLayerBaseModel(0, m_Uuid, COMM_PID, "clazz",
                CommType.PhysicalLink, m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface);
        CommsLayerBaseModel phys = new CommsLayerBaseModel(0, m_Uuid, COMM_PID, "clazz",
                CommType.PhysicalLink, m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface);
        
        model.setPhysical(phys);
        assertThat(model.getCommsTopLayerName(), is(phys.getName()));
        
        CommsLayerLinkModelImpl link = new CommsLayerLinkModelImpl(0, m_Uuid, COMM_PID, 
                "clazz", m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface);
        model.setLink(link);
        assertThat(model.getCommsTopLayerName(), is(link.getName()));
        
        model.setTransport(base);
        assertThat(model.getCommsTopLayerName(), is(base.getName()));
    }
    
    /**
     * Verify depending on whether or not the comms stack model has a link layer or a transport layer the correct 
     * image is displayed.
     */
    @Test
    public void testGetImage()
    {
        //verify no link layer or transport layer the default image will be shown
        CommsStackModelImpl model = new CommsStackModelImpl(m_CommsImageInterface);
        
        assertThat(model.getImage(), is("thoseIcons/comms/comms_generic.png"));
        
        CommsLayerBaseModel commsBaseModelPhys = mock(CommsLayerBaseModel.class);
        when(commsBaseModelPhys.getImage()).thenReturn("commsPhysLayerImage.png");
        model.setPhysical(commsBaseModelPhys);
        
        //verify when both transport layer and link layer are null the physical image is shown
        assertThat(model.getImage(), is("commsPhysLayerImage.png"));
        
        CommsLayerLinkModel linkModel = mock(CommsLayerLinkModel.class);
        when(linkModel.getImage()).thenReturn("linkLayerImage.png");
        model.setLink(linkModel);
        
        //verify when comms stack model has link layer and transport layer is null the get image on 
        //link layer is called
        assertThat(model.getImage(), is("linkLayerImage.png"));
        
        CommsLayerBaseModel commsBaseModelTrans = mock(CommsLayerBaseModel.class);
        when(commsBaseModelTrans.getImage()).thenReturn("commsTransLayerImage.png");
        
        model.setTransport(commsBaseModelTrans);
        
        //verify when comms stack model has link layer and transport layer trans link image is shown
        assertThat(model.getImage(), is("commsTransLayerImage.png"));
        
        //verify link layer is null and transport layer present the transport layer image is shown
        model = new CommsStackModelImpl(m_CommsImageInterface);
        model.setTransport(commsBaseModelTrans);
        
        assertThat(model.getImage(), is("commsTransLayerImage.png"));
    }
}
