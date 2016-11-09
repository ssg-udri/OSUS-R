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

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

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
 * Class that handles converting between Java and protocol buffer equivalent enumerations.
 * 
 * @author cweisenborn
 */
public final class EnumConverter
{
    /**
     * BiMap that contains key value pairs used for converting between {@link AssetActiveStatus} and 
     * {@link mil.dod.th.core.remote.proto.AssetMessages.AssetActiveStatus}.
     */
    private static final BiMap<AssetActiveStatus, AssetMessages.AssetActiveStatus> ASSET_ACTIVE_STATUS =
            ImmutableBiMap.<AssetActiveStatus, AssetMessages.AssetActiveStatus>builder()
            .put(AssetActiveStatus.ACTIVATED, AssetMessages.AssetActiveStatus.ACTIVATED)
            .put(AssetActiveStatus.ACTIVATING, AssetMessages.AssetActiveStatus.ACTIVATING)
            .put(AssetActiveStatus.DEACTIVATED, AssetMessages.AssetActiveStatus.DEACTIVATED)
            .put(AssetActiveStatus.DEACTIVATING, AssetMessages.AssetActiveStatus.DEACTIVATING).build();
    
    /**
     * BiMap that contains key value pairs used for converting between {@link OperationMode} and 
     * {@link mil.dod.th.core.remote.proto.BaseMessages.OperationMode}.
     */
    private static final BiMap<OperationMode, BaseMessages.OperationMode> OPERATION_MODE =
            ImmutableBiMap.<OperationMode, BaseMessages.OperationMode>builder()
            .put(OperationMode.OPERATIONAL_MODE, BaseMessages.OperationMode.OPERATIONAL_MODE)
            .put(OperationMode.TEST_MODE, BaseMessages.OperationMode.TEST_MODE).build();
    
    /**
     * BiMap that contains key value pairs used for converting between {@link LinkStatus} and 
     * {@link mil.dod.th.core.remote.proto.LinkLayerMessages.LinkStatus}.
     */
    private static final BiMap<LinkStatus, LinkLayerMessages.LinkStatus> LINK_STATUS = 
            ImmutableBiMap.<LinkStatus, LinkLayerMessages.LinkStatus>builder()
            .put(LinkStatus.OK, LinkLayerMessages.LinkStatus.OK)
            .put(LinkStatus.LOST, LinkLayerMessages.LinkStatus.LOST).build();
    
    /**
     * BiMap that contains key value pairs used for converting between {@link MissionStatus} and {@link ProgramStatus}.
     */
    private static final BiMap<MissionStatus, ProgramStatus> MISSION_STATUS =
            ImmutableBiMap.<MissionStatus, ProgramStatus>builder()
            .put(MissionStatus.CANCELED, ProgramStatus.CANCELED)
            .put(MissionStatus.EXECUTED, ProgramStatus.EXECUTED)
            .put(MissionStatus.EXECUTING, ProgramStatus.EXECUTING)
            .put(MissionStatus.EXECUTING_TEST, ProgramStatus.EXECUTING_TEST)
            .put(MissionStatus.INITIALIZATION_ERROR, ProgramStatus.INITIALIZATION_ERROR)
            .put(MissionStatus.SCHEDULED, ProgramStatus.SCHEDULED)
            .put(MissionStatus.SCRIPT_ERROR, ProgramStatus.SCRIPT_ERROR)
            .put(MissionStatus.SHUTDOWN, ProgramStatus.SHUTDOWN)
            .put(MissionStatus.SHUTTING_DOWN, ProgramStatus.SHUTTING_DOWN)
            .put(MissionStatus.UNSATISFIED, ProgramStatus.UNSATISFIED)
            .put(MissionStatus.VARIABLE_ERROR, ProgramStatus.VARIABLE_ERROR)
            .put(MissionStatus.WAITING_INITIALIZED, ProgramStatus.WAITING_INITIALIZED)
            .put(MissionStatus.WAITING_UNINITIALIZED, ProgramStatus.WAITING_UNINITIALIZED).build();
    
    /**
     * BiMap that contains key value pairs used for converting between {@link MissionTestResult} and {@link TestResult}.
     */
    private static final BiMap<MissionTestResult, TestResult> TEST_RESULT =
            ImmutableBiMap.<MissionTestResult, TestResult>builder()
            .put(MissionTestResult.FAILED, TestResult.FAILED)
            .put(MissionTestResult.PASSED, TestResult.PASSED).build();
    
    /**
     * BiMap that contains key value pairs used for converting between 
     * {@link mil.dod.th.core.persistence.ObservationQuery.SortField} and {@link SortField}.
     */
    private static final BiMap<ObservationQuery.SortField, SortField> SORT_FIELD = 
            ImmutableBiMap.<ObservationQuery.SortField, SortField>builder()
            .put(ObservationQuery.SortField.CreatedTimestamp, SortField.CreatedTimestamp)
            .put(ObservationQuery.SortField.ObservedTimestamp, SortField.ObservedTimestamp).build();
    
    /**
     * BiMap that contains key value pairs used for converting between 
     * {@link mil.dod.th.core.persistence.ObservationQuery.SortOrder} and {@link SortOrder}.
     */
    private static final BiMap<ObservationQuery.SortOrder, SortOrder> SORT_ORDER =
            ImmutableBiMap.<ObservationQuery.SortOrder, SortOrder>builder()
            .put(ObservationQuery.SortOrder.Ascending, SortOrder.Ascending)
            .put(ObservationQuery.SortOrder.Descending, SortOrder.Descending).build();
    
    /**
     * BiMap that contains key value pairs used for converting between {@link EncryptionMode} and {@link EncryptType}.
     */
    private static final BiMap<EncryptionMode, EncryptType> ENCRYPTION_MODE =
            ImmutableBiMap.<EncryptionMode, EncryptType>builder()
            .put(EncryptionMode.AES_ECDH_ECDSA, EncryptType.AES_ECDH_ECDSA)
            .put(EncryptionMode.NONE, EncryptType.NONE).build();
    
    /**
     * Private constructor to prevent instantiation.
     */
    private EnumConverter()
    {
        
    }
    
    /**
     * Method used to convert proto based 
     * {@link mil.dod.th.core.remote.proto.AssetMessages.AssetActiveStatus} to 
     * {@link AssetActiveStatus}.
     * 
     * @param activeStatus
     *      The proto asset active status enumeration to be converted.
     * @return
     *      The java equivalent asset active status enumeration.
     */
    public static AssetActiveStatus convertProtoAssetActiveStatusToJava(
            final AssetMessages.AssetActiveStatus activeStatus)
    {
        if (ASSET_ACTIVE_STATUS.containsValue(activeStatus))
        {
            return ASSET_ACTIVE_STATUS.inverse().get(activeStatus);
        }
        throw new IllegalArgumentException(
                String.format("Invalid proto asset active status specified: %s", activeStatus));
    }
    
    /**
     * Method used to convert java based 
     * {@link AssetActiveStatus} to 
     * {@link mil.dod.th.core.remote.proto.AssetMessages.AssetActiveStatus}.
     * 
     * @param activeStatus
     *      The java asset active status enumeration to be converted.
     * @return
     *      The proto equivalent asset active status enumeration.
     */
    public static AssetMessages.AssetActiveStatus convertJavaAssetActiveStatusToProto(
            final AssetActiveStatus activeStatus)
    {
        if (ASSET_ACTIVE_STATUS.containsKey(activeStatus))
        {
            return ASSET_ACTIVE_STATUS.get(activeStatus);
        }
        throw new IllegalArgumentException(String.format("Invalid java asset active status specified: %s", 
                activeStatus));
    }
    
    /**
     * Method used to convert proto based {@link mil.dod.th.core.remote.proto.BaseMessages.OperationMode} to 
     * {@link OperationMode}.
     * 
     * @param operationMode
     *      The proto operation mode enumeration to be converted.
     * @return
     *      The java equivalent operation mode enumeration.
     */
    public static OperationMode convertProtoOperationModeToJava(final BaseMessages.OperationMode operationMode)
    {
        if (OPERATION_MODE.containsValue(operationMode))
        {
            return OPERATION_MODE.inverse().get(operationMode);
        }
        throw new IllegalArgumentException(String.format("Invalid proto operation mode specified: %s", 
                operationMode));
    }
    
    /**
     * Method used to convert java based {@link OperationMode} to 
     * {@link mil.dod.th.core.remote.proto.BaseMessages.OperationMode}.
     * 
     * @param operationMode
     *      The java enumeration to be converted.
     * @return
     *      The proto equivalent enumeration.
     */
    public static BaseMessages.OperationMode convertJavaOperationModeToProto(final OperationMode operationMode)
    {
        if (OPERATION_MODE.containsKey(operationMode))
        {
            return OPERATION_MODE.get(operationMode);
        }
        throw new IllegalArgumentException(String.format("Invalid java operation mode specified: %s", 
                operationMode));
    }
    
    /**
     * Method used to convert proto based {@link mil.dod.th.core.remote.proto.LinkLayerMessages.LinkStatus} to 
     * {@link LinkStatus}.
     * 
     * @param status
     *      The proto enumeration to be converted.
     * @return
     *      The java equivalent enumeration.
     */
    public static LinkStatus convertProtoLinkStatusToJava(final LinkLayerMessages.LinkStatus status)
    {
        if (LINK_STATUS.containsValue(status))
        {
            return LINK_STATUS.inverse().get(status);
        }
        throw new IllegalArgumentException(String.format("Invalid proto link status specified: %s", status));
    }
    
    /**
     * Method used to convert java based {@link mil.dod.th.core.remote.proto.LinkLayerMessages.LinkStatus} to 
     * {@link LinkStatus}.
     * 
     * @param status
     *      The java enumeration to be converted.
     * @return
     *      The proto equivalent enumeration.
     */
    public static LinkLayerMessages.LinkStatus convertJavaLinkStatusToProto(final LinkStatus status)
    {
        if (LINK_STATUS.containsKey(status))
        {
            return LINK_STATUS.get(status);
        }
        throw new IllegalArgumentException(String.format("Invalid java link status specified: %s", status));
    }
    
    /**
     * Helper method that translates {@link ProgramStatus} to protocol buffer equivalents.
     * @param status
     *     the program status to translate
     * @return
     *     the protocol buffer equivalent
     * @throws IllegalArgumentException
     *     in the event that a status value is received that does not have a protocol buffer equivalent
     */
    public static MissionStatus convertProgramStatusToMissionStatus(final ProgramStatus status)  //NOCHECKSTYLE:
            throws IllegalArgumentException  //high cyclomatic complexity, need to account for all program statuses
    {   
        if (MISSION_STATUS.containsValue(status))
        {
            return MISSION_STATUS.inverse().get(status);
        }
        throw new IllegalArgumentException(String.format("Invalid program status specified: %s", status));
    }
    
    /**
     * Helper method that translates {@link MissionStatus} to java equivalents.
     * @param status
     *     the mission status to translate
     * @return
     *     the java equivalent
     * @throws IllegalArgumentException
     *     in the event that a status value is received that does not have a java equivalent
     */
    public static ProgramStatus convertMissionStatusToProgramStatus(final MissionStatus status) //NOCHECKSTYLE:
            throws IllegalArgumentException //high cyclomatic complexity, need to account for all program statuses
    {  
        if (MISSION_STATUS.containsKey(status))
        {
            return MISSION_STATUS.get(status);
        }
        throw new IllegalArgumentException(String.format("Invalid mission status specified: %s", status));
    }
    
    /**
     * Helper method that translates {@link TestResult} to protocol buffer equivalents.
     * @param result
     *     the test result to translate
     * @return
     *     the protocol buffer equivalent
     * @throws IllegalArgumentException
     *     in the event that a test result value is received that does not have a protocol buffer equivalent
     */
    public static MissionTestResult convertToMissionTestResult(final TestResult result) throws IllegalArgumentException
    {
        if (TEST_RESULT.containsValue(result))
        {
            return TEST_RESULT.inverse().get(result);
        }
        throw new IllegalArgumentException(String.format("Invalid Test Result: %s", result));
    }
    
    /**
     * Helper method that translates {@link MissionTestResult} to java equivalents.
     * @param result
     *      the test result to translate
     * @return
     *      the java equivalent enumeration
     * @throws IllegalArgumentException
     *      in the event that a test result value is received that does not have a java equivalent
     */
    public static TestResult convertToTestResult(final MissionTestResult result) throws IllegalArgumentException
    {
        if (TEST_RESULT.containsKey(result))
        {
            return TEST_RESULT.get(result);
        }
        throw new IllegalArgumentException(String.format("Invalid Mission Program Test Result", result));
    }
    
    /**
     * Method used to convert proto based {@link SortField} to 
     * {@link mil.dod.th.core.persistence.ObservationQuery.SortField}.
     * 
     * @param sortField
     *      The proto sort field enumeration to be converted.
     * @return
     *      The equivalent java sort field enumeration.
     */
    public static ObservationQuery.SortField convertProtoSortFieldToJava(final SortField sortField)
    {
        if (SORT_FIELD.containsValue(sortField))
        {
            return SORT_FIELD.inverse().get(sortField);
        }
        throw new IllegalArgumentException(String.format("Invalid proto sort field specified: %s", sortField));
    }
    
    /**
     * Method used to convert java based {@link mil.dod.th.core.persistence.ObservationQuery.SortField} to 
     * {@link SortField}.
     * 
     * @param sortField
     *      The java sort field enumeration to be converted.
     * @return
     *      The equivalent proto sort field enumeration.
     */
    public static SortField convertJavaSortFieldToProto(final ObservationQuery.SortField sortField)
    {
        if (SORT_FIELD.containsKey(sortField))
        {
            return SORT_FIELD.get(sortField);
        }
        throw new IllegalArgumentException(String.format("Invalid java sort field specified: %s", sortField));
    }
    
    /**
     * Method used to convert proto based {@link SortOrder} to 
     * {@link mil.dod.th.core.persistence.ObservationQuery.SortOrder}.
     * 
     * @param sortOrder
     *      The proto sort order enumeration to be converted.
     * @return
     *      The equivalent java sort order enumeration.
     */
    public static ObservationQuery.SortOrder convertProtoSortOrderToJava(final SortOrder sortOrder)
    {
        if (SORT_ORDER.containsValue(sortOrder))
        {
            return SORT_ORDER.inverse().get(sortOrder);
        }
        throw new IllegalArgumentException(String.format("Invalid proto sort order specified: %s", sortOrder));
    }
    
    /**
     * Method used to convert java based {@link mil.dod.th.core.persistence.ObservationQuery.SortOrder} to 
     * {@link SortOrder}.
     * 
     * @param sortOrder
     *      The java enumeration to be converted.
     * @return
     *      The equivalent proto enumeration.
     */
    public static SortOrder convertJavaSortOrderToProto(final ObservationQuery.SortOrder sortOrder)
    {
        if (SORT_ORDER.containsKey(sortOrder))
        {
            return SORT_ORDER.get(sortOrder);
        }
        throw new IllegalArgumentException(String.format("Invalid java sort order specified: %s", sortOrder));
    }
    
    /**
     * Method used to convert an {@link EncryptionMode} to it's proto equivalent.
     * 
     * @param mode
     *      The {@link EncryptionMode} to be converted to a proto equivalent.
     * @return
     *      The {@link EncryptType} that is equivalent.
     */
    public static EncryptType convertEncryptionModeToEncryptType(final EncryptionMode mode)
    {
        if (ENCRYPTION_MODE.containsKey(mode))
        {
            return ENCRYPTION_MODE.get(mode);
        }
        throw new IllegalArgumentException(String.format("Unable to convert mode %s to a type!", 
                mode.toString()));
    }
    
    /**
     * Method used to convert an {@link EncryptType} to it's java equivalent.
     * 
     * @param type
     *      The {@link EncryptType} to be converted to a java equivalent.
     * @return
     *      The {@link EncryptionMode} that is equivalent.
     */
    public static EncryptionMode convertEncryptTypeToEncryptionMode(final EncryptType type)
    {
        if (ENCRYPTION_MODE.containsValue(type))
        {
            return ENCRYPTION_MODE.inverse().get(type);
        }
        throw new IllegalArgumentException(String.format("Unable to convert type %s to a mode!", 
                type.toString()));
    }
}
