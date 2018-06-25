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
package mil.dod.th.core.ccomm.link;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

import mil.dod.th.core.ConfigurationConstants;
import mil.dod.th.core.factory.FactoryObject;

/**
 * Defines metadata of properties that are available to all {@link LinkLayer}s. Retrieve properties using {@link 
 * LinkLayer#getConfig()}.
 * 
 * @author dhumeniuk
 */
@OCD (description = ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION)
public interface LinkLayerAttributes
{
    /** Configuration property key for {@link #activateOnStartup()}. */
    String CONFIG_PROP_ACTIVATE_ON_STARTUP = FactoryObject.TH_PROP_PREFIX + ".activate.on.startup";

    /** Configuration property key for {@link #retries()}. */
    String CONFIG_PROP_RETRIES = FactoryObject.TH_PROP_PREFIX + ".retries";

    /** Configuration property key for {@link #readTimeoutMs()}. */
    String CONFIG_PROP_READ_TIMEOUT_MS = FactoryObject.TH_PROP_PREFIX + ".read.timeout.ms";
    
    /** Configuration property key for {@link #physicalLinkName()}. */
    String CONFIG_PROP_PHYSICAL_LINK_NAME = FactoryObject.TH_PROP_PREFIX + ".physical.link.name";

    /** 
     * Configuration property for the activate on startup flag.
     * 
     * @return true if link layer should be activated on start up of the core, false if not
     */
    @AD(required = false, deflt = "false", id = CONFIG_PROP_ACTIVATE_ON_STARTUP,
        description = "Determines whether the link layer should be activated during startup")
    boolean activateOnStartup();

    /** 
     * Configuration property for the number of retries for {@link 
     * LinkLayer#send(LinkFrame, mil.dod.th.core.ccomm.Address)}.
     * 
     * @return number of retries
     */
    @AD(required = false, deflt = "2", id = CONFIG_PROP_RETRIES,
        description = "Number of retries to attempt when sending a fragment")
    int retries();
    
    /**
     * Configuration property for the timeout in milliseconds during read calls.
     * 
     * @return timeout in milliseconds for timeout or 0 for block forever
     */
    @AD(required = false, deflt = "0", id = CONFIG_PROP_READ_TIMEOUT_MS,
        description = "Timeout value for read calls in milliseconds")
    int readTimeoutMs();
    
    /**
     * Configuration property for the name of the {@link mil.dod.th.core.ccomm.physical.PhysicalLink} used by this 
     * {@link LinkLayer}.
     * 
     * @return name of the {@link mil.dod.th.core.ccomm.physical.PhysicalLink}
     */
    @AD(required = false, deflt = "", id = CONFIG_PROP_PHYSICAL_LINK_NAME,
        description = "Name of the physical link used by the link layer")
    String physicalLinkName();
}
