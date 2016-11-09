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
package mil.dod.th.core.ccomm.physical;

import aQute.bnd.annotation.ProviderType;
import mil.dod.th.core.factory.FactoryObjectContext;

/**
 * This is the context of the {@link PhysicalLink} that is made available to implementors of {@link PhysicalLinkProxy}. 
 * Each instance of a {@link PhysicalLink} will have a matching context to allow the plug-in to interact with the rest
 * of the system.
 * 
 * @author dhumeniuk
 *
 */
@ProviderType
public interface PhysicalLinkContext extends PhysicalLink, FactoryObjectContext
{

}
