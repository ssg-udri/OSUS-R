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

import mil.dod.th.core.types.ccomm.FlowControlEnum;
import mil.dod.th.core.types.ccomm.ParityEnum;
import mil.dod.th.core.types.ccomm.StopBitsEnum;

/**
 * This class contains constants related the configuration of the NovAtel plug-in.
 * @author allenchl
 *
 */
public final class NovatelConstants
{
    /**
     * The default data bits value for the NovAtel serial port.
     */
    public final static int DEFAULT_NOVATEL_DATA_BITS = 8;
    
    /**
     * The default parity value for the NovAtel serial port.
     */
    public final static ParityEnum DEFAULT_NOVATEL_PARITY = ParityEnum.NONE;
    
    /**
     * The default stop bits value for the NovAtel serial port.
     */
    public final static StopBitsEnum DEFAULT_NOVATEL_STOP_BITS = StopBitsEnum.ONE_STOP_BIT;
    
    /**
     * The default flow control for the NovAtel serial port.
     */
    public final static FlowControlEnum DEFAULT_NOVATEL_FLOW_CONTROL = FlowControlEnum.NONE;
    
    /**
     * String name of the NovAtel time log.
     */
    public final static String NOVATEL_TIME_MESSAGE = "TIME";
    
    /**
     * String that represents the name of NovAtel INSPVA log.
     */
    public final static String NOVATEL_INSPVA_MESSAGE = "INSPVA";
    
    /**
     * Hidden constructor to prevent instantiation.
     */
    private NovatelConstants()
    {
        //to prevent instantiation
    }
}
