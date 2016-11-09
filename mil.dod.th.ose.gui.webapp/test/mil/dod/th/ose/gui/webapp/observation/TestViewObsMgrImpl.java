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
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import mil.dod.th.core.observation.types.ImageMetadata;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.types.DigitalMedia;
import mil.dod.th.core.types.image.PixelResolution;
import mil.dod.th.ose.utils.ImageIOService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.primefaces.model.StreamedContent;

/**
 * Test class for {@link ViewObsMgrImpl}.
 * 
 * @author cweisenborn
 */
public class TestViewObsMgrImpl
{
    private ViewObsMgrImpl m_SUT;
    
    private BufferedImage m_Buffered;
    
    private ImageIOService m_ImageIOService;
    
    private final byte[] m_Data = {25, 30, 45, 67, 19, 122, 5};
    
    private final String m_Encoding = "image/jpeg";    
    
    @Before
    public void setup() throws IOException
    {
        m_SUT = new ViewObsMgrImpl();
        m_ImageIOService = mock(ImageIOService.class);
        m_Buffered = mock(BufferedImage.class);
        m_SUT.setImageIOService(m_ImageIOService);
        when(m_ImageIOService.read(Mockito.any(InputStream.class))).thenReturn(m_Buffered);
        when(m_Buffered.getHeight()).thenReturn(108);
        when(m_Buffered.getWidth()).thenReturn(192);
    }
    
    /**
     * Test that the getObservation method returns an observation object.
     * Verify that the observation matches the observation that was set.
     */
    @Test
    public void testGetObservation()
    {
        final Observation obs = buildTestObsHighRes();
        m_SUT.setObservation(obs);
        
        assertThat(m_SUT.getObservation(), is(obs));
    }
    
    /**
     * Test that the getMedia method returns a streamed content object.
     * Verify the contents of the streamed content are correct.
     */
    @Test
    public void testGetMedia() throws IOException
    {
        //Test observation with an image that has a resolution greater than the max is allowed to be displayed. Should
        //be scaled appropriately.
        Observation obs = buildTestObsHighRes();
        m_SUT.setObservation(obs);
        
        //Verify that the image is scaled correctly since it's height and width are greater than the max allowed.
        assertThat(m_SUT.getHeight(), is(391));
        assertThat(m_SUT.getWidth(), is(470));
        
        //Verify that the proper encoding type is set for the streamed content.
        final StreamedContent media = m_SUT.getMedia();
        assertThat(media.getContentType(), is(m_Encoding));
        
        //Verify that the bytes stored within the stream content are correct.
        int index = 0;
        int dataByte = 0;
        final InputStream dataStream = media.getStream();
        while ((dataByte = dataStream.read()) != -1)
        {
            assertThat((byte)dataByte, is(m_Data[index]));
            index++;
        }
    }
    
    /**
     * Test that an observation with an image that has a resolution less than the max is allowed to be displayed.
     * Should not be scaled.
     */
    @Test
    public void testLowResObsDisplay()
    {
        Observation obs = buildTestObsLowRes();
        m_SUT.setObservation(obs);
        
        //Verify that the image is not scaled since it's height and width are less than the max allowed.
        assertThat(m_SUT.getHeight(), is(100));
        assertThat(m_SUT.getWidth(), is(200));
    }
    
    /**
     * Test that an observation with an image that has a resolution greater than the max is allowed to be displayed.
     * Should be scaled appropriately.
     */
    @Test
    public void testHighResObsDisplay()
    {
        Observation obs = buildTestObsHighRes();
        obs.getImageMetadata().setResolution(new PixelResolution(370, 850));
        m_SUT.setObservation(obs);
        
        //Verify that the image is scaled since it's height is greater than the max allowed.
        assertThat(m_SUT.getHeight(), is(470));
        assertThat(m_SUT.getWidth(), is(204));
    }

    /**
     * Test that an observation with an image that has no resolution is allowed to be displayed.
     */
    @Test
    public void testNoResObsDisplay() throws IOException
    {        
        //Test observation with an image that has no resolution metadata.
        Observation obs = buildTestObsNoRes();
        m_SUT.setObservation(obs);
        
        //Verify that the image is not scaled since it's height and width are less than the max allowed.
        assertThat(m_SUT.getHeight(), is(108));
        assertThat(m_SUT.getWidth(), is(192));
    }
    
    /**
     * Test that the hasResolution method returns true when a resolution is deciphered or false when not.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testHasResolution() throws IOException
    {
        // The observation has resolution metadata. It should report that it does have a resolution.
        Observation obs = buildTestObsLowRes();
        m_SUT.setObservation(obs);
        assertThat(m_SUT.hasResolution(), is(true));
        
        // The observation has metadata but not resolution metadata, but the image itself has a resolution.
        // It should report that it does have a resolution.
        obs = buildTestObsNoRes();
        final ImageMetadata imgMeta = new ImageMetadata();
        obs.setImageMetadata(imgMeta);
        assertThat(m_SUT.hasResolution(), is(true));
        
        // The observation has no resolution metadata but the image does have a resolution. 
        // It should report that it does have a resolution.
        obs = buildTestObsNoRes();
        m_SUT.setObservation(obs);
        assertThat(m_SUT.hasResolution(), is(true));
        
        // The observation has no resolution metadata and the input stream will not report a resolution
        // because of an IOException. It should report that it does not have a resolution.
        when(m_ImageIOService.read(Mockito.any(InputStream.class))).thenThrow(IOException.class);
        m_SUT.setObservation(obs);
        assertThat(m_SUT.hasResolution(), is(false));
    }
    
    /**
     * Test that the downloadMedia method returns a streamed content object.
     * Verify the contents of the streamed content object.
     */
    @Test
    public void testDownloadMedia() throws IOException
    {
        StreamedContent media = m_SUT.downloadMedia();
        //Verify that a default streamed content is returned if no observation is set.
        assertThat(media.getContentType(), is(nullValue()));
        assertThat(media.getStream(), is(nullValue()));
        
        final Observation obs = new Observation();
        obs.setDigitalMedia(new DigitalMedia(m_Data, "video/avi"));
        obs.setUuid(UUID.randomUUID());
        m_SUT.setObservation(obs);
        
        //Verify that the proper encoding type is set for the streamed content.
        media = m_SUT.downloadMedia();
        assertThat(media.getContentType(), is("video/avi"));
        
        //Verify that the bytes stored within the stream content are correct.
        int index = 0;
        int dataByte = 0;
        final InputStream dataStream = media.getStream();
        while ((dataByte = dataStream.read()) != -1)
        {
            assertThat((byte)dataByte, is(m_Data[index]));
            index++;
        }
    }
    
    /**
     * Method used to create an fake observation which can be used to test the various methods. This observation has
     * image meta data with a resolution greater than that of the max that is allowed to be displayed.
     * 
     * @return
     *          An observation with fake data.
     */
    private Observation buildTestObsHighRes()
    {
        final Observation obs = new Observation();
        obs.setAssetUuid(UUID.randomUUID());
        obs.setAssetName("bob");
        obs.setUuid(UUID.randomUUID());
        final ImageMetadata imgMeta = new ImageMetadata();
        final PixelResolution resolution = new PixelResolution(900, 750);
        imgMeta.setResolution(resolution);
        obs.setImageMetadata(imgMeta);
        final DigitalMedia media = new DigitalMedia(m_Data, m_Encoding);
        obs.setDigitalMedia(media);
        
        return obs;
    }
    
    /**
     * Method used to create an fake observation which can be used to test the various methods. This observation has
     * image meta data with a resolution less than that of the max that is allowed to be displayed.
     * 
     * @return
     *          An observation with fake data.
     */
    private Observation buildTestObsLowRes()
    {
        final Observation obs = new Observation();
        obs.setAssetUuid(UUID.randomUUID());
        obs.setAssetName("bob");
        obs.setUuid(UUID.randomUUID());
        final ImageMetadata imgMeta = new ImageMetadata();
        final PixelResolution resolution = new PixelResolution(200, 100);
        imgMeta.setResolution(resolution);
        obs.setImageMetadata(imgMeta);
        final DigitalMedia media = new DigitalMedia(m_Data, m_Encoding);
        obs.setDigitalMedia(media);
        
        return obs;
    }
    
    /**
     * Method used to create an fake observation which can be used to test the various methods. This observation has
     * no resolution in the image metadata.
     * 
     * @return
     *          An observation with fake data.
     */
    private Observation buildTestObsNoRes() throws IOException
    {
        final Observation obs = new Observation();
        obs.setAssetUuid(UUID.randomUUID());
        obs.setAssetName("bob");
        obs.setUuid(UUID.randomUUID());
        final DigitalMedia media = new DigitalMedia(m_Data, m_Encoding);
        obs.setDigitalMedia(media);
        
        return obs;
    }
}
