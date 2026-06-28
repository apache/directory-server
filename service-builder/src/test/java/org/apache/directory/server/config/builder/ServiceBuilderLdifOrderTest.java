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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.junit.jupiter.api.Test;


public class ServiceBuilderLdifOrderTest
{
    @Test
    public void testReadTestEntriesLoadsDirectoryFilesInNameOrder() throws Exception
    {
        File ldifDirectory = Files.createTempDirectory( "ldif-order" ).toFile();

        Files.write( new File( ldifDirectory, "20-second.ldif" ).toPath(), Arrays.asList(
            "dn: cn=second,ou=system",
            "objectClass: person",
            "objectClass: top",
            "cn: second",
            "sn: second",
            "" ), StandardCharsets.UTF_8 );

        Files.write( new File( ldifDirectory, "10-first.ldif" ).toPath(), Arrays.asList(
            "dn: cn=first,ou=system",
            "objectClass: person",
            "objectClass: top",
            "cn: first",
            "sn: first",
            "" ), StandardCharsets.UTF_8 );

        List<LdifEntry> entries = ServiceBuilder.readTestEntries( ldifDirectory.getAbsolutePath() );

        assertEquals( 2, entries.size() );
        assertEquals( "cn=first,ou=system", entries.get( 0 ).getDn().getName() );
        assertEquals( "cn=second,ou=system", entries.get( 1 ).getDn().getName() );
    }
}
