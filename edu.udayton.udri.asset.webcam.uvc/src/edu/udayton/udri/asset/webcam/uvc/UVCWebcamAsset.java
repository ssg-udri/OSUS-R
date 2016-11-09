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
package edu.udayton.udri.asset.webcam.uvc;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.metatype.Configurable;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamException;
import com.github.sarxos.webcam.ds.v4l4j.V4l4jDriver;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.Asset.AssetActiveStatus;
import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.commands.CaptureImageCommand;
import mil.dod.th.core.asset.commands.CaptureImageResponse;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.observation.types.ImageMetadata;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.types.DigitalMedia;
import mil.dod.th.core.types.SensingModality;
import mil.dod.th.core.types.SensingModalityEnum;
import mil.dod.th.core.types.image.Camera;
import mil.dod.th.core.types.image.CameraTypeEnum;
import mil.dod.th.core.types.image.ImageCaptureReason;
import mil.dod.th.core.types.image.ImageCaptureReasonEnum;
import mil.dod.th.core.types.image.PixelResolution;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.core.validator.ValidationFailedException;

import org.osgi.service.log.LogService;

/**
 * An asset plug-in to capture images from a UVC webcam.
 */
@Component(factory = Asset.FACTORY) // NOCHECKSTYLE: Large number of dependecies required
public class UVCWebcamAsset implements AssetProxy 
{
    private AssetContext m_Context;
    private Webcam m_Webcam;
    private String m_PersistanceFailure = "Observation persistance failed";
    private Dimension m_Resolution;
    private SensingModality m_Modality;

    @Override
    public void initialize(final AssetContext context, final Map<String, Object> props) throws FactoryException
    {
        m_Context = context;
        
        final UVCWebcamAssetAttributes attr = Configurable.createConfigurable(UVCWebcamAssetAttributes.class, props);
        m_Resolution = attr.resolution().getSize();
        
        final String os = System.getProperty("os.name");
        
        if (os.contains("Linux"))
        {
            Webcam.setDriver(new V4l4jDriver());
        }
        
        try
        {
            m_Webcam = Webcam.getDefault();
        } 
        catch (final WebcamException e)
        {
            Logging.log(LogService.LOG_ERROR, e.toString());
            Logging.log(LogService.LOG_ERROR, "Failed to get default camera. Recreate asset.");
            m_Context.setStatus(SummaryStatusEnum.BAD, "Failed to get default camera");
            throw new FactoryException("Failed to initialize, could not get default camera");
        }
        
        
        final Dimension[] customResolution = new Dimension[] {m_Resolution.getSize(), };
        m_Webcam.setCustomViewSizes(customResolution);
        m_Webcam.setViewSize(m_Resolution.getSize());
        
        m_Modality = new SensingModality(SensingModalityEnum.IMAGER, "Webcam");
        
        m_Context.setStatus(SummaryStatusEnum.OFF, "Initialized");
    }
    
    @Override
    public void updated(final Map<String, Object> props)
    {
        //There are currently no attributes to be updated.
    }
    
    @Override
    public void onActivate() throws AssetException
    {
        try
        {
            m_Webcam.open();
            m_Context.setStatus(SummaryStatusEnum.GOOD, "Activated");
        }
        catch (final WebcamException e)
        {
            final String log_message = "Failed to activate";
            Logging.log(LogService.LOG_ERROR, e.toString());
            Logging.log(LogService.LOG_ERROR, log_message);
            m_Context.setStatus(SummaryStatusEnum.BAD, log_message);
            throw new AssetException(log_message);
        }
        
    }
    
    @Override
    public void onDeactivate() throws AssetException
    {
        try
        {
            m_Webcam.close();
            m_Context.setStatus(SummaryStatusEnum.OFF, "Deactivated");
        } 
        catch (final WebcamException e)
        {
            final String log_message = "Failed to deactivate";
            Logging.log(LogService.LOG_ERROR, e.toString());
            Logging.log(LogService.LOG_ERROR, log_message);
            m_Context.setStatus(SummaryStatusEnum.BAD, log_message);
            throw new AssetException(log_message);
        }
        
    }
    
    @Override
    public Observation onCaptureData() throws AssetException
    {
        final DigitalMedia media = captureImage();
        
        final String captureReasonDescription = "OnCaptureData command was recieved, manually or from a mission.";
        final ImageMetadata meta = createMetadata(captureReasonDescription);
        
        final Observation obs = new Observation()
            .withDigitalMedia(media)
            .withImageMetadata(meta)
            .withModalities(m_Modality);
        
        return obs;
    }
    
    /**
     * Method that captures an image to send back as an observation with DigitalMedia and ImageMetadata.
     * 
     * @return 
     *      the observation with image
     * @throws AssetException when capturing the image fails
     */
    public Observation onCaptureImageCommand() throws AssetException
    {
        final DigitalMedia media = captureImage();
        
        final String captureReasonDescription = "Manual capture image command was recieved.";
        final ImageMetadata meta = createMetadata(captureReasonDescription);
        
        final Observation obs = new Observation()
            .withDigitalMedia(media)
            .withImageMetadata(meta)
            .withModalities(m_Modality);
        
        try 
        {
            m_Context.persistObservation(obs);
        } 
        catch (final PersistenceFailedException | ValidationFailedException e) 
        {
            Logging.log(LogService.LOG_ERROR, e.toString());
            Logging.log(LogService.LOG_ERROR, m_PersistanceFailure);
            throw new CommandExecutionException("Failed to persist observation");
        }
        
        return obs;
    }
    
    @Override
    public Status onPerformBit() throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException("BIT is currently unsupported.");
    }
    
    @Override
    public Response onExecuteCommand(final Command command) throws CommandExecutionException
    {
        if (command instanceof CaptureImageCommand && m_Context.getActiveStatus().equals(AssetActiveStatus.ACTIVATED))
        {
            try 
            {
                onCaptureImageCommand();
                return new CaptureImageResponse();
            } 
            catch (final AssetException e) 
            {
                final String log_message = "Failed to execute Capture Image Command";
                Logging.log(LogService.LOG_ERROR, e.toString());
                Logging.log(LogService.LOG_ERROR, log_message);
                m_Context.setStatus(SummaryStatusEnum.BAD, log_message);
            }
            
        }
        
        throw new CommandExecutionException("Failed to execute command.");
    }
    
    @Override
    public Set<Extension<?>> getExtensions()
    {
        return new HashSet<Extension<?>>();
    }
    
    /**
     * Method to create the metedata that's required for the observation.
     * 
     * @param captureReasonDescription A string describing the capture reason
     * @return The ImageMetadata
     */
    public ImageMetadata createMetadata(final String captureReasonDescription)
    {
        final double widthDbl = m_Webcam.getViewSize().getWidth();
        final double heightDbl = m_Webcam.getViewSize().getHeight();
        final int width = (int) widthDbl;
        final int height = (int) heightDbl;
        final PixelResolution resolution = new PixelResolution(width, height);
        
        final Date date = new Date();
        final Long captureTime = date.getTime();
        
        final int id = 0;
        final Camera imager = new Camera(id, m_Webcam.getName(), CameraTypeEnum.UNKNOWN);

        final ImageCaptureReason captureReason = new ImageCaptureReason(ImageCaptureReasonEnum.MANUAL, 
                captureReasonDescription);
        
        final ImageMetadata meta = new ImageMetadata()
            .withColor(true)
            .withResolution(resolution)
            .withCaptureTime(captureTime)
            .withImageCaptureReason(captureReason)
            .withImager(imager);
        
        return meta;
    }
     
    /**
     * Method to grab a frame from the video input and return the DigitalMedia.
     * 
     * @return The DigitalMedia with the captured image 
     * @throws AssetException when it failes to write the buffered image to the byte stream
     */
    public DigitalMedia captureImage() throws AssetException
    {
        final BufferedImage bufferedImage = m_Webcam.getImage();
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        byte[] bytes = null;
        
        try 
        {
            ImageIO.write(bufferedImage, "png", stream);
            bytes = stream.toByteArray();
        } 
        catch (final Exception e) 
        {
            final String log_message = "Failed to write buffered image to byte stream.";
            Logging.log(LogService.LOG_ERROR, e.toString());
            Logging.log(LogService.LOG_ERROR, log_message);
            throw new AssetException(log_message);
        }
        
        final DigitalMedia media = new DigitalMedia(bytes, "image/png");
        return media;
    }
}
    