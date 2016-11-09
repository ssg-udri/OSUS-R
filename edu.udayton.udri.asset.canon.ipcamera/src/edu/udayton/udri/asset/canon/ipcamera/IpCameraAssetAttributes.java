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
package edu.udayton.udri.asset.canon.ipcamera;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;
import mil.dod.th.core.ConfigurationConstants;

import mil.dod.th.core.asset.AssetAttributes;

/**
 * Interface which defines the configurable properties for IpCameraAsset.
 */
@OCD (description = ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION)
public interface IpCameraAssetAttributes extends AssetAttributes
{
    
    /**
     * A tag created for the IP camera.
     */
    String IP = "IpCamera.IP";
    
    /**
     * Defines the IP address of the camera.
     * 
     * @return
     *     the IP address assigned to the Camera
     */
    @AD(required = false, 
        deflt = "192.168.0.1", //NOPMD hard coded IP address, default
        name = "IP",
        id = IP,
        description = "The IP address of the camera"
        )
    String ipAddress(); 
}