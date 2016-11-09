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
package mil.dod.th.ose.utils;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.metatype.AttributeDefinition;

/**
 * @author dlandoll
 */
public class TestConfigurationUtils
{
    @Before
    public void setUp() throws Exception
    {
    }

    @Test
    public final void testParseStringByType()
    {
        assertThat((Boolean)ConfigurationUtils.parseStringByType(AttributeDefinition.BOOLEAN, "true"), is(true));
        assertThat((Boolean)ConfigurationUtils.parseStringByType(AttributeDefinition.BOOLEAN, "false"), is(false));

        assertThat((Byte)ConfigurationUtils.parseStringByType(AttributeDefinition.BYTE, "53"), is((byte)53));

        assertThat((Character)ConfigurationUtils.parseStringByType(AttributeDefinition.CHARACTER, "f"), is('f'));

        assertThat((Double)ConfigurationUtils.parseStringByType(AttributeDefinition.DOUBLE, "38283222.123828573122"),
                is(38283222.123828573122));

        assertThat((Float)ConfigurationUtils.parseStringByType(AttributeDefinition.FLOAT, "38282.12322"),
                is((float)38282.12322));

        assertThat((Integer)ConfigurationUtils.parseStringByType(AttributeDefinition.INTEGER, "58768458"), 
                is(58768458));

        assertThat((Long)ConfigurationUtils.parseStringByType(AttributeDefinition.LONG, "3213561554"), is(3213561554L));

        assertThat((Short)ConfigurationUtils.parseStringByType(AttributeDefinition.SHORT, "-21558"), is((short)-21558));

        assertThat((String)ConfigurationUtils.parseStringByType(AttributeDefinition.STRING, "hello"), is("hello"));

        try
        {
            ConfigurationUtils.parseStringByType(452, "blah");
            fail("Expecting exception");
        }
        catch (IllegalArgumentException e)
        {
        }
    }
    
    /**
     * Verify the proper class is returned for the given {@link AttributeDefinition} type.
     */
    @Test
    public void testConvertToClassType()
    {
        assertThat(ConfigurationUtils.convertToClassType(AttributeDefinition.BOOLEAN).getSimpleName(), is("Boolean"));
        assertThat(ConfigurationUtils.convertToClassType(AttributeDefinition.BYTE).getSimpleName(), is("Byte"));
        assertThat(ConfigurationUtils.convertToClassType(AttributeDefinition.CHARACTER).getSimpleName(), 
                is("Character"));
        assertThat(ConfigurationUtils.convertToClassType(AttributeDefinition.DOUBLE).getSimpleName(), is("Double"));
        assertThat(ConfigurationUtils.convertToClassType(AttributeDefinition.FLOAT).getSimpleName(), is("Float"));
        assertThat(ConfigurationUtils.convertToClassType(AttributeDefinition.INTEGER).getSimpleName(), is("Integer"));
        assertThat(ConfigurationUtils.convertToClassType(AttributeDefinition.LONG).getSimpleName(), is("Long"));
        assertThat(ConfigurationUtils.convertToClassType(AttributeDefinition.SHORT).getSimpleName(), is("Short"));
        assertThat(ConfigurationUtils.convertToClassType(AttributeDefinition.STRING).getSimpleName(), is("String"));
    }
  
    @SuppressWarnings({"unchecked"})
    @Test
    public final void testParseStringsByType()
    {
        final String[] values = {"one", "two"};
        
        assertArrayEquals((Object[])ConfigurationUtils.parseStringsByType(AttributeDefinition.STRING, 2, values), 
                values);
        assertThat((Vector<String>)ConfigurationUtils.parseStringsByType(AttributeDefinition.STRING, -2, values), 
                contains(values));
        assertThat((String)ConfigurationUtils.parseStringsByType(AttributeDefinition.STRING, 0, new String[] {"blah"}), 
                is("blah"));
    }

    /**
     * Test check a map for configuration values.
     * Verify if the map is invalid exception is throw
     */
    @Test
    public final void testInvalidMap()
    {
        //an empty map is invalid for a component
        Map<String, Object> properties = new HashMap<String, Object>();
        try
        {
            ConfigurationUtils.configPropertiesSet(properties);
            fail("Expecting exception the map doesn't contain default properties.");
        }
        catch (IllegalArgumentException e)
        {
            //expecting exception
        }
    }

    /**
     * Test check a map for configuration values.
     * Verify if the map contains only default properties that false is returned.
     */
    @Test
    public final void testComponentMap()
    {
        //prop map
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(ComponentConstants.COMPONENT_NAME, "name");
        properties.put(ComponentConstants.COMPONENT_ID, 2);
        boolean result = ConfigurationUtils.configPropertiesSet(properties);
        //verify
        assertThat(result, is(false));
    }

    /**
     * Test check a map for configuration values.
     * Verify if the map default properties and additional, presumably configuration properties that true is returned.
     */
    @Test
    public final void testComponentConfigurationMap()
    {
        //prop map
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(ComponentConstants.COMPONENT_NAME, "name");
        properties.put(ComponentConstants.COMPONENT_ID, 2);
        properties.put("I am a configuration value", "value");
        boolean result = ConfigurationUtils.configPropertiesSet(properties);
        //verify
        assertThat(result, is(true));
    }
    
    /**
     * Test that the conversion method will take a dictionary and create a map with all the same properties.
     */
    @Test
    public final void testConvertDictionaryPropsToMap()
    {
        Dictionary<String, Object> dictProps = new Hashtable<String, Object>();
        dictProps.put("a", 50);
        dictProps.put("b", "test");
        dictProps.put("c", 71.56);
        
        Map<String, Object> mapProps = ConfigurationUtils.convertDictionaryPropsToMap(dictProps);
        
        assertThat(mapProps.size(), is(3));
        assertThat((Integer)mapProps.get("a"), is(50));
        assertThat((String)mapProps.get("b"), is("test"));
        assertThat((Double)mapProps.get("c"), is(71.56));
    }
    
    /**
     * Test that the clone method will copy a dictionary into a new object.
     */
    @Test
    public final void testCloneDictionary()
    {
        Dictionary<String, Object> original = new Hashtable<String, Object>();
        original.put("a", 50);
        original.put("b", "test");
        original.put("c", 71.56);
        
        Dictionary<String, Object> clone = ConfigurationUtils.cloneDictionary(original);
        
        assertThat(original, not(sameInstance(clone)));
        assertThat(clone.size(), is(3));
        assertThat((Integer)clone.get("a"), is(50));
        assertThat((String)clone.get("b"), is("test"));
        assertThat((Double)clone.get("c"), is(71.56));
    }
}
