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
// Header file for the File module, contains prototypes for C++ functions
// actually defined, not directly using the GNU C library.
//
//==============================================================================

#ifndef TERMINALIO_H_
#define TERMINALIO_H_

#include <termios.h>

enum
{
    TIO_SUCCESS = 0,
    TIO_INVALID_ARGS = -1,
    TIO_INVALID_IFLAG = -2,
    TIO_INVALID_OFLAG = -3,
    TIO_INVALID_CFLAG = -4,
    TIO_INVALID_LFLAG = -5,
    TIO_INVALID_CCS = -6
};

int tcSetAttr(int fd, tcflag_t c_iflag, tcflag_t c_oflag, tcflag_t c_cflag, tcflag_t c_lflag, cc_t c_cc[]);

int ioCtl(int fd, int cmd, int flag);

#endif /* TERMINALIO_H_ */
