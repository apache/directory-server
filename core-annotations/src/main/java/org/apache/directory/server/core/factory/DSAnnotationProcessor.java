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
package org.apache.directory.server.core.factory;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;

import javax.naming.NamingException;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.BTreePartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.GenericIndex;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.entry.DefaultServerEntry;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Helper class used to create a DS from the annotations
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DSAnnotationProcessor
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( DSAnnotationProcessor.class );


    /**
     * Create the DirectoryService
     */
    private static DirectoryService createDS( CreateDS dsBuilder ) throws Exception
    {
        LOG.debug( "Starting DS {}...", dsBuilder.name() );
        Class<?> factory = dsBuilder.factory();
        DirectoryServiceFactory dsf = ( DirectoryServiceFactory ) factory.newInstance();

        DirectoryService service = dsf.getDirectoryService();
        service.setAccessControlEnabled( dsBuilder.enableAccessControl() );
        service.setAllowAnonymousAccess( dsBuilder.allowAnonAccess() );
        service.getChangeLog().setEnabled( dsBuilder.enableChangeLog() );

        List<Interceptor> interceptorList = service.getInterceptors();
        for ( Class<?> interceptorClass : dsBuilder.additionalInterceptors() )
        {
            interceptorList.add( ( Interceptor ) interceptorClass.newInstance() );
        }

        service.setInterceptors( interceptorList );

        dsf.init( dsBuilder.name() );

        // Process the Partition, if any.
        for ( CreatePartition createPartition : dsBuilder.partitions() )
        {
            Partition partition;

            // Determine the partition type
            if ( createPartition.type() == Partition.class )
            {
                // The annotation does not specify a specific partition type.
                // We use the partition factory to create partition and index instances.
                PartitionFactory partitionFactory = dsf.getPartitionFactory();
                partition = partitionFactory.createPartition( createPartition.name(), createPartition.suffix(),
                    createPartition.cacheSize(), new File( service.getWorkingDirectory(), createPartition.name() ) );

                CreateIndex[] indexes = createPartition.indexes();
                for ( CreateIndex createIndex : indexes )
                {
                    partitionFactory.addIndex( partition, createIndex.attribute(), createIndex.cacheSize() );
                }
            }
            else
            {
                // The annotation contains a specific partition type, we use that type.
                partition = createPartition.type().newInstance();
                partition.setId( createPartition.name() );
                partition.setSuffix( createPartition.suffix() );

                if ( partition instanceof BTreePartition<?> )
                {
                    BTreePartition<?> btreePartition = ( BTreePartition<?> ) partition;
                    btreePartition.setCacheSize( createPartition.cacheSize() );
                    btreePartition.setPartitionDir( new File( service.getWorkingDirectory(), createPartition.name() ) );

                    // Process the indexes if any
                    CreateIndex[] indexes = createPartition.indexes();

                    for ( CreateIndex createIndex : indexes )
                    {
                        Index index;
                        if ( createIndex.type() == Index.class )
                        {
                            // The annotation does not specify a specific index type.
                            // We use the generic index implementation.
                            index = new GenericIndex( createIndex.attribute(), createIndex.cacheSize() );
                        }
                        else
                        {
                            // The annotation contains a specific index type, we use that type.
                            index = createIndex.type().newInstance();
                            index.setAttributeId( createIndex.attribute() );
                            index.setCacheSize( createIndex.cacheSize() );
                        }
                        btreePartition.addIndexedAttributes( index );
                    }
                }
            }

            partition.setSchemaManager( service.getSchemaManager() );

            // Inject the partition into the DirectoryService
            service.addPartition( partition );

            // Last, process the context entry
            ContextEntry contextEntry = createPartition.contextEntry();

            if ( contextEntry != null )
            {
                injectEntries( service, contextEntry.entryLdif() );
            }
        }

        return service;
    }


    /**
     * Create a DirectoryService from a Unit test annotation
     *
     * @param description The annotations containing the info from which we will create the DS
     * @return A valid DS
     */
    public static DirectoryService getDirectoryService( Description description ) throws Exception
    {
        CreateDS dsBuilder = description.getAnnotation( CreateDS.class );

        if ( dsBuilder != null )
        {
            return createDS( dsBuilder );
        }
        else
        {
            LOG.debug( "No {} DS.", description.getDisplayName() );
            return null;
        }
    }


    /**
     * Create a DirectoryService from an annotation. The @CreateDS annotation must
     * be associated with either the method or the encapsulating class. We will first
     * try to get the annotation from the method, and if there is none, then we try
     * at the class level. 
     *
     * @return A valid DS
     */
    public static DirectoryService getDirectoryService() throws Exception
    {
        CreateDS dsBuilder = null;

        // Get the caller by inspecting the stackTrace
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // In Java5 the 0th stacktrace element is: java.lang.Thread.dumpThreads(Native Method)
        int index = stackTrace[0].getMethodName().equals( "dumpThreads" ) ? 3 : 2;

        // Get the enclosing class
        Class<?> classCaller = Class.forName( stackTrace[index].getClassName() );

        // Get the current method
        String methodCaller = stackTrace[index].getMethodName();

        // Check if we have any annotation associated with the method
        Method[] methods = classCaller.getMethods();

        for ( Method method : methods )
        {
            if ( methodCaller.equals( method.getName() ) )
            {
                dsBuilder = method.getAnnotation( CreateDS.class );

                if ( dsBuilder != null )
                {
                    break;
                }
            }
        }

        // No : look at the class level
        if ( dsBuilder == null )
        {
            dsBuilder = classCaller.getAnnotation( CreateDS.class );
        }

        // Ok, we have found a CreateDS annotation. Process it now.
        return createDS( dsBuilder );
    }


    /**
     * injects an LDIF entry in the given DirectoryService
     * 
     * @param entry the LdifEntry to be injected
     * @param service the DirectoryService
     * @throws Exception
     */
    private static void injectEntry( LdifEntry entry, DirectoryService service ) throws Exception
    {
        if ( entry.isChangeAdd() )
        {
            service.getAdminSession().add( new DefaultServerEntry( service.getSchemaManager(), entry.getEntry() ) );
        }
        else if ( entry.isChangeModify() )
        {
            service.getAdminSession().modify( entry.getDn(), entry.getModificationItems() );
        }
        else
        {
            String message = I18n.err( I18n.ERR_117, entry.getChangeType() );
            throw new NamingException( message );
        }
    }


    /**
     * injects the LDIF entries present in a LDIF file
     * 
     * @param service the DirectoryService 
     * @param ldifFiles the array of LDIF file names (only )
     * @throws Exception
     */
    public static void injectLdifFiles( Class<?> clazz, DirectoryService service, String[] ldifFiles ) throws Exception
    {
        if ( ( ldifFiles != null ) && ( ldifFiles.length > 0 ) )
        {
            for ( String ldifFile : ldifFiles )
            {
                InputStream is = clazz.getClassLoader().getResourceAsStream( ldifFile );
                if ( is == null )
                {
                    throw new FileNotFoundException( "LDIF file '" + ldifFile + "' not found." );
                }
                else
                {
                    try
                    {
                        LdifReader ldifReader = new LdifReader( is );

                        for ( LdifEntry entry : ldifReader )
                        {
                            injectEntry( entry, service );
                        }

                        ldifReader.close();
                    }
                    catch ( Exception e )
                    {
                        LOG.error( I18n.err( I18n.ERR_80, ldifFile, e.getLocalizedMessage() ) );
                    }
                }
            }
        }
    }


    /**
     * Inject an ldif String into the server. DN must be relative to the
     * root.
     *
     * @param service the directory service to use 
     * @param ldif the ldif containing entries to add to the server.
     * @throws NamingException if there is a problem adding the entries from the LDIF
     */
    public static void injectEntries( DirectoryService service, String ldif ) throws Exception
    {
        LdifReader reader = new LdifReader();
        List<LdifEntry> entries = reader.parseLdif( ldif );

        for ( LdifEntry entry : entries )
        {
            injectEntry( entry, service );
        }

        // And close the reader
        reader.close();
    }


    /**
     * Apply the LDIF entries to the given service
     */
    public static void applyLdifs( Description desc, DirectoryService service ) throws Exception
    {
        if ( desc == null )
        {
            return;
        }

        ApplyLdifFiles applyLdifFiles = desc.getAnnotation( ApplyLdifFiles.class );

        if ( applyLdifFiles != null )
        {
            LOG.debug( "Applying {} to {}", applyLdifFiles.value(), desc.getDisplayName() );
            injectLdifFiles( desc.getClass(), service, applyLdifFiles.value() );
        }

        ApplyLdifs applyLdifs = desc.getAnnotation( ApplyLdifs.class );

        if ( ( applyLdifs != null ) && ( applyLdifs.value() != null ) )
        {
            String[] ldifs = applyLdifs.value();

            String DN_START = "dn:";

            StringBuilder sb = new StringBuilder();

            for ( int i = 0; i < ldifs.length; )
            {
                String s = ldifs[i++].trim();
                if ( s.startsWith( DN_START ) )
                {
                    sb.append( s ).append( '\n' );

                    // read the rest of lines till we encounter DN again
                    while ( i < ldifs.length )
                    {
                        s = ldifs[i++];
                        if ( !s.startsWith( DN_START ) )
                        {
                            sb.append( s ).append( '\n' );
                        }
                        else
                        {
                            break;
                        }
                    }

                    LOG.debug( "Applying {} to {}", sb, desc.getDisplayName() );
                    injectEntries( service, sb.toString() );
                    sb.setLength( 0 );

                    i--; // step up a line
                }
            }
        }
    }
}
