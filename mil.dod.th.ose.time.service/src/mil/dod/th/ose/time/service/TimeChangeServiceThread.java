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
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.logging.Level;

import mil.dod.th.ose.time.service.logging.TimeChangeServiceLogger;
import mil.dod.th.ose.time.service.util.ChangeSystemTimeWrapper;

/**
 * Service which listens on a port and reads in a long number representing
 * the time in milliseconds and sets the system time to that time.
 * 
 * @author nickmarcucci
 */
public class TimeChangeServiceThread implements Runnable
{
    /**
     * Value returned when a client socket read times out.
     */
    private static final int SOCKET_READ_TO_VAL = -2;
    
    /**
     * Socket timeout specified in milliseconds.
     */
    private static final int SOCKET_TIMEOUT = 10000;
    
    /**
     * Value to indicate that the client is just checking to see if connection still
     * exists.
     */
    private static final long HEARTBEAT_SIGNAL = -22L;
    
    /**
     * The server socket used to listen/accept client connections.
     */
    private final ServerSocket m_ServerSocket;
    
    /**
     * Interface to set the system time.
     */
    private final ChangeSystemTimeWrapper m_SysTimeInterface;
    
    /**
     * Indicates that thread should continue processing.
     */
    private boolean m_Run;
    
    /**
     * Constructor.
     * @param socket
     *  the server socket to be used to listen for connections.
     * @param sysInterface
     *  the interface to set the system time.
     * @throws SocketException 
     *  if the socket timeout for accept cannot be set.
     */
    public TimeChangeServiceThread(final ServerSocket socket, 
            final ChangeSystemTimeWrapper sysInterface) throws SocketException
    {
        m_Run = true;
        m_ServerSocket = socket;
        m_ServerSocket.setSoTimeout(SOCKET_TIMEOUT);
        
        m_SysTimeInterface = sysInterface;
    }
    
    @Override
    public void run()
    {
        boolean madeConnection = false;
        
        while (m_Run)
        {
            if (!madeConnection)
            {
                try (final InputStream clientInputStream = makeConnection())
                {
                    if (clientInputStream != null)
                    {
                        madeConnection = true;
                    }
                    while (m_Run && madeConnection)
                    {
                        try
                        {
                            final long timeToSet = readData(clientInputStream);
                            
                            //read was closed so no connection
                            if (timeToSet == -1)
                            {
                                madeConnection = false;
                            }
                            else
                            {
                                //make sure there wasn't a socket timeout.
                                if (timeToSet != SOCKET_READ_TO_VAL && timeToSet != HEARTBEAT_SIGNAL)
                                {
                                    m_SysTimeInterface.setSystemTime(timeToSet);
                                }
                                else
                                {
                                    madeConnection = false;
                                }
                            }
                        }
                        catch (final IOException exception)
                        {
                            TimeChangeServiceLogger.logMessage(Level.WARNING, 
                                    "Could not read from client socket connection.", exception);
                            madeConnection = false;
                        }
                    }
                }
                catch (final IOException exception)
                {
                    TimeChangeServiceLogger.logMessage(Level.WARNING, 
                            "Could not read the client socket connection.", exception);
                    madeConnection = false;
                }
            }
        }
    }
    
    /**
     * Stops execution of the running thread.
     */
    public void stopRun()
    {
        m_Run = false;
        
        try
        {
            m_ServerSocket.close();
        }
        catch (final IOException exception)
        {
            TimeChangeServiceLogger.logMessage(Level.SEVERE, 
                    "Could not close server socket due to exception.", exception);
        }
    }
    
    /**
     * Reads a <code>long</code> number from a given input stream.
     * @param stream
     *  the input stream to read the long number from
     * @return
     *  the <code>long</code> number if read was successful; -1 if EOF found;
     *  -2 if timeout has occurred; HEARTBEAT (-22) for heartbeat messages
     * @throws IOException
     *  if there is an exception while trying to read from the 
     *  input stream.
     */
    private long readData(final InputStream stream) throws IOException
    {
        //size of the long number that we are expecting to read from the stream.
        final int expectedBytesToRead = 8;
        
        final byte[] rcvBuffer = new byte[expectedBytesToRead];
        int bytesRemaining = expectedBytesToRead;
        while (bytesRemaining > 0)
        {
            try
            {
                final int bytesRead = stream.read(rcvBuffer, 
                        expectedBytesToRead - bytesRemaining, bytesRemaining);
                
                if (bytesRead == -1)
                {
                    return -1;
                }
                
                bytesRemaining -= bytesRead;
            }
            catch (final SocketException exception)
            {
                //don't want to log anything here. A timeout is expected.
                return SOCKET_READ_TO_VAL;
            }
        }
        
        final ByteBuffer buffer = ByteBuffer.wrap(rcvBuffer);
        
        return buffer.getLong();
    }
    /**
     * Function to retrieve a client connection.
     * @return
     *  the input stream that connected or null if 
     *  there was an error trying to connect.
     * @throws IOException
     *  if there is an exception while trying to connect to the 
     *  input stream.
     */
    private InputStream makeConnection() throws IOException
    {
        InputStream clientInputStream = null;
        try (final Socket clientSocket = getClientSocket())
        {
            if (clientSocket != null)
            {
                clientInputStream = getClientSocketInputStream(clientSocket);
            }
        }
        catch (final IOException exception)
        {
            TimeChangeServiceLogger.logMessage(Level.SEVERE, 
                    "An error occurred while listening for the client connection. "
                            + "Connection might have timed out.", exception);
        }
        return clientInputStream;
    }
    
    /**
     * Function to retrieve a client socket connection.
     * @return
     *  the client socket that connected or null if 
     *  there was an error trying to connect.
     */
    private Socket getClientSocket()
    {
        Socket clientSocket = null;
        
        try
        {
            clientSocket = m_ServerSocket.accept();
            clientSocket.setSoTimeout(SOCKET_TIMEOUT);
        }
        catch (final SocketException exception)
        {   //NOCHECKSTYLE: Avoid empty catch blocks.
            //Accept might timeout from time to time waiting for a client.
        }
        catch (final IOException exception)
        {
            TimeChangeServiceLogger.logMessage(Level.SEVERE, 
                    "An error occurred while listening for a client connection. "
                            + "Connection may have timed out.", exception);
        }
        
        return clientSocket;
    }
    
    /**
     * Function to retrieve the input stream on a client socket.
     * @param client
     *  the client socket on which the input stream is to be 
     *  retrieved from.
     * @return
     *  the input stream if successful; null otherwise (client socket will also be closed);
     */
    private InputStream getClientSocketInputStream(final Socket client)
    {
        InputStream inputStream = null;
        
        try
        {
            inputStream = client.getInputStream();
        }
        catch (final IOException exception)
        {
            TimeChangeServiceLogger.logMessage(Level.SEVERE, 
                    "An error occured trying to retrieve a client's input stream", exception);
        }

        return inputStream;
    }
    
}
