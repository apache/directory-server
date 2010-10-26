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

package org.apache.directory.server.config;


import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.directory.server.config.beans.AdsBaseBean;
import org.apache.directory.server.config.beans.ConfigBean;
import org.apache.directory.server.config.beans.DirectoryServiceBean;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A class used for reading the configuration present in a Partition
 * and instantiate the necessary objects like DirectoryService, Interceptors etc.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ConfigCreator
{
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ConfigCreator.class );

    /**
     * instantiates a DirectoryService based on the configuration present in the partition 
     *
     * @throws Exception
     */
    private DirectoryService createDirectoryService( DirectoryServiceBean directoryServiceBean ) throws LdapException
    {
        DirectoryService directoryService = new DefaultDirectoryService();

        // MUST attributes
        directoryService.setInstanceId( directoryServiceBean.getDirectoryServiceId() );
        directoryService.setReplicaId( directoryServiceBean.getDsReplicaId() );

        List<Interceptor> interceptors = createInterceptors( directoryServiceBean.getInterceptors() );
        directoryService.setInterceptors( interceptors );
        
        AuthenticationInterceptor authnInterceptor = ( AuthenticationInterceptor ) directoryService.getInterceptor( AuthenticationInterceptor.class.getName() );
        authnInterceptor.setPwdPolicyConfig( createPwdPolicyConfig( directoryServiceBean.getPasswordPolicy() ) );

        Map<String, Partition> partitions = createPartitions( directoryServiceBean.getPartitions() );

        Partition systemPartition = partitions.remove( "system" );

        if ( systemPartition == null )
        {
            //throw new Exception( I18n.err( I18n.ERR_505 ) );
        }

        directoryService.setSystemPartition( systemPartition );
        directoryService.setPartitions( new HashSet<Partition>( partitions.values() ) );

        // MAY attributes
        directoryService.setAccessControlEnabled( directoryServiceBean.isDsAccessControlEnabled() );
        directoryService.setAllowAnonymousAccess( directoryServiceBean.isDsAllowAnonymousAccess() );
        directoryService.setChangeLog( createChangeLog( directoryServiceBean.getChangeLog() ) );
        directoryService.setDenormalizeOpAttrsEnabled( directoryServiceBean.isDsDenormalizeOpAttrsEnabled() );
        directoryService.setJournal( createJournal( directoryServiceBean.getJournal() ) );
        directoryService.setMaxPDUSize( directoryServiceBean.getDsMaxPDUSize() );
        directoryService.setPasswordHidden( directoryServiceBean.isDsPasswordHidden() );

        //EntryAttribute replAttr = dsEntry.get( ConfigSchemaConstants.ADS_DS_REPLICATION );

        //if ( replAttr != null )
        //{
            // configure replication
        //}

        directoryService.setSyncPeriodMillis( directoryServiceBean.getDsSyncPeriodMillis() );

        String entryFilePath = directoryServiceBean.getDsTestEntries();
        directoryService.setTestEntries( readTestEntries( entryFilePath ) );

        if ( !directoryServiceBean.isEnabled() )
        {
            // will only be useful if we ever allow more than one DS to be configured and
            // switch between them
            // decide which one to use based on this flag
        }

        return directoryService;
    }

    
    /**
     * Create a new DirectoryService instance using the ConfigBean as a container for 
     * the configuration
     * 
     * @param configBean The Bean containing all the needed configuration to create the DS
     * @param directoryServiceId The DS id we wnat to instanciate
     * @return An instance of DS
     */
    public DirectoryService createDirectoryService( ConfigBean configBean, String directoryServiceId ) throws LdapException
    {
        List<AdsBaseBean> baseBeans = configBean.getDirectoryServiceBeans();
        
        for ( AdsBaseBean baseBean : baseBeans )
        {
            if ( !( baseBean instanceof DirectoryServiceBean ) )
            {
                String message = "Cannot instanciate a DS if the bean does not contain DirectoryService beans";
                LOG.error( message );
                throw new ConfigurationException( message );
            }
            
            DirectoryServiceBean directoryServiceBean = (DirectoryServiceBean)baseBean;
            
            if ( directoryServiceBean.getDirectoryServiceId().equalsIgnoreCase( directoryServiceId ) )
            {
                DirectoryService directoryService = createDirectoryService( directoryServiceBean );
                
                return directoryService;
            }
        }
        
        LOG.info( "Cannot instanciate the {} directory service, it was not found in the configuration", directoryServiceId );
        return null;
    }
}