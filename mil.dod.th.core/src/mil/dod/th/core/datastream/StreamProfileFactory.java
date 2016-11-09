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
package mil.dod.th.core.datastream;

import aQute.bnd.annotation.ProviderType;

import mil.dod.th.core.datastream.capability.StreamProfileCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;

/**
 * Interface defines attributes of a stream profile plug-in and can be retrieved 
 * through {@link StreamProfile#getFactory()}.
 * 
 * @author jmiller
 *
 */
@ProviderType
public interface StreamProfileFactory extends FactoryDescriptor
{
    /**
     * Same as {@link FactoryDescriptor#getCapabilities()}, but returns additional capability information specific to
     * a stream profile.
     * 
     * @return the stream profile's capabilities
     */
    StreamProfileCapabilities getStreamProfileCapabilities();
}
