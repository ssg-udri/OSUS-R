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
package mil.dod.th.core.validator;

import aQute.bnd.annotation.ProviderType;

/**
 * OSGi service to validate a core JAXB type that has a backing schema defined.
 */
@ProviderType
public interface Validator
{

    /**
     * Validate the given object against the core schemas.
     * 
     * @param object
     *      object to validate, must be a JAXB object provided by the core
     * @throws ValidationFailedException
     *      if the validation fails
     */
    void validate(Object object) throws ValidationFailedException;
}
