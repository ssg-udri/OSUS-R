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
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mil.dod.th.core.asset.capability.AssetCapabilities;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace.
            AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetRequestData;
import mil.dod.th.ose.gui.webapp.advanced.configuration.AttributeModel;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ConfigPropModelImpl;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ConfigurationWrapperImpl;
import mil.dod.th.ose.gui.webapp.advanced.configuration.UnmodifiableConfigMetatypeModel;
import mil.dod.th.ose.gui.webapp.advanced.configuration.UnmodifiablePropertyModel;
import mil.dod.th.ose.gui.webapp.asset.AssetMgrImpl.RemoteCreateAssetHandler;
import mil.dod.th.ose.gui.webapp.controller.ActiveControllerImpl;
import mil.dod.th.ose.gui.webapp.controller.ControllerModel;
import mil.dod.th.ose.shared.SharedMessageUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.service.metatype.AttributeDefinition;

import com.google.protobuf.Message;

/**
 * Test the implementation capabilities of the {@link AddAssetDialogHelperImpl}.
 * @author callen
 *
 */
public class TestAddAssetDialogHelperImpl 
{
    private AddAssetDialogHelperImpl m_SUT;
    private MessageFactory m_MessageFactory;
    private AssetMgrImpl m_AssetMgr;
    private MessageWrapper m_MessageWrapper;
    private ConfigurationWrapperImpl m_ConfigWrapper;
    private ActiveControllerImpl m_ActiveController;
    private ControllerModel m_ControllerModel;

    /**
     * Setup the injected services and create the 
     * AddAssetDialogHelper instance that will be the 
     * class under test. 
     */
    @Before
    public void setUp()
    {
        m_SUT = new AddAssetDialogHelperImpl();
        
        //Mocked classes
        m_AssetMgr = mock(AssetMgrImpl.class);
        m_MessageFactory = mock(MessageFactory.class);
        m_MessageWrapper = mock(MessageWrapper.class);
        m_ActiveController = mock(ActiveControllerImpl.class);
        m_ControllerModel = mock(ControllerModel.class);
        m_ConfigWrapper = mock(ConfigurationWrapperImpl.class);
        
        //set injected services
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setAssetMgr(m_AssetMgr);
        m_SUT.setActiveController(m_ActiveController);
        m_SUT.setConfigWrapper(m_ConfigWrapper);
        
        when(m_MessageFactory.createAssetDirectoryServiceMessage(
            Mockito.any(AssetDirectoryServiceMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        when(m_ActiveController.getActiveController()).thenReturn(m_ControllerModel);
        when(m_ControllerModel.getId()).thenReturn(125);
    }
    
    /**
     * Test setting an asset factory.
     * 
     * Verify that the get asset method returns the correct model.
     * 
     * Verify that the isSetAssetFactory method correctly return true/false if a factory is or is not set.
     */
    @Test
    public void testSetAssetFactory()
    {
        //mock asset factory
        AssetFactoryModel model = mock(AssetFactoryModel.class);

        //check that is set factory returns false as there is no factory set
        assertThat(m_SUT.isSetAssetFactory(), is(false));

        //set a factory model
        m_SUT.setAssetFactory(model);

        //verify
        assertThat(m_SUT.isSetAssetFactory(), is(true));
        assertThat(m_SUT.getAssetFactory(), is(model));
    }
    
    /**
     * Test setting the ability for the helper to hold an assets name.
     * 
     * Verify correct name is returned when requested.
     * 
     * 
     */
    @Test
    public void testSetNewAssetName()
    {
        //name for asset
        String name = "James";

        //check that no name is set
        assertThat(m_SUT.getNewAssetName(), is(nullValue()));

        //set a name
        m_SUT.setNewAssetName(name);

        //verify
        assertThat(m_SUT.getNewAssetName(), is(name));
    }

    /**
     * Test the initialization of fields.
     * 
     * Verify that all fields are reset.
     */
    @Test
    public void testInit()
    {
        //name for asset
        String name = "James";

        //check that no name is set
        assertThat(m_SUT.getNewAssetName(), is(nullValue()));

        //set a name
        m_SUT.setNewAssetName(name);

        //verify
        assertThat(m_SUT.getNewAssetName(), is(name));
        
        //mock asset factory
        AssetFactoryModel model = mock(AssetFactoryModel.class);

        //check that is set factory returns false as there is no factory set
        assertThat(m_SUT.isSetAssetFactory(), is(false));

        //set a factory model
        m_SUT.setAssetFactory(model);

        //verify
        assertThat(m_SUT.isSetAssetFactory(), is(true));

        //call init
        m_SUT.init();
        
        //verify
        assertThat(m_SUT.isSetAssetFactory(), is(false));
        assertThat(m_SUT.getNewAssetName().isEmpty(), is(true));
    }
    
    /**
     * Test the request to create a new asset.
     * Verify message sent.
     * Verify response handler requested from asset manager.
     */
    @Test
    public void testRequestCreateAsset()
    {
        //controller id to use
        int systemId = 321;

        //create variables
        String productType = "mil.dod.you.me.Everybody";
        String name = "EverybodyAsset";

        m_SUT.requestCreateAsset(productType, systemId, name);

        //verify message sent to remove the assetS
        ArgumentCaptor<CreateAssetRequestData> request = 
            ArgumentCaptor.forClass(CreateAssetRequestData.class);

        verify(m_MessageFactory).createAssetDirectoryServiceMessage(
            eq(AssetDirectoryServiceMessageType.CreateAssetRequest), 
                request.capture());
        verify(m_MessageWrapper).queue(eq(systemId), Mockito.any(RemoteCreateAssetHandler.class));
        verify(m_AssetMgr).createRemoteAssetHandler();

        //get message
        CreateAssetRequestData requestCapture = request.getValue();

        //verify
        assertThat(requestCapture.getProductType(), is(productType));
    }
    
    /**
     * Test the request to create a new asset.
     * Verify message sent.
     * Verify response handler requested from asset manager.
     * Verify configuration properties are sent with message.
     */
    @Test
    public void testRequestCreateAssetWithProps()
    {
        
        //mock asset factory
        AssetFactoryModel factoryModel = mock(AssetFactoryModel.class);
        when(factoryModel.getFullyQualifiedAssetType()).thenReturn("com.example.Awesome");
        
        //mock configuration and meta data
        UnmodifiableConfigMetatypeModel configModel = mock(UnmodifiableConfigMetatypeModel.class);
        when(m_ConfigWrapper.getConfigurationDefaultsByFactoryPidAsync(125, "com.example.AwesomeConfig"))
            .thenReturn(configModel);
        
        AttributeModel attrib1 = mock(AttributeModel.class);
        AttributeModel attrib2 = mock(AttributeModel.class);
        when(attrib1.getId()).thenReturn("some.value1");
        when(attrib1.getType()).thenReturn(AttributeDefinition.INTEGER);
        when(attrib1.getDefaultValues()).thenReturn(new ArrayList<String>());
        when(attrib1.getOptionLabels()).thenReturn(new ArrayList<String>());
        when(attrib2.getId()).thenReturn("some.value2");
        when(attrib2.getType()).thenReturn(AttributeDefinition.BOOLEAN);
        when(attrib2.getDefaultValues()).thenReturn(new ArrayList<String>());
        when(attrib2.getOptionLabels()).thenReturn(new ArrayList<String>());
        
        ConfigPropModelImpl prop1 = new ConfigPropModelImpl(attrib1);
        prop1.setValue(1337);
        ConfigPropModelImpl prop2 = new ConfigPropModelImpl(attrib2);
        prop2.setValue(true);
        List<UnmodifiablePropertyModel> propList = new ArrayList<>();
        propList.add(prop1);
        propList.add(prop2);
        when(configModel.getProperties()).thenReturn(propList);
        
        //controller id to use
        int systemId = 321;

        //create variables
        String productType = "mil.dod.you.me.Everybody";
        String name = "EverybodyAsset";

        m_SUT.setAssetFactory(factoryModel);
        m_SUT.requestCreateAsset(productType, systemId, name);

        //verify message sent to remove the assetS
        ArgumentCaptor<CreateAssetRequestData> request = 
            ArgumentCaptor.forClass(CreateAssetRequestData.class);

        verify(m_MessageFactory).createAssetDirectoryServiceMessage(
            eq(AssetDirectoryServiceMessageType.CreateAssetRequest), 
                request.capture());
        verify(m_MessageWrapper).queue(eq(systemId), Mockito.any(RemoteCreateAssetHandler.class));
        verify(m_AssetMgr).createRemoteAssetHandler();

        //get message
        CreateAssetRequestData requestCapture = request.getValue();

        //verify
        assertThat(requestCapture.getProductType(), is(productType));
        Map<String, Object> requestProps = SharedMessageUtils.convertListSimpleTypesMapEntrytoMapStringObject(
                requestCapture.getPropertiesList());
        assertThat(requestProps, hasEntry("some.value1", (Object)1337));
        assertThat(requestProps, hasEntry("some.value2", (Object)true));
    }

/**
     * Test the request for asset types.
     * Verify message sent.
     */
    @Test
    public void testRequestAssetTypeUpdate()
    {
        //controller id to use
        int systemId = 321;

        m_SUT.requestAssetTypeUpdate(systemId);
        
        //verify
        verify(m_MessageFactory).createAssetDirectoryServiceMessage(
            AssetDirectoryServiceMessageType.GetAssetTypesRequest, null);
        verify(m_MessageWrapper).queue(systemId, null);
    }
    
    /**
     * Verify returns null if the dialog helper does not have a set asset factory.
     */
    @Test
    public void testGetAssetCapabilitiesReturnNull()
    {
        AssetCapabilities caps = m_SUT.getAssetCaps();
        
        assertThat(caps, nullValue());
    }
    
    /**
     * Verify AssetCapabilities are returned from an AssetFactoryModel.
     */
    @Test
    public void testGetAssetCapabilities()
    {
        AssetFactoryModel assetFactory = mock(AssetFactoryModel.class);
        AssetCapabilities assetCapabilities = mock(AssetCapabilities.class);
        when(assetFactory.getFactoryCaps()).thenReturn(assetCapabilities);
        
        m_SUT.setAssetFactory(assetFactory);
        AssetCapabilities returnedCaps = m_SUT.getAssetCaps();
        
        assertThat(returnedCaps, notNullValue());
        assertThat(returnedCaps, is(assetCapabilities));
    }
}
