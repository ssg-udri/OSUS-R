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
package edu.udayton.udri.asset.novatel;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;
import mil.dod.th.core.ConfigurationConstants;
import mil.dod.th.core.asset.AssetAttributes;

/**
 * Defines the metadata for the properties available to the {@link edu.udayton.udri.asset.novatel.NovatelAsset}.
 * 
 * @author cweisenborn
 */
@OCD (description = ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION)
public interface NovatelAssetAttributes extends AssetAttributes
{     
    /**
     * The port constant of the NovAtel serial port.
     */
    String CONFIG_PROP_PHYS_PORT = "serial.port";
    
    /**
     * The baud rate constant of the NovAtel serial port.
     */
    String CONFIG_PROP_BAUD_RATE = "baud.rate";
    
    /**
     * Novatel handle message frequency key constant.
     */
    String CONFIG_PROP_HANDLE_MESSAGE_FREQUENCY = "handle.ins.frequency";  
    
    /**
     * Novatel connect to system time service key constant.
     */
    String CONFIG_PROP_SYNC_SYSTEM_TIME_WITH_GPS = "sync.system.time.with.gps";
    
    /**
     * Novatel time service port key constant.
     */
    String CONFIG_PROP_TIME_SERVICE_PORT = "time.service.port";
    
    /**
     * Configuration property for the physical port used by the NovAtel asset.
     * 
     * @return
     *      String representation of the physical port used by the NovAtel.
     */
    @AD(id = CONFIG_PROP_PHYS_PORT, name = "Physical Port", description = "The actual port value to be used (e.g. COM1,"
            + " COM3)", required = false, deflt = "COM1")
    String physicalPort();
    
    /**
     * Configuration property for the baud rate to be applied to the physical port.
     * 
     * @return
     *      Integer that represents the baud rate to be applied.
     */
    @AD(id = CONFIG_PROP_BAUD_RATE, name = "Baud Rate", description = "The baud rate to be applied to the serial port "
            + "connection.", deflt = "115200", min = "0", max = "230400", required = false)
    int buadRate();
    
    /**
     * Configuration property for the frequency at which INSPVA messages are handled.
     * 
     * @return
     *      Integer used to determine the frequency messages should be handled at.
     */
    @AD(id = CONFIG_PROP_HANDLE_MESSAGE_FREQUENCY, name = "Handle Message Frequency", description = "The frequency at "
            + "which INSPVA messages are handled. A value of 2 means that every other message will be handled. To "
            + "handle all of the INSPVA messages use a value of 1.", deflt = "20", min = "1", max = "2000", 
            required = false)
    int handleMessageFreq();
    
    /**
     * Configuration property that indicates whether the NovAtel asset should attempt to sync the system time from the
     * GPS.
     * 
     * @return
     *      Boolean value used to determine whether or not to sync time.
     */
    @AD(id = CONFIG_PROP_SYNC_SYSTEM_TIME_WITH_GPS, name = "Sync System Time With GPS", description = "Flag to "
            + "indicate whether the Novatel Asset should attempt to sync the system time from the GPS. Changing this "
            + "value will only take effect on activation of asset.", deflt = "true", required = false)
    boolean syncSystemTimeWithGps();
    
    /**
     * Configuration property for the port over which the NovAtel asset will attempt to sync the system time from the
     * GPS.
     * 
     * @return
     *      Integer representing the port number the time service should use to synce system time from the GPS.
     */
    @AD(id = CONFIG_PROP_TIME_SERVICE_PORT, name = "Time Service Port", description = "The port over which the Novatel "
            + "Asset will attempt to sync the system time from the GPS. Changing this value will only take effect on "
            + "activation of asset.", deflt = "4444", min = "1", max = "65535", required = false)
    int timeServicePort();
}
