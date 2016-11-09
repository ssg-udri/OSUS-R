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
package mil.dod.th.ose.test;

import java.lang.reflect.Field; // NOCHECKSTYLE: use of reflection, should be avoided as this class itself should 
import java.lang.reflect.Modifier; // NOCHECKSTYLE: be avoided as noted below

/**
 * Use of this class should be avoided if at all possible. An example of an acceptable use is when working with SWIG
 * generated code that creates static fields that are backed by native calls that can't otherwise be mocked.
 * 
 * Powermock itself cannot be used due to: https://code.google.com/p/powermock/issues/detail?id=324
 * 
 * @author dhumeniuk
 *
 */
public class FinalStaticMocker
{
    public static void mockIt(Class<?> clazz, String fieldName, Object newValue) throws IllegalStateException 
    {
        try
        {
            Field field = clazz.getDeclaredField(fieldName);
            
            field.setAccessible(true);
    
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    
            field.set(null, newValue);
        } 
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
    }
}
