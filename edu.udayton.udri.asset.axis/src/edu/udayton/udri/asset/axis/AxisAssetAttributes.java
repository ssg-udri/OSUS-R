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
package edu.udayton.udri.asset.axis;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;
import mil.dod.th.core.ConfigurationConstants;
import mil.dod.th.core.asset.AssetAttributes;

/**
 * Defines the metadata for the properties available to the {@link edu.udayton.udri.asset.axis.AxisAsset}.
 * 
 * @author cweisenborn
 */
@OCD (description = ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION)
public interface AxisAssetAttributes extends AssetAttributes 
{
    /**
     * Property constant for the IP address.
     */
    String CONFIG_PROP_IP_ADDRESS = "ip.address";
    
    /**
     * Configuration property for the IP address of the axis camera.
     * 
     * @return
     *      String representation of the IP address.
     */
    @AD(id = CONFIG_PROP_IP_ADDRESS, name = "IP Address", description = "IP address of the axis camera", 
            required = false, deflt = "Change me")
    String ipAddress();
}
