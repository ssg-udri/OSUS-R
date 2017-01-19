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

import java.util.Map;

import junit.framework.TestCase;
import example.asset.ExampleAsset;
import example.asset.ExampleAssetAttributes;
import example.asset.ExampleAssetExtension1;
import example.asset.ExampleAssetExtension2;
import example.asset.exception.ExampleSlowUpdateAsset;
import example.asset.exception.ExampleSlowUpdateAssetAttributes;
import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.integration.commons.AssetUtils;
import mil.dod.th.ose.integration.commons.FactoryUtils;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * Test functionality that applies to all factory objects, like configuration, naming and extensions.
 */
public class TestFactoryObject extends TestCase
{
    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    @Override
    public void setUp()
    {
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, ExampleSlowUpdateAsset.class, 5000);
        
        AssetUtils.deleteAllAssets(m_Context);
    }
       
    @Override
    public void tearDown() throws Exception
    {
        AssetUtils.deleteAllAssets(m_Context);
    }

    /**
     * Verify that even if an object's 
     * {@link mil.dod.th.core.factory.FactoryObjectProxy#updated(java.util.Map)} 
     * call is lengthy that the setProperty call does not return until the process has completed.
     */
    public void testSetPropertes_SlowUpdateAsset() throws Exception
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        assertThat(assetDirectoryService, is(notNullValue()));
        
        Asset asset = assetDirectoryService.createAsset(ExampleSlowUpdateAsset.class.getName());
        
        Map<String, Object> properties = asset.getProperties();
        properties.put(ExampleSlowUpdateAssetAttributes.KEY_EXAMPLE_PROP, "DIFFERENT VERY DIFFERENT!");
        asset.setProperties(properties);

        AssetUtils.activateAsset(m_Context, asset, 5);
        
        AssetUtils.deactivateAsset(m_Context, asset);
    }
    
    /**
     * Verify that at creation default property values are not included as a property if not set.
     * Verify that after updating a property, that defaults for properties which were not set do not get included
     * into the created configuration.
     */
    public void testCreation_NoDefaults() throws IllegalArgumentException, AssetException, 
        IllegalStateException, FactoryException
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        assertThat(assetDirectoryService, is(notNullValue()));
        
        Asset asset = assetDirectoryService.createAsset(ExampleAsset.class.getName());
        
        Map<String, Object> properties = asset.getProperties();
        //verify that initial creation does not add in props that are not required
        assertThat(properties.keySet(), not(hasItem(ExampleAssetAttributes.CONFIG_PROP_DEVICE_POWER_NAME)));

        //set the activate property
        properties.put(ExampleAssetAttributes.CONFIG_PROP_ACTIVATE_ON_STARTUP, true);
        asset.setProperties(properties);

        //Verify that setting a property did not introduce the default, of a property that was not set.
        //This is done to ensure that the act of setting a property whether to its default or not,
        //does not introduce other property's values which were not previously set to values
        //different than their defaults into the map of known properties.
        assertThat(asset.getProperties().keySet(), not(hasItem(ExampleAssetAttributes.CONFIG_PROP_DEVICE_POWER_NAME)));
    }
    
    /**
     * Verify no exceptions if deleting object with no properties set.
     */
    public void testDelete_NoPropertiesSet() throws Exception
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        
        Asset asset = assetDirectoryService.createAsset(ExampleAsset.class.getName());
        asset.delete();
    }
    
    /**
     * Verify an extension can be retrieved and called upon.
     */
    public void testExtensions() throws Exception
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        
        Asset asset = assetDirectoryService.createAsset(ExampleAsset.class.getName());

        ExampleAssetExtension1 ext1 = asset.getExtension(ExampleAssetExtension1.class);
        assertThat(ext1.addSuffix("blah"), is(asset.getName() + "blah"));
        
        ExampleAssetExtension2 ext2 = asset.getExtension(ExampleAssetExtension2.class);
        ext2.performFunction();       
    }
}