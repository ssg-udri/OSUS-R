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
# Script to start the controller and run in the foreground.  Will only allow
# one controller to run at a time.  Tracks pid of process so it can be kill 
# later.
#
#==============================================================================

DEBUG="FALSE"
SUSPEND="n"
DEBUG_PORT=8000
# base options, modify max heap if necessary            
OPTIONS="-Xmx256m"

function printHelp
{
    echo "Usage: $0 -dx"
    echo "  -d  run as a daemon"
    echo "  -x  run in debug mode using port $DEBUG_PORT"
    echo "  -s  in debug mode, suspend start until debugger is attached"
}

cd `dirname $0`/..

while getopts ":dxs" opt; do
    case $opt in
        d)
            OPTIONS="$OPTIONS -Dgosh.args=--noi"
            ;;
        x)
            DEBUG="TRUE"
            ;;
        s)
            SUSPEND="y"
            ;;
        ?)
            echo "Invalid option: -$OPTARG" >&2
            printHelp           
            exit 1
            ;;
    esac
done

if [ "$DEBUG" = "TRUE" ]
then
    OPTIONS="$OPTIONS -Xdebug -Xrunjdwp:transport=dt_socket,address=$DEBUG_PORT,server=y,suspend=$SUSPEND"
fi

if [ `type -p java` ]
then
    JAVA_EXE="java"
else
    if [ -e $JAVA_HOME/bin/java ]
    then
        JAVA_EXE="$JAVA_HOME/bin/java"
    else
        echo "Cannot find java, add to PATH or define JAVA_HOME env variable."
        exit 1
    fi
fi

PROGRAM="$JAVA_EXE $OPTIONS -jar bin/felix.jar"
PNAME="java"

BASEDIR="/tmp"
PIDFILE="$BASEDIR/controller.pid"

if [ -e $PIDFILE ]
then
    echo "Controller already started"
    exit
fi

echo "Please be patient, startup may take a while depending on the hardware platform."

if [ "$1" = "-d" ]
then
    $PROGRAM &

    PID=$!
    if [ $(pgrep -x $PNAME | wc -w) -gt 0 ];
    then                                                                           
        echo "Controller started with PID="$PID
        echo $PID > "$PIDFILE"                                                 
    else
        echo "Controller did not start."
    fi
    
else
    $PROGRAM
fi
