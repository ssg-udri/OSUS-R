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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;

/**
 * The activator to start a SmartphoneSensorSocketServer for smartphone connections and a web server for hosting the
 * webpage accessed by a smartphone.
 * 
 * @author rosenwnj
 */
public final class ServerActivator implements BundleActivator 
{
    private static final int WEBSOCKET_PORT_NUMBER = 9090;
    private static final int HTTP_SERVER_PORT_NUMBER = 9191;
    
    /**
     * The HTTP server to host the web content.
     */
    private HttpServer m_HttpServer;
    
    /**
     * The socket server to be started and stopped.
     */
    private SmartphoneSensorSocketServer m_SocketServer;
    
    @Override
    public void start(final BundleContext context) throws Exception 
    {
        m_HttpServer = new HttpServer();
        
        final NetworkListener listener = new NetworkListener("listener",
                NetworkListener.DEFAULT_NETWORK_HOST,
                HTTP_SERVER_PORT_NUMBER);
        
        m_HttpServer.addListener(listener);
        
        final CLStaticHttpHandler handler = new CLStaticHttpHandler(HttpServer.class.getClassLoader(), "/WebContent/");
        m_HttpServer.getServerConfiguration().addHttpHandler(handler, "/");
        m_HttpServer.start();
        
        m_SocketServer = new SmartphoneSensorSocketServer(new InetSocketAddress(WEBSOCKET_PORT_NUMBER), context);
        m_SocketServer.start();
    }

    @Override
    public void stop(final BundleContext context) throws Exception 
    {
        m_HttpServer.shutdownNow();
        
        m_SocketServer.stop();
    }
}