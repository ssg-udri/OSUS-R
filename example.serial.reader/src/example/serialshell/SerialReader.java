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
package example.serialshell;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.ccomm.physical.SerialPort;
import mil.dod.th.core.types.ccomm.FlowControlEnum;
import mil.dod.th.core.types.ccomm.ParityEnum;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.core.types.ccomm.StopBitsEnum;

/**
 * Just an example component that reads from a serial port and prints out the data to the console.
 * 
 * @author dhumeniuk
 *
 */
@Component
public class SerialReader implements Runnable
{
    /**
     * Serial port to read from.
     */
    private SerialPort m_SerialPort;
    
    /**
     * Thread that will read from the port.
     */
    private Thread m_ReaderThread;
    
    /**
     * Whether the thread should be running.
     */
    private boolean m_Running;
    
    /**
     * Service for getting the {@link SerialPort}.
     */
    private CustomCommsService m_CustomCommsService;

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
    
    /**
     * Activate the component by opening a serial port and starting a thread to listen to the port.
     * 
     * @throws Exception
     *      if any exception happens during activation
     */
    @Activate
    public void activate() throws Exception
    {
        final UUID uuid = m_CustomCommsService.tryCreatePhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, "COM1");
        m_SerialPort = (SerialPort)m_CustomCommsService.requestPhysicalLink(uuid);
        final int baudRate = 115200;
        final int dataBits = 8;
        m_SerialPort.setSerialPortProperties(baudRate, dataBits, ParityEnum.NONE, StopBitsEnum.ONE_STOP_BIT, 
                FlowControlEnum.NONE);
        m_SerialPort.open();
        
        m_ReaderThread = new Thread(this);
        m_ReaderThread.start();
    }

    /**
     * Deactivate the component by closing/releasing the serial port and stopping the thread.
     * 
     * @throws Exception
     *      if any exception happens during deactivation
     */
    @Deactivate
    public void deactivate() throws Exception
    {
        m_Running = false;
        m_SerialPort.close();
        m_SerialPort.release();
    }

    @Override
    public void run()
    {
        System.out.println("Started serial shell listening thread on " + m_SerialPort.getName());
        m_Running = true;
        
        try
        {
            while (m_Running)
            {
                final InputStream is = m_SerialPort.getInputStream();
                final int c = is.read();
                System.out.format("%c.", c);
            }
        }
        catch (final PhysicalLinkException | IOException e)
        {
            e.printStackTrace();
        }
        
        System.out.println("Finishing serial shell listening thread");
    }
}
