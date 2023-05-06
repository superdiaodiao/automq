/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kafka.log.es;

import sdk.elastic.stream.api.Stream;
import sdk.elastic.stream.api.StreamClient;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ElasticLogStreamManager {
    private final Map<String, LazyStream> streamMap = new ConcurrentHashMap<>();
    private final StreamClient streamClient;


    public ElasticLogStreamManager(Map<String, Long> streams, StreamClient streamClient) {
        this.streamClient = streamClient;
        streams.forEach((name, streamId) -> {
            try {
                streamMap.put(name, new LazyStream(name, streamId, streamClient));
            } catch (Exception e) {
                // TODO: handle exception
                throw new RuntimeException(e);
            }
        });
    }

    public LazyStream getStream(String name) {
        return streamMap.computeIfAbsent(name, key -> {
            try {
                return new LazyStream(name, LazyStream.NOOP_STREAM_ID, streamClient);
            } catch (Exception e) {
                // TODO: handle exception
                return null;
            }
        });
    }

    public Map<String, Stream> streams() {
        return Collections.unmodifiableMap(streamMap);
    }
}