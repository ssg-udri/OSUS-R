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
package mil.dod.th.ose.core.integration;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import java.io.IOException;

import junit.framework.TestCase;
import example.ccomms.ExampleLinkLayer;
import example.ccomms.ExamplePhysicalLink;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.link.BaseLinkFrame;
import mil.dod.th.core.ccomm.link.LinkFrame;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.link.LinkLayer.LinkStatus;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.ose.integration.commons.CustomCommsUtils;
import mil.dod.th.ose.integration.commons.EventHandlerSyncer;
import mil.dod.th.ose.integration.commons.FactoryUtils;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.event.Event;

/**
 * @author dhumeniuk
 *
 */
public class TestLinkLayer extends TestCase
{
    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    private LinkLayer m_LinkLayer = null;
    private PhysicalLink m_Phys= null;
    private Event m_LastEvent;
    
    @Override
    public void setUp()
    {
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, ExamplePhysicalLink.class, 5000);
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, ExampleLinkLayer.class, 5000);

        CustomCommsUtils.deleteAllLayers(m_Context);        
    }
    
    @Override
    public void tearDown() throws Exception
    {
        CustomCommsUtils.deleteAllLayers(m_Context);
    }

    /**
     * Verify that a {@link LinkLayer} will post an event when it is activated and deactivated.
     */
    public final void testActivateEvent() throws InterruptedException, CCommException, PersistenceFailedException,
        IOException
    { 
        final String linkName = "ActivateEvent";
        createLinkLayer(linkName);
        
        EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context, LinkLayer.TOPIC_ACTIVATED, 
                String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_NAME, linkName));
        
        m_LinkLayer.activateLayer();
        
        syncer.waitForEvent(2);
        
        syncer = new EventHandlerSyncer(m_Context, LinkLayer.TOPIC_DEACTIVATED, String.format("(%s=%s)", 
                FactoryDescriptor.EVENT_PROP_OBJ_NAME, linkName));
       
        m_LinkLayer.deactivateLayer();
        
        syncer.waitForEvent(2);
    }
    
    /**
     * Verify that a {@link LinkLayer} will post an event when it sends data.
     */
    public final void testSendEvent() throws CCommException, InterruptedException
    {
        final String linkName = "SendEvent";
        createLinkLayer(linkName);

        EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context, LinkLayer.TOPIC_DATA_SENT, 
                String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_NAME, linkName));
   
        AddressManagerService addressMgrSvc = ServiceUtils.getService(m_Context, AddressManagerService.class);
        
        Address addr = addressMgrSvc.getOrCreateAddress("Example:30");
        
        //activate the link so that it can send the frame
        m_LinkLayer.activateLayer();
        
        LinkFrame frame = new BaseLinkFrame();
        m_LinkLayer.send(frame, addr);
        
        m_LastEvent = syncer.waitForEvent(2);
        
        assertThat((LinkFrame)m_LastEvent.getProperty(LinkLayer.EVENT_PROP_LINK_FRAME), is(frame));
        assertThat((Address)m_LastEvent.getProperty("dest." + Address.EVENT_PROP_ADDRESS_SUFFIX), is(addr));
        
        m_LinkLayer.deactivateLayer();
    }

    /**
     * Test asking a link layer to perform its BIT.
     * Verify that the status is returned, and the status changed event is posted.
     */
    public final void testPerformBitEvent() throws CCommException, InterruptedException
    {
        final String linkName = "PerformBitEvent";
        createLinkLayer(linkName);
        
        EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context, LinkLayer.TOPIC_STATUS_CHANGED, 
                String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_NAME, linkName));
        
        //perform bit 
        LinkStatus status = m_LinkLayer.performBit();
        assertThat(status, is(LinkStatus.OK));
        syncer.waitForEvent(3);
    }
    
    /**
     * Create a {@link LinkLayer} with the given name.
     * @param name
     *      the name for the given link layer
     */
    private void createLinkLayer(final String name) throws IllegalArgumentException, CCommException
    {
        CustomCommsService customCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);
        assertThat(customCommsService, is(notNullValue()));
        m_Phys = customCommsService.createPhysicalLink(PhysicalLinkTypeEnum.I_2_C);
        m_LinkLayer = customCommsService.createLinkLayer(ExampleLinkLayer.class.getName(), name, m_Phys.getName());

        assertThat(m_LinkLayer, is(notNullValue()));
    }
}
