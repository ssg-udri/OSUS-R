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
package mil.dod.th.ose.gui.webapp.comms;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import com.google.common.base.Strings;

import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.ose.shared.StringUtils;

/**
 * Converts between a String and a {@link PhysicalLinkTypeEnum}.
 * @author dhumeniuk
 *
 */
@FacesConverter(forClass = PhysicalLinkTypeEnum.class)
public class PhysicalLinkTypeConverter implements Converter
{

    @Override
    public Object getAsObject(final FacesContext context, final UIComponent comp, final String value)
    {
        if (Strings.isNullOrEmpty(value))
        {
            return null;
        }
        
        return PhysicalLinkTypeEnum.fromValue(value.replace(" ", ""));
    }

    @Override
    public String getAsString(final FacesContext context, final UIComponent comp, final Object value)
    {
        if (value == null)
        {
            return null;
        }
        
        final PhysicalLinkTypeEnum type = (PhysicalLinkTypeEnum)value;
        return StringUtils.splitCamelCase(type.value());
    }

}
