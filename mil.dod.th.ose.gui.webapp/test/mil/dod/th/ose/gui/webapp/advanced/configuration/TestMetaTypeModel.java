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
package mil.dod.th.ose.gui.webapp.advanced.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import mil.dod.th.core.remote.proto.MetaTypeMessages.AttributeDefinitionType;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.metatype.AttributeDefinition;

/**
 * Test class for the {@link MetaTypeModel} class.
 * 
 * @author cweisenborn
 */
public class TestMetaTypeModel
{
    private MetaTypeModel m_SUT;
    
    @Before
    public void setup()
    {
        m_SUT = new MetaTypeModel("test pid", 25L);
    }
    
    /**
     * Test the get pid method.
     * Verify that the correct pid is returned.
     */
    @Test
    public void testGetPid()
    {
        assertThat(m_SUT.getPid(), is("test pid"));
    }
    
    /**
     * Test the get bundle id method.
     * Verify that the correct bundle id is returned.
     */
    @Test
    public void testGetBundleId()
    {
        assertThat(m_SUT.getBundleId(), is(25L));
    }
    
    /**
     * Test the get attributes method.
     * Verify that the get attributes method returns a list containing the correct attribute.
     */
    @Test
    public void testGetAttributes()
    {
        AttributeModel attribute = new AttributeModel(AttributeDefinitionType.newBuilder()
                .setId("key")
                .setName("name")
                .setCardinality(1)
                .setAttributeType(AttributeDefinition.STRING)
                .setRequired(true)
                .build());
        
        m_SUT.getAttributes().add(attribute);
        
        assertThat(m_SUT.getAttributes().size(), is(1));
        assertThat(m_SUT.getAttributes().get(0), is(attribute));
    }
}
