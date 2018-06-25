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
package edu.udayton.udri.asset.novatel.impl.message;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.log.Logging;
import mil.dod.th.core.types.status.SummaryStatusEnum;

import org.osgi.service.log.LogService;

import edu.udayton.udri.asset.novatel.connection.NovatelConnectionMgr;
import edu.udayton.udri.asset.novatel.message.MessageReader;
import edu.udayton.udri.asset.novatel.message.MessageReceiver;

/**
 * Implementation for the {@link MessageReader}.
 * 
 * @author allenchl
 */
@Component
public class MessageReaderImpl implements MessageReader
{
    /**
     * The connection manager which holds the data stream.
     */
    private NovatelConnectionMgr m_ConnectionManager;
    
    /**
     * The message receiver which handles messages from the data stream.
     */
    private MessageReceiver m_Receiver;
    
    /**
     * The thread that is reading from the data stream.
     */
    private Thread m_Reader;
    
    /**
     * Flag that denotes if the reader should be reading.
     */
    private boolean m_DoRead;

    /**
     * Bind the connection manager service.
     * @param manager
     *      the connection manager service to use
     */
    @Reference
    public void setNovatelConnectionMgr(final NovatelConnectionMgr manager)
    {
        m_ConnectionManager = manager;
    }
    
    @Override
    public void startRetreiving(final MessageReceiver receiver)
    {
        if (m_DoRead)
        {
            throw new IllegalStateException("Unable to start reading, because message reading is already happening.");
        }
        m_Receiver = receiver;
        m_DoRead = true;
        m_Reader = new Thread(new ReadThread());
        m_Reader.start();
    }

    @Override
    public void stopRetrieving()
    {
        if (!m_DoRead)
        {
            throw new IllegalStateException("Unable to stop reading, because message reading was never started.");
        }
        m_DoRead = false;
        final int timeout = 1000;
        m_Reader.interrupt(); //interrupt first
        try
        {
            m_Reader.join(timeout);
        }
        catch (final InterruptedException e)
        {
            Logging.log(LogService.LOG_WARNING, "Data message reading thread was interrupted during join.");
        }
        if (m_Reader.isAlive())
        {
            Logging.log(LogService.LOG_ERROR, "The reader thread did not completely stop.");
        }
    }
    
    /**
     * This class is the thread which reads from the data source.
     */
    private class ReadThread implements Runnable
    {
        @Override
        public void run()
        {
            //denotes if there was an error during an iteration
            boolean errored = false;
            while (m_DoRead)
            {
                //we want to be able to handle all exceptions so the thread doesn't die
                try
                {
                    //if there was an error last iteration try to reconnect
                    if (errored)
                    {
                        // wait before trying to reconnect so we don't overwhelm the asset with status changes
                        final int waitReconnectTimeout = 1200;
                        Thread.sleep(waitReconnectTimeout);
                        //try to reconnect, if fails, will throw exception
                        m_ConnectionManager.reconnect();
                        errored = false;
                    }
                    else
                    {
                        final String dataString = m_ConnectionManager.readMessage();
                        // null means there was no data
                        if (dataString != null)
                        {
                            m_Receiver.handleDataString(dataString);
                        }
                    }
                }
                // try to be able to recover no matter the exception case
                catch (final Exception e)
                {
                    errored = true;
                    m_Receiver.handleReadError(SummaryStatusEnum.BAD, e.getMessage());

                    Logging.log(LogService.LOG_WARNING, e,
                            "The data message reading thread has experienced an exception.");
                }
            }
        }
    }
}
