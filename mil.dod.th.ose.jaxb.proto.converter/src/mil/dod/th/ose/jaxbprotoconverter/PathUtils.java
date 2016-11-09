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
package mil.dod.th.ose.jaxbprotoconverter;

import java.io.File;
import java.nio.file.Path;

/**
 * Class that contains methods for working with file paths.
 * 
 * @author cweisenborn
 */
public final class PathUtils
{
    /**
     * Private constructor to avoid instantiation.
     */
    private PathUtils()
    {
        
    }
    
    /**
     * Gets the relative from the specified directory to the specified file.
     * 
     * @param file
     *      The file to create a relative path for.
     * @param baseDir
     *      The base directory the path to the file should be relative to.
     * @return
     *      The relative path from the base directory to the specified file.
     */
    public static Path getRelativePath(final File file, final File baseDir)
    {
        final Path fileDirPath = file.toPath();
        final Path baseDirPath = baseDir.toPath();
        return baseDirPath.relativize(fileDirPath);
    }
}
