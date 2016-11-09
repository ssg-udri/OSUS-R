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

import java.util.Arrays;

import org.osgi.service.metatype.AttributeDefinition;

import mil.dod.th.core.remote.proto.MetaTypeMessages.AttributeDefinitionType;

/**
 * @author dhumeniuk
 *
 */
public class ModelFactory
{
    public static ConfigPropModelImpl createPropModel(final String key, final String value)
    {
        ConfigPropModelImpl propModel = new ConfigPropModelImpl(createAttributeModel(key, AttributeDefinition.STRING));
        propModel.setValue(value);
        return propModel;
    }

    public static AttributeModel createAttributeModel(String key, int type)
    {
        return createAttributeModel(key, type, "default");
    }

    public static AttributeModel createAttributeModel(String key, int type, String... deflts)
    {
        return new AttributeModel(AttributeDefinitionType.newBuilder()
                .setId(key)
                .setName(key)
                .setDescription("descrip")
                .setCardinality(1)
                .setAttributeType(type)
                .addAllDefaultValue(Arrays.asList(deflts))
                .setRequired(true)
                .build());
    }
}
