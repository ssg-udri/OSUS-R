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
package mil.dod.th.ose.controller.integration.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;

import org.junit.Test;

import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.types.Version;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.types.MapEntry;
import mil.dod.th.ose.controller.integration.ObservationHelper;
import mil.dod.th.ose.junit4xmltestrunner.IntegrationTestRunner;
import mil.dod.th.ose.test.matchers.JaxbUtil;

/**
 * @author Dave Humeniuk
 *
 */
public class TestPersistedObservations
{
    /**
     * Verify system mode and version attributes are properly stored in the observation store.
     */
    @Test
    public void testObservationRestored()
    {
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        assertThat(observationStore, is(notNullValue()));
        
        Observation expectedObs = ObservationHelper.createBasicObservation();
        Observation persistedObs = observationStore.find(expectedObs.getUuid());

        //verify that the system mode was persisted
        assertThat(persistedObs.isSystemInTestMode(), is(true));
        
        //the version field 
        Version version = persistedObs.getVersion();
        assertThat(version.getMajorNumber(), is(1));
        assertThat(version.getMinorNumber(), is(2));
        
        //reserved fields
        List<MapEntry> reserved = persistedObs.getReserved();
        assertThat(reserved.get(0).getKey(), is("String"));
        assertThat(reserved.get(0).getValue(), is((Object)"Bob"));
        assertThat(reserved.get(1).getKey(), is("Double"));
        assertThat(reserved.get(1).getValue(), is((Object)2.25));
        assertThat(reserved.get(2).getKey(), is("Integer"));
        assertThat(reserved.get(2).getValue(), is((Object)100));
        assertThat(reserved.get(3).getKey(), is("Boolean"));
        assertThat(reserved.get(3).getValue(), is((Object)true));
        assertThat(reserved.get(4).getKey(), is("Long"));
        assertThat(reserved.get(4).getValue(), is((Object)12345L));
    }
  
    /**
     * Verify standard image observation created by {@link ObservationHelper} is the same is what is pull from the 
     * {@link ObservationStore} after system restart (actually persisted).
     */
    @Test
    public void testImageObservationRestored()
    {       
        Observation expectedObs = ObservationHelper.createCompleteImageObservation();
        
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        Observation persistedObs = observationStore.find(expectedObs.getUuid());
        
        JaxbUtil.assertEqualContent(persistedObs, expectedObs);
    }
    
    /**
     * Verify standard coordinates observation created by {@link ObservationHelper}
     * is the same is what is pull from the {@link ObservationStore} after system restart (actually persisted).
     */
    @Test
    public void testCoordinatesObservationRestored()
    {
        Observation expectedObs = ObservationHelper.createCoordinatesObservation();
        
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        Observation persistedObs = observationStore.find(expectedObs.getUuid());
        
        JaxbUtil.assertEqualContent(persistedObs, expectedObs);
    }
    
    /**
     * Verify standard orientation observation created by {@link ObservationHelper} 
     * is the same is what is pull from the {@link ObservationStore} after system restart (actually persisted).
     */
    @Test
    public void testOrientationObservationRestored()
    {    
        Observation expectedObs = ObservationHelper.createOrientationObservation();
        
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        Observation persistedObs = observationStore.find(expectedObs.getUuid());
        
        JaxbUtil.assertEqualContent(persistedObs, expectedObs);
    }
    
    /**
     * Verify standard detection observation created by {@link ObservationHelper} 
     * is the same is what is pull from the {@link ObservationStore} after system restart (actually persisted).
     */
    @Test
    public void testDetectionObservationRestored()
    {
        for (int i = 1; i <= 4; i++)
        {
            Observation expectedObs = ObservationHelper.createDetectionObservation(i);
        
            ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
            Observation persistedObs = observationStore.find(expectedObs.getUuid());
        
            JaxbUtil.assertEqualContent(persistedObs, expectedObs);
        }
    }
     
    /**
     * Verify standard status observation created by {@link ObservationHelper} 
     * is the same is what is pull from the {@link ObservationStore} after system restart (actually persisted).
     * @throws DatatypeConfigurationException 
     *      thrown when there is a duration configuration error
     */
    @Test
    public void testStatusObservationRestored() throws DatatypeConfigurationException
    {   
        Observation expectedObs = ObservationHelper.createStatusObservation();
        
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        Observation persistedObs = observationStore.find(expectedObs.getUuid());
        
        JaxbUtil.assertEqualContent(persistedObs, expectedObs);
    }
    
    /**
     * Verify standard orientation observation created by {@link ObservationHelper} 
     * is the same is what is pull from the {@link ObservationStore} after system restart (actually persisted).
     */
    @Test
    public void testWeatherObservationRestored()
    {
        Observation expectedObs = ObservationHelper.createWeatherObservation();
        
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        Observation persistedObs = observationStore.find(expectedObs.getUuid());
        
        JaxbUtil.assertEqualContent(persistedObs, expectedObs);
    }
    
    /**
     * Verify standard audio observation created by {@link ObservationHelper} 
     * is the same is what is pull from the {@link ObservationStore} after system restart (actually persisted).
     */
    @Test
    public void testAudioObservationRestored()
    {
        Observation expectedObs = ObservationHelper.createAudioObservation();
        
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        Observation persistedObs = observationStore.find(expectedObs.getUuid());
        
        JaxbUtil.assertEqualContent(persistedObs, expectedObs);
    }
    
    /**
     * Verify standard video observation created by {@link ObservationHelper} 
     * is the same is what is pull from the {@link ObservationStore} after system restart (actually persisted).
     */
    @Test
    public void testVideoObservationRestored()
    {      
        Observation expectedObs = ObservationHelper.createVideoObservation();
        
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        Observation persistedObs = observationStore.find(expectedObs.getUuid());
        
        JaxbUtil.assertEqualContent(persistedObs, expectedObs);
    }
    
    /**
     * Verify complete biological observation created by {@link ObservationHelper} 
     * is the same as what is pull from the {@link ObservationStore} after system restart (actually persisted).
     */
    @Test
    public void testBiologicalObservationRestored()
    {
        Observation expectedObs = ObservationHelper.createBiologicalObservation();
        
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        Observation persistedObs = observationStore.find(expectedObs.getUuid());
        
        JaxbUtil.assertEqualContent(persistedObs, expectedObs);
    }
    
    /**
     * Verify complete chemical observation created by {@link ObservationHelper} 
     * is the same as what is pull from the {@link ObservationStore} after system restart (actually persisted).
     */
    @Test
    public void testChemicalObservationRestored()
    {
        Observation expectedObs = ObservationHelper.createChemicalObservation();
        
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        Observation persistedObs = observationStore.find(expectedObs.getUuid());
        
        JaxbUtil.assertEqualContent(persistedObs, expectedObs);
    }
    
    /**
     * Verify complete Cbrne trigger observation created by {@link ObservationHelper} 
     * is the same as what is pull from the {@link ObservationStore} after system restart (actually persisted).
     */
    @Test
    public void testCbrneTriggerObservationRestored()
    {
        Observation expectedObs = ObservationHelper.createCbrneTriggerObservation();
        
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        Observation persistedObs = observationStore.find(expectedObs.getUuid());
        
        JaxbUtil.assertEqualContent(persistedObs, expectedObs);
    }
    
    /**
     * Verify complete water quality observation created by {@link ObservationHelper} 
     * is the same as what is pull from the {@link ObservationStore} after system restart (actually persisted).
     */
    @Test
    public void testWaterQualityObservationRestored()
    {
        Observation expectedObs = ObservationHelper.createWaterQualityObservation();
        
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        Observation persistedObs = observationStore.find(expectedObs.getUuid());
        
        JaxbUtil.assertEqualContent(persistedObs, expectedObs);
    }
}
