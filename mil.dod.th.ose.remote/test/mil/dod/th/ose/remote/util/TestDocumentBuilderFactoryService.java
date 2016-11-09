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
package mil.dod.th.ose.remote.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;

/**
 * Test the Document builder factory service.
 * @author callen
 *
 */
public class TestDocumentBuilderFactoryService
{
    private DocumentBuilderFactoryService m_SUT;

    @Before
    public void setUp()
    {
        m_SUT = new DocumentBuilderFactoryService();
    }

    /**
     * Test getting an instance of a document build from the factory service.
     * Verify that a document builder is returned.
     */
    @Test
    public void getDocumentBuilder() throws ParserConfigurationException
    {
        assertThat(m_SUT.getDocumentBuilder(), is(instanceOf(DocumentBuilder.class)));
    }
}
