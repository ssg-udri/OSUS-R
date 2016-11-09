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
// Header file for the FileOperations module, contains prototypes for C++
// functions actually defined, not directly using the GNU C library.
//
//==============================================================================

#ifndef FILE_H_
#define FILE_H_

#include <sys/types.h>
#include "Types.h"

enum
{
    FO_SUCCESS = 0,
    FO_READ_FAILURE = -1,
    FO_SELECT_FAILURE = -2,
    FO_READ_TIMEOUT = -3,
    FO_INVALID_ARG = -4
};

int readBuffer(int fd, sint8 buf[], int offset, size_t count, int timeoutMS);
int writeBuffer(int fd, const sint8 buf[], int offset, size_t count);

#endif /* FILE_H_ */
