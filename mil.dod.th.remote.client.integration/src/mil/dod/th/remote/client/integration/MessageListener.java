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
package mil.dod.th.remote.client.integration;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.BundleContext;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.protobuf.ProtocolMessageEnum;

import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.remote.client.MessageListenerCallback;
import mil.dod.th.remote.client.MessageListenerService;
import mil.dod.th.remote.client.RemoteMessage;

/**
 * Wraps the {@link MessageListenerService} to more easily wait for messages.
 * 
 * @author dhumeniuk
 *
 */
public class MessageListener
{
    private BundleContext m_Context;
    private MessageListenerService m_MsgListenerService;
    private BlockingQueue<RemoteMessage<?>> m_ReceivedMsgs = new ArrayBlockingQueue<>(100);
    private ExecutorService m_Executor = Executors.newCachedThreadPool();
    
    public MessageListener(BundleContext context)
    {
        m_Context = context;
        m_MsgListenerService = ServiceUtils.getService(m_Context, MessageListenerService.class);
                
        m_MsgListenerService.registerCallback(new MessageListenerCallback<BaseNamespace>()
            {
                @Override
                public void handleMessage(RemoteMessage<BaseNamespace> message)
                {
                    m_ReceivedMsgs.add(message);
                }
            });
    }

    /**
     * Wait for the message with the given namespace and data message type.
     */
    public <T> T waitForMessage(final Namespace namespace, final ProtocolMessageEnum dataMessageType, int timeout, 
            TimeUnit unit)
    {
        final List<RemoteMessage<?>> invalidMsgs = new ArrayList<>();
        
        Future<T> future = m_Executor.submit(new Callable<T>()
        {
            @SuppressWarnings("unchecked")
            @Override
            public T call() throws Exception
            {
                while (true)
                {
                    RemoteMessage<?> msg = m_ReceivedMsgs.poll(500, TimeUnit.MILLISECONDS);
                    if (msg == null || msg.getDataMessage() == null)
                    {
                        continue;
                    }
                    if (msg.getNamespace().equals(namespace) &&
                            msg.getDataMessageType().equals(dataMessageType))
                    {
                        return (T)msg.getDataMessage();
                    }
                    else
                    {
                        invalidMsgs.add(msg);
                    }
                }
            }
        });
        
        try
        {
            return future.get(timeout, unit);
        }
        catch (InterruptedException | ExecutionException e)
        {
            throw new IllegalStateException(e);
        }
        catch (TimeoutException e)
        {
            fail(String.format("Expecting %s:%s, but got: %s", namespace, dataMessageType,
                    Lists.transform(invalidMsgs, new Function<RemoteMessage<?>, String>()
                    {    
                        @Override
                        public String apply(RemoteMessage<?> msg)
                        {
                            return msg.getNamespace() + ":" + msg.getDataMessageType();
                        }
                    })));
            throw new IllegalStateException();
        }
    }
}
