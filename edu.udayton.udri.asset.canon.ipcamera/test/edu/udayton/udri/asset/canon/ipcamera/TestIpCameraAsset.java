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

import static org.hamcrest.MatcherAssert.assertThat; //NOCHECKSTYLE, Static Member Import, Import needed for testing
import static org.hamcrest.CoreMatchers.equalTo; //NOCHECKSTYLE, Static Member Import, Import needed for testing
import static org.hamcrest.CoreMatchers.instanceOf; //NOCHECKSTYLE, Static Member Import, Import needed for testing
import static org.mockito.Mockito.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import mil.dod.th.core.asset.Asset.AssetActiveStatus;
import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.AssetException;
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
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.types.spatial.AzimuthDegrees;
import mil.dod.th.core.types.spatial.ElevationDegrees;
import mil.dod.th.core.types.spatial.OrientationOffset;
import mil.dod.th.core.types.status.OperatingStatus;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.core.validator.ValidationFailedException;

/**
 * @author Timothy
 *
 */
public class TestIpCameraAsset //NOCHECKSTYLE, Data Abstraction Coupling, Large amount of referenced values.
{
    private IpCameraAsset m_SUT;
    @Mock private AssetContext m_Context;
    @Mock private Map<String, Object> m_Props;
    @Mock private UrlUtils m_UrlUtil;
    
    @Before
    public void setup() throws FactoryException
    {
        MockitoAnnotations.initMocks(this);
        m_SUT = new IpCameraAsset();
        m_Props = new HashMap<String, Object>();
        m_Props.put(IpCameraAssetAttributes.IP, "192.168.1.55"); //NOPMD Hard Coded IP address, testing
        m_SUT.setUrlUtil(m_UrlUtil);
        m_SUT.initialize(m_Context, m_Props);
    }
    
    @Test
    public void testOnActivate() throws AssetException, ValidationFailedException
    {
        m_SUT.onActivate();
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(m_Context).setStatus(statusCaptor.capture());
        Status status = statusCaptor.getValue();
        OperatingStatus opStatus = status.getSummaryStatus();
        assertThat(opStatus.getSummary(), equalTo(SummaryStatusEnum.GOOD));
        assertThat(opStatus.getDescription(), equalTo("Asset Activated"));
    }
    
    @Test
    public void testOnDeactivate() throws AssetException, ValidationFailedException
    {
        m_SUT.onDeactivate();
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(m_Context).setStatus(statusCaptor.capture());
        Status status = statusCaptor.getValue();
        OperatingStatus opStatus = status.getSummaryStatus();
        assertThat(opStatus.getSummary(), equalTo(SummaryStatusEnum.OFF));
        assertThat(opStatus.getDescription(), equalTo("Asset Deactivated"));
    }

    @Test
    public void testUpdated() throws AssetException, IOException
    {
        m_SUT.onActivate();
        Map<String, Object> props = new HashMap<>();
        props.put(IpCameraAssetAttributes.IP, "https://192.186.1.55/-wvhttp-01-/");
        URLConnection urlCon = mock(URLConnection.class);
        InputStream input = mock(InputStream.class);
        when(m_UrlUtil.getConnection(any(URL.class))).thenReturn(urlCon);
        when(m_UrlUtil.getInputStream(any(URLConnection.class))).thenReturn(input);
        m_SUT.updated(props);
    }
    
    //Catches the IO exception when the Update method creates a URLconnection to the URL.
    @Test
    public void testUpdatedIoException1() throws AssetException, IOException
    {
        m_SUT.onActivate();
        Map<String, Object> props = new HashMap<>();
        props.put(IpCameraAssetAttributes.IP, "https://192.186.1.55/-wvhttp-01-/");
        when(m_UrlUtil.getConnection(any(URL.class))).thenThrow(new IOException());
        m_SUT.updated(props);
    }
    
    //Catches the IO exception when the Update method creates an Input Stream to the URL.
    @Test
    public void testUpdatedIoException2() throws AssetException, IOException
    {
        m_SUT.onActivate();
        Map<String, Object> props = new HashMap<>();
        props.put(IpCameraAssetAttributes.IP, "https://192.186.1.55/-wvhttp-01-/");
        URLConnection urlCon = mock(URLConnection.class);
        when(m_UrlUtil.getConnection(any(URL.class))).thenReturn(urlCon);
        when(m_UrlUtil.getInputStream(any(URLConnection.class))).thenThrow(new IOException());
        m_SUT.updated(props);
    }
    
    @Test
    public void testPerformBit() throws AssetException, IOException
    {
        m_SUT.onActivate();
        URLConnection urlCon = mock(URLConnection.class);
        InputStream input = mock(InputStream.class);
        when(m_UrlUtil.getConnection(any(URL.class))).thenReturn(urlCon).thenReturn(urlCon);
        when(m_UrlUtil.getInputStream(any(URLConnection.class))).thenReturn(input);
        Status status =  m_SUT.onPerformBit();
        assertThat(status.getSummaryStatus().getSummary(), equalTo(SummaryStatusEnum.GOOD));
        assertThat(status.getSummaryStatus().getDescription(), equalTo("BIT Passed"));
    }
    
    //Catches the IO exception when BIT tries to create a URL connection
    @Test(expected = CommandExecutionException.class)
    public void testPerformBitIoException1() throws IOException, AssetException
    {
        m_SUT.onActivate();
        when(m_UrlUtil.getConnection(any(URL.class))).thenThrow(new IOException());
        m_SUT.onPerformBit();
    }
    
    //Catches the IO exception when BIT tries to create an Input Stream from the camera.
    @Test(expected = CommandExecutionException.class)
    public void testPerformBitIoException2() throws IOException, AssetException
    {
        m_SUT.onActivate();
        URLConnection urlCon = mock(URLConnection.class);
        when(m_UrlUtil.getConnection(any(URL.class))).thenReturn(urlCon);
        when(m_UrlUtil.getInputStream(any(URLConnection.class))).thenThrow(new IOException());
        m_SUT.onPerformBit();
    }
    
    @Test(expected = CommandExecutionException.class)
    public void testNoUrlOnCaptureData() throws CommandExecutionException, InterruptedException
    {
        when(m_Context.getActiveStatus()).thenReturn(AssetActiveStatus.ACTIVATED);
        m_SUT.onCaptureData();
    }
    
    @Test
    public void testNoUrlOnBit() throws CommandExecutionException, InterruptedException
    {
        Status status = m_SUT.onPerformBit();
        assertThat(status.getSummaryStatus().getSummary(), equalTo(SummaryStatusEnum.BAD));
        assertThat(status.getSummaryStatus().getDescription(), equalTo("BIT_Failed"));
    }
    
    @Test(expected = CommandExecutionException.class)
    public void testNoUrlOnExecuteCommand() throws CommandExecutionException, InterruptedException
    {
        SetPanTiltCommand command = new SetPanTiltCommand();
        m_SUT.onExecuteCommand(command); 
    }
    
    @Test
    public void testCaptureData() throws IOException, InterruptedException, AssetException
    {
        m_SUT.onActivate();
        final byte[] fakeImg = new byte[]{1, 42, 7, 56, 20};
        InputStream input = new InputStream()
        { 
            private int m_Counter;
            @Override
            public  int read() throws IOException
            {
                int returnVal = -1;
                if (m_Counter < fakeImg.length)
                {
                    returnVal = fakeImg[m_Counter];
                    m_Counter++;
                }
                return returnVal;
            }

        }; 
        URLConnection urlCon = mock(URLConnection.class);
        when(m_UrlUtil.getConnection(any(URL.class))).thenReturn(urlCon);
        when(m_UrlUtil.getInputStream(any(URLConnection.class))).thenReturn(input);
        when(m_Context.getActiveStatus()).thenReturn(AssetActiveStatus.ACTIVATED);
        ArgumentCaptor<URL> urlCaptor = ArgumentCaptor.forClass(URL.class);
        Observation obvervation = m_SUT.onCaptureData();
        verify(m_UrlUtil).getConnection(urlCaptor.capture());
        URL infoUrl = urlCaptor.getValue();
        final byte[] finalImg = new byte[]{1, 42, 7, 56, 20};
        assertThat(infoUrl.toString(), equalTo("http://192.168.1.55/-wvhttp-01-/image.cgi"));
        assertThat(obvervation.getDigitalMedia().getValue(), equalTo(finalImg));
    }
    
    //Catches IO exception when Capture data tries to make a connection to the URL
    @Test(expected = CommandExecutionException.class)
    public void testCaptureDataIoException1() throws IOException, InterruptedException, AssetException
    {
        m_SUT.onActivate();
        when(m_Context.getActiveStatus()).thenReturn(AssetActiveStatus.ACTIVATED);
        when(m_UrlUtil.getConnection(any(URL.class))).thenThrow(new IOException());
        m_SUT.onCaptureData();
    }
    
    //Catches IO exception when Capture data tries to make an Input Stream from the connection
    @Test(expected = CommandExecutionException.class)
    public void testCaptureDataIoException2() throws IOException, InterruptedException, AssetException
    {
        m_SUT.onActivate();
        URLConnection connection = mock(URLConnection.class);
        when(m_Context.getActiveStatus()).thenReturn(AssetActiveStatus.ACTIVATED);
        when(m_UrlUtil.getConnection(any(URL.class))).thenReturn(connection);
        when(m_UrlUtil.getInputStream(any(URLConnection.class))).thenThrow(new IOException());
        m_SUT.onCaptureData();
    }
    
    @Test(expected = CommandExecutionException.class)
    public void testCaptureDataNotActive() throws InterruptedException, AssetException
    {
        m_SUT.onActivate();
        when(m_Context.getActiveStatus()).thenReturn(AssetActiveStatus.DEACTIVATED);
        m_SUT.onCaptureData();
    }
    
    @Test
    public void testSetTilt() throws IOException, InterruptedException, AssetException
    {
        InputStream tiltInput = mock(InputStream.class);
        m_SUT.onActivate();
        ArgumentCaptor<URL> urlCaptor = ArgumentCaptor.forClass(URL.class);
        SetPanTiltCommand setTilt = new SetPanTiltCommand();
        final ElevationDegrees tiltElevationDegrees = new ElevationDegrees();
        tiltElevationDegrees.setValue(17);
        final OrientationOffset orientationOffset = new OrientationOffset();
        orientationOffset.setElevation(tiltElevationDegrees);
        orientationOffset.setAzimuth(null);
        setTilt.setPanTilt(orientationOffset);
        when(m_UrlUtil.getInputStream(any(URL.class))).thenReturn(tiltInput);     
        when(m_Context.getActiveStatus()).thenReturn(AssetActiveStatus.ACTIVATED);
        Response tiltResponse = m_SUT.onExecuteCommand(setTilt);
        verify(m_UrlUtil).getInputStream(urlCaptor.capture());
        URL tiltUrl = urlCaptor.getValue();
        assertThat(tiltUrl.toString(), equalTo("http://192.168.1.55/-wvhttp-01-/control?tilt=1700"));
        assertThat(tiltResponse, instanceOf(SetPanTiltResponse.class));
    }  
    
    @Test
    public void testSetPan() throws IOException, InterruptedException, AssetException
    {
        InputStream panInput = mock(InputStream.class);
        m_SUT.onActivate();
        ArgumentCaptor<URL> urlCaptor = ArgumentCaptor.forClass(URL.class);
        SetPanTiltCommand setPan = new SetPanTiltCommand();
        final AzimuthDegrees panAzimuthDegree = new AzimuthDegrees();
        panAzimuthDegree.setValue(7);
        final OrientationOffset panOrientationOffset = new OrientationOffset();
        panOrientationOffset.setElevation(null);
        panOrientationOffset.setAzimuth(panAzimuthDegree);
        setPan.setPanTilt(panOrientationOffset);
        when(m_UrlUtil.getInputStream(any(URL.class))).thenReturn(panInput);
        when(m_Context.getActiveStatus()).thenReturn(AssetActiveStatus.ACTIVATED);
        Response panResponse = m_SUT.onExecuteCommand(setPan);
        verify(m_UrlUtil).getInputStream(urlCaptor.capture());
        URL panURL = urlCaptor.getValue();
        assertThat(panURL.toString(), equalTo("http://192.168.1.55/-wvhttp-01-/control?pan=660"));
        assertThat(panResponse, instanceOf(SetPanTiltResponse.class));
    }
    
    //Catches IO exception when handleSetPan method tries to create connection to the URL
    @Test(expected = CommandExecutionException.class)
    public void testSetPanIoException1() throws IOException, InterruptedException, AssetException
    {
        m_SUT.onActivate();
        SetPanTiltCommand setPanTiltCommand = new SetPanTiltCommand();
        final AzimuthDegrees panAzimuthDegrees = new AzimuthDegrees();
        panAzimuthDegrees.setValue(7);
        final OrientationOffset panOrientationOffset = new OrientationOffset();
        panOrientationOffset.setElevation(null);
        panOrientationOffset.setAzimuth(panAzimuthDegrees);
        setPanTiltCommand.setPanTilt(panOrientationOffset);
        when(m_Context.getActiveStatus()).thenReturn(AssetActiveStatus.ACTIVATED);
        when(m_UrlUtil.getInputStream(any(URL.class))).thenThrow(new IOException());
        m_SUT.onExecuteCommand(setPanTiltCommand);
    }
    
    //Catches IO exception when handleSetTilt method tries to create connection to the URL
    @Test(expected = CommandExecutionException.class)
    public void testSetTiltIoException2() throws IOException, InterruptedException, AssetException
    {
        m_SUT.onActivate();
        SetPanTiltCommand setPanTiltCommand = new SetPanTiltCommand();
        final ElevationDegrees tiltElevationDegrees = new ElevationDegrees();
        tiltElevationDegrees.setValue(17);
        final OrientationOffset orientationOffset = new OrientationOffset();
        orientationOffset.setElevation(tiltElevationDegrees);
        orientationOffset.setAzimuth(null);
        setPanTiltCommand.setPanTilt(orientationOffset);
        when(m_Context.getActiveStatus()).thenReturn(AssetActiveStatus.ACTIVATED);
        when(m_UrlUtil.getInputStream(any(URL.class))).thenThrow(new IOException());
        m_SUT.onExecuteCommand(setPanTiltCommand);
    }
    
    @Test(expected = CommandExecutionException.class)
    public void testOnExecutionNotActivated() throws IOException, InterruptedException, AssetException
    {
        m_SUT.onActivate();
        SetPanTiltCommand setPanTiltCommand = new SetPanTiltCommand();
        when(m_Context.getActiveStatus()).thenReturn(AssetActiveStatus.DEACTIVATED);
        m_SUT.onExecuteCommand(setPanTiltCommand);
    }
    
    @Test
    public void testSetCameraSettings() throws IOException, InterruptedException, AssetException
    {
        InputStream input = mock(InputStream.class);
        SetCameraSettingsCommand setCameraSettings = new SetCameraSettingsCommand();
        setCameraSettings.setZoom((float).42);
        when(m_UrlUtil.getInputStream(any(URL.class))).thenReturn(input);
        when(m_Context.getActiveStatus()).thenReturn(AssetActiveStatus.ACTIVATED);
        m_SUT.onActivate(); 
        Response zoomResponse = m_SUT.onExecuteCommand(setCameraSettings);
        ArgumentCaptor<URL> urlCaptor = ArgumentCaptor.forClass(URL.class);
        verify(m_UrlUtil).getInputStream(urlCaptor.capture());
        URL zoomUrl = urlCaptor.getValue();
        assertThat(zoomUrl.toString(), equalTo("http://192.168.1.55/-wvhttp-01-/control?zoom=2900"));
        assertThat(zoomResponse, instanceOf(SetCameraSettingsResponse.class));
    }
    
    @Test(expected = CommandExecutionException.class)
    public void testSetCameraSettingsIoException() throws IOException, InterruptedException, AssetException
    {
        m_SUT.onActivate();
        SetCameraSettingsCommand setCameraSettings = new SetCameraSettingsCommand();
        setCameraSettings.setZoom((float).42);
        when(m_Context.getActiveStatus()).thenReturn(AssetActiveStatus.ACTIVATED);
        when(m_UrlUtil.getInputStream(any(URL.class))).thenThrow(new IOException());
        m_SUT.onExecuteCommand(setCameraSettings);
    }
    
    @Test
    public void testGetPanTilt() throws IOException, InterruptedException, AssetException
    {
        m_SUT.onActivate();
        final byte[] propsBytes = 
                new byte[]{'c', '.', '1', '.', 'p', 'a', 'n', ':', '=', '9', '4', '.', '4', '\n',
                           'c', '.', '1', '.', 't', 'i', 'l', 't', ':', '=', '1', '0', '0', '\n'};
        InputStream input1 = new InputStream()
        {
            private int m_Counter;
            private int m_ReadBytesCounter;
            @Override
            public int read() throws IOException
            {
                int returnVal = -1;
                if (m_Counter < propsBytes.length)
                {
                    returnVal = propsBytes[m_Counter];
                    m_Counter++;
                }
                return returnVal;
            }
            
            @Override
            public int read(byte[] b) throws IOException
            {
                int bytesRead = -1;
                if (m_ReadBytesCounter == 0)
                {
                    System.arraycopy(propsBytes, 0, b, 0, propsBytes.length);
                    bytesRead = propsBytes.length;
                }
                m_ReadBytesCounter++;
                return bytesRead;
            }
        };
        InputStream input2 = new InputStream()
        {
            private int m_Counter;
            private int m_ReadBytesCounter;
            @Override
            public int read() throws IOException
            {
                int returnVal = -1;
                if (m_Counter < propsBytes.length)
                {
                    returnVal = propsBytes[m_Counter];
                    m_Counter++;
                }
                return returnVal;
            }
            
            @Override
            public int read(byte[] b) throws IOException
            {
                int bytesRead = -1;
                if (m_ReadBytesCounter == 0)
                {
                    System.arraycopy(propsBytes, 0, b, 0, propsBytes.length);
                    bytesRead = propsBytes.length;
                }
                m_ReadBytesCounter++;
                return bytesRead;
            }
        };
        URLConnection urlCon = mock(URLConnection.class);
        when(m_UrlUtil.getConnection(any(URL.class))).thenReturn(urlCon).thenReturn(urlCon);
        when(m_UrlUtil.getInputStream(any(URLConnection.class))).thenReturn(input1).thenReturn(input2);
        when(m_Context.getActiveStatus()).thenReturn(AssetActiveStatus.ACTIVATED);
        ArgumentCaptor<URL> urlCaptor = ArgumentCaptor.forClass(URL.class);
        GetPanTiltCommand fakeGetPanTilt = new GetPanTiltCommand();     
        GetPanTiltResponse getPanTiltResponse = (GetPanTiltResponse)m_SUT.onExecuteCommand(fakeGetPanTilt);
        verify(m_UrlUtil, times(2)).getConnection(urlCaptor.capture());
        URL infoUrl = urlCaptor.getValue(); 
        assertThat(getPanTiltResponse, instanceOf(GetPanTiltResponse.class));
        assertThat(infoUrl.toString(), equalTo("http://192.168.1.55/-wvhttp-01-/info.cgi"));
        assertThat(getPanTiltResponse.getPanTilt().getAzimuth().getValue(), equalTo((double)1));
        assertThat(getPanTiltResponse.getPanTilt().getElevation().getValue(), equalTo((double)1));
    }
    
    //Catches the IO exception in the handleGetTilt when trying to create a URL connection
    @Test(expected = CommandExecutionException.class)
    public void testGetTiltIoException1() throws IOException, InterruptedException, AssetException
    {
        m_SUT.onActivate();
        GetPanTiltCommand fakeGetPanTilt = new GetPanTiltCommand();
        when(m_Context.getActiveStatus()).thenReturn(AssetActiveStatus.ACTIVATED);
        when(m_UrlUtil.getConnection(any(URL.class))).thenThrow(new IOException()).thenThrow(new IOException());
        m_SUT.onExecuteCommand(fakeGetPanTilt);  
    }
    
    //Catches the IO exception in the handleGetTilt when trying to create an Input Stream
    @Test(expected = CommandExecutionException.class)
    public void testGetTiltIoException2() throws IOException, InterruptedException, AssetException
    {
        m_SUT.onActivate();
        GetPanTiltCommand fakeGetPanTilt = new GetPanTiltCommand();
        URLConnection urlCon = mock(URLConnection.class);
        when(m_Context.getActiveStatus()).thenReturn(AssetActiveStatus.ACTIVATED);
        when(m_UrlUtil.getConnection(any(URL.class))).thenReturn(urlCon).thenReturn(urlCon);
        when(m_UrlUtil.getInputStream(any(URLConnection.class)))
            .thenThrow(new IOException()).thenThrow(new IOException());
        m_SUT.onExecuteCommand(fakeGetPanTilt);  
    }
    
    //Catches the IO exception in the handleGetPan when trying to create an URL connection
    @Test(expected = CommandExecutionException.class)
    public void testGetPanIoException1() throws IOException, InterruptedException, AssetException
    {
        m_SUT.onActivate();
        final byte[] propsBytes = 
                new byte[]{'c', '.', '1', '.', 'p', 'a', 'n', ':', '=', '9', '4', '.', '4', '\n',
                           'c', '.', '1', '.', 't', 'i', 'l', 't', ':', '=', '1', '0', '0', '\n'};
        InputStream input = new InputStream()
        {
            private int m_Counter;
            private int m_ReadBytesCounter;
            @Override
            public int read() throws IOException
            {
                int returnVal = -1;
                if (m_Counter < propsBytes.length)
                {
                    returnVal = propsBytes[m_Counter];
                    m_Counter++;
                }
                return returnVal;
            }
            
            @Override
            public int read(byte[] b) throws IOException
            {
                int bytesRead = -1;
                if (m_ReadBytesCounter == 0)
                {
                    System.arraycopy(propsBytes, 0, b, 0, propsBytes.length);
                    bytesRead = propsBytes.length;
                }
                m_ReadBytesCounter++;
                return bytesRead;
            }
        };
        GetPanTiltCommand fakeGetPanTilt = new GetPanTiltCommand();
        URLConnection urlCon = mock(URLConnection.class);
        when(m_Context.getActiveStatus()).thenReturn(AssetActiveStatus.ACTIVATED);
        when(m_UrlUtil.getConnection(any(URL.class))).thenReturn(urlCon).thenThrow(new IOException());
        when(m_UrlUtil.getInputStream(any(URLConnection.class))).thenReturn(input);
        m_SUT.onExecuteCommand(fakeGetPanTilt);  
    }
    
    //Catches the IO exception in the handleGetTilt when trying to create an Input Stream
    @Test(expected = CommandExecutionException.class)
    public void testGetPanIoException2() throws IOException, InterruptedException, AssetException
    {
        m_SUT.onActivate();
        final byte[] propsBytes = 
                new byte[]{'c', '.', '1', '.', 'p', 'a', 'n', ':', '=', '9', '4', '.', '4', '\n',
                           'c', '.', '1', '.', 't', 'i', 'l', 't', ':', '=', '1', '0', '0', '\n'};
        InputStream input = new InputStream()
        {
            private int m_Counter;
            private int m_ReadBytesCounter;
            @Override
            public int read() throws IOException
            {
                int returnVal = -1;
                if (m_Counter < propsBytes.length)
                {
                    returnVal = propsBytes[m_Counter];
                    m_Counter++;
                }
                return returnVal;
            }
            
            @Override
            public int read(byte[] b) throws IOException
            {
                int bytesRead = -1;
                if (m_ReadBytesCounter == 0)
                {
                    System.arraycopy(propsBytes, 0, b, 0, propsBytes.length);
                    bytesRead = propsBytes.length;
                }
                m_ReadBytesCounter++;
                return bytesRead;
            }
        };
        GetPanTiltCommand fakeGetPanTilt = new GetPanTiltCommand();
        URLConnection urlCon = mock(URLConnection.class);
        when(m_Context.getActiveStatus()).thenReturn(AssetActiveStatus.ACTIVATED);
        when(m_UrlUtil.getConnection(any(URL.class))).thenReturn(urlCon).thenReturn(urlCon);
        when(m_UrlUtil.getInputStream(any(URLConnection.class))).thenReturn(input).thenThrow(new IOException());
        m_SUT.onExecuteCommand(fakeGetPanTilt);  
    }
    
    @Test
    public void testGetCameraSettings() throws IOException, InterruptedException, AssetException
    {
        m_SUT.onActivate();
        final byte[] propsBytes = 
                new byte[]{'c', '.', '1', '.', 'z', 'o', 'o', 'm', ':', '=', '5', '0', '0', '\n'};                  
        InputStream inStream = new InputStream()
        {
            private int m_Counter;
            private int m_ReadBytesCounter;
            @Override
            public int read() throws IOException
            {
                int returnVal = -1;
                if (m_Counter < propsBytes.length)
                {
                    returnVal = propsBytes[m_Counter];
                    m_Counter++;
                }
                return returnVal;
            }
            
            @Override
            public int read(byte[] b) throws IOException
            {
                int bytesRead = -1;
                if (m_ReadBytesCounter == 0)
                {
                    System.arraycopy(propsBytes, 0, b, 0, propsBytes.length);
                    bytesRead = propsBytes.length;
                }
                m_ReadBytesCounter++;
                return bytesRead;
            }
        }; 
        URLConnection urlCon = mock(URLConnection.class);
        when(m_UrlUtil.getConnection(any(URL.class))).thenReturn(urlCon);
        when(m_UrlUtil.getInputStream(any(URLConnection.class))).thenReturn(inStream);
        when(m_Context.getActiveStatus()).thenReturn(AssetActiveStatus.ACTIVATED);
        ArgumentCaptor<URL> urlCaptor = ArgumentCaptor.forClass(URL.class);
        GetCameraSettingsCommand fakeGetCameraSettings = new GetCameraSettingsCommand();     
        GetCameraSettingsResponse getCameraSettingsResponse = (GetCameraSettingsResponse)m_SUT
                 .onExecuteCommand(fakeGetCameraSettings);
        verify(m_UrlUtil).getConnection(urlCaptor.capture());
        URL infoUrl = urlCaptor.getValue();
        assertThat(getCameraSettingsResponse, instanceOf(GetCameraSettingsResponse.class));
        assertThat(infoUrl.toString(), equalTo("http://192.168.1.55/-wvhttp-01-/info.cgi"));
        assertThat(getCameraSettingsResponse.getZoom(), equalTo((float)0.9));
    }
    
    //Catches the IO exception when getCameraSettings tries to create a URL connection to the URL
    @Test(expected = CommandExecutionException.class)
    public void testGetCameraSettingsIOException1() throws IOException, InterruptedException, AssetException
    {
        m_SUT.onActivate();
        GetCameraSettingsCommand fakeGetCameraSettings = new GetCameraSettingsCommand();
        when(m_Context.getActiveStatus()).thenReturn(AssetActiveStatus.ACTIVATED);
        when(m_UrlUtil.getConnection(any(URL.class))).thenThrow(new IOException());
        m_SUT.onExecuteCommand(fakeGetCameraSettings);  
    }
    
    //Catches the IO exception when getCameraSettings tries to create an Input Stream
    @Test(expected = CommandExecutionException.class)
    public void testGetCameraSettingsIOException2() throws IOException, InterruptedException, AssetException
    {
        m_SUT.onActivate();
        GetCameraSettingsCommand fakeGetCameraSettings = new GetCameraSettingsCommand();
        URLConnection urlCon = mock(URLConnection.class);
        when(m_Context.getActiveStatus()).thenReturn(AssetActiveStatus.ACTIVATED);
        when(m_UrlUtil.getConnection(any(URL.class))).thenReturn(urlCon);
        when(m_UrlUtil.getInputStream(any(URLConnection.class))).thenThrow(new IOException());
        m_SUT.onExecuteCommand(fakeGetCameraSettings);  
    }
}