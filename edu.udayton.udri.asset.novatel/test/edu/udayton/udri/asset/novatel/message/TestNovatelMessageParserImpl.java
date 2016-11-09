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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import edu.udayton.udri.asset.novatel.impl.message.NovatelMessageParserImpl;

/**
 * Test class for the {@link NovatelMessageParserImpl}.
 * @author allenchl
 *
 */
public class TestNovatelMessageParserImpl
{
    private final String m_InsData = "#INSPVAA,COM1,0,48.5,FINESTEERING,1702,47.848,00000000,ddae,7053;"
            + "1702,221725.999755000,28.111270808,-80.624022245,-20.775880711,-63.661527253,39.651102732,"
            + "-0.000502600,-0.782552644,0.238876476,148.083596481,INS_SOLUTION_GOOD*b1967880";
    
    private final String m_TimeData = "#TIMEA,COM1,0,48.5,FINESTEERING,1702,47.848,00000000,ddae,7053;"
            + " VALID,-3.432839221e-09,6.603391298e-08,-16.00000000000,2013,11,7,0,15,23000,VALID*13cf9c8b";
    
    private NovatelMessageParser m_SUT;
    
    @Before
    public void setup()
    {
        m_SUT = new NovatelMessageParserImpl();
    }
    
    /**
     * Test that the offset must be set.
     */
    @Test
    public void testParseTimeMessageDefault() throws NovatelMessageException
    {
        //verify offset is not known
        assertThat(m_SUT.isOffsetKnown(), is(false));
        //an offset of 0, will throw an exception as 0 means it isn't known
        try
        {
            m_SUT.parseInsMessage(m_InsData);
            fail("Expected exception because a time message has not been parsed to set the time offset.");
        }
        catch (NovatelMessageException e)
        {
            //expected exception
        }
        
        //adjust the time offset
        m_SUT.evaluateTimeMessage(m_TimeData);
        
        //evaluate the same message
        NovatelInsMessage messageAfterOffset = m_SUT.parseInsMessage(m_InsData);
        //Verify the time. All data is static so this value should not change. 
        //The value is the time since Jan 6, 1980 + GPS weeks + GPS Seconds with the -16 second offset from the 
        //TimeData message.
        assertThat(messageAfterOffset.getUtcTime(), is(1345334431000L));
        assertThat(m_SUT.isOffsetKnown(), is(true));
    }
    
    /**
     * Test parsing an INS message.
     */
    @Test
    public void testParseInsMessage() throws NovatelMessageException
    {
        //set the time offset
        m_SUT.evaluateTimeMessage(m_TimeData);
        NovatelInsMessage message = m_SUT.parseInsMessage(m_InsData);
        
        assertThat(message.getCoordinates().getLongitude().getValue(), is(Double.parseDouble("-80.624022245")));
        assertThat(message.getCoordinates().getLatitude().getValue(), is(Double.parseDouble("28.111270808")));
        assertThat(message.getCoordinates().getAltitude().getValue(), is(Double.parseDouble("-20.775880711")));
        assertThat(message.getOrientation().getHeading().getValue(), is(Double.parseDouble("148.083596481")));
        assertThat(message.getOrientation().getElevation().getValue(), is(Double.parseDouble("0.238876476")));
        assertThat(message.getOrientation().getBank().getValue(), is(Double.parseDouble("-0.782552644")));
    }
    
    /**
     * Test parsing a INSPVA message that is missing the heading field.
     */
    @Test
    public void testParseInspvaMessageNotEnoughFields() throws NovatelMessageException
    {
        try
        {
            m_SUT.parseInsMessage(
                    "#INSPVAA,COM1,0,48.5,FINESTEERING,1702,47.848,00000000,ddae,7053;"
                        + "221725.999755000,28.111270808,-80.624022245,-20.775880711,-63.661527253,39.651102732,"
                        + "-0.000502600,-0.782552644,0.238876476,148.083596481,INS_SOLUTION_GOOD*b1967880");
            fail("Expected Exception because the heading field is missing");
        }
        catch (NovatelMessageException e)
        {
            //expected exception
        }
    }
    
    /**
     * Test parsing a INSPVA message that is missing the header.
     */
    @Test
    public void testParseInspvaMessageNoHeader() throws NovatelMessageException
    {
        try
        {
            m_SUT.parseInsMessage(
                    "1702,221725.999755000,28.111270808,-80.624022245,-20.775880711,-63.661527253,39.651102732,"
                       + "-0.000502600,-0.782552644,0.238876476,INS_SOLUTION_GOOD");
            fail("Expected Exception because the header is missing");
        }
        catch (NovatelMessageException e)
        {
            //expected exception
        }
    }
    
    /**
     * Test parsing a INSPVA message that is missing header fields.
     */
    @Test
    public void testParseInspvaMessageMissingHeaderFields() throws NovatelMessageException
    {
        try
        {
            m_SUT.parseInsMessage(";" +
                    "1702,221725.999755000,28.111270808,-80.624022245,-20.775880711,-63.661527253,39.651102732,"
                       + "-0.000502600,-0.782552644,0.238876476,INS_SOLUTION_GOOD");
            fail("Expected Exception because the header is missing");
        }
        catch (NovatelMessageException e)
        {
            //expected exception
        }
    }
    
    /**
     * Test parsing a time message sets the offset.
     */
    @Test
    public void testParseTimeMessage() throws NovatelMessageException
    {
        //the offset here is one second less than in the original message
        String newTimeData = "#TIMEA,COM1,0,48.5,FINESTEERING,1702,47.848,00000000,ddae,7053;"
                + " VALID,-3.432839221e-09,6.603391298e-08,-17.00000000000,2013,11,7,0,15,23000,VALID*13cf9c8b";
        
        //set the off-set, defaults to zero otherwise
        m_SUT.evaluateTimeMessage(m_TimeData);
        //should have an offset of -16
        NovatelInsMessage messageBeforeNewOffset = m_SUT.parseInsMessage(m_InsData);
        
        //adjust the time offset to -17
        m_SUT.evaluateTimeMessage(newTimeData);
        
        //evaluate the same message
        NovatelInsMessage messageAfterOffset = m_SUT.parseInsMessage(m_InsData);
        //new time should be less than before because off set is larger in the new time message
        assertThat(messageAfterOffset.getUtcTime(), is(lessThan(messageBeforeNewOffset.getUtcTime())));
    }
    
    /**
     * Test parsing a time message that is invalid as reported in the message.
     */
    @Test
    public void testParseTimeMessageInvalid()
    {
        try
        {
            m_SUT.evaluateTimeMessage(
                "#TIMEA,COM1,0,48.5,FINESTEERING,1702,47.848,00000000,ddae,7053;"
                + " INVALID,-3.432839221e-09,6.603391298e-08,-16.00000000000,2013,11,7,0,15");
            fail("Expected Exception because the message is invalid");
        }
        catch (NovatelMessageException e)
        {
            //expected exception
        }
    }
    
    /**
     * Test parsing a time message where the header is missing.
     */
    @Test
    public void testParseTimeMessageHeaderMissing()
    {
        try
        {
            m_SUT.evaluateTimeMessage(
                " VALID,-3.432839221e-09,6.603391298e-08,-16.00000000000,2013,11,7,0,15");
            fail("Expected Exception because the header is missing");
        }
        catch (NovatelMessageException e)
        {
            //expected exception
        }
    }
    
    /**
     * Test parsing a time message that is missing a value.
     */
    @Test
    public void testParseTimeMessageEmptyField()
    {
        try
        {
            m_SUT.evaluateTimeMessage(
                "#TIMEA,COM1,0,48.5,FINESTEERING,1702,47.848,00000000,ddae,7053;"
                + " VALID,-3.432839221e-09,6.603391298e-08,-16.00000000000,2013,11,7,0,15,''");
            fail("Expected Exception because a field is missing");
        }
        catch (NovatelMessageException e)
        {
            //expected exception
        }
    }
    
    /**
     * Test parsing a time message where the offset is missing.
     */
    @Test
    public void testParseTimeMessageEmptyUtcOffsetField()
    {
        try
        {
            m_SUT.evaluateTimeMessage(
                    "#TIMEA,COM1,0,48.5,FINESTEERING,1702,47.848,00000000,ddae,7053;"
                    + " VALID,-3.432839221e-09,6.603391298e-08,  ,2013,11,7,0,15,23000,VALID*13cf9c8b");
            fail("Expected Exception because a field is empty");
        }
        catch (NovatelMessageException e)
        {
            //expected exception
        }
    }
    
    /**
     * Test parsing a INSPVA message where the message is not good.
     */
    @Test
    public void testParseINSPVABad()
    {
        String data = "#INSPVAA,COM1,0,48.5,FINESTEERING,1702,47.848,00000000,ddae,7053;"
                + "1702,221725.999755000,28.111270808,-80.624022245,-20.775880711,-63.661527253,39.651102732,"
                + "-0.000502600,-0.782552644,0.238876476,148.083596481,INS_SOLUTION_BAD*b1967880";
        
        try
        {
            m_SUT.parseInsMessage(data);
            fail("Expected exception because the message is not good.");
        }
        catch (NovatelMessageException e)
        {
            //expected exception
        }
    }
}
