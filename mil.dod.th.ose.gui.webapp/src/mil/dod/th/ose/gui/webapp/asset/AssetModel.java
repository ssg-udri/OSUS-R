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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import mil.dod.th.core.asset.Asset.AssetActiveStatus;
import mil.dod.th.core.asset.AssetAttributes;
import mil.dod.th.core.asset.capability.CommandCapabilities;
import mil.dod.th.core.types.command.CommandTypeEnum;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.ose.gui.webapp.LocationModel;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ConfigurationWrapper;
import mil.dod.th.ose.gui.webapp.advanced.configuration.UnmodifiablePropertyModel;
import mil.dod.th.ose.gui.webapp.factory.AbstractFactoryBaseModel;
import mil.dod.th.ose.gui.webapp.factory.FactoryObjMgr;
import mil.dod.th.ose.gui.webapp.general.StatusCapable;
import mil.dod.th.ose.gui.webapp.utils.ArgumentValidatorUtil.ValidationEnum;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.Coordinates;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.Orientation;

import org.primefaces.event.ToggleEvent;
import org.primefaces.model.Visibility;

/**
 * This model is used to visually represent an asset. All values changed, like the name will be updated through
 * other interfaces which will request changes to the actual asset. 
 * @author callen
 *
 */
public class AssetModel extends AbstractFactoryBaseModel implements StatusCapable
{
    /**
     * This model's 'widgetVar' used to individually represent interactions with this asset to the DOM.
     * This widgetVar's value will be a string representation of the asset's UUID. This variable is used for
     * individually representing the asset's 'edit name' component.
     */
    private final String m_WidgetVarName;
    
    /**
     * Fully qualified class name of the asset.
     */
    private final String m_Type;
    
    /**
     * The reference to the asset types mgr.
     */
    private final AssetTypesMgr m_AssetTypesMgr;
    
    /**
     * Location of the asset.
     */
    private final LocationModel m_Location = new LocationModel();
    
    /**
     * Whether the asset's location is being currently edited.
     */
    private boolean m_EditingLocation;
    
    /**
     * The asset's active status.
     */
    private AssetActiveStatus m_ActiveStatus;
    
    /**
     * The asset's summary status.
     */
    private SummaryStatusEnum m_Summary;
    
    /**
     * Asset summary status description. 
     */
    private String m_SummaryDescription;
    
    /**
     * The reference to the asset image class.
     */
    private final AssetImage m_AssetImage;
    
    /**
     * Flag denoting if the represented Asset's location fields are open or close.
     */
    private boolean m_IsLocationCollapsed;

    private List<String> m_SensorIdList;

    private String m_SensorId;
    
    /**
     * Public constructor for creating an asset model. All other values like the feature support
     * will be set to false for booleans and "unknown" for string fields.
     * @param controllerId
     *     the system id to which this asset model represents an asset of
     * @param uuid
     *     the UUID of the asset which this model represents
     * @param pid
     *     the pid of the asset
     * @param type
     *     the fully qualified class name of the asset
     * @param mgr
     *     the factory object's manager
     * @param configWrapper
     *      reference to the configuration wrapper bean
     * @param typesMgr
     *      the asset types manager
     * @param imageInterface
     *      the image interface to use when displaying images for this asset.
     */
    public AssetModel(final int controllerId, final UUID uuid, final String pid, final String type, 
            final FactoryObjMgr mgr, final ConfigurationWrapper configWrapper, final AssetTypesMgr typesMgr, 
            final AssetImage imageInterface)
    {
        super(controllerId, uuid, pid, type, mgr, configWrapper);
        
        m_AssetTypesMgr = typesMgr;
        m_Type = type;

        //widget vars
        m_WidgetVarName = "editNameWid" +  uuid.toString().replace('-', 'R');

        m_AssetImage = imageInterface;

        m_SensorIdList = new ArrayList<>();
    }

    /**
     * Set whether the asset is editing it's location.
     * @param editing
     *      boolean of whether or not the asset's location is being edited
     */
    public void setEditingLocation(final boolean editing)
    {
        m_EditingLocation = editing;
    }
    
    /**
     * Get whether the asset is editing it's location.
     * @return
     *      boolean of whether the asset is editing it's location
     */
    public boolean isEditingLocation()
    {
        return m_EditingLocation;
    }
    
    /**
     * Get whether or not the asset's position is readable. When the override position property is false, location 
     * information is set/retrieved via position commands and stored in a data store.
     * @return
     *      whether or not the asset's position is readable.
     */
    public boolean isReadableAsync()
    {
        if (isPositionGetAvailableAsync())
        {
            return true;
        }
        
        final UnmodifiablePropertyModel prop = getPropertyAsync(AssetAttributes.CONFIG_PROP_PLUGIN_OVERRIDES_POSITION);
        
        if (prop != null)
        {
            return !(Boolean)prop.getValue();
        }
        
        //if no config props
        return false;
    }
    
    /**
     * Get whether or not the asset's position is editable. When the override position property is false, location 
     * information is set/retrieved via position commands and stored in a data store.
     * @return
     *      whether or not the asset's position is available
     */
    public boolean isEditableAsync()
    {
        if (isPositionSetAvailableAsync())
        {
            return true;
        }
        
        final UnmodifiablePropertyModel prop = getPropertyAsync(AssetAttributes.CONFIG_PROP_PLUGIN_OVERRIDES_POSITION);
        
        if (prop != null)
        {
            return !(Boolean)prop.getValue();
        }
        
        //if no config props
        return false;
    }
    
    /**
     * Get the location of the asset.
     * 
     * @return
     *      model representing the location
     */
    public LocationModel getLocation()
    {
        return m_Location;
    }
    
    /**
     * Set the coordinates of the asset.
     * 
     * @param coords
     *      the coordinates of the asset or <code>null</code> to clear values
     */
    public void setCoordinates(final Coordinates coords)
    {
        if (coords == null)
        {
            m_Location.setLongitude(null);
            m_Location.setLatitude(null);
            m_Location.setAltitude(null);
        }
        else
        {
            m_Location.setLongitude(coords.getLongitude().getValue());
            m_Location.setLatitude(coords.getLatitude().getValue());
            m_Location.setAltitude(coords.getAltitude().getValue());
        }
    }
    
    /**
     * Set the orientation of the asset.
     * 
     * @param orien
     *      the orientation of the asset or <code>null</code> to clear values
     */
    public void setOrientation(final Orientation orien)
    {
        if (orien == null)
        {
            m_Location.setHeading(null);
            m_Location.setElevation(null);
            m_Location.setBank(null);
        }
        else
        {
            m_Location.setHeading(orien.getHeading().getValue());
            m_Location.setElevation(orien.getElevation().getValue());
            m_Location.setBank(orien.getBank().getValue());
        }
    }

    /**
     * Get the variable name of the widget.
     * @return
     *      the variable name of the widget
     */
    public String getWidgetVarName()
    {
        return m_WidgetVarName;
    }
    
    /**
     * Get the active status of the asset. May return Deactivated if the status is not yet known.
     * @return
     *    the active status of the asset
     */
    public AssetActiveStatus getActiveStatus()
    {
        return m_ActiveStatus;
    }
    
    /**
     * Set the status of the asset.
     * @param status
     *    the active status of the asset
     */
    public void setActiveStatus(final AssetActiveStatus status)
    {
        m_ActiveStatus = status;
    }
    
    /**
     * Set the summary of the asset.
     * @param summary
     *    the summary to set for the asset
     */
    public void setSummary(final SummaryStatusEnum summary)
    {
        m_Summary = summary;
    }
    
    /**
     * Get the summary description of the asset.
     * @return
     *    the summary description of the asset or an empty string if the description is null
     */
    public String getSummaryDescription()
    {
        if (m_SummaryDescription == null)
        {
            return "";
        }
        else
        {
            return m_SummaryDescription;
        }
    }
    
    /**
     * Set the summary description of the asset.
     * @param summaryDescription
     *    the summary to set for the asset
     */
    public void setSummaryDescription(final String summaryDescription)
    {
        m_SummaryDescription = summaryDescription;
    }
    
    /**
     * Get the data capture support of the asset.
     * @return
     *     the boolean representing if data capturing is supported
     */
    public boolean isDataCaptureSupportAsync()
    {
        final CommandCapabilities commands = getAssetFactoryCommandCapabilitiesAsync();
            
        //make sure commands are supported
        if (commands != null)
        {
            return commands.isCaptureData();
        }
        
        return false;
    }
    
    
    /**
     * Get the BIT support of the asset.
     * @return
     *     the boolean value representing if the asset has a BIT
     */
    public boolean isBitSupportAsync()
    {
        final CommandCapabilities commands = getAssetFactoryCommandCapabilitiesAsync();
        
        //make sure commands are supported
        if (commands != null)
        {
            return commands.isPerformBIT();
        }
        
        return false;
    }
    
    /**
     * Get the flag value for if this asset is active.
     * @return
     *     the boolean value representing if the asset is start up enabled
     */
    public boolean isActive()
    {
        return m_ActiveStatus == AssetActiveStatus.ACTIVATED;
    }
    
    /**
     * Get the fully qualified class name of the asset.
     * @return assetType
     *     the fully qualified class name of the asset.
     */
    public String getType()
    {
        return m_Type;
    }
    
    /**
     * Set the location toggle.
     * @param toggledOpen
     *      toggle event that is posted when the associated fieldset is opened or closed
     */
    public void setLocationToggle(final ToggleEvent toggledOpen)
    {
        if (toggledOpen.getVisibility() == Visibility.VISIBLE)
        {
            m_IsLocationCollapsed = false;
        }
        else
        {
            m_IsLocationCollapsed = true;
        }
    }
    
    /**
     * Get the location toggle.
     * @return
     *      <code> true </code> if collapsed, <code> false </code> if open
     */
    public boolean isLocationToggle()
    {
        return m_IsLocationCollapsed;
    }

    /**
     * Get whether the asset supports sensor IDs.
     * 
     * @return
     *      <code>true</code> if sensor ID is supported, <code>false</code> otherwise
     */
    public boolean hasSensorId()
    {
        if (!m_SensorIdList.isEmpty())
        {
            return true;
        }

        final CommandCapabilities commands = getAssetFactoryCommandCapabilitiesAsync();
        if (commands != null)
        {
            return commands.isCaptureDataBySensor();
        }

        return false;
    }

    public List<String> getSensorIds()
    {
        return m_SensorIdList;
    }

    public String getSensorId()
    {
        return m_SensorId;
    }

    /**
     * Update the sensor ID list with the current value last set by {@link #setSensorId(String)}.
     */
    public void updateSensorIds()
    {
        if (m_SensorId != null && !m_SensorIdList.contains(m_SensorId))
        {
            m_SensorIdList.add(m_SensorId);
        }
    }

    public void setSensorId(final String sensorId)
    {
        m_SensorId = sensorId == null ? null : sensorId.trim();
    }

    /**
     * Validate the values for the location fields.
     * @param context
     *          The current faces context, method interface is defined by JSF so parameter is required even if not used
     * @param component
     *          The JSF component calling the validate method.
     * @param value
     *          The value being validated.
     * @throws ValidatorException
     *          Thrown value passed is not valid.
     */
    public void validateValue(final FacesContext context, final UIComponent component, 
            final Object value) throws ValidatorException
    {
        ValidationEnum result = ValidationEnum.PARSING_FAILURE;
        
        final Double parsedValue = Double.parseDouble(value.toString());
        
        if (component.getId().equals("assetLatLocation"))
        {
            final int latMin = -90;
            final int latMax = 90;
            result = minMaxCheckInclusive(latMin, latMax, parsedValue);
        }
        else if (component.getId().equals("assetLongLocation"))
        {
            final int longMin = -180;
            final int longMax = 180;
            result = minMaxCheckInclusive(longMin, longMax, parsedValue);
        }
        else if (component.getId().equals("assetAltitudeLocation"))
        {
            final int altMin = -10000;
            final int altMax = 50000;
            result = minMaxCheckInclusive(altMin, altMax, parsedValue);
        }
        else if (component.getId().equals("assetBankLocation"))
        {
            final int bankMin = -180;
            final int bankMax = 180;
            result = minMaxCheckInclusive(bankMin, bankMax, parsedValue);
        }
        else if (component.getId().equals("assetElevationLocation"))
        {
            final int eleMin = -90;
            final int eleMax = 90;
            result = minMaxCheckInclusive(eleMin, eleMax, parsedValue);
        }
        else if (component.getId().equals("assetHeadingLocation"))
        {
            final int headMin = 0;
            final int headMax = 360;
            if (parsedValue < headMin)
            {
                result = ValidationEnum.LESS_THAN_MIN;
            }
            else if (parsedValue >= headMax)
            {
                result = ValidationEnum.GREATER_THAN_MAX;
            }
            else
            {
                result = ValidationEnum.PASSED;
            }
        }
        
        if (result != ValidationEnum.PASSED)
        {
            final FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, result.toString(), null);
            throw new ValidatorException(msg);
        }
    }
    
    /**
     * Find if the value is above the max or below the min and return the enum determining if it is valid.
     * @param min
     *      the minimum value the parsed value can have
     * @param max
     *      the maximum value the parsed value can have
     * @param value
     *      the value to check
     * @return
     *      the validation enum determining if the value passed validation or not
     */
    public ValidationEnum minMaxCheckInclusive(final int min, final int max, final double value)
    {
        if (value < min)
        {
            return ValidationEnum.LESS_THAN_MIN;
        }
        else if (value > max)
        {
            return ValidationEnum.GREATER_THAN_MAX;
        }
        
        return ValidationEnum.PASSED;
    }
    
    /**
     * Get the command capabilities for the asset factory model for this asset.
     * @return
     *      the command capabilities for the asset
     */
    private CommandCapabilities getAssetFactoryCommandCapabilitiesAsync()
    {
        final AssetFactoryModel factory = getAssetFactoryModelAsync();
        
        if (factory != null && factory.getFactoryCaps() != null)
        {
            return factory.getFactoryCaps().getCommandCapabilities();
            
        }
        
        return null;
    }
    
    /**
     * Get the factory model for this asset from the asset types manager.  
     * @return
     *      the factory model, returns null if no factory model was found, or if the factory model does not have
     *          factory capabilities or command capabilities.
     */
    private AssetFactoryModel getAssetFactoryModelAsync()
    {
        return m_AssetTypesMgr.getAssetFactoryForClassAsync(super.getControllerId(), getType());
    }
    
    /**
     * Get whether the asset's capability to set location is available.
     * @return
     *      boolean of whether the asset's set location is available
     */
    private boolean isPositionSetAvailableAsync()
    {
        final CommandCapabilities commands = getAssetFactoryCommandCapabilitiesAsync();
        
        //make sure commands are supported
        if (commands != null)
        {
            return commands.getSupportedCommands().contains(CommandTypeEnum.SET_POSITION_COMMAND);
        }
        
        return false;
    }
    
    /**
     * Get whether the asset's position is available.
     * @return
     *      whether the asset's positions is available
     */
    private boolean isPositionGetAvailableAsync()
    {
        final CommandCapabilities commands = getAssetFactoryCommandCapabilitiesAsync();
        
        //make sure commands are supported
        if (commands != null)
        {
            return commands.getSupportedCommands().contains(CommandTypeEnum.GET_POSITION_COMMAND);
        }
        
        return false;
    }
    
    /**
     * Get the image for this asset model.
     * @return
     *      return the asset image
     */
    @Override
    public String getImage()
    {
        final AssetFactoryModel factory = getAssetFactoryModelAsync();
        
        if (factory == null)
        {
            return m_AssetImage.getImage(null);
        }
        
        return m_AssetImage.getImage(factory.getFactoryCaps());
    }
    
    //For AssetModel complete will be satisfied when: 
    //activated status and capabilities exists for asset model, asset name is present, and summary status is present
    @Override
    public boolean isComplete()
    {
        //check to make sure the abstract factory base model is complete
        if (!super.isComplete())
        {
            return false;
        }
        
        //check if the summary has not been retrieved
        if (m_Summary == null)
        {
            return false;
        }
        
        //check that since summary has been retrieved if there is a message
        if (m_SummaryDescription == null)
        {
            return false;
        }
        
        //check if the status has not been retrieved
        if (m_ActiveStatus == null)
        {
            return false;
        }
        
        //check if the asset capabilities are not available
        if (getAssetFactoryCommandCapabilitiesAsync() == null)
        {
            return false;
        }
        
        return true;
    }

    @Override
    public SummaryStatusEnum getSummaryStatus()
    {
        return m_Summary;
    }
}
