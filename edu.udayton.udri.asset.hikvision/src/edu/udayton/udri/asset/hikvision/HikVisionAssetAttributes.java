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
package edu.udayton.udri.asset.hikvision;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;
import mil.dod.th.core.ConfigurationConstants;
import mil.dod.th.core.asset.AssetAttributes;

/**
 * Interface which defines the configurable properties for HikVisionAsset.
 */
@OCD(description = ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION)
public interface HikVisionAssetAttributes extends AssetAttributes
{

    /**
     * User Name Variable.
     */
    String USERNAME = "HikVision.UserName";
    
    /**
     * IP Address Variable.
     */
    String IP = "HikVision.IP";
    
    /**
     * Password Variable.
     */
    String PASSWORD = "HikVision.Password";
    
    /**
     * Current Version.
     */
    String VERSION = "HikVision.Version";
    
    /**
     * Setting for Current version.
     * 
     * @return 
     *     the Current version of the bundle
     */
    @AD(required = false, description = "Current version of the bundle", id = VERSION, name = "Version")
    String version();

    /**
     * Setting the default connection to nothing forcing the user to input the correct IP address.
     * 
     * @return
     *     a string that represents IP of the asset you want to be connected
     */
    @AD(required = false, // whether the property must be supplied when updating the configuration
        deflt = "0", // default (only applicable if required is false)
        description = "The IP of the asset you need to connect to",
        id = IP,
        name = "IP Address") // description of property shown in gui
    String ip(); // The IP address you need to connect to

    /**
     * Setting the default user name to nothing forcing the user to enter the user name.
     * 
     * @return 
     *     a string that represents the current user name
     */
    @AD(required = false, deflt = " ", description = "User Name to login", id = USERNAME, name = "User Name")
    String userName(); // User Name

    /**
     * Setting the default password to nothing forcing the user to enter the correct password.
     * 
     * @return 
     *      a string that represents the current password
     */
    @AD(required = false, deflt = " ", description = "Password for login", id = PASSWORD, name = "User Password")
    String password(); // Login Password
}
