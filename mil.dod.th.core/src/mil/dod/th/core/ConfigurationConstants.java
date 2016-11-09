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
package mil.dod.th.core;


/**
 * This class contains the description constant for partially defined 
 * {@link org.osgi.service.metatype.ObjectClassDefinition}s.
 * @author allenchl
 *
 */
public final class ConfigurationConstants
{
    /**
     * Describes partially defined {@link org.osgi.service.metatype.ObjectClassDefinition}s. 
     * This should be used for core API and plug-in specific OCDs which need to be combined with other OCDs to make 
     * a complete OCD. 
     * For example, MyAssetAttributes, which extends AssetAttributes, the two OCD definitions are not complete until
     * the metatype service combines them into one OCD. So both metatype interface definitions must have this string as
     * the description. As seen below:
     * <p>
     * <code>@OCD (description = ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION)
     * <br>public interface MyAssetAttributes extends AssetAttributes
     * {...}</code>
     */
    public static final String PARTIAL_OBJECT_CLASS_DEFINITION = "th.partial.ocd";
    
    /**
     * Hidden constructor to prevent instantiation.
     */
    private ConfigurationConstants()
    {
        //private constructor to prevent instantiation.
    }
}
