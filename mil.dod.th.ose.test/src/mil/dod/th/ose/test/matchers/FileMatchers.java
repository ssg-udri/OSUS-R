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

import org.hamcrest.Matcher;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.io.File;
import java.io.IOException;

/**
 * Matchers for checking different attributes of a file. This matcher should not be used in unit tests unless a mocked
 * File object is used.
 */
public class FileMatchers
{
    /**
     * Test if actually a file using {@link File#isDirectory()}.
     */
    public static Matcher<File> isDirectory()
    {
        return new TypeSafeMatcher<File>()
        {
            /** File being checked. */
            private File fileTested;

            @Override
            public boolean matchesSafely(File item)
            {
                fileTested = item;
                return item.isDirectory();
            }

            @Override
            public void describeTo(Description description)
            {
                description.appendText(" that ");
                description.appendValue(fileTested);
                description.appendText("is a directory");
            }
        };
    }

    /**
     * Test if actually a file using {@link File#exists()}.
     */
    public static Matcher<File> exists()
    {
        return new TypeSafeMatcher<File>()
        {
            /** File being checked. */
            private File fileTested;

            @Override
            public boolean matchesSafely(File item)
            {
                fileTested = item;
                return item.exists();
            }

            @Override
            public void describeTo(Description description)
            {
                description.appendText(" that file ");
                description.appendValue(fileTested);
                description.appendText(" exists");
            }
        };
    }

    /**
     * Test if actually a file using {@link File#isFile()}.
     */
    public static Matcher<File> isFile()
    {
        return new TypeSafeMatcher<File>()
        {
            /** File being checked. */
            private File fileTested;

            @Override
            public boolean matchesSafely(File item)
            {
                fileTested = item;
                return item.isFile();
            }

            @Override
            public void describeTo(Description description)
            {
                description.appendText(" that ");
                description.appendValue(fileTested);
                description.appendText("is a file");
            }
        };
    }

    public static Matcher<File> readable()
    {
        return new TypeSafeMatcher<File>()
        {
            /** File being checked. */
            private File fileTested;

            @Override
            public boolean matchesSafely(File item)
            {
                fileTested = item;
                return item.canRead();
            }

            @Override
            public void describeTo(Description description)
            {
                description.appendText(" that file ");
                description.appendValue(fileTested);
                description.appendText("is readable");
            }
        };
    }

    public static Matcher<File> writable()
    {
        return new TypeSafeMatcher<File>()
        {
            /** File being checked. */
            private File fileTested;

            @Override
            public boolean matchesSafely(File item)
            {
                fileTested = item;
                return item.canWrite();
            }

            @Override
            public void describeTo(Description description)
            {
                description.appendText(" that file ");
                description.appendValue(fileTested);
                description.appendText("is writable");
            }
        };
    }

    public static Matcher<File> sized(final Matcher<Long> size)
    {
        return new TypeSafeMatcher<File>()
        {
            /** File being checked. */
            private File fileTested;
            /** Size of file. */
            private long length;

            @Override
            public boolean matchesSafely(File item)
            {
                fileTested = item;
                length = item.length();
                return size.matches(length);
            }

            @Override
            public void describeTo(Description description)
            {
                description.appendText(" that file ");
                description.appendValue(fileTested);
                description.appendText(" is sized ");
                description.appendDescriptionOf(size);
                description.appendText(", not " + length);
            }
        };
    }

    public static Matcher<File> named(final Matcher<String> name)
    {
        return new TypeSafeMatcher<File>()
        {
            private File fileTested;

            @Override
            public boolean matchesSafely(File item)
            {
                fileTested = item;
                return name.matches(item.getName());
            }

            @Override
            public void describeTo(Description description)
            {
                description.appendText(" that file ");
                description.appendValue(fileTested);
                description.appendText(" is named");
                description.appendDescriptionOf(name);
                description.appendText(" not ");
                description.appendValue(fileTested.getName());
            }
        };
    }

    public static Matcher<File> withCanonicalPath(final Matcher<String> path)
    {
        return new TypeSafeMatcher<File>()
        {
            @Override
            public boolean matchesSafely(File item)
            {
                try
                {
                    return path.matches(item.getCanonicalPath());
                }
                catch (IOException e)
                {
                    return false;
                }
            }

            @Override
            public void describeTo(Description description)
            {
                description.appendText("with canonical path '");
                description.appendDescriptionOf(path);
                description.appendText("'");
            }
        };
    }

    public static Matcher<File> withAbsolutePath(final Matcher<String> path)
    {
        return new TypeSafeMatcher<File>()
        {
            @Override
            public boolean matchesSafely(File item)
            {
                return path.matches(item.getAbsolutePath());
            }

            @Override
            public void describeTo(Description description)
            {
                description.appendText("with absolute path '");
                description.appendDescriptionOf(path);
                description.appendText("'");
            }
        };
    }
}
