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
package edu.udayton.udri.asset.hikvision;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import mil.dod.th.core.asset.Asset;
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
import mil.dod.th.core.types.DigitalMedia;
import mil.dod.th.core.types.SensingModality;
import mil.dod.th.core.types.SensingModalityEnum;
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
import org.osgi.service.log.LogService;

/**
 * Hikvision IP camera asset implementation.
 * 
 * @author Noah
 */
@Component(factory = Asset.FACTORY) // NOCHECKSTYLE - The Fan-Out complexity is 44 and allowed 30
public class HikVisionAsset implements AssetProxy
{
    /**
     * Reference to the context which provides this class with methods to interact with the rest of the system.
     */
    private static final String PUT = "PUT";
    private static final String GET = "GET";
    private static final String DATA = "<PTZData version='2.0' xmlns='http://www.isapi.org/ver20/XMLSchema'>"
            + " <AbsoluteHigh><elevation> %s</elevation><azimuth>%s0</azimuth>"
            + "<absoluteZoom> %s</absoluteZoom></AbsoluteHigh> </PTZData>";
    private static final String URL_IMAGE_CAPTURE = "http://%s/ISAPI/Streaming/channels/1/picture";
    private static final String CODE_RESPONSE = "The Code response is : ";
    private static final String URL_CAMERA_ABSOLUTE = "http://%s/ISAPI/PTZCtrl/channels/1/absolute";
    private static final String BIT_FAIL = "BIT failed";
    private static final String URL_CAMERA_STATUS = "http://%s/ISAPI/PTZCtrl/channels/1/status";
    private static final String UTF = "UTF-8";
    private static final String ELEVATION_AND_AZIMUTH = "Elevation is %s Azimuth is %s";
    
    //These are just constants for the formulas
    private static final int CONVERSION_FROM_CAMERA_TO_GUI_AZIMUTH = 10;
    private static final int MAX_AZIMUTH_NUMBER = 180;
    private static final int DEGREES_360 = 360;
    private static final int CONVERSION_FROM_CAMERA_TO_GUI_ZOOM = 1000;

    private UrlUtil m_UrlUtil;
    private AssetContext m_Context;
    private CommandProcessor m_CommandProcessor;
    private String m_IpAddress;
    private String m_UserName;
    private String m_Password;

    @Reference
    public void setUrlUtil(final UrlUtil urlUtil)
    {
        m_UrlUtil = urlUtil; 
    }
    
    @Reference
    public void setCommandProcessor(final CommandProcessor commandProcessor)
    {
        m_CommandProcessor = commandProcessor;
    }
    
    @Override
    public void initialize(final AssetContext context, final Map<String, Object> props) throws FactoryException
    {
        m_Context = context;
        final HikVisionAssetAttributes config = Configurable.createConfigurable(HikVisionAssetAttributes.class, props);

        // Properties that have been defined in HikVisionAssetAttributes will be accessible through the `config` object
        m_UserName = config.userName();
        m_Password = config.password();
        m_IpAddress = config.ip();        
        authenticate(m_UserName, m_Password);
    }

    @Override
    public void updated(final Map<String, Object> props)
    {
        final HikVisionAssetAttributes config = Configurable.createConfigurable(HikVisionAssetAttributes.class, props);

        // Properties that have been defined in HikVisionAssetAttributes will be accessible through the `config` object
        m_UserName = config.userName();
        m_Password = config.password();
        m_IpAddress = config.ip();
        authenticate(m_UserName, m_Password);
        
        Logging.log(LogService.LOG_DEBUG, "User Name is " + m_UserName + " Password is " + m_Password);
    }

    @Override
    public void onActivate() throws AssetException
    {
        final Status status = new Status().withSummaryStatus(
                new OperatingStatus(SummaryStatusEnum.GOOD, "Activated"));
        try
        { 
            m_Context.setStatus(status); 
        }
        catch (final ValidationFailedException ex)
        { 
            Logging.log(LogService.LOG_ERROR, ex.getMessage());
        }
    }

    @Override
    public void onDeactivate() throws AssetException
    {
        final Status status = new Status().withSummaryStatus(
                new OperatingStatus(SummaryStatusEnum.OFF, "Deactivated"));
        try
        { 
            m_Context.setStatus(status); 
        }
        catch (final ValidationFailedException ex)
        { 
            Logging.log(LogService.LOG_ERROR, ex.getMessage());
        }
    } 

    @Override
    public Observation onCaptureData()
    {
        Logging.log(LogService.LOG_DEBUG, "Hikvision IP camera asset data captured");
        
        byte[] imageData = null;
        try
        { 
            imageData = writeImage();
        }
        catch (final AssetException ex)
        {
            Logging.log(LogService.LOG_ERROR, ex.getMessage());
        }
        final ImageMetadata imageMeta = new ImageMetadata();
        imageMeta.setColor(true);
        imageMeta.setImager(new Camera(0, "Example Camera", CameraTypeEnum.VISIBLE));
        imageMeta.setPictureType(PictureTypeEnum.FULL_FIELD_OF_VIEW);
        final int pixWidth = 752;
        final int pixHeight = 480;
        imageMeta.setResolution(new PixelResolution(pixWidth, pixHeight));
        imageMeta.setImageCaptureReason(
                new ImageCaptureReason(ImageCaptureReasonEnum.MANUAL, "Manual order to capture image."));   

        final DigitalMedia digitalMedia = new DigitalMedia(imageData, "image/jpg");

        final Observation obs = new Observation().withDigitalMedia(digitalMedia).withImageMetadata(
                imageMeta).withModalities(new SensingModality().withValue(SensingModalityEnum.IMAGER));
        return obs;
    }

    @Override
    public Status onPerformBit()
    {
        Logging.log(LogService.LOG_DEBUG, "Performing BIT test");
        final Status status = new Status().withSummaryStatus(new OperatingStatus(SummaryStatusEnum.BAD, BIT_FAIL));
        URL url = null;
        try
        {
            url = new URL(String.format(URL_IMAGE_CAPTURE, m_IpAddress));
        }
        catch (final MalformedURLException ex) 
        {
            Logging.log(LogService.LOG_ERROR, ex.getMessage());
            return new Status().withSummaryStatus(new OperatingStatus(SummaryStatusEnum.BAD, BIT_FAIL));
        }    
        HttpURLConnection urlConnection = null;
        try
        {
            urlConnection = m_UrlUtil.getConnection(url);
        }
        catch (final IOException ex)
        {  
            Logging.log(LogService.LOG_ERROR, ex.getMessage());
            return new Status().withSummaryStatus(new OperatingStatus(SummaryStatusEnum.BAD, BIT_FAIL));           
        }
        try 
        {
            urlConnection.setRequestMethod(GET);        
            urlConnection.setDoOutput(true);  
        }
        catch (final ProtocolException ex)
        {
            Logging.log(LogService.LOG_ERROR, ex.getMessage());
            return new Status().withSummaryStatus(new OperatingStatus(
                    SummaryStatusEnum.BAD, BIT_FAIL));                       
        } 
        final BufferedReader inStream;
        try
        {
            inStream = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), UTF));
        }
        catch (final IOException ex) 
        {        
            Logging.log(LogService.LOG_ERROR, ex.getMessage());
            return new Status().withSummaryStatus(new OperatingStatus(SummaryStatusEnum.BAD, BIT_FAIL));
        } 
        String line = null;   
        try
        {
            line = inStream.readLine(); 
            inStream.close();
        }
        catch (final IOException ex)
        {
            Logging.log(LogService.LOG_ERROR, ex.getMessage());
            return new Status().withSummaryStatus(new OperatingStatus(SummaryStatusEnum.BAD, BIT_FAIL));
        }
        if (line != null)
        {
            return new Status().withSummaryStatus(new OperatingStatus(SummaryStatusEnum.GOOD, "BIT Passed"));           
        }
        else
        {
            return status; 
        }
    }
 
    @Override
    public Response onExecuteCommand(final Command command) throws CommandExecutionException, InterruptedException
    {
        Logging.log(LogService.LOG_DEBUG, "The command is : " + command);
        if (command instanceof SetPanTiltCommand)
        {
            int elevation = 0;
            int azimuth = 0;
            final SetPanTiltCommand setPanTilt = (SetPanTiltCommand)command;

            try
            {
                final HttpURLConnection urlConnection = getAbsoluteConnection();
                
                if (m_CommandProcessor.isAzimuthSet(setPanTilt))
                {
                    azimuth = m_CommandProcessor.getAzimuth(setPanTilt);
                }
                else
                {
                    azimuth = getCurrentAzimuthConvertion();                         
                }                
                if (m_CommandProcessor.isElevationSet(setPanTilt))
                {
                    elevation = m_CommandProcessor.getElevation(setPanTilt); 
                } 
                else
                {
                    elevation = getCurrentElevation();
                }            
       
                final DataOutputStream outputStream = new DataOutputStream(urlConnection.getOutputStream());

                // sends the new values to the asset
                outputStream.writeBytes(String.format(DATA, elevation, azimuth, getCurrentZoom()));
                outputStream.close();

                // the connection code given back from the asset
                Logging.log(LogService.LOG_DEBUG, CODE_RESPONSE + urlConnection.getResponseCode());
            }
            catch (final IOException ex)
            {
                Logging.log(LogService.LOG_ERROR, ex.getMessage());
            }
            return new SetPanTiltResponse();
        }
        if (command instanceof SetCameraSettingsCommand)
        {
            final SetCameraSettingsCommand setCameraSettings = (SetCameraSettingsCommand)command;
            final int zoom;

            try
            {
                final HttpURLConnection urlConnection = getAbsoluteConnection();
            
                zoom = m_CommandProcessor.getZoom(setCameraSettings);           

                Logging.log(LogService.LOG_DEBUG, String.format(DATA, 
                        getCurrentElevation(), getCurrentAzimuth(), zoom));

                final DataOutputStream outputStream = new DataOutputStream(urlConnection.getOutputStream());

                outputStream.writeBytes(String.format(DATA, getCurrentElevation(
                            ), getCurrentAzimuthConvertion(), zoom));
                
                outputStream.close();

                // the connection code given back from the asset
                Logging.log(LogService.LOG_DEBUG, CODE_RESPONSE + urlConnection.getResponseCode());
            }
            catch (final IOException ex)
            {
                Logging.log(LogService.LOG_ERROR, ex.getMessage());
            } 
            return new SetCameraSettingsResponse();
        }        
        if (command instanceof GetPanTiltCommand)
        {
            final GetPanTiltResponse getPanTilt = new GetPanTiltResponse();

            try
            {
                Logging.log(LogService.LOG_DEBUG, String.format(ELEVATION_AND_AZIMUTH,
                        getCurrentElevation(), getCurrentAzimuth()));

                final OrientationOffset orientationOffset = setCurrentAzimuthAndElevation();

                getPanTilt.setPanTilt(orientationOffset);
 
                return getPanTilt;
            } 
            catch (final IOException ex)
            {
                Logging.log(LogService.LOG_ERROR, ex.getMessage());
            }
        }        
        if (command instanceof GetCameraSettingsCommand)
        {
            final GetCameraSettingsResponse getCameraSettings = new GetCameraSettingsResponse();
            final int zoom = 0;
            try
            {
                getCameraSettings.setZoom((float)getCurrentZoom() / CONVERSION_FROM_CAMERA_TO_GUI_ZOOM);
            }
            catch (final IOException ex) 
            {
                Logging.log(LogService.LOG_ERROR, ex.getMessage());
            } 
            Logging.log(LogService.LOG_DEBUG, " zoom as a float " 
                + (float)zoom / CONVERSION_FROM_CAMERA_TO_GUI_ZOOM);
            return getCameraSettings;
        }
        else
        {
            throw new CommandExecutionException("Could not execute specified command.");
        }
    }

    @Override
    public Set<Extension<?>> getExtensions()
    {
        return new HashSet<Extension<?>>();
    }
    
    /**
     * Sets the login user and password so you can access online.
     * @param userName
     *     Name of user
     * @param password
     *     Password
     */
    private void authenticate(final String userName, final String password)
    {
        Authenticator.setDefault(new Authenticator()
        {
            @Override
            protected PasswordAuthentication getPasswordAuthentication()
            {
                return new PasswordAuthentication(userName, password.toCharArray());
            }
        });
    }
    
    /**
     * Gets the Connection needed from the asset.
     * @return
     *     HttpURLConnection
     * @throws IOException
     *     In case Wrong URL/Null
     */
    private HttpURLConnection getAbsoluteConnection() throws IOException
    {
        final URL url = new URL(String.format(URL_CAMERA_ABSOLUTE, m_IpAddress));
        final HttpURLConnection urlConnection = m_UrlUtil.getConnection(url);        
        urlConnection.setRequestMethod(PUT);
        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);        
        return urlConnection;
    }

    /**
     * Finds the value within the XML.
     * @param checkLine
     *     The String you want to start parsing after
     * @param totalLine
     *     The Full line you get back from the xml
     * @return
     *     The int value from the xml
     */
    private int parsingValueFromXml(final String checkLine, final String totalLine)
    {
        int result = 0;
        final int expectedLength = 6;

        final String innerLine = totalLine.substring(checkLine.length(), checkLine.length() + expectedLength);
        int count = 0;
        for (int i = 0; i < expectedLength; i++) 
        {
            final char a = innerLine.charAt(i);
            if (a == '<')
            {
                count = i;
                break;
            }
        }
        final String finalLine = innerLine.substring(0, count);
        result = Integer.parseInt(finalLine);

        return result;
    }

    /**
     * Connects to the asset to get the status.
     * @return
     *     The XML from the asset
     * @throws UnsupportedEncodingException
     *     Wrong Encoding from asset
     * @throws IOException
     *     In case of NULL
     */
    private BufferedReader getStatus() throws UnsupportedEncodingException, IOException
    {
        final BufferedReader inStream;        
        final URL url = new URL(String.format(URL_CAMERA_STATUS, m_IpAddress));
        final HttpURLConnection urlConnection = m_UrlUtil.getConnection(url);
        urlConnection.setRequestMethod(GET);
        urlConnection.setDoOutput(true);
        inStream = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), UTF));        
        return inStream;  
    }

    /**
     * Processing the XML to get the Elevation.
     * @return
     *     The Value of Elevation from the XML
     * @throws IOException
     *     In case of NULL
     */
    private int getCurrentElevation() throws IOException
    {   
        int elevation = 0;    
        final BufferedReader inStream = getStatus();
    
        String inLine;
        while ((inLine = inStream.readLine()) != null)
        {
            if (inLine.contains("<elev"))
            {
                elevation = parsingValueFromXml("<elevation>", inLine);
            }    
        }
        return elevation;
    }
    
    /**
     * Converts the input from the GUI and Asset.
     * @return
     *     The int value
     * @throws IOException
     *     In case of null
     */
    private int getCurrentAzimuthConvertion() throws IOException
    {
        int azimuth = 0;
        if (getCurrentAzimuth() < 0)
        {
            azimuth = getCurrentAzimuth() + DEGREES_360;
        }
        else
        {
            azimuth = getCurrentAzimuth();
        }
        return azimuth;
    }

    /**
     * Processing the XML to get the Azimuth.
     * @return
     *     The Value of Azimuth from the XML
     * @throws IOException
     *     In case of NULL
     */
    private int getCurrentAzimuth() throws IOException
    {
        int azimuth = 0;        
        final BufferedReader inStream = getStatus();
        
        String inLine;
        while ((inLine = inStream.readLine()) != null)
        {        
            if (inLine.contains("<azi"))
            {
                azimuth = parsingValueFromXml("<azimuth>", inLine);
                azimuth = azimuth / CONVERSION_FROM_CAMERA_TO_GUI_AZIMUTH;
                if (azimuth > MAX_AZIMUTH_NUMBER)
                {
                    azimuth -= DEGREES_360;  
                }
            }
        }
        return azimuth;
    }
  
    /**
     * Processing the XML to get the Zoom.
     * @return
     *     The Value of Zoom from the XML
     * @throws IOException
     *     In case of NULL
     */
    private int getCurrentZoom() throws IOException
    {
        int zoom = 0;
        final BufferedReader inStream = getStatus();
        
        String inLine;
        while ((inLine = inStream.readLine()) != null)
        {        
            if (inLine.contains("<abso"))
            {
                zoom = parsingValueFromXml("<absoluteZoom>", inLine);                
            }
        }
        return zoom;
    }
 
    /**
     * Process the Current Azimuth and Elevation from the asset.
     * @return
     *     The current Orientation Offset from the camera
     * @throws IOException
     *     In case of NULL
     */
    private OrientationOffset setCurrentAzimuthAndElevation() throws IOException
    {
        final OrientationOffset orientationOffset = new OrientationOffset();
        
        orientationOffset.setAzimuth(new AzimuthDegrees().withValue(getCurrentAzimuth()));        
        orientationOffset.setElevation(new ElevationDegrees().withValue(
                (double)(getCurrentElevation())
                / 10)); // NOCHECKSTYLE - converting the XML to match the correct output;        
        return orientationOffset;
    }
    
    /**
     * Gets the Image from the camera in the form of a byte array.
     * @return
     *     The byte array from the asset
     * @throws AssetException
     *     If the data received from the asset is valid or not
     */
    private byte[] writeImage() throws AssetException 
    {
        URL url = null;
        HttpURLConnection urlConnection = null;
        try
        {
            url = new URL(String.format(URL_IMAGE_CAPTURE, m_IpAddress));
            Logging.log(LogService.LOG_DEBUG, String.format(URL_IMAGE_CAPTURE, m_IpAddress));
        }
        catch (final MalformedURLException ex) 
        {
            Logging.log(LogService.LOG_ERROR, ex.getMessage());
            return new byte[]{0};
        }
        try
        {
            urlConnection = m_UrlUtil.getConnection(url);
        }
        catch (final IOException ex)
        {
            Logging.log(LogService.LOG_ERROR, ex.getMessage());
            return new byte[]{0};
        }        
        try (InputStream inStream = urlConnection.getInputStream())
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final int size = 1024; 
            final byte[] buffer = new byte[size];
            
            for (int readNum; (readNum = inStream.read(buffer)) != -1;) //NOCHECKSTYLE - avoid inner assignments
            {
                baos.write(buffer, 0, readNum);
            }
            
            final byte[] imageData = baos.toByteArray(); 
             
            baos.flush();
            baos.close();
            return imageData;            
        }
        catch (final IOException ex)
        {
            throw new AssetException(ex);
        }        
    }
}