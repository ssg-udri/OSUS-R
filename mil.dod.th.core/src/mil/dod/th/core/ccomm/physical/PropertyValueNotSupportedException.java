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
//
// DESCRIPTION:
// This file contains the PropertyValueNotSupportedException exception class.  
// Generated from Enterprise Architect.  The Enterprise Architect model should 
// be updated for all non-implementation changes (function names, arguments, 
// notes, etc.) and re-synced with the code.
//
//==============================================================================
package mil.dod.th.core.ccomm.physical;

/**
 * Exception thrown when a particular value is not supported by a physical link. Different platforms will support 
 * different values.
 * 
 * @author dhumeniuk
 */
public class PropertyValueNotSupportedException extends PhysicalLinkException
{
    /**
     * Unique id for this exception.
     */
    private static final long serialVersionUID = -2736820455884623027L;

    /**
     * Main constructor for throwing exception.
     * 
     * @param key
     *            key for the property that does not support the value
     * @param value
     *            value not supported by the property
     */
    public PropertyValueNotSupportedException(final String key, final Object value)
    {
        super(String.format("The value '%s' for the %s property is not supported", value, key));
    }
}
