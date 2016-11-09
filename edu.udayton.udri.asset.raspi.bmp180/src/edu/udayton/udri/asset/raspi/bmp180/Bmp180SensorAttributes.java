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

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;
import mil.dod.th.core.ConfigurationConstants;
import mil.dod.th.core.asset.AssetAttributes;

/**
 * Interface which defines the configurable properties for Bmp180Sensor.
 */
@OCD (description = ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION)
public interface Bmp180SensorAttributes extends AssetAttributes
{
    /**
     * The sampling setting at which the sensor will record pressure. Settings 0-3 are allowed: 0 taking 1 sample and 
     * 4.5ms to complete readings, and 3 taking 8 samples and 25.5ms to complete readings. Refer to the README for 
     * more details.
     * @return short the sampling setting
     */
    @AD (required = false, name = "Sampling setting", deflt = "0",
            description = "The setting at which the sensor records pressure. Settings 0-3 are allowed. Setting 0 "
                    + "takes 1 sample and 4.5ms to complete readings. Setting 1 takes 2 samples and 7.5ms to complete "
                    + "readings. Setting 2 takes 4 samples and 13.5ms to complete. Setting 3 takes 8 samples and "
                    + "25.5ms to complete readings. Refer to the README for more details.")
    short samplingSetting();
    
    /**
     * The sensor's altitude in meters.
     * @return double the altitude in meters
     */
    @AD (required = false, name = "Altitude", deflt = "225",
            description = "The altitude of the sensor above sea level in meters.")
    double altitude();
    
}
