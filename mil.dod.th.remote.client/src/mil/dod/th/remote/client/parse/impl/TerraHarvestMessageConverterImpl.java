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
package mil.dod.th.remote.client.parse.impl;

import java.util.HashMap;
import java.util.Map;

import aQute.bnd.annotation.component.Component;

import com.google.protobuf.InvalidProtocolBufferException;

import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.remote.client.RemoteMessage;
import mil.dod.th.remote.client.parse.MessageParser;
import mil.dod.th.remote.client.parse.TerraHarvestMessageConverter;

/**
 * Message converter implementation.
 * 
 * @author dlandoll
 */
@Component
public class TerraHarvestMessageConverterImpl implements TerraHarvestMessageConverter
{
    private static final Map<Namespace, MessageParser<?>> PARSER_MAP;
    static
    {
        PARSER_MAP = new HashMap<>();
        PARSER_MAP.put(Namespace.AssetDirectoryService, new AssetDirectoryServiceParser());
        PARSER_MAP.put(Namespace.Base, new BaseParser());
        PARSER_MAP.put(Namespace.DataStreamService, new DataStreamServiceParser());
        PARSER_MAP.put(Namespace.EventAdmin, new EventAdminParser());
    }

    @Override
    public RemoteMessage<?> convertMessage(final TerraHarvestMessage message) throws InvalidProtocolBufferException
    {
        final TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(message.getTerraHarvestPayload());
        final MessageParser<?> parser = PARSER_MAP.get(payload.getNamespace());
        if (parser == null)
        {
            return null;
        }

        return parser.parse(message, payload);
    }

    @Override
    public boolean isSupported(final Namespace namespace)
    {
        return PARSER_MAP.containsKey(namespace);
    }
}
