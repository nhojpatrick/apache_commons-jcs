package org.apache.commons.jcs3.utils.threadpool;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.commons.jcs3.utils.props.PropertyLoader;
import org.junit.Test;

/**
 * Verify that the manager can create pools as intended by the default and
 * specified file names.
 */
public class ThreadPoolManagerUnitTest
{

    /**
     * Make sure it can load a default cache.ccf file
     */
    @Test
    public void testDefaultConfig()
    {
        final Properties props = PropertyLoader.loadProperties( "thread_pool.properties" );
        ThreadPoolManager.setProps( props );
        final ThreadPoolManager mgr = ThreadPoolManager.getInstance();
        assertNotNull( mgr );

        final ExecutorService pool = mgr.getExecutorService( "test1" );
        assertNotNull( pool );
    }

    /**
     * Gets a couple pools by name and then see if they are in the list.
     */
    @Test
    public void testGetPoolNames()
    {
        final ThreadPoolManager mgr = ThreadPoolManager.getInstance();
        assertNotNull( mgr );

        final String poolName1 = "testGetPoolNames1";
        mgr.getExecutorService( poolName1 );

        final String poolName2 = "testGetPoolNames2";
        mgr.getExecutorService( poolName2 );

        final Set<String> names = mgr.getPoolNames();
        assertTrue( "Should have name in list.", names.contains( poolName1 ) );
        assertTrue( "Should have name in list.", names.contains( poolName2 ) );
    }

    /**
     * Make sure it can load a certain configuration
     */
    @Test
    public void testSpecialConfig()
    {
        final Properties props = PropertyLoader.loadProperties( "thread_pool.properties" );
        ThreadPoolManager.setProps( props );
        final ThreadPoolManager mgr = ThreadPoolManager.getInstance();
        assertNotNull( mgr );

        final ExecutorService pool = mgr.getExecutorService( "aborttest" );
        assertNotNull( pool );
    }
}
