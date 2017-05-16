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
package edu.udayton.udri.asset.novatel.message;



/**
 * This interface describes the service which takes data strings which are expected to contain data 
 * from the SPAN-CPT unit and processes the information into a form which 
 * the {@link edu.udayton.udri.asset.novatel.NovatelAsset} can consume.
 * @author allenchl
 *
 */
public interface NovatelMessageParser
{
    /**
     * Parse an INSPVA data message.
     * 
     * @param data
     *      string which contains the INS message to parse
     * @return 
     *      a {@link NovatelInsMessage}
     * @throws NovatelMessageException
     *      if the message is malformed, a value is missing, the UTC offset is not known, 
     *      or if the INS/Time data received states that it is not valid
     */
    NovatelInsMessage parseInsMessage(String data) throws NovatelMessageException;

    /**
     * Evaluate a TIMEA data message. The 'data' time is represented in UTC, while the header contains the GPS time. The
     * OFFSET between the two time definitions will be stored internally within this parser and applied to
     * {@link NovatelInsMessage}s parsed by this service.
     * 
     * @param data
     *      string which contains the time data
     * @throws NovatelMessageException
     *      if the message is malformed, or a value is missing
     */
    void evaluateTimeMessage(String data) throws NovatelMessageException;

    /**
     * Check if the parser has the UTC time offset available.
     * 
     * @return <code> true </code> if the offset is known, <code> false </code> otherwise
     */
    boolean isOffsetKnown();
}
