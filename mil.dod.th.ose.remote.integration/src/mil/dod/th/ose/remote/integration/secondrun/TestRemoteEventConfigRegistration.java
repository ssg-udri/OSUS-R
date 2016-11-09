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
package mil.dod.th.ose.remote.integration.secondrun;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace;
import mil.dod.th.core.remote.proto.EventMessages.SendEventData;
import mil.dod.th.core.remote.proto.MapTypes.ComplexTypesMapEntry;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetResponseData;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.ose.remote.integration.AssetNamespaceUtils;
import mil.dod.th.ose.remote.integration.MessageListener;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import mil.dod.th.remote.lexicon.observation.types.ObservationGen;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.ByteString;

import example.asset.ExampleAsset;

/**
 * This test class verifies that remote event registrations in the configs.xml are actually
 * registered for and send events when applicable.
 * @author allenchl
 *
 */
public class TestRemoteEventConfigRegistration
{
    private static SharedMessages.UUID testUuid;
    private Socket m_Socket;
    
    /**
     * create socket.
     */
    @Before
    public void setup() throws UnknownHostException, IOException, InterruptedException
    {
        m_Socket = SocketHostHelper.connectToController();
    }

    /**
     * Close the socket. 
     */
    @After
    public void teardown() throws UnknownHostException, IOException
    {       
        try 
        {
            AssetNamespaceUtils.removeAsset(m_Socket, testUuid);
        }
        finally
        {
            m_Socket.close();
        }
    }
    
    /**
     * Verify event sent in response to the created asset's capturing of data.
     */
    @Test
    public void testCaptureDataRemoteEventReg() throws IOException
    {
        CreateAssetResponseData createAssetResponse = AssetNamespaceUtils.createAsset(m_Socket,
                ExampleAsset.class.getName(), "remoteConfigTestAsset", null);
        testUuid = createAssetResponse.getInfo().getUuid();
        MessageListener listener = new MessageListener(m_Socket);
        AssetNamespaceUtils.requestDataCapture(m_Socket, testUuid);
        
        //listen for response message 
        EventAdminNamespace message = (EventAdminNamespace)listener.waitForMessage(Namespace.EventAdmin, 
                EventAdminMessageType.SendEvent, 1000);
        SendEventData data = SendEventData.parseFrom(message.getData());
        assertThat(data.getTopic(), is("mil/dod/th/core/persistence/ObservationStore/OBSERVATION_PERSISTED_WITH_OBS"));
        Map<String, Object> props = new HashMap<>();
        for (ComplexTypesMapEntry entry : data.getPropertyList())
        {
            if (entry.hasObservationNative())
            {
                props.put(entry.getKey(), entry.getObservationNative());
            }
            else
            {
                props.put(entry.getKey(), entry.getMulti().getStringValue());
            }
        }
        assertThat((String)props.get(FactoryDescriptor.EVENT_PROP_OBJ_NAME), is("remoteConfigTestAsset"));
        assertThat(((ObservationGen.Observation)props.get("observation")).getClass(), 
                is((Object)ObservationGen.Observation.class));
    }
    
    /**
     * Verify event sent in response to the created asset's capturing of data.
     */
    @Test
    public void testCaptureDataRemoteEventRegXml() throws IOException
    {
        CreateAssetResponseData createAssetResponse = AssetNamespaceUtils.createAsset(m_Socket,
                ExampleAsset.class.getName(), "remoteConfigTestAssetXml", null);
        testUuid = createAssetResponse.getInfo().getUuid();
        MessageListener listener = new MessageListener(m_Socket);
        AssetNamespaceUtils.requestDataCapture(m_Socket, testUuid);
        
        //listen for response message 
        EventAdminNamespace message = (EventAdminNamespace)listener.waitForMessage(Namespace.EventAdmin, 
                EventAdminMessageType.SendEvent, 1000);
        SendEventData data = SendEventData.parseFrom(message.getData());
        assertThat(data.getTopic(), is("mil/dod/th/core/persistence/ObservationStore/OBSERVATION_PERSISTED_WITH_OBS"));
        Map<String, Object> props = new HashMap<>();
        for (ComplexTypesMapEntry entry : data.getPropertyList())
        {
            if (entry.hasObservationXml())
            {
                props.put(entry.getKey(), entry.getObservationXml());
            }
            else
            {
                props.put(entry.getKey(), entry.getMulti().getStringValue());
            }
        }
        assertThat((String)props.get(FactoryDescriptor.EVENT_PROP_OBJ_NAME), is("remoteConfigTestAssetXml"));
        assertThat(((ByteString)props.get("observation")).isEmpty(), is(false));
    }
}
