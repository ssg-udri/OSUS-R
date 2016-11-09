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
//    Javascript logging functions.
//==============================================================================
//Array contains all logged messages.
window.thJSLog = [];

/**
 * Method for logging information for later retrieval.
 * @param message
 *      information to be logged
 */
function logMessage(message)
{
    if (message != null)
    {
        window.thJSLog.push(Date().toString() + ": " + message.toString());
    }
}

/**
 * Method that returns an array contains all logged messages. Subsequent calls to this method will only return any 
 * messages logged since the previous call to this method.
 * @returns {Array}
 *       returns an array that contains all messages that have been logged since the previous call to this method
 */
function getLoggedMessages()
{
    logCopy = window.thJSLog.slice();
    window.thJSLog = [];
    return logCopy;
}
