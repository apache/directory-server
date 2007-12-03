/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.changelog;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.cursor.ListCursor;
import org.apache.directory.shared.ldap.ldif.Entry;
import org.apache.directory.shared.ldap.util.DateUtils;

import javax.naming.NamingException;
import java.io.*;
import java.util.*;


/**
 * A change log store that keeps it's information in memory.
 *
 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MemoryChangeLogStore implements TaggableChangeLogStore
{
    private static final String REV_FILE = "revision";
    private static final String TAG_FILE = "tags";
    private static final String CHANGELOG_FILE = "changelog.dat";

    private long currentRevision;
    private Tag latest;
    private final Map<Long,Tag> tags = new HashMap<Long,Tag>( 100 );
    private final List<ChangeLogEvent> events = new ArrayList<ChangeLogEvent>();
    private File workingDirectory;


    public Tag tag( long revision ) throws NamingException
    {
        if ( tags.containsKey( revision ) )
        {
            return tags.get( revision );
        }

        latest = new Tag( revision, null );
        tags.put( revision, latest );
        return latest;
    }


    public Tag tag() throws NamingException
    {
        if ( latest != null && latest.getRevision() == currentRevision )
        {
            return latest;
        }

        latest = new Tag( currentRevision, null );
        tags.put( currentRevision, latest );
        return latest;
    }


    public Tag tag( String description ) throws NamingException
    {
        if ( latest != null && latest.getRevision() == currentRevision )
        {
            return latest;
        }

        latest = new Tag( currentRevision, description );
        tags.put( currentRevision, latest );
        return latest;
    }


    public void init( DirectoryService service ) throws NamingException
    {
        workingDirectory = service.getWorkingDirectory();
        loadRevision();
        loadTags();
        loadChangeLog();
    }


    private void loadRevision() throws NamingException
    {
        File revFile = new File( workingDirectory, REV_FILE );
        if ( revFile.exists() )
        {
            BufferedReader reader = null;
            try
            {
                reader = new BufferedReader( new FileReader( revFile ) );
                String line = reader.readLine();
                currentRevision = Long.valueOf( line );
            }
            catch ( IOException e )
            {
                throw new NamingException( "Failed to open stream to read from revfile: " + revFile.getAbsolutePath() );
            }
            finally
            {
                if ( reader != null )
                {
                    //noinspection EmptyCatchBlock
                    try
                    {
                        reader.close();
                    }
                    catch ( IOException e )
                    {
                    }
                }
            }
        }
    }


    private void saveRevision() throws NamingException
    {
        File revFile = new File( workingDirectory, REV_FILE );
        if ( revFile.exists() )
        {
            revFile.delete();
        }

        PrintWriter out = null;
        try
        {
            out = new PrintWriter( new FileWriter( revFile ) );
            out.println( currentRevision );
            out.flush();
        }
        catch ( IOException e )
        {
            throw new NamingException( "Failed to write out revision file." );
        }
        finally
        {
            if ( out != null )
            {
                out.close();
            }
        }
    }


    private void saveTags() throws NamingException
    {
        File tagFile = new File( workingDirectory, TAG_FILE );
        if ( tagFile.exists() )
        {
            tagFile.delete();
        }

        FileOutputStream out = null;
        try
        {
            out = new FileOutputStream( tagFile );

            Properties props = new Properties();
            for ( Tag tag : tags.values() )
            {
                String key = String.valueOf( tag.getRevision() );
                if ( tag.getDescription() == null )
                {
                    props.setProperty( key, "null" );
                }
                else
                {
                    props.setProperty( key, tag.getDescription() );
                }
            }

            props.store( out, null );
            out.flush();
        }
        catch ( IOException e )
        {
            throw new NamingException( "Failed to write out revision file." );
        }
        finally
        {
            if ( out != null )
            {
                //noinspection EmptyCatchBlock
                try
                {
                    out.close();
                }
                catch ( IOException e )
                {
                }
            }
        }
    }


    private void loadTags() throws NamingException
    {
        File revFile = new File( workingDirectory, REV_FILE );
        if ( revFile.exists() )
        {
            Properties props = new Properties();
            FileInputStream in = null;
            try
            {
                in = new FileInputStream( revFile );
                props.load( in );
                ArrayList<Long> revList = new ArrayList<Long>();
                for ( Object key : props.keySet() )
                {
                    revList.add( Long.valueOf( ( String ) key ) );
                }

                Collections.sort( revList );
                Tag tag = null;

                // @todo need some serious syncrhoization here on tags
                tags.clear();
                for ( Long lkey : revList )
                {
                    String rev = String.valueOf( lkey );
                    String desc = props.getProperty( rev );

                    if ( desc != null && desc.equals( "null" ) )
                    {
                        tag = new Tag( lkey, null );
                    }
                    else
                    {
                        tag = new Tag( lkey, desc );
                    }

                    tags.put( lkey, tag );
                }

                latest = tag;
            }
            catch ( IOException e )
            {
                throw new NamingException( "Failed to open stream to read from revfile: " + revFile.getAbsolutePath() );
            }
            finally
            {
                if ( in != null )
                {
                    //noinspection EmptyCatchBlock
                    try
                    {
                        in.close();
                    }
                    catch ( IOException e )
                    {
                    }
                }
            }
        }
    }


    private void loadChangeLog() throws NamingException
    {
        File file = new File( workingDirectory, CHANGELOG_FILE );
        if ( file.exists() )
        {
            ObjectInputStream in = null;

            try
            {
                in = new ObjectInputStream( new FileInputStream( file ) );
                ArrayList<ChangeLogEvent> changeLogEvents = new ArrayList<ChangeLogEvent>();

                while ( true )
                {
                    try
                    {
                        ChangeLogEvent event = ( ChangeLogEvent ) in.readObject();
                        changeLogEvents.add( event );
                    }
                    catch ( EOFException eofe )
                    {
                        break;
                    }
                }

                // @todo man o man we need some synchronization later after getting this to work
                this.events.clear();
                this.events.addAll( changeLogEvents );
            }
            catch ( Exception e )
            {
                NamingException ne = new NamingException( "Failed to open stream to read from changelog file: "
                        + file.getAbsolutePath() );
                ne.setRootCause( e );
                throw ne;
            }
            finally
            {
                if ( in != null )
                {
                    //noinspection EmptyCatchBlock
                    try
                    {
                        in.close();
                    }
                    catch ( IOException e )
                    {
                    }
                }
            }
        }
    }


    private void saveChangeLog() throws NamingException
    {
        File file = new File( workingDirectory, CHANGELOG_FILE );
        if ( file.exists() )
        {
            file.delete();
        }

        try
        {
            file.createNewFile();
        }
        catch ( IOException e )
        {
            NamingException ne = new NamingException( "Failed to create new file for changelog: "
                    + file.getAbsolutePath() );
            ne.setRootCause( e );
            throw ne;
        }

        ObjectOutputStream out = null;

        try
        {
            out = new ObjectOutputStream( new FileOutputStream( file ) );

            for ( ChangeLogEvent event : events )
            {
                out.writeObject( event );
            }

            out.flush();
        }
        catch ( Exception e )
        {
            NamingException ne = new NamingException( "Failed to open stream to write to changelog file: "
                    + file.getAbsolutePath() );
            ne.setRootCause( e );
            throw ne;
        }
        finally
        {
            if ( out != null )
            {
                //noinspection EmptyCatchBlock
                try
                {
                    out.close();
                }
                catch ( IOException e )
                {
                }
            }
        }
    }


    public void sync() throws NamingException
    {
        saveRevision();
        saveTags();
        saveChangeLog();
    }


    public void destroy() throws NamingException
    {
        saveRevision();
        saveTags();
        saveChangeLog();
    }


    public long getCurrentRevision()
    {
        return currentRevision;
    }


    public long log( LdapPrincipal principal, Entry forward, Entry reverse ) throws NamingException
    {
        currentRevision++;
        ChangeLogEvent event = new ChangeLogEvent( currentRevision, DateUtils.getGeneralizedTime(), 
                principal, forward, reverse );
        events.add( event );
        return currentRevision;
    }


    public ChangeLogEvent lookup( long revision ) throws NamingException
    {
        if ( revision < 0 )
        {
            throw new IllegalArgumentException( "revision must be greater than or equal to 0" );
        }

        if ( revision > getCurrentRevision() )
        {
            throw new IllegalArgumentException( "The revision must not be greater than the current revision" );
        }

        return events.get( ( int ) revision );
    }


    public Cursor<ChangeLogEvent> find() throws NamingException
    {
        return new ListCursor<ChangeLogEvent>( events );
    }


    public Cursor<ChangeLogEvent> findBefore( long revision ) throws NamingException
    {
        return new ListCursor<ChangeLogEvent>( events, ( int ) revision );
    }


    public Cursor<ChangeLogEvent> findAfter( long revision ) throws NamingException
    {
        return new ListCursor<ChangeLogEvent>( ( int ) revision, events );
    }


    public Cursor<ChangeLogEvent> find( long startRevision, long endRevision ) throws NamingException
    {
        return new ListCursor<ChangeLogEvent>( ( int ) startRevision, events, ( int ) ( endRevision + 1 ) );
    }


    public Tag getLatest()
    {
        return latest;
    }
}
