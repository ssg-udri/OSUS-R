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
package test.package;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

public final class TestEnumConverter {
    private static final BiMap<mil.dod.th.ose.jaxbprotoconverter.TestEnumConverterFileGenerator$TestEnum, test.proto.ProtoClassName.Test.Enum> VALUE_MAP =
        ImmutableBiMap.<mil.dod.th.ose.jaxbprotoconverter.TestEnumConverterFileGenerator$TestEnum, test.proto.ProtoClassName.Test.Enum>builder()
       .put(mil.dod.th.ose.jaxbprotoconverter.TestEnumConverterFileGenerator$TestEnum.ONE, test.proto.ProtoClassName.Test.Enum.ONE)
       .put(mil.dod.th.ose.jaxbprotoconverter.TestEnumConverterFileGenerator$TestEnum.TWO, test.proto.ProtoClassName.Test.Enum.TWO)
       .build();

    public static test.proto.ProtoClassName.Test.Enum convertJavaEnumToProto(mil.dod.th.ose.jaxbprotoconverter.TestEnumConverterFileGenerator$TestEnum value) {
        if (VALUE_MAP.containsKey(value)) {
            return VALUE_MAP.get(value);
        }
        throw new IllegalArgumentException(String.format("Invalid java enum specified: %s", value));
    }

    public static mil.dod.th.ose.jaxbprotoconverter.TestEnumConverterFileGenerator$TestEnum convertProtoEnumToJava(test.proto.ProtoClassName.Test.Enum value) {
        if (VALUE_MAP.containsValue(value)) {
            return VALUE_MAP.inverse().get(value);
        }
        throw new IllegalArgumentException(String.format("Invalid proto enum specified: %s", value));
    }
}
