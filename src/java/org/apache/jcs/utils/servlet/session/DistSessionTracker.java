package org.apache.jcs.utils.servlet.session;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache JCS" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache JCS", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jcs.utils.servlet.session.DistSession;
import org.apache.jcs.utils.servlet.session.DistSessionGC;
import org.apache.jcs.utils.servlet.session.DistSessionPoolManager;
import org.apache.jcs.utils.servlet.session.ISessionConstants;
import org.apache.jcs.utils.servlet.session.MetaHttpSession;

/**
 * Manages sessions for the HttpRequestSessionWrapper
 *
 * @author Aaron Smuts
 * @created January 15, 2002
 * @version $Revision$ $Date$
 */

public class DistSessionTracker implements ISessionConstants
{
    private final static Log log =
        LogFactory.getLog( DistSessionTracker.class );

    /** Description of the Field */
    public final static String SESSION_COOKIE_NAME = "SESSION_ID";

    private static DistSessionPoolManager dsMgr = new DistSessionPoolManager( 200 );
    /** Description of the Field */
    public static DistSessionTracker instance;
    static int clients;

    /** Used to check and remove expired DistSession objects. */
    private static transient Set sessIdSet = Collections.synchronizedSet( new HashSet() );

    /** Used to asynchronously remove expired DistSession objects. */
    private static transient DistSessionGC gc = new DistSessionGC( sessIdSet );
    static
    {
        gc.setDaemon( true );
        gc.start();
    }


    /** Constructor for the DistSessionTracker object */
    private DistSessionTracker() { }


    /**
     * Gets the instance attribute of the DistSessionTracker class
     *
     * @return The instance value
     */
    public static DistSessionTracker getInstance()
    {
        if ( instance == null )
        {
            synchronized ( DistSessionTracker.class )
            {
                if ( instance == null )
                {
                    instance = new DistSessionTracker();
                }
            }
        }
        clients++;
        return instance;
    }


    /**
     * Gets the session attribute of the DistSessionTracker object
     *
     * @return The session value
     */
    public MetaHttpSession getSession( HttpServletRequest req )
    {
        return getSession( false, req, null );
    }


    /**
     * Gets the session attribute of the DistSessionTracker object
     *
     * @return The session value
     */
    public MetaHttpSession getSession( boolean create, HttpServletRequest req, HttpServletResponse res )
    {
        MetaHttpSession ses = getDistSession( req );
        return !ses.valid() && create ? createDistSession( req, res ) : ses;
    }


    /** Description of the Method */
    private MetaHttpSession createDistSession( HttpServletRequest req, HttpServletResponse res )
    {
        //not really a pool but a factory.  needs to be changed
        // theres no way to get it back int the pool
        //DistSession sess = ds.Mgr.getDistSession()
        String session_id = null;
        DistSession sess = new DistSession();
        try
        {
            // create a cookie that corrsponds to a session value in the svo
            sess.initNew();
//      sess.initNew(req.getServerName());
            session_id = sess.getId();
            Cookie c = new Cookie( SESSION_COOKIE_NAME, session_id );
            c.setPath( "/" );
            c.setMaxAge( -1 );
            res.addCookie( c );
            if ( log.isInfoEnabled() )
            {
                log.info( "created cookie session with session=" + sess );
            }
            if ( !sessIdSet.add( session_id ) )
            {
                log.error( "Session " + sess + " already exist when creating a session" );
            }
            gc.notifySession();
        }
        catch ( Exception e )
        {
            log.error( e );
            return new MetaHttpSession( session_id, null );
        }
        return new MetaHttpSession( session_id, sess );
    }
    // end createSession

    /**
     * Gets the distSession attribute of the DistSessionTracker object
     *
     * @return The distSession value
     */
    private MetaHttpSession getDistSession( HttpServletRequest req )
    {
        log.info( "in getSession" );
        DistSession sess = null;
        String session_id = getRequestedSessionId( req );

        if ( session_id == null )
        {
            log.info( "no cookie found" );
            return new MetaHttpSession( null, null );
        }
        sess = new DistSession();

        if ( !sess.init( session_id ) )
        {
            return new MetaHttpSession( session_id, null );
        }
        long idleTime = System.currentTimeMillis() - sess.getLastAccessedTime();
        int max = sess.getMaxInactiveInterval();
        if ( idleTime > max / 2 )
        {
            if ( idleTime < max )
            {
                sess.access();
            }
            else
            {
                sessIdSet.remove( session_id );
                sess.invalidate();
                return new MetaHttpSession( session_id, null );
            }
        }
        return new MetaHttpSession( session_id, sess );
    }
    // end  getSession

    /**
     * Gets the requested session id from the cookie. Todo, implement URL
     * session tracking.
     */
    public String getRequestedSessionId( HttpServletRequest req )
    {
        String session_id = null;
        Cookie[] cookies = req.getCookies();
        if ( cookies != null )
        {
            for ( int i = 0; i < cookies.length; i++ )
            {
                String tempName = cookies[i].getName();
                if ( tempName.equals( SESSION_COOKIE_NAME ) )
                {
                    session_id = cookies[i].getValue();
                }
            }
        }
        return session_id;
    }


    /** Gets the requestedSessionIdValid attribute of the DistSessionTracker object */
    public boolean isRequestedSessionIdValid( HttpServletRequest req )
    {
        DistSession sess = new DistSession();
        return sess.init( getRequestedSessionId( req ) );
    }

    /** Description of the Method */
    public void release()
    {
        clients--;
    }


    /**
     * Gets the stats attribute of the DistSessionTracker object
     *
     * @return The stats value
     */
    public String getStats()
    {
        String stats = "Number of clients: " + clients;
        return stats;
    }
}
// end class


