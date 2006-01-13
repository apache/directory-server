/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.common.ldif ;


import java.io.Reader ; 
import java.io.IOException ;
import java.io.InputStream ;
import java.io.BufferedReader ;
import java.io.InputStreamReader ;

import java.util.Iterator ;


/**
 * Iterates through a set of LDIF's on a input channel.
 * 
 * @author <a href="mailto:dev@directory.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class LdifIterator implements Iterator
{
    /** whether or not debugging is enabled */
    private static final boolean DEBUG = false ;

    /** the prefetched LDIF record off of the stream */
    private String prefetched = null ;

    /** the monitor used */
    private LdifIteratorMonitor monitor = new LdifIteratorMonitorAdapter() ;

    /** input reader to read from */
    private BufferedReader in = null ;

    /** a temporary buffer */
    private StringBuffer buf = new StringBuffer() ;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates a new LdifIterator object on a stream.
     *
     * @param in the input stream to read from
     *
     * @throws IOException if we cannot wrap the stream with a reader
     */
    public LdifIterator( InputStream in ) throws IOException
    {
        this( new InputStreamReader( in ) ) ;

        if ( in == null )
        {
            throw new NullPointerException( "InputStream cannot be null!" );
        }
    }


    /**
     * Creates a new LdifIterator object on a reader.
     *
     * @param in the reader to read from
     *
     * @throws IOException if we cannot wrap the reader with a bufferd reader
     */
    public LdifIterator( Reader in ) throws IOException
    {
        if ( in == null )
        {
            throw new NullPointerException( "Reader cannot be null!" );
        }

        this.in = new BufferedReader( in ) ;
        debug( "<init>: -- opended file" ) ;
        prefetch() ;
        debug( "<init>: -- prefetch complete" ) ;
    }


    /**
     * Creates a new LdifIterator object with a monitor on a stream.
     *
     * @param in the input stream to read from
     * @param monitor monitor to log to
     *
     * @throws IOException if we cannot wrap the stream with a reader
     */
    public LdifIterator( InputStream in, LdifIteratorMonitor monitor ) throws IOException
    {
        this( new InputStreamReader( in ), monitor ) ;

        if ( in == null )
        {
            throw new NullPointerException( "InputStream cannot be null!" );
        }
    }


    /**
     * Creates a new LdifIterator object with a monitor on a reader.
     *
     * @param monitor monitor to log to
     * @param in the reader to read from
     *
     * @throws IOException if we cannot wrap the reader with a bufferd reader
     */
    public LdifIterator( Reader in, LdifIteratorMonitor monitor ) throws IOException
    {
        this( in ) ;

        if ( monitor != null )
        {
            this.monitor = monitor ;
        }

        if ( in == null )
        {
            throw new NullPointerException( "Reader cannot be null!" );
        }
    }


    // ------------------------------------------------------------------------
    // Iterator Methods
    // ------------------------------------------------------------------------


    /**
     * Tests to see if another LDIF is on the input channel.
     *
     * @return true if another LDIF is available false otherwise.
     */
    public boolean hasNext()
    {
        if ( DEBUG )
        {
            debug( "hasNext(): -- returning " + ( prefetched != null ) ) ;
        }


        return null != prefetched ;
    }


    /**
     * Gets the next LDIF on the channel.
     *
     * @return the next LDIF as a String.
     */
    public Object next()
    {
        String retVal = prefetched ;

        try
        {
            debug( "next(): -- called" ) ;
            prefetch() ;

            if ( DEBUG )
            { // Don't pay string + price if debug is off
                debug( "next(): -- returning ldif\n" + retVal ) ;
            }
        }
        catch ( IOException e )
        {
            error( "Premature termination of LDIF iterator due to "
                + "underlying stream error", e ) ;
        }


        return retVal ;
    }


    /**
     * Always throws UnsupportedOperationException!
     * @see java.util.Iterator#remove()
     */
    public void remove()
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * Handles an error/exception by logging it with a valid monitor or sending
     * it to the console if a monitor is not available.
     *
     * @param msg Message to log
     * @param throwable the offending throwable 
     */
    private void error( String msg, Throwable throwable )
    {
        monitor.fatalFailure( msg, throwable ) ;
    }


    /**
     * If debugging is enabled these log messages are sent to either the 
     * console or to the monitor if one is available.
     *
     * @param msg the debug message to log
     */
    private void debug( String msg )
    {
        monitor.infoAvailable( msg ) ;
    }


    /**
     * Prefetches a multi-line LDIF fron the input channel.
     *
     * @throws IOException if it cannot read from the input channel
     */
    private void prefetch() throws IOException
    {
        boolean insideLdif = false ;
        String line = null ;

        while ( null != ( line = in.readLine() ) )
        {
            debug( "readLine(): " + line ) ;
            line = filterComment( line ).trim() ;

            if ( insideLdif )
            {
                if ( line.equals( "" ) )
                {
                    break ;
                }


                debug( "prefetch(): -- appending last line to buffer" ) ;
                buf.append( line ).append( '\n' ) ;
            }
            else
            {
                if ( line.equals( "" ) )
                {
                    continue ;
                }


                insideLdif = true ;
                debug( "prefetch(): -- appending last line to buffer" ) ;
                buf.append( line ).append( '\n' ) ;
            }
        }


        if ( ( null == line ) && ( 0 == buf.length() ) )
        {
            debug( "prefetch(): -- line was null and buffer was empty" ) ;
            debug( "prefetch(): -- iterator has been consumed" ) ;
            prefetched = null ;
        }
        else
        {
            debug( "prefetch(): -- LDIF prefetched and set as next" ) ;
            prefetched = buf.toString() ;

            if ( DEBUG )
            {
                debug( "prefetch(): -- \n" + prefetched ) ;
            }
        }


        buf.setLength( 0 ) ;
        debug( "prefetch(): -- LDIF buffer cleared" ) ;
    }


    /**
     * Removes comments from a line as a filter before processing the lines.
     *
     * @param line the line to filter comments out of 
     *
     * @return the comment free line
     */
    public static String filterComment( String line )
    {
        int index = line.indexOf( '#' ) ;

        if ( -1 == index )
        {
            return line ;
        }
        else if ( index == 0 )
        {
            return "" ;
        }

        while ( -1 != index )
        {
            // If this is an escaped '#' then take new index from current
            // index + 1 and continue from start of loop.
            if ( ( ( index - 1 ) > 0 ) && ( '\\' == line.charAt( index - 1 ) ) )
            {
                if ( ( index + 1 ) < line.length() )
                {
                    index = line.indexOf( '#', index + 1 ) ;

                    continue ;
                }
                else
                { // This line has escaped '#' and no comment after it

                    return line ;
                }
            }

            return line.substring( 0, index ) ;
        }

        return line ;
    }
}
