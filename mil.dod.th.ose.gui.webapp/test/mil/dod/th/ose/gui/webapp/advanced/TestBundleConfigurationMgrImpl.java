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
package mil.dod.th.ose.gui.webapp.advanced;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.proto.BundleMessages;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespace.BundleMessageType;
import mil.dod.th.ose.gui.webapp.controller.ActiveControllerImpl;
import mil.dod.th.ose.gui.webapp.controller.ControllerModel;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.google.protobuf.Message;

/**
 * Test class for the {@link BundleConfigurationMgrImpl} class.
 * 
 * @author cweisenborn
 */
public class TestBundleConfigurationMgrImpl
{
    private BundleConfigurationMgrImpl m_SUT;
    private MessageFactory m_MessageFactory;
    private BundleMgrImpl m_BundleMgr;
    private ActiveControllerImpl m_ActiveController;
    private ControllerModel m_Controller;
    private MessageWrapper m_MessageWrapper;
    
    @Before
    public void setup()
    {
        //Mocked classes.
        m_MessageFactory = mock(MessageFactory.class);
        m_ActiveController = mock(ActiveControllerImpl.class);
        m_BundleMgr = mock(BundleMgrImpl.class);
        m_Controller = mock(ControllerModel.class);
        m_MessageWrapper = mock(MessageWrapper.class);
        
        //Mock methods needed to retrieve the active controller and bundle manager service.
        when(m_ActiveController.getActiveController()).thenReturn(m_Controller);
        when(m_Controller.getId()).thenReturn(100);
        
        m_SUT = new BundleConfigurationMgrImpl();
        m_SUT.setActiveController(m_ActiveController);
        m_SUT.setBundleManager(m_BundleMgr);
        m_SUT.setMessageFactory(m_MessageFactory);
        
        when(m_MessageFactory.createBundleMessage(Mockito.any(BundleMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
    }
    
    /**
     * Test that the appropriate bundle is retrieved to display information on.
     * Verify that the correct bundle is returned. 
     */
    @Test
    public void testGetInfoBundle()
    {
        BundleModel infoBundle = mock(BundleModel.class);
        m_SUT.setInfoBundle(infoBundle);
        assertThat(m_SUT.getInfoBundle(), is(infoBundle));
    }
    
    /**
     * Test that the correct list of filtered bundles is returned.
     * Verify the bundle list is correct.
     */
    @Test
    public void testGetFilteredBundles()
    {
        List<BundleModel> filteredBundleList = new ArrayList<BundleModel>();
        BundleModel testBundle = mock(BundleModel.class);
        filteredBundleList.add(testBundle);
        m_SUT.setFilteredBundles(filteredBundleList);
        assertThat(m_SUT.getFilteredBundles(), is(filteredBundleList));
    }
    
    /**
     * Test that the get bundles method returns the appropriate list of bundles from the bundle manager service.
     * Verify the list of bundles returned is correct.
     */
    @Test
    public void testGetBundles()
    {   
        //Mock the bundle list returned by the bundle manager.
        List<BundleModel> bundles = new ArrayList<BundleModel>();
        BundleModel bundle = mock(BundleModel.class);
        bundles.add(bundle);
        when(m_BundleMgr.getBundlesAsync(100)).thenReturn(bundles);
        
        //Verify the list of bundles returned is correct.
        assertThat(m_SUT.getBundles(), is(bundles));
        //Verify that the bundle manager service was called with the appropriate controller ID.
        verify(m_BundleMgr).getBundlesAsync(100);
    }
    
    /**
     * Test the start bundle method.
     * Verify that a start bundle message with the appropriate contents is sent.
     */
    @Test
    public void testStartBundle()
    {        
        //Argument captor to capture the start message.
        ArgumentCaptor<BundleMessages.StartRequestData> messageCaptor = 
                ArgumentCaptor.forClass(BundleMessages.StartRequestData.class);
        //Replay
        m_SUT.startBundle(Long.valueOf(25));
        //Verify that the appropriate remote message sender method is called.
        verify(m_MessageFactory).createBundleMessage(eq(BundleMessageType.StartRequest), 
                messageCaptor.capture());
        verify(m_MessageWrapper).queue(eq(100), (ResponseHandler) eq(null));
        //Verify the bundle ID within the start message.
        assertThat(messageCaptor.getValue().getBundleId(), is(Long.valueOf(25)));
    }
    
    /**
     * Test the stop bundle method.
     * Verify that a stop bundle message with the appropriate contents is sent.
     */
    @Test
    public void testStopBundle()
    {
        //Argument captor to capture the stop message.
        ArgumentCaptor<BundleMessages.StopRequestData> messageCaptor = 
                ArgumentCaptor.forClass(BundleMessages.StopRequestData.class);
        //Replay
        m_SUT.stopBundle(Long.valueOf(25));
        //Verify that the appropriate remote message sender method is called.
        verify(m_MessageFactory).createBundleMessage(eq(BundleMessageType.StopRequest), messageCaptor.capture());
        verify(m_MessageWrapper).queue(eq(100), (ResponseHandler) eq(null));
        //Verify the bundle ID within the stop message.
        assertThat(messageCaptor.getValue().getBundleId(), is(Long.valueOf(25)));
    }
    
    /**
     * Test the uninstall bundle method.
     * Verify that an uninstall bundle message with the appropriate contents is sent.
     */
    @Test
    public void testUninstallBundle()
    {
        //Argument captor to capture the uninstall message.
        ArgumentCaptor<BundleMessages.UninstallRequestData> messageCaptor =
                ArgumentCaptor.forClass(BundleMessages.UninstallRequestData.class);
        //Replay
        m_SUT.uninstallBundle(Long.valueOf(25));
        //Verify that the appropriate remote message sender method is called.
        verify(m_MessageFactory).createBundleMessage(eq(BundleMessageType.UninstallRequest), messageCaptor.capture());
        verify(m_MessageWrapper).queue(eq(100), (ResponseHandler) eq(null));
        //Verify the bundle ID within the uninstall method.
        assertThat(messageCaptor.getValue().getBundleId(), is(Long.valueOf(25)));
    }
}
