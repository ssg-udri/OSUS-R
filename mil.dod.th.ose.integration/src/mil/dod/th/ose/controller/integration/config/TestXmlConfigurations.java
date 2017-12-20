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
import static mil.dod.th.ose.test.matchers.Matchers.*;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.physical.PhysicalLinkAttributes;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.ose.controller.integration.utils.BundleTestUtils;
import mil.dod.th.ose.junit4xmltestrunner.IntegrationTestRunner;
import mil.dod.th.ose.remote.api.RemoteEventAdmin;
import mil.dod.th.ose.remote.api.RemoteEventRegistration;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import example.asset.ExampleAsset;
import example.asset.ExampleAssetAttributes;
import example.metatype.XML.ExampleClass;
import example.metatype.XML.ExampleClassConfig;
import example.zzz.config.ZzzExampleClass;
import example.zzz.config.ZzzExampleClassConfig;

/**
 * Tests loading of configuration information from an .xml file for all factory object types. Also tests 
 * .xml configuration of OSGi factory and non-factory configurations. This .xml is loaded at controller startup and
 * should create and configure objects based on its structure.
 * 
 * @author jlatham
 */
public class TestXmlConfigurations
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
        assertThat(exampleAssets.size(), greaterThanOrEqualTo(2));
        
        Asset testAsset1 = assetDirectoryService.getAssetByName(assetName1);    
        assertThat(testAsset1.getConfig().activateOnStartup(), is(true));
        assertThat(testAsset1.getProperties(), hasEntry(ExampleAssetAttributes.CONFIG_PROP_DEVICE_POWER_NAME, 
                (Object)"config-value"));
        assertThat(testAsset1.getActiveStatus(), is(Asset.AssetActiveStatus.ACTIVATED));
        
        Asset testAsset2 = assetDirectoryService.getAssetByName(assetName2);
        assertThat(testAsset2.getProperties(), hasEntry(ExampleAssetAttributes.CONFIG_PROP_DEVICE_POWER_NAME, 
                (Object)"config-value"));    
        assertThat(testAsset2.getActiveStatus(), is(Asset.AssetActiveStatus.DEACTIVATED));
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
        assertThat(addressManagerService.checkAddressAlreadyExists("Example:2"), is(true));          
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
     * Tests that a Transport Layer can be properly configure from the configs.xml.
     */
    @Test
    public final void testTransportLayerConfiguration()
    {
        String testTransportLayerName = "xmlConfigTransportLayer";
        
        CustomCommsService customCommsService = IntegrationTestRunner.getService(CustomCommsService.class);
        assertThat(customCommsService, is(notNullValue()));

        TransportLayer testTransport = customCommsService.getTransportLayer(testTransportLayerName);
        assertThat(testTransport.getConfig().readTimeoutMs(), is(1500));
        assertThat(testTransport.getConfig().linkLayerName(), is("xmlConfigLinkLayer"));
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
     * Tests that an OSGi non-factory configuration can be set from the configs.xml.
     */
    @Test
    public final void testOSGiNonFactoryConfiguration() throws IOException
    {
        int exampleConfigTestValue = 50;
        String exampleConfigTestValueString = "config-value";
        
        ConfigurationAdmin configAdmin = IntegrationTestRunner.getService(ConfigurationAdmin.class);
        assertThat(configAdmin, is(notNullValue()));
        
        Configuration exampleConfig = configAdmin.getConfiguration(ExampleClass.CONFIG_PID); 
        
        // check that the configuration is not bound to the mil.dod.th.ose.config.loading.jar
        Bundle configLoaderBundle = BundleTestUtils.getBundleBySymName("mil.dod.th.ose.config.loading");
        assertThat(configLoaderBundle, is(notNullValue()));
        String configLoaderBundleLocation = configLoaderBundle.getLocation();        
        assertThat(exampleConfig.getBundleLocation(), is(not(equalTo(configLoaderBundleLocation))));
                
        Dictionary<String,Object> properties = exampleConfig.getProperties();        
        assertThat(properties, is(notNullValue()));
        assertThat(properties.isEmpty(), is(false));        
        
        assertThat(properties, dictionaryHasEntry(ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_VALUE, 
                (Object)exampleConfigTestValue));  
        assertThat(properties, dictionaryHasEntry(ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_VALUE_STRING, 
                (Object)exampleConfigTestValueString));  
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
        
        // check that the configuration is not bound to the mil.dod.th.ose.config.loading.jar
        Bundle configLoaderBundle = BundleTestUtils.getBundleBySymName("mil.dod.th.ose.config.loading");
        assertThat(configLoaderBundle, is(notNullValue()));
        String configLoaderBundleLocation = configLoaderBundle.getLocation();        
        assertThat(configurations[0].getBundleLocation(), is(not(equalTo(configLoaderBundleLocation))));
        
        assertThat(configurations.length, is(1));

        assertThat(configurations[0].getProperties(), dictionaryHasEntry("severity", (Object)"Debug"));
        assertThat(configurations[0].getProperties(), dictionaryHasEntry("bundleSymbolicFilter", 
                (Object)"org.apache.felix..*"));
    }     

    /*
     * Tests that a bundle loaded after config loader activation still is configured.
     */
    @Test 
    public final void testOSGiConfiguration_BundleLoadedAfterActivation() throws IOException
    {
        ConfigurationAdmin configAdmin = IntegrationTestRunner.getService(ConfigurationAdmin.class);
        assertThat(configAdmin, is(notNullValue()));
        
        Configuration exampleConfig = configAdmin.getConfiguration(ZzzExampleClass.CONFIG_PID); 
        
        Dictionary<String,Object> properties = exampleConfig.getProperties();        
        assertThat(properties, is(notNullValue()));
        assertThat(properties.isEmpty(), is(false));        
        
        assertThat(properties, dictionaryHasEntry(ZzzExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_VALUE, (Object)200));
    }
    
    /**
     * Verify the config loader bundle registered for remote event denoted in configs.xml.
     */
    @Test
    public void testRemoteEventAdminConfigLoading()
    {
        RemoteEventAdmin remoteEventAdmin = IntegrationTestRunner.getService(RemoteEventAdmin.class);
        
        Map<Integer, RemoteEventRegistration> regs = remoteEventAdmin.getRemoteEventRegistrations();
        assertThat(regs.size(), is(1));
        
        /*Entry from config file
         *<eventConfigs canQueueEvent="false" eventFilter="(obj.name=remoteConfigTestAsset)" systemId="99">
         *   <eventTopics>mil/dod/th/core/asset/Asset/DATA_CAPTURED</eventTopics>
         *</eventConfigs>
         */
        RemoteEventRegistration regData = regs.get(1);
        assertThat(regData.getSystemId(), is(99));
        
        assertThat(regData.getEventRegistrationRequestData().getCanQueueEvent(), is(false));
        assertThat(regData.getEventRegistrationRequestData().getFilter(), is("(obj.name=remoteConfigTestAsset)"));
        assertThat(regData.getEventRegistrationRequestData().getTopicCount(), is(1));
        assertThat(regData.getEventRegistrationRequestData().getTopicList().get(0), 
                is("mil/dod/th/core/asset/Asset/DATA_CAPTURED"));
        assertThat(regData.getEventRegistrationRequestData().getObjectFormat(), 
                is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
        assertThat(regData.getEventRegistrationRequestData().getExpirationTimeHours(), is(5));
    }
}
