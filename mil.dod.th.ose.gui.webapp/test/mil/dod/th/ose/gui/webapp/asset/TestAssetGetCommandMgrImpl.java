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
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.faces.application.FacesMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.google.protobuf.InvalidProtocolBufferException;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.capability.AssetCapabilities;
import mil.dod.th.core.asset.capability.CommandCapabilities;
import mil.dod.th.core.asset.commands.GetVersionResponse;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandResponseData;
import mil.dod.th.core.types.command.CommandResponseEnum;
import mil.dod.th.core.types.command.CommandTypeEnum;
import mil.dod.th.ose.gui.webapp.asset.AssetGetCommandMgrImpl.CommandEventHandler;
import mil.dod.th.ose.gui.webapp.asset.AssetGetCommandMgrImpl.AsyncCommandResponseEventHandler;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.ose.remote.api.CommandConverter;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.lexicon.asset.commands.BaseTypesGen;
import mil.dod.th.remote.lexicon.asset.commands.GetVersionResponseGen;
import mil.dod.th.remote.lexicon.types.command.CommandTypesGen;

/**
 * Tests the asset get command manager.
 * 
 * @author nickmarcucci
 *
 */
public class TestAssetGetCommandMgrImpl
{
    private static int CONTROLLER_ID = 1;
    private AssetGetCommandMgrImpl m_SUT;
    
    private EventAdmin m_EventAdmin;
    private GrowlMessageUtil m_GrowlUtil;
    private BundleContextUtil m_BundleUtil;
    private BundleContext m_BundleContext;
    private AssetTypesMgr m_AssetTypesMgr;
    private CommandConverter m_CommandConverter;
    @SuppressWarnings("rawtypes") //TH-534:unable to parameterize at the moment
    private ServiceRegistration m_HandlerReg;
    
    
    @SuppressWarnings("unchecked")
    @Before
    public void init()
    {
        m_SUT = new AssetGetCommandMgrImpl();
        
        m_EventAdmin = mock(EventAdmin.class);
        m_GrowlUtil = mock(GrowlMessageUtil.class);
        m_BundleUtil = mock(BundleContextUtil.class);
        m_AssetTypesMgr = mock(AssetTypesMgr.class);
        m_CommandConverter = mock(CommandConverter.class);
        m_BundleContext = mock(BundleContext.class);
        
        //mock behavior for event listener
        when(m_BundleUtil.getBundleContext()).thenReturn(m_BundleContext);
        when(m_BundleContext.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
            Mockito.any(Dictionary.class))).thenReturn(m_HandlerReg);
        
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setGrowlMessageUtil(m_GrowlUtil);
        m_SUT.setBundleContextUtil(m_BundleUtil);
        m_SUT.setCommandConverter(m_CommandConverter);
        m_SUT.setAssetTypesMgr(m_AssetTypesMgr);
        
        m_SUT.postConstruct();
        
        verify(m_BundleUtil, times(2)).getBundleContext();
        verify(m_BundleContext, times(2)).registerService(eq(EventHandler.class), 
                Mockito.any(EventHandler.class), Mockito.any(Dictionary.class));
    }
    
    /**
     * Verify that correct command model is retrieved.
     */
    @Test
    public void testGetAssetCommands()
    {
        AssetModel asset = mock(AssetModel.class);
        UUID uuid = UUID.randomUUID();
        when(asset.getControllerId()).thenReturn(CONTROLLER_ID);
        when(asset.getUuid()).thenReturn(uuid);
        
        AssetCapabilities assetCaps = mock(AssetCapabilities.class);
        CommandCapabilities capabilities = mock(CommandCapabilities.class);
        AssetFactoryModel fModel = mock(AssetFactoryModel.class);
        when(m_AssetTypesMgr.getAssetFactoryForClassAsync(eq(CONTROLLER_ID), Mockito.anyString()))
            .thenReturn(fModel);
        
        when(fModel.getFactoryCaps()).thenReturn(assetCaps);
        when(assetCaps.getCommandCapabilities()).thenReturn(capabilities);
        
        List<CommandTypeEnum> list = new ArrayList<CommandTypeEnum>();
        list.add(CommandTypeEnum.GET_VERSION_COMMAND);
        list.add(CommandTypeEnum.GET_TUNE_SETTINGS_COMMAND);
        when(capabilities.getSupportedCommands()).thenReturn(list);
        
        AssetGetCommandModel commandModel = m_SUT.getAssetCommands(asset);
        assertThat(commandModel, notNullValue());
        assertThat(commandModel.getUuid(), is(uuid));
        assertThat(commandModel.getSupportedCommands().size(), is(2));
        
        //Update list of supported commands
        list.remove(CommandTypeEnum.GET_VERSION_COMMAND);
        list.add(CommandTypeEnum.GET_PROFILES_COMMAND);
        list.add(CommandTypeEnum.GET_MODE_COMMAND);
        when(capabilities.getSupportedCommands()).thenReturn(list);
        
        //Verify model contains appropriate set of commands after being updated.
        commandModel = m_SUT.getAssetCommands(asset);
        assertThat(commandModel, notNullValue());
        assertThat(commandModel.getUuid(), is(uuid));
        assertThat(commandModel.getSupportedCommands().size(), is(2));
        assertThat(commandModel.getSupportedCommands(), containsInAnyOrder(CommandTypeEnum.GET_TUNE_SETTINGS_COMMAND,
                CommandTypeEnum.GET_PROFILES_COMMAND));
        
        AssetModel anotherAsset = mock(AssetModel.class);
        UUID uuid2 = UUID.randomUUID();
        when(anotherAsset.getControllerId()).thenReturn(CONTROLLER_ID);
        when(anotherAsset.getUuid()).thenReturn(uuid2);
        
        AssetGetCommandModel anotherModel = m_SUT.getAssetCommands(anotherAsset);
        assertThat(anotherModel, notNullValue());
        assertThat(anotherModel.getUuid(), is(uuid2));
        
        list.clear();
        list.add(CommandTypeEnum.SET_CAMERA_SETTINGS_COMMAND);
        when(capabilities.getSupportedCommands()).thenReturn(list);
        
        anotherModel = m_SUT.getAssetCommands(anotherAsset);
        assertThat(anotherModel, nullValue());
    }
    
    /*
     * Verify that command handler with unknown controller doesn't do anything if controller id
     * isn't known.
     */
    @Test
    public void testCommandHandlerUnknownControllerId()
    {
        CommandEventHandler eventHandler = m_SUT.new CommandEventHandler();
        
        ExecuteCommandResponseData dataMsg = createVersionResponseMessage(UUID.randomUUID());
        
        Map<String, Object> map = new HashMap<>();
        map.put(RemoteConstants.EVENT_PROP_SOURCE_ID, CONTROLLER_ID);
        map.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, dataMsg);
        Event event = new Event("topic", map);
        
        eventHandler.handleEvent(event);
        
        verify(m_EventAdmin, never()).postEvent(Mockito.any(Event.class));
    }
    
    /*
     * Verify that command handler dosen't do anything if asset is not known.
     */
    @Test
    public void testCommandHandlerUnknownModel()
    {
        CommandEventHandler eventHandler = m_SUT.new CommandEventHandler();
        
        AssetModel asset = mock(AssetModel.class);
        UUID uuid = UUID.randomUUID();
        when(asset.getControllerId()).thenReturn(CONTROLLER_ID);
        when(asset.getUuid()).thenReturn(uuid);
        
        AssetCapabilities assetCaps = mock(AssetCapabilities.class);
        CommandCapabilities capabilities = mock(CommandCapabilities.class);
        AssetFactoryModel fModel = mock(AssetFactoryModel.class);
        when(m_AssetTypesMgr.getAssetFactoryForClassAsync(eq(CONTROLLER_ID), Mockito.anyString()))
            .thenReturn(fModel);
        
        when(fModel.getFactoryCaps()).thenReturn(assetCaps);
        when(assetCaps.getCommandCapabilities()).thenReturn(capabilities);
        
        List<CommandTypeEnum> list = new ArrayList<CommandTypeEnum>();
        list.add(CommandTypeEnum.GET_VERSION_COMMAND);
        when(capabilities.getSupportedCommands()).thenReturn(list);
        
        m_SUT.getAssetCommands(asset);
        
        AssetModel assetUnknown = mock(AssetModel.class);
        UUID uuid2 = UUID.randomUUID();
        when(assetUnknown.getControllerId()).thenReturn(CONTROLLER_ID);
        when(assetUnknown.getUuid()).thenReturn(uuid2);
        
        ExecuteCommandResponseData dataMsg = createVersionResponseMessage(uuid);
        
        Map<String, Object> map = new HashMap<>();
        map.put(RemoteConstants.EVENT_PROP_SOURCE_ID, CONTROLLER_ID);
        map.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, dataMsg);
        
        Event event = new Event("topic", map);
        
        eventHandler.handleEvent(event);
        
        verify(m_EventAdmin, never()).postEvent(Mockito.any(Event.class));
    }
    
    /*
     * Verify command handler processes response data correctly.
     */
    @Test
    public void testCommandHandler() throws InvalidProtocolBufferException, ObjectConverterException
    {
        CommandEventHandler eventHandler = m_SUT.new CommandEventHandler();
        
        AssetModel asset = mock(AssetModel.class);
        UUID uuid = UUID.randomUUID();
        when(asset.getControllerId()).thenReturn(CONTROLLER_ID);
        when(asset.getUuid()).thenReturn(uuid);
        
        AssetCapabilities assetCaps = mock(AssetCapabilities.class);
        CommandCapabilities capabilities = mock(CommandCapabilities.class);
        AssetFactoryModel fModel = mock(AssetFactoryModel.class);
        when(m_AssetTypesMgr.getAssetFactoryForClassAsync(eq(CONTROLLER_ID), Mockito.anyString()))
            .thenReturn(fModel);
        
        when(fModel.getFactoryCaps()).thenReturn(assetCaps);
        when(assetCaps.getCommandCapabilities()).thenReturn(capabilities);
        
        List<CommandTypeEnum> list = new ArrayList<CommandTypeEnum>();
        list.add(CommandTypeEnum.GET_VERSION_COMMAND);
        when(capabilities.getSupportedCommands()).thenReturn(list);
        
        m_SUT.getAssetCommands(asset);
        
        Response theResponse = mock(Response.class);
        when(m_CommandConverter.getJavaResponseType(Mockito.any(byte[].class), 
                Mockito.any(CommandResponseEnum.class))).thenReturn(theResponse);
        when(m_CommandConverter.getCommandTypeFromResponseType(
                Mockito.any(CommandResponseEnum.class))).thenReturn(CommandTypeEnum.GET_VERSION_COMMAND);
        
        ExecuteCommandResponseData dataMsg = createVersionResponseMessage(uuid);
        
        Map<String, Object> map = new HashMap<>();
        map.put(RemoteConstants.EVENT_PROP_SOURCE_ID, CONTROLLER_ID);
        map.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, dataMsg);
        
        Event event = new Event("topic", map);
        
        eventHandler.handleEvent(event);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent, notNullValue());
        assertThat(capturedEvent.getTopic(), is(AssetGetCommandMgr.TOPIC_GET_RESPONSE_RECEIVED));
    }
    
    /*
     * Verify command handler logs proper error with invalid protocol exception.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCommandHandlerInvalidProtocolBufferException() throws InvalidProtocolBufferException, 
        ObjectConverterException
    {
        CommandEventHandler eventHandler = m_SUT.new CommandEventHandler();
        
        AssetModel asset = mock(AssetModel.class);
        UUID uuid = UUID.randomUUID();
        when(asset.getControllerId()).thenReturn(CONTROLLER_ID);
        when(asset.getUuid()).thenReturn(uuid);
        
        AssetCapabilities assetCaps = mock(AssetCapabilities.class);
        CommandCapabilities capabilities = mock(CommandCapabilities.class);
        AssetFactoryModel fModel = mock(AssetFactoryModel.class);
        when(m_AssetTypesMgr.getAssetFactoryForClassAsync(eq(CONTROLLER_ID), Mockito.anyString()))
            .thenReturn(fModel);
        
        when(fModel.getFactoryCaps()).thenReturn(assetCaps);
        when(assetCaps.getCommandCapabilities()).thenReturn(capabilities);
        
        List<CommandTypeEnum> list = new ArrayList<CommandTypeEnum>();
        list.add(CommandTypeEnum.GET_VERSION_COMMAND);
        when(capabilities.getSupportedCommands()).thenReturn(list);
        
        m_SUT.getAssetCommands(asset);
        
        when(m_CommandConverter.getJavaResponseType(Mockito.any(byte[].class), 
                Mockito.any(CommandResponseEnum.class))).thenThrow(InvalidProtocolBufferException.class);
        when(m_CommandConverter.getCommandTypeFromResponseType(
                Mockito.any(CommandResponseEnum.class))).thenReturn(CommandTypeEnum.GET_VERSION_COMMAND);
        
        ExecuteCommandResponseData dataMsg = createVersionResponseMessage(uuid);
        
        Map<String, Object> map = new HashMap<>();
        map.put(RemoteConstants.EVENT_PROP_SOURCE_ID, CONTROLLER_ID);
        map.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, dataMsg);
        
        Event event = new Event("topic", map);
        
        eventHandler.handleEvent(event);
        
        String msg = String.format("Received a get command response of type " 
                + "%s for system 0x%08x for asset with uuid" 
                + " %s but could not convert that response.", 
                CommandResponseEnum.GET_VERSION_RESPONSE, CONTROLLER_ID, asset.getUuid());
        verify(m_EventAdmin, never()).postEvent(Mockito.any(Event.class));
        verify(m_GrowlUtil).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_ERROR), 
                eq("Unable to handle Get Command Response"), eq(msg));
    }
    
    /*
     * Verify command handler logs proper error with object converter exception.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCommandHandlerObjectConverterException() throws InvalidProtocolBufferException, 
        ObjectConverterException
    {
        CommandEventHandler eventHandler = m_SUT.new CommandEventHandler();
        
        AssetModel asset = mock(AssetModel.class);
        UUID uuid = UUID.randomUUID();
        when(asset.getControllerId()).thenReturn(CONTROLLER_ID);
        when(asset.getUuid()).thenReturn(uuid);
        
        AssetCapabilities assetCaps = mock(AssetCapabilities.class);
        CommandCapabilities capabilities = mock(CommandCapabilities.class);
        AssetFactoryModel fModel = mock(AssetFactoryModel.class);
        when(m_AssetTypesMgr.getAssetFactoryForClassAsync(eq(CONTROLLER_ID), Mockito.anyString()))
            .thenReturn(fModel);
        
        when(fModel.getFactoryCaps()).thenReturn(assetCaps);
        when(assetCaps.getCommandCapabilities()).thenReturn(capabilities);
        
        List<CommandTypeEnum> list = new ArrayList<CommandTypeEnum>();
        list.add(CommandTypeEnum.GET_VERSION_COMMAND);
        when(capabilities.getSupportedCommands()).thenReturn(list);
        
        m_SUT.getAssetCommands(asset);
        
        when(m_CommandConverter.getJavaResponseType(Mockito.any(byte[].class), 
                Mockito.any(CommandResponseEnum.class))).thenThrow(InvalidProtocolBufferException.class);
        when(m_CommandConverter.getCommandTypeFromResponseType(
                Mockito.any(CommandResponseEnum.class))).thenReturn(CommandTypeEnum.GET_VERSION_COMMAND);
        
        ExecuteCommandResponseData dataMsg = createVersionResponseMessage(uuid);
        
        Map<String, Object> map = new HashMap<>();
        map.put(RemoteConstants.EVENT_PROP_SOURCE_ID, CONTROLLER_ID);
        map.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, dataMsg);
        
        Event event = new Event("topic", map);
        
        eventHandler.handleEvent(event);
        
        String msg = String.format("Received a get command response of type " 
                + "%s for system 0x%08x for asset with uuid" 
                + " %s but could not convert that response.", 
                CommandResponseEnum.GET_VERSION_RESPONSE, CONTROLLER_ID, asset.getUuid());
        verify(m_EventAdmin, never()).postEvent(Mockito.any(Event.class));
        verify(m_GrowlUtil).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_ERROR), 
                eq("Unable to handle Get Command Response"), eq(msg));
    }
    
    /**
     * Verify command response handler processes event data correctly.
     */
    @Test
    public void testCommandResponseHandler()
    {
        AsyncCommandResponseEventHandler eventHandler = m_SUT.new AsyncCommandResponseEventHandler();
        
        AssetModel asset = mock(AssetModel.class);
        UUID uuid = UUID.randomUUID();
        when(asset.getControllerId()).thenReturn(CONTROLLER_ID);
        when(asset.getUuid()).thenReturn(uuid);
        
        //set up the command map
        AssetCapabilities assetCaps = mock(AssetCapabilities.class);
        CommandCapabilities capabilities = mock(CommandCapabilities.class);
        AssetFactoryModel fModel = mock(AssetFactoryModel.class);
        when(m_AssetTypesMgr.getAssetFactoryForClassAsync(eq(CONTROLLER_ID), Mockito.anyString()))
            .thenReturn(fModel);
        
        when(fModel.getFactoryCaps()).thenReturn(assetCaps);
        when(assetCaps.getCommandCapabilities()).thenReturn(capabilities);
        
        List<CommandTypeEnum> list = new ArrayList<CommandTypeEnum>();
        list.add(CommandTypeEnum.GET_VERSION_COMMAND);
        when(capabilities.getSupportedCommands()).thenReturn(list);
        
        //initializing...
        m_SUT.getAssetCommands(asset);
        
        //converter mocking
        when(m_CommandConverter.getCommandResponseEnumFromClassName(GetVersionResponse.class.getName())).
            thenReturn(CommandResponseEnum.GET_VERSION_RESPONSE);
        when(m_CommandConverter.getCommandTypeFromResponseType(CommandResponseEnum.GET_VERSION_RESPONSE)).
            thenReturn(CommandTypeEnum.GET_VERSION_COMMAND);
        
        GetVersionResponse versionResponse = new GetVersionResponse(null, null, "version");
        Map<String, Object> map = new HashMap<>();
        map.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, CONTROLLER_ID);
        map.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid.toString());
        map.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE, GetVersionResponse.class.getName());
        map.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE, versionResponse);
        
        Event event = new Event("topic", map);
        
        eventHandler.handleEvent(event);
        
        //verify data
        AssetGetCommandModel getModel = m_SUT.getAssetCommands(asset);
        assertThat(getModel.getCommandResponseByType(CommandTypeEnum.GET_VERSION_COMMAND), is(notNullValue()));
        assertThat(getModel.getCommandResponseByType(CommandTypeEnum.GET_VERSION_COMMAND).printResponseMessage(), 
                is(versionResponse.toString()));
    }
    
    /**
     * Verify no action take if the system collection is null when a command updated response is received.
     */
    @Test
    public void testCommandResponseHandlerNullSystemLookup()
    {
        AsyncCommandResponseEventHandler eventHandler = m_SUT.new AsyncCommandResponseEventHandler();
        
        GetVersionResponse versionResponse = new GetVersionResponse(null, null, "version");
        Map<String, Object> map = new HashMap<>();
        map.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, CONTROLLER_ID);
        map.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, UUID.randomUUID().toString());
        map.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE, GetVersionResponse.class.getName());
        map.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE, versionResponse);
        
        Event event = new Event("topic", map);
        
        eventHandler.handleEvent(event);
        
        //verify no interactions
        verify(m_CommandConverter, never()).getCommandResponseEnumFromClassName(anyString());
    }
    
    /**
     * Verify no action taken if the system collection of models is not null, but the given asset UUID is unknown.
     */
    @Test
    public void testCommandResponseHandlerNullModel()
    {
        AsyncCommandResponseEventHandler eventHandler = m_SUT.new AsyncCommandResponseEventHandler();
        
        AssetModel asset = mock(AssetModel.class);
        UUID uuid = UUID.randomUUID();
        when(asset.getControllerId()).thenReturn(CONTROLLER_ID);
        when(asset.getUuid()).thenReturn(uuid);
        
        //set up the command map
        AssetCapabilities assetCaps = mock(AssetCapabilities.class);
        CommandCapabilities capabilities = mock(CommandCapabilities.class);
        AssetFactoryModel fModel = mock(AssetFactoryModel.class);
        when(m_AssetTypesMgr.getAssetFactoryForClassAsync(eq(CONTROLLER_ID), Mockito.anyString()))
            .thenReturn(fModel);
        
        when(fModel.getFactoryCaps()).thenReturn(assetCaps);
        when(assetCaps.getCommandCapabilities()).thenReturn(capabilities);
        
        List<CommandTypeEnum> list = new ArrayList<CommandTypeEnum>();
        list.add(CommandTypeEnum.GET_VERSION_COMMAND);
        when(capabilities.getSupportedCommands()).thenReturn(list);
        
        //initializing system lookup map
        m_SUT.getAssetCommands(asset);
        
        GetVersionResponse versionResponse = new GetVersionResponse(null, null, "version");
        Map<String, Object> map = new HashMap<>();
        map.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, CONTROLLER_ID);
        map.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, UUID.randomUUID().toString());
        map.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE, GetVersionResponse.class.getName());
        map.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE, versionResponse);
        
        Event event = new Event("topic", map);
        
        eventHandler.handleEvent(event);
        
        //verify no interactions
        verify(m_CommandConverter, never()).getCommandResponseEnumFromClassName(anyString());
    }
    
    /**
     * Test that cleanup will actually deregister the event handlers.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testCleanUpAndReg()
    {
        ServiceRegistration handlerRegA = mock(ServiceRegistration.class);
        ServiceRegistration handlerRegB = mock(ServiceRegistration.class);
        ArgumentCaptor<Dictionary> dictionaryCap = ArgumentCaptor.forClass(Dictionary.class);
        when(m_BundleUtil.getBundleContext()).thenReturn(m_BundleContext);
        when(m_BundleContext.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
                dictionaryCap.capture())).thenReturn(handlerRegA, handlerRegB);
        
        m_SUT.postConstruct();
        assertThat(dictionaryCap.getAllValues().size(), is(2));
        Dictionary<String, Object> dict1 = dictionaryCap.getAllValues().get(0);
        Dictionary<String, Object> dict2 = dictionaryCap.getAllValues().get(1);
        
        assertThat((String)dict1.get(EventConstants.EVENT_TOPIC), 
                isOneOf(RemoteConstants.TOPIC_MESSAGE_RECEIVED, 
                        Asset.TOPIC_COMMAND_RESPONSE_UPDATED + RemoteConstants.REMOTE_TOPIC_SUFFIX));
        assertThat((String)dict2.get(EventConstants.EVENT_TOPIC), 
                isOneOf(RemoteConstants.TOPIC_MESSAGE_RECEIVED, 
                        Asset.TOPIC_COMMAND_RESPONSE_UPDATED + RemoteConstants.REMOTE_TOPIC_SUFFIX));
        m_SUT.cleanup();
        
        //verify
        verify(handlerRegA).unregister();
        verify(handlerRegB).unregister();
    }
    
    /**
     * Method that creates a {@link ExecuteCommandResponseData} message with a version response message set and 
     * specified UUID.
     */
    private ExecuteCommandResponseData createVersionResponseMessage(final UUID uuid)
    {
        BaseTypesGen.Response baseResponse = BaseTypesGen.Response.newBuilder().build();
        
        GetVersionResponseGen.GetVersionResponse versionResponse = GetVersionResponseGen.GetVersionResponse.newBuilder()
                .setBase(baseResponse)
                .setCurrentVersion("ver")
                .build();
        
        ExecuteCommandResponseData dataMsg = ExecuteCommandResponseData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid))
                .setResponseType(CommandTypesGen.CommandResponse.Enum.GET_VERSION_RESPONSE)
                .setResponse(versionResponse.toByteString())
                .build();
        return dataMsg;
    }
}
