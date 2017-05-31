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
package example.asset.gui;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

import mil.dod.th.core.ConfigurationConstants;
import mil.dod.th.core.asset.AssetAttributes;

/**
 * Defines the metadata for the properties available to the {@link example.asset.gui.ExampleUpdateAsset}.
 * 
 * @author cweisenborn
 */
@OCD (description = ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION)
public interface ExampleUpdateAssetAttributes extends AssetAttributes
{
    String CONFIG_PROP_SENSOR_ID = "ex.update.sensor.id";
    String CONFIG_PROP_LATITUDE = "ex.update.latitude";
    String CONFIG_PROP_LONGITUDE = "ex.update.longitude";

    /**
     * Device sensor ID.
     * 
     * @return
     *      Device sensor ID
     */
    @AD(id = CONFIG_PROP_SENSOR_ID, name = "Sensor ID",
            description = "Used by asset to update asset position with a sensor ID", required = false,
            deflt = "")
    String sensorId();

    /**
     * Device latitude.
     * 
     * @return
     *      Device latitude
     */
    @AD(id = CONFIG_PROP_LATITUDE, name = "Latitude",
            description = "Used by asset to update asset location latitude", required = false,
            deflt = "0.0")
    double latitude();

    /**
     * Device longitude.
     * 
     * @return
     *      Device longitude
     */
    @AD(id = CONFIG_PROP_LONGITUDE, name = "Longitude",
            description = "Used by asset to update asset location longitude", required = false,
            deflt = "0.0")
    double longitude();
}
