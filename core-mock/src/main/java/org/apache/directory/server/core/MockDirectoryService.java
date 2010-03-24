/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core;


import java.io.File;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.changelog.ChangeLog;
import org.apache.directory.server.core.event.EventService;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.interceptor.InterceptorChain;
import org.apache.directory.server.core.journal.Journal;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.replication.ReplicationConfiguration;
import org.apache.directory.server.core.schema.SchemaService;
import org.apache.directory.shared.ldap.csn.Csn;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.SchemaManager;


public class MockDirectoryService implements DirectoryService
{
    int count;
    
    
    public MockDirectoryService( int count )
    {
        this.count = count;
    }
    
    public Hashtable<String, Object> getEnvironment()
    {
        return null;
    }


    public void setEnvironment( Hashtable<String, Object> environment )
    {
    }


    public long revert( long revision ) throws NamingException
    {
        return 0;
    }


    public long revert() throws NamingException
    {
        return 0;
    }


    public PartitionNexus getPartitionNexus()
    {
        return null;
    }


    public InterceptorChain getInterceptorChain()
    {
        return null;
    }


    public void addPartition( Partition partition ) throws NamingException
    {
    }


    public void removePartition( Partition partition ) throws NamingException
    {
    }


    public SchemaManager getSchemaManager()
    {
        return null;
    }


    public ReferralManager getReferralManager()
    {
        return null;
    }


    public void setReferralManager( ReferralManager referralManager )
    {
    }


    public void setSchemaManager( SchemaManager schemaManager )
    {
    }


    public SchemaService getSchemaService()
    {
        return null;
    }


    public void setSchemaService( SchemaService schemaService )
    {

    }


    public void startup() throws NamingException
    {
    }


    public void shutdown() throws NamingException
    {
    }


    public void sync() throws NamingException
    {
    }


    public boolean isStarted()
    {
        return true;
    }


    public LdapContext getJndiContext() throws NamingException
    {
        return null;
    }


    public DirectoryService getDirectoryService()
    {
        return null;
    }


    public void setInstanceId( String instanceId )
    {

    }


    public String getInstanceId()
    {
        return null;
    }


    public Set<? extends Partition> getPartitions()
    {
        return null;
    }


    public void setPartitions( Set<? extends Partition> partitions )
    {
    }


    public boolean isAccessControlEnabled()
    {
        return false;
    }


    public void setAccessControlEnabled( boolean accessControlEnabled )
    {
    }


    public boolean isAllowAnonymousAccess()
    {
        return false;
    }


    public void setAllowAnonymousAccess( boolean enableAnonymousAccess )
    {

    }


    public List<Interceptor> getInterceptors()
    {
        return null;
    }


    public void setInterceptors( List<Interceptor> interceptors )
    {

    }


    public List<LdifEntry> getTestEntries()
    {
        return null;
    }


    public void setTestEntries( List<? extends LdifEntry> testEntries )
    {
    }


    public File getWorkingDirectory()
    {
        return null;
    }


    public void setWorkingDirectory( File workingDirectory )
    {
    }


    public void validate()
    {
    }


    public void setShutdownHookEnabled( boolean shutdownHookEnabled )
    {

    }


    public boolean isShutdownHookEnabled()
    {
        return false;
    }


    public void setExitVmOnShutdown( boolean exitVmOnShutdown )
    {

    }


    public boolean isExitVmOnShutdown()
    {
        return false;
    }


    public void setMaxSizeLimit( long maxSizeLimit )
    {

    }


    public long getMaxSizeLimit()
    {
        return 0;
    }


    public void setMaxTimeLimit( int maxTimeLimit )
    {

    }


    public int getMaxTimeLimit()
    {
        return 0;
    }


    public void setSystemPartition( Partition systemPartition )
    {

    }


    public Partition getSystemPartition()
    {
        return null;
    }


    public boolean isDenormalizeOpAttrsEnabled()
    {
        return false;
    }


    public void setDenormalizeOpAttrsEnabled( boolean denormalizeOpAttrsEnabled )
    {

    }
    
    public void setChangeLog( ChangeLog changeLog )
    {
        
    }
    
    public ChangeLog getChangeLog()
    {
        return null;
    }


    public Journal getJournal()
    {
        return null;
    }


    public ServerEntry newEntry( DN dn ) throws NamingException
    {
        return null;
    }
    
    public ServerEntry newEntry( String ldif, String dn )
    {
        return null;
    }


    public OperationManager getOperationManager()
    {
        return new MockOperationManager( count );
    }


    public CoreSession getSession() throws Exception
    {
        return null;
    }


    public CoreSession getSession( LdapPrincipal principal ) throws Exception
    {
        return null;
    }


    public CoreSession getSession( DN principalDn, byte[] credentials ) throws Exception
    {
        return null;
    }

    
    public CoreSession getSession( DN principalDn, byte[] credentials, String saslMechanism, String saslAuthId )
        throws Exception
    {
        return null;
    }

    public CoreSession getAdminSession() throws Exception
    {
        return null;
    }

    public EventService getEventService()
    {
        return null;
    }

    public void setEventService( EventService eventService )
    {
    }
    
    public boolean isPasswordHidden()
    {
        return false;
    }
    
    public void setPasswordHidden( boolean passwordHidden )
    {
    }


    public int getMaxPDUSize()
    {
        return Integer.MAX_VALUE;
    }


    public void setMaxPDUSize( int maxPDUSize )
    {
        // Do nothing
    }

    
    public Interceptor getInterceptor( String interceptorName )
    {
        return null;
    }
    
    
    public Csn getCSN()
    {
        return null;
    }
    
    
    public int getReplicaId()
    {
        return 0;
    }
    
    
    public void setReplicaId( int replicaId )
    {
        
    }

    public void setJournal( Journal journal )
    {
        
    }

    public void setReplicationConfiguration( ReplicationConfiguration replicationConfig )
    {
        // TODO Auto-generated method stub
        
    }

    public ReplicationConfiguration getReplicationConfiguration()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
