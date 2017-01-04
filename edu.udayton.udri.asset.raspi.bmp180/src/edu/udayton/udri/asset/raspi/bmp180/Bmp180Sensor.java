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
package edu.udayton.udri.asset.raspi.bmp180;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.metatype.Configurable;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.observation.types.Weather;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.core.types.PressureMillibars;
import mil.dod.th.core.types.TemperatureCelsius;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.ose.shared.pm.CountingWakeLock;
import mil.dod.th.ose.shared.pm.CountingWakeLock.CountingWakeLockHandle;

/**
 * BMP180 Temperature/Pressure Sensor Plug-in.
 * 
 * @author Nathaniel Rosenwald
 */
@Component(factory = Asset.FACTORY) // NOCHECKSTYLE: Class Data Abstraction Coupling is required
public class Bmp180Sensor implements AssetProxy
{ 
    private static final int EEPROM_START = 0xAA;
    private static final int I2C_ADDRESS = 0x77;
    private static final int CONTROL_REGISTER = 0xF4;
    private static final byte MSB_ADDR = (byte) 0xF6;
    private static final byte GET_TEMP_CMD = (byte) 0x2E;
    private static final int PRESSURE_WRITE_START = 0x34;
    
    private static final int FIVE_MILLISECONDS = 5;
    private static final int EIGHT_MILLISECONDS = 8;
    private static final int FOURTEEN_MILLISECONDS = 14;
    private static final int TWENTY_SIX_MILLISECONDS = 26;

    private static final int CALIBRATION_BYTES = 22;
    private static final int CALIBRATION_SIZE = 10;
    private static final int CALIBRATION_ARRAY_AC4 = 3;
    private static final int CALIBRATION_ARRAY_AC6 = 5;
    private static final int CALIBRATION_ARRAY_MB = 8;
    private static final int PRESSURE_DATA_SIZE = 3;
    
    private static final int SIXTEEN_BIT_SHIFT = 16;
    private static final int EIGHT_BIT_SHIFT = 8;
    private static final int SIX_BIT_SHIFT = 6;

    private int[] m_CalibrationData;

    private double m_Altitude;
    private short m_SamplingSetting;
    private short m_SleepTime;
    
    private I2CDevice m_Bmp180;
    private I2CBus m_Bus;
    
    private AssetContext m_Context;

    /**
     * Reference to the counting {@link WakeLock} used by this asset.
     */
    private CountingWakeLock m_CountingLock = new CountingWakeLock();

    @Override
    public void initialize(final AssetContext context, final Map<String, Object> props) throws FactoryException
    {
        m_Context = context;
        m_CountingLock.setWakeLock(m_Context.createPowerManagerWakeLock(getClass().getSimpleName() + "WakeLock"));

        final Bmp180SensorAttributes config = Configurable.createConfigurable(Bmp180SensorAttributes.class, props);
        m_SamplingSetting = config.samplingSetting();
        m_Altitude = config.altitude();
        
        switch(m_SamplingSetting)
        {
            case 0:     
                m_SleepTime = FIVE_MILLISECONDS;
                break;
            case 1:     
                m_SleepTime = EIGHT_MILLISECONDS;
                break;
            case 2:     
                m_SleepTime = FOURTEEN_MILLISECONDS;
                break;
            default:    
                m_SleepTime = TWENTY_SIX_MILLISECONDS;
        }

        final File i2cModule = new File("/dev/i2c-1");
        if (!i2cModule.exists())
        {
            m_Context.setStatus(SummaryStatusEnum.BAD, "Could not find the i2c module. Please load the required "
                    + "modules.");
            throw new FactoryException("Could not find the i2c module. Please load the required i2c modules");
        }
        
        m_Context.setStatus(SummaryStatusEnum.OFF, "Initialized");
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
        final Bmp180SensorAttributes config = Configurable.createConfigurable(Bmp180SensorAttributes.class, props);
        m_SamplingSetting = config.samplingSetting();
        m_Altitude = config.altitude();
        
        switch(m_SamplingSetting)
        {
            case 0:     
                m_SleepTime = FIVE_MILLISECONDS;
                break;
            case 1:     
                m_SleepTime = EIGHT_MILLISECONDS;
                break;
            case 2:     
                m_SleepTime = FOURTEEN_MILLISECONDS;
                break;
            default:    
                m_SleepTime = TWENTY_SIX_MILLISECONDS;
        }
    }
    
    @Override
    public void onActivate() throws AssetException
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            try 
            {
                m_Bus = I2CFactory.getInstance(I2CBus.BUS_1);
                m_Bmp180 = m_Bus.getDevice(I2C_ADDRESS);
                readCalibrationData();
            } 
            catch (final Exception e) 
            {
                m_Context.setStatus(SummaryStatusEnum.BAD, "Unable to activate");
                throw new AssetException(e);
            }
            m_Context.setStatus(SummaryStatusEnum.GOOD, "Activated");
        }
    }

    @Override
    public void onDeactivate() throws AssetException
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            try 
            {
                m_Bus.close();
            } 
            catch (final IOException e) 
            {
                m_Context.setStatus(SummaryStatusEnum.BAD, "Unable to deactivate");
                throw new AssetException(e);
            }
            
            m_Context.setStatus(SummaryStatusEnum.OFF, "Deactivated");
        }
    }
    
    @Override
    public Observation onCaptureData() throws AssetException
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            final TemperatureCelsius temp;
            final PressureMillibars press;
            
            try 
            {
                final Bmp180SensorCalculations calc = new Bmp180SensorCalculations(m_CalibrationData, 
                        readTemperature(), 
                        readPressure(), 
                        m_SamplingSetting, 
                        m_Altitude);
                
                temp = new TemperatureCelsius().withValue(calc.getTemperature());
                press = new PressureMillibars().withValue(calc.getPressure());
            } 
            catch (final Exception e) 
            {
                m_Context.setStatus(SummaryStatusEnum.BAD, "Unable to retrieve the temperature or pressure");
                throw new AssetException(e);
            }
            
            final Weather weather = new Weather().withTemperature(temp).withPressure(press);
            
            return new Observation().withWeather(weather);
        }
    }
    
    @Override
    public Observation onCaptureData(final String sensorId) throws AssetException
    {
        throw new AssetException(
            new UnsupportedOperationException("Bmp180Sensor does not support capturing data by sensorId."));
    }

    @Override
    public Status onPerformBit()
    {
        throw new UnsupportedOperationException("Built-In Testing is not supported");
    }
    
    @Override
    public Response onExecuteCommand(final Command command)
    {
        throw new UnsupportedOperationException("Command execution is not supported");
    }
    
    @Override
    public Set<Extension<?>> getExtensions()
    {
        return new HashSet<Extension<?>>();
    }
    
    /**
     * Reads the calibration data from the BMP180 sensor. There is an extra short read as the MB registry data on the 
     * BMP180 is not needed, but all calibration bytes must be read in order.
     *  
     * @throws IOException when failing to read a byte either from across i2c or from the data stream  
     */
    private void readCalibrationData() throws IOException
    {
        final byte[] bytes = new byte[CALIBRATION_BYTES];

        m_Bmp180.read(EEPROM_START, bytes, 0, CALIBRATION_BYTES);
        final DataInputStream bmp180CaliIn = new DataInputStream(new ByteArrayInputStream(bytes));
        
        m_CalibrationData = new int[CALIBRATION_SIZE];

        for (int i = 0; i < m_CalibrationData.length; i++)
        {
            if (CALIBRATION_ARRAY_AC4 <= i && i <= CALIBRATION_ARRAY_AC6)
            {
                m_CalibrationData[i] = bmp180CaliIn.readUnsignedShort();
            }
            else
            {
                if (i == CALIBRATION_ARRAY_MB)
                {
                    bmp180CaliIn.readShort();
                    m_CalibrationData[i] = bmp180CaliIn.readShort();
                }
                else
                {
                    m_CalibrationData[i] = bmp180CaliIn.readShort();
                }
            }
        }
    }
      
    /**
     * Reads the uncompensated temperature from the BMP180 sensor.
     * 
     * @return float the uncompensated temperature
     * @throws IOException when failing to read a byte either from across i2c or from the data stream
     * @throws InterruptedException when failing to sleep
     */
    private int readTemperature() throws IOException, InterruptedException
    {
        final byte[] bytesTemp = new byte[2];
        
        m_Bmp180.write(CONTROL_REGISTER, GET_TEMP_CMD);
        Thread.sleep(FIVE_MILLISECONDS);

        m_Bmp180.read(MSB_ADDR, bytesTemp, 0, 2);
        
        final DataInputStream bmp180In = new DataInputStream(new ByteArrayInputStream(bytesTemp));
        
        final int UT = bmp180In.readUnsignedShort();
        
        bmp180In.close();
        
        return UT;
    }

    /**
     * Reads the uncompensated pressure from the BMP180 sensor.
     * 
     * @return long the uncompensated pressure
     * @throws IOException when failing to read a byte either from across i2c or from the data stream
     * @throws InterruptedException when failing to sleep
     */
    private long readPressure() throws IOException, InterruptedException
    {   
        final byte[] bytesPress = new byte[PRESSURE_DATA_SIZE];
        
        m_Bmp180.write(CONTROL_REGISTER, (byte)(PRESSURE_WRITE_START + (m_SamplingSetting << SIX_BIT_SHIFT)));
        Thread.sleep(m_SleepTime);
            
        m_Bmp180.read(MSB_ADDR, bytesPress, 0, PRESSURE_DATA_SIZE);
        
        final DataInputStream bmp180In = new DataInputStream(new ByteArrayInputStream(bytesPress));
        
        final long msb = bmp180In.read();
        final long lsb = bmp180In.read();
        final long xlsb = bmp180In.read();
        
        bmp180In.close();
        
        final long UP = ((msb << SIXTEEN_BIT_SHIFT) + (lsb << EIGHT_BIT_SHIFT) + xlsb) 
                >> (EIGHT_BIT_SHIFT - m_SamplingSetting);
        
        return UP;
    }
}
