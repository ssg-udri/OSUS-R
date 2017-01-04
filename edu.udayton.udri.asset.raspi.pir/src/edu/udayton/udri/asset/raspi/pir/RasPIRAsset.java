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
package edu.udayton.udri.asset.raspi.pir;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.observation.types.Detection;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.core.types.SensingModality;
import mil.dod.th.core.types.SensingModalityEnum;
import mil.dod.th.core.types.detection.DetectionTypeEnum;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.ose.shared.pm.CountingWakeLock;
import mil.dod.th.ose.shared.pm.CountingWakeLock.CountingWakeLockHandle;

import org.osgi.service.log.LogService;

/**
 * Raspberry Pi PIR Plug-In.
 * 
 * @author Nathaniel Rosenwald
 */
@Component(factory = Asset.FACTORY)
public class RasPIRAsset implements AssetProxy
{ 
    private AssetContext m_Context;
    
    private GpioController m_GpioLink;
    private GpioPinDigitalInput m_Pir;

    /**
     * Reference to the counting {@link WakeLock} used by this asset.
     */
    private CountingWakeLock m_CountingLock = new CountingWakeLock();

    @Override
    public void initialize(final AssetContext context, final Map<String, Object> props) throws FactoryException
    {
        m_GpioLink = GpioFactory.getInstance();
        
        m_Context = context;
        m_CountingLock.setWakeLock(m_Context.createPowerManagerWakeLock(getClass().getSimpleName() + "WakeLock"));
        m_Context.setStatus(SummaryStatusEnum.GOOD, "Initialized");
    }
    
    /**
     * OSGi deactivate method used to delete any wake locks used by the asset.
     */
    @Deactivate
    public void tearDown()
    {
        m_CountingLock.deleteWakeLock();
    }
    
    @Override
    public void updated(final Map<String, Object> props)
    {
        //There are currently no attributes to be updated.
    }
    
    @Override
    public void onActivate() throws AssetException
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            Logging.log(LogService.LOG_INFO, "Beginning activation");
            
            try 
            {
                m_Pir = m_GpioLink.provisionDigitalInputPin(RaspiPin.GPIO_07, PinPullResistance.PULL_DOWN);
            
                m_Pir.addListener(new GpioPinListenerDigital() 
                {
                    @Override
                    public void handleGpioPinDigitalStateChangeEvent(final GpioPinDigitalStateChangeEvent event) 
                    {
                        if (event.getState() == PinState.HIGH)
                        {
                            try
                            {
                                onMotionDetection();
                            }
                            catch (final AssetException e)
                            {
                                Logging.log(LogService.LOG_ERROR, e.toString());
                                Logging.log(LogService.LOG_ERROR, "Creation of PIR observation failed");
                            }
                        }
                    } 
                });
                Logging.log(LogService.LOG_INFO, "Raspberry Pi PIR activated");
                m_Context.setStatus(SummaryStatusEnum.GOOD, "Activated");
            } 
            catch (final Exception e) 
            {
                Logging.log(LogService.LOG_ERROR, e.toString());
                Logging.log(LogService.LOG_ERROR, "Raspberry Pi PIR activation failed, attempting deactivation");
                m_Context.setStatus(SummaryStatusEnum.BAD, "Failed activation, attempting deactivation");
                onDeactivate();
            }
        }
    }
    
    @Override
    public void onDeactivate() throws AssetException
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            m_Pir.removeAllListeners();
            m_GpioLink.unprovisionPin(m_Pir);
        
            Logging.log(LogService.LOG_INFO, "Raspberry Pi PIR deactivated");
            m_Context.setStatus(SummaryStatusEnum.OFF, "Deactivated");
        }
    }
    
    /**
     * Method that creates and returns an observation with the sensing modality and detection type
     * when motion has been detected.
     * 
     * @return 
     *      persisted observation with sensing modality and detection type
     * @throws AssetException 
     *      thrown if creation of observation fails
     */
    private Observation onMotionDetection() throws AssetException
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            final Date date = new Date();
            Logging.log(LogService.LOG_INFO, "Motion Detected " + date);
    
            final SensingModality sensmod = new SensingModality(SensingModalityEnum.PIR, "PIR");
    
            final Detection detect = new Detection();
            detect.setType(DetectionTypeEnum.ALARM);
            
            final Observation obs = new Observation().withModalities(sensmod).withDetection(detect);
            try 
            {
                m_Context.persistObservation(obs);
            } 
            catch (final PersistenceFailedException | ValidationFailedException e) 
            {
                Logging.log(LogService.LOG_ERROR, e.toString());
                Logging.log(LogService.LOG_ERROR, "Observation persistance failed");
            }
            return obs;
        }
    }
    
    @Override
    public Observation onCaptureData() throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException("onCaptureData() is not supported");
    }
    
    @Override
    public Observation onCaptureData(final String sensorId) throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException("onCaptureData(sensorId) is not supported");
    }
    
    @Override
    public Status onPerformBit()
    {
        Logging.log(LogService.LOG_INFO, "Performing Plug-in BIT");
        return new Status();
    }
    
    @Override
    public Response onExecuteCommand(final Command command) throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException("onExecuteCommand() is not supported");
    }
    
    @Override
    public Set<Extension<?>> getExtensions()
    {   
        return new HashSet<Extension<?>>();
    }
}
