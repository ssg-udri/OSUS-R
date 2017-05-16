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
package edu.udayton.udri.asset.axis;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.GetPanTiltCommand;
import mil.dod.th.core.asset.commands.GetPanTiltResponse;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.asset.commands.SetPanTiltCommand;
import mil.dod.th.core.asset.commands.SetPanTiltResponse;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.observation.types.ImageMetadata;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.core.types.DigitalMedia;
import mil.dod.th.core.types.factory.SpatialTypesFactory;
import mil.dod.th.core.types.image.Camera;
import mil.dod.th.core.types.image.CameraTypeEnum;
import mil.dod.th.core.types.image.ImageCaptureReason;
import mil.dod.th.core.types.image.ImageCaptureReasonEnum;
import mil.dod.th.core.types.image.PictureTypeEnum;
import mil.dod.th.core.types.image.PixelResolution;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.ose.shared.pm.CountingWakeLock;
import mil.dod.th.ose.shared.pm.CountingWakeLock.CountingWakeLockHandle;
import mil.dod.th.ose.utils.UrlService;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.log.LogService;

/**
 * AXIS Camera Asset implementation.
 * 
 * @author Admin
 */
// TD: fix data coupling, this is mostly an example asset
@SuppressWarnings("classdataabstractioncoupling")
@Component(factory = Asset.FACTORY)
public class AxisAsset implements AssetProxy
{     
    /**
     * Service which simplifies the commands supported by this asset.
     */
    private CommandProcessor m_CommandProcessor;
    
    /**
     * Service which constructs URLs.
     */
    private UrlService m_UrlService;
    
    /**
     * Reference to the asset context for the specific asset instance.
     */
    private AssetContext m_Context;
    
    /**
     * String representation of the IP address of the axis camera.
     */
    private String m_IpAddress;
    
    /**
     * Reference to the counting {@link WakeLock} used by this asset.
     */
    private CountingWakeLock m_CountingLock = new CountingWakeLock();
    
    /**
     * Bind the command processor.
     * @param commandProcessor
     *      the service which simplifies the commands for this asset
     */
    @Reference
    public void setCommandProcessor(final CommandProcessor commandProcessor)
    {
        m_CommandProcessor = commandProcessor;
    }
    
    /**
     * Bind the URL service.
     * 
     * @param urlService
     *      the service which 
     */
    @Reference
    public void setUrlService(final UrlService urlService)
    {
        m_UrlService = urlService;
    }

    @Override
    public void initialize(final AssetContext context, final Map<String, Object> props) throws FactoryException 
    {
        final AxisAssetAttributes attributes = Configurable.createConfigurable(AxisAssetAttributes.class, props);
        m_Context = context;
        m_IpAddress = attributes.ipAddress();
        m_CountingLock.setWakeLock(m_Context.createPowerManagerWakeLock(getClass().getSimpleName() + "WakeLock"));
    }
    
    /**
     * Method that gets called when the asset is deleted.
     */
    @Deactivate
    public void deactivateInstance()
    {
        m_CountingLock.deleteWakeLock();
    }
    
    @Override
    public void updated(final Map<String, Object> props)throws ConfigurationException 
    {
        final AxisAssetAttributes attributes = Configurable.createConfigurable(AxisAssetAttributes.class, props);
        m_IpAddress = attributes.ipAddress();
    }

    @Override
    public void onActivate() throws AssetException
    {
        Logging.log(LogService.LOG_DEBUG, "AXIS Camera Asset activated");
        m_Context.setStatus(SummaryStatusEnum.GOOD, "Asset Activated");
    }

    @Override
    public void onDeactivate() throws AssetException
    {
        Logging.log(LogService.LOG_DEBUG, "AXIS Camera Asset deactivated");
        m_Context.setStatus(SummaryStatusEnum.OFF, "Asset Deactivated");
    }

    @Override
    public Observation onCaptureData() throws AssetException
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            final byte[] imageData = writeImage();
            if (imageData == null)
            {
                throw new AssetException(String.format(
                        "Image capture from Axis Camera [%s] is empty.", m_Context.getName()));
            }

            final ImageMetadata imageMeta = new ImageMetadata();
            imageMeta.setColor(true);
            imageMeta.setImager(new Camera().withType(CameraTypeEnum.VISIBLE));
            imageMeta.setPictureType(PictureTypeEnum.FULL_FIELD_OF_VIEW);
            final int pixWidth = 752;
            final int pixHeight = 480;
            imageMeta.setResolution(new PixelResolution(pixWidth, pixHeight));
            imageMeta.setImageCaptureReason(new ImageCaptureReason(ImageCaptureReasonEnum.MANUAL, 
                    "Manual order to capture image."));

            final DigitalMedia digitalMedia = new DigitalMedia(imageData, "image/jpg");
            final Observation obs = new Observation().withDigitalMedia(digitalMedia).withImageMetadata(imageMeta);

            Logging.log(LogService.LOG_DEBUG, "AXIS Camera Asset data captured");
            return obs;
        }
    }

    @Override
    public Observation onCaptureData(final String sensorId) throws AssetException
    {
        throw new AssetException(
            new UnsupportedOperationException("AxisAsset does not support capturing data by sensorId."));
    }

    @Override
    public Status onPerformBit() throws AssetException
    {
        throw new UnsupportedOperationException("AXIS Camera Asset does not support performing a BIT");
    }
    
    @Override
    public Response onExecuteCommand(final Command capabilityCommand) throws CommandExecutionException
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            Logging.log(LogService.LOG_DEBUG, capabilityCommand.getClass().getSimpleName());
            if (capabilityCommand instanceof SetPanTiltCommand)
            {
                final SetPanTiltCommand setPT = (SetPanTiltCommand)capabilityCommand;
                final String url = m_CommandProcessor.processSetPanTilt(setPT, m_IpAddress);
                try
                {
                    final URLConnection connection = m_UrlService.constructUrlConnection(url);
                    connection.getDate();
                }
                catch (final IOException e)
                {
                    throw new CommandExecutionException(e);
                }

                return new SetPanTiltResponse();
            }
            else if (capabilityCommand instanceof GetPanTiltCommand)
            {
                final String url = m_CommandProcessor.processGetPanTilt(m_IpAddress);
                final StringBuilder stringBuilder = new StringBuilder();
                try
                {
                    final URLConnection connection = m_UrlService.constructUrlConnection(url);
                    try (InputStream inStream = connection.getInputStream();
                            Reader reader = new InputStreamReader(inStream))
                    {
                        int data = reader.read();
                        while (data != -1)
                        {
                            stringBuilder.append((char)data);
                            data = reader.read();
                        }
                    }
                }
                catch (final IOException e)
                {
                    throw new CommandExecutionException(e);
                }

                final String positionQuery = stringBuilder.toString();
                final String[] props = positionQuery.split("\n");
                final String pan = props[0];
                final String tilt = props[1];
                final String splitter = "=";
                final Float panValue = Float.valueOf(pan.split(splitter)[1]);
                final Float tiltValue = Float.valueOf(tilt.split(splitter)[1]);

                return new GetPanTiltResponse().withPanTilt(SpatialTypesFactory
                        .newOrientationOffset(panValue, tiltValue));
            }
            else
            {
                throw new CommandExecutionException(
                        String.format("Could not execute command [%s].", capabilityCommand));
            }
        }
    }

    @Override
    public Set<Extension<?>> getExtensions()
    {
        return new HashSet<Extension<?>>();
    }

    /**
     * Write a still image.
     * @return
     *      byte array containing the image data
     * @throws AssetException
     *      if the image data is invalid or unable to be acquired 
     */
    private byte[] writeImage() throws AssetException
    {
        final String urlString = m_CommandProcessor.processStillImageRequest(m_IpAddress);

        URLConnection connection = null;
        try 
        {
            connection = m_UrlService.constructUrlConnection(urlString);
        }
        catch (final IOException e)
        {
            throw new AssetException(e);
        }
        
        try (InputStream inStream = connection.getInputStream())
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final int size = 1024;
            final byte[] buffer = new byte[size];
            
            for (int readNum; (readNum = inStream.read(buffer)) != -1;) //NOCHECKSTYLE avoid inner assignments
            {
                baos.write(buffer, 0, readNum);
            }
            
            final byte[] imageData = baos.toByteArray();
            
            baos.flush();
            baos.close();
            return imageData;
        }
        catch (final IOException e)
        {
            throw new AssetException(e);
        }
    }
}
