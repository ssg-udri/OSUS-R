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
package mil.dod.th.remote.client.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import com.google.common.base.Preconditions;

import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.remote.client.ChannelStateCallback;
import mil.dod.th.remote.client.MessageListenerCallback;
import mil.dod.th.remote.client.MessageListenerService;
import mil.dod.th.remote.client.RemoteMessage;
import mil.dod.th.remote.client.parse.TerraHarvestMessageConverter;

/**
 * Implementation of the {@link MessageListenerService}.
 * 
 * @author dlandoll
 */
@Component
public class MessageListenerServiceImpl implements MessageListenerService
{
    private static final Namespace ANY_NAMESPACE = null;
    private static final String REGISTER_NULL_CALLBACK_ERROR_MSG = "Message callback argument is required to register";

    /**
     * This map contains the message listener threads created for each channel that is added to the service by the
     * remote system ID.
     */
    private final Map<Integer, MessageListener> m_ListenerMap =
        Collections.synchronizedMap(new HashMap<Integer, MessageListener>());

    /**
     * This map contains a list of message listener callbacks that have been registered for each remote interface
     * namespace.
     */
    private final Map<Namespace, List<MessageListenerCallback<?>>> m_MessageCallbackMap =
        Collections.synchronizedMap(new HashMap<Namespace, List<MessageListenerCallback<?>>>());

    private TerraHarvestMessageConverter m_Converter;
    private Logging m_Logging;

    @Reference
    public void setLogging(final Logging logging)
    {
        m_Logging = logging;
    }

    @Reference
    public void setConverter(final TerraHarvestMessageConverter converter)
    {
        m_Converter = converter;
    }

    @Override
    public synchronized void addRemoteChannel(final int srcId, final InputStream input,
        final ChannelStateCallback callback) throws IllegalArgumentException, IllegalStateException
    {
        Preconditions.checkArgument(input != null, "Channel input stream argument is required");
        Preconditions.checkState(!m_ListenerMap.containsKey(srcId), "Channel already exists for source ID", srcId);

        final RemoteChannelInfo channelInfo = new RemoteChannelInfo(srcId, input, callback);
        final MessageListener listener = new MessageListener(channelInfo);
        m_ListenerMap.put(srcId, listener);

        final Thread thread = new Thread(listener);
        thread.setName("MessageListener-srcId-" + srcId);
        thread.start();
    }

    @Override
    public synchronized void removeRemoteChannel(final int srcId) throws IllegalArgumentException
    {
        final MessageListener listener = m_ListenerMap.remove(srcId);
        Preconditions.checkArgument(listener != null, "Channel for source ID does not exist", srcId);

        listener.shutdown();
    }

    @Override
    public synchronized void registerCallback(final MessageListenerCallback<?> callback) throws IllegalArgumentException
    {
        Preconditions.checkArgument(callback != null, REGISTER_NULL_CALLBACK_ERROR_MSG);

        List<MessageListenerCallback<?>> callbacks = m_MessageCallbackMap.get(ANY_NAMESPACE);
        if (callbacks == null)
        {
            callbacks = Collections.synchronizedList(new ArrayList<MessageListenerCallback<?>>());
            m_MessageCallbackMap.put(ANY_NAMESPACE, callbacks);
        }

        callbacks.add(callback);
    }

    @Override
    public synchronized void registerCallback(final Namespace type, final MessageListenerCallback<?> callback)
            throws IllegalArgumentException
    {
        Preconditions.checkArgument(m_Converter.isSupported(type), "Namespace is not supported", type.name());
        Preconditions.checkArgument(callback != null, REGISTER_NULL_CALLBACK_ERROR_MSG);

        List<MessageListenerCallback<?>> callbacks = m_MessageCallbackMap.get(type);
        if (callbacks == null)
        {
            callbacks = Collections.synchronizedList(new ArrayList<MessageListenerCallback<?>>());
            m_MessageCallbackMap.put(type, callbacks);
        }

        callbacks.add(callback);
    }

    @Override
    public synchronized void unregisterCallback(final MessageListenerCallback<?> callback)
            throws IllegalArgumentException
    {
        Preconditions.checkArgument(callback != null, "Message callback argument is required to unregister");

        List<MessageListenerCallback<?>> callbacks = m_MessageCallbackMap.get(ANY_NAMESPACE);
        if (callbacks != null)
        {
            tryRemoveCallback(callback, callbacks);
        }

        for (Namespace type : Namespace.values())
        {
            callbacks = m_MessageCallbackMap.get(type);
            if (callbacks != null)
            {
                tryRemoveCallback(callback, callbacks);
            }
        }
    }

    /**
     * Remove the callback if found within the list.
     * 
     * @param callback
     *      callback reference to remove
     * @param callbackList
     *      list to check for removal
     */
    private void tryRemoveCallback(final MessageListenerCallback<?> callback,
        final List<MessageListenerCallback<?>> callbackList)
    {
        final Iterator<MessageListenerCallback<?>> callbackIter = callbackList.iterator();
        while (callbackIter.hasNext())
        {
            final MessageListenerCallback<?> existingCallback = callbackIter.next();
            if (existingCallback.equals(callback))
            {
                callbackIter.remove();
            }
        }
    }

    /**
     * Execute callback(s) for the given message. This method is called from the listening thread and must be
     * synchronized to ensure thread safety for the message callback list.
     * 
     * @param message
     *      newly received remote message
     * @param messageCallbackList
     *      list of callbacks to receive the message
     */
    @SuppressWarnings("unchecked")
    private synchronized void executeCallbacks(@SuppressWarnings("rawtypes") final RemoteMessage message,
            final List<MessageListenerCallback<?>> messageCallbackList)
    {
        for (MessageListenerCallback<?> messageCallback : messageCallbackList)
        {
            try
            {
                messageCallback.handleMessage(message);
            }
            catch (final Exception ex)
            {
                m_Logging.error(ex, "Exception thrown during message callback");
            }
        }
    }

    /**
     * Message listening thread.
     */
    private class MessageListener implements Runnable
    {
        private final AtomicBoolean m_Running;
        private final RemoteChannelInfo m_RemoteChannelInfo;

        /**
         * Creates a new message listener for the given channel information.
         * 
         * @param channelInfo
         *      channel info
         */
        MessageListener(final RemoteChannelInfo channelInfo)
        {
            m_Running = new AtomicBoolean(false);
            m_RemoteChannelInfo = channelInfo;
        }

        @Override
        public void run()
        {
            final ChannelStateCallback channelCallback = m_RemoteChannelInfo.getCallback();

            m_Running.set(true);

            while (m_Running.get())
            {
                try
                {
                    // Wait for a new incoming message
                    final TerraHarvestMessage thmessage =
                        TerraHarvestMessage.parseDelimitedFrom(m_RemoteChannelInfo.getInStream());

                    @SuppressWarnings("rawtypes")
                    final RemoteMessage message = m_Converter.convertMessage(thmessage);
                    if (message == null)
                    {
                        m_Logging.debug("Ignoring unknown message received from system ID %d", thmessage.getSourceId());
                        continue;
                    }

                    final List<MessageListenerCallback<?>> messageCallbacksAny =
                        m_MessageCallbackMap.get(ANY_NAMESPACE);
                    final List<MessageListenerCallback<?>> messageCallbacksType =
                        m_MessageCallbackMap.get(message.getNamespace());

                    if (messageCallbacksAny == null && messageCallbacksType == null)
                    {
                        m_Logging.debug("Ignoring message with no callbacks registered for %s namespace",
                            message.getNamespace().name());
                        continue;
                    }

                    if (messageCallbacksType != null)
                    {
                        executeCallbacks(message, messageCallbacksType);
                    }

                    if (messageCallbacksAny != null)
                    {
                        executeCallbacks(message, messageCallbacksAny);
                    }
                }
                catch (final Exception ex)
                {
                    m_Logging.error(ex, "Exception while listening for remote message");
                    if (m_Running.get())
                    {
                        final int srcId = m_RemoteChannelInfo.getChannelId();
                        m_ListenerMap.remove(srcId);

                        if (channelCallback != null)
                        {
                            channelCallback.onChannelRemoved(srcId, ex);
                        }
                        m_Logging.debug("Exiting message listener thread due to exception for remote system 0x%08x", 
                                m_RemoteChannelInfo.getChannelId());
                        return;
                    }
                }
            }

            if (channelCallback != null)
            {
                channelCallback.onChannelRemoved(m_RemoteChannelInfo.getChannelId());
            }
            m_Logging.debug("Exiting message listener thread for remote system 0x%08x", 
                    m_RemoteChannelInfo.getChannelId());
        }

        /**
         * Stop the message listening thread.
         */
        public void shutdown()
        {
            m_Running.set(false);
            try
            {
                m_RemoteChannelInfo.getInStream().close();
            }
            catch (final Exception ex)
            { // NOCHECKSTYLE: Ignore exception, just making sure the channel stream is closed
            }
        }
    }
}
