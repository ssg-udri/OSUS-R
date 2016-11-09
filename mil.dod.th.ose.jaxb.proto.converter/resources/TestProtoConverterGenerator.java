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

import com.google.protobuf.DescriptorProtos.FileOptions;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.ProtocolMessageEnum;


public class TestEnum {
    public static ProtocolMessageEnum convertJavaEnumToProto(Enum<?> javaValue) {
        switch (javaValue.getClass().getName()) {
            case "mil.dod.th.ose.jaxbprotoconverter.TestEnumConverterFileGenerator$TestEnum":
                return TestEnumConverter.convertJavaEnumToProto((mil.dod.th.ose.jaxbprotoconverter.TestEnumConverterFileGenerator$TestEnum)javaValue);
            default:
                throw new IllegalArgumentException(String.format("Unknown java enum specified: %s", javaValue));
        }
    }

    public static Enum<?> convertProtoEnumToJava(ProtocolMessageEnum protoValue) {
        switch (protoValue.getClass().getName()) {
            case "test.proto.ProtoClassName.Test.Enum":
                return TestEnumConverter.convertProtoEnumToJava((test.proto.ProtoClassName.Test.Enum)protoValue);
            default:
                throw new IllegalArgumentException(String.format("Unknown proto enum specified: %s", protoValue));
        }
    }

    public static Enum<?> convertProtoEnumToJava(EnumValueDescriptor protoValueDesc) {
        FileOptions fileOptions = protoValueDesc.getFile().getOptions();
        String className = fileOptions.getJavaPackage()
            + "." + fileOptions.getJavaOuterClassname()
            + "." + protoValueDesc.getType().getContainingType().getName()
            + "." + protoValueDesc.getType().getName();

        switch (className) {
            case "test.proto.ProtoClassName.Test.Enum":
                return TestEnumConverter.convertProtoEnumToJava(test.proto.ProtoClassName.Test.Enum.valueOf(protoValueDesc));
            default:
                throw new IllegalArgumentException(String.format("Unknown proto enum descriptor specified: %s", protoValueDesc));
        }
    }
}
