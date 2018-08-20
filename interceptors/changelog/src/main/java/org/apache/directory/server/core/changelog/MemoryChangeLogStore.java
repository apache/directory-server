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


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.ListCursor;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.util.DateUtils;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.changelog.ChangeLogEvent;
import org.apache.directory.server.core.api.changelog.ChangeLogEventSerializer;
import org.apache.directory.server.core.api.changelog.Tag;
import org.apache.directory.server.core.api.changelog.TaggableChangeLogStore;
import org.apache.directory.server.i18n.I18n;


/**
 * A change log store that keeps it's information in memory.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MemoryChangeLogStore implements TaggableChangeLogStore
{

    private static final String REV_FILE = "revision";
    private static final String TAG_FILE = "tags";
    private static final String CHANGELOG_FILE = "changelog.dat";

    /** An incremental number giving the current revision */
    private long currentRevision;

    /** The latest tag */
    private Tag latest;

    /** A Map of tags and revisions */
    private final Map<Long, Tag> tags = new HashMap<>( 100 );

    private final List<ChangeLogEvent> events = new ArrayList<>();
    private File workingDirectory;

    /** The DirectoryService */
    private DirectoryService directoryService;


    /**
     * {@inheritDoc}
     */
    @Override
    public Tag tag( long revision )
    {
        if ( tags.containsKey( revision ) )
        {
            return tags.get( revision );
        }

        latest = new Tag( revision, null );
        tags.put( revision, latest );
        
        return latest;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Tag tag()
    {
        if ( ( latest != null ) && ( latest.getRevision() == currentRevision ) )
        {
            return latest;
        }

        latest = new Tag( currentRevision, null );
        tags.put( currentRevision, latest );
        return latest;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Tag tag( String description )
    {
        if ( ( latest != null ) && ( latest.getRevision() == currentRevision ) )
        {
            return latest;
        }

        latest = new Tag( currentRevision, description );
        tags.put( currentRevision, latest );
        return latest;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void init( DirectoryService service ) throws LdapException
    {
        workingDirectory = service.getInstanceLayout().getLogDirectory();
        this.directoryService = service;
        
        try
        {
            loadRevision();
            loadTags();
            loadChangeLog();
        }
        catch ( IOException ioe )
        {
            throw new LdapException( ioe.getMessage(), ioe );
        }
    }


    // This will suppress PMD.EmptyCatchBlock warnings in this method
    private void loadRevision() throws IOException
    {
        File revFile = new File( workingDirectory, REV_FILE );

        if ( revFile.exists() )
        {
            try ( BufferedReader reader = new BufferedReader( new FileReader( revFile ) ) )
            {
                String line = reader.readLine();
                currentRevision = Long.valueOf( line );
            }
        }
    }


    private void saveRevision() throws IOException
    {
        File revFile = new File( workingDirectory, REV_FILE );

        if ( revFile.exists() && !revFile.delete() )
        {
            throw new IOException( I18n.err( I18n.ERR_726_FILE_UNDELETABLE, revFile.getAbsolutePath() ) );
        }

        try ( PrintWriter out = new PrintWriter( new FileWriter( revFile ) ) )
        {
            out.println( currentRevision );
            out.flush();
        }
    }


    // This will suppress PMD.EmptyCatchBlock warnings in this method
    private void saveTags() throws IOException
    {
        File tagFile = new File( workingDirectory, TAG_FILE );

        if ( tagFile.exists() && !tagFile.delete() )
        {
            throw new IOException( I18n.err( I18n.ERR_726_FILE_UNDELETABLE, tagFile.getAbsolutePath() ) );
        }

        OutputStream out = null;

        try
        {
            out = Files.newOutputStream( tagFile.toPath() );

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
        finally
        {
            if ( out != null )
            {
                out.close();
            }
        }
    }


    // This will suppress PMD.EmptyCatchBlock warnings in this method
    private void loadTags() throws IOException
    {
        File revFile = new File( workingDirectory, REV_FILE );

        if ( revFile.exists() )
        {
            Properties props = new Properties();
            InputStream in = null;

            try
            {
                in = Files.newInputStream( revFile.toPath() );
                props.load( in );
                ArrayList<Long> revList = new ArrayList<>();

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

                    if ( ( desc != null ) && desc.equals( "null" ) )
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
            finally
            {
                if ( in != null )
                {
                    in.close();
                }
            }
        }
    }


    // This will suppress PMD.EmptyCatchBlock warnings in this method
    private void loadChangeLog() throws IOException
    {
        File file = new File( workingDirectory, CHANGELOG_FILE );

        if ( file.exists() )
        {
            try ( ObjectInputStream in = new ObjectInputStream( Files.newInputStream( file.toPath() ) ) )
            {
                int size = in.readInt();

                ArrayList<ChangeLogEvent> changeLogEvents = new ArrayList<>( size );

                for ( int i = 0; i < size; i++ )
                {
                    ChangeLogEvent event = ChangeLogEventSerializer.deserialize( directoryService.getSchemaManager(),
                        in );
                    event.getCommitterPrincipal().setSchemaManager( directoryService.getSchemaManager() );
                    changeLogEvents.add( event );
                }

                // @todo man o man we need some synchronization later after getting this to work
                this.events.clear();
                this.events.addAll( changeLogEvents );
            }
        }
    }


    // This will suppress PMD.EmptyCatchBlock warnings in this method
    private void saveChangeLog() throws IOException
    {
        File file = new File( workingDirectory, CHANGELOG_FILE );

        if ( file.exists() && !file.delete() )
        {
            throw new IOException( I18n.err( I18n.ERR_726_FILE_UNDELETABLE, file.getAbsolutePath() ) );
        }

        file.createNewFile();

        try ( ObjectOutputStream out = new ObjectOutputStream( Files.newOutputStream( file.toPath() ) ) )
        {
            out.writeInt( events.size() );

            for ( ChangeLogEvent event : events )
            {
                ChangeLogEventSerializer.serialize( event, out );
            }

            out.flush();
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void sync() throws LdapException
    {
        try
        {
            saveRevision();
            saveTags();
            saveChangeLog();
        }
        catch ( IOException ioe )
        {
            throw new LdapException( ioe.getMessage(), ioe );
        }
    }


    /**
     * Save logs, tags and revision on disk, and clean everything in memory
     */
    @Override
    public void destroy() throws LdapException
    {
        try
        {
            saveRevision();
            saveTags();
            saveChangeLog();
        }
        catch ( IOException ioe )
        {
            throw new LdapException( ioe.getMessage(), ioe );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long getCurrentRevision()
    {
        return currentRevision;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeLogEvent log( LdapPrincipal principal, LdifEntry forward, LdifEntry reverse )
    {
        currentRevision++;
        ChangeLogEvent event = new ChangeLogEvent( currentRevision, DateUtils.getGeneralizedTime(),
            principal, forward, reverse );
        events.add( event );
        
        return event;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeLogEvent log( LdapPrincipal principal, LdifEntry forward, List<LdifEntry> reverses )
    {
        currentRevision++;
        ChangeLogEvent event = new ChangeLogEvent( currentRevision, DateUtils.getGeneralizedTime(),
            principal, forward, reverses );
        events.add( event );
        
        return event;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeLogEvent lookup( long revision )
    {
        if ( revision < 0 )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_239 ) );
        }

        if ( revision > getCurrentRevision() )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_240 ) );
        }

        return events.get( ( int ) revision );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Cursor<ChangeLogEvent> find()
    {
        return new ListCursor<>( events );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Cursor<ChangeLogEvent> findBefore( long revision )
    {
        return new ListCursor<>( events, ( int ) revision );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Cursor<ChangeLogEvent> findAfter( long revision )
    {
        return new ListCursor<>( ( int ) revision, events );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Cursor<ChangeLogEvent> find( long startRevision, long endRevision )
    {
        return new ListCursor<>( ( int ) startRevision, events, ( int ) ( endRevision + 1 ) );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Tag getLatest()
    {
        return latest;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Tag removeTag( long revision )
    {
        return tags.remove( revision );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Tag tag( long revision, String descrition )
    {
        if ( tags.containsKey( revision ) )
        {
            return tags.get( revision );
        }

        latest = new Tag( revision, descrition );
        tags.put( revision, latest );
        return latest;
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "MemoryChangeLog\n" );
        sb.append( "latest tag : " ).append( latest ).append( '\n' );

        sb.append( "Nb of events : " ).append( events.size() ).append( '\n' );

        int i = 0;

        for ( ChangeLogEvent event : events )
        {
            sb.append( "event[" ).append( i++ ).append( "] : " );
            sb.append( "\n---------------------------------------\n" );
            sb.append( event );
            sb.append( "\n---------------------------------------\n" );
        }

        return sb.toString();
    }
}
