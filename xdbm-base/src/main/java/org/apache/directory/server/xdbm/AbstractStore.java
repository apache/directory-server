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
package org.apache.directory.server.xdbm;


import java.io.File;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.SchemaManager;


/**
 * Base implementation of a {@link Store}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public abstract class AbstractStore<E, ID> implements Store<E, ID>
{

    /** The default cache size is set to 10 000 objects */
    public static final int DEFAULT_CACHE_SIZE = 10000;

    /** true if initialized */
    protected boolean initialized;

    /** the partition directory to use for files */
    protected File partitionDir;

    /** true if we sync disks on every write operation */
    protected boolean isSyncOnWrite = true;

    /** The store cache size */
    protected int cacheSize = DEFAULT_CACHE_SIZE;

    /** The store unique identifier */
    protected String id;

    /** The suffix DN */
    protected DN suffixDn;

    /** A pointer on the schemaManager */
    protected SchemaManager schemaManager;


    protected void protect( String property )
    {
        if ( initialized )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_576, property ) );
        }
    }


    public boolean isInitialized()
    {
        return initialized;
    }


    public void setPartitionDir( File partitionDir )
    {
        protect( "partitionDir" );
        this.partitionDir = partitionDir;
    }


    public File getPartitionDir()
    {
        return partitionDir;
    }


    public void setSyncOnWrite( boolean isSyncOnWrite )
    {
        protect( "syncOnWrite" );
        this.isSyncOnWrite = isSyncOnWrite;
    }


    public boolean isSyncOnWrite()
    {
        return isSyncOnWrite;
    }


    public void setCacheSize( int cacheSize )
    {
        protect( "cacheSize" );
        this.cacheSize = cacheSize;
    }


    public int getCacheSize()
    {
        return cacheSize;
    }


    public void setId( String id )
    {
        protect( "id" );
        this.id = id;
    }


    public String getId()
    {
        return id;
    }


    public void setSuffixDn( DN suffixDn )
    {
        protect( "suffixDn" );
        if ( !suffixDn.isNormalized() )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_218, suffixDn.getName() ) );
        }
        this.suffixDn = suffixDn;
    }


    public DN getSuffixDn()
    {
        return suffixDn;
    }
}
