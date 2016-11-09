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

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

/**
 * Interface for the intermediary model for a configuration property and associated meta type information. Contains a
 * setter method for the value field and validating the value. This interface is intended to be used on the 
 * configuration page where the model is to be altered on the JSF page and then returned.
 * 
 * @author cweisenborn
 */
public interface ModifiablePropertyModel extends UnmodifiablePropertyModel
{
    /**
     * Method that sets the value for the property. The value is only set internally and can be later used to
     * transmit the altered value to the system containing the configuration.
     * 
     * @param value
     *          The value to be set.
     */
    void setValue(Object value);
    
    /**
     * Method used to validate the configuration property value being set.
     * 
     * @param context
     *          The current faces context.
     * @param component
     *          The JSF component calling the validate method.
     * @param value
     *          The value being validated.
     * @throws ValidatorException
     *          Thrown value passed is not valid.
     */
    void validateValue(final FacesContext context, final UIComponent component, final Object value) 
            throws ValidatorException;
}
