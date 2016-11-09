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
package example.ccomms.exception;

import mil.dod.th.core.ccomm.link.LinkLayerAttributes;
import aQute.bnd.annotation.metatype.Meta.OCD;

/**
 * Attribute interface meant for testing what happens if the OCD description is missing.
 * 
 * @author callen
 */
@OCD
public interface ExampleLinkLayerMissingOcdDescAttributes extends LinkLayerAttributes
{
    //Interface required but physical link doesn't need any additional properties defined.
}
