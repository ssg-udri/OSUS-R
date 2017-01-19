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
package edu.udayton.udri.asset.axis.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import junit.framework.TestCase;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.commands.SetPanTiltCommand;
import mil.dod.th.core.types.factory.SpatialTypesFactory;
import mil.dod.th.ose.integration.commons.AssetUtils;
import mil.dod.th.ose.integration.commons.FactoryUtils;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * Test functionality of the AXIS asset. Does not require the presence of the physical asset to run.
 * 
 * @author Dave Humeniuk
 *
 */
public class TestAxis extends TestCase
{
    private final String PRODUCT_TYPE = "edu.udayton.udri.asset.axis.AxisAsset";
    
    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    private AssetDirectoryService m_AssetDirectoryService;
    
    
    @Override
    public void setUp() throws Exception
    {
        m_AssetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        assertThat(m_AssetDirectoryService, is(notNullValue()));
        
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, PRODUCT_TYPE, 1000);
    }
    
    @Override
    public void tearDown()
    {
        AssetUtils.deleteAllAssets(m_Context);
    }
    
    /**
     * Test that the asset can be created.
     */
    public void testCreateAsset() throws Exception
    {
        Asset asset = m_AssetDirectoryService.createAsset(PRODUCT_TYPE);
        assertThat(asset, is(notNullValue()));
    }
    
    /**
     * Test that the asset can be activated and deactivated.
     */
    public void testActivateDeactivateAsset() throws Exception
    {
        Asset asset = m_AssetDirectoryService.createAsset(PRODUCT_TYPE);
        assertThat(asset, is(notNullValue()));
        
        AssetUtils.activateAsset(m_Context, asset, 3);

        AssetUtils.deactivateAsset(m_Context, asset);
    }
    
    /**
     * Test that the set pan tilt command can be sent. Will not always get response if no asset, but should at least
     * allow command to be sent. 
     */
    public void testSetPanTilt() throws Exception
    {
        Asset asset = m_AssetDirectoryService.createAsset(PRODUCT_TYPE);
        assertThat(asset, is(notNullValue()));
        
        AssetUtils.activateAsset(m_Context, asset, 3);
        
        asset.executeCommand(new SetPanTiltCommand().withPanTilt(SpatialTypesFactory.newOrientationOffset(0, 0)));
    }
}
