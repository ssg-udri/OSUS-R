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

import static org.junit.Assert.fail;

import java.util.List;

import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace;
import mil.dod.th.core.remote.proto.EventMessages.SendEventData;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.ose.remote.integration.MessageListener.MessageDetails;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ProtocolMessageEnum;

/**
 * Contains base interface for message matching and different implementations.
 * 
 * @author dhumeniuk
 */
public class MessageMatchers
{
    /**
     * Interface for matching remote messages.
     */
    public interface MessageMatcher
    {
        /**
         * Compares the message details for a single message with the matcher.
         * 
         * @param details
         *      details of a message
         * @return
         *      true if the details match up with the expected message, false if not
         * @throws InvalidProtocolBufferException
         *      if unable to parse the message details
         */
        boolean match(MessageDetails details) throws InvalidProtocolBufferException;
        
        /**
         * Get the number of matches with this matcher.
         * 
         * @return
         *      number times the matcher matches a message
         */
        int getActualMatches();
        
        /**
         * Assert if the match has occurred.  Must be called from the test runner thread, not a separate thread.
         * 
         * @param messages
         *      all the messages that were received, used for display purposes only, match determination has already 
         *      been made
         * @throws AssertionError
         *      if the assertion fails
         */
        void assertMatch(List<MessageDetails> messages) throws AssertionError;

        /**
         * Whether or not the matcher has matched yet.
         * 
         * @return
         *      true if previously matched, false if not yet
         */
        boolean hasMatched();
    }
    
    /**
     * Handle basic logic for matchers.
     * 
     * Currently assumes that one or more matches are expected.  Could be changed to support an exact amount in the 
     * future.
     * 
     * @author dhumeniuk
     *
     */
    public static abstract class AbstractMessageMatcher implements MessageMatcher
    {
        final private MatchCount m_MatchCount;
        private Throwable m_LastFailMessage;
        
        public AbstractMessageMatcher(MatchCount matchCount)
        {
            m_MatchCount = matchCount;
        }

        @Override
        public int getActualMatches()
        {
            return m_MatchCount.getActualMatches();
        }
        
        abstract boolean doMatch(MessageDetails details) throws InvalidProtocolBufferException;
        
        @Override
        public boolean match(MessageDetails details)
        {
            boolean match;
            try
            {
                match = doMatch(details);
            }
            catch (InvalidProtocolBufferException e)
            {
                m_LastFailMessage = e;
                return false;
            }
            if (match)
            {
                m_MatchCount.foundMatch();
            }
            return match;
        }
        
        @Override
        public boolean hasMatched()
        {
            if (m_LastFailMessage != null)
            {
                return false;
            }
            
            return m_MatchCount.isSatisfied();
        }
        
        @Override
        public void assertMatch(List<MessageDetails> messages) throws AssertionError
        {
            if (m_LastFailMessage != null)
            {
                throw new IllegalStateException(m_LastFailMessage);
            }
            
            if (!m_MatchCount.isSatisfied())
            {
                StringBuilder messageStr = new StringBuilder();
                for (MessageDetails message : messages)
                {
                    String detailMsg = "";
                    if (message.payload.getNamespace() == Namespace.EventAdmin 
                            && message.messageType == EventAdminMessageType.SendEvent) 
                    {
                        EventAdminNamespace eventMessage = (EventAdminNamespace)message.namespaceMessage;
                        SendEventData sendEventData;
                        try
                        {
                            sendEventData = SendEventData.parseFrom(eventMessage.getData());
                        }
                        catch (InvalidProtocolBufferException e)
                        {
                            throw new IllegalStateException(e);
                        }
                        detailMsg = ":" + sendEventData.getTopic();
                    }
                    else if (message.payload.getNamespace() == Namespace.Base
                            && message.messageType == BaseMessageType.GenericErrorResponse)
                    {
                        BaseNamespace baseMessage = (BaseNamespace)message.namespaceMessage;
                        GenericErrorResponseData errorMsg;
                        try
                        {
                            errorMsg = GenericErrorResponseData.parseFrom(baseMessage.getData());
                        }
                        catch (InvalidProtocolBufferException e)
                        {
                            throw new IllegalStateException(e);
                        }
                        detailMsg = ":" + errorMsg.getError() + ":" + errorMsg.getErrorDescription();
                    }
                        
                    messageStr.append(message.message.getMessageId() + ":" + message.payload.getNamespace() 
                            + ":" + message.messageType + detailMsg + "\n");
                }
                fail(m_MatchCount.getFailureMessage(this) + "\nGot: " + messageStr.toString());
            }
        }
    }
    
    /**
     * Matches messages with the same namespace and message type enums.
     */
    public static class BasicMessageMatcher extends AbstractMessageMatcher
    {
        final private Namespace m_Namespace;
        final private ProtocolMessageEnum m_Type;
        
        public BasicMessageMatcher(Namespace expectedNamespace, ProtocolMessageEnum expectedType)
        {
            this(expectedNamespace, expectedType, MatchCount.atLeastOnce());
        }

        public BasicMessageMatcher(Namespace expectedNamespace, ProtocolMessageEnum expectedType, MatchCount matchCount)
        {
            super(matchCount);
            m_Namespace = expectedNamespace;
            m_Type = expectedType;
        }

        @Override
        public boolean doMatch(MessageDetails details)
        {
            return details.payload.getNamespace() == m_Namespace && details.messageType == m_Type;
        }
        
        @Override
        public String toString()
        {
            return m_Namespace + ":" + m_Type;
        }
    }
    
    public static class EventMessageMatcher extends AbstractMessageMatcher
    {
        final private String m_Topic;
        
        public EventMessageMatcher(String topic)
        {
            this(topic, MatchCount.atLeastOnce());
        }
        
        public EventMessageMatcher(String topic, MatchCount matchCount)
        {
            super(matchCount);
            m_Topic = topic;
        }
        
        @Override
        public boolean doMatch(MessageDetails details) throws InvalidProtocolBufferException
        {
            if (details.payload.getNamespace() == Namespace.EventAdmin 
                    && details.messageType == EventAdminMessageType.SendEvent) 
            {
                EventAdminNamespace eventMessage = (EventAdminNamespace)details.namespaceMessage;
                SendEventData sendEventData = SendEventData.parseFrom(eventMessage.getData());
                return sendEventData.getTopic().equals(m_Topic);
            }
            
            return false;
        }
        
        @Override
        public String toString()
        {
            return "EventAdmin:SendEvent:" + m_Topic;
        }
    }
}
