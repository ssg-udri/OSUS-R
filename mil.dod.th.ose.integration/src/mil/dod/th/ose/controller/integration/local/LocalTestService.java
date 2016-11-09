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
package mil.dod.th.ose.controller.integration.local;

import java.util.ArrayList;
import java.util.List;

import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.controller.TerraHarvestController;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.mp.EventHandlerHelper;
import mil.dod.th.core.mp.ManagedExecutors;
import mil.dod.th.core.mp.MissionProgramManager;
import mil.dod.th.core.mp.TemplateProgramManager;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.system.TerraHarvestSystem;
import mil.dod.th.core.validator.Validator;
import mil.dod.th.core.xml.XmlMarshalService;
import mil.dod.th.core.xml.XmlUnmarshalService;
import mil.dod.th.ose.controller.integration.TestPrepFor2ndRun;
import mil.dod.th.ose.controller.integration.config.TestConfigurationAdminEventBridge;
import mil.dod.th.ose.controller.integration.config.TestRemoteChannelConfigs;
import mil.dod.th.ose.controller.integration.config.TestRemoteChannelConfigs2ndRun;
import mil.dod.th.ose.controller.integration.config.TestXmlConfigurations;
import mil.dod.th.ose.controller.integration.config.TestXmlConfigurations2ndRun;
import mil.dod.th.ose.controller.integration.controller.TestGenericTerraHarvestController;
import mil.dod.th.ose.controller.integration.controller.TestTerraHarvestController;
import mil.dod.th.ose.controller.integration.core.TestMissionProgramManager;
import mil.dod.th.ose.controller.integration.core.TestPersistedAddresses;
import mil.dod.th.ose.controller.integration.core.TestPersistedAsset;
import mil.dod.th.ose.controller.integration.core.TestPersistedCommLayers;
import mil.dod.th.ose.controller.integration.core.TestPersistedMissionPrograms;
import mil.dod.th.ose.controller.integration.core.TestPersistedObservations;
import mil.dod.th.ose.controller.integration.logging.TestLogging;
import mil.dod.th.ose.controller.integration.remote.TestPersistedRemoteChannelLookup;
import mil.dod.th.ose.controller.integration.remote.TestRemoteChannelLookup;
import mil.dod.th.ose.integration.TestBundlesActivated;
import mil.dod.th.ose.junit4xmltestrunner.IntegrationTestService;
import mil.dod.th.ose.junit4xmltestrunner.LogOutputStream;
import mil.dod.th.ose.remote.api.RemoteEventAdmin;
import mil.dod.th.ose.shared.JdoDataStore;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Component;
import example.asset.ExampleAsset;
import example.ccomms.EchoTransport;
import example.ccomms.ExampleAddress;
import example.ccomms.ExampleLinkLayer;

/**
 * @author dhumeniuk
 *
 */
@Component(provide = IntegrationTestService.class)
public class LocalTestService extends IntegrationTestService
{
    @Override
    public void addServiceReferences() throws InvalidSyntaxException
    {
        // core OSGi service references
        addReference(LogReaderService.class);
        addReference(LogService.class);
        addReference(ConfigurationAdmin.class);
        addReference(EventAdmin.class);
        addReference(CommandProcessor.class);
        
        addReference(LogOutputStream.class);
        
        // core API references
        addReference(CustomCommsService.class);
        addReference(AssetDirectoryService.class);
        addReference(AddressManagerService.class);
        addReference(Validator.class);
        addReference(ObservationStore.class);
        addReference(PersistentDataStore.class);
        addReference(TerraHarvestSystem.class);
        addReference(TerraHarvestController.class);
        addReference(MissionProgramManager.class);
        addReference(EventHandlerHelper.class);
        addReference(ManagedExecutors.class);
        addReference(TemplateProgramManager.class);
        addReference(XmlMarshalService.class);
        addReference(XmlUnmarshalService.class);
        
        //reference to the observation jdo data store.
        addReference("obsDataStore", JdoDataStore.class, "(" + JdoDataStore.PROP_KEY_DATASTORE_TYPE + "="
            + JdoDataStore.PROP_OBSERVATION_STORE + ")");
        
        // add optional references, not all systems will have a power manager
        addOptionalReference(PowerManager.class);
        
        // core remote interface references (core API, but implemented in separate bundle) 
        addReference(RemoteChannelLookup.class);
        addReference(RemoteEventAdmin.class);
        
        // references to specific factories
        addReference("exampleAssetFactory",FactoryDescriptor.class, 
                "(" + FactoryDescriptor.PRODUCT_TYPE_SERVICE_PROPERTY + "=" + ExampleAsset.class.getName() + ")");
        addReference("exampleAddressFactory",FactoryDescriptor.class, 
                "(" + FactoryDescriptor.PRODUCT_TYPE_SERVICE_PROPERTY + "=" + ExampleAddress.class.getName() + ")");
        addReference("exampleLinkFactory",FactoryDescriptor.class, 
                "(" + FactoryDescriptor.PRODUCT_TYPE_SERVICE_PROPERTY + "=" + ExampleLinkLayer.class.getName() + ")");
        addReference("echoTransportFactory",FactoryDescriptor.class, 
                "(" + FactoryDescriptor.PRODUCT_TYPE_SERVICE_PROPERTY + "=" + EchoTransport.class.getName() + ")");
    }

    @Override
    public List<Class<?>> getSharedTestClasses()
    {
        List<Class<?>> testClasses = new ArrayList<Class<?>>();
                
        testClasses.add(TestBundlesActivated.class);
        testClasses.add(TestLogging.class);
               
        // config component. Needs to run first to check state of controller at startup.
        testClasses.add(TestXmlConfigurations.class);
        // other config component
        testClasses.add(TestConfigurationAdminEventBridge.class);
        
        // core component
        testClasses.add(TestMissionProgramManager.class);
        
        // controller component
        testClasses.add(TestTerraHarvestController.class);
                
        // remote interface component
        testClasses.add(TestRemoteChannelLookup.class);
        
        // must be the last class as it creates things needed for the 2nd run
        testClasses.add(TestPrepFor2ndRun.class);
        
        return testClasses;
    }
    
    @Override
    public List<Class<?>> getTargetTestClasses()
    {
        List<Class<?>> targetTestClasses = new ArrayList<Class<?>>();
        
        //  config component. Verifies that remote channels are create appropriately. Should not be ran by other
        //  integration tests.
        targetTestClasses.add(TestRemoteChannelConfigs.class);
        
        return targetTestClasses;
    }

    @Override
    public String getTargetName()
    {
        return "generic-controller";
    }

    @Override
    public List<Class<?>> getOptionalTestClasses()
    {
        List<Class<?>> optionalTestClasses = new ArrayList<Class<?>>();
        
        // 2nd run of configuration component. Needs to run first to check state of controller at startup.
        optionalTestClasses.add(TestXmlConfigurations2ndRun.class);
        optionalTestClasses.add(TestRemoteChannelConfigs2ndRun.class);
        
        // persisted core tests
        optionalTestClasses.add(TestPersistedMissionPrograms.class);
        optionalTestClasses.add(TestPersistedAddresses.class);
        optionalTestClasses.add(TestPersistedCommLayers.class);
        optionalTestClasses.add(TestPersistedObservations.class);
        optionalTestClasses.add(TestPersistedAsset.class);
        optionalTestClasses.add(TestGenericTerraHarvestController.class);
        
        // remote interface
        optionalTestClasses.add(TestPersistedRemoteChannelLookup.class);       
               
        return optionalTestClasses;
    }
}
