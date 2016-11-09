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
// Source file for the TerminalIO module, contains implementation for C++
// functions actually defined, not directly using the GNU C library focused on
// terminal operations.
//
//==============================================================================

#include "TerminalIO.h"

#include <stdio.h>
#include <string.h>
#include <sys/ioctl.h>

/**
 * Must pass the correct size array to c_cc, assumes size is equal to macro NCCS.
 */
int tcSetAttr(int fd, tcflag_t c_iflag, tcflag_t c_oflag, tcflag_t c_cflag, tcflag_t c_lflag, cc_t c_cc[])
{
    struct termios desired;
    struct termios actual;

    // fill in with initial values
    if (tcgetattr(fd, &desired) == -1)
    {
        return TIO_INVALID_ARGS;
    }

    // copy parameters to structure for setting attributes
    desired.c_iflag = c_iflag;
    desired.c_oflag = c_oflag;
    desired.c_cflag = c_cflag;
    desired.c_lflag = c_lflag;
    memcpy(desired.c_cc, c_cc, sizeof(desired.c_cc));

    // flush any incoming data before setting values
    if (tcflush(fd, TCIFLUSH) == -1)
    {
        return TIO_INVALID_ARGS;
    }
    else if (tcsetattr(fd, TCSANOW, &desired) == -1)
    {
        return TIO_INVALID_ARGS;
    }
    else if (tcgetattr(fd, &actual) == -1)
    {
        return TIO_INVALID_ARGS;
    }

    // see if any values didn't stick
    if (desired.c_iflag != actual.c_iflag)
    {
        return TIO_INVALID_IFLAG;
    }
    else if (desired.c_oflag != actual.c_oflag)
    {
        return TIO_INVALID_OFLAG;
    }
    else if (desired.c_cflag != actual.c_cflag)
    {
        return TIO_INVALID_CFLAG;
    }
    else if (desired.c_lflag != actual.c_lflag)
    {
        return TIO_INVALID_LFLAG;
    }
    else if (memcmp(desired.c_cc, actual.c_cc, sizeof(desired.c_cc)))
    {
        return TIO_INVALID_CCS;
    }

    return TIO_SUCCESS;
}

int ioCtl(int fd, int cmd, int flag)
{
    return ioctl(fd, cmd, &flag);
}
