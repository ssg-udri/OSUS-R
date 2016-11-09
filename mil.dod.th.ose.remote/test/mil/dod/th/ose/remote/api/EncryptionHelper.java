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
package mil.dod.th.ose.remote.api;

/**
 * Helper class that contains methods to help with testing classes that use encryption.
 * 
 * @author cweisenborn
 */
public class EncryptionHelper 
{
    /**
     * Return length many bytes of the passed in byte array as a hex string.
     * 
     * @param data the bytes to be converted.
     * @param length the number of bytes in the data block to be converted.
     * @return a hex representation of length bytes of data.
     */
    public static String toHex(final byte[] data, final int length)
    {
        final StringBuffer buf = new StringBuffer();
        final String digits = "0123456789abcdef";
        for (int i = 0; i != length; i++)
        {
            final int variName = data[i] & 0xff;//NOCHECKSTYLE '0xff'
            
            buf.append(digits.charAt(variName >> 4));//NOCHECKSTYLE '4'
            buf.append(digits.charAt(variName & 0xf));//NOCHECKSTYLE '0xf'
        }
        
        return buf.toString();
    }
    
    /**
     * Return the passed in byte array as a hex string.
     * 
     * @param data the bytes to be converted.
     * @return a hex representation of data.
     */
    public static String toHex(final byte[] data)
    {
        return toHex(data, data.length);
    }
}
