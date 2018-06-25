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

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.log.Logging;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

/**
 * A class to handle incoming websocket connection data. New connections create a new client asset to return
 * observations containing GPS coordinates and battery level.
 * 
 * @author rosenwnj
 *
 */
public class SmartphoneSensorSocketServer extends WebSocketServer 
{
    private static final String JSON_LATITUDE = "latitude";
    private static final String JSON_LONGITUDE = "longitude";
    private static final String JSON_BATTERY_LEVEL = "batteryLevel";
    private static final String JSON_ERROR = "Unknown phone data retrieved";
    
    /**
     * The AssetDirectoryService for creating and removing assets.
     */
    private AssetDirectoryService m_AssetService;
    
    /**
     * A map of all current smartphone connections. The string key is the hostname of the smartphone.
     */
    private Map<String, Asset> m_ConnectionMap = new HashMap<String, Asset>();

    /**
     * Constructor for a SmartphoneSensorSocketServer. An address consisting of the hostname and/or port is needed,
     * along with the BundleContext for referencing the AssetDirectoryService.
     * 
     * @param address the address on which the server is created and listening for connections and data
     * @param context the BundleContext for creating references to the AssetDirectoryService and LoggingService
     */
    public SmartphoneSensorSocketServer(final InetSocketAddress address,
            final BundleContext context)
    {
        super(address);
        
        final ServiceReference<AssetDirectoryService> assetRef = 
            context.getServiceReference(AssetDirectoryService.class);
        
        m_AssetService = context.getService(assetRef);
    }

    @Override
    public void onClose(final WebSocket socket, final int closeCode, final String reason, final boolean remote) 
    {
        final String connectionHostname = socket.getRemoteSocketAddress().getHostName();
        final Asset removingClient = m_ConnectionMap.get(connectionHostname);

        removingClient.delete();
        m_ConnectionMap.remove(connectionHostname);
    }

    @Override
    public void onError(final WebSocket socket, final Exception e)
    {
        Logging.log(LogService.LOG_ERROR, "Socket server connection error");
        e.printStackTrace();
    }

    @Override
    public void onMessage(final WebSocket socket, final String message) 
    {
        Logging.log(LogService.LOG_INFO, "New smartphone data recieved");
        
        final String connectionHostname = socket.getRemoteSocketAddress().getHostName();
        
        final Asset currentClient = m_ConnectionMap.get(connectionHostname);
        final Map<String, Object> currentClientProps = currentClient.getProperties();
        
        try 
        {
            final JSONObject clientData = new JSONObject(message);
            
            switch (clientData.getString("dataType"))
            {
                case "coordinates": currentClientProps.put(JSON_LATITUDE, clientData.getDouble(JSON_LATITUDE));
                                    currentClientProps.put(JSON_LONGITUDE, clientData.getDouble(JSON_LONGITUDE));
                                    break;
                                    
                case "batteryStatus":    currentClientProps.put(JSON_BATTERY_LEVEL,
                                             clientData.getDouble(JSON_BATTERY_LEVEL));
                                         break;
                                     
                default:            Logging.log(LogService.LOG_ERROR, JSON_ERROR);
                                    throw new IllegalArgumentException(JSON_ERROR);
            }
            
            currentClient.setProperties(currentClientProps);
            m_ConnectionMap.put(connectionHostname, currentClient);
        } 
        catch (final Exception e) 
        {
            Logging.log(LogService.LOG_ERROR, "Error passing JSON data to asset");
            e.printStackTrace();
        }
    }

    @Override
    public void onOpen(final WebSocket socket, final ClientHandshake handshake)
    {
        final String connectionHostname = socket.getRemoteSocketAddress().getHostName();
        
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("hostname", connectionHostname);
        
        try 
        {
            final Asset newAsset = m_AssetService.createAsset(
                    "edu.udayton.udri.smartphone.SmartphoneSensorAsset",
                    "SP-" + connectionHostname, props);
            m_ConnectionMap.put(connectionHostname, newAsset);
        } 
        catch (final AssetException | IllegalArgumentException e) 
        {
            Logging.log(LogService.LOG_ERROR, "Error creating smartphone asset");
            e.printStackTrace();
        }
    }
}
