package org.apache.commons.jcs3.utils.discovery;

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
import java.util.ArrayList;

import org.apache.commons.jcs3.log.Log;
import org.apache.commons.jcs3.log.LogManager;

/**
 * Used to periodically broadcast our location to other caches that might be listening.
 * @deprecated Functionality moved to UDPDiscoveryService
 */
@Deprecated
public class UDPDiscoverySenderThread
    implements Runnable
{
    /** The logger. */
    private static final Log log = LogManager.getLog( UDPDiscoverySenderThread.class );

    /**
     * details of the host, port, and service being advertised to listen for TCP socket connections
     */
    private final UDPDiscoveryAttributes attributes;

    /** List of known regions. */
    private ArrayList<String> cacheNames = new ArrayList<>();

    /**
     * @param cacheNames The cacheNames to set.
     */
    protected void setCacheNames( final ArrayList<String> cacheNames )
    {
        log.info( "Resetting cacheNames = [{0}]", cacheNames );
        this.cacheNames = cacheNames;
    }

    /**
     * @return Returns the cacheNames.
     */
    protected ArrayList<String> getCacheNames()
    {
        return cacheNames;
    }

    /**
     * Constructs the sender with the port to tell others to connect to.
     * <p>
     * On construction the sender will request that the other caches let it know their addresses.
     * @param attributes host, port, etc.
     * @param cacheNames List of strings of the names of the region participating.
     */
    public UDPDiscoverySenderThread( final UDPDiscoveryAttributes attributes, final ArrayList<String> cacheNames )
    {
        this.attributes = attributes;

        this.cacheNames = cacheNames;

        log.debug( "Creating sender thread for discoveryAddress = [{0}] and "
                + "discoveryPort = [{1}] myHostName = [{2}] and port = [{3}]",
                () -> attributes.getUdpDiscoveryAddr(),
                () -> attributes.getUdpDiscoveryPort(),
                () -> attributes.getServiceAddress(),
                () -> attributes.getServicePort() );

        try (UDPDiscoverySender sender = new UDPDiscoverySender(
                attributes.getUdpDiscoveryAddr(),
                attributes.getUdpDiscoveryPort(),
                attributes.getUdpTTL()))
        {
            // move this to the run method and determine how often to call it.
            sender.requestBroadcast();

            log.debug( "Sent a request broadcast to the group" );
        }
        catch ( final IOException e )
        {
            log.error( "Problem sending a Request Broadcast", e );
        }
    }

    /**
     * Send a message.
     */
    @Override
    public void run()
    {
        // create this connection each time.
        // more robust
        try (UDPDiscoverySender sender = new UDPDiscoverySender(
                attributes.getUdpDiscoveryAddr(),
                attributes.getUdpDiscoveryPort(),
                attributes.getUdpTTL()))
        {
            sender.passiveBroadcast( attributes.getServiceAddress(), attributes.getServicePort(), cacheNames );

            // todo we should consider sending a request broadcast every so
            // often.

            log.debug( "Called sender to issue a passive broadcast" );
        }
        catch ( final IOException e )
        {
            log.error( "Problem calling the UDP Discovery Sender [{0}:{1}]",
                    attributes.getUdpDiscoveryAddr(),
                    attributes.getUdpDiscoveryPort(), e );
        }
    }

    /**
     * Issues a remove broadcast to the others.
     */
    protected void shutdown()
    {
        // create this connection each time.
        // more robust
        try (UDPDiscoverySender sender = new UDPDiscoverySender(
                attributes.getUdpDiscoveryAddr(),
                attributes.getUdpDiscoveryPort(),
                attributes.getUdpTTL()))
        {
            sender.removeBroadcast( attributes.getServiceAddress(), attributes.getServicePort(), cacheNames );

            log.debug( "Called sender to issue a remove broadcast in shudown." );
        }
        catch ( final IOException e )
        {
            log.error( "Problem calling the UDP Discovery Sender", e );
        }
    }
}
