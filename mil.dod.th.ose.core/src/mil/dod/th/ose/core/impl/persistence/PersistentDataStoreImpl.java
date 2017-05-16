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
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Modified;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.ose.shared.H2DataStore;
import mil.dod.th.ose.shared.JdoDataStore;
import mil.dod.th.ose.shared.SystemConfigurationConstants;
import mil.dod.th.ose.utils.FileUtils;

import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventAdmin;

/**
 * Implementation of the {@link PersistentDataStore}.
 * 
 * @author jconn
 */
@Component(designate = H2DataStoreConfig.class, configurationPolicy = ConfigurationPolicy.optional,
    provide = { PersistentDataStore.class, H2DataStore.class, JdoDataStore.class }, 
    properties = JdoDataStore.PROP_KEY_DATASTORE_TYPE + "=" + JdoDataStore.PROP_PERSISTENT_STORE)
public class PersistentDataStoreImpl extends AbstractH2DataStore<PersistentData> implements PersistentDataStore
{
    /**
     * Base database file name, used to create URL and filename.
     */
    public static final String DATABASE_FILE_BASE = "datastores/PersistentDataStore";
    
    /**
     * Framework property key for the maximum cache size. Ignored if component property is already set.
     * 
     * @see H2DataStoreConfig#maxDatabaseCacheSize()
     */
    public static final String MAX_CACHE_SIZE = "mil.dod.th.ose.core.persistentdatastore.maxcache";

    /** Name of the default date/time field used by entries in the data store. */
    private static final String TIME_FIELD_NAME = "timestamp";

    /** Query by Description Filter. */
    private static final String DESCRIPTION_FILTER = "description == '%s'";

    /** Query by Context Filter. */
    private static final String CONTEXT_FILTER = "context == '%s'";
    
    /** Query by Context and Time Filter. */
    private static final String CONTEXT_TIME_FILTER = CONTEXT_FILTER + AND + TIME_FILTER;
    
    /** Query by Context and Description Filter. */
    private static final String CONTEXT_DESCRIPTION_FILTER = CONTEXT_FILTER + AND + DESCRIPTION_FILTER;
    
    /** Query by Context and Description and Time Filter. */
    private static final String CONTEXT_DESCRIPTION_TIME_FILTER = CONTEXT_DESCRIPTION_FILTER + AND + TIME_FILTER;
    
    /**
     * Constructor.
     */
    public PersistentDataStoreImpl()
    {
        super(PersistentData.class, TIME_FIELD_NAME);
    }

    /**
     * Activate the component by activating the abstract persistent store.
     * 
     * @param props
     *      configuration properties associated with the component
     * @param context
     *      context of the bundle containing this component
     */
    @Activate
    public void activate(final Map<String, Object> props, final BundleContext context)
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

    @Reference
    @Override
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        super.setEventAdmin(eventAdmin);
    }

    @Reference
    @Override
    public void setPowerManager(final PowerManager powerManager)
    {
        super.setPowerManager(powerManager);
    }

    @Reference
    @Override
    public void setPersistenceManagerFactoryCreator(final PersistenceManagerFactoryCreator pmFactoryCreator)
    {
        super.setPersistenceManagerFactoryCreator(pmFactoryCreator);
    }

    @Override
    public PersistentData persist(final Class<?> context,
                                  final UUID uuid,
                                  final String description,
                                  final Serializable entity)
            throws IllegalArgumentException, PersistenceFailedException
    {
        validateArgumentContext(context);
        validateArgumentUuid(uuid);
        validateArgumentDescription(description);
        validateArgumentEntity(entity);

        final PersistentData pData =
            super.makePersistent(new PersistentData(uuid, description, context.getName(), entity));

        postEvent(TOPIC_DATA_PERSISTED, pData);

        return pData;
    }

    @Override
    public void merge(final PersistentData persistentData)
            throws IllegalArgumentException, PersistenceFailedException
    {
        validateArgumentPersistentData(persistentData);

        super.makePersistent(persistentData);

        postEvent(TOPIC_DATA_MERGED, persistentData);
    }

    @Override
    public void removeMatching(final Class<?> context)
            throws IllegalArgumentException
    {
        validateArgumentContext(context);
        
        removeOnFilter(CONTEXT_FILTER, context.getName());
    }

    @Override
    public void removeMatching(final Class<?> context,
                               final Date startTime,
                               final Date stopTime)
            throws IllegalArgumentException
    {
        validateArgumentContext(context);
        validateArgumentStartStopTimes(startTime, stopTime);
        
        removeOnFilter(CONTEXT_TIME_FILTER,
                context.getName(), TIME_FIELD_NAME, startTime.getTime(), TIME_FIELD_NAME, stopTime.getTime());
    }

    @Override
    public void removeMatching(final Class<?> context,
                               final String description)
            throws IllegalArgumentException
    {
        validateArgumentContext(context);
        validateArgumentDescription(description);

        removeOnFilter(CONTEXT_DESCRIPTION_FILTER, context.getName(), description);
    }

    @Override
    public void removeMatching(final Class<?> context,
                               final String description,
                               final Date startTime,
                               final Date stopTime)
            throws IllegalArgumentException
    {
        validateArgumentContext(context);
        validateArgumentDescription(description);
        validateArgumentStartStopTimes(startTime, stopTime);
        
        removeOnFilter(CONTEXT_DESCRIPTION_TIME_FILTER, context.getName(), description, TIME_FIELD_NAME,
                startTime.getTime(), TIME_FIELD_NAME, stopTime.getTime());
    }

    @Override
    public Collection<PersistentData> query(final Class<?> context)
            throws IllegalArgumentException
    {
        validateArgumentContext(context);
        
        return queryOnFilter(CONTEXT_FILTER, context.getName());
    }

    @Override
    public Collection<PersistentData> query(final Class<?> context,
                                                      final String description)
            throws IllegalArgumentException
    {
        validateArgumentContext(context);
        validateArgumentDescription(description);
        
        return queryOnFilter(CONTEXT_DESCRIPTION_FILTER, context.getName(), description);
    }

    @Override
    public Collection<PersistentData> query(final Class<?> context,
                                                      final Date startTime,
                                                      final Date stopTime)
            throws IllegalArgumentException
    {
        validateArgumentContext(context);
        validateArgumentStartStopTimes(startTime, stopTime);

        return queryOnFilter(CONTEXT_TIME_FILTER, context.getName(), TIME_FIELD_NAME, startTime.getTime(),
                TIME_FIELD_NAME, stopTime.getTime());
    }

    @Override
    public Collection<PersistentData> query(final Class<?> context,
                                                      final String description,
                                                      final Date startTime,
                                                      final Date stopTime)
            throws IllegalArgumentException
    {
        validateArgumentContext(context);
        validateArgumentDescription(description);
        validateArgumentStartStopTimes(startTime, stopTime);

        return queryOnFilter(CONTEXT_DESCRIPTION_TIME_FILTER, context.getName(), description, TIME_FIELD_NAME,
                startTime.getTime(), TIME_FIELD_NAME, stopTime.getTime());
    }

    @Override
    protected void deleteCleanup()
    {
        // No cleanup required when deleting entries from the database
    }

    /**
     * Helper method to wrap base postEvent and create the properties from the persistentData.
     * 
     * @param topic
     *            the specified event topic
     * @param persistentData
     *            the specified persistentData
     */
    private void postEvent(final String topic,
                           final PersistentData persistentData)
    {
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(PersistentDataStore.EVENT_PROP_DATA_CONTEXT, persistentData.getContext());
        props.put(PersistentDataStore.EVENT_PROP_DATA_UUID, persistentData.getUUID());
        props.put(PersistentDataStore.EVENT_PROP_DATA_DESCRIPTION, persistentData.getDescription());
        postEvent(topic, props);
    }

    /**
     * Helper method to validate the description argument.
     * 
     * @param description
     *            the specified data description
     */
    private void validateArgumentDescription(final String description)
    {
        if (description == null)
        {
            throw new IllegalArgumentException("description must not be null");
        }
    }

    /**
     * Helper method to validate the context argument.
     * 
     * @param context
     *            the specified data context
     */
    private void validateArgumentContext(final Class<?> context)
    {
        if (context == null)
        {
            throw new IllegalArgumentException("context must not be null");
        }
    }

    /**
     * Helper method to validate the persistentData argument.
     * 
     * @param persistentData
     *            the specified persistent data
     */
    private void validateArgumentPersistentData(final PersistentData persistentData)
    {
        if (persistentData == null)
        {
            throw new IllegalArgumentException("persistentData must not be null");
        }
    }

    /**
     * Helper method to validate the entity argument.
     * 
     * @param entity
     *            the specified serializable entity
     */
    private void validateArgumentEntity(final Serializable entity)
    {
        if (entity == null)
        {
            throw new IllegalArgumentException("entity must not be null");
        }
    }

    @Override
    public long getUsableSpace()
    {
        return FileUtils.getPartition(new File(DATABASE_FILE_BASE)).getUsableSpace();
    }
}
