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
package mil.dod.th.ose.controller.integration.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static mil.dod.th.ose.test.matchers.Matchers.*;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Set;
import java.util.UUID;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.physical.PhysicalLinkAttributes;
import mil.dod.th.ose.junit4xmltestrunner.IntegrationTestRunner;

import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import example.asset.ExampleAsset;
import example.asset.ExampleAssetAttributes;
import example.metatype.XML.ExampleClass;

/**
 * Tests loading of configuration information from an .xml file for all factory object types on a 2nd run of the 
 * controller. This will check the creation policy properties in the configs.xml. This .xml is loaded 
 * at controller startup and should create and configure objects based on its structure.
 * 
 * @author jlatham
 */
public class TestXmlConfigurations2ndRun
{    
    /*
     * Tests that Assets can be correctly configured from the configs.xml.
     */
    @Test
    public final void testAssetConfiguration() throws IllegalArgumentException, IllegalStateException
    {
        String assetName1 = "xmlConfigAsset1";
        String assetName2 = "xmlConfigAsset2";
        
        AssetDirectoryService assetDirectoryService = IntegrationTestRunner.getService(AssetDirectoryService.class);
        assertThat(assetDirectoryService, is(notNullValue()));
        
        Set<Asset> assets = assetDirectoryService.getAssets();
        assertThat(assets.size(), greaterThan(0));
        
        Set<Asset> exampleAssets = assetDirectoryService.getAssetsByType(ExampleAsset.class.getName());
        assertThat(exampleAssets.size(), greaterThanOrEqualTo(1));
        
        Asset testAsset1 = assetDirectoryService.getAssetByName(assetName1);    
        assertThat(testAsset1.getConfig().activateOnStartup(), is(true));
        assertThat(testAsset1.getProperties(), hasEntry(ExampleAssetAttributes.CONFIG_PROP_DEVICE_POWER_NAME,
                (Object)"config-value"));
        assertThat(testAsset1.getActiveStatus(), is(Asset.AssetActiveStatus.ACTIVATED));        
       
        try
        {
            assetDirectoryService.getAssetByName(assetName2); 
            fail("Should throw IllegalArgumentException as asset should not have been created.");
        }
        catch (IllegalArgumentException e)
        {
            // Do nothing as this exception should be thrown.
        }
    }
    
    /*
     * Tests that Addresses can be correctly configure via the configs.xml.
     */
    @Test
    public final void testAddressConfiguration()
    {
        AddressManagerService addressManagerService = IntegrationTestRunner.getService(AddressManagerService.class);
        assertThat(addressManagerService, is(notNullValue()));
        
        assertThat(addressManagerService.checkAddressAlreadyExists("Example:1"), is(true));
        // Example:2 might be recreated automatically when transport channel is created, so don't check here
        assertThat(addressManagerService.checkAddressAlreadyExists("Example:3"), is(false));
    }
    
    /*
     * Tests that a Link Layer can be configured correctly from the configs.xml.
     */
    @Test
    public final void testLinkLayerConfiguration()
    {
        String testLinkName = "xmlConfigLinkLayer";
        
        CustomCommsService customCommsService = IntegrationTestRunner.getService(CustomCommsService.class);
        assertThat(customCommsService, is(notNullValue()));
        
        LinkLayer testLinkLayer = customCommsService.getLinkLayer(testLinkName);
        assertThat(testLinkLayer, is(notNullValue()));
        
        assertThat(testLinkLayer.isActivated(), is(true));

        assertThat(testLinkLayer.getConfig().activateOnStartup(), is(true));
        assertThat(testLinkLayer.getConfig().retries(), is(3));
        assertThat(testLinkLayer.getConfig().readTimeoutMs(), is(1000));
        assertThat(testLinkLayer.getConfig().physicalLinkName(), is("xmlConfigPhysicalLink"));
    }
    
    /*
     * Tests that a Transport Layer is not re-created by the configs.xml on second run.
     */
    @Test
    public final void testTransportLayerConfiguration()
    {
        String testTransportLayerName = "xmlConfigTransportLayer";
        
        CustomCommsService customCommsService = IntegrationTestRunner.getService(CustomCommsService.class);
        assertThat(customCommsService, is(notNullValue()));

        try
        {
            customCommsService.getTransportLayer(testTransportLayerName);
            fail("Should throw IllegalArguementException as transport layer should not have been created.");
        }
        catch (IllegalArgumentException e)
        {
            // Do nothing exception should be thrown.
        }
    }
    
    /*
     * Tests that a Physical Layer can be configured correctly from the configs.xml.
     */
    @Test
    public final void testPhysicalLayerConfiguration() throws IOException
    {
        String testPhysicalLinkName = "xmlConfigPhysicalLink";
        
        CustomCommsService customCommsService = IntegrationTestRunner.getService(CustomCommsService.class);
        assertThat(customCommsService, is(notNullValue()));
        
        String pidToTest = null;
        for (UUID id : customCommsService.getPhysicalLinkUuids())
        {
            String thisName = customCommsService.getPhysicalLinkName(id);
            if (thisName.equals(testPhysicalLinkName))
            {
                pidToTest = customCommsService.getPhysicalLinkPid(id);                        
            }
        }        
        assertThat(pidToTest, is(notNullValue()));
       
        ConfigurationAdmin configAdmin = IntegrationTestRunner.getService(ConfigurationAdmin.class);
        assertThat(configAdmin, is(notNullValue()));
        
        Configuration exampleConfig = configAdmin.getConfiguration(pidToTest);
        
        Dictionary<String,Object> properties = exampleConfig.getProperties();        
        assertThat(properties, is(notNullValue()));
        assertThat(properties.isEmpty(), is(false));        
        assertThat(properties, dictionaryHasEntry(PhysicalLinkAttributes.CONFIG_PROP_READ_TIMEOUT_MS, (Object)500));
        assertThat(properties, dictionaryHasEntry(PhysicalLinkAttributes.CONFIG_PROP_DATA_BITS, (Object)7));      
    }
    
    /*
     * Tests that an OSGi non-factory configuration does not exists on a 2nd run when removed.
     */
    @Test
    public final void testOSGiNonFactoryConfiguration() throws IOException, InvalidSyntaxException
    {
        ConfigurationAdmin configAdmin = IntegrationTestRunner.getService(ConfigurationAdmin.class);
        assertThat(configAdmin, is(notNullValue()));
        
        Configuration[] currentConfigs = configAdmin.listConfigurations(String.format("(service.pid=%s)", 
                ExampleClass.CONFIG_PID));
        assertThat(currentConfigs, is(nullValue()));
    }
    
    /*
     * Tests that an OSGi factory configuration can be properly set from the configs.xml.
     */
    @Test
    public final void testOSGiFactoryConfiguration() throws IOException, InvalidSyntaxException
    {
        ConfigurationAdmin configAdmin = IntegrationTestRunner.getService(ConfigurationAdmin.class);
        assertThat(configAdmin, is(notNullValue()));
        
        Configuration[] configurations = configAdmin.listConfigurations(
                "(service.factoryPid=mil.dod.th.ose.logging.LogFilterImpl*)");
        assertThat(configurations, is(notNullValue()));
        assertThat(configurations.length, is(1));

        assertThat(configurations[0].getProperties(), dictionaryHasEntry("severity", (Object)"Debug"));
        assertThat(configurations[0].getProperties(), dictionaryHasEntry("bundleSymbolicFilter", 
                (Object)"org.apache.felix..*"));
    }     
}
