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
package mil.dod.th.ose.utils;

import java.io.File;

/**
 * File utility class.
 * 
 * @author dhumeniuk
 *
 */
public final class FileUtils
{
    /**
     * Hide constructor to prevent instantiation.
     */
    private FileUtils()
    {
        
    }
    
    /**
     * Get the partition file given a file.  Recursively search up the file path until a partition is found.  Each file
     * exists in a partition on the system, find this partition.  For example, the file "C:\path\file.txt" would have a 
     * partition of "C:".
     * 
     * @param file
     *      file want to know the partition of it
     * @return
     *      file object representing a partition where the given file exists
     */
    public static File getPartition(final File file)
    {
        final File absoluteFile = file.getAbsoluteFile();
        if (absoluteFile.getTotalSpace() > 0L)
        {
            return absoluteFile;
        }
        return getPartition(absoluteFile.getParentFile());
    }
}
