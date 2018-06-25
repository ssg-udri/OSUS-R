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
package edu.udayton.udri.asset.novatel;

import java.io.IOException;
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
import mil.dod.th.core.asset.commands.GetPositionCommand;
import mil.dod.th.core.asset.commands.GetPositionResponse;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.core.types.ComponentType;
import mil.dod.th.core.types.ComponentTypeEnum;
import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.Orientation;
import mil.dod.th.core.types.status.ComponentStatus;
import mil.dod.th.core.types.status.OperatingStatus;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.ose.shared.pm.CountingWakeLock;
import mil.dod.th.ose.shared.pm.CountingWakeLock.CountingWakeLockHandle;

import org.osgi.service.log.LogService;

import edu.udayton.udri.asset.novatel.connection.NovatelConnectionMgr;
import edu.udayton.udri.asset.novatel.message.MessageReader;
import edu.udayton.udri.asset.novatel.message.MessageReceiver;
import edu.udayton.udri.asset.novatel.message.NovatelInsMessage;
import edu.udayton.udri.asset.novatel.message.NovatelMessageException;
import edu.udayton.udri.asset.novatel.message.NovatelMessageException.FormatProblem;
import edu.udayton.udri.asset.novatel.message.NovatelMessageParser;
import edu.udayton.udri.asset.novatel.timechanger.TimeChange;

/**
 * Novatel IMU/GPS Asset implementation. 
 * More information about the data messages and factory configuration of the hardware which this plug-in is 
 * representing can be found at http://www.novatel.com/assets/Documents/Manuals/om-20000122.pdf and
 * http://www.novatel.com/assets/Documents/Manuals/om-20000094.pdf .
 *
 * @author allenchl
 */
@Component(factory = Asset.FACTORY)
public class NovatelAsset implements AssetProxy, MessageReceiver, StatusHandler
{
    /**
     * Component description string.
     */
    private static final String COMPONENT_DESCRIPTION = "INS Unit";
    
    /**
    * Component type identifier for item that implements this interface.
    */
    private static final ComponentTypeEnum COMPONENT_TYPE_IDENTIFIER = ComponentTypeEnum.NAVIGATION;
    
    /**
     * Reference to the asset context for the specific asset instance.
     */
    private AssetContext m_Context;
    
    /**
     * The service which offers novatel data messages for this asset to process.
     */
    private MessageReader m_MessageReader;
    
    /**
     * The service which handles the communication layer between the software and the SPAN-CPT hardware.
     */
    private NovatelConnectionMgr m_ConnectionManager;
    
    /**
     * The {@link TimeChange} interface to use to update the system time.
     */
    private TimeChange m_TimeChangeService; 
    
    /**
     * The service which parses the data from the SPAN-CPT unit.
     */
    private NovatelMessageParser m_Parser;
    
    /**
     * The service which calculates the overall asset status.
     */
    private NovatelStatusService m_StatusService; 
    
    /**
     * The frequency in which INS messages should be handled.
     */
    private int m_HandleInsFreq;
    
    /**
     * THe name of the physical port used to receive data from the Novatel.
     */
    private String m_PhysPort;
    
    /**
     * Baud rate used by the physical port.
     */
    private int m_BaudRate;
    
    /**
     * Port used by the time service to sync system time with the GPS.
     */
    private int m_TimeServicePort;
    
    /**
     * How many INS/position messages have been received since last handling of data.
     */
    private int m_InsIterations = 1;
    
    /**
     * The latest orientation information from this asset.
     */
    private Orientation m_Orientation;
    
    /**
     * The latest coordinates information from this asset.
     *
     */
    private Coordinates m_Coordinates;
    
    /**
     * Flag value denoting if the asset is activated.
     */
    private boolean m_IsActivated;

    /**
     * Flag denoting whether or not to activate the time service.
     */
    private boolean m_SyncSystemTimeWithGps;

    /**
     * Reference to the counting {@link WakeLock} used by this asset.
     */
    private CountingWakeLock m_CountingLock = new CountingWakeLock();

    /**
     * Bind the message reader service.
     * @param reader
     *      the reader service to use
     */
    @Reference
    public void setMessageReader(final MessageReader reader)
    {
        m_MessageReader = reader;
    }
    
    /**
     * Bind the connection manager service.
     * @param manager
     *      the connection manager service to use
     */
    @Reference
    public void setNovatelConnectionMgr(final NovatelConnectionMgr manager)
    {
        m_ConnectionManager = manager;
    }
    
    /**
     * Bind the message parser service.
     * @param parser
     *      the message parser service to use
     */
    @Reference
    public void setNovatelMessageParser(final NovatelMessageParser parser)
    {
        m_Parser = parser;
    }
    
    /**
     * Method used to set the time change service.
     * @param timeService
     *      the time change service.
     */
    @Reference
    public void setTimeChangeService(final TimeChange timeService)
    {
        m_TimeChangeService = timeService;
    }
    
    /**
     * Method used to set the status service.
     * @param statusService
     *      the status service to use.
     */
    @Reference
    public void setStatusService(final NovatelStatusService statusService)
    {
        m_StatusService = statusService;
    }
    
    @Override
    public void initialize(final AssetContext context, final Map<String, Object> props)
    {
        m_Context = context;
        final NovatelAssetAttributes attributes = 
                Configurable.createConfigurable(NovatelAssetAttributes.class, props);
        setProperties(attributes);
        handleStatusUpdate(m_TimeChangeService.getComponentStatus());
        handleStatusUpdate(getComponentStatus(SummaryStatusEnum.OFF, "NovAtel asset is off."));
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
    public void updated(final Map<String, Object> props)
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            final NovatelAssetAttributes attributes = 
                    Configurable.createConfigurable(NovatelAssetAttributes.class, props);
            setProperties(attributes);
            if (!m_IsActivated)
            {
                //nothing to update, all values are pulled at activation
                return;
            }

            //try to stop retrieval of data messages
            try
            {
                //might not have ever started
                m_MessageReader.stopRetrieving();
            }
            catch (final IllegalStateException e)
            {
                Logging.log(LogService.LOG_DEBUG, e, "The retrieval of data messages wasn't previously started.");
            }

            //try to close the serial port being used
            try
            {
                //might not have ever started
                m_ConnectionManager.stopProcessing();
            }
            catch (final IllegalStateException e)
            {
                Logging.log(LogService.LOG_DEBUG, e, "Reading from the Serial Port wasn't previously started.");
            }

            /*
             * try to restart processing and data retrieval with updated property values
             */
            m_InsIterations = 1;
            try
            {
                m_ConnectionManager.startProcessing(m_PhysPort, m_BaudRate);
            }
            catch (final AssetException e)
            {
                Logging.log(LogService.LOG_ERROR, e, "Unable to update the data source manager for the NovAtel asset.");
                handleStatusUpdate(getComponentStatus(SummaryStatusEnum.BAD, e.getMessage()));
                return; // don't try to start retrieving data, it will just error out.
            }
            m_MessageReader.startRetreiving(this);
            handleStatusUpdate(getComponentStatus(
                    SummaryStatusEnum.GOOD, "Novatel asset updated successfully"));
        }
    }

    @Override
    public void onActivate() throws AssetException
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            m_ConnectionManager.startProcessing(m_PhysPort, m_BaudRate);
            m_MessageReader.startRetreiving(this);

            //if connecting to the time service is enabled.
            if (m_SyncSystemTimeWithGps)
            {
                m_TimeChangeService.connectTimeService(this, m_TimeServicePort);
            }

            if (!m_Parser.isOffsetKnown())
            {
                handleStatusUpdate(getComponentStatus(
                        SummaryStatusEnum.BAD, "The UTC offset is not known yet, position data will not be handled."));
            }
            else
            {
                handleStatusUpdate(getComponentStatus(
                        SummaryStatusEnum.GOOD, "Asset has activated."));
            }
            Logging.log(LogService.LOG_INFO, "Novatel Asset activated");
            m_IsActivated = true;
        }
    }

    @Override
    public void onDeactivate() throws AssetException
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            try
            {
                //might not of ever started
                m_MessageReader.stopRetrieving();
            }
            catch (final IllegalStateException e)
            {
                Logging.log(LogService.LOG_DEBUG, e, "Unable to stop retrieving while deactivating.");
            }
            try
            {
                //might not of ever started
                m_ConnectionManager.stopProcessing();
            }
            catch (final IllegalStateException e)
            {
                Logging.log(LogService.LOG_DEBUG, e, "Unable to stop processing while deactivating.");
            }

            if (m_SyncSystemTimeWithGps)
            {
                try
                {
                    m_TimeChangeService.disconnectTimeService();
                }
                catch (final IllegalStateException | IOException | InterruptedException exception)
                {
                    Logging.log(LogService.LOG_ERROR, 
                            "An error occured disconnecting from the time service.", exception);
                }
            }

            Logging.log(LogService.LOG_INFO, "Novatel Asset deactivated");
            handleStatusUpdate(getComponentStatus(
                    SummaryStatusEnum.OFF, "Asset has been deactivated"));
            m_IsActivated = false;
        }
    }

    @Override
    public Observation onCaptureData()
    {
        throw new UnsupportedOperationException(String.format("Asset [%s] does not support capturing data.", 
                m_Context.getName()));
    }

    @Override
    public Observation onCaptureData(final String sensorId)
    {
        throw new UnsupportedOperationException(String.format("Asset [%s] does not support capturing data by sensorId.",
                m_Context.getName()));
    }

    @Override
    public Status onPerformBit() throws AssetException
    {
        throw new UnsupportedOperationException(String.format(
                "The Novatel Asset [%s] does not support performing a built-in-test(BIT)", m_Context.getName()));
    }
    
    @Override
    public Response onExecuteCommand(final Command capabilityCommand) throws CommandExecutionException
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            if (capabilityCommand instanceof GetPositionCommand)
            {
                if (m_Coordinates == null || m_Orientation == null)
                {
                    throw new CommandExecutionException("Position data is not known for the NovAtel Asset");
                }
                return new GetPositionResponse().withLocation(m_Coordinates).withOrientation(m_Orientation);
            }
            else
            {
                throw new CommandExecutionException(String.format("Asset [%s] does not support command [%s] data.", 
                        m_Context.getName(), capabilityCommand.toString()));
            }
        }
    }
    
    @Override
    public void handleDataString(final String message) throws ValidationFailedException
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            if (message.contains(NovatelConstants.NOVATEL_INSPVA_MESSAGE))
            {
                if (m_InsIterations >= m_HandleInsFreq)
                {
                    final NovatelInsMessage insMessage;
                    try
                    {
                        insMessage = m_Parser.parseInsMessage(message);
                    }
                    catch (final NovatelMessageException e)
                    {
                        handleNovatelMessageException(e);
                        Logging.log(LogService.LOG_ERROR, e, 
                                "The NovAtel asset was unable to handle an INSPVA message.");
                        return; //there is nothing to continue processing
                    }
                    handleStatusUpdate(getComponentStatus(
                            SummaryStatusEnum.GOOD, "INS data is being properly received and processed."));
                    if (m_SyncSystemTimeWithGps)
                    {
                        try
                        {
                            m_TimeChangeService.changeTime(insMessage.getUtcTime());
                        }
                        catch (final AssetException exception)
                        {
                            Logging.log(LogService.LOG_ERROR, exception, "Unable to change the system time.");
                        }
                    }
                    //observation
                    final Observation obs = new Observation();

                    //fill in data
                    obs.setAssetLocation(insMessage.getCoordinates());
                    obs.setAssetOrientation(insMessage.getOrientation());
                    obs.setObservedTimestamp(insMessage.getUtcTime());

                    m_Context.persistObservation(obs);
                    m_InsIterations = 1;

                    //since the values are validated now set the position data for the asset
                    m_Coordinates = insMessage.getCoordinates();
                    m_Orientation = insMessage.getOrientation();
                }
                else
                {
                    m_InsIterations++;
                }
            }
            else if (message.contains(NovatelConstants.NOVATEL_TIME_MESSAGE))
            {
                //Process the time message to update the GPS to UTC time offset
                try
                {
                    m_Parser.evaluateTimeMessage(message);
                }
                catch (final NovatelMessageException e)
                {
                    Logging.log(LogService.LOG_ERROR, e, "NovAtel asset is unable to handle a TIMEA message.");
                    handleNovatelMessageException(e);
                }
                //The status of the asset will be updated once a successful position data message has been handled.
                //Don't update now because there may be other pending actions needed for the plug-in to behave as 
                //intended.
            }
        }
    }

    @Override
    public synchronized void handleStatusUpdate(final ComponentStatus status)
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            final Status statusToPost = m_StatusService.getStatus(m_Context.getLastStatus(), status);
            if (statusToPost != null)
            {
                try 
                {
                    m_Context.setStatus(statusToPost);
                }
                catch (final ValidationFailedException e) 
                {
                    m_Context.setStatus(statusToPost.getSummaryStatus().getSummary(), 
                            statusToPost.getSummaryStatus().getDescription());
                }
            }
        }
    }

    @Override
    public void handleReadError(final SummaryStatusEnum summaryStatusEnum, final String statusDescription)
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            handleStatusUpdate(getComponentStatus(summaryStatusEnum, statusDescription));
        }
    }

    @Override
    public Set<Extension<?>> getExtensions()
    {
        return new HashSet<Extension<?>>();
    }

    /**
     * Create a component status representing this component.
     * @param status
     *      the status to set for this component
     * @param description
     *      descriptive string of the status
     * @return
     *      a complete component status
     */
    private ComponentStatus getComponentStatus(final SummaryStatusEnum status, 
            final String description)
    {
        final ComponentStatus insStatus = new ComponentStatus();
        final ComponentType insCompType = new ComponentType();
        insCompType.setDescription(COMPONENT_DESCRIPTION);
        insCompType.setType(COMPONENT_TYPE_IDENTIFIER);
        insStatus.setComponent(insCompType);
        final OperatingStatus insOpStatus = new OperatingStatus();
        insOpStatus.setSummary(status);
        insOpStatus.setDescription(description);
        insStatus.setStatus(insOpStatus);
        return insStatus;
    }
    
    /**
     * Analyze an {@link NovatelMessageException}'s {@link FormatProblem}.
     * @param exception
     *      the exception object to analyze the causation of
     */
    private void handleNovatelMessageException(final NovatelMessageException exception)
    {
        final FormatProblem problem = exception.getFormatProblem();
        switch (problem)
        {
            case INCOMPLETE_INS_MESSAGE:
                handleStatusUpdate(getComponentStatus(SummaryStatusEnum.BAD, exception.getMessage()));
                break;
            case INCOMPLETE_TIME_MESSAGE:
                handleStatusUpdate(getComponentStatus(SummaryStatusEnum.BAD, exception.getMessage()));
                break;
            case INS_STATUS_NOT_GOOD:
                handleStatusUpdate(getComponentStatus(
                        SummaryStatusEnum.DEGRADED, exception.getMessage()));
                break;
            case TIME_RELIABILITY:
                handleStatusUpdate(getComponentStatus(
                        SummaryStatusEnum.DEGRADED, exception.getMessage()));
                break;
            case UTC_OFFSET_UNKNOWN:
                handleStatusUpdate(getComponentStatus(SummaryStatusEnum.BAD, exception.getMessage()));
                break;
            case PARSING_ERROR:
                handleStatusUpdate(
                        getComponentStatus(SummaryStatusEnum.DEGRADED, exception.getMessage()));
                break;
            default:
                handleStatusUpdate(
                        getComponentStatus(SummaryStatusEnum.UNKNOWN, exception.getMessage()));
                break;
        }
    }
    
    /**
     * Method used to set the local properties needed by the asset.
     * 
     * @param attributes
     *       Novatel attributes class that contains configuration properties needed by the asset.
     */
    private void setProperties(final NovatelAssetAttributes attributes)
    {
        m_HandleInsFreq = attributes.handleMessageFreq();
        m_BaudRate = attributes.buadRate();
        m_PhysPort = attributes.physicalPort();
        m_SyncSystemTimeWithGps = attributes.syncSystemTimeWithGps();
        m_TimeServicePort = attributes.timeServicePort();
    }
}
