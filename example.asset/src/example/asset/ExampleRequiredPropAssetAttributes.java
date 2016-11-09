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

import mil.dod.th.core.ConfigurationConstants;
import mil.dod.th.core.asset.AssetAttributes;
import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

/**
 * Defines the metadata for the properties available to the {@link example.asset.ExampleRequiredPropAsset}.
 * 
 * @author cweisenborn
 */
@OCD (description = ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION)
public interface ExampleRequiredPropAssetAttributes extends AssetAttributes
{
    String CONFIG_PROP_HOSTNAME = "hostname";
    String CONFIG_PROP_PORT_NUM = "portNumber";
    
    @AD(name = CONFIG_PROP_HOSTNAME, required = true, description = "Example configurable hostname property.")
    String hostname();
    
    @AD(name = CONFIG_PROP_PORT_NUM, required = true, description = "Example configurable port property.")
    int portNumber();
}
