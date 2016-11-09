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
package example.asset.exception;

import aQute.bnd.annotation.metatype.Meta.OCD;
import mil.dod.th.core.ConfigurationConstants;
import mil.dod.th.core.asset.AssetAttributes;

/**
 * Defines the metadata for the properties available to the {@link example.asset.exception.ExampleObsExAsset}.
 * 
 * @author dlandoll
 */
@OCD (description = ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION)
public interface ExampleObsExAssetAttributes extends AssetAttributes
{
    //Interface required but the asset doesn't need any additional configuration properties defined.
}
