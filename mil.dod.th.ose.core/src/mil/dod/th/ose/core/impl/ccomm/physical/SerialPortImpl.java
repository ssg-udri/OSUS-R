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
package mil.dod.th.ose.core.impl.ccomm.physical;

import java.util.Map;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.ccomm.physical.SerialPort;
import mil.dod.th.core.ccomm.physical.SerialPortAttributes;
import mil.dod.th.core.ccomm.physical.SerialPortProxy;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.types.ccomm.FlowControlEnum;
import mil.dod.th.core.types.ccomm.ParityEnum;
import mil.dod.th.core.types.ccomm.StopBitsEnum;

/**
 * Basic implementation of a serial port layer.
 * @author allenchl
 *
 */
@Component(factory = PhysicalLinkInternal.COMPONENT_SERIAL_PORT_FACTORY_REG_ID)
public class SerialPortImpl extends PhysicalLinkImpl implements SerialPort
{
    @Reference
    @Override
    public void setLoggingService(final LoggingService loggingService) 
    {
        super.setLoggingService(loggingService);
    }
    
    @Override
    public void setSerialPortProperties(final int baudRate, final int dataBits, final ParityEnum parity,
        final StopBitsEnum stopBits, final FlowControlEnum flowControl) throws PhysicalLinkException
    {
        // Populate dictionary with properties to be set (w/ enums converted to String)
        final Map<String, Object> properties = getProperties();
        properties.put(SerialPortAttributes.CONFIG_PROP_BAUD_RATE, baudRate);
        properties.put(SerialPortAttributes.CONFIG_PROP_DATA_BITS, dataBits);
        properties.put(SerialPortAttributes.CONFIG_PROP_PARITY, parity.toString());
        properties.put(SerialPortAttributes.CONFIG_PROP_STOP_BITS, stopBits.toString());
        properties.put(SerialPortAttributes.CONFIG_PROP_FLOW_CONTROL, flowControl.toString());
        
        try
        {
            setProperties(properties);
        }
        catch (final FactoryException ex)
        {
            throw new PhysicalLinkException("Error setting serial port properties", ex);
        }
    }

    @Override
    public void setDTR(final boolean high) throws IllegalStateException
    {
        ((SerialPortProxy)getProxy()).setDTR(high);
    }

    @Override
    public SerialPortAttributes getConfig()
    {
        return Configurable.createConfigurable(SerialPortAttributes.class, getProperties());
    }
}
