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
package mil.dod.th.ose.sdk.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.io.File;
import java.util.Map.Entry;

import junit.framework.TestCase;
import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.ose.integration.commons.AssetUtils;
import mil.dod.th.ose.integration.commons.CustomCommsUtils;
import mil.dod.th.ose.integration.commons.FactoryUtils;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * Test that all SDK plug-ins have implemented the getExtensions method.
 * 
 * @author cweisenborn
 */
public class TestPluginExtensions extends TestCase
{
    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    private AssetDirectoryService m_AssetDirectoryService;
    private CustomCommsService m_CustomCommsService;

    @Override
    public void setUp() throws Exception
    {
        m_AssetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        assertThat(m_AssetDirectoryService, is(notNullValue()));
        
        m_CustomCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);
        assertThat(m_CustomCommsService, is(notNullValue()));

        for (Entry<String, String> entry : SdkUtils.PLUGIN_MAP.entrySet())
        {
            File pluginDir = SdkUtils.getPluginDir(m_Context, entry.getKey());
            FactoryUtils.installPlugin(m_Context, new File(pluginDir, "generated/" + entry.getValue() + ".jar"), 
                    entry.getValue() + "." + entry.getKey());
        }
    }
    
    @Override
    public void tearDown()
    {
        AssetUtils.deleteAllAssets(m_Context);
        CustomCommsUtils.deleteAllLayers(m_Context);
    }
    
    /**
     * Test that an SDK generated asset returns an empty list of extensions.
     */
    public void testGetSdkAssetExtensions() throws IllegalArgumentException, FactoryException
    {
        Asset asset = m_AssetDirectoryService.createAsset(SdkUtils.getProductType(SdkUtils.SDK_ASSET_NAME));
        assertThat(asset, is(notNullValue()));
        
        assertThat(asset.getExtensionTypes().isEmpty(), is(true));
    }
    
    /**
     * Test that an SDK generated physical link layer returns an empty list of extensions.
     */
    public void testGetSdkPhysicalLayerExtensions() throws IllegalArgumentException, FactoryException
    {
        PhysicalLink physLink = m_CustomCommsService.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT);
        assertThat(physLink, is(notNullValue()));
        
        assertThat(physLink.getExtensionTypes().isEmpty(), is(true));
    }
    
    /**
     * Test that an SDK generated link layer returns an empty list of extensions.
     */
    public void testGetSdkLinkLayerExtensions() throws IllegalArgumentException, FactoryException
    {
        PhysicalLink physLink = m_CustomCommsService.createPhysicalLink(PhysicalLinkTypeEnum.SPI);
        assertThat(physLink, is(notNullValue()));

        LinkLayer linkLayer = m_CustomCommsService.createLinkLayer(SdkUtils.getProductType(
                SdkUtils.SDK_LINK_LAYER_NAME), physLink.getName());
        assertThat(linkLayer, is(notNullValue()));
        
        assertThat(linkLayer.getExtensionTypes().isEmpty(), is(true));
    }
    
    /**
     * Test that an SDK generated transport layer returns an empty list of extensions.
     */
    public void testGetSdkTransportLayerExtensions() throws FactoryException
    {
        TransportLayer transportLayer = m_CustomCommsService.createTransportLayer(
                SdkUtils.getProductType(SdkUtils.SDK_TRANS_LAYER_NAME),
                SdkUtils.SDK_TRANS_LAYER_NAME, (String)null);
        assertThat(transportLayer, is(notNullValue()));
        
        assertThat(transportLayer.getExtensionTypes().isEmpty(), is(true));
    }
}
