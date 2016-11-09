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
package example.asset;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

import mil.dod.th.core.ConfigurationConstants;
import mil.dod.th.core.asset.AssetAttributes;

/**
 * Defines the metadata for the properties available to the {@link example.asset.ExampleAsset}.
 * 
 * @author cweisenborn
 */
@OCD (description = ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION)
public interface ExampleAssetAttributes extends AssetAttributes
{
    String CONFIG_PROP_DEVICE_POWER_NAME = "devicePowerName";
    
    /**
     * Device power toggle name.
     * 
     * @return
     *      Device power toggle name
     */
    @AD(id = CONFIG_PROP_DEVICE_POWER_NAME, name = "Device Power Toggle Name",
            description = "Name of device power toggle to use", required = false,
            deflt = "invalid\\, update this value!")
    String devicePowerName();
    
    @AD(description = "Example of a multiple choice", required = false, deflt = "OptionA")
    MultiChoice exampleChoice();
    
    enum MultiChoice
    {
        OptionA, 
        OptionB,
        OptionC
    }
}
