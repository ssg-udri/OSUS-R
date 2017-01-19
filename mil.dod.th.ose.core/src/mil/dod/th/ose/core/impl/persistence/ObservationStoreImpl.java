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
package mil.dod.th.ose.core.impl.persistence;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Modified;
import aQute.bnd.annotation.component.Reference;

import com.google.common.base.Preconditions;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.persistence.ObservationQuery;
import mil.dod.th.core.persistence.ObservationQuery.SortField;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.core.types.Version;
import mil.dod.th.core.types.observation.ObservationSubTypeEnum;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.core.validator.Validator;
import mil.dod.th.ose.shared.H2DataStore;
import mil.dod.th.ose.shared.JdoDataStore;
import mil.dod.th.ose.shared.SystemConfigurationConstants;
import mil.dod.th.ose.utils.ClassService;
import mil.dod.th.ose.utils.FileUtils;
import mil.dod.th.ose.utils.PropertyRetriever;

import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventAdmin;

/**
 * Implementation of the {@link ObservationStore}.
 * 
 * @author jconn
 */
@Component(designate = H2DataStoreConfig.class, configurationPolicy = ConfigurationPolicy.optional,
        provide = { ObservationStore.class, H2DataStore.class, JdoDataStore.class }, 
        properties = JdoDataStore.PROP_KEY_DATASTORE_TYPE + "=" + JdoDataStore.PROP_OBSERVATION_STORE)
public class ObservationStoreImpl extends AbstractH2DataStore<Observation> implements ObservationStore
{
    /**
     * Base database file name, used to create URL and filename.
     */
    public static final String DATABASE_FILE_BASE = "datastores/ObservationStore";
    
    /**
     * Framework property key for the maximum cache size. Ignored if component property is already set.
     * 
     * @see H2DataStoreConfig#maxDatabaseCacheSize()
     */
    public static final String MAX_CACHE_SIZE = "mil.dod.th.ose.core.observationstore.maxcache";

    /**
     * The object which contains the current version of all new observations.
     */
    private Version m_Version;
    
    /**
     * Property retriever service.
     */
    private PropertyRetriever m_PropertyRetriever;
    
    /**
     * Service used to validate observations.
     */
    private Validator m_ObservationValidator;

    /**
     * Service for accessing the {@link Class} class, so static methods are not called.
     */
    private ClassService m_ClassService;

    /**
     * Constructor.
     */
    public ObservationStoreImpl()
    {
        super(Observation.class, SortField.CreatedTimestamp.getJdoFieldName());
    }

    /**
     * Set the property retriever service which will get the needed properties embedded within the bundle.
     * @param retriever
     *     the property retriever service
     */
    @Reference
    public void setPropertyRetriever(final PropertyRetriever retriever)
    {
        m_PropertyRetriever = retriever;
    }
    
    /**
     * Set the observation validator service.
     * @param obsValidator
     *     the observation validator service
     */
    @Reference
    public void setObservationValidator(final Validator obsValidator)
    {
        m_ObservationValidator = obsValidator;
    }
    
    /**
     * Bind the service.
     * 
     * @param classService
     *      service to bind to the component
     */
    @Reference
    public void setClassService(final ClassService classService)
    {
        m_ClassService = classService;
    }
    
    @Reference
    @Override
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        super.setEventAdmin(eventAdmin);
    }

    @Reference
    @Override
    public void setPersistenceManagerFactoryCreator(
        final PersistenceManagerFactoryCreator persistenceManagerFactoryCreator)
    {
        super.setPersistenceManagerFactoryCreator(persistenceManagerFactoryCreator);
    }

    @Reference
    @Override
    public void setPowerManager(final PowerManager powerManager)
    {
        super.setPowerManager(powerManager);
    }

    /**
     * Activate the component by activating the abstract persistent store.
     * 
     * @param props
     *      configuration properties associated with the component
     * @param context
     *      the bundle context of the bundle which contains this component
     * @throws IOException 
     *      thrown in the event that the file can not be found
     */
    @Activate
    public void activate(final Map<String, Object> props, final BundleContext context) throws IOException
    {
        final String maxCacheSizeFrameworkProp = context.getProperty(MAX_CACHE_SIZE);
        final Map<String, Object> combindedProps = new HashMap<String, Object>(props);
        if (props.get(H2DataStoreConfig.MAX_CACHE_SIZE_KEY) == null)
        {
            // may still be null if framework property is not set, will end up using default if so
            combindedProps.put(H2DataStoreConfig.MAX_CACHE_SIZE_KEY, maxCacheSizeFrameworkProp);
        }
        
        final String dataDir = context.getProperty(SystemConfigurationConstants.DATA_DIR_PROPERTY);
        activateStore(String.format("jdbc:h2:file:%s", dataDir + File.separator + DATABASE_FILE_BASE), combindedProps);
        
        //pull out the version numbers
        final URL entry = m_ClassService.getResource(Observation.class, "version.properties");
        final Properties properties = m_PropertyRetriever.getPropertiesFromUrl(entry);
        //major number
        final int major = Integer.parseInt(properties.getProperty("majorNum"));
        //minor number
        final int minor = Integer.parseInt(properties.getProperty("minorNum"));
        //save the information
        m_Version = new Version(major, minor); 
    }

    /**
     * Deactivate the persistent store.
     */
    @Deactivate
    public void deactivate()
    {
        deactivateStore();
    }
    
    /**
     * Update the configuration of the component.
     * 
     * @param props
     *      updated properties for the component
     */
    @Modified
    public void modified(final Map<String, Object> props)
    {
        updateProps(props);
    }

    @Override
    public void persist(final Observation observation) throws IllegalArgumentException, ValidationFailedException, 
            PersistenceFailedException 
    {
        final Observation validatedObservation = validateArgumentObservation(observation);

        super.makePersistent(validatedObservation);

        postEvent(TOPIC_OBSERVATION_PERSISTED, validatedObservation);
    }
    
    @Override
    public void merge(final Observation observation)
            throws IllegalArgumentException, PersistenceFailedException, ValidationFailedException
    {
        final Observation validatedObservation = validateArgumentObservation(observation);

        super.makePersistent(validatedObservation);
        
        postEvent(TOPIC_OBSERVATION_MERGED, validatedObservation);
    }
    
    @Override
    public ObservationQuery newQuery()
    {
        return new ObservationQueryImpl(this);
    }

    @Override
    public long removeByAssetUuid(final UUID assetUuid)
    {
        Preconditions.checkNotNull(assetUuid);
        
        return newQuery().withAssetUuid(assetUuid).remove();
    }

    @Override
    public long removeByAsset(final Asset asset)
            throws IllegalArgumentException
    {
        Preconditions.checkNotNull(asset);
        
        return removeByAssetUuid(asset.getUuid());      
    }

    @Override
    public long removeByAssetType(final String assetType)
    {
        Preconditions.checkNotNull(assetType);
        
        return newQuery().withAssetType(assetType).remove();
    }

    @Override
    public long removeBySubType(final ObservationSubTypeEnum ObservationSubType)
    {
        Preconditions.checkNotNull(ObservationSubType);
        
        return newQuery().withSubType(ObservationSubType).remove();
    }

    @Override
    public long removeBySystemId(final Integer systemId)
    {
        Preconditions.checkNotNull(systemId);
        
        return newQuery().withSystemId(systemId).remove();
    };
    
    @Override
    public Collection<Observation> queryByAssetUuid(final UUID assetUuid)
    {
        Preconditions.checkNotNull(assetUuid);

        return newQuery().withAssetUuid(assetUuid).execute();
    }

    @Override
    public Collection<Observation> queryByAsset(final Asset asset)
    {
        Preconditions.checkNotNull(asset);

        return queryByAssetUuid(asset.getUuid());
    }

    @Override
    public Collection<Observation> queryByAssetType(final String assetType)
    {
        Preconditions.checkNotNull(assetType);

        return newQuery().withAssetType(assetType).execute();
    }

    @Override
    public Collection<Observation> queryBySubType(final ObservationSubTypeEnum ObservationSubType)
    {
        Preconditions.checkNotNull(ObservationSubType);
        
        return newQuery().withSubType(ObservationSubType).execute();
    }
    
    @Override
    public Collection<Observation> queryBySystemId(final Integer systemId) throws NullPointerException
    {
        Preconditions.checkNotNull(systemId);
        
        return newQuery().withSystemId(systemId).execute();
    }

    @Override
    protected void deleteCleanup()
    {
        try
        {
            // Manually cleanup media, coordinate and orientation entries from the database where Datanucleus cannot
            // automatically handle when entries are shared/duplicated between different fields
            executeSql(
                    "DELETE FROM COORDINATES WHERE "
                        + "NOT EXISTS(SELECT NULL FROM OBSERVATION_ASSETLOCATION a WHERE a.ID_ID=ID) "
                        + "AND NOT EXISTS(SELECT NULL FROM OBSERVATION_POINTINGLOCATION p WHERE p.ID_ID=ID) "
                        + "AND NOT EXISTS(SELECT NULL FROM DETECTION_TARGETLOCATION d WHERE d.ID_ID=ID)",
                    null);
            executeSql(
                    "DELETE FROM ORIENTATION WHERE "
                        + "NOT EXISTS(SELECT NULL FROM OBSERVATION_ASSETORIENTATION a WHERE a.ID_ID=ID) "
                        + "AND NOT EXISTS(SELECT NULL FROM OBSERVATION_PLATFORMORIENTATION p WHERE p.ID_ID=ID)",
                    null);
            executeSql(
                    "DELETE FROM DIGITALMEDIA WHERE "
                        + "NOT EXISTS(SELECT NULL FROM OBSERVATION_DIGITALMEDIA d WHERE d.ID_ID=ID) "
                        + "AND NOT EXISTS(SELECT NULL FROM IMAGEMETADATA_MASKSAMPLESOFINTEREST m WHERE m.ID_EID=ID)",
                    null);
        }
        catch (final SQLException ex)
        {
            throw new PersistenceFailedException("Observation delete cleanup failed", ex);
        }
    }

    /**
     * Helper method to validate the observation argument.
     * 
     * @param observation
     *            the specified {@link Observation}
     * @return 
     *            the validated observation
     * @throws IllegalArgumentException
     *            if the observation is null
     * @throws ValidationFailedException
     *            if the observation is not valid when compared to the observation schema
     */
    private Observation validateArgumentObservation(final Observation observation)
            throws IllegalArgumentException, ValidationFailedException
    {
        if (observation == null)
        {
            throw new IllegalArgumentException("observation must not be null");
        }
        m_ObservationValidator.validate(observation);
        return observation;
    }

    /**
     * Helper method to wrap base postEvent and create the properties from the observation.
     * 
     * @param topic
     *          the specified event topic
     * @param observation
     *          the specified observation
     */
    private void postEvent(final String topic, final Observation observation)
    {
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(EVENT_PROP_OBSERVATION_UUID, observation.getUuid());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, observation.getAssetType());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, observation.getAssetUuid().toString());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, observation.getAssetName());
        props.put(EVENT_PROP_SYS_ID, observation.getSystemId());
        props.put(EVENT_PROP_OBSERVATION_TYPE, findObservationSubType(observation));
        props.put(EVENT_PROP_SENSOR_ID, observation.getSensorId());
        props.put(EVENT_PROP_SYS_IN_TEST_MODE, observation.isSystemInTestMode());
        
        postEvent(topic, props);

        postEventWithObs(topic, props, observation);
    }

    /**
     * Method to find what type the given observation is.
     * 
     * @param observation
     *  the observation to check what type it is
     * @return
     *  the String representation of the {@link ObservationSubTypeEnum} or NONE if no 
     *  type applies.
     */
    private String findObservationSubType(final Observation observation) // NOCHECKSTYLE: Complexity required to check
                                                                         // each sub-type
    {
        if (observation.isSetAudioMetadata())
        {
            return ObservationSubTypeEnum.AUDIO_METADATA.toString();
        }
        else if (observation.isSetDetection())
        {
            return ObservationSubTypeEnum.DETECTION.toString();
        }
        else if (observation.isSetImageMetadata())
        {
            return ObservationSubTypeEnum.IMAGE_METADATA.toString();
        }
        else if (observation.isSetStatus())
        {
            return ObservationSubTypeEnum.STATUS.toString();
        }
        else if (observation.isSetVideoMetadata())
        {
            return ObservationSubTypeEnum.VIDEO_METADATA.toString();
        }
        else if (observation.isSetWeather())
        {
            return ObservationSubTypeEnum.WEATHER.toString();
        }
        else if (observation.isSetBiological())
        {
            return ObservationSubTypeEnum.BIOLOGICAL.toString();
        }
        else if (observation.isSetCbrneTrigger())
        {
            return ObservationSubTypeEnum.CBRNE_TRIGGER.toString();
        }
        else if (observation.isSetWaterQuality())
        {
            return ObservationSubTypeEnum.WATER_QUALITY.toString();
        }
        else if (observation.isSetChemical())
        {
            return ObservationSubTypeEnum.CHEMICAL.toString();
        }
        else if (observation.isSetPower())
        {
            return ObservationSubTypeEnum.POWER.toString();
        }
        else if (observation.isSetChannelMetadata())
        {
            return ObservationSubTypeEnum.CHANNEL_METADATA.toString();
        }

        return ObservationSubTypeEnum.NONE.toString();
    }

    /**
     * Post an event with the topic adjusted to identify that the event includes the actual observation object.
     * @param topic
     *     the specified event topic
     * @param props
     *     the properties for the event
     * @param observation
     *     the specified observation
     */
    private void postEventWithObs(final String topic, final Map<String, Object> props, final Observation observation)
    {
        //post the event again with the observation
        props.put(EVENT_PROP_OBSERVATION, observation);

        if (topic.equals(ObservationStore.TOPIC_OBSERVATION_PERSISTED))
        {
            postEvent(ObservationStore.TOPIC_OBSERVATION_PERSISTED_WITH_OBS, props);
        }
        else if (topic.equals(ObservationStore.TOPIC_OBSERVATION_MERGED))
        {
            postEvent(ObservationStore.TOPIC_OBSERVATION_MERGED_WITH_OBS, props);
        }
    }

    @Override
    public long getUsableSpace()
    {
        return FileUtils.getPartition(new File(DATABASE_FILE_BASE)).getUsableSpace();
    }

    @Override
    public Version getObservationVersion()
    {
        //create a new version object
        return new Version(m_Version.getMajorNumber(), m_Version.getMinorNumber());
    }
}
