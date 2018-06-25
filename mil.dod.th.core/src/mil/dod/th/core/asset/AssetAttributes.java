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
package mil.dod.th.core.asset;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

import mil.dod.th.core.ConfigurationConstants;
import mil.dod.th.core.factory.FactoryObject;

/**
 * Defines metadata of properties that are available to all {@link Asset}s. Retrieve properties using {@link 
 * Asset#getConfig()}.
 * 
 * @author dhumeniuk
 */
@OCD (description = ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION)
public interface AssetAttributes
{
    /** Configuration property key for {@link #activateOnStartup()}. */
    String CONFIG_PROP_ACTIVATE_ON_STARTUP = FactoryObject.TH_PROP_PREFIX + ".activate.on.startup";
    
    /** Configuration property key for {@link #pluginOverridesPosition()}. */
    String CONFIG_PROP_PLUGIN_OVERRIDES_POSITION = FactoryObject.TH_PROP_PREFIX + ".plugin.overrides.position";
    
    /** 
     * Configuration property for the activate on startup flag.
     * 
     * @return true if asset should be activated on start up of the core, false if not
     */
    @AD(required = false, deflt = "false", id = CONFIG_PROP_ACTIVATE_ON_STARTUP,
        description = "Determines whether the asset should be activated during startup")
    boolean activateOnStartup();
    
    /** 
     * Configuration property for position override by plug-in.
     * 
     * @return true if the plug-in provides the position, false if the core manages the position
     */
    @AD(required = false, deflt = "false", id = CONFIG_PROP_PLUGIN_OVERRIDES_POSITION,
        description = "When this property is false, position is managed by the core for the asset instance. When true, "
                      + "position will be managed by the plug-in.")
    boolean pluginOverridesPosition();
}
