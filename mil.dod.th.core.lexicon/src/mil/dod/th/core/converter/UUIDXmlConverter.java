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

package mil.dod.th.core.converter;

import java.util.UUID;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * XmlAdatper class used by JAXB to bind an xml UUID to a java.util.UUID object.
 * 
 * @author ssetter
 * @version 1.0
 */
public class UUIDXmlConverter extends XmlAdapter<String, UUID>
{
    /**
     * Marshals a UUID object to a string.
     * 
     * @param uuid
     *            uuid to marshal
     * @return string representation of the uuid
     */
    @Override
    public String marshal(final UUID uuid)
    {
        if (uuid == null)
        {
            return null;
        }
        
        return uuid.toString();
    }

    /**
     * Unmarshals a uuid object from a string.
     * 
     * @param uuid
     *            uuid to unmarshal
     * @return reconstructed uuid object
     */
    @Override
    public UUID unmarshal(final String uuid)
    {
        return UUID.fromString(uuid);
    }

}