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
package edu.udayton.udri.asset.novatel.impl.connection;

import java.io.IOException;
import java.util.UUID;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.ccomm.physical.SerialPort;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;

import org.osgi.service.log.LogService;

import edu.udayton.udri.asset.novatel.NovatelConstants;
import edu.udayton.udri.asset.novatel.connection.NovatelConnectionMgr;

/**
 * Implementation of the {@link NovatelConnectionMgr} class.
 * 
 * @author cweisenborn
 */
@Component
public class NovatelConnectionMgrImpl implements NovatelConnectionMgr
{   
    /**
     * Timeout in milliseconds for the serial port while reading.
     */
    private static final int SERIAL_READ_TIMEOUT_MS = 1000;
    
    /** 
     * Service for acquiring physical link (serial port). 
     */
    private CustomCommsService m_CustomCommsService;

    /**
     * The serial port used to retrieve data from the unit.
     */
    private SerialPort m_SerialPort;
    
    /**
     * Buffered reader used to read NovAtel messages in from the serial port.
     */
    private LineReader m_Reader;
    
    /**
     * Save the last used value for the baud rate, will be used if reconnection is needed.
     */
    private Integer m_BaudRate;
    
    /**
     * Save the last used value for the port, will be used if reconnection is needed.
     */
    private String m_Port;
    
    /**
     * Used by OSGI to bind the CustomCommsService.
     * 
     * @param customCommsService
     *      CustomCommsService reference.
     */
    @Reference
    public void setCustomCommsService(final CustomCommsService customCommsService)
    {
        m_CustomCommsService = customCommsService;
    }
    
    /**
     * Bind to the factory that produces Serial ports.
     * 
     * @param factory
     *      serial port factory descriptor, argument required for OSGi binding method to define the service to bind
     */
    @Reference(target = PhysicalLink.FILTER_SERIAL_PORT)
    public void setSerialPortFactory(final FactoryDescriptor factory)
    {
        // do nothing
    }
    
    @Override // NOCHECKSTYLE: This logic is required until refactoring can be done
    public void startProcessing(final String physPort, final int baudRate) throws AssetException
    {
        if (isProcessing())
        {
            throw new IllegalStateException("Unable to connect to serial port because it is already connected.");
        }
        try
        {
            //attempt to get the serial port
            final UUID serialUuid = m_CustomCommsService.tryCreatePhysicalLink(
                    PhysicalLinkTypeEnum.SERIAL_PORT, physPort);
            m_SerialPort = (SerialPort)m_CustomCommsService.requestPhysicalLink(serialUuid);
        }
        catch (final CCommException exception)
        {
            // can't create link
            Logging.log(LogService.LOG_ERROR, "Cannot create Physical Link [%s]", physPort);
            throw new AssetException(exception);
        }
        catch (final IllegalStateException exception)
        {
            Logging.log(LogService.LOG_ERROR, "Physical Link [%s] is already in use.", physPort);
            throw new AssetException(exception);
        }
        catch (final IllegalArgumentException exception)
        {
            Logging.log(LogService.LOG_ERROR, exception, "Cannot create Physical Link");
            throw new AssetException(exception);
        }

        try
        {
            // setup serial port
            m_SerialPort.setSerialPortProperties(baudRate, 
                    NovatelConstants.DEFAULT_NOVATEL_DATA_BITS, 
                    NovatelConstants.DEFAULT_NOVATEL_PARITY, 
                    NovatelConstants.DEFAULT_NOVATEL_STOP_BITS, 
                    NovatelConstants.DEFAULT_NOVATEL_FLOW_CONTROL);
        }
        catch (final PhysicalLinkException exception)
        {
            Logging.log(LogService.LOG_ERROR, exception, "Unable to set Serial Port properties.");
            releaseCleanupSerialPort(false);
            throw new AssetException(exception);
        }
        try
        {
            m_SerialPort.setReadTimeout(SERIAL_READ_TIMEOUT_MS);
        }
        catch (final IllegalArgumentException exception)
        {
            Logging.log(LogService.LOG_ERROR, "Invalid read timeout for physical link: %s", m_SerialPort.getName());
            releaseCleanupSerialPort(false);
            throw new AssetException(exception);
        }
        catch (final FactoryException exception)
        {
            Logging.log(LogService.LOG_ERROR, "Unable to set the read timeout for the physical link: %s", 
                    m_SerialPort.getName());
            releaseCleanupSerialPort(false);
            throw new AssetException(exception);
        }
        try
        {
            m_SerialPort.open();
        }
        catch (final PhysicalLinkException exception)
        {
            Logging.log(LogService.LOG_ERROR, "Unable to open physical link: %s", m_SerialPort.getName());
            releaseCleanupSerialPort(true);
            throw new AssetException(exception);
        }
        
        try
        {
            m_Reader = new LineReader(m_SerialPort.getInputStream());
        }
        catch (final PhysicalLinkException exception)
        {
            Logging.log(LogService.LOG_ERROR, "Unable to retrieve input stream from physical link: %s", 
                    m_SerialPort.getName());
            releaseCleanupSerialPort(true);
            throw new AssetException(exception);
        }

        m_BaudRate = baudRate;
        m_Port = physPort;
    }

    @Override
    public void stopProcessing()
    {
        if (!isProcessing())
        {
            throw new IllegalStateException("Unable to disconnect from serial port because it was never connected.");
        }
        try
        {
            m_Reader.close();
        }
        catch (final IOException exception)
        {
            Logging.log(LogService.LOG_WARNING, exception, "Unable to close buffered reader for input stream from " 
                    + "physical link: %s", m_SerialPort.getName());
        }
        finally
        {
            releaseCleanupSerialPort(true);
        }
    }

    @Override
    public boolean isProcessing()
    {
        if (m_SerialPort != null && m_SerialPort.isOpen())
        {
            return true;
        }
        return false;
    }
    
    @Override
    public String readMessage() throws AssetException, IOException
    {
        if (m_Reader == null)
        {
            throw new AssetException("No buffered reader to currently read from.");
        }
        return m_Reader.readLine();
    }
    
    @Override 
    public void reconnect() throws AssetException, IOException
    {
        if (m_BaudRate == null || m_Port == null)
        {
            throw new IllegalStateException("The baud rate and port are not known.");
        }

        if (isProcessing())
        {
            stopProcessing();
        }
        if (m_SerialPort == null)
        {
            startProcessing(m_Port, m_BaudRate);
        }
    }
    
    /**
     * Release and cleanup SerialPort ownership.
     * @param doClose
     *      if <code> true </code> the port should be closed before releasing
     */
    private void releaseCleanupSerialPort(final boolean doClose)
    {
        if (doClose)
        {
            try
            {  
                m_SerialPort.close();
            }
            catch (final PhysicalLinkException e)
            {
                Logging.log(LogService.LOG_WARNING, 
                    "Unable to close the NovAtel physical link: %s", m_SerialPort.getName());
            }
        }

        try
        {
            m_SerialPort.release();
        }
        catch (final IllegalStateException e)
        {
            Logging.log(LogService.LOG_WARNING, e, "Unable to release the NovAtel PhysicalLink.");
        }

        m_SerialPort = null; //NOPMD: explicit null set, set to null to fully release ownership
    }
}
