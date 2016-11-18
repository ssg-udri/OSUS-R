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
package edu.udayton.udri.asset.canon.ipcamera;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.Asset.AssetActiveStatus;
import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.GetCameraSettingsCommand;
import mil.dod.th.core.asset.commands.GetCameraSettingsResponse;
import mil.dod.th.core.asset.commands.GetPanTiltCommand;
import mil.dod.th.core.asset.commands.GetPanTiltResponse;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.asset.commands.SetCameraSettingsCommand;
import mil.dod.th.core.asset.commands.SetCameraSettingsResponse;
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
import mil.dod.th.core.types.image.Camera;
import mil.dod.th.core.types.image.CameraTypeEnum;
import mil.dod.th.core.types.image.ImageCaptureReason;
import mil.dod.th.core.types.image.ImageCaptureReasonEnum;
import mil.dod.th.core.types.image.PictureTypeEnum;
import mil.dod.th.core.types.image.PixelResolution;
import mil.dod.th.core.types.spatial.AzimuthDegrees;
import mil.dod.th.core.types.spatial.ElevationDegrees;
import mil.dod.th.core.types.spatial.OrientationOffset;
import mil.dod.th.core.types.status.OperatingStatus;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.ose.shared.pm.CountingWakeLock;
import mil.dod.th.ose.shared.pm.CountingWakeLock.CountingWakeLockHandle;

import org.osgi.service.log.LogService;

/**
 * Plug-in for the Canon VB-C60 IP camera implementation.
 * 
 * @author Timothy
 */
@Component(factory = Asset.FACTORY) //NOCHECKSTYLE, Fan Out Complexity, large number of import due to commands.
public class IpCameraAsset implements AssetProxy
{
    private static String INFOURL = "info.cgi";
    private static final double TILT_ADJUST = 100;
    private static final double PAN_ADJUST = 94.4;
    private static final int ZOOM_ADJUST = 5000;
    private static final String URL_HEAD = "http://";
    private static final String URL_TAIL = "/-wvhttp-01-/";

    private AssetContext m_Context;
    private IpCameraAssetAttributes m_Config;
    private URL m_Url;
    private UrlUtils m_UrlUtil;

    /**
     * Reference to the counting {@link WakeLock} used by this asset.
     */
    private CountingWakeLock m_CountingLock = new CountingWakeLock();
    
    
    @Override
    public void initialize(final AssetContext context, final Map<String, 
            Object> props) throws FactoryException
    {
        m_Context = context;
        m_Config = Configurable.createConfigurable(IpCameraAssetAttributes.class, props);
        m_CountingLock.setWakeLock(m_Context.createPowerManagerWakeLock(getClass().getSimpleName() + "WakeLock"));
    }
    
    /**
     * OSGi deactivate method used to delete any wake locks used by the asset.
     */
    @Deactivate
    public void tearDown()
    {
        m_CountingLock.deleteWakeLock();
    }
    
    @Reference
    public void setUrlUtil(final UrlUtils urlConnection)
    {
        m_UrlUtil = urlConnection;
    }

    @Override
    public void updated(final Map<String, Object> props)
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            m_Config = Configurable.createConfigurable(IpCameraAssetAttributes.class, props);
            final String newUrlString = URL_HEAD + m_Config.ipAddress() + URL_TAIL;
            final URL newUrl;
            try 
            {
                newUrl = new URL(newUrlString);
            }
            catch (final MalformedURLException ex) 
            {
                m_Context.setStatus(SummaryStatusEnum.BAD, "Failure creating an updated URL");
                Logging.log(LogService.LOG_ERROR, ex, "Failure creating new URL");
                return;
            }
            if (!(m_Url.toString().equals(newUrl.toString())))
            {
                m_Url = newUrl;
                final URLConnection urlConnection;
                try
                {
                    urlConnection = getUrlConnection(m_Url);
                }
                catch (final CommandExecutionException ex)
                {
                    Logging.log(LogService.LOG_ERROR, ex, "failed creating a connection to the IP camera");
                    m_Context.setStatus(SummaryStatusEnum.BAD, "Failed to create a connection to the camera");
                    return;
                }
                final InputStream infoInput;
                try
                {
                    infoInput = getInputStream(urlConnection); 
                }
                catch (final CommandExecutionException ex)
                {
                    Logging.log(LogService.LOG_ERROR, ex, "Error creating the input stream to camera");
                    m_Context.setStatus(SummaryStatusEnum.BAD, "Failed to create an input stream from the camera");
                    return;
                }
                final Properties infoProp = new Properties();
                try
                {
                    infoProp.load(infoInput);
                }
                catch (final IOException ex)
                {
                    Logging.log(LogService.LOG_ERROR, ex, "creating properties file for camera");
                    m_Context.setStatus(SummaryStatusEnum.BAD, "Failure retreiving data from the new input stream");
                    return;
                }
            }
            Logging.log(LogService.LOG_DEBUG, "New URL: " + URL_HEAD + m_Config.ipAddress() + URL_TAIL);
            m_Context.setStatus(SummaryStatusEnum.GOOD, "Failure retreiving data from the input stream");
        }
    }

    @Override
    public void onActivate() throws AssetException
    {
        Logging.log(LogService.LOG_INFO, "Plug-in for the Canon VB-C60 IP camera activated");
        try
        {
            m_Url = new URL(URL_HEAD + m_Config.ipAddress() + URL_TAIL);
        }
        catch (final MalformedURLException ex)
        {
            Logging.log(LogService.LOG_ERROR, ex, "Bad IP Address");
            throw new AssetException(ex);
        }
        final Status status = new Status().withSummaryStatus(
                new OperatingStatus(SummaryStatusEnum.GOOD, "Asset Activated"));
        try
        {
            m_Context.setStatus(status);
        }
        catch (final ValidationFailedException ex)
        {
            throw new AssetException(ex);
        }
    }

    @Override
    public void onDeactivate() throws AssetException
    {
        Logging.log(LogService.LOG_INFO, "Plug-in for the Canon VB-C60 IP camera deactivated");
        final Status status = new Status().withSummaryStatus(
                new OperatingStatus(SummaryStatusEnum.OFF, "Asset Deactivated"));
        try
        {
            m_Context.setStatus(status);
        }
        catch (final ValidationFailedException ex)
        {
            throw new AssetException(ex);
        }
    }

    @Override
    public Observation onCaptureData() throws CommandExecutionException
    {
        if (m_Context.getActiveStatus() != AssetActiveStatus.ACTIVATED)
        {
            Logging.log(LogService.LOG_ERROR, "Asset is not Activated");
            throw new CommandExecutionException("Asset not Activated");
        }

        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            final ImageMetadata imageMeta = new ImageMetadata();
            imageMeta.setColor(true);
            imageMeta.setImager(new Camera(0, "Example Camera", CameraTypeEnum.VISIBLE));
            imageMeta.setPictureType(PictureTypeEnum.FULL_FIELD_OF_VIEW);
            final int pixWidth = 320;
            final int pixHeight = 240;
            imageMeta.setResolution(new PixelResolution(pixWidth, pixHeight));
            imageMeta.setImageCaptureReason(
                    new ImageCaptureReason(ImageCaptureReasonEnum.MANUAL, "Manual order to capture image."));
    
            if (m_Url == null)
            {
                throw new CommandExecutionException("No IP Given");
            }
            final URL imageUrl;
            try
            {
                imageUrl = new URL(m_Url + "image.cgi");
            }
            catch (final MalformedURLException ex)
            {
                Logging.log(LogService.LOG_ERROR, "Malformed URL. Image URL");
                throw new CommandExecutionException(ex);
            }
            final URLConnection con = getUrlConnection(imageUrl);
            final InputStream input = getInputStream(con);
            final ByteArrayOutputStream imageCreation;
            imageCreation = new ByteArrayOutputStream();
            int check;
            do
            {
                try
                {
                    check = input.read();
                }
                catch (final IOException ex) 
                {
                    Logging.log(LogService.LOG_ERROR, "Error receiving data from input stream for image retrieval");
                    throw new CommandExecutionException(ex);
                }
                if (check != -1)
                {
                    imageCreation.write(check);
                }
            } while (check != -1);
            if (imageCreation.size() == 0)
            {
                Logging.log(LogService.LOG_ERROR, "No data retrieved from the camera.");
                throw new CommandExecutionException("Empty image byte array");
            }
            final byte[] imageFinal = imageCreation.toByteArray();
            final DigitalMedia digitalMedia = new DigitalMedia(imageFinal, "image/jpg");
            Logging.log(LogService.LOG_INFO, "Plug-in for the Canon VB-C60 IP camera data captured");
            final Observation obs = new Observation().withDigitalMedia(digitalMedia).withImageMetadata(imageMeta);
            return obs;
        }
    }
    
    @Override
    public Status onPerformBit() throws CommandExecutionException
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            if (m_Url == null)
            {
                Logging.log(LogService.LOG_ERROR, "Missing IP address");
                return new Status().withSummaryStatus(new OperatingStatus(SummaryStatusEnum.BAD, "BIT_Failed"));
            }
            Logging.log(LogService.LOG_INFO, "Performing BIT on Plug-in for the Canon VB-C60 IP camera BIT");
            final URL panTiltUrl;
            try
            {
                panTiltUrl = new URL(m_Url + INFOURL);
            }
            catch (final MalformedURLException ex)
            {
                Logging.log(LogService.LOG_ERROR, ex, "Error creating the URL for perform BIT");
                return new Status().withSummaryStatus(new OperatingStatus(SummaryStatusEnum.BAD, "BIT_FAIL"));
            }
            final URLConnection urlCon = getUrlConnection(panTiltUrl);
            final InputStream infoInput = getInputStream(urlCon);
            final Properties infoProp = new Properties();
            try
            {
                infoProp.load(infoInput);
            }
            catch (final IOException ex)
            {
                Logging.log(LogService.LOG_ERROR, ex, "Error creating the properties file from the input stream");
                return new Status().withSummaryStatus(new OperatingStatus(SummaryStatusEnum.BAD, "BIT FAIL"));
            }
            return new Status().withSummaryStatus(new OperatingStatus(SummaryStatusEnum.GOOD, "BIT Passed"));
        }
    }

    @Override
    public Response onExecuteCommand(final Command command) throws CommandExecutionException, InterruptedException
    {
        if (m_Context.getActiveStatus() != AssetActiveStatus.ACTIVATED)
        {
            Logging.log(LogService.LOG_ERROR, "Asset isn't activated");
            throw new CommandExecutionException("Asset not activated");
        }

        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            if (m_Url == null)
            {
                throw new CommandExecutionException("No Given IP");
            }
            else if (command instanceof SetPanTiltCommand)
            {
                final SetPanTiltCommand setPanTilt = (SetPanTiltCommand)command; 
                if (setPanTilt.getPanTilt().getAzimuth() != null)
                { 
                    handleSetPan(setPanTilt); 
                }
                if (setPanTilt.getPanTilt().getElevation() != null)
                { 
                    handleSetTilt(setPanTilt); 
                }
                return new SetPanTiltResponse();
            }
            else if (command instanceof GetPanTiltCommand)
            {
                final double tiltFinal = handleGetTilt();
                final double panFinal = handleGetPan();
                final ElevationDegrees tiltElevationDegree = new ElevationDegrees();
                tiltElevationDegree.setValue(tiltFinal);
                final AzimuthDegrees panAzimuthDegrees = new AzimuthDegrees();
                panAzimuthDegrees.setValue(panFinal);
                final OrientationOffset orientationOffset = new OrientationOffset();
                orientationOffset.setAzimuth(panAzimuthDegrees);
                orientationOffset.setElevation(tiltElevationDegree);
                final GetPanTiltResponse panTiltResponse = new GetPanTiltResponse();
                panTiltResponse.setPanTilt(orientationOffset);
                return panTiltResponse;
            }
            else if (command instanceof SetCameraSettingsCommand)
            {
                final SetCameraSettingsCommand setCameraSettings = (SetCameraSettingsCommand)command;
                handleSetZoom(setCameraSettings);
                return new SetCameraSettingsResponse();
            }
            else if (command instanceof GetCameraSettingsCommand)
            {
                final float zoomFinal = handleGetZoom();
                final GetCameraSettingsResponse cameraSettingsResponse = new GetCameraSettingsResponse();
                cameraSettingsResponse.setZoom(zoomFinal);
                return cameraSettingsResponse;
            }
            else
            {
                throw new CommandExecutionException("Could not execute specified command.");
            }
        }
    }
    
    @Override
    public Set<Extension<?>> getExtensions()
    {
        // discouraged, but can be used in cases where it is necessary to provide an extended API.
        return new HashSet<Extension<?>>();
    }
    
    /**
     * Method used to send the new pan value to the Camera.
     * @param setPanTilt
     *     Command sent to the asset with the value from the new pan.
     * @throws CommandExecutionException 
     *     an error connecting to the IP camera.
     */
    private void handleSetPan(final SetPanTiltCommand setPanTilt) throws CommandExecutionException
    {
        final int pan = (int)(setPanTilt.getPanTilt().getAzimuth()
                .getValue() * PAN_ADJUST);
        final URL panFinal;
        try
        {
            panFinal = new URL(m_Url + "control?pan=" + pan);
        }
        catch (final MalformedURLException ex)
        {
            Logging.log(LogService.LOG_ERROR, ex, "Malformed URL, Forming Pan Command URL");
            throw new CommandExecutionException("Malformed URL, Forming Pan Command");
        }
        Logging.log(LogService.LOG_DEBUG, "Pan Command Sent: " + panFinal);
        getInputStream(panFinal);
        Logging.log(LogService.LOG_INFO, "Updated pan to: " + pan);
    }
    
    /**
     * Method used to send the new tilt value to the Camera.
     * @param setPanTilt
     *     Command sent to the asset with the value from the new tilt.
     * @throws CommandExecutionException
     *     an error connecting to the IP camera.
     */
    private void handleSetTilt(final SetPanTiltCommand setPanTilt) throws CommandExecutionException
    {
        final int tilt = (int)(setPanTilt.getPanTilt().getElevation()
                .getValue() * TILT_ADJUST); //Used to adjust the tilt value to the correct size
        final URL tiltFinal;
        try
        {
            tiltFinal = new URL(m_Url + "control?tilt=" + tilt);
        }
        catch (final MalformedURLException ex)
        {
            Logging.log(LogService.LOG_ERROR, ex, "Malformed URL, Forming Tilt Command URL");
            throw new CommandExecutionException("Malformed URL, forming Tilt Command");
        }
        Logging.log(LogService.LOG_DEBUG, "Tilt Command Sent: " + tiltFinal);
        getInputStream(tiltFinal);
        Logging.log(LogService.LOG_INFO, "Updated tilt to: " + tilt);
    }
    
    /**
     * Method used to get the tilt value for the sync function.
     * @return double
     *     current tilt value of the camera
     * @throws CommandExecutionException
     *     an error connecting to the IP camera.
     */
    private double handleGetTilt() throws CommandExecutionException
    {
        final URL tiltUrl;
        try
        {
            tiltUrl = new URL(m_Url + INFOURL);
        }
        catch (final MalformedURLException ex)
        {
            Logging.log(LogService.LOG_ERROR, ex, "Error creating the Tilt info URL");
            throw new CommandExecutionException(ex);
        }
        final URLConnection urlCon = getUrlConnection(tiltUrl);
        final InputStream infoInput = getInputStream(urlCon);
        final Properties infoProp = new Properties();
        try
        {
            infoProp.load(infoInput);
        }
        catch (final IOException ex)
        {
            Logging.log(LogService.LOG_ERROR, ex, "Error creating properties file");
            throw new CommandExecutionException(ex);
        }
        final String tiltString = "c.1.tilt";
        final int cutOff = infoProp.getProperty(tiltString).lastIndexOf('=');
        return  (Double.parseDouble(infoProp.getProperty(tiltString)
                .substring(cutOff + 1))) / TILT_ADJUST; //Used to adjust the tilt value to the correct number
    }
    
    /**
     * Method used to get the pan value for the sync function.
     * @return double
     *     the current pan value of the camera
     * @throws CommandExecutionException
     *     an error connecting to the IP camera.
     */
    private double handleGetPan() throws CommandExecutionException
    {
        final URL panUrl;
        try
        {
            panUrl = new URL(m_Url + INFOURL);
        }
        catch (final MalformedURLException ex)
        {
            Logging.log(LogService.LOG_ERROR, ex, "Error creating the Pan info URL");
            throw new CommandExecutionException(ex);
        }
        final URLConnection urlCon = getUrlConnection(panUrl);
        final InputStream infoInput = getInputStream(urlCon);
        final Properties infoProp = new Properties();
        try
        {
            infoProp.load(infoInput);
        }
        catch (final IOException ex)
        {
            Logging.log(LogService.LOG_ERROR, ex, "Error writing the properties file");
            throw new CommandExecutionException(ex);
        }
        final String panString = "c.1.pan";
        final int cutOff = infoProp.getProperty(panString).lastIndexOf('=');
        return Double.parseDouble(infoProp.getProperty(panString)
                .substring(cutOff + 1)) / PAN_ADJUST;
    }
    
    /**
     * Method to handle sending the new zoom to the camera.
     * @param setCameraSettings
     *     SetCameraSettingsCommand, used to get the values for the new zoom
     * @throws CommandExecutionException
     *     in case there is an error connecting to the IP camera.
     */
    private void handleSetZoom(final SetCameraSettingsCommand setCameraSettings) throws CommandExecutionException
    {
        final int zoomTemp = (int)(setCameraSettings.getZoom() * ZOOM_ADJUST);
        final int zoom = ZOOM_ADJUST - zoomTemp;
        final URL commandFinal;
        try
        {
            commandFinal = new URL(m_Url + "control?zoom=" + zoom);
        }
        catch (final MalformedURLException ex)
        {
            Logging.log(LogService.LOG_ERROR, ex, "Malformed URL, Forming Zoom Command URL");
            throw new CommandExecutionException("Malformed URL, Forming Zoom Command");
        }
        Logging.log(LogService.LOG_DEBUG, "Zoom Sent Command: " + commandFinal);
        getInputStream(commandFinal);
        Logging.log(LogService.LOG_INFO, "Updated zoom to: " + setCameraSettings.getZoom()); 
    }

    /**
     * Method used to get the zoom for the sync command.
     * @return float
     *     current zoom value of the camera
     * @throws CommandExecutionException
     *     connecting to the IP camera.
     */
    private float handleGetZoom() throws CommandExecutionException
    {
        final URL zoomUrl;
        try
        {
            zoomUrl = new URL(m_Url + INFOURL);
        }
        catch (final MalformedURLException ex)
        {
            Logging.log(LogService.LOG_ERROR, ex, "Error creating the Zoom info URL");
            throw new CommandExecutionException(ex);
        }
        final URLConnection urlCon = getUrlConnection(zoomUrl);
        final InputStream infoInput = getInputStream(urlCon);
        final Properties infoProp = new Properties();
        try
        {
            infoProp.load(infoInput);
        }
        catch (final IOException ex)
        {
            Logging.log(LogService.LOG_ERROR, ex, "Error creating the properties file");
            throw new CommandExecutionException(ex);
        }
        final String zoomString = "c.1.zoom";
        final int cutOff = infoProp.getProperty(zoomString).indexOf('=');
        return (ZOOM_ADJUST - Float.parseFloat(infoProp.getProperty(zoomString)
                .substring(cutOff + 1))) / ZOOM_ADJUST; 
    }
    
    /**
     * Method used to get the zoom for the sync command.
     * @param url
     *     A URL to create a connection to
     * @return URLConnection
     *     A connection to the given URL
     * @throws CommandExecutionException
     *     if it fails to connect to the camera.
     */
    private URLConnection getUrlConnection(final URL url) throws CommandExecutionException
    {
        final URLConnection urlCon;
        try
        {
            urlCon = m_UrlUtil.getConnection(url);
        }
        catch (final IOException ex)
        {
            Logging.log(LogService.LOG_ERROR, ex, "Error connecting to the camera");
            throw new CommandExecutionException(ex);
        }
        return urlCon;
    }
    
    /**
     * Method used to get the zoom for the sync command.
     * @param urlCon
     *     A URLConnection to the camera
     * @return InputStream
     *     An input stream from the camera
     * @throws CommandExecutionException
     *     if it fails to connect to the camera.
     */
    private InputStream getInputStream(final URLConnection urlCon) throws CommandExecutionException
    {
        final InputStream input;
        try
        {
            input = m_UrlUtil.getInputStream(urlCon);
        }
        catch (final IOException ex)
        {
            Logging.log(LogService.LOG_ERROR, ex, "Error creating the input stream");
            throw new CommandExecutionException(ex); 
        }
        return input;
    }
    
    /**
     * Method used to get the zoom for the sync command.
     * @param url
     *     A URL to get an input stream of data from.
     * @throws CommandExecutionException
     *     if it fails to connect to the camera.
     */
    private void getInputStream(final URL url) throws CommandExecutionException
    {
        try
        {
            m_UrlUtil.getInputStream(url);
        }
        catch (final IOException ex)
        {
            Logging.log(LogService.LOG_ERROR, ex, "Failure to Send Command.");
            throw new CommandExecutionException(ex);
        }
    }
}
