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
//
// DESCRIPTION:
// Source file for the FileOperations module, contains implementation for C++
// functions actually defined, not directly using the GNU C library.
//
//==============================================================================

#include "FileOperations.h"

#include <unistd.h>

/**
 * Try reading using the given file descriptor.  If timeoutMS is 0, then attempt
 * to read immediately.  See read(2) man pages for details (may block or not
 * depending on how file was opened).
 */
int readBuffer(int fd, sint8 buf[], int offset, size_t count, int timeoutMS)
{
    if (timeoutMS == 0)
    {
        return read(fd, buf+offset, count);
    }
    else
    {
        // fd_set can only handle a certain range of fds
        if ((fd < 0) || (fd >= __FD_SETSIZE))
        {
            return FO_INVALID_ARG;
        }

        fd_set readfds;
        struct timeval timeout;

        timeout.tv_sec = timeoutMS/1000;
        timeout.tv_usec = (timeoutMS%1000) * 1000;
        FD_ZERO(&readfds);
        FD_SET(fd, &readfds);

        if (select(fd+1, &readfds, NULL, NULL, &timeout) == -1)
        {
            // error, future improvement, read errno
            return FO_SELECT_FAILURE;
        }

        if (FD_ISSET(fd, &readfds))
        {
            return read(fd, buf+offset, count);
        }
        else
        {
            // timeout
            return FO_READ_TIMEOUT;
        }
    }
}

int writeBuffer(int fd, const sint8 buf[], int offset, size_t count)
{
    return write(fd, buf+offset, count);
}
