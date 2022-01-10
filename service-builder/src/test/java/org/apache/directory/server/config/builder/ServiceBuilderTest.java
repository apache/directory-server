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
package org.apache.directory.server.config.builder;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.api.ldap.model.constants.LdapSecurityConstants;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.config.beans.HashInterceptorBean;
import org.apache.directory.server.config.beans.InterceptorBean;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.hash.ConfigurableHashingInterceptor;
import org.apache.directory.server.i18n.I18n;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


/**
 * Tests the factory methods of the ServiceBuilder.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ServiceBuilderTest 
{
    private static SchemaManager schemaManager;

    @BeforeAll
    public static void initSchemaManager() throws Exception
    {
        File partitionsDirectory = Files.createTempDirectory( "partitions" ).toFile();
        File schemaPartitionDirectory = new File( partitionsDirectory, "schema" );
        new DefaultSchemaLdifExtractor( partitionsDirectory ).extractOrCopy();

        SchemaLoader loader = new LdifSchemaLoader( schemaPartitionDirectory );
        schemaManager = new DefaultSchemaManager( loader );

        // We have to load the schema now, otherwise we won't be able
        // to initialize the Partitions, as we won't be able to parse
        // and normalize their suffix Dn
        schemaManager.loadAllEnabled();

        List<Throwable> errors = schemaManager.getErrors();

        if ( errors.size() != 0 )
        {
            fail( "unable to create initialize schema manager: " + I18n.err( I18n.ERR_317, Exceptions.printErrors( errors ) ) );
        }
    }

    @Test
    public void testCreateConfigurableHashInterceptor()
    {
        HashInterceptorBean bean = new HashInterceptorBean();
        bean.setInterceptorClassName( "org.apache.directory.server.core.hash.ConfigurableHashingInterceptor" );
        bean.setHashAlgorithm( "SSHA-256" );
        bean.addHashAttributes( 
                new String[] {
                    schemaManager.getAttributeType( "userPassword" ).getOid(),
                    schemaManager.getAttributeType( "cn" ).getOid(),
                });
        
        List<InterceptorBean> interceptorBeans = new ArrayList<>();
        interceptorBeans.add( bean );

        try 
        {
            List<Interceptor> interceptors = ServiceBuilder.createInterceptors( interceptorBeans );
            assertNotNull( interceptors );
            assertEquals( 1, interceptors.size() );
            
            Interceptor interceptor = interceptors.get( 0 );
            assertEquals( ConfigurableHashingInterceptor.class, interceptor.getClass() );
            
            DirectoryService directoryService = new DefaultDirectoryService();
            directoryService.setSchemaManager( schemaManager );
            interceptor.init( directoryService );
            
            List<AttributeType> hashAttributeTypes = ((ConfigurableHashingInterceptor)interceptor).getAttributeTypes();
            assertTrue( hashAttributeTypes.contains( schemaManager.getAttributeType( "userPassword" ) ) );
            assertTrue( hashAttributeTypes.contains( schemaManager.getAttributeType( "cn" ) ) );
            
            assertEquals( LdapSecurityConstants.HASH_METHOD_SSHA256,
                    ((ConfigurableHashingInterceptor)interceptor).getAlgorithm() );
        }
        catch ( Exception e ) 
        {
            fail( "unable to create hash interceptor: " + e.getMessage() );
        }
    }
}
