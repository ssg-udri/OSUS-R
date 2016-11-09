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
package mil.dod.th.ose.gui.webapp.asset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mil.dod.th.ose.gui.webapp.advanced.configuration.ConfigurationWrapper;
import mil.dod.th.ose.gui.webapp.asset.AssetDisplayHelper.ActiveControllerEventHandler;
import mil.dod.th.ose.gui.webapp.controller.ActiveController;
import mil.dod.th.ose.gui.webapp.controller.ControllerModel;
import mil.dod.th.ose.gui.webapp.factory.FactoryObjMgr;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 * Test class for the {@link AssetDisplayHelper} class.
 * 
 * @author cweisenborn
 */
public class TestAssetDisplayHelperImpl
{
    private AssetDisplayHelper m_SUT;
    private AssetMgr m_AssetMgr;
    private AssetTypesMgr m_AssetTypesMgr;
    private ActiveController m_ActiveController;
    private ConfigurationWrapper m_ConfigWrapper;
    private BundleContextUtil m_BundleUtil;
    private ServiceRegistration<?> m_Registration;
    private ActiveControllerEventHandler m_ActiveControllerHandler;
    private AssetImage m_AssetImageInterface;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setup()
    { 
        //Mock needed classes.
        m_AssetMgr = mock(AssetMgr.class);
        m_AssetTypesMgr = mock(AssetTypesMgr.class);
        m_ActiveController = mock(ActiveController.class);
        m_ConfigWrapper = mock(ConfigurationWrapper.class);
        m_BundleUtil = mock(BundleContextUtil.class);
        m_Registration = mock(ServiceRegistration.class);
        BundleContext bundleContext = mock(BundleContext.class);
        
        m_AssetImageInterface = mock(AssetImage.class);
       
        m_SUT = new AssetDisplayHelper();
        
        //set injected services
        m_SUT.setBundleContextUtil(m_BundleUtil);
        m_SUT.setAssetMgr(m_AssetMgr);
        m_SUT.setActiveController(m_ActiveController);
        
        when(m_BundleUtil.getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
                Mockito.any(Dictionary.class))).thenReturn(m_Registration);
        
        //Setup all dependencies.
        m_SUT.setupDependencies();
        
        //Verify and capture the event handler being registered.
        ArgumentCaptor<EventHandler> eventHandlerCaptor = ArgumentCaptor.forClass(EventHandler.class);
        verify(bundleContext).registerService(eq(EventHandler.class), eventHandlerCaptor.capture(), 
                Mockito.any(Dictionary.class));
        
        //Set the event handler.
        m_ActiveControllerHandler = (ActiveControllerEventHandler)eventHandlerCaptor.getAllValues().get(0);
    }
    
    /**
     * Test the pre destory method that unregisters all event handlers.
     * Verify that unregister method is called for the service registration.
     */
    @Test
    public void testUnregisterEvents()
    {
        m_SUT.unregisterEvents();
        
        //Verify that unregister is called once for the active controller event handler.
        verify(m_Registration).unregister();
    }
    
    /**
     * Test the get factory object list async method.
     * Verify that the appropriate list of asset models is returned.
     */
    @Test
    public void testGetFactoryObjectListAsync()
    {
        ControllerModel controller = mock(ControllerModel.class);
        FactoryObjMgr mgr = mock(FactoryObjMgr.class);
        AssetModel asset = new AssetModel(5, UUID.randomUUID(), "bob", "bobType", 
                mgr, m_ConfigWrapper, m_AssetTypesMgr, m_AssetImageInterface);
        List<AssetModel> assetList = new ArrayList<AssetModel>();
        assetList.add(asset);
        
        //Mock call to asset manager and active controller.
        when(m_AssetMgr.getAssetsForControllerAsync(5)).thenReturn(assetList);
        when(m_ActiveController.getActiveController()).thenReturn(controller);
        when(controller.getId()).thenReturn(5);
        
        //Verify that the list correct list is returned.
        assertThat(m_SUT.getFactoryObjectListAsync(), is(assetList));
    }
    
    /**
     * Test the get feature title method.
     * Verify that asset is returned by the method.
     */
    @Test
    public void testGetFeatureTitle()
    {
        assertThat(m_SUT.getFeatureTitle(), is("Asset"));
    }
    
    /**
     * Test that the active controller event handler handles an active controller changed event appropriately.
     * Verify that the selected asset is returned to null when the active controller is changed.
     */
    @Test
    public void testActiveControllerEventHandler()
    {
        FactoryObjMgr mgr = mock(FactoryObjMgr.class);
        AssetModel asset = new AssetModel(5, UUID.randomUUID(), "bob", "bobType", 
                mgr, m_ConfigWrapper, m_AssetTypesMgr, m_AssetImageInterface);
        m_SUT.setSelectedFactoryObject(asset);
        
        //Verify that null isn't returned for the select asset.
        assertThat(m_SUT.getSelectedFactoryObject(), is(notNullValue()));
        
        //Create active controller changed event.
        Map<String, Object> props = new HashMap<String, Object>();
        Event actvieControllerChanged = new Event(ActiveController.TOPIC_ACTIVE_CONTROLLER_CHANGED, props);
        
        //Call active controller handle event method.
        m_ActiveControllerHandler.handleEvent(actvieControllerChanged);
        
        //Verify that null is returned for the select asset since the active controller has changed.
        assertThat(m_SUT.getSelectedFactoryObject(), is(nullValue()));
    }
}
