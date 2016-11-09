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
package mil.dod.th.ose.jaxbprotoconverter.proto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

/**
 * @author cweisenborn
 */
public class TestProtoFile
{
    private ProtoFile m_SUT;
    private ProtoModel m_ProtoModel;
    private File m_XsdFile;
    
    @Before
    public void setup()
    {
        m_XsdFile = mock(File.class);
        m_ProtoModel = new ProtoModel();
        m_SUT = new ProtoFile(m_ProtoModel, m_XsdFile);
    }
    
    /**
     * Verify that the appropriate file is returned by the get XSD file method.
     */
    @Test
    public void testGetXsdFile()
    {
        assertThat(m_SUT.getXsdFile(), is(m_XsdFile));
    }
    
    /**
     * Verify that the appropriate proto model is return by the get proto model method.
     */
    @Test
    public void testGetProtoModel()
    {
        assertThat(m_SUT.getProtoModel(), is(m_ProtoModel));
    }
    
    /**
     * Verify that a set is returned by the get imports method.
     */
    @Test
    public void testGetImports()
    {
        assertThat(m_SUT.getImports(), is(notNullValue()));
    }
    
    /**
     * Verify that a map is returned by the get messages map method.
     */
    @Test
    public void testGetMessagesMap()
    {
        assertThat(m_SUT.getMessageMap(), is(notNullValue()));
    }
}
