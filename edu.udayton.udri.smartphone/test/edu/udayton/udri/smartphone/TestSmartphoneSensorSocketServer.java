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
package edu.udayton.udri.smartphone;

import static org.mockito.Mockito.*;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.factory.FactoryException;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

@RunWith(MockitoJUnitRunner.class)
public class TestSmartphoneSensorSocketServer 
{
    private static final int WEBSOCKET_PORT_NUMBER = 9090;
    private static final String WEBSOCKET_IP_ADDRESS = "10.111.99.99"; //NOPMD: Need defined IP address
    
    private SmartphoneSensorSocketServer m_SUT;
    
    @Mock private BundleContext m_Context;
    @Mock private ServiceReference<AssetDirectoryService> m_ServiceRef;
    @Mock private AssetDirectoryService m_Service;
    @Mock private WebSocket m_Socket;
    @Mock private ClientHandshake m_Shake;
    @Mock private Map<String, Asset> m_ConnectionMap;
    @Mock private Asset m_Asset;
    
    @Before
    public void setUp() throws UnknownHostException
    {
        when(m_Context.getServiceReference(AssetDirectoryService.class)).thenReturn(m_ServiceRef);
        when(m_Context.getService(m_ServiceRef)).thenReturn(m_Service);
        
        m_SUT = new SmartphoneSensorSocketServer(
                new InetSocketAddress(WEBSOCKET_PORT_NUMBER),
                m_Context);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testOnOpen() throws AssetException
    {
        when(m_Socket.getRemoteSocketAddress())
            .thenReturn(new InetSocketAddress(WEBSOCKET_IP_ADDRESS, WEBSOCKET_PORT_NUMBER));
        when(m_Service.createAsset(anyString(), anyString(), anyMap())).thenReturn(m_Asset);
        
        m_SUT.onOpen(m_Socket, m_Shake);
        
        verify(m_Service).createAsset(anyString(), anyString(), anyMap());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testOnMessage() throws IllegalArgumentException, IllegalStateException, FactoryException
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("dataType", "coordinates");
        jsonObject.put("longitude", -54.33);
        jsonObject.put("latitude", 43.7);
        
        when(m_Socket.getRemoteSocketAddress())
            .thenReturn(new InetSocketAddress(WEBSOCKET_IP_ADDRESS, WEBSOCKET_PORT_NUMBER));
        when(m_Service.createAsset(anyString(), anyString(), anyMap())).thenReturn(m_Asset);
        
        testOnOpen();
        
        m_SUT.onMessage(m_Socket, jsonObject.toString());
        
        jsonObject = new JSONObject();
        jsonObject.put("dataType", "batteryStatus");
        jsonObject.put("batteryLevel", .76);

        m_SUT.onMessage(m_Socket, jsonObject.toString());
        
        verify(m_Asset, times(2)).setProperties(anyMap());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testOnClose() throws AssetException, IllegalArgumentException
    {
        when(m_Socket.getRemoteSocketAddress())
            .thenReturn(new InetSocketAddress(WEBSOCKET_IP_ADDRESS, WEBSOCKET_PORT_NUMBER));
        when(m_Service.createAsset(anyString(), anyString(), anyMap())).thenReturn(m_Asset);
        
        m_SUT.onOpen(m_Socket, m_Shake);
        
        m_SUT.onClose(m_Socket, 1000, "", true);
        
        verify(m_Asset).delete();
    }
    
    @Test
    public void testOnError()
    {
        m_SUT.onError(m_Socket, new Exception());
    }
}
