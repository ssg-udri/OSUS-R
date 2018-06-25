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
package example.message.router;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Modified;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.system.TerraHarvestSystem;

import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;

import example.message.SocketListener;

/**
 * Example of a service using the message routing events to handle message routing to networked controllers.
 * 
 * @author jlatham
 */
@Component(provide = ExampleMessageRouter.class, designate = ExampleMessageRouterConfig.class, 
    configurationPolicy = ConfigurationPolicy.optional, immediate = true)
public class ExampleMessageRouter
{
    private static final String SHUTDOWN_ERROR = "Error shutting down server thread.";
    private static final int TIMEOUT_MS = 5000;
    
    private int m_ServerPort;
    private ServerSocket m_ServerSocket;
    private LoggingService m_Logger;
    private TerraHarvestSystem m_System;
    private AtomicBoolean m_Activated;
    private Map<Long, SocketListener> m_ClientMap;
    private List<ComponentInstance> m_ComponentList;
    private ConnectionListener m_ConnectionListener;
    private Thread m_ConnectionThread;
    private ComponentFactory m_SocketListenerFactory;
    
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logger = logging;
    }
    
    @Reference
    public void setTerraHarvestSystem(final TerraHarvestSystem sys)
    {
        m_System = sys;
    }
    
    @Reference(target = "(" + ComponentConstants.COMPONENT_FACTORY + "=" + SocketListener.FACTORY + ")")
    public void setSocketListenerFactory(final ComponentFactory factory)
    {
        m_SocketListenerFactory = factory;
    }
    
    /**
     * Activation method that sets up the example message router.
     * 
     * @param props
     *      Properties to be passed to the service upon activation.
     * @throws IOException
     *      If the message router encounters an error activating.
     */
    @Activate
    public void activate(final Map<String, Object> props) throws IOException
    {
        m_Logger.debug("===== Activating ExampleMessageRouter =====");
        m_Activated = new AtomicBoolean();
        m_ClientMap = new HashMap<>();
        m_ComponentList = new ArrayList<>();
        modified(props);
    }
    
    /**
     * Method that is called when configuration properties are updated.
     * 
     * @param props
     *      Map of updated configuration properties.
     */
    @Modified
    public void modified(final Map<String, Object> props)
    {
        m_Logger.debug("Updating server properties.");
        final ExampleMessageRouterConfig config = Configurable
                .createConfigurable(ExampleMessageRouterConfig.class, props);
        m_ServerPort = config.port();
        stopSocketListener();
        try
        {
            startSocketListener();
        }
        catch (final IOException e)
        {
            m_Logger.error(e, "Problem updating server properties, cannot restart socket listener");
        }
        m_Logger.debug("Server properties successfully updated.");
    }
    
    /**
     * Method that is called when the message router service is deactivated.
     */
    @Deactivate
    public void deactivate()
    {
        m_Activated.set(false);
    }
    
    /**
     * Method that is called to stop the socket listener.
     */
    private void stopSocketListener()
    {        
        if (m_ServerSocket != null)
        {
            if (m_ClientMap != null)
            {
                for (long key : m_ClientMap.keySet())
                {
                    m_ClientMap.get(key).shutdown();
                }
            }

            m_ConnectionListener.shutdown();
            try
            {
                m_ServerSocket.close();
            }
            catch (final IOException ex)
            {
                m_Logger.error(ex, SHUTDOWN_ERROR);
            }
            
            try
            {
                m_ConnectionThread.join(TIMEOUT_MS);
            }
            catch (final InterruptedException ex)
            {
                m_Logger.debug("Connection thread interrupted while waiting to join.");
            }
            
            for (ComponentInstance instance: m_ComponentList)
            {
                instance.dispose();
            }
        }
    }
    
    /**
     * Starts the socket listener.
     * 
     * @throws IOException
     *      If the socket listener cannot be started.
     */
    private void startSocketListener() throws IOException
    {
        m_Logger.debug("Started Message Router server on port %s", m_ServerPort);

        m_ConnectionListener = new ConnectionListener();

        m_ConnectionThread = new Thread(m_ConnectionListener);
        m_ConnectionThread.setName("ConnectionThread");
        m_ConnectionThread.start();
    }
    
    /**
     * Listener that handles client connections.
     */
    private class ConnectionListener implements Runnable
    {
        @Override
        public void run()
        {
            m_Activated.set(true);
            try
            {
                m_ServerSocket = new ServerSocket(m_ServerPort);

                while (m_Activated.get() && !m_ServerSocket.isClosed())
                {            
                    final Socket clientSocket = m_ServerSocket.accept();
                    clientSocket.setSoTimeout(TIMEOUT_MS);
                    m_Logger.debug("Accepted connection from %s", clientSocket.getInetAddress());

                    final byte[] systemIdBytes = 
                            ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(m_System.getId()).array();
                    clientSocket.getOutputStream().write(systemIdBytes);

                    final Dictionary<String, Object> props = new Hashtable<>();
                    props.put(SocketListener.PROP_SOCKET, clientSocket);
                    
                    final ComponentInstance instance = m_SocketListenerFactory.newInstance(props);
                    final SocketListener socketListener = (SocketListener)instance.getInstance();
                    
                    m_Logger.debug("Storing socketListener for id [%s]", socketListener.getSystemId());
                    m_ClientMap.put(socketListener.getSystemId(), socketListener);
                    m_ComponentList.add(instance);
                    
                    final Thread thread = new Thread(socketListener);
                    thread.setName("SocketListener-client-systemId-" + socketListener.getSystemId());
                    thread.start();    
                }
            }
            catch (final IOException ex)
            {
                m_Logger.error(ex, "Error accepting connection from client.");
            }
            finally
            {
                try
                {
                    m_ServerSocket.close();
                }
                catch (final IOException ex)
                {
                    m_Logger.error(ex, SHUTDOWN_ERROR);
                }
            }
        }
        
        /**
         * Method that shuts down the connection listener.
         */
        public void shutdown()
        {
            m_Activated.set(false);
        }
    }
}
