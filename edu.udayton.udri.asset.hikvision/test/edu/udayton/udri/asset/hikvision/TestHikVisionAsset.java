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

import static org.hamcrest.MatcherAssert.assertThat; //NOCHECKSTYLE don't have to put MatcherAssert in front assertThat
import static org.hamcrest.CoreMatchers.equalTo; //NOCHECKSTYLE So you don't have to put CoreMatchers in front equalTo
import static org.hamcrest.CoreMatchers.is;//NOCHECKSTYLE So you don't have to put CoreMatchers in front is
import static org.hamcrest.core.IsNull.notNullValue;//NOCHECKSTYLE So you don't have to put IsNull in front notNullValue
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.GetCameraSettingsCommand;
import mil.dod.th.core.asset.commands.GetCameraSettingsResponse;
import mil.dod.th.core.asset.commands.GetPanTiltCommand;
import mil.dod.th.core.asset.commands.GetPanTiltResponse;
import mil.dod.th.core.asset.commands.SetCameraSettingsCommand;
import mil.dod.th.core.asset.commands.SetCameraSettingsResponse;
import mil.dod.th.core.asset.commands.SetPanTiltCommand;
import mil.dod.th.core.asset.commands.SetPanTiltResponse;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.core.types.status.OperatingStatus;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.core.validator.ValidationFailedException;

/**
 * @author Noah
 *
 */
public class TestHikVisionAsset  //NOCHECKSTYLE - Abstraction Coupling is above Allowed.
{
    private HikVisionAsset m_SUT;
     
    @Mock private UrlUtil m_UrlUtil;
    @Mock private CommandProcessor m_CommandProcessor;
    @Mock private AssetContext m_Context;
    @Mock private WakeLock m_WakeLock;
    
    @Before
    public void setup() throws FactoryException
    { 
        MockitoAnnotations.initMocks(this);
        when(m_Context.createPowerManagerWakeLock(HikVisionAsset.class.getSimpleName() 
                + "WakeLock")).thenReturn(m_WakeLock);
        
        m_SUT = new HikVisionAsset(); 
        m_SUT.setUrlUtil(m_UrlUtil);
        m_SUT.setCommandProcessor(m_CommandProcessor); 
        Map<String, Object> props = new HashMap<>();
        props.put(HikVisionAssetAttributes.USERNAME, "joe"); 
        props.put(HikVisionAssetAttributes.PASSWORD, "32423");
        props.put(HikVisionAssetAttributes.IP, "113243");
     
        m_SUT.initialize(m_Context, props);       
    } 
    
    @Test
    public void testUpdate()
    {        
        Map<String, Object> props = new HashMap<>();
        props.put(HikVisionAssetAttributes.USERNAME, "You");
        props.put(HikVisionAssetAttributes.PASSWORD, "1"); 
        props.put(HikVisionAssetAttributes.IP, "113243"); 
        
        m_SUT.updated(props);        
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
        assertThat(opStatus.getDescription(), equalTo("Activated"));       
    }
    
    @Test 
    public void testOnActivateException() throws  AssetException, ValidationFailedException
    {   
        Status status = mock(Status.class); 
        doThrow(new ValidationFailedException()).when(m_Context).setStatus(status);
        m_SUT.onActivate();            
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
        assertThat(opStatus.getDescription(), equalTo("Deactivated"));    
    }
     
    @Test
    public void testOnBitHttpConnection() throws IOException
    {        
        HttpURLConnection urlConnection = mock(HttpURLConnection.class);
        when(m_UrlUtil.getConnection(any(URL.class))).thenReturn(urlConnection);        
        String data = "string\n";
        InputStream inStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));  
        when(urlConnection.getInputStream()).thenReturn(inStream);        
        Status status = m_SUT.onPerformBit();  
        
        verify(m_UrlUtil).getConnection(any(URL.class));
        verify(urlConnection).getInputStream();     
        
        assertThat(status.getSummaryStatus().getSummary(), equalTo(SummaryStatusEnum.GOOD));
        assertThat(status.getSummaryStatus().getDescription(), equalTo("BIT Passed"));        
    }
    
    @Test(expected = NullPointerException.class)
    public void testOnBitExceptionInput() throws IOException
    {
        HttpURLConnection urlConnection = mock(HttpURLConnection.class);
        when(m_UrlUtil.getConnection(any(URL.class))).thenReturn(urlConnection);
        doThrow(new ProtocolException()).when(urlConnection).setRequestMethod(any(String.class));        
        m_SUT.onPerformBit();  
    }
    
    @Test
    public void testOnBitExceptionBuffInput() throws IOException
    {
        HttpURLConnection urlConnection = mock(HttpURLConnection.class);
        when(m_UrlUtil.getConnection(any(URL.class))).thenReturn(urlConnection);
        when(urlConnection.getInputStream()).thenThrow(new IOException("ok"));        
        m_SUT.onPerformBit(); 
    }
     
    @Test 
    public void testOnBitExceptionConnection() throws IOException
    {
        when(m_UrlUtil.getConnection(any(URL.class))).thenThrow(new IOException("go"));       
        m_SUT.onPerformBit();
    } 
    
    @Test
    public void testOnBitExceptionInline() throws IOException
    {
        HttpURLConnection urlConnection = mock(HttpURLConnection.class);
        BufferedReader inStream = mock(BufferedReader.class);  
        
        when(m_UrlUtil.getConnection(any(URL.class))).thenReturn(urlConnection);
        String data = "string\n";
        InputStream inline = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));

        when(urlConnection.getInputStream()).thenReturn(inline);
        when(inStream.readLine()).thenThrow(new IOException("yep"));
        doThrow(new IOException("lll")).when(inStream).close(); 
         
        m_SUT.onPerformBit();
    }
   
    @Test
    public void testOnCaptureData() throws IOException
    {
        HttpURLConnection urlConnection = mock(HttpURLConnection.class);        
        String data = "string;soif;oisf;oisaifdsihfdoiusaf \n";
        InputStream inStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));

        when(m_UrlUtil.getConnection(any(URL.class))).thenReturn(urlConnection);
        when(urlConnection.getInputStream()).thenReturn(inStream);         
        Observation testResult = m_SUT.onCaptureData(); 
        
        assertThat(testResult.getDigitalMedia().getValue(), is(data.getBytes(StandardCharsets.UTF_8)));
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }
    
    @Test
    public void testOnCaptureDataExceptionInputStream() throws IOException
    {
        HttpURLConnection urlConnection = mock(HttpURLConnection.class);
        when(m_UrlUtil.getConnection(any(URL.class))).thenReturn(urlConnection);        
        when(urlConnection.getInputStream()).thenThrow(new IOException("ok"));  
        
        m_SUT.onCaptureData(); 
        
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }
    
    @Test
    public void testOnCaptureDataExceptionConnect() throws IOException
    { 
        when(m_UrlUtil.getConnection(any(URL.class))).thenThrow(new IOException("go"));        
        m_SUT.onCaptureData();  
        
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }
    
    @Test (expected = CommandExecutionException.class)
    public void testOnExecuteCommandException() throws InterruptedException, CommandExecutionException
    {        
        Command command = mock(Command.class);
        
        m_SUT.onExecuteCommand(command);
        
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }
    
    @Test
    public void testOnExecuteCommandSetPanTiltTrue() 
            throws CommandExecutionException, InterruptedException, IOException 
    {
        HttpURLConnection urlConnection = mock(HttpURLConnection.class);
        OutputStream outputStream = mock(OutputStream.class);        
        String data = "<absoluteZoom>10<absoluteZoom>";
        InputStream inStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));

        when(m_UrlUtil.getConnection(any(URL.class))).thenReturn(urlConnection);
        when(urlConnection.getInputStream()).thenReturn(inStream).thenReturn(inStream);
        when(urlConnection.getOutputStream()).thenReturn(outputStream);
        when(m_CommandProcessor.isAzimuthSet(any(SetPanTiltCommand.class))).thenReturn(true);
        when(m_CommandProcessor.isElevationSet(any(SetPanTiltCommand.class))).thenReturn(true);
        when(m_CommandProcessor.getAzimuth(any(SetPanTiltCommand.class))).thenReturn(3);
        when(m_CommandProcessor.getElevation(any(SetPanTiltCommand.class))).thenReturn(10);        
        
        SetPanTiltResponse testResult = (SetPanTiltResponse)m_SUT.onExecuteCommand(new SetPanTiltCommand());
        verify(m_CommandProcessor).getAzimuth(any(SetPanTiltCommand.class));
        verify(m_CommandProcessor).getElevation(any(SetPanTiltCommand.class));
        assertThat(testResult, is(notNullValue()));
        
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }
    
    @Test
    public void testOnExecuteCommandSetPanTiltFalse() 
            throws CommandExecutionException, InterruptedException, IOException 
    {
        HttpURLConnection urlConnection = mock(HttpURLConnection.class);
        OutputStream outputStream = mock(OutputStream.class);        
        String data1 = "<azimuth>2000<azimuth>";
        InputStream inStream1 = new ByteArrayInputStream(data1.getBytes(StandardCharsets.UTF_8));
        String data2 = "<elevation>20<elevation>";
        InputStream inStream2 = new ByteArrayInputStream(data2.getBytes(StandardCharsets.UTF_8));

        when(m_UrlUtil.getConnection(any(URL.class))).thenReturn(urlConnection).thenReturn(urlConnection)
                .thenReturn(urlConnection).thenReturn(urlConnection);
        when(urlConnection.getInputStream()).thenReturn(inStream1).thenReturn(inStream1)
                .thenReturn(inStream2).thenReturn(inStream2);
        when(urlConnection.getOutputStream()).thenReturn(outputStream);
        when(m_CommandProcessor.isAzimuthSet(any(SetPanTiltCommand.class))).thenReturn(false);
        when(m_CommandProcessor.isElevationSet(any(SetPanTiltCommand.class))).thenReturn(false);       
        
        SetPanTiltResponse testResult = (SetPanTiltResponse)m_SUT.onExecuteCommand(new SetPanTiltCommand());
        assertThat(testResult, is(notNullValue()));
        
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }
    
    @Test
    public void testOnExecuteCommandSetCameraSettings() 
            throws CommandExecutionException, InterruptedException, IOException
    {
        HttpURLConnection urlConnection = mock(HttpURLConnection.class); 
        OutputStream outputStream = mock(OutputStream.class);        
        String data1 = "<azimuth>20<azimuth>";
        String data2 = "<elevation>15<elevation>";
        String data3 = "<azimuth>2500<azimuth>";
        InputStream inStream1 = new ByteArrayInputStream(data2.getBytes(StandardCharsets.UTF_8));
        InputStream inStream2 = new ByteArrayInputStream(data3.getBytes(StandardCharsets.UTF_8));
        InputStream inStream3 = new ByteArrayInputStream(data2.getBytes(StandardCharsets.UTF_8));
        InputStream inStream4 = new ByteArrayInputStream(data1.getBytes(StandardCharsets.UTF_8));
        InputStream inStream5 = new ByteArrayInputStream(data1.getBytes(StandardCharsets.UTF_8));
        
        when(m_UrlUtil.getConnection(any(URL.class))).thenReturn(urlConnection); 
        when(m_CommandProcessor.getZoom(any(SetCameraSettingsCommand.class))).thenReturn(1);
        
        //Each thenReturn is in the order of when getInputStream is called.
        when(urlConnection.getInputStream()).thenReturn(inStream1).thenReturn(inStream2)
        .thenReturn(inStream5).thenReturn(inStream3).thenReturn(inStream4);
        when(urlConnection.getResponseCode()).thenReturn(6);
        when(urlConnection.getOutputStream()).thenReturn(outputStream);

        SetCameraSettingsResponse testResult = (SetCameraSettingsResponse)m_SUT.onExecuteCommand(
                new SetCameraSettingsCommand()); 
         
        assertThat(testResult, is(notNullValue()));
        
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }
    
    @Test
    public void testOnExecuteCommandSetCameraSettingsWithNegitveNumber() throws 
        CommandExecutionException, InterruptedException, IOException
    {
        HttpURLConnection urlConnection = mock(HttpURLConnection.class);
        OutputStream outputStream = mock(OutputStream.class);        
        String data1 = "<azimuth>20<azimuth>";
        String data2 = "<elevation>15<elevation>";
        String data3 = "<azimuth>-10<azimuth>";
        InputStream inStream1 = new ByteArrayInputStream(data2.getBytes(StandardCharsets.UTF_8));
        InputStream inStream2 = new ByteArrayInputStream(data3.getBytes(StandardCharsets.UTF_8));
        InputStream inStream3 = new ByteArrayInputStream(data2.getBytes(StandardCharsets.UTF_8));
        InputStream inStream4 = new ByteArrayInputStream(data1.getBytes(StandardCharsets.UTF_8));
        InputStream inStream5 = new ByteArrayInputStream(data3.getBytes(StandardCharsets.UTF_8));
        
        when(m_UrlUtil.getConnection(any(URL.class))).thenReturn(urlConnection); 
        when(m_CommandProcessor.getZoom(any(SetCameraSettingsCommand.class))).thenReturn(1);
        when(urlConnection.getInputStream()).thenReturn(inStream1).thenReturn(inStream2)
        .thenReturn(inStream5).thenReturn(inStream3).thenReturn(inStream4);
        when(urlConnection.getResponseCode()).thenReturn(666);
        when(urlConnection.getOutputStream()).thenReturn(outputStream);
        SetCameraSettingsResponse testResult = (SetCameraSettingsResponse)m_SUT.onExecuteCommand(
                new SetCameraSettingsCommand());          

        assertThat(testResult, is(notNullValue()));
        
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    } 
    
    @Test(expected = NullPointerException.class)
    public void testOnExecuteCommandSetCameraSettingsException() 
            throws IOException, CommandExecutionException, InterruptedException 
    {
        when(m_UrlUtil.getConnection(any(URL.class))).thenThrow(new IOException()); 
        m_SUT.onExecuteCommand(new SetCameraSettingsCommand()); 
        
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }
         
    @Test
    public void testOnExecuteCommandGetPanTilt() throws CommandExecutionException, InterruptedException, IOException
    {
        HttpURLConnection urlConnection = mock(HttpURLConnection.class);             
        String data1 = "<azimuth>20<azimuth>";
        String data2 = "<elevation>15<elevation>"; 
        InputStream inStream1 = new ByteArrayInputStream(data2.getBytes(StandardCharsets.UTF_8));
        InputStream inStream2 = new ByteArrayInputStream(data1.getBytes(StandardCharsets.UTF_8));
        InputStream inStream3 = new ByteArrayInputStream(data1.getBytes(StandardCharsets.UTF_8)); 
        InputStream inStream4 = new ByteArrayInputStream(data2.getBytes(StandardCharsets.UTF_8));
      
        when(m_UrlUtil.getConnection(Mockito.any(URL.class))).thenReturn(urlConnection);
        when(urlConnection.getInputStream()).thenReturn(inStream1)
        .thenReturn(inStream2).thenReturn(inStream3).thenReturn(inStream4);
        GetPanTiltResponse testResult = (GetPanTiltResponse)m_SUT.onExecuteCommand(new GetPanTiltCommand()); 
        
        assertThat(testResult.getPanTilt().getAzimuth().getValue(), equalTo(2.0));        
        assertThat(testResult.getPanTilt().getElevation().getValue(), equalTo(1.5));        
    
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }    
    
    @Test(expected = NullPointerException.class)
    public void testOnExecuteCommandGetPanTiltException() 
            throws IOException, CommandExecutionException, InterruptedException
    {
        when(m_UrlUtil.getConnection(Mockito.any(URL.class))).thenThrow(new IOException());
        m_SUT.onExecuteCommand(new GetPanTiltCommand());  
        
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }
    
    @Test
    public void testOnExecuteCommandGetCameraSettings() 
            throws CommandExecutionException, InterruptedException, IOException
    {        
        HttpURLConnection urlConnection = mock(HttpURLConnection.class);        
        String data = "<absoluteZoom>300<absoluteZoom>";
        InputStream inStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)); 
        
        when(m_UrlUtil.getConnection(Mockito.any(URL.class))).thenReturn(urlConnection);
        when(urlConnection.getInputStream()).thenReturn(inStream);          
        GetCameraSettingsResponse testResults = (GetCameraSettingsResponse)
                m_SUT.onExecuteCommand(new GetCameraSettingsCommand());
        
        assertThat(testResults.getZoom(), equalTo(.3f));
        
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }
     
    @Test(expected = NullPointerException.class)
    public void testOnExecuteCommandGetCameraSettingsException() 
            throws IOException, CommandExecutionException, InterruptedException
    {
        when(m_UrlUtil.getConnection(any(URL.class))).thenThrow(new IOException());
        m_SUT.onExecuteCommand(new GetCameraSettingsCommand());
        
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }
    
    @Test
    public void testTearDown()
    {
        m_SUT.tearDown();
        
        verify(m_WakeLock).delete();
    }
}