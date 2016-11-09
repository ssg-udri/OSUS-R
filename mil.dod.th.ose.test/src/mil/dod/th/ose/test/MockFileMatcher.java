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

import java.io.File;
import java.util.Arrays;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.mockito.MockingDetails;
import org.mockito.Mockito;

/**
 * Check if a mocked file matches the given path.
 */
public class MockFileMatcher extends TypeSafeMatcher<File>
{
    /**
     * File to match.
     */
    private String m_Filename;
    
    public MockFileMatcher(String file)
    {
        m_Filename = file;
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendText("Mocked file named: " + m_Filename.toString());
    }

    @Override
    protected boolean matchesSafely(File item)
    {
        MockingDetails details = Mockito.mockingDetails(item);
        if (!details.isMock())
        {
            return false;
        }
        
        String[] parts1 = item.toString().split("\\\\|/");
        String[] parts2 = m_Filename.split("\\\\|/");
        return Arrays.equals(parts1, parts2);
    }

    /*
     * Factory method to create instance of matcher. 
     * The point of the factory method is to make test code read clearly.
     */
    @Factory
    public static Matcher<File> matches(String file)
    {
        return new MockFileMatcher(file);
    }
}
