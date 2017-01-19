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
package mil.dod.th.remote.client.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import java.net.Socket;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace
    .AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetAssetsResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.ControllerInfoData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.SharedMessages.UUID;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen.LexiconFormat;
import mil.dod.th.remote.client.MessageListenerService;
import mil.dod.th.remote.client.MessageSenderService;
import mil.dod.th.remote.client.generate.AssetDirectoryMessageGenerator;
import mil.dod.th.remote.client.generate.BaseMessageGenerator;
import mil.dod.th.remote.client.generate.EventAdminMessageGenerator;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * @author dhumeniuk
 *
 */
public class TestRemoteClient extends TestCase
{
    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    
    private Socket m_Socket;
    private MessageSenderService m_MsgSenderService;
    private MessageListenerService m_MsgListenerService;

    private EventAdminMessageGenerator m_EventAdminMessageGen;
    private AssetDirectoryMessageGenerator m_AssetDirMessageGen;
    private BaseMessageGenerator m_BaseMessageGen;
    
    @Override
    public void setUp() throws Exception
    {
        m_Socket = new Socket("localhost", 4000);
        
        m_MsgSenderService = ServiceUtils.getService(m_Context, MessageSenderService.class);
        m_MsgListenerService = ServiceUtils.getService(m_Context, MessageListenerService.class);
        
        m_EventAdminMessageGen = ServiceUtils.getService(m_Context, EventAdminMessageGenerator.class);
        m_AssetDirMessageGen = ServiceUtils.getService(m_Context, AssetDirectoryMessageGenerator.class);
        m_BaseMessageGen = ServiceUtils.getService(m_Context, BaseMessageGenerator.class);
        
        m_MsgSenderService.setClientId(1);
        
        m_MsgListenerService.addRemoteChannel(SystemConstants.REMOTE_SYS_ID, m_Socket.getInputStream(), null);
        m_MsgSenderService.addRemoteChannel(SystemConstants.REMOTE_SYS_ID, m_Socket.getOutputStream(), null);
        
        AssetUtils.deleteAllAsset(m_Context);
        m_EventAdminMessageGen.createEventCleanupRequest().send(SystemConstants.REMOTE_SYS_ID);
    }
    
    @Override
    public void tearDown() throws Exception
    {
        m_EventAdminMessageGen.createEventCleanupRequest().send(SystemConstants.REMOTE_SYS_ID);
        AssetUtils.deleteAllAsset(m_Context);
        
        m_MsgSenderService.removeRemoteChannel(SystemConstants.REMOTE_SYS_ID);
        m_MsgListenerService.removeRemoteChannel(SystemConstants.REMOTE_SYS_ID);
        
        m_Socket.close();
    }

    public void testGetAssets() throws Exception
    {
        AssetUtils.createAsset(m_Context, "example.asset.ExampleAsset", "get-asset");
        
        MessageListener listener = new MessageListener(m_Context);
              
        m_AssetDirMessageGen.createGetAssetsRequest().send(SystemConstants.REMOTE_SYS_ID);
        GetAssetsResponseData response = listener.waitForMessage(Namespace.AssetDirectoryService, 
                AssetDirectoryServiceMessageType.GetAssetsResponse, 5, TimeUnit.SECONDS);
        
        assertThat(response.getAssetInfoCount(), is(1));
        assertThat(response.getAssetInfoList().get(0).getProductType(), is("example.asset.ExampleAsset"));
    }

    public void testSendEventRegister() throws Exception
    {
        UUID assetUuid = AssetUtils.createAsset(m_Context, "example.asset.ExampleAsset", "event-reg-asset");
        
        final BlockingQueue<Event> receivedEvents = new LinkedBlockingQueue<>();
        final EventHandler eventHandler = new EventHandler()
        {
            @Override
            public void handleEvent(final Event event)
            {
                receivedEvents.add(event);
            }
        };

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(EventConstants.EVENT_TOPIC,
            new String[] {ObservationStore.TOPIC_OBSERVATION_PERSISTED_WITH_OBS,
                ObservationStore.TOPIC_OBSERVATION_MERGED_WITH_OBS});
        ServiceRegistration<EventHandler> reg = m_Context.registerService(EventHandler.class, eventHandler, props);

        try
        {
            m_EventAdminMessageGen.createEventRegRequest()
                .setCanQueueEvent(true)
                .setExpirationTimeHours(500)
                .setObjectFormat(LexiconFormat.Enum.XML)
                .setTopics(ObservationStore.TOPIC_OBSERVATION_PERSISTED_WITH_OBS,
                        ObservationStore.TOPIC_OBSERVATION_MERGED_WITH_OBS)
                .send(SystemConstants.REMOTE_SYS_ID);
            
            AssetUtils.captureData(m_Context, assetUuid);
            
            Event data = receivedEvents.poll(5, TimeUnit.SECONDS);
            assertThat(data, is(notNullValue()));
        }
        finally
        {
            reg.unregister();
        }
    }
    
    public void testGetControllerInfo() throws Exception
    {
        MessageListener listener = new MessageListener(m_Context);
        
        m_BaseMessageGen.createGetControllerInfoRequest().send(SystemConstants.REMOTE_SYS_ID);
        ControllerInfoData data = listener.waitForMessage(Namespace.Base, BaseMessageType.ControllerInfo, 5, 
                TimeUnit.SECONDS);
        
        assertThat(data.getName(), is("TerraHarvest"));
    }
}
