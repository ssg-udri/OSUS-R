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
package mil.dod.th.core.datastream;


import java.net.URI;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;
import mil.dod.th.core.ConfigurationConstants;
import mil.dod.th.core.factory.FactoryObject;

/**
 * Defines metadata of properties that are available to all {@link StreamProfile}s.
 * Retrieve properties using {@link StreamProfile#getConfig()}.
 * 
 * @author jmiller
 *
 */
@OCD (description = ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION)
public interface StreamProfileAttributes
{
    
    /** Configuration property key for {@link #assetName()}. */
    String CONFIG_PROP_ASSET_NAME = FactoryObject.TH_PROP_PREFIX + ".asset.name";
    
    /** Configuration property key for {@link #bitrateKbps()}. */
    String CONFIG_PROP_BITRATE_KBPS = FactoryObject.TH_PROP_PREFIX + ".bitratekbps";
    
    /** Configuration property key for {@link #dataSource()}. */
    String CONFIG_PROP_DATA_SOURCE = FactoryObject.TH_PROP_PREFIX + ".data.source";
    
    /** Configuration property key for {@link #format()}. */
    String CONFIG_PROP_FORMAT = FactoryObject.TH_PROP_PREFIX + ".format";
    
    /** Configuration property key for {@link #sensorId()}. */
    String CONFIG_PROP_SENSOR_ID = FactoryObject.TH_PROP_PREFIX + ".sensor.id";
    
    /**
     * Configuration property for the asset associated with the stream profile.
     * 
     * @return name of the asset, as a String.
     */
    @AD(required = true, id = CONFIG_PROP_ASSET_NAME, description = "Name of the asset associated "
            + "with this stream profile configuration, as a String.")
    String assetName();
    
    /**
     * Configuration property for the target bitrate of the stream profile.
     * 
     * @return bitrate in kilobits per second, or a value less than 0 to disable 
     * transcoding.
     */
    @AD(required = false, deflt = "-1.0", id = CONFIG_PROP_BITRATE_KBPS, description = "Target bitrate "
            + "in kilobits per second, or a value less than 0 to disable transcoding and use the source bitrate.")
    double bitrateKbps();
    
    /**
     * Configuration property for the URI of the streaming data source.
     * 
     * @return URI where consumer can acquire source data stream.
     */
    @AD(required = true, id = CONFIG_PROP_DATA_SOURCE, description = "URI for the source data stream. "
            + "The asset plugin is reponsible for making the source stream available through this URI. "
            + "In the case of an IP-based streaming asset, the URI might already be provided and can "
            + "be used here.")
    URI dataSource();
    
    /**
     * Configuration property for the stream data format.
     * 
     * @return String representation of a {@link mil.dod.th.core.datastream.types.StreamFormat}, which can 
     * be either a standard MIME type (e.g. "video/mpg") or a custom format label. For a custom format, the 
     * label will be the fully qualified class name of the custom format interface.
     * 
     */
    @AD(required = true, id = CONFIG_PROP_FORMAT, description = "Format of the output stream, which is"
            + " either a standard MIME type or a custom format label")
    String format();
    
    /**
     * Configuration property for the sensor ID associated with the stream.
     * 
     * @return sensor ID as a String. This field is optional, as assets are not required to have sensor IDs.
     */
    @AD(required = false, deflt = "", id = CONFIG_PROP_SENSOR_ID, description = "Sensor ID as a String. "
            + "An asset can have zero or more sensors associated with it")
    String sensorId();
    
}
