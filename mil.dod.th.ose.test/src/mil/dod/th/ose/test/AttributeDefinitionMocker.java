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
package mil.dod.th.ose.test;

import static org.mockito.Mockito.*;

import org.osgi.service.metatype.AttributeDefinition;

/**
 * Mock the {@link AttributeDefinition} interface.
 * @author dhumeniuk
 *
 */
public class AttributeDefinitionMocker
{
    /**
     * Method used to produce a mock attribute definition. Note cardinality of attribute definition is always assumed
     * to be 0.
     * @param id
     *  the id of the attribute definition
     * @param name
     *  the name of the attribute definition
     * @param desc
     *  the description of the attribute definition
     * @param type
     *  the type that this attribute definition represents
     * @param defaults
     *  a string outlining the possible default values of the definition
     * @return
     *  the mocked attribute definition
     */
    public static AttributeDefinition mockIt(final String id, final String name, 
            final String desc, final int type, final String defaults)
    {
        AttributeDefinition definition = mock(AttributeDefinition.class);
        
        when(definition.getID()).thenReturn(id);
        when(definition.getName()).thenReturn(name);
        when(definition.getDescription()).thenReturn(desc);
        when(definition.getType()).thenReturn(type);
        when(definition.getDefaultValue()).thenReturn(new String[] {defaults});
        when(definition.getCardinality()).thenReturn(0);
        
        return definition;
    }
    
    /**
     * Create an array of all {@link AttributeDefinition}s for testing.
     */
    public static AttributeDefinition[] mockArrayAll()
    {
        AttributeDefinition[] attrs = new AttributeDefinition[6];
        attrs[0] = mockIt("id1","name1", "desc1", AttributeDefinition.BOOLEAN, "false");
        attrs[1] = mockIt("id2","name2", "desc2", AttributeDefinition.INTEGER, "1");
        attrs[2] = mockIt("id3","name3", "desc3", AttributeDefinition.STRING, "value");
        attrs[3] = mockIt("id4","name4", "desc4", AttributeDefinition.FLOAT, "1.1");
        attrs[4] = mockIt("id5","name5", "desc5", AttributeDefinition.CHARACTER, null);
        attrs[5] = mockIt("id6","name6", "desc6", AttributeDefinition.BYTE, null);
        
        return attrs;
    }
    
    /**
     * Create an array of required {@link AttributeDefinition}s for testing.
     */
    public static AttributeDefinition[] mockArrayRequired()
    {
        AttributeDefinition[] attrs = new AttributeDefinition[2];
        attrs[0] = mockIt("id5","name5", "desc5", AttributeDefinition.CHARACTER, null);
        attrs[1] = mockIt("id6","name6", "desc6", AttributeDefinition.BYTE, null);
        
        return attrs;
    }
    
    /**
     * Create an array of optional {@link AttributeDefinition}s for testing.
     */
    public static AttributeDefinition[] mockArrayOptional()
    {
        AttributeDefinition[] attrs = new AttributeDefinition[4];
        attrs[0] = mockIt("id1","name1", "desc1", AttributeDefinition.BOOLEAN, "false");
        attrs[1] = mockIt("id2","name2", "desc2", AttributeDefinition.INTEGER, "1");
        attrs[2] = mockIt("id3","name3", "desc3", AttributeDefinition.STRING, "value");
        attrs[3] = mockIt("id4","name4", "desc4", AttributeDefinition.FLOAT, "1.1");
        
        return attrs;
    }
}
