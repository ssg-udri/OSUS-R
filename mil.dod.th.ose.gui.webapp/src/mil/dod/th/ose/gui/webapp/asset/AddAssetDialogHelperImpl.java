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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import mil.dod.th.core.asset.capability.AssetCapabilities;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace.
            AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetRequestData;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ConfigPropModelImpl;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ConfigurationWrapperImpl;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ModifiablePropertyModel;
import mil.dod.th.ose.gui.webapp.advanced.configuration.UnmodifiableConfigMetatypeModel;
import mil.dod.th.ose.gui.webapp.advanced.configuration.UnmodifiablePropertyModel;
import mil.dod.th.ose.gui.webapp.controller.ActiveControllerImpl;
import mil.dod.th.ose.shared.SharedMessageUtils;

import org.glassfish.osgicdi.OSGiService;
import org.osgi.service.log.LogService;

/**
 * Implementation of the {@link AddAssetDialogHelper}.
 * @author callen
 *
 */
@ManagedBean(name = "addAssetHelper")
@ViewScoped
public class AddAssetDialogHelperImpl implements AddAssetDialogHelper 
{
    /**
     * Holds the name intended for an asset during creation.
     */
    private String m_AssetName;

    /**
     * Holds the currently selected asset factory model.
     */
    private AssetFactoryModel m_Model;

    /**
     * Asset manager service to use.
     */
    @ManagedProperty(value = "#{assetMgr}")
    private AssetMgrImpl assetMgr; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Configuration wrapper service to use.
     */
    @ManagedProperty(value = "#{configWrapper}")
    private ConfigurationWrapperImpl configWrapper; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Active controller service.
     */
    @ManagedProperty(value = "#{activeController}") 
    private ActiveControllerImpl activeController; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Inject the MessageFactory service for creating remote messages.
     */
    @Inject @OSGiService
    private MessageFactory m_MessageFactory;
    
    /**
     * List of properties for the given factory object.
     */
    private List<ModifiablePropertyModel> m_PropertyList;
    
    /**
     * Public constructor.
     */
    public AddAssetDialogHelperImpl()
    {
        m_PropertyList = new ArrayList<ModifiablePropertyModel>();
    }
   
    /**
     * Method that sets the MessageFactory service to use.
     * 
     * @param messageFactory
     *          MessageFactory service to set.
     */
    public void setMessageFactory(final MessageFactory messageFactory)
    {
        m_MessageFactory = messageFactory;
    }
    
    /**
     * Set the asset manager service to use. 
     * @param assetManager
     *     the asset manager service
     */
    public void setAssetMgr(final AssetMgrImpl assetManager)
    {
        assetMgr = assetManager;
    }
    
    /**
     * Set the configuration wrapper service to use.
     * @param configurationWrapper
     *      the configuration wrapper service
     */
    public void setConfigWrapper(final ConfigurationWrapperImpl configurationWrapper)
    {
        configWrapper = configurationWrapper;
    }
    
    /**
     * Set the active controller service to use.
     * @param activeControllerService
     *      the active controller service
     */
    public void setActiveController(final ActiveControllerImpl activeControllerService)
    {
        activeController = activeControllerService;
    }

    @Override
    public void setAssetFactory(final AssetFactoryModel factory) 
    {
        m_Model = factory;
        if (m_Model != null)
        {
            getConfigurationDefaults();
        }
    }

    @Override
    public AssetFactoryModel getAssetFactory() 
    {
        return m_Model;
    }

    @Override
    public boolean isSetAssetFactory() 
    {
        return m_Model != null;
    }
    
    @Override
    public String getNewAssetName()
    {
        return m_AssetName;
    }

    @Override
    public void setNewAssetName(final String name)
    {
        m_AssetName = name;
    }

    @Override
    public void init()
    {
        m_AssetName = "";
        setAssetFactory(null);
    }

    @Override
    public void requestCreateAsset(final String productType, final int controllerId, final String name)
    {   
        final CreateAssetRequestData.Builder request = CreateAssetRequestData.newBuilder().
                setProductType(productType).setName(name);
        if (!m_PropertyList.isEmpty())
        {
            final Map<String, Object> props = new HashMap<>();
            for (ModifiablePropertyModel property: m_PropertyList)
            {
                props.put(property.getKey(), property.getValue());
            }
            request.addAllProperties(SharedMessageUtils.convertMapToListSimpleTypesMapEntry(props));
        }
        m_MessageFactory.createAssetDirectoryServiceMessage(AssetDirectoryServiceMessageType.CreateAssetRequest, 
            request.build()).queue(controllerId, assetMgr.createRemoteAssetHandler());
        Logging.log(LogService.LOG_DEBUG, "Requested to create asset with name [%s] and of product type [%s]", name, 
                productType);
    }

    @Override
    public void requestAssetTypeUpdate(final int controllerId)
    {
        m_MessageFactory.createAssetDirectoryServiceMessage(AssetDirectoryServiceMessageType.GetAssetTypesRequest, 
            null).queue(controllerId, null);
    }
    
    @Override
    public AssetCapabilities getAssetCaps()
    {
        if (getAssetFactory() == null)
        {
            return null;
        }
        return getAssetFactory().getFactoryCaps();
    }
    
    @Override
    public List<ModifiablePropertyModel> getProperties()
    {
        if (m_PropertyList.isEmpty() && m_Model != null)
        {
            getConfigurationDefaults();
        }
        
        return m_PropertyList;
    }
    
    /**
     * Get the default configuration properties for a asset factory model.
     */
    private void getConfigurationDefaults()
    {
        m_PropertyList.clear();
        final UnmodifiableConfigMetatypeModel configMetaModel = configWrapper.getConfigurationDefaultsByFactoryPidAsync(
                activeController.getActiveController().getId(), m_Model.getFullyQualifiedAssetType() + "Config");
        if (configMetaModel != null)
        {
            for (UnmodifiablePropertyModel propModel : configMetaModel.getProperties())
            {
                final ModifiablePropertyModel model = new ConfigPropModelImpl((ConfigPropModelImpl)propModel);
                m_PropertyList.add(model);
            }
        }
    }
}
