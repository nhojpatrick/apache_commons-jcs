package org.apache.commons.jcs3.engine.memory.fifo;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.apache.commons.jcs3.engine.CacheElement;
import org.apache.commons.jcs3.engine.CompositeCacheAttributes;
import org.apache.commons.jcs3.engine.ElementAttributes;
import org.apache.commons.jcs3.engine.behavior.ICompositeCacheAttributes;
import org.apache.commons.jcs3.engine.control.CompositeCache;
import org.junit.Test;

/** Unit tests for the fifo implementation. */
public class FIFOMemoryCacheUnitTest
{
    /**
     * Verify that the oldest inserted item is removed
     * <p>
     * @throws IOException
     */
    @Test
    public void testExpirationPolicy_doubleOver()
        throws IOException
    {
        // SETUP
        final int maxObjects = 10;
        final String cacheName = "testExpirationPolicy_oneExtra";

        final ICompositeCacheAttributes attributes = new CompositeCacheAttributes();
        attributes.setCacheName(cacheName);
        attributes.setMaxObjects( maxObjects );
        attributes.setSpoolChunkSize( 1 );

        final FIFOMemoryCache<String, String> cache = new FIFOMemoryCache<>();
        cache.initialize( new CompositeCache<>( attributes, new ElementAttributes() ) );

        // DO WORK
        for ( int i = 0; i < maxObjects * 2; i++ )
        {
            final CacheElement<String, String> element = new CacheElement<>( cacheName, "key" + i, "value" + i );
            cache.update( element );
        }

        // VERIFY
        assertEquals( "Should have max elements", maxObjects, cache.getSize() );
        for ( int i = maxObjects * 2 - 1; i > maxObjects; i-- )
        {
            assertNotNull( "Should have elemnt " + i, cache.get( "key" + i ) );
        }
    }

    /**
     * Verify that the oldest inserted item is removed
     * <p>
     * @throws IOException
     */
    @Test
    public void testExpirationPolicy_oneExtra()
        throws IOException
    {
        // SETUP
        final int maxObjects = 10;
        final String cacheName = "testExpirationPolicy_oneExtra";

        final ICompositeCacheAttributes attributes = new CompositeCacheAttributes();
        attributes.setCacheName(cacheName);
        attributes.setMaxObjects( maxObjects );
        attributes.setSpoolChunkSize( 1 );

        final FIFOMemoryCache<String, String> cache = new FIFOMemoryCache<>();
        cache.initialize( new CompositeCache<>( attributes, new ElementAttributes() ) );

        for ( int i = 0; i < maxObjects; i++ )
        {
            final CacheElement<String, String> element = new CacheElement<>( cacheName, "key" + i, "value" + i );
            cache.update( element );
        }

        final CacheElement<String, String> oneMoreElement = new CacheElement<>( cacheName, "onemore", "onemore" );

        // DO WORK
        cache.update( oneMoreElement );

        // VERIFY
        assertEquals( "Should have max elements", maxObjects, cache.getSize() );
        System.out.println(cache.getKeySet());
        for ( int i = maxObjects - 1; i > 1; i-- )
        {
            assertNotNull( "Should have element " + i, cache.get( "key" + i ) );
        }
        assertNotNull( "Should have oneMoreElement", cache.get( "onemore" ) );
    }
}
