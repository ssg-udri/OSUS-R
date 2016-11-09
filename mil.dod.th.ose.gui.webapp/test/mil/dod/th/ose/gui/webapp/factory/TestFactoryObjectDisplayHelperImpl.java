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
package mil.dod.th.ose.gui.webapp.factory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import mil.dod.th.ose.gui.webapp.asset.AssetDisplayHelper;
import mil.dod.th.ose.gui.webapp.asset.AssetMgr;
import mil.dod.th.ose.gui.webapp.asset.AssetModel;
import mil.dod.th.ose.gui.webapp.controller.ActiveController;
import mil.dod.th.ose.gui.webapp.controller.ControllerModel;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;

/**
 * @author callen
 *
 */
public class TestFactoryObjectDisplayHelperImpl 
{
    private AssetDisplayHelper m_SUT;
    private AssetMgr m_AssetMgr;
    private ActiveController m_ActiveController;
    private BundleContextUtil m_BundleContextUtil;

    //UUID to use
    private UUID uuid = UUID.randomUUID();
    //controller id to use
    private int systemId = 321;

    @Before
    public void setUp()
    {
        m_SUT = new AssetDisplayHelper();
        
        //Mocked class
        m_AssetMgr = mock(AssetMgr.class);
        m_ActiveController = mock(ActiveController.class);
        m_BundleContextUtil = mock(BundleContextUtil.class);
        m_SUT.setBundleContextUtil(m_BundleContextUtil);
        
        //Mock methods needed to retrieve the asset manager.
        m_SUT.setActiveController(m_ActiveController);
        m_SUT.setAssetMgr(m_AssetMgr);
        m_SUT.setBundleContextUtil(m_BundleContextUtil);
        
        BundleContext context = mock(BundleContext.class);
        when(m_BundleContextUtil.getBundleContext()).thenReturn(context);
        m_SUT.setupDependencies(); 
    }

    /**
     * Test setting a selected factory object.
     * Verify that initially a selected object is not set.
     * Verify once set that the correct object model is set.
     * Verify that isSetSelectedObject returns proper flag value depending on whether the actual 
     * selected object is set or not.
     */
    @Test
    public void testSetSelectedObject()
    {
        //check that a selected object is not set
        assertThat(m_SUT.isSetSelectedObject(), is(false));

        //create mock asset model
        AssetModel model = mock(AssetModel.class);
        when(model.getUuid()).thenReturn(uuid);
        when(model.getControllerId()).thenReturn(systemId);

        //set the selected object
        m_SUT.setSelectedFactoryObject(model);
        
        //verify
        assertThat(m_SUT.getSelectedFactoryObject().getUuid(), is(uuid));
        assertThat(m_SUT.isSetSelectedObject(), is(true));
    }
    
    /**
     * Test getting the factory objects to display for the asset service.
     */
    @Test
    public void testGetFactoryObjectList()
    {
        ControllerModel contModel = mock(ControllerModel.class);
        when(contModel.getId()).thenReturn(systemId);

        //create list of asset models
        List<AssetModel> assetModels = new ArrayList<AssetModel>();
        AssetModel model = mock(AssetModel.class);
        when(model.getUuid()).thenReturn(uuid);
        when(model.getControllerId()).thenReturn(systemId);
        AssetModel model2 = mock(AssetModel.class);
        when(model2.getUuid()).thenReturn(UUID.randomUUID());
        when(model2.getControllerId()).thenReturn(systemId);
        
        //add the models
        assetModels.add(model);
        assetModels.add(model2);

        //mock behavior for the asset manager
        when(m_AssetMgr.getAssetsForControllerAsync(systemId)).thenReturn(assetModels);
        when(m_ActiveController.getActiveController()).thenReturn(contModel);

        //verify, should be the two models created above
        assertThat((m_SUT.getFactoryObjectListAsync()).size(), is(2));
    }
}
