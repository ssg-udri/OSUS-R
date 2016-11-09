#==============================================================================
# This software is part of the Open Standard for Unattended Sensors (OSUS)
# reference implementation (OSUS-R).
#
# To the extent possible under law, the author(s) have dedicated all copyright
# and related and neighboring rights to this software to the public domain
# worldwide. This software is distributed without any warranty.
#
# You should have received a copy of the CC0 Public Domain Dedication along
# with this software. If not, see
# <http://creativecommons.org/publicdomain/zero/1.0/>.
#==============================================================================
#
# DESCRIPTION:
# Script to stop the controller that is running in the background.  Will only
# try to kill if already started (by existence of pid file).
#
#==============================================================================

BASEDIR="/tmp"
PIDFILE="$BASEDIR/controller.pid"

if [ ! -f $PIDFILE ]
then
    echo "Controller not running"
    exit
fi

PID=`cat "$PIDFILE"`
kill $PID
rm "$PIDFILE"