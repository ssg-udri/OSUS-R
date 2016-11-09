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
package edu.udayton.udri.smartphone;

import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TestSmartphoneSensorAsset 
{
    private SmartphoneSensorAsset m_SUT;
    @Mock private AssetContext m_Context;
    
    private Map<String, Object> m_Props;
    
    @Before
    public void setUp() throws FactoryException
    {
        m_SUT = new SmartphoneSensorAsset();
        
        m_Props = new HashMap<String, Object>();

        m_Props.put("latitude", -36.3);
        m_Props.put("longitude", 84.7);
        m_Props.put("batteryLevel", .91);
        
        m_SUT.initialize(m_Context, m_Props);
    }
    
    @Test
    public void testUpdated() throws Exception
    {
        m_SUT.updated(m_Props);
        
        verify(m_Context).persistObservation(any(Observation.class));
        verify(m_Context).setStatus(any(Status.class));
    }
    
    @Test
    public void testOnActivate()
    {
        try
        {
            m_SUT.onActivate();
            Assert.fail("Calling onActivate() failed to throw an exception");
        }
        catch (Exception e) //NOPMD: Expected behavior.
        {                   //NOCHECKSTYLE: Expected behavior.
            
        }
    }
    
    @Test
    public void testOnDeactivate()
    {
        try
        {
            m_SUT.onDeactivate();
            Assert.fail("Calling onDeactivate() failed to throw an exception");
        }
        catch (Exception e) //NOPMD: Expected behavior.
        {                   //NOCHECKSTYLE: Expected behavior.
            
        }
    }
    
    @Test
    public void testOnCaptureData()
    {
        try
        {
            m_SUT.onCaptureData();
            Assert.fail("Calling onCaptureData() failed to throw an exception");
        }
        catch (Exception e) //NOPMD: Expected behavior.
        {                   //NOCHECKSTYLE: Expected behavior.
            
        }
    }
    
    @Test
    public void testOnPerformBit()
    {
        try
        {
            m_SUT.onPerformBit();
            Assert.fail("Calling onPerformBit() failed to throw an exception");
        }
        catch (Exception e) //NOPMD: Expected behavior.
        {                   //NOCHECKSTYLE: Expected behavior.
            
        }
    }
    
    @Test
    public void testOnExecuteCommand()
    {
        Command command = mock(Command.class);
        
        try
        {
            m_SUT.onExecuteCommand(command);
            Assert.fail("Calling onExecuteCommand() failed to throw an exception");
        }
        catch (Exception e) //NOPMD: Expected behavior.
        {                   //NOCHECKSTYLE: Expected behavior.
            
        }
    }
}
