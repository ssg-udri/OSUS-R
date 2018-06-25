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
package edu.udayton.udri.asset.novatel.impl.timechanger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.types.ComponentType;
import mil.dod.th.core.types.ComponentTypeEnum;
import mil.dod.th.core.types.status.ComponentStatus;
import mil.dod.th.core.types.status.OperatingStatus;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.ose.utils.ClientSocketFactory;

import org.osgi.service.log.LogService;

import edu.udayton.udri.asset.novatel.StatusHandler;
import edu.udayton.udri.asset.novatel.timechanger.TimeChange;

/**
 * Implementation of the {@link TimeChange} interface.
 * @author nickmarcucci
 *
 */
@Component (servicefactory = true)
public class TimeChangeImpl implements TimeChange
{
    /**
     * Description that is used in describing the component type.
     */
    private static final String COMPONENT_DESCRIPTION = "Time Service";
    
    /**
     * Component type identifier for item that implements this interface.
     */
    private static final ComponentTypeEnum COMPONENT_TYPE_IDENTIFIER = ComponentTypeEnum.SOFTWARE_UNIT;
    
    /**
     * Off status description.
     */
    private static final String OFF_DESCRIPTION = "Time service is now off.";
    
    /**
     * The most recent status for this component.
     */
    private ComponentStatus m_Status = getComponentStatus(SummaryStatusEnum.OFF, OFF_DESCRIPTION);
    
    /**
     * The {@link ClientSocketFactory} used to retrieve a client socket.
     */
    private ClientSocketFactory m_ClientSocketFactory; 
    
    /**
     * Flag to denote if time service code should be running.
     */
    private boolean m_Run;
    
    /**
     * Thread that runs the time service.
     */
    private Thread m_TimeThread;
    
    /**
     * The service that performs the sending of system times to the time service.
     */
    private TimeServiceThread m_TimeService;
    
    /**
     * Blocking queue to hold the next time message to be synced to.
     */
    private final BlockingQueue<Long> m_BlockingQueue = new ArrayBlockingQueue<Long>(1);
    
    /**
     * The {@link StatusHandler} which handles changes of the operation of this service.
     */
    private StatusHandler m_StatusHandler;
    
    /**
     * Set the {@link ClientSocketFactory} to use.
     * @param factory
     *  the factory that is to be used.
     */
    @Reference
    public void setClientSocketFactory(final ClientSocketFactory factory)
    {
        m_ClientSocketFactory = factory;
    }
    
    @Override
    public synchronized void changeTime(final long milliseconds) throws AssetException
    {
        if (!m_Run)
        {
            throw new AssetException("Must call connectTimeService before calling this method.");
        }
        
        //Calculate the difference in the time of the system and the one given to the 
        //function
        final long difference = Math.abs(System.currentTimeMillis() - milliseconds);
        final int timeDiffLimit = 1000;
      
        if (difference >= timeDiffLimit)
        {
            Logging.log(LogService.LOG_INFO, 
                    "There was a difference of %d ms between the system and GPS time.", difference);
            
            //remove if there is something in the queue.
            m_BlockingQueue.poll();
            
            m_BlockingQueue.add(milliseconds);
        }
    }

    @Override
    public void connectTimeService(final StatusHandler handler, final int port)
    {
        if (m_Run)
        {
            throw new IllegalStateException("The time service has already been started.");
        }
        
        m_Run = true;
        m_StatusHandler = handler;
       
        m_TimeService = new TimeServiceThread(port);
        
        m_TimeThread = new Thread(m_TimeService);
        m_TimeThread.start();
    }

    @Override
    public void disconnectTimeService() throws InterruptedException, IllegalStateException, IOException
    {
        if (m_Run)
        {
            m_Run = false;
            m_TimeService.disconnect();
        
            m_TimeThread.interrupt();
            final int timeout = 5000;
            m_TimeThread.join(timeout);
            
            if (m_TimeThread.isAlive())
            {
                Logging.log(LogService.LOG_WARNING, 
                        "Time service thread did not shutdown properly after join timeout.");
            }
            m_StatusHandler.handleStatusUpdate(getComponentStatus(SummaryStatusEnum.OFF, OFF_DESCRIPTION));
        }
        else
        {
            throw new IllegalStateException("The time service is not currently running.");
        }
    }
    
    @Override
    public ComponentStatus getComponentStatus()
    {
        return m_Status;
    }
    
    /**
     * Create a component status representing this component.
     * @param status
     *      the status to set for this component
     * @param description
     *      descriptive string of the status
     * @return
     *      a complete component status
     */
    private ComponentStatus getComponentStatus(final SummaryStatusEnum status, final String description)
    {
        final ComponentStatus timeStatus = new ComponentStatus();
        final ComponentType timeCompType = new ComponentType();
        timeCompType.setDescription(COMPONENT_DESCRIPTION);
        timeCompType.setType(COMPONENT_TYPE_IDENTIFIER);
        timeStatus.setComponent(timeCompType);
        final OperatingStatus timeOpStatus = new OperatingStatus();
        timeOpStatus.setSummary(status);
        timeOpStatus.setDescription(description);
        timeStatus.setStatus(timeOpStatus);
        
        m_Status = timeStatus;
        return timeStatus;
    }
    
    /**
     * Class performs the connecting and disconnecting of the client socket to communicate with the time service.
     * This class will attempt to reconnect to the time service if any connection errors occur. Once a connection is 
     * made, the class will attempt to send the given time over the connected socket.
     * @author nickmarcucci
     *
     */
    private class TimeServiceThread implements Runnable
    {
        /**
         * String to identify the host IP address. Will always be local host.
         */
        private static final String HOST = "127.0.0.1"; //NOPMD: Avoid hard coded IP; it will always be localhost
        
        /**
         * Heartbeat signal value which is used to indicate that the connection to the time service is still alive.
         */
        private static final long HEARTBEAT_SIGNAL = -22L;
        
        /**
         * Sleep timeout limit.
         */
        private static final int FAILED_CONNECTION_SLEEP = 3000;
        
        /**
         * The client socket created from the {@link ClientSocketFactory}.
         */
        private Socket m_Socket;
        
        /**
         * The output stream from the created socket.
         */
        private OutputStream m_OutputStream; 
        
        /**
         * The time service's port over which it is listening for the time messages.
         */
        private final int m_Port;
        
        /**
         * Indicates whether a connection to the time service has been made.
         */
        private boolean m_IsConnected;
        
        /**
         * The total number of times that this thread has attempted to indicate to the 
         * time service that it should change the system time.
         */
        private int m_AttemptsToChangeTime;
        
        /**
         * Constructor.
         * @param port
         *  the port over which this thread is to communicate
         */
        TimeServiceThread(final int port)
        {
            m_IsConnected = false;
            m_Port = port;
            m_AttemptsToChangeTime = 0;
        }
        
        @Override
        public void run()
        {
            while (m_Run)
            {
                if (m_IsConnected)
                {
                    try
                    {
                        final Long time = m_BlockingQueue.poll(5, TimeUnit.SECONDS);
                        if (time == null)
                        {
                            //send a heartbeat message if timeout to check that connection to 
                            //time service is still alive.
                            sendData(HEARTBEAT_SIGNAL);
                        }
                        else 
                        {
                            changeTime(time);
                        }
                       
                    }
                    catch (final InterruptedException exception)
                    {
                        Logging.log(LogService.LOG_DEBUG, "Thread was interrupted while polling " 
                                + "for the next time to set.", exception);
                        
                    }
                    catch (final IOException exception)
                    {
                        final String errorMsg = "An error has occurred trying to send " 
                                + "the desired time to the system time service.";
                        Logging.log(LogService.LOG_ERROR, errorMsg, exception);
                        
                        m_IsConnected = false;
                        
                        m_StatusHandler.handleStatusUpdate(
                                getComponentStatus(SummaryStatusEnum.BAD, exception.getMessage()));
                    }
                }
                else
                {
                    m_IsConnected = connect();
                    
                    if (m_IsConnected)
                    {
                        m_StatusHandler.handleStatusUpdate(
                                getComponentStatus(SummaryStatusEnum.GOOD, "Time service connected."));
                    }
                    else
                    {
                        try
                        {
                            Thread.sleep(FAILED_CONNECTION_SLEEP);
                        }
                        catch (final InterruptedException exception)
                        {
                            Logging.log(LogService.LOG_DEBUG, "Time service thread was " 
                                    + "interrupted while sleeping after a failed connection.", exception);
                        }
                    }
                }
            }
        }
        
        /**
         * Function to connect to the time service over the specified port. 
         * @return
         *  true if the connection was successful. false otherwise.
         */
        private boolean connect()
        {
            try
            {
                m_Socket = m_ClientSocketFactory.createClientSocket(HOST, m_Port);
                m_OutputStream = m_Socket.getOutputStream();
            }
            catch (final IOException exception)
            {
                final String errorMsg = "An error occurred trying to create a client "
                        + "socket when trying to connect to the time service.";
                
                Logging.log(LogService.LOG_ERROR, exception, errorMsg);
                
                
                m_StatusHandler.handleStatusUpdate(getComponentStatus(SummaryStatusEnum.BAD, exception.getMessage()));
                return false;
            }
            
            return true;
        }
        
        /**
         * Function to discontinue processing on this thread.
         * @throws IOException
         *  if an error occurs trying to close the socket.
         */
        public void disconnect() throws IOException
        {
            m_Run = false;
            
            if (m_OutputStream != null)
            {
                m_OutputStream.close();
            }
            
            if (m_Socket != null)
            {
                m_Socket.close();
            }
        }
        
        /**
         * Sends the given time in milliseconds over the currently connected socket to the time 
         * service if there is a difference of at least one second between the current system time 
         * and the given time. 
         * 
         * @param milliseconds
         *  the time in milliseconds that the system should be set to.
         *  
         * @throws IOException
         *  if the output stream used to write the time is null or if an error occurs during the 
         *  write call.
         */
        private void changeTime(final long milliseconds) throws IOException
        {
            sendData(milliseconds);
            
            m_AttemptsToChangeTime++;
            final Date date = new Date(milliseconds);
            Logging.log(LogService.LOG_INFO, "Attempting to change system time for the %d time. " 
                    + "Time sent is %s", m_AttemptsToChangeTime, date.toString());
            
        }
        
        /**
         * Function to send data over the known output stream.
         * @param data
         *  the data to send over the known output stream
         * @throws IOException
         *  if the output stream used to write the time is null or if an error occurs during the
         *  write call.
         */
        private void sendData(final long data) throws IOException
        {
            if (m_OutputStream == null)
            {
                Logging.log(LogService.LOG_ERROR, 
                        "Outputstream to write to time service is null. Cannot write desired time to service.");
                throw new IOException();
            }
            
            //allocate 8 bytes for long variable.
            final ByteBuffer byteBuffer = ByteBuffer.allocate(8);
            byteBuffer.putLong(data);
          
            m_OutputStream.write(byteBuffer.array());
        }
    }
}
