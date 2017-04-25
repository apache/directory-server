/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.journal;


import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifUtils;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.journal.JournalStore;


/**
 * @todo : Missing Javadoc
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
*/
public class DefaultJournalStore implements JournalStore
{
    /** The directory where the journal is stored */
    private File workingDirectory;

    /** The journal file name */
    private String fileName;

    /** The file containing the journal */
    private File journal;

    /** The stream used to write data into the journal */
    private Writer writer;


    /**
     * {@inheritDoc}
     */
    public void destroy() throws Exception
    {
        if ( writer != null )
        {
            writer.close();
        }
    }


    /**
     * Initialize the interceptor
     */
    public void init( DirectoryService service ) throws Exception
    {
        if ( workingDirectory == null )
        {
            workingDirectory = service.getInstanceLayout().getLogDirectory();
        }

        /** Load or create the journal file */
        if ( fileName == null )
        {
            fileName = "journal.ldif";
        }

        journal = new File( workingDirectory, fileName );

        // The new requests are added at the end of the existing journal
        writer = new PrintWriter(
            new OutputStreamWriter(
                Files.newOutputStream( journal.toPath(), StandardOpenOption.APPEND ) ) );
    }


    /**
     * Stores an event into the journal.
     * 
     * @param principal The principal who is logging the change
     * @param revision The operation revision
     * @param forward The change to log
     */
    public boolean log( LdapPrincipal principal, long revision, LdifEntry forward )
    {
        synchronized ( writer )
        {
            try
            {
                // Write the LdapPrincipal
                writer.write( "# principal: " );
                writer.write( principal.getName() );
                writer.write( '\n' );

                // Write the timestamp
                writer.write( "# timestamp: " );
                writer.write( Long.toString( System.currentTimeMillis() ) );
                writer.write( '\n' );

                // Write the revision
                writer.write( "# revision: " );
                writer.write( Long.toString( revision ) );
                writer.write( "\n" );

                // Write the entry
                writer.write( LdifUtils.convertToLdif( forward, 80 ) );
                writer.flush();
            }
            catch ( LdapException ne )
            {
                return false;
            }
            catch ( IOException ioe )
            {
                return false;
            }
        }

        return true;
    }


    /**
     * Records a ack for a change
     *
     * @param revision The change revision which is acked
     * @return <code>true</code> if the ack has been written
     * @throws Exception if there are problems logging the ack
     */
    public boolean ack( long revision )
    {
        synchronized ( writer )
        {
            try
            {
                // Write the revision
                writer.write( "# ack-revision: " );
                writer.write( Long.toString( revision ) );
                writer.write( "\n\n" );

                writer.flush();
            }
            catch ( IOException ioe )
            {
                return false;
            }
        }

        return true;
    }


    /**
     * Records a nack for a change
     *
     * @param revision The change revision which is nacked
     * @return <code>true</code> if the nack has been written
     * @throws Exception if there are problems logging the nack
     */
    public boolean nack( long revision )
    {
        synchronized ( writer )
        {
            try
            {
                // Write the revision
                writer.write( "# nack-revision: " );
                writer.write( Long.toString( revision ) );
                writer.write( "\n\n" );

                writer.flush();
            }
            catch ( IOException ioe )
            {
                return false;
            }
        }

        return true;
    }


    public void sync() throws Exception
    {
        // TODO Auto-generated method stub

    }


    public long getCurrentRevision()
    {
        // TODO Auto-generated method stub
        return 0;
    }


    /**
     * @return the fileName
     */
    public String getFileName()
    {
        return fileName;
    }


    /**
     * @param fileName the fileName to set
     */
    public void setFileName( String fileName )
    {
        this.fileName = fileName;
    }


    /**
     * {@inheritDoc}
     */
    public void setWorkingDirectory( String workingDirectoryName )
    {
        this.workingDirectory = new File( workingDirectoryName );
    }
}
