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
package mil.dod.th.ose.time.service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.logging.Level;

import mil.dod.th.ose.time.service.logging.TimeChangeServiceLogger;
import mil.dod.th.ose.time.service.util.ChangeSystemTimeWrapperImpl;

/**
 * Class for entry point into starting the system time service. 
 * This class will handle starting and stopping of the service.
 * @author nickmarcucci
 *
 */
public final class TimeChangeServiceMain
{
    /**
     * The server port to listen on.
     */
    public static final int SERVER_PORT = 4444;
    
    /**
     *  Time service implementation.
     */
    private static TimeChangeServiceThread  m_ServiceThread;
    
    /**
     * Thread which runs the time service.
     */
    private static Thread m_Thread;
    
    /**
     * Private Constructor.
     */
    private TimeChangeServiceMain()
    {
        
    }
    
    /**
     * Main method to begin the service.
     * 
     * @param args
     *  the command line arguments passed in on invocation.
     */
    public static void main(final String[] args)
    {
        //see if there is an argument of where to log to.
        if (args.length == 1)
        {
            TimeChangeServiceLogger.setupLogger(args[0]);
        }
        else 
        {
            return;
        }
        
        //adds a callback so that on shutdown we can stop the running thread.
        Runtime.getRuntime().addShutdownHook(new Thread(new TimeServiceShutdownHook()));
        onStart();
    }
    
    /**
     * Function to begin server socket processing of time data.
     */
    private static void onStart()
    {
        ServerSocket serverSocket = null;
        
        try
        {
            serverSocket = new ServerSocket(SERVER_PORT);
        }
        catch (final IOException exception)
        {
            final String msg = String.format(
                    "An error occurred trying to open the server socket on port %d", SERVER_PORT);
            TimeChangeServiceLogger.logMessage(Level.SEVERE, msg, exception);
            return;
        }
        
        try
        {
            m_ServiceThread = new TimeChangeServiceThread(serverSocket, new ChangeSystemTimeWrapperImpl());
            m_Thread = new Thread(m_ServiceThread);
            m_Thread.start();
        }
        catch (final SocketException exception)
        {
            TimeChangeServiceLogger.logMessage(Level.SEVERE, 
                    "System Time Service could not be started.", exception);
        }
        
        
        TimeChangeServiceLogger.logMessage(Level.INFO, "System Time Service has started.");
    }
    
    /**
     * Class to provide shutdown hook when service is stopped.
     * Will halt reading of time data.
     * @author nickmarcucci
     *
     */
    private static class TimeServiceShutdownHook implements Runnable
    {
        @Override
        public void run()
        {
            TimeChangeServiceLogger.logMessage(Level.INFO, "System Time Service is stopping");
            
            final int timeout = 10000;
            if (m_Thread != null)
            {
                try
                {
                    m_Thread.join(timeout);
                }
                catch (final InterruptedException exception)
                {
                    TimeChangeServiceLogger.logMessage(Level.SEVERE, 
                            "Could not stop time service thread!", exception);
                }
            }
        }
    }
}
