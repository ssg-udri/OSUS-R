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
package mil.dod.th.ose.gui.webapp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import mil.dod.th.core.asset.capability.AssetCapabilities;
import mil.dod.th.core.asset.capability.AudioCapabilities;
import mil.dod.th.core.asset.capability.CommandCapabilities;
import mil.dod.th.core.asset.capability.DigitalMediaCapabilities;
import mil.dod.th.core.capability.BaseCapabilities;
import mil.dod.th.core.ccomm.link.capability.LinkLayerCapabilities;
import mil.dod.th.core.ccomm.physical.capability.PhysicalLinkCapabilities;
import mil.dod.th.core.ccomm.physical.capability.SerialPort;
import mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities;
import mil.dod.th.core.controller.capability.ControllerCapabilities;
import mil.dod.th.core.controller.capability.PhysicalLink;
import mil.dod.th.core.types.DigitalMedia;
import mil.dod.th.core.types.OperatingSystemTypeEnum;
import mil.dod.th.core.types.SensingModality;
import mil.dod.th.core.types.SensingModalityEnum;
import mil.dod.th.core.types.VoltageVolts;
import mil.dod.th.core.types.ccomm.LinkLayerTypeEnum;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.core.types.command.CommandTypeEnum;
import mil.dod.th.core.types.spatial.DistanceMeters;

/**
 * A starting point to put common objects that represent fake data. Note that
 * these are fakes and not mocks
 * 
 * @author jgold
 * 
 */
public final class CapabilitiesFakeObjects {

    /**
     * Generate a fake AssetCapabilities object.  Should have 14 attributes defined.
     * @return the capabilities object
     */
    public AssetCapabilities genAssetCapabilities()
    {
        AssetCapabilities ac = new AssetCapabilities();
        genBaseCaps(ac);
        ac.withModalities(genSensingModalities(SensingModalityEnum.ACOUSTIC)).
            withMinRange(genDistanceMeasurement(0)).
            withMaxRange(genDistanceMeasurement(100)).
            withMinFov(0D).
            withMaxFov(0D).
            withNominalFov(50D).
            withCommandCapabilities(genCommandCaps()).
            withDigitalMediaCapabilities(genDigMediaCaps()).
            withAudioCapabilities(genAudioCaps());
        
        return ac;
    }
    
    /**
     * Generate a fake ControllerCapabilities object.
     * @return the capabilities object
     */
    public ControllerCapabilities genControllerCapabilities()
    {
        ControllerCapabilities cc = new ControllerCapabilities();
        genBaseCaps(cc);
        cc.withBatteryAmpHourReported(false).
            withCpuSpeed(1.20F).
            withLowPowerModeSupported(true).
            withMaxVoltage(new VoltageVolts().withValue(0.10F)).
            withMinVoltage(new VoltageVolts().withValue(.01F)).
            withNominalVoltage(new VoltageVolts().withValue(.08F)).
            withOperatingSystemsSupported(OperatingSystemTypeEnum.WIN_7).
            withBatteryAmpHourReported(false).
            withPhysicalLink(new HashSet<PhysicalLink>()).
            withPhysicalStorageSize(2).
            withSystemMemory(4).
            withVoltageReported(false);
        
        return cc;
    }
    
    /**
     * Set the base capability values
     * @param bc the object that extends BaseCapabilities
     */
    private void genBaseCaps(BaseCapabilities bc)
    {
        bc.withPrimaryImage(genImage()).
            withSecondaryImages(genImages(3)).
            withProductName("test").
            withDescription("test description").
            withManufacturer("manufacturer");
    }
    
    /**
     * Generate a DigitalMedia object with zero bytes with jpeg encoding.
     * @return DigitalMedia object
     */
    public DigitalMedia genImage()
    {
        DigitalMedia dm = new DigitalMedia(new byte[0], "image/jpeg");
        return dm;
    }
    
    /**
     * Generate a list of images. See genImage().
     * @param count the # of images to generate
     * @return the list
     */
    public List<DigitalMedia> genImages(int count)
    {
        List<DigitalMedia> list = new ArrayList<DigitalMedia>();
        for (int i = 0; i < count; i++)
        {
            list.add(genImage());
        }
        
        return list;
    }
    
    /**
     * Generate a fake list of SensingModality objects.
     * @param sme the value to set.
     * @return    the generated object.
     */
    public List<SensingModality> genSensingModalities(SensingModalityEnum sme)
    {
        SensingModality sm = new SensingModality(sme, "test");
        List<SensingModality> list = new ArrayList<SensingModality>();
        list.add(sm);
        return list;        
    }
    
    /**
     * Generate a fake DistanceMeasurement object with everything set to 0 except the value.
     * @param value the value
     * @return  the generated object.
     */
    public DistanceMeters genDistanceMeasurement(double value)
    {
        DistanceMeters dm =
                new DistanceMeters().
                    withError(0).
                    withPrecision(0).
                    withStdev(0).
                    withValue(value).
                    withVariance(0);
        return dm;
    }
    
    /**
     * Generate CommandCapabilities stub.
     * @return the CommandCapabilities object.
     */
    public CommandCapabilities genCommandCaps()
    {
        List<CommandTypeEnum> list = new ArrayList<CommandTypeEnum>();
        list.add(CommandTypeEnum.CAPTURE_IMAGE_COMMAND);
        list.add(CommandTypeEnum.GET_PAN_TILT_COMMAND);
        list.add(CommandTypeEnum.SET_PAN_TILT_COMMAND);
        CommandCapabilities cc = 
                new CommandCapabilities().
                    withSupportedCommands(list);
        return cc;
    }
    
    public DigitalMediaCapabilities genDigMediaCaps()
    {
        List<String> list = new ArrayList<String>();
        list.add("image/jpeg");
        list.add("image/png");
        DigitalMediaCapabilities dmc =
                new DigitalMediaCapabilities().
                    withEncodings(list);
        
        return dmc;
    }
    
    /**
     * Empty AudioCapabilities object.  Intentionally left
     * variables unset to produce the "object with no children set" scenario.
     * @return an AudioCapabilities object
     */
    public AudioCapabilities genAudioCaps()
    {
        AudioCapabilities ac = new AudioCapabilities();
        return ac;
    }
    
    /**
     * Generate a simple ControllerCaps instance.  Should have 10 attributes defined.
     * @return the ControllerCapabilties object.
     */
    public ControllerCapabilities genCtlrCaps()
    {
        ControllerCapabilities cc = new ControllerCapabilities();
        genBaseCaps(cc);
        cc.withOperatingSystemsSupported(OperatingSystemTypeEnum.MAC_OSX).
            withPhysicalLink(genPhysLinks());
        return cc;
    }
    
    /**
     * Generate a simple Physical Link for a serial port.
     * @return A list of 1 PhysicalLink
     */
    public List<PhysicalLink> genPhysLinks()
    {
        List<PhysicalLink> list = new ArrayList<PhysicalLink>();
        list.add(new PhysicalLink("test", PhysicalLinkTypeEnum.SERIAL_PORT));
        return list;
    }
    
    /**
     * Generate a simple PhysicalLinkCapabilities object.
     * Should have 5 attributes defined.
     * @return the PhysicalinkCapabilities object.
     */
    public PhysicalLinkCapabilities genPhysLinkCaps()
    {
        PhysicalLinkCapabilities plc = new PhysicalLinkCapabilities();
        genBaseCaps(plc);
        plc.withSerialPort(genSerialPort());
        
        return plc;
    }
    
    /**
     * Generate a simple SerialPort object.
     * @return The SerialPort object.
     */
    public SerialPort genSerialPort()
    {
        SerialPort sp = new SerialPort();
        return sp;
    }
    
    /**
     * Generate a simple LinkLayerCapabilities object with a few attributes set.
     * Should have 7 attributes defined.
     * @return the LinkLayerCapabilities object.
     */
    public LinkLayerCapabilities genLinkLayerCaps()
    {
        LinkLayerCapabilities lc = new LinkLayerCapabilities();
        genBaseCaps(lc);
        lc.withModality(LinkLayerTypeEnum.LINE_OF_SIGHT).
            withMtu(128).
            withPerformBITSupported(true).
            withSupportsAddressing(false);
        
        return lc;
    }
    
    /**
     * Generate a simple TransportLayerCapabilites object.  Should have 6 attributes defined.
     * @return the TransportLayerCapabilities object.
     */
    public TransportLayerCapabilities genTransportLayerCaps()
    {
        TransportLayerCapabilities tlc = new TransportLayerCapabilities();
        genBaseCaps(tlc);
        tlc.withLinkLayerModalitiesSupported(LinkLayerTypeEnum.CELLULAR, LinkLayerTypeEnum.SATCOM);
        return tlc;
    }
}
