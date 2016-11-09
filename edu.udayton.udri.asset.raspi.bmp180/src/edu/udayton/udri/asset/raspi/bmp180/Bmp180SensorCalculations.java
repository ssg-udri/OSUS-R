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


/**
 * Calculations (pressure and temperature) for the BMP180.
 * 
 * @author Nathaniel Rosenwald
 */
public class Bmp180SensorCalculations 
{
    private float m_Temperature;
    private long m_Pressure;
    
    /**
     * Calculates the compensated pressure and temperature readings.
     * 
     * @param calData calibration data
     * @param UT uncompensated temperature
     * @param UP uncompensated pressure
     * @param setting sampling setting
     * @param altitude sensor altitude
     */
    public Bmp180SensorCalculations(final int[] calData, 
            final int UT, 
            final long UP, 
            final short setting, 
            final double altitude)
    {
        if (calData.length != 10)
        {
            throw new IllegalArgumentException("The calibration does not have 10 values.");
        }
        
        final int AC1 = calData[0];
        final int AC2 = calData[1];
        final int AC3 = calData[2];
        final int AC4 = calData[3];
        final int AC5 = calData[4];
        final int AC6 = calData[5];
        final int B1 =  calData[6];
        final int B2 =  calData[7];
        final int MC =  calData[8];
        final int MD =  calData[9];
        
        
        long X1 = ((UT - AC6) * AC5) >> 15;
        long X2 = (MC << 11) / (X1 + MD);
        final long B5 = X1 + X2;
        
        m_Temperature = (float)((B5 + 8) >> 4) / 10;
        
        final long B6 = B5 - 4000;
        X1 = (B2 * (B6 * B6) >> 12) >> 11;
        X2 = AC2 * B6 >> 11;
        long X3 = X1 + X2;
        final long B3 = (((AC1 * 4 + X3) << setting) + 2) / 4;
        X1 = AC3 * B6 >> 13;
        X2 = (B1 * ((B6 * B6) >> 12)) >> 16;
        X3 = (X1 + X2 + 2) / 4;
        final long B4 = (AC4 * (X3 + 32768)) >> 15;
        final long B7 = (UP - B3) * (50000 >> setting);
        
        long p = (B7 / B4) * 2;
        
        X1 = (p >> 8) * (p >> 8);
        X1 = (X1 * 3038) >> 16;
        X2 = (-7357 * p) >> 16;
        
        p += (X1 + X2 + 3791) >> 4;
        p /= 100;
        p /= Math.pow((288 - 0.0065 * altitude) / 288, 5.256);
        m_Pressure = p;
    }
    
    /**
     * Gets the calculated temperature.
     * 
     * @return float the calculated temperature
     */
    public float getTemperature()
    {
        return m_Temperature;
    }
    
    /**
     * Gets the calculated pressure.
     * 
     * @return float the calculated pressure.
     */
    public long getPressure()
    {
        return m_Pressure;
    }
}
