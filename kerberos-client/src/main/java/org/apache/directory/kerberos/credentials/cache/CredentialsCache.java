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
package org.apache.directory.kerberos.credentials.cache;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.shared.kerberos.components.PrincipalName;


/**
 * Kerberos credentials cache in FCC format
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CredentialsCache
{
    private int version = CredentialsCacheConstants.FCC_FVNO_4;
    private List<Tag> tags;
    private PrincipalName primaryPrincipal;
    private List<Credentials> credentialsList = new ArrayList<>();


    public static CredentialsCache load( File cacheFile ) throws IOException
    {
        return load( Files.newInputStream( cacheFile.toPath() ) );
    }


    public static CredentialsCache load( InputStream is ) throws IOException
    {
        try (CacheInputStream cis = new CacheInputStream( is ))
        {
            CredentialsCache credCache = new CredentialsCache();
            cis.read( credCache );
            return credCache;
        }
    }


    public static void store( File fileName, CredentialsCache credCache ) throws IOException
    {
        store( Files.newOutputStream( fileName.toPath() ), credCache );
    }


    public static void store( OutputStream os, CredentialsCache credCache ) throws IOException
    {
        CacheOutputStream cos = new CacheOutputStream( os );

        cos.write( credCache );

        cos.close();
    }


    public void addCredentials( Credentials cred )
    {
        this.credentialsList.add( cred );
    }


    public int getVersion()
    {
        return this.version;
    }


    public void setVersion( int version )
    {
        this.version = version;
    }


    /**
     * @return the primary principal
     */
    public PrincipalName getPrimaryPrincipalName()
    {
        return this.primaryPrincipal;
    }


    /**
     * Set the primary principal
     * 
     * @param principal The PrincipalName to set
     */
    public void setPrimaryPrincipalName( PrincipalName principal )
    {
        this.primaryPrincipal = principal;
    }


    public void setTags( List<Tag> tags )
    {
        this.tags = tags;
    }


    public List<Tag> getTags()
    {
        return this.tags;
    }


    /**
     * @return the credentials entries
     */
    public List<Credentials> getCredsList()
    {
        return this.credentialsList;
    }


    public static void main( String[] args ) throws IOException
    {
        String dumpFile = File.createTempFile( "credCache-", ".cc" ).getAbsolutePath();
        System.out.println( "This tool tests CredentialsCache reading and writing, " +
            "and will load the built-in sample credentials cache by default, and dump to " + dumpFile );

        System.out
            .println( "To specify your own credentials cache file, run this as: CredentialsCache [cred-cache-file] " );

        System.out.println( "When dumped successfully, run 'klist -e -c' from MIT to check the dumped file" );

        CredentialsCache cc;
        String cacheFile = args.length > 0 ? args[0] : null;
        if ( cacheFile == null )
        {
            byte[] sampleCache = SampleCredentialsCacheResource.getCacheContent();
            ByteArrayInputStream bais = new ByteArrayInputStream( sampleCache );
            cc = CredentialsCache.load( bais );
        }
        else
        {
            cc = CredentialsCache.load( new File( cacheFile ) );
        }

        if ( cc != null )
        {
            System.out.println( "Reading credentials cache is successful" );

            File tmpCacheFile = new File( dumpFile );
            tmpCacheFile.delete();
            CredentialsCache.store( tmpCacheFile, cc );

            System.out.println( "Writing credentials cache successfully to: " + dumpFile );
        }
    }
}
