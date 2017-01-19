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
package mil.dod.th.ose.test.matchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.lang.reflect.InvocationTargetException;  // NOCHECKSTYLE: TD: illegal package, new warning, old code
import java.lang.reflect.Method; // NOCHECKSTYLE: ditto
import java.util.List;

import javax.xml.bind.annotation.XmlType;

/**
 * Contains utility functions for JAXB annotated classes.
 * 
 * @author Dave Humeniuk
 *
 */
public class JaxbUtil
{
    /**
     * Test if the two JAXB objects have the same content, but not necessarily the same internal state information that 
     * would be tested using the standard equals operation.  Also, this method gives a lot more details if two objects
     * are not equal, detailing where in the object the fields differ.
     */
    public static void assertEqualContent(Object o1, Object o2)
    {
        try
        {
            JaxbUtil.assertEqualContentRec(o1, o2);
        }
        catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException
                | SecurityException e)
        {
            // don't want to force all callers to catch exceptions, something must have gone wrong, make test fail
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("rawtypes")
    private static void assertEqualContentRec(Object o1, Object o2) throws InvocationTargetException, 
        IllegalAccessException, IllegalArgumentException, NoSuchMethodException, SecurityException
    {
        Class<?> clazz = o1.getClass();
        String className = clazz.getSimpleName();
        
        for (Method method : clazz.getMethods())
        {
            String methodName = method.getName();
            if (!methodName.startsWith("isSet") && (methodName.startsWith("get") || methodName.startsWith("is")))
            {
                //If method returns a primitive then verify that the field is actually set before invoking.
                if (method.getReturnType().isPrimitive() && !isSet(clazz, methodName, o1, o2))
                {
                    continue;
                }

                // valid field, compare
                Object result1 = method.invoke(o1);
                Object result2 = method.invoke(o2);
                
                if (result1 == null && result2 == null)
                {
                    // fields are equal continue
                }
                else
                {
                    // both not null, check to see if only one is though
                    assertThat("First argument value for " + methodName + " for class " + className,
                            result1, is(notNullValue()));
                    assertThat("Second argument value for " + methodName + " for class " + className,
                            result2, is(notNullValue()));
                    
                    if (result1 instanceof List)
                    {
                        // can't compare a list of objects, must iterate through each one and compare
                        List result1List = (List)result1;
                        List result2List = (List)result2;
                        
                        assertThat("List for " + methodName + " for class " + className + " are the same size", 
                                result1List.size(), is(result2List.size()));
                        
                        for (int i = 0; i < result1List.size(); i++)
                        {
                            Object value1 = result1List.get(0);
                            Object value2 = result2List.get(0);
                            
                            assertEqualContentRec(value1, value2);
                        }
                    }
                    else if (result1.getClass().getAnnotation(XmlType.class) == null)
                    {
                        // not a JAXB type so just compare normally
                        assertThat("Fields for " + methodName + " match for class " + className, result1, is(result2));
                    }
                    else
                    {
                        // is a JAXB type so use this method to exclude JDO fields
                        assertEqualContentRec(result1, result2);
                    }
                }
            }
        }
    }
    
    private static boolean isSet(final Class<?> clazz, final String methodName, final Object o1, final Object o2) 
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, 
            SecurityException
    {
        final String isSetMethodName = methodName.startsWith("get") ? 
                methodName.replaceFirst("get", "isSet") : methodName.replaceFirst("is", "isSet");
        Method isSetMethod = clazz.getMethod(isSetMethodName);

        final Boolean isSetO1 = (Boolean)isSetMethod.invoke(o1);
        final Boolean isSetO2 = (Boolean)isSetMethod.invoke(o2);
        
        //Verify that both fields are either set or not set.
        assertThat(isSetO1, is(isSetO2));
        
        return isSetO1;
    }
}
