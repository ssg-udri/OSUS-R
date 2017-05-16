package ${package};

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

import mil.dod.th.core.ConfigurationConstants;

import ${basePackage}.${attrType}Attributes;

/**
 * Interface which defines the configurable properties for ${class}.
 */
@OCD (description = ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION)
public interface ${class}Attributes extends ${attrType}Attributes
{
    // ${task}: Replace example configuration properties with properties specific to the plug-in. 
        
    /**
     * Each method annotated with @AD becomes a configuration property available to the plug-in. The return type of each
     * method is the type of the configuration property. All simple types are supported and other types are supported if
     * they can be converted from a string (e.g., class with a constructor accepting a string or an enum). Also, the 
     * type can be an array or a collection for properties with multiple values.
     * @return
     *      the current int configuration value
     */
    
    @AD(required = false, // whether the property must be supplied when updating the configuration
        deflt = "1", // default value of the property as a string (only applicable if required is false)
        min = "1", // minimum value of the property as a string (not required)
        max = "100", // maximum value of the property as a string (not required)
        name = "Some Property", // display name of the property
        description = "Put in a description") // description of the property shown when editing the property
    int someProperty(); // method name becomes the property key and is used to generate the name if not provided above
    
    /**
     * Example property enum.
     */
    enum X 
    { 
        /** A value. */
        A, 
        
        /** B value. */
        B,
        
        /** C value. */
        C; 
    }
    
    /**
     * Example of a property using {@link X} type.
     * @return
     *      current enum configuration value
     */
    @AD
    X propertyRestrictedToX();
    
    /**
     * Example of a property using a type with a string constructor.
     * @return
     *      current URL configuration value
     */
    @AD
    java.net.URL url();
    
    /**
     * Example of a property using a type with an array.
     * @return
     *      current string configuration values
     */
    @AD
    String[] names();
}
