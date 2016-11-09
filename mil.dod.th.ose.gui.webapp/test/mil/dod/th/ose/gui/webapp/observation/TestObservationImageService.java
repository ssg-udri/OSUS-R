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
package mil.dod.th.ose.gui.webapp.observation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import mil.dod.th.core.types.observation.ObservationSubTypeEnum;

import org.junit.Test;

/**
 * @author nickmarcucci
 *
 */
public class TestObservationImageService
{
    private ObservationImageService m_SUT;
    
    /**
     * Verify that the correct mini observation image path is returned for the given inputs.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testTryGetMiniObservationImage()
    {
        m_SUT = new ObservationImageService();
        
        GuiObservation obs = mock(GuiObservation.class);
        
        List<RelatedObservationIdentity> refIds = new ArrayList<>();
        refIds.add(new RelatedObservationIdentity(true, ObservationSubTypeEnum.DETECTION));
        refIds.add(new RelatedObservationIdentity(true, ObservationSubTypeEnum.AUDIO_METADATA));
        refIds.add(new RelatedObservationIdentity(true, null));
        
        when(obs.getRelatedObservationModels()).thenReturn(refIds);
        
        assertThat(m_SUT.tryGetMiniObservationImage(obs, -1), is("thoseIcons/observations/mini-icons/unknown.png"));
        assertThat(m_SUT.tryGetMiniObservationImage(obs, 0), is("thoseIcons/observations/mini-icons/detection.png"));
        assertThat(m_SUT.tryGetMiniObservationImage(obs, 1), 
                is("thoseIcons/observations/mini-icons/audio_metadata.png"));
        assertThat(m_SUT.tryGetMiniObservationImage(obs, 2), is("thoseIcons/observations/mini-icons/unknown.png"));
        assertThat(m_SUT.tryGetMiniObservationImage(obs, 3), is("thoseIcons/observations/mini-icons/unknown.png"));
        
        when(obs.getRelatedObservationModels()).thenReturn(new ArrayList<RelatedObservationIdentity>());

        assertThat(m_SUT.tryGetMiniObservationImage(obs, 1), is("thoseIcons/observations/mini-icons/unknown.png"));
        
        assertThat(m_SUT.tryGetMiniObservationImage(null, -1), is("thoseIcons/observations/mini-icons/unknown.png"));

        List<RelatedObservationIdentity> list = mock(ArrayList.class);
        when(list.size()).thenReturn(1);
        when(obs.getRelatedObservationModels()).thenReturn(list);
        
        when(list.get(0)).thenReturn(null);
        assertThat(m_SUT.tryGetMiniObservationImage(obs, 0), is("thoseIcons/observations/mini-icons/unknown.png"));
    }
    
    /**
     * Verify that the correct image path is returned for regular sized observation images.
     */
    @Test
    public void testTryGetObservationImage()
    {
        m_SUT = new ObservationImageService();
        
        assertThat(m_SUT.tryGetObservationImage(ObservationSubTypeEnum.DETECTION), 
                is("thoseIcons/observations/detection.png"));
        
        assertThat(m_SUT.tryGetObservationImage(null), is("thoseIcons/observations/unknown.png"));
    }
}
