package org.apache.jcs.engine.memory.mru;

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

import java.io.IOException;

import org.apache.jcs.engine.behavior.ICacheElement;
import org.apache.jcs.engine.memory.AbstractDoulbeLinkedListMemoryCache;
import org.apache.jcs.engine.memory.util.MemoryElementDescriptor;

/**
 * The most recently used items move to the front of the list and get spooled to disk if the cache
 * hub is configured to use a disk cache.
 */
public class MRUMemoryCache
    extends AbstractDoulbeLinkedListMemoryCache
{
    /** Don't change */
    private static final long serialVersionUID = 5013101678192336129L;

    /**
     * Adds the item to the front of the list. A put doesn't count as a usage.
     * <p>
     * It's not clear if the put operation sould be different. Perhaps this should remove the oldest
     * if full, and then put.
     * <p>
     * @param ce
     * @return MemoryElementDescriptor the new node
     * @exception IOException
     */
    protected MemoryElementDescriptor adjustListForUpdate( ICacheElement ce )
        throws IOException
    {
        return addFirst( ce );
    }

    /**
     * Makes the item the last in the list.
     * <p>
     * @param me
     */
    protected void adjustListForGet( MemoryElementDescriptor me )
    {
        list.makeLast( me );
    }
}
