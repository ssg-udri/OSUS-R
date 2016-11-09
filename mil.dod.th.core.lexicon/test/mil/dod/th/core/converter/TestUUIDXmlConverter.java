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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.UUID;

import org.junit.Test;


/**
 * @author jconn
 *
 */
public class TestUUIDXmlConverter
{
    private final UUID m_UUID = UUID.randomUUID();
    private final String m_UUIDString = m_UUID.toString();
    

    @Test
    public void testUUIDXmlConverter()
    {
        final UUIDXmlConverter converter = new UUIDXmlConverter();
        assertThat(converter, is(notNullValue()));
        
        final String uuidString = converter.marshal(m_UUID);
        assertThat(uuidString, is(m_UUIDString));

        final UUID recoveredUUID = converter.unmarshal(m_UUIDString);
        assertThat(recoveredUUID, is(m_UUID));
    }
}
