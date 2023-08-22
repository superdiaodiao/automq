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

package kafka.log.s3;

import com.automq.elasticstream.client.api.FetchResult;
import com.automq.elasticstream.client.api.RecordBatch;
import com.automq.elasticstream.client.api.RecordBatchWithContext;
import kafka.log.s3.cache.ReadDataBlock;
import kafka.log.s3.cache.S3BlockCache;
import kafka.log.s3.model.RangeMetadata;
import kafka.log.s3.model.StreamMetadata;
import kafka.log.s3.objects.ObjectManager;
import kafka.log.s3.streams.StreamManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class S3StreamTest {
    Wal wal;
    S3BlockCache blockCache;
    StreamManager streamManager;
    ObjectManager objectManager;
    S3Stream stream;

    @BeforeEach
    public void setup() {
        wal = mock(Wal.class);
        blockCache = mock(S3BlockCache.class);
        streamManager = mock(StreamManager.class);
        objectManager = mock(ObjectManager.class);
        StreamMetadata metadata = new StreamMetadata();
        metadata.setStreamId(233);
        metadata.setEpoch(1);
        metadata.setStartOffset(100);
        metadata.setRanges(List.of(new RangeMetadata(1, 50, -1, 10)));
        stream = new S3Stream(metadata, wal, blockCache, streamManager, objectManager);
    }

    @Test
    public void testFetch() throws Throwable {
        when(objectManager.getObjects(eq(233L), eq(110L), eq(120L), eq(100))).thenReturn(List.of(123L, 124L));
        when(blockCache.read(eq(123L), eq(233L), eq(110L), eq(120L), eq(100)))
                .thenReturn(CompletableFuture.completedFuture(newReadDataBlock(110, 115, 10)));
        when(blockCache.read(eq(124L), eq(233L), eq(115L), eq(120L), eq(90)))
                .thenReturn(CompletableFuture.completedFuture(newReadDataBlock(115, 120, 10)));
        FetchResult rst = stream.fetch(110, 120, 100).get(1, TimeUnit.SECONDS);
        assertEquals(2, rst.recordBatchList().size());
        assertEquals(110, rst.recordBatchList().get(0).baseOffset());
        assertEquals(120, rst.recordBatchList().get(1).lastOffset());
    }

    ReadDataBlock newReadDataBlock(long start, long end, int size) {
        RecordBatch recordBatch = DefaultRecordBatch.of((int) (end - start), size);
        RecordBatchWithContext recordBatchWithContext = new DefaultRecordBatchWithContext(recordBatch, start);
        return new ReadDataBlock(List.of(recordBatchWithContext));
    }
}