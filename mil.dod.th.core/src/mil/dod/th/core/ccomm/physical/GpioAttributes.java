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
package mil.dod.th.core.ccomm.physical;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;
import mil.dod.th.core.ConfigurationConstants;
import mil.dod.th.core.factory.FactoryObject;

/**
 * Defines metadata of properties that are available to all {@link Gpio} instances. Retrieve properties using {@link 
 * Gpio#getConfig()}.
 * 
 * @author dhumeniuk
 *
 */
@OCD (description = ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION)
public interface GpioAttributes extends PhysicalLinkAttributes
{
    /**
     * Property key for {@link #ddrMask()}.
     */
    String CONFIG_PROP_DDR_MASK = FactoryObject.TH_PROP_PREFIX + ".ddr.mask";

    /**
     * Property key for {@link #dataMask()}.
     */
    String CONFIG_PROP_DATA_MASK = FactoryObject.TH_PROP_PREFIX + ".data.mask";

    /** 
     * Configuration property for the data directional register mask.
     * 
     * @return DDR mask
     */
    @AD(required = false, deflt = "0", id = CONFIG_PROP_DDR_MASK,
        description = "Data directional register mask")
    int ddrMask();
    
    /** 
     * Configuration property for the data mask, which bits will be updated when writing to the port.
     * 
     * @return data mask
     */
    @AD(required = false, deflt = "0", id = CONFIG_PROP_DATA_MASK,
        description = "Bit mask for which bits will be updated when writing to device")
    int dataMask();
}
