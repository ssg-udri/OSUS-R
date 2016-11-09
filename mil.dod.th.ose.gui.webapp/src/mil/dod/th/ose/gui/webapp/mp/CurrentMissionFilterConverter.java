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
package mil.dod.th.ose.gui.webapp.mp;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import mil.dod.th.core.mp.Program.ProgramStatus;

/**
 * Class in charge of converting between a string version of {@link ProgramStatus} and the 
 * actual object implementation.
 * @author nickmarcucci
 *
 */
@FacesConverter(value = "missionFilterConverter")
public class CurrentMissionFilterConverter implements Converter
{

    @Override
    public Object getAsObject(final FacesContext context, final UIComponent component, final String value)
    {
        return ProgramStatus.valueOf(value);
    }

    @Override
    public String getAsString(final FacesContext context, final UIComponent component, final Object value)
    {
        return ((ProgramStatus)value).toString();
    }

}
