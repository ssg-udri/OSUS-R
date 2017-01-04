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
package edu.udayton.udri.asset.gps;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.ccomm.physical.SerialPort;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.core.types.ccomm.FlowControlEnum;
import mil.dod.th.core.types.ccomm.ParityEnum;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.core.types.ccomm.StopBitsEnum;
import mil.dod.th.core.types.status.ComponentStatus;
import mil.dod.th.core.types.status.OperatingStatus;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.ose.shared.pm.CountingWakeLock;
import mil.dod.th.ose.shared.pm.CountingWakeLock.CountingWakeLockHandle;

import org.osgi.service.log.LogService;

/**
 * GPS SDK Plug-in implementation.
 * 
 * @author Tim Dale
 */
@Component(factory = Asset.FACTORY)  //NOCHECKSTYLE: Class data abstract coupling is required
public class GPSAsset implements AssetProxy
{   
    /**
     * Timeout in milliseconds for the serial port while reading before reader gives up.
     */
    private static final int SERIAL_READ_TIMEOUT_MS = 1000;
    
    /**
     * Context member of the Asset.
     */
    private AssetContext m_Context; 
    
    /**
     * Serial port to read from.
     */
    private SerialPort m_SerialPort;
    
    /**
     * Rate of transmissions. 
     */
    private int m_BaudRate;
    
    /**
     * Number of bits per transmission.
     */
    private int m_DataBits;
    
    /**
     * Name of the physical link. 
     */
    private String m_PhysicalLinkName;
    
    /**
     * Service for getting the {@link SerialPort}.
     */
    private CustomCommsService m_CustomCommsService;
    
    /**
     * PhysicalLink UUID.
     */
    private UUID m_UUID;
    
    /**
     * Reference to the counting {@link WakeLock} used by this asset.
     */
    private CountingWakeLock m_CountingLock = new CountingWakeLock();
    
    /**
     * Bind the custom comms service.
     * 
     * @param commsService
     *      service to bind
     */
    @Reference
    public void setCustomCommsService(final CustomCommsService commsService)
    {
        m_CustomCommsService = commsService;
    }
    
    @Override
    public void initialize(final AssetContext context, final Map<String, Object> props) throws FactoryException
    {
        final GPSAssetAttributes gpsAtrb = Configurable.createConfigurable(GPSAssetAttributes.class, props);
        m_BaudRate = gpsAtrb.baudRate();
        m_DataBits = gpsAtrb.dataBits();
        m_PhysicalLinkName = gpsAtrb.physLinkName();

        m_Context = context;
        m_CountingLock.setWakeLock(m_Context.createPowerManagerWakeLock(getClass().getSimpleName() + "WakeLock"));
        m_Context.setStatus(SummaryStatusEnum.GOOD, "Initialized");
    }
    
    /**
     * OSGi deactivate method used to delete any wake locks used by the asset.
     */
    @Deactivate
    public void tearDown()
    {
        m_CountingLock.deleteWakeLock();
    }

    @Override
    public void updated(final Map<String, Object> props)
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            final GPSAssetAttributes gpsAtrb = Configurable.createConfigurable(GPSAssetAttributes.class, props);
            m_BaudRate = gpsAtrb.baudRate();
            m_DataBits = gpsAtrb.dataBits();
            m_PhysicalLinkName = gpsAtrb.physLinkName();
    
            if (m_SerialPort != null && !m_SerialPort.isOpen())
            {
                try
                {
                    m_SerialPort.setSerialPortProperties(
                            m_BaudRate, 
                            m_DataBits, 
                            ParityEnum.NONE, 
                            StopBitsEnum.ONE_STOP_BIT, 
                            FlowControlEnum.NONE);
                }
                catch (final PhysicalLinkException e)
                {
                    Logging.log(LogService.LOG_ERROR, e,
                            "PhysicalLinkException while updating the connection of GPS asset [%s].",
                            m_Context.getName());
                } 
            } 
            else if (m_SerialPort != null && m_SerialPort.isOpen())
            {
                //refresh the connection and update the properties
                try
                {
                    m_SerialPort.close();
                    m_SerialPort.setSerialPortProperties(
                            m_BaudRate,
                            m_DataBits,
                            ParityEnum.NONE,
                            StopBitsEnum.ONE_STOP_BIT, 
                            FlowControlEnum.NONE);
                    m_SerialPort.open();
                }
                
                catch (final PhysicalLinkException | IllegalArgumentException | IllegalStateException e)
                {
                    Logging.log(LogService.LOG_ERROR, e,
                            "Exception while refreshing the connection of GPS asset [%s].",
                            m_Context.getName());
                }
            }
        }
    }
    
    @Override
    public void onActivate() throws AssetException
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            //Activate the comms port connection.
            try
            {
                m_UUID = m_CustomCommsService.tryCreatePhysicalLink(
                        PhysicalLinkTypeEnum.SERIAL_PORT,
                        m_PhysicalLinkName);
                m_SerialPort = (SerialPort)m_CustomCommsService.requestPhysicalLink(m_UUID);
                m_SerialPort.setSerialPortProperties(
                        m_BaudRate,
                        m_DataBits,
                        ParityEnum.NONE,
                        StopBitsEnum.ONE_STOP_BIT, 
                        FlowControlEnum.NONE);
                
                //Prevents RXTX read from timing out.
                m_SerialPort.setReadTimeout(SERIAL_READ_TIMEOUT_MS);
                
                m_SerialPort.open();
                
                final Thread gpsThread = new Thread(new GpsObservationThread(m_SerialPort, m_Context));
                gpsThread.start();
            }
            catch (final CCommException | IllegalArgumentException | PhysicalLinkException e)
            {
                Logging.log(LogService.LOG_ERROR, e,
                        "Exception opening the Serial Port connection for asset [%s].",
                        m_Context.getName());
                
                Logging.log(LogService.LOG_INFO, "GPS asset deactivated"); //NOCHECKSTYLE: repeated text is intentional 
                m_Context.setStatus(new Status().withSummaryStatus(
                        new OperatingStatus(SummaryStatusEnum.BAD, "Asset Deactivated due to connection exception.")));
                
                return;
            }
            catch (final FactoryException e)
            {
                Logging.log(LogService.LOG_ERROR, e,
                        "FactoryException setting the timeout for asset [%s].",
                        m_Context.getName());
                Logging.log(LogService.LOG_INFO, "GPS asset deactivated");
                m_Context.setStatus(new Status().withSummaryStatus(
                        new OperatingStatus(SummaryStatusEnum.BAD,
                                "Asset Deactivated due to exception leading to time out failing to be set.")));
                return;
            }

            //Set the asset status.
            Logging.log(LogService.LOG_INFO, "GPS asset activated");
            m_Context.setStatus(new Status().withSummaryStatus(
                    new OperatingStatus(SummaryStatusEnum.GOOD, "Asset Activated")));
        }
        catch (final ValidationFailedException e)
        {
            Logging.log(LogService.LOG_ERROR, e,
                    "ValidationFailedException reading from the Serial Port connection for asset [%s].",
                    m_Context.getName());
        }
    }
    
    @Override
    public void onDeactivate() throws AssetException
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            if (m_SerialPort != null)
            {
                Logging.log(LogService.LOG_DEBUG, "Closing Serial Port"); 
                try
                {
                    m_SerialPort.close();
                    m_SerialPort.release();
                }
                catch (final PhysicalLinkException e)
                {
                    Logging.log(LogService.LOG_ERROR, e,
                            "PhysicalLinkException while deactivating GPS asset [%s].",
                            m_Context.getName());
                }
                catch (final IllegalArgumentException e)
                {
                    Logging.log(LogService.LOG_ERROR, e,
                            "IllegalArgumentException while deactivating GPS asset [%s].",
                            m_Context.getName());
                }
                catch (final IllegalStateException e)
                {
                    Logging.log(LogService.LOG_ERROR, e,
                            "IllegalStateException while deactivating GPS asset [%s].",
                            m_Context.getName());
                }
                catch (final NullPointerException e)
                {
                    Logging.log(LogService.LOG_ERROR, e,
                            "NullPointerException while deactivating GPS asset [%s].",
                            m_Context.getName());
                }
            }
        }
        
        Logging.log(LogService.LOG_INFO, "GPS Plug-in deactivated");
        m_Context.setStatus(SummaryStatusEnum.OFF, "Asset Deactivated");
    }
    
    @Override
    public Observation onCaptureData()
    {
        throw new UnsupportedOperationException(String.format("Asset [%s] does not support capturing data.", 
                m_Context.getName()));
    }
    
    @Override
    public Observation onCaptureData(final String sensorId)
    {
        throw new UnsupportedOperationException(String.format("Asset [%s] does not support capturing data by sensorId.",
                m_Context.getName()));
    }
    
    @Override
    public Status onPerformBit()
    {
        Logging.log(LogService.LOG_INFO, "Performing Plug-in BIT");
        
        if (m_SerialPort == null || !m_SerialPort.isOpen())
        {
            return new Status().withComponentStatuses(new ComponentStatus())
                    .withSummaryStatus(new OperatingStatus(SummaryStatusEnum.BAD, "BIT Failed"));
        }
        
        return new Status().withComponentStatuses(new ComponentStatus())
                    .withSummaryStatus(new OperatingStatus(SummaryStatusEnum.GOOD, "BIT Passed"));
    }
    
    @Override
    public Response onExecuteCommand(final Command command) throws CommandExecutionException, InterruptedException
    {
        throw new UnsupportedOperationException("onExecuteCommand() is not supported");
    }
    
    @Override
    public Set<Extension<?>> getExtensions()
    {
        return new HashSet<Extension<?>>();
    }
}
