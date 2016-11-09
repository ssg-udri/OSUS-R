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

import java.util.List;

/**
 * Interface for the intermediary model for a configuration property and associated meta type information. This
 * interface contains no setter methods.
 * 
 * @author cweisenborn
 */
public interface UnmodifiablePropertyModel
{

    /**
     * Method that retrieves the key of the property.
     * 
     * @return
     *         The key of the property.
     */
    String getKey();

    /**
     * Method that retrieves the name of the property.
     * 
     * @return
     *          The name of the property.
     */
    String getName();

    /**
     * Method that retrieves the description of the property.
     * 
     * @return
     *          The description of the property.
     */
    String getDescription();

    /**
     * Method that retrieves the value of the configuration property. If no value has been set then the default values 
     * for the property is returned.
     * 
     * @return
     *          The value of property. Returns the default value if no value is currently set.
     */
    Object getValue();

    /**
     * Retrieves the default values for the configuration property.
     * 
     * @return
     *          List of strings that represents the default values for the configuration property.
     */
    List<String> getDefaultValues();

    /**
     * Retrieves the list of optional choices.
     * 
     * @return
     *          A list of options
     */
    List<OptionModel> getOptions();
    
    /**
     * Retrieve the class type of the value.
     * 
     * @return
     *          The class type of the value.
     */
    Class<?> getType();
    
    /**
     * Whether this property is a boolean type, same as checking if {@link #getType()} is equal to {@link Boolean} 
     * class.
     * 
     * @return
     *      true if type is a boolean type
     */
    boolean isBooleanType();

    /**
     * Retrieve the cardinality of the property.
     * 
     * @return
     *          Integer that represents the cardinality of the property.
     */
    int getCardinality();

    /**
     * Whether is property is required or optional.
     * 
     * @return
     *      True if the property is required, false otherwise.
     */
    boolean isRequired();
}
