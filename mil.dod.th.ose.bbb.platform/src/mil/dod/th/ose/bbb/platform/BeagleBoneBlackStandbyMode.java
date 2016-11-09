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
package mil.dod.th.ose.bbb.platform;

/**
 * Enumeration that represents the available standby modes for the system.
 * 
 * @author cweisenborn
 */
public enum BeagleBoneBlackStandbyMode
{
    /**
     * This state offers significant power savings as everything in the system is put into a low-power state, except 
     * for memory, which is placed in self-refresh mode to retain its contents.
     */
    MEM("mem"),
    
    /**
     * This state offers minimal, though real, power savings, while providing a very low-latency transition back to a 
     * working system.
     */
    STANDBY("standby");
    
    private String m_Mode;
    
    /**
     * Private constructor that accepts the string representation of the enumeration.
     * 
     * @param mode
     *      String representation of the mode.
     */
    BeagleBoneBlackStandbyMode(final String mode)
    {
        m_Mode = mode;
    }
    
    @Override
    public String toString()
    {
        return m_Mode;
    }
}
