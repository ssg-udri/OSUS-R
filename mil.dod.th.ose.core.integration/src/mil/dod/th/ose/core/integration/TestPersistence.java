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
package mil.dod.th.ose.core.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import junit.framework.TestCase;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.observation.types.AudioMetadata;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Weather;
import mil.dod.th.core.persistence.ObservationQuery.SortField;
import mil.dod.th.core.persistence.ObservationQuery.SortOrder;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.core.types.DigitalMedia;
import mil.dod.th.core.types.TemperatureCelsius;
import mil.dod.th.core.types.Version;
import mil.dod.th.core.types.audio.AudioRecorder;
import mil.dod.th.core.types.audio.AudioRecorderEnum;
import mil.dod.th.core.types.observation.ObservationSubTypeEnum;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.ose.test.matchers.JaxbUtil;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.log.LogService;

import example.asset.data.ExampleNonLexiconData;

/**
 * Test the {@link ObservationStore} and {@link PersistentDataStore}. This tests the API, but does not verify things are
 * persisted after shutdown. That is done with other classes.
 * 
 * @author jconn
 *
 */
public class TestPersistence extends TestCase
{
    private static final String ASSETTYPE = "TestAssetType";
    private static final UUID ASSETUUID = UUID.randomUUID();

    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    private ObservationStore m_ObservationStore;
    private PersistentDataStore m_PersistentDataStore;
    
    @Override
    public void setUp() throws Exception
    {   
        m_ObservationStore = ServiceUtils.getService(m_Context, ObservationStore.class);
        assertThat(m_ObservationStore, is(notNullValue()));
        
        m_PersistentDataStore = ServiceUtils.getService(m_Context, PersistentDataStore.class);
        assertThat(m_PersistentDataStore, is(notNullValue()));
    }
    
    @Override
    public void tearDown()
    {
        //remove all previous observations
        m_ObservationStore.removeByAssetType(ASSETTYPE);
        assertThat(m_ObservationStore.queryByAssetType(ASSETTYPE).isEmpty(), is(true));
    }
    
    /**
     * Creates observation.
     * 
     * @return the observation
     */
    private Observation createObservation()
    {   
        final Long timeStamp = new Date().getTime();
        final double temperatureCelsius = 37.0;
        
        final TemperatureCelsius temperatureMeasurement = new TemperatureCelsius();
        temperatureMeasurement.setValue(temperatureCelsius);
        
        final Weather weather = new Weather();
        weather.setTemperature(temperatureMeasurement);
        
        final Observation observation = new Observation();
        observation.setCreatedTimestamp(timeStamp);
        observation.setUuid(UUID.randomUUID());
        observation.setAssetUuid(ASSETUUID);
        observation.setAssetName("asset-name");
        observation.setAssetType(ASSETTYPE);
        observation.setWeather(weather);
        observation.setVersion(new Version(1,2));

        return observation;
    }
    
    /**
     * This test will verify that an observation that has been persisted can be marshalled to XML and then unmarshalled
     * back into an object and still be valid.  Basically ensures the marshalling actually generates valid XML data.
     */
    public void testMarhsalledFile() 
        throws JAXBException, IllegalArgumentException, PersistenceFailedException, ValidationFailedException
    {
        File outputFile = m_Context.getDataFile("test.xml");
        
        final Observation observation = createObservation();
        m_ObservationStore.persist(observation);
        m_ObservationStore.remove(observation.getUuid());
        
        JAXBContext context = JAXBContext.newInstance(Observation.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.marshal(observation, outputFile);
        
        Unmarshaller unmarshaller = context.createUnmarshaller();
        Observation marshalledObservation = (Observation)unmarshaller.unmarshal(outputFile);
        m_ObservationStore.persist(marshalledObservation);
        m_ObservationStore.remove(observation.getUuid());
    }
    
    /**
     * Verify that the UUID of an observation must be unique.
     */
    public void testObservationStore()
        throws ValidationFailedException, IllegalArgumentException, PersistenceFailedException
    {
        final Observation observation = createObservation();
        final UUID uuid = new UUID(observation.getUuid().getMostSignificantBits(),
                                   observation.getUuid().getLeastSignificantBits());
        
        assertThat(m_ObservationStore.find(uuid), is(nullValue()));
        
        m_ObservationStore.persist(observation);
        
        final Observation observation2 = createObservation();
        observation2.setUuid(uuid);
        
        Throwable e = null;
        try { m_ObservationStore.persist(observation2); }
        catch (Throwable ex) { e = ex; }
        assertThat(e, is(instanceOf(PersistenceFailedException.class)));
        
        final Observation returnedObservation = m_ObservationStore.find(uuid);

        assertThat(returnedObservation, is(notNullValue()));
        assertThat(returnedObservation.getCreatedTimestamp(), is(observation.getCreatedTimestamp()));
        assertThat(returnedObservation.getUuid(), is(uuid));
        assertThat(returnedObservation.getAssetUuid(), is(observation.getAssetUuid()));
        assertThat(returnedObservation.getAssetType(), is(observation.getAssetType()));
        assertThat(returnedObservation.getWeather().getTemperature(), is(observation.getWeather().getTemperature()));
        
        Logging.log(LogService.LOG_INFO, 
                "\n====== Observation ======\n= Timestamp = %d\n= UUID = %s\n= AssetUuid = %s\n= AssetType = %s\n",
                returnedObservation.getCreatedTimestamp(),
                returnedObservation.getUuid().toString(),
                returnedObservation.getAssetUuid(),
                returnedObservation.getAssetType());

        m_ObservationStore.remove(uuid);
        
        assertThat(m_ObservationStore.contains(uuid), is(false));
    }
    
    /**
     * Verify can query for all observations if no query parameters are set.
     */
    public void testObservationQueryAll()
        throws ValidationFailedException, IllegalArgumentException, PersistenceFailedException
    {
        // should allow no parameters
        List<Observation> initialObs = new ArrayList<>(m_ObservationStore.newQuery().execute());
        
        List<Observation> expectedObs = new ArrayList<>();
        for (int i = 0; i < 20; i++)
        {
            expectedObs.add(createObservation());
            if (i > 20 / 2)
            {
                // change the 2nd half to use a different asset type
                expectedObs.get(i).setAssetType("other-type");
            }
            m_ObservationStore.persist(expectedObs.get(i));
        }
        
        // should allow no parameters
        List<Observation> afterObs = new ArrayList<>(m_ObservationStore.newQuery().execute());
        
        assertThat("Query did not return number of expected observations",
                (afterObs.size() - initialObs.size()) >= expectedObs.size());
    }
    
    /**
     * Verify that the correct number of observations is returned when count is requested for the specific asset. 
     */
    public void testTotalNumberOfObservations() throws ValidationFailedException, 
        IllegalArgumentException, PersistenceFailedException
    {
        // should allow no parameters
        List<Observation> initialObs = 
                new ArrayList<>(m_ObservationStore.newQuery().withAssetType(ASSETTYPE).execute());
        assertThat((int)m_ObservationStore.newQuery().withAssetType(ASSETTYPE).getCount(), is(initialObs.size()));
        
        List<Observation> expectedObs = new ArrayList<>();
        for (int i = 0; i < 20; i++)
        {
            expectedObs.add(createObservation());
            if (i > 20 / 2)
            {
                // change the 2nd half to use a different asset type
                expectedObs.get(i).setAssetType("other-type");
            }
            m_ObservationStore.persist(expectedObs.get(i));
        }
        
        List<Observation> afterObs = new ArrayList<>(m_ObservationStore.newQuery().withAssetType(ASSETTYPE).execute());
        
        assertThat((int)m_ObservationStore.newQuery().withAssetType(ASSETTYPE).getCount(), is(afterObs.size()));
    }
    
    /**
     * Verify that observation can be queried for within a specific time frame (createdTime), and that ONLY those
     * observations are returned. Also, query both other things at the same time.
     */
    public void testQueryByCreatedTime() throws Exception
    {
        //create one observation now
        final Observation observation = createObservation();
        m_ObservationStore.persist(observation);
        
        // wait between persisting so the previous observation is not included in the query
        Thread.sleep(1000);
        
        //time when collection of observations started
        final Date startTime = new Date();
        //create observations
        for (int i = 0; i < 4; i++)
        {
            m_ObservationStore.persist(createObservation());
        }
        //time when creation stopped
        final Date stopTime = new Date();
        
        //query by asset type
        final Collection<Observation> observationCollection1 = m_ObservationStore.queryByAssetType(ASSETTYPE);
        assertThat(observationCollection1, is(notNullValue()));
        //created 4 above, and one previously
        assertThat(observationCollection1.size(), is(5));

        //query by creation time and ensure that the same observations are returned, minus the one created before
        //the start time
        final Collection<Observation> observationCollection2 = m_ObservationStore.newQuery()
                    .withAssetType(ASSETTYPE)
                    .withTimeCreatedRange(startTime, stopTime)
                    .withMaxObservations(20)
                    .withSubType(ObservationSubTypeEnum.WEATHER)
                    .execute();
        
        assertThat(observationCollection2, is(notNullValue()));
        assertThat(observationCollection2.size(), is(4));

        //query by observed time and ensure that no observations are returned since observed field is not set
        final Collection<Observation> observationCollection3 = m_ObservationStore.newQuery()
                    .withTimeObservedRange(startTime, stopTime)
                    .withMaxObservations(20)
                    .execute();
        
        assertThat(observationCollection3, is(notNullValue()));
        assertThat(observationCollection3.size(), is(0));
    }

    /**
     * Verify that observation can be queried for within a specific time frame (observedTime), and that ONLY those
     * observations are returned. Also, query both other things at the same time.
     */
    public void testQueryByObservedTime() throws Exception
    {
        final long OBSERVED_TIME_OFFSET = 1000L;

        //create one observation now
        Observation observation = createObservation();
        observation.setObservedTimestamp(System.currentTimeMillis() - OBSERVED_TIME_OFFSET);
        m_ObservationStore.persist(observation);
        
        // wait between persisting so the previous observation is not included in the query
        Thread.sleep(1000);
        
        //time when collection of observations started
        final Date startTime = new Date(System.currentTimeMillis() - OBSERVED_TIME_OFFSET);

        //create observations
        for (int i = 0; i < 4; i++)
        {
            observation = createObservation();
            observation.setObservedTimestamp(System.currentTimeMillis() - OBSERVED_TIME_OFFSET);
            m_ObservationStore.persist(observation);
        }

        //time when creation stopped
        final Date stopTime = new Date(System.currentTimeMillis() - OBSERVED_TIME_OFFSET);

        //query by asset type
        final Collection<Observation> observationCollection1 = m_ObservationStore.queryByAssetType(ASSETTYPE);
        assertThat(observationCollection1, is(notNullValue()));
        //created 4 above, and one previously
        assertThat(observationCollection1.size(), is(5));

        //query by observed time and ensure that the same observations are returned, minus the one created before
        //the start time
        final Collection<Observation> observationCollection2 = m_ObservationStore.newQuery()
                    .withAssetType(ASSETTYPE)
                    .withTimeObservedRange(startTime, stopTime)
                    .withMaxObservations(20)
                    .withSubType(ObservationSubTypeEnum.WEATHER)
                    .execute();

        assertThat(observationCollection2, is(notNullValue()));
        assertThat(observationCollection2.size(), is(4));
    }

    /**
     * Test query by observation type.
     */
    public void testQueryByObsType() throws ValidationFailedException, IllegalArgumentException, 
        PersistenceFailedException
    {
        //verify there are no pre-existing observations
        m_ObservationStore.removeBySubType(ObservationSubTypeEnum.AUDIO_METADATA);
        final Collection<Observation> observationCollection = m_ObservationStore.
                queryBySubType(ObservationSubTypeEnum.AUDIO_METADATA);
        assertThat(observationCollection, is(notNullValue()));
        assertThat(observationCollection.size(), is(0));
        
        //create an audio observation
        final UUID audioUuid = UUID.randomUUID();
        final Observation audioObservation = new Observation();
        final AudioMetadata audioMetadata = new AudioMetadata();
        audioMetadata.setStartTime(0L);
        audioMetadata.setEndTime(1000L);
        final AudioRecorder audioRecorder = new AudioRecorder();
        audioRecorder.setValue(AudioRecorderEnum.MICROPHONE);
        audioMetadata.setRecorderType(audioRecorder);
        audioMetadata.setSampleRateKHz(20.0);
        
        audioObservation.setAudioMetadata(audioMetadata);
        audioObservation.setAssetUuid(audioUuid);
        audioObservation.setAssetName("audioAssetName");
        audioObservation.setAssetType("audioAsset");
        audioObservation.setCreatedTimestamp(System.currentTimeMillis());
        audioObservation.setUuid(UUID.randomUUID());
        audioObservation.setDigitalMedia(new DigitalMedia(new byte[1000], "audio/mp3"));
        audioObservation.setVersion(new Version(1,0));
        
        m_ObservationStore.persist(audioObservation);
        final Observation returnedAudioObservation = m_ObservationStore.find(audioObservation.getUuid());
        
        //update the asset id
        final UUID mergedUuid = UUID.randomUUID();
        returnedAudioObservation.setAssetUuid(mergedUuid);
        m_ObservationStore.merge(returnedAudioObservation);
        
        //verify
        final Observation returnedAudioObservation2 = m_ObservationStore.find(returnedAudioObservation.getUuid());
        assertThat(returnedAudioObservation2.getAssetUuid(), is(mergedUuid));
        Collection<Observation> audioObservations = m_ObservationStore.queryBySubType(
                ObservationSubTypeEnum.AUDIO_METADATA);
        assertThat(audioObservations.size(), is(1));
    }
    
    /**
     * Verify can change order in query to get collection back with a different order.
     */
    public void testObservationQuerySortOrder()
        throws ValidationFailedException, IllegalArgumentException, PersistenceFailedException
    {
        List<Observation> expectedObs = new ArrayList<>();
        for (int i = 0; i < 3; i++)
        {
            Observation obs = createObservation();
            obs.setCreatedTimestamp((long)i);
            expectedObs.add(obs);
            m_ObservationStore.persist(expectedObs.get(i));
        }
        
        List<Observation> actualObs = new ArrayList<>(m_ObservationStore.newQuery().withAssetType(ASSETTYPE).execute());
        
        // verify default is descending (first is newest, last created)
        assertThat(actualObs.size(), is(expectedObs.size()));
        JaxbUtil.assertEqualContent(actualObs.get(0), expectedObs.get(2));
        JaxbUtil.assertEqualContent(actualObs.get(1), expectedObs.get(1));
        JaxbUtil.assertEqualContent(actualObs.get(2), expectedObs.get(0));
        
        // try again with ascending order
        actualObs = new ArrayList<>(m_ObservationStore.newQuery()
                .withAssetType(ASSETTYPE).withOrder(SortField.CreatedTimestamp, SortOrder.Ascending).execute());
        
        // verify now ascending (first is oldest, first created)
        assertThat(actualObs.size(), is(expectedObs.size()));
        JaxbUtil.assertEqualContent(actualObs.get(0), expectedObs.get(0));
        JaxbUtil.assertEqualContent(actualObs.get(1), expectedObs.get(1));
        JaxbUtil.assertEqualContent(actualObs.get(2), expectedObs.get(2));
    }
    
    /**
     * Verify can change sorting field in query to get collection back.
     */
    public void testObservationQuerySortField()
        throws ValidationFailedException, IllegalArgumentException, PersistenceFailedException
    {
        List<Observation> expectedObs = new ArrayList<>();
        for (int i = 1; i < 4; i++)
        {
            Observation obs = createObservation();
            obs.setCreatedTimestamp((long)i);
            obs.setObservedTimestamp((long)(4-i));
            expectedObs.add(obs);
            m_ObservationStore.persist(expectedObs.get(i-1));
        }
        
        List<Observation> actualObs = new ArrayList<>(m_ObservationStore.newQuery().withAssetType(ASSETTYPE).execute());
        
        // verify default is descending (first is newest, last created)
        assertThat(actualObs.size(), is(expectedObs.size()));
        JaxbUtil.assertEqualContent(actualObs.get(0), expectedObs.get(2));
        JaxbUtil.assertEqualContent(actualObs.get(1), expectedObs.get(1));
        JaxbUtil.assertEqualContent(actualObs.get(2), expectedObs.get(0));
        
        // try again with ascending order
        actualObs = new ArrayList<>(m_ObservationStore.newQuery()
                .withAssetType(ASSETTYPE).withOrder(SortField.ObservedTimestamp, SortOrder.Descending).execute());
        
        // verify now ascending (first is oldest, first created)
        assertThat(actualObs.size(), is(expectedObs.size()));
        JaxbUtil.assertEqualContent(actualObs.get(0), expectedObs.get(0));
        JaxbUtil.assertEqualContent(actualObs.get(1), expectedObs.get(1));
        JaxbUtil.assertEqualContent(actualObs.get(2), expectedObs.get(2));
    }
    
    /**
     * Verify can limit query to get collection back with a smaller amount of observations.
     */
    public void testObservationQueryMax()
        throws ValidationFailedException, IllegalArgumentException, PersistenceFailedException
    {
        List<Observation> expectedObs = new ArrayList<>();
        for (int i = 0; i < 10; i++)
        {
            Observation obs = createObservation();
            obs.setCreatedTimestamp((long)i);
            expectedObs.add(obs);
            m_ObservationStore.persist(expectedObs.get(i));
        }
        
        List<Observation> actualObs = new ArrayList<>(m_ObservationStore.newQuery().withAssetType(ASSETTYPE).execute());
        
        // verify default is all
        assertThat(actualObs.size(), is(expectedObs.size()));
        for (int i = 0; i < expectedObs.size(); i++)
        {
            assertThat(actualObs.get(i), is(expectedObs.get(expectedObs.size() - i - 1)));
        }
        
        // try with max now
        actualObs = new ArrayList<>(m_ObservationStore.newQuery().
                withAssetType(ASSETTYPE).withMaxObservations(5).execute());
        
        // verify size is equal to max, but still the 5 newest
        assertThat(actualObs.size(), is(5));
        for (int i = 0; i < 5; i++)
        {
            assertThat(actualObs.get(i), is(expectedObs.get(expectedObs.size() - i - 1)));
        }
    }
    
    public void testObservationQueryBySystemId() throws IllegalArgumentException, PersistenceFailedException, 
        ValidationFailedException
    {
        List<Observation> expectedObs = new ArrayList<>();
        for (int i = 0; i < 5; i++)
        {
            Observation obs = createObservation();
            obs.setSystemId(1);
            obs.setSensorId("SystemIdTester");
            m_ObservationStore.persist(obs);
        }
        
        for (int i = 0; i < 5; i++)
        {
            Observation obs = createObservation();
            obs.setSystemId(42);
            obs.setSensorId("SystemIdTester");
            expectedObs.add(obs);
            m_ObservationStore.persist(obs);
        }
        
        List<Observation> actualObs = new ArrayList<>(m_ObservationStore.newQuery().withSystemId(42).execute());
        
        assertThat(actualObs.size(), is(5));
        
        for (Observation obs : expectedObs)
        {
            assertThat(actualObs, hasItem(obs));
        }
    }
    
    /**
     * Verify can limit query to a range to get collection back with a smaller amount of observations.
     */
    public void testObservationQueryRange()
        throws ValidationFailedException, IllegalArgumentException, PersistenceFailedException
    {
        List<Observation> expectedObs = new ArrayList<>();
        for (int i = 0; i < 20; i++)
        {
            Observation obs = createObservation();
            obs.setCreatedTimestamp((long)i);
            expectedObs.add(obs);
            m_ObservationStore.persist(expectedObs.get(i));
        }
        
        List<Observation> actualObs = new ArrayList<>(m_ObservationStore.newQuery()
                .withAssetType(ASSETTYPE).withRange(5, 10).execute());
        
        // from is inclusive, to is exclusive so it should first 5 newest are skipped, then 5 after that
        assertThat(actualObs.size(), is(5));
        JaxbUtil.assertEqualContent(actualObs.get(0), expectedObs.get(14));
        JaxbUtil.assertEqualContent(actualObs.get(1), expectedObs.get(13));
        JaxbUtil.assertEqualContent(actualObs.get(2), expectedObs.get(12));
        JaxbUtil.assertEqualContent(actualObs.get(3), expectedObs.get(11));
        JaxbUtil.assertEqualContent(actualObs.get(4), expectedObs.get(10));
        
        // do with range all the way to the end
        actualObs = new ArrayList<>(m_ObservationStore.newQuery()
                .withAssetType(ASSETTYPE).withRange(15, 20).execute());
        
        // from is inclusive, to is exclusive so it should be the 5 oldest ones, still with newest first
        assertThat(actualObs.size(), is(5));
        JaxbUtil.assertEqualContent(actualObs.get(0), expectedObs.get(4));
        JaxbUtil.assertEqualContent(actualObs.get(1), expectedObs.get(3));
        JaxbUtil.assertEqualContent(actualObs.get(2), expectedObs.get(2));
        JaxbUtil.assertEqualContent(actualObs.get(3), expectedObs.get(1));
        JaxbUtil.assertEqualContent(actualObs.get(4), expectedObs.get(0));
    }
    
    /**
     * Verify can use some of the same query methods to remove observations
     */
    public void testObservationRemoval()
        throws ValidationFailedException, IllegalArgumentException, PersistenceFailedException
    {
        List<Observation> expectedObs = new ArrayList<>();
        for (int i = 0; i < 20; i++)
        {
            Observation obs = createObservation();
            obs.setCreatedTimestamp((long)i);
            expectedObs.add(obs);
            m_ObservationStore.persist(expectedObs.get(i));
        }
        
        long obsRemoved = m_ObservationStore.newQuery().withAssetType(ASSETTYPE).remove();
        assertThat(obsRemoved, is((long)expectedObs.size()));
        List<Observation> obsLeft = new ArrayList<>(m_ObservationStore.newQuery().withAssetType(ASSETTYPE).execute());
        
        //should be the 0 left
        assertThat(obsLeft.size(), is(0));
    }
    
    /**
     * Test Persistent Data Store.
     */
    public void testPersistentDataStore() 
        throws IllegalArgumentException, PersistenceFailedException, InterruptedException
    {
        final ExampleNonLexiconData exampleDataType = new ExampleNonLexiconData(12345L, "testString");
        
        final String dataDescriptor = "Long";
        final UUID uuid = UUID.randomUUID();
        
        PersistentData pData = m_PersistentDataStore.find(uuid);
        assertThat(pData, is(nullValue()));
        
        final Long lowerBound = new Date().getTime();
        final PersistentData pDataRet = m_PersistentDataStore.persist(ExampleNonLexiconData.class, uuid, 
                dataDescriptor, exampleDataType);
        assertThat(pDataRet, is(notNullValue()));
        final Long upperBound = new Date().getTime();
        
        pData = m_PersistentDataStore.find(uuid);
        assertThat(pData, is(notNullValue()));
        
        assertThat((ExampleNonLexiconData)pData.getEntity(), is(exampleDataType));
        assertThat(pData.getContext(), is(ExampleNonLexiconData.class.getName()));
        assertThat(pData.getDescription(), is(dataDescriptor));
        assertThat(pData.getUUID(), is(uuid));
        assertThat("Timestamp lower than expected", pData.getTimestamp() >= lowerBound);
        assertThat("Timestamp higher than expected", pData.getTimestamp() <= upperBound);
        
        final Collection<? extends PersistentData> pDataColl = 
                m_PersistentDataStore.query(ExampleNonLexiconData.class);
        boolean foundNewlyPersisted = false;
        for (PersistentData pDataItem : pDataColl)
        {
            Logging.log(LogService.LOG_INFO, "%s %s %s: Collection Entity Value = %d %s",
                    pDataItem.getContext(),
                    pDataItem.getDescription(),
                    pDataItem.getTimestamp(),
                    ((ExampleNonLexiconData)pDataItem.getEntity()).getNumber(),
                    ((ExampleNonLexiconData)pDataItem.getEntity()).getString());
            if (uuid.equals(pDataItem.getUUID()))
            {
                foundNewlyPersisted = true;
            }
        }
        
        assertThat(foundNewlyPersisted, is(true));
    }
    
    /**
     * Verify correct number of entries is returned based on queries.
     */
    public void testPersistentDataStoreQueries() throws IllegalArgumentException, PersistenceFailedException,
        InterruptedException 
    {
        final ExampleNonLexiconData exampleDataType = new ExampleNonLexiconData(12345L, "testString");
        final ExampleNonLexiconData exampleDataType2 = new ExampleNonLexiconData(12345L, 
                "testString2");
        
        //clear out data store
        m_PersistentDataStore.removeMatching(ExampleNonLexiconData.class);
        
        //record the begin time and end time of the first entry
        Long startTime = System.currentTimeMillis();
        m_PersistentDataStore.persist(ExampleNonLexiconData.class, UUID.randomUUID(), "Long",
                exampleDataType);
        Long endTime = System.currentTimeMillis();
        
        //add one second delay before next data store entries
        Thread.sleep(1000);
        
        //add two more entries
        m_PersistentDataStore.persist(ExampleNonLexiconData.class, UUID.randomUUID(), "Long", 
                exampleDataType2);
        m_PersistentDataStore.persist(ExampleNonLexiconData.class, UUID.randomUUID(), "Short", 
                exampleDataType2);
        
        //query by context
        Collection<? extends PersistentData> pDataColl = m_PersistentDataStore.query(ExampleNonLexiconData.class);
        assertThat(pDataColl.size(), is(3));
        
        //query by context and description
        pDataColl = m_PersistentDataStore.query(ExampleNonLexiconData.class, "Long");        
        assertThat(pDataColl.size(), is(2));

        //query by context and date
        pDataColl = m_PersistentDataStore.query(ExampleNonLexiconData.class, new Date(startTime), new Date(endTime));
        assertThat(pDataColl.size(), is(1));
        
        //query by context, description, and date
        pDataColl = m_PersistentDataStore.query(ExampleNonLexiconData.class, "Short", new Date(startTime), 
                new Date(endTime));
        assertThat(pDataColl.size(), is(0));
    }
}
