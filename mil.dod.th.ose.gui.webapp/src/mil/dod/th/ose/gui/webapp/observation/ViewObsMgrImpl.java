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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.inject.Inject;

import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.types.DigitalMedia;
import mil.dod.th.core.types.image.PixelResolution;
import mil.dod.th.ose.utils.ImageIOService;

import org.glassfish.osgicdi.OSGiService;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 * Implementation of the {@link ViewObsMgr} class.
 * 
 * @author cweisenborn
 */
@ManagedBean(name = "viewObsMgr")
@SessionScoped
public class ViewObsMgrImpl implements ViewObsMgr
{   
    /**
     * Integer that represents the max height/width an image is allowed to be displayed at.
     */
    private static final int MAX_RESOLUTION = 470;
    
    /**
     * Inject the ImageIO service.
     */
    @Inject @OSGiService
    private ImageIOService m_ImageIOService;
    
    /**
     * Reference to an {@link Observation}.
     */
    private Observation m_Observation;
    
    /**
     * Reference to the media object that has been converted from byte array and has been stored for display.
     */
    private StreamedContent m_Media;
    
    /**
     * Resolution existence status.
     */
    private boolean m_HasResolution;
    
    /**
     * Display height of the image.
     */
    private int m_Height;
    
    /**
     * Display width of the image.
     */
    private int m_Width;
    
    @Override
    public void setObservation(final Observation observation)
    {
        m_Media = buildOrDownloadMedia(observation);
        m_Observation = observation;
    }
    
    @Override
    public Observation getObservation()
    {
        return m_Observation;
    }
    
    /**
     * Method that sets the ImageIO service to use.
     * 
     * @param imageIOService
     *      The instance of ImageIO to use.
     */
    public void setImageIOService(final ImageIOService imageIOService)
    {
        m_ImageIOService = imageIOService;
    }
    
    @Override
    public StreamedContent getMedia()
    {
        return m_Media;
    }
    
    @Override
    public boolean hasResolution()
    {
        return m_HasResolution;
    }
    
    @Override
    public int getHeight()
    {
        return m_Height;
    }
    
    @Override
    public int getWidth()
    {
        return m_Width;
    }
    
    @Override
    public StreamedContent downloadMedia()
    {
        return buildOrDownloadMedia(m_Observation);
    }
    
    /**
     * Method that accepts an observation as a parameter and builds a file from the digital media content
     * stored within. The browser will determine if the image or video type is supported by looking at the file
     * extension. If the type is not supported  then the file will need to be downloaded to view it.
     * 
     * @param observation
     *          The observation that contains the digital media to be viewed or downloaded.
     * @return
     *          {@link StreamedContent} built from the image/video/audio stored in the observation.
     */
    private StreamedContent buildOrDownloadMedia(final Observation observation)
    {
        if (observation == null)
        {
            return new DefaultStreamedContent();
        }
        
        final DigitalMedia media = observation.getDigitalMedia();
        final String encoding = media.getEncoding();
        final byte[] data = media.getValue();
        final String extension =  "." + encoding.split("/")[1];
        final PixelResolution resolution;
        
        if (observation.isSetImageMetadata() && observation.getImageMetadata().getResolution() != null)
        {
            resolution = observation.getImageMetadata().getResolution();
            setDisplayResolution(resolution);
        }
        else
        {
            try
            {
                resolution = getImageResolution(data);
                setDisplayResolution(resolution);
            }
            catch (final IOException e)
            {
                m_HasResolution = false;
            }
        }
        
        return new DefaultStreamedContent(new ByteArrayInputStream(data), encoding, 
                    observation.getUuid().toString() + extension);
    }
    
    /**
     * Method to set the resolution to display the image.
     * 
     * @param resolution
     *          Actual image resolution.
     */
    private void setDisplayResolution(final PixelResolution resolution)
    {
        m_Width = resolution.getWidth();
        m_Height = resolution.getHeight();
        m_HasResolution = true;
        if (m_Width > MAX_RESOLUTION)
        {
            final float ratio = (float)resolution.getHeight() / resolution.getWidth();
            m_Height = (int)(MAX_RESOLUTION * ratio);
            m_Width = MAX_RESOLUTION;
        }
        if (m_Height > MAX_RESOLUTION)
        {
            final float ratio = (float)resolution.getWidth() / resolution.getHeight();
            m_Width = (int)(MAX_RESOLUTION * ratio);
            m_Height = MAX_RESOLUTION;
        }
    }
    
    /**
     * Method to determine an image's height and width.
     * 
     * @param data
     *      The raw image being examined.
     * @return
     *      Pixel resolution of the image.
     * @throws IOException 
     *      When the input stream contains invalid data.
     */
    private PixelResolution getImageResolution(final byte[] data) throws IOException
    {
        final InputStream imageStream = new ByteArrayInputStream(data);
        final BufferedImage img = m_ImageIOService.read(imageStream);
        int width = 0;
        int height = 0;
        if (img != null)
        {
            width = img.getWidth();
            height = img.getHeight();
        }
        return new PixelResolution(width, height);
    }
}
