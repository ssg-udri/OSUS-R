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
package mil.dod.th.ose.remote.integration;

import mil.dod.th.ose.remote.integration.MessageMatchers.MessageMatcher;

/**
 * Used to assert a certain number of matches.
 * 
 * @author dhumeniuk
 * 
 */
public class MatchCount
{
    /**
     * Types of match counting possible with this class.
     */
    private enum MatchCountType
    {
        AT_LEAST, EXACTLY
    }

    private MatchCountType m_Type;
    private int m_DesiredTimes;
    private int m_ActualTimes = 0;

    private MatchCount(MatchCountType type, int times)
    {
        m_Type = type;
        m_DesiredTimes = times;
    }

    /// match creation helpers, don't call constructor directly, call from here
    public static MatchCount atLeastOnce()
    {
        return atLeast(1);
    }
    public static MatchCount atLeast(int n)
    {
        return new MatchCount(MatchCountType.AT_LEAST, n);
    }
    public static MatchCount once()
    {
        return times(1);
    }
    public static MatchCount times(int n)
    {
        return new MatchCount(MatchCountType.EXACTLY, n);
    }

    /**
     * Whether the actual count meets the desired count.
     */
    public boolean isSatisfied()
    {
        switch (m_Type)
        {
            case AT_LEAST:
                return m_ActualTimes >= m_DesiredTimes;
            case EXACTLY:
                return m_ActualTimes == m_DesiredTimes;
            default:
                throw new IllegalStateException(m_Type + " is not supported");    
        }
    }

    public int getActualMatches()
    {
        return m_ActualTimes;
    }

    /**
     * Will increment the count for actual matches.  Should only be called by the matcher, not be consumers.
     */
    public void foundMatch()
    {
        m_ActualTimes++;
    }

    /**
     * Get the failure message based on the parameters.
     */
    public String getFailureMessage(MessageMatcher matcher)
    {
        if (m_DesiredTimes == 1)
        {
            return "No match for message: " + matcher;
        }
        
        if (m_Type == MatchCountType.AT_LEAST)
        {
            return String.format("Expected at least %d messages, got %d message for %s", m_DesiredTimes,
                    m_ActualTimes, matcher);
        }
        else if (m_Type == MatchCountType.EXACTLY)
        {
            return String.format("Expected %d messages, got %d message for %s", m_DesiredTimes,
                    m_ActualTimes, matcher);
        }
        else
        {
            throw new IllegalStateException(m_Type + " is not supported");
        }
    }
}
