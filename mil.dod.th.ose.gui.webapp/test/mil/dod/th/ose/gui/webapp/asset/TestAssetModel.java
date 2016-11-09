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
package mil.dod.th.ose.gui.webapp.asset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.faces.component.UIComponent;
import javax.faces.validator.ValidatorException;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.Asset.AssetActiveStatus;
import mil.dod.th.core.asset.capability.AssetCapabilities;
import mil.dod.th.core.asset.capability.CommandCapabilities;
import mil.dod.th.core.types.SensingModality;
import mil.dod.th.core.types.SensingModalityEnum;
import mil.dod.th.core.types.command.CommandTypeEnum;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ConfigurationWrapper;
import mil.dod.th.ose.gui.webapp.factory.FactoryObjMgr;
import mil.dod.th.ose.gui.webapp.utils.ArgumentValidatorUtil.ValidationEnum;
import mil.dod.th.ose.gui.webapp.utils.FacesContextUtil;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.BankDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.Coordinates;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.ElevationDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.HaeMeters;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.HeadingDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.LatitudeWgsDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.LongitudeWgsDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.Orientation;

import org.junit.Test;
import org.primefaces.event.ToggleEvent;
import org.primefaces.model.Visibility;

/**
 * Test the ability for the asset model to correctly store and return proper values.
 * @author callen
 *
 */
public class TestAssetModel 
{
    private static final String ASSET_ICON_DFLT = "thoseIcons/default/defaultImage.png";
    private static final String ASSET_ICON_ACOUSTIC = "thoseIcons/sensingModality/acoustic.png";
    private static final String ASSET_PID = "pid";
    private static final String ASSET_NAME = "James";
    
    private UUID m_Uuid = UUID.randomUUID();
   
    private final ConfigurationWrapper m_ConfigWrapper = mock(ConfigurationWrapper.class);
    private final AssetTypesMgr m_AssetTypesMgr = mock(AssetTypesMgr.class);
    private final FacesContextUtil m_FacesUtil = mock(FacesContextUtil.class);
    private final AssetImage m_AssetImageInterface = new AssetImage();
    private final FactoryObjMgr m_FactoryMgr = mock(FactoryObjMgr.class);
    
    /**
     * Test the default/initial values set for a model at creation.
     * 
     * Verify that all values are returned as documented.
     */
    @Test
    public void testDefaultValues()
    {
        //create asset model
        AssetModel model = new AssetModel(123, m_Uuid, ASSET_PID, Asset.class.getName(), 
                m_FactoryMgr, m_ConfigWrapper, m_AssetTypesMgr, m_AssetImageInterface);

        //verify default values
        assertThat(model.getActiveStatus(), is(nullValue()));
        assertThat(model.getSummaryStatus(), is(nullValue()));
        assertThat(model.getName(), is("Unknown (" + m_Uuid + ")"));
        assertThat(model.isActive(), is(false));
        assertThat(model.isBitSupportAsync(), is(false));
        assertThat(model.isDataCaptureSupportAsync(), is(false));
        assertThat(model.getUuid(), is(m_Uuid));
        assertThat(model.getControllerId(), is(123));
        assertThat(model.getPid(), is(ASSET_PID));
        assertThat(model.getWidgetVarName(), is("editNameWid" + m_Uuid.toString().replace('-', 'R')));
    }
    
    /**
     * Test setter methods of the asset model.
     * 
     * Verify that isX methods update appropriately to newly set values.
     */
    @Test
    public void testSetters()
    {
        //create asset model
        AssetModel model = new AssetModel(123, m_Uuid, ASSET_PID, Asset.class.getName(), 
                m_FactoryMgr, m_ConfigWrapper, m_AssetTypesMgr, m_AssetImageInterface);
        
        assertThat(model.isComplete(), is(false));
        
        //update the ASSET_NAME
        model.updateName(ASSET_NAME);
        assertThat(model.getName(), is(ASSET_NAME));
        
        model.setName("test");
        assertThat(model.getWorkingName(), is("test"));
        
        //set active status
        model.setActiveStatus(AssetActiveStatus.ACTIVATING);
        //check that isActive returns false
        assertThat(model.isActive(), is(false));
        //set active status to activated
        model.setActiveStatus(AssetActiveStatus.ACTIVATED);
        assertThat(model.isActive(), is(true));
        
        //set the summary
        model.setSummary(SummaryStatusEnum.OFF);
        assertThat(model.getSummaryStatus(), is(SummaryStatusEnum.OFF));
        
        //set the pid 
        model.setPid("ChangedPid");
        assertThat(model.getPid(), is ("ChangedPid"));
        
        model.setPid(null);
        assertThat(model.getPid(), is(""));
        
        //set coordinates
        HaeMeters haeMeters = HaeMeters.newBuilder().setValue(200d).build();
        LatitudeWgsDegrees lam = LatitudeWgsDegrees.newBuilder().setValue(-85d).build();
        LongitudeWgsDegrees lom = LongitudeWgsDegrees.newBuilder().setValue(40d).build();
        Coordinates c = Coordinates.newBuilder().setLongitude(lom).setAltitude(haeMeters).setLatitude(lam).build();
        
        model.setCoordinates(c);
        assertThat(Double.valueOf(model.getLocation().getLatitude().toString()), is(-85d));
        assertThat(Double.valueOf(model.getLocation().getLongitude().toString()), is(40d));
        assertThat(Double.valueOf(model.getLocation().getAltitude().toString()), is(200d));
        
        //set orientation
        ElevationDegrees em = ElevationDegrees.newBuilder().setValue(400d).build();
        HeadingDegrees hm = HeadingDegrees.newBuilder().setValue(180d).build();
        BankDegrees bank = BankDegrees.newBuilder().setValue(88d).build();
        Orientation o = Orientation.newBuilder().setBank(bank).setElevation(em).setHeading(hm).build();
        
        model.setOrientation(o);
        assertThat(Double.valueOf(model.getLocation().getElevation().toString()), is(400d));
        assertThat(Double.valueOf(model.getLocation().getBank().toString()), is(88d));
        assertThat(Double.valueOf(model.getLocation().getHeading().toString()), is(180d));
    }
    
    /**
     * Verify that the capabilities are returned as false when the factory returned is null.
     */
    @Test
    public void testCapabilitiesSupportFactoryNull()
    {
        //create asset model and mocked objects
        AssetModel model = new AssetModel(123, m_Uuid, ASSET_PID, Asset.class.getName(), 
                m_FactoryMgr, m_ConfigWrapper, m_AssetTypesMgr, m_AssetImageInterface);
        
        assertThat(model.isDataCaptureSupportAsync(), is(false));
        assertThat(model.isBitSupportAsync(), is(false));
        assertThat(model.isReadableAsync(), is(false));
        assertThat(model.isEditableAsync(), is(false));
    }
    
    /**
     * Verify that the capabilities are returned as false when capabilities are returned as null.
     */
    @Test
    public void testCapabilitiesSupportCapabilitiesNull()
    {
        //create asset model and mocked objects
        AssetModel model = new AssetModel(123, m_Uuid, ASSET_PID, Asset.class.getName(), 
                m_FactoryMgr, m_ConfigWrapper, m_AssetTypesMgr, m_AssetImageInterface);
        AssetFactoryModel factoryModel = mock(AssetFactoryModel.class);
        
        when(m_AssetTypesMgr.getAssetFactoryForClassAsync(123, Asset.class.getName())).thenReturn(factoryModel);
        
        assertThat(model.isDataCaptureSupportAsync(), is(false));
        assertThat(model.isBitSupportAsync(), is(false));
        assertThat(model.isReadableAsync(), is(false));
        assertThat(model.isEditableAsync(), is(false));
    }
    
    /**
     * Verify that the capabilities are returned as false when commands are returned as null.
     */
    @Test
    public void testCapabilitiesSupportCommandsNull()
    {
        AssetFactoryModel afm = mock(AssetFactoryModel.class);
        when(afm.getFactoryCaps()).thenReturn(null);
        when(m_AssetTypesMgr.getAssetFactoryForClassAsync(anyInt(), anyString())).thenReturn(afm);
        
        AssetCapabilities ac = mock(AssetCapabilities.class);
        when(afm.getFactoryCaps()).thenReturn(ac);
        
        //create asset model and mocked objects
        AssetModel model = new AssetModel(123, m_Uuid, ASSET_PID, Asset.class.getName(), 
                m_FactoryMgr, m_ConfigWrapper, m_AssetTypesMgr, m_AssetImageInterface);
        AssetFactoryModel factoryModel = mock(AssetFactoryModel.class);
        AssetCapabilities assetCapabils = mock(AssetCapabilities.class);
        
        when(m_AssetTypesMgr.getAssetFactoryForClassAsync(123, Asset.class.getName())).thenReturn(factoryModel);
        when(factoryModel.getFactoryCaps()).thenReturn(assetCapabils);
        
        assertThat(model.isDataCaptureSupportAsync(), is(false));
        assertThat(model.isBitSupportAsync(), is(false));
        assertThat(model.isReadableAsync(), is(false));
        assertThat(model.isEditableAsync(), is(false));
    }
    
    /**
     * Verify that the capabilities are retrieved from the asset types manager every time the methods are called.
     */
    @Test
    public void testCapabilitiesSupport()
    {
        List<CommandTypeEnum> supportedCommands = new ArrayList<CommandTypeEnum>();
        supportedCommands.add(CommandTypeEnum.GET_POSITION_COMMAND);
        supportedCommands.add(CommandTypeEnum.SET_POSITION_COMMAND);
        
        CommandCapabilities commandCapabils = mock(CommandCapabilities.class);
        when(commandCapabils.isCaptureData()).thenReturn(true);
        when(commandCapabils.isPerformBIT()).thenReturn(true);
        when(commandCapabils.getSupportedCommands()).thenReturn(supportedCommands);
        
        AssetCapabilities ac = new AssetCapabilities();
        ac.setCommandCapabilities(commandCapabils);
        
        AssetFactoryModel afm = mock(AssetFactoryModel.class);
        when(afm.getFactoryCaps()).thenReturn(null);
        when(m_AssetTypesMgr.getAssetFactoryForClassAsync(anyInt(), anyString())).thenReturn(afm);
        when(afm.getFactoryCaps()).thenReturn(ac);
        
        //create asset model and mocked objects
        AssetModel model = new AssetModel(123, m_Uuid, ASSET_PID, Asset.class.getName(), 
                m_FactoryMgr, m_ConfigWrapper, m_AssetTypesMgr, m_AssetImageInterface);
        AssetFactoryModel factoryModel = mock(AssetFactoryModel.class);
        AssetCapabilities assetCapabils = mock(AssetCapabilities.class);
        
        when(m_AssetTypesMgr.getAssetFactoryForClassAsync(123, Asset.class.getName())).thenReturn(factoryModel);
        when(factoryModel.getFactoryCaps()).thenReturn(assetCapabils);
        when(assetCapabils.getCommandCapabilities()).thenReturn(commandCapabils);
        when(commandCapabils.isPerformBIT()).thenReturn(true);
        when(commandCapabils.isCaptureData()).thenReturn(true);
        
        assertThat(model.isDataCaptureSupportAsync(), is(true));
        assertThat(model.isBitSupportAsync(), is(true));
        assertThat(model.isReadableAsync(), is(true));
        assertThat(model.isEditableAsync(), is(true));
    }
    
    /**
     * Test that the widget var is set at creation, and is unique for individual asset models.
     */
    @Test
    public void testUniquenessOfWidgetVar()
    {
        //create first asset model
        AssetModel model1 = new AssetModel(123, m_Uuid, ASSET_PID, Asset.class.getName(), 
                m_FactoryMgr, m_ConfigWrapper, m_AssetTypesMgr, m_AssetImageInterface);
        String wid1 = model1.getWidgetVarName();
        assertThat(model1.getWidgetVarName(), is("editNameWid" + m_Uuid.toString().replace('-', 'R')));

        //create second asset model
        UUID uuid2 = UUID.randomUUID();
        AssetModel model2 = new AssetModel(123, uuid2, ASSET_PID, Asset.class.getName(), 
                m_FactoryMgr, m_ConfigWrapper, m_AssetTypesMgr, m_AssetImageInterface);
        String wid2 = model2.getWidgetVarName();
        assertThat(model2.getWidgetVarName(), is("editNameWid" +  uuid2.toString().replace('-', 'R')));

        //create third asset model
        UUID uuid3 = UUID.randomUUID();
        AssetModel model3 = new AssetModel(123, uuid3, ASSET_PID, Asset.class.getName(), 
                m_FactoryMgr, m_ConfigWrapper, m_AssetTypesMgr, m_AssetImageInterface);
        String wid3 = model3.getWidgetVarName();
        assertThat(model3.getWidgetVarName(), is("editNameWid" +  uuid3.toString().replace('-', 'R')));

        //verify unique values
        assertThat(wid1, not(wid2));
        assertThat(wid3, not(wid2));
        assertThat(wid1, not(wid3));
    }
    
    /**
     * Test that getType returns the asset type specified when the model is constructed
     */
    @Test
    public void testGetType()
    {
        AssetModel model = new AssetModel(0, m_Uuid, ASSET_PID, Asset.class.getName(), 
                m_FactoryMgr, m_ConfigWrapper, m_AssetTypesMgr, m_AssetImageInterface);
        assertThat(model.getType(), is(Asset.class.getName()));
    }
    
    /**
     * Validate all directional measurements and their limits.
     */
    @Test
    public void testValidateValue()
    {
        UIComponent uic = mock(UIComponent.class);

        AssetModel model = new AssetModel(0, m_Uuid, ASSET_PID, Asset.class.getName(), 
                m_FactoryMgr, m_ConfigWrapper, m_AssetTypesMgr, m_AssetImageInterface);
        
        when(uic.getId()).thenReturn("assetLatLocation");
        validateTestMsg(model, uic, -91D, ValidationEnum.LESS_THAN_MIN.toString());
        validateTestMsg(model, uic, 91D, ValidationEnum.GREATER_THAN_MAX.toString());
        model.validateValue(m_FacesUtil.getFacesContext(), uic, 90D);
        model.validateValue(m_FacesUtil.getFacesContext(), uic, -90D);

        when(uic.getId()).thenReturn("assetElevationLocation");
        validateTestMsg(model, uic, -91D, ValidationEnum.LESS_THAN_MIN.toString());
        validateTestMsg(model, uic, 91D, ValidationEnum.GREATER_THAN_MAX.toString());
        model.validateValue(m_FacesUtil.getFacesContext(), uic, 90D);
        model.validateValue(m_FacesUtil.getFacesContext(), uic, -90D);

        when(uic.getId()).thenReturn("assetAltitudeLocation");
        validateTestMsg(model, uic, -10001D, ValidationEnum.LESS_THAN_MIN.toString());
        validateTestMsg(model, uic, 50001D, ValidationEnum.GREATER_THAN_MAX.toString());
        model.validateValue(m_FacesUtil.getFacesContext(), uic, -10000);
        model.validateValue(m_FacesUtil.getFacesContext(), uic, 50000);

        when(uic.getId()).thenReturn("assetBankLocation");
        validateTestMsg(model, uic, -181D, ValidationEnum.LESS_THAN_MIN.toString());
        validateTestMsg(model, uic, 181D, ValidationEnum.GREATER_THAN_MAX.toString());
        model.validateValue(m_FacesUtil.getFacesContext(), uic, -180);
        model.validateValue(m_FacesUtil.getFacesContext(), uic, 180);

        when(uic.getId()).thenReturn("assetLongLocation");
        validateTestMsg(model, uic, -181D, ValidationEnum.LESS_THAN_MIN.toString());
        validateTestMsg(model, uic, 181D, ValidationEnum.GREATER_THAN_MAX.toString());
        model.validateValue(m_FacesUtil.getFacesContext(), uic, -180);
        model.validateValue(m_FacesUtil.getFacesContext(), uic, 180);

        when(uic.getId()).thenReturn("assetHeadingLocation");
        validateTestMsg(model, uic, -1D, ValidationEnum.LESS_THAN_MIN.toString());
        validateTestMsg(model, uic, 360D, ValidationEnum.GREATER_THAN_MAX.toString());
        model.validateValue(m_FacesUtil.getFacesContext(), uic, 0);
        model.validateValue(m_FacesUtil.getFacesContext(), uic, 359);
    }
    
    /**
     * Test the validation for asset directional measurements
     * @param model An AssetModel
     * @param uic the UIComponent being validated
     * @param value the value to validate against
     * @param expectedMsg the message returned from the validation
     */
    private void validateTestMsg(AssetModel model, UIComponent uic, Double value, String expectedMsg)
    {
        try
        {
            model.validateValue(m_FacesUtil.getFacesContext(), uic, value);
            assertThat("Method should have returned with Exception", false);
        }
        catch(ValidatorException ve)
        {
            String result = ve.getFacesMessage().getSummary();
            assertThat(result, is(expectedMsg));
        }
    }
    
    /**
     * Confirm that if an AssetModel has a FactoryModel with Capabilities, then the image is the first sensing modality
     * in the capabilities list, verify that if the AssetModel does not have any capabilities then the image is the 
     * default image.
     */
    @Test
    public void testGetImage()
    {
        AssetModel model = new AssetModel(0, m_Uuid, ASSET_PID, Asset.class.getName(), 
                m_FactoryMgr, m_ConfigWrapper, m_AssetTypesMgr, m_AssetImageInterface);
        String result = model.getImage();
        assertThat(result, is(ASSET_ICON_DFLT));

        AssetFactoryModel afm = mock(AssetFactoryModel.class);
        when(afm.getFactoryCaps()).thenReturn(null);
        when(m_AssetTypesMgr.getAssetFactoryForClassAsync(anyInt(), anyString())).thenReturn(afm);
        result = model.getImage();
        assertThat(result, is(ASSET_ICON_DFLT));
        
        //Test with list size of 0
        List<SensingModality> smList = new ArrayList<SensingModality>();
        AssetCapabilities ac = mock(AssetCapabilities.class);
        when(ac.getModalities()).thenReturn(smList);
        when(afm.getFactoryCaps()).thenReturn(ac);
        result = model.getImage();

        //test with item in list
        SensingModality sm = mock(SensingModality.class);
        smList.add(sm);
        when(sm.getValue()).thenReturn(SensingModalityEnum.ACOUSTIC);
        result = model.getImage();
        assertThat(result, is(ASSET_ICON_ACOUSTIC));
    }
    
    /**
     * Verify the asset model is complete once activated status and capabilities exists for asset model, 
     * and the asset ASSET_NAME and asset position are present.
     * Verify that the model is still considered complete even if the summary description is null/unset.
     */
    @Test
    public void testIsComplete()
    {
        AssetModel model = new AssetModel(0, m_Uuid, ASSET_PID, Asset.class.getName(), 
                m_FactoryMgr, m_ConfigWrapper, m_AssetTypesMgr, m_AssetImageInterface);
        
        //make sure the model is not complete
        assertThat(model.isComplete(), is(false));

        //set the name first as this will fulfill the SUPER isComplete
        model.updateName(ASSET_NAME);
        assertThat(model.getName(), is(ASSET_NAME));
        
        //make sure the model is not complete
        assertThat(model.isComplete(), is(false));

        //set the summary
        model.setSummary(SummaryStatusEnum.GOOD);
        
        //make sure the model is not complete
        assertThat(model.isComplete(), is(false));
        
        //set summary
        model.setSummaryDescription("sd");
        assertThat(model.getSummaryDescription(), is("sd"));
        
        //make sure the model is not complete
        assertThat(model.isComplete(), is(false));
        
        //set the status
        model.setActiveStatus(AssetActiveStatus.ACTIVATED);
        
        //make sure the model is not complete
        assertThat(model.isComplete(), is(false));

        //mock the capabilities
        AssetFactoryModel factoryModel = mock(AssetFactoryModel.class);
        AssetCapabilities assetCapabils = mock(AssetCapabilities.class);
        CommandCapabilities commandCapabils = mock(CommandCapabilities.class);
        
        when(m_AssetTypesMgr.getAssetFactoryForClassAsync(anyInt(), anyString())).thenReturn(factoryModel);
        when(factoryModel.getFactoryCaps()).thenReturn(assetCapabils);
        when(assetCapabils.getCommandCapabilities()).thenReturn(commandCapabils);
        
        assertThat(model.isComplete(), is(true));
    }
    
    /**
     * Verify that the toggle location state is properly translated.
     */
    @Test
    public void testToggleLocation()
    {
        AssetModel model = new AssetModel(0, m_Uuid, ASSET_PID, Asset.class.getName(), 
                m_FactoryMgr, m_ConfigWrapper, m_AssetTypesMgr, m_AssetImageInterface);
        
        //verify default
        assertThat(model.isLocationToggle(), is(false));
        
        //create toggle event, this event is fired when the location is collapsed
        ToggleEvent event = mock(ToggleEvent.class);
        when(event.getVisibility()).thenReturn(Visibility.HIDDEN);
        model.setLocationToggle(event);
        
        //verify change
        assertThat(model.isLocationToggle(), is(true));
        
        //create an open toggle event, this event is fired when the location is opened
        when(event.getVisibility()).thenReturn(Visibility.VISIBLE);
        model.setLocationToggle(event);
        
         //verify reset
        assertThat(model.isLocationToggle(), is(false));
    }
    
    /**
     * Test setting the summary description to null does not cause an error.
     * Verify on 'get' that an empty string is returned.
     */
    @Test
    public void testNullDescription()
    {
        AssetModel model = new AssetModel(0, m_Uuid, ASSET_PID, Asset.class.getName(), 
                m_FactoryMgr, m_ConfigWrapper, m_AssetTypesMgr, m_AssetImageInterface);
        
        model.setSummaryDescription(null);
        
        assertThat(model.getSummaryDescription(), is(""));
    }
}
