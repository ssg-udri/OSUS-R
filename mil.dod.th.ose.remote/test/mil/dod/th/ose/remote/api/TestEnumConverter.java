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
package mil.dod.th.ose.remote.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Test;

import mil.dod.th.core.asset.Asset.AssetActiveStatus;
import mil.dod.th.core.ccomm.link.LinkLayer.LinkStatus;
import mil.dod.th.core.controller.TerraHarvestController.OperationMode;
import mil.dod.th.core.mp.MissionScript.TestResult;
import mil.dod.th.core.mp.Program.ProgramStatus;
import mil.dod.th.core.persistence.ObservationQuery;
import mil.dod.th.core.remote.proto.AssetMessages;
import mil.dod.th.core.remote.proto.BaseMessages;
import mil.dod.th.core.remote.proto.LinkLayerMessages;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionStatus;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionTestResult;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.SortField;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.SortOrder;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.ose.remote.api.RemoteSettings.EncryptionMode;

/**
 * Test class for the {@link EnumConverter}.
 * 
 * @author cweisenborn
 */
public class TestEnumConverter
{
    /**
     * Verify that all proto asset active status enumerations can be converted to proto equivalents.
     */
    @Test
    public void testConvertProtoAssetActiveStatusToJava()
    {
        for (AssetMessages.AssetActiveStatus status: 
            AssetMessages.AssetActiveStatus.values())
        {
            assertThat(EnumConverter.convertProtoAssetActiveStatusToJava(status),
                    is(notNullValue()));
        }
    }
    
    /**
     * Verify that all java asset active status enumerations can be converted to proto equivalents.
     */
    @Test
    public void testConvertJavaAssetActiveStatusToProto()
    {
        for (AssetActiveStatus status: AssetActiveStatus.values())
        {
            assertThat(EnumConverter.convertJavaAssetActiveStatusToProto(status),
                    is(notNullValue()));
        }
    }
    
    /**
     * Verify that all proto operation mode enumerations can be converted to java equivalents.
     */
    @Test
    public void testConvertProtoOperationModeToJava()
    {
        for (BaseMessages.OperationMode opMode: BaseMessages.OperationMode.values())
        {
            assertThat(EnumConverter.convertProtoOperationModeToJava(opMode),
                    is(notNullValue()));
        }
    }
    
    /**
     * Verify that all java operation mode enumeration can be converted to proto equivalents.
     */
    @Test
    public void testConvertJavaOperationModeToProto()
    {
        for (OperationMode opMode: OperationMode.values())
        {
            assertThat(EnumConverter.convertJavaOperationModeToProto(opMode),
                    is(notNullValue()));
        }
    }
    
    /**
     * Verify that all proto link status enumerations can be converted to java equivalents.
     */
    @Test
    public void testConvertProtoLinkStatusToJava()
    {
        for (LinkLayerMessages.LinkStatus status: LinkLayerMessages.LinkStatus.values())
        {
            assertThat(EnumConverter.convertProtoLinkStatusToJava(status),
                    is(notNullValue()));
        }
    }
    
    /**
     * Verify that all java link status enumerations can be converted to proto equivalents.
     */
    @Test
    public void testConvertJavaLinkStatusToProto()
    {
        for (LinkStatus status: LinkStatus.values())
        {
            assertThat(EnumConverter.convertJavaLinkStatusToProto(status),
                    is(notNullValue()));
        }
    }
    
    /**
     * Verify that all program status enumerations can be converted to mission status equivalents.
     */
    @Test
    public void testConvertProgramStatusToMissionStatus()
    {
        for (ProgramStatus status: ProgramStatus.values())
        {
            assertThat(EnumConverter.convertProgramStatusToMissionStatus(status),
                    is(notNullValue()));
        }
    }
    
    /**
     * Verify that all mission status enumerations can be converted to program status equivalents.
     */
    @Test
    public void testConvertMissionStatusToProgramStatus()
    {
        for (MissionStatus status: MissionStatus.values())
        {
            assertThat(EnumConverter.convertMissionStatusToProgramStatus(status),
                    is(notNullValue()));
        }
    }
    
    /**
     * Verify that all test result enumerations can be converted to mission test result equivalents.
     */
    @Test
    public void testConvertToMissionTestResult()
    {
        for (TestResult result: TestResult.values())
        {
            assertThat(EnumConverter.convertToMissionTestResult(result),
                    is(notNullValue()));
        }
    }
    
    /**
     * Verify that all mission test result enumerations can be converted to test result equivalents.
     */
    @Test
    public void testConvertToTestResult()
    {
        for (MissionTestResult result: MissionTestResult.values())
        {
            assertThat(EnumConverter.convertToTestResult(result),
                    is(notNullValue()));
        }
    }
    
    /**
     * Verify that all proto sort field enumerations can be converted to java equivalents.
     */
    @Test
    public void testConvertProtoSortFieldToJava()
    {
        for (SortField sortField: SortField.values())
        {
            assertThat(EnumConverter.convertProtoSortFieldToJava(sortField),
                    is(notNullValue()));
        }
    }
    
    /**
     * Verify that all java sort field enumerations can be converted to proto equivalents.
     */
    @Test
    public void testConvertJavaSortFieldToProto()
    {
        for (ObservationQuery.SortField sortField: ObservationQuery.SortField.values())
        {
            assertThat(EnumConverter.convertJavaSortFieldToProto(sortField),
                    is(notNullValue()));
        }
    }
    
    /**
     * Verify that all proto sort order enumerations can be converted to java equivalents.
     */
    @Test
    public void testConvertProtoSortOrderToJava()
    {
        for (SortOrder sortOrder: SortOrder.values())
        {
            assertThat(EnumConverter.convertProtoSortOrderToJava(sortOrder),
                    is(notNullValue()));
        }
    }
    
    /**
     * Verify that all java sort order enumerations can be converted to proto equivalents.
     */
    @Test
    public void testConvertJavaSortOrderToProto()
    {
        for (ObservationQuery.SortOrder sortOrder: ObservationQuery.SortOrder.values())
        {
            assertThat(EnumConverter.convertJavaSortOrderToProto(sortOrder),
                    is(notNullValue()));
        }
    }
    
    /**
     * Verify that convert encrypt type to proto function converts the enums appropriately.
     */
    @Test
    public void testConvertEncryptTypeToProto()
    {
        for (EncryptType type: EncryptType.values())
        {
            assertThat(EnumConverter.convertEncryptTypeToEncryptionMode(type), 
                    is(notNullValue()));
        }
    }
    
    /**
     * Verify that the convert encrypt type to java function converts the enums appropriately.
     */
    @Test
    public void testConvertEncryptTypeToJava()
    {
        for (EncryptionMode mode: EncryptionMode.values())
        {
            assertThat(EnumConverter.convertEncryptionModeToEncryptType(mode),
                    is(notNullValue()));
        }
    }
}
