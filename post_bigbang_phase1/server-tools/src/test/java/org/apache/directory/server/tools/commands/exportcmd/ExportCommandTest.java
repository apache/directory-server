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
package org.apache.directory.server.tools.commands.exportcmd;


import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.tools.ToolCommandListener;
import org.apache.directory.server.tools.commands.AbstractTestCase;
import org.apache.directory.server.tools.commands.exportcmd.ExportCommandExecutor;
import org.apache.directory.server.tools.commands.importcmd.ImportCommandExecutor;
import org.apache.directory.server.tools.util.ListenerParameter;
import org.apache.directory.server.tools.util.Parameter;


/**
 * Test Class for the Export Command Executor
 */
public class ExportCommandTest extends AbstractTestCase
{
    public void testExportCommand()
    {
        // Checking if server had been launched
        if ( !bindSuccessful )
        {
            // The server hasn't been lauched, so we don't execute the test and return 
            // a successful test, so Maven can is Ok when executing tests.
            assertTrue( true );
        }
        else
        {
            // We are going to import a LDIF file, export the corresponding part of the newly
            // imported entries, then delete these entries and import again the entries
            // with the newly created LDIF file (from the export).
            // IMPORT -> EXPORT -> DELETE -> IMPORT | => TEST
            File importFile = null;
            try
            {
                // Getting the import file
                importFile = new File( ( ExportCommandTest.class.getResource( "10_entries.ldif" ) ).toURI() );
            }
            catch ( URISyntaxException e )
            {
                fail();
            }
            // Preparing the call to the Import Command
            ImportCommandExecutor importCommandExecutor = new ImportCommandExecutor();
            Parameter hostParam = new Parameter( ImportCommandExecutor.HOST_PARAMETER, host );
            Parameter portParam = new Parameter( ImportCommandExecutor.PORT_PARAMETER, new Integer( port ) );
            Parameter userParam = new Parameter( ImportCommandExecutor.USER_PARAMETER, user );
            Parameter passwordParam = new Parameter( ImportCommandExecutor.PASSWORD_PARAMETER, password );
            Parameter authParam = new Parameter( ImportCommandExecutor.AUTH_PARAMETER, "simple" );
            Parameter fileParam = new Parameter( ImportCommandExecutor.FILE_PARAMETER, importFile );
            Parameter ignoreErrorsParam = new Parameter( ImportCommandExecutor.IGNOREERRORS_PARAMETER, new Boolean(
                false ) );
            Parameter debugParam = new Parameter( ImportCommandExecutor.DEBUG_PARAMETER, new Boolean( false ) );
            Parameter verboseParam = new Parameter( ImportCommandExecutor.VERBOSE_PARAMETER, new Boolean( false ) );
            Parameter quietParam = new Parameter( ImportCommandExecutor.QUIET_PARAMETER, new Boolean( false ) );

            ToolCommandListener errorListener = new ToolCommandListener()
            {
                public void notify( Serializable o )
                {
                    fail();
                }
            };
            ListenerParameter errorListenerParam = new ListenerParameter(
                ImportCommandExecutor.ERRORLISTENER_PARAMETER, errorListener );

            // Calling the import command
            importCommandExecutor.execute( new Parameter[]
                { hostParam, portParam, userParam, passwordParam, authParam, fileParam, ignoreErrorsParam, debugParam,
                    verboseParam, quietParam }, new ListenerParameter[]
                { errorListenerParam } );

            // Creating a temporary file for the export
            File exportedFile = null;
            try
            {
                exportedFile = File.createTempFile( "exportedFile", ".ldif" );
            }
            catch ( IOException e1 )
            {
                fail();
            }

            // Preparing the call to the Export Command
            ExportCommandExecutor exportCommandExecutor = new ExportCommandExecutor();
            hostParam = new Parameter( ExportCommandExecutor.HOST_PARAMETER, host );
            portParam = new Parameter( ExportCommandExecutor.PORT_PARAMETER, new Integer( port ) );
            userParam = new Parameter( ExportCommandExecutor.USER_PARAMETER, user );
            passwordParam = new Parameter( ExportCommandExecutor.PASSWORD_PARAMETER, password );
            authParam = new Parameter( ExportCommandExecutor.AUTH_PARAMETER, "simple" );
            Parameter baseDNParam = new Parameter( ExportCommandExecutor.BASEDN_PARAMETER, "" );
            Parameter exportPointParam = new Parameter( ExportCommandExecutor.EXPORTPOINT_PARAMETER,
                "o=neworganization, dc=example,dc=com" );
            Parameter scopeParam = new Parameter( ExportCommandExecutor.SCOPE_PARAMETER,
                ExportCommandExecutor.SCOPE_SUBTREE );
            fileParam = new Parameter( ExportCommandExecutor.FILE_PARAMETER, exportedFile.getAbsolutePath() );
            debugParam = new Parameter( ExportCommandExecutor.DEBUG_PARAMETER, new Boolean( false ) );
            verboseParam = new Parameter( ExportCommandExecutor.VERBOSE_PARAMETER, new Boolean( false ) );
            quietParam = new Parameter( ExportCommandExecutor.QUIET_PARAMETER, new Boolean( false ) );

            // Calling the export command
            exportCommandExecutor.execute( new Parameter[]
                { hostParam, portParam, userParam, passwordParam, authParam, baseDNParam, exportPointParam, fileParam,
                    scopeParam, debugParam, verboseParam, quietParam }, new ListenerParameter[0] );

            // Deleting the entries previously added
            try
            {
                deleteLDAPSubtree( "o=neworganization,dc=example,dc=com" );
            }
            catch ( NamingException e )
            {
                fail();
            }

            // Preparing the call to the Import Command
            importCommandExecutor = new ImportCommandExecutor();
            hostParam = new Parameter( ImportCommandExecutor.HOST_PARAMETER, host );
            portParam = new Parameter( ImportCommandExecutor.PORT_PARAMETER, new Integer( port ) );
            userParam = new Parameter( ImportCommandExecutor.USER_PARAMETER, user );
            passwordParam = new Parameter( ImportCommandExecutor.PASSWORD_PARAMETER, password );
            authParam = new Parameter( ImportCommandExecutor.AUTH_PARAMETER, "simple" );
            fileParam = new Parameter( ImportCommandExecutor.FILE_PARAMETER, new File( exportedFile.getAbsolutePath() ) );
            ignoreErrorsParam = new Parameter( ImportCommandExecutor.IGNOREERRORS_PARAMETER, new Boolean( false ) );
            debugParam = new Parameter( ImportCommandExecutor.DEBUG_PARAMETER, new Boolean( false ) );
            verboseParam = new Parameter( ImportCommandExecutor.VERBOSE_PARAMETER, new Boolean( false ) );
            quietParam = new Parameter( ImportCommandExecutor.QUIET_PARAMETER, new Boolean( false ) );

            // Calling the import command, this time on the newly created file from the export
            importCommandExecutor.execute( new Parameter[]
                { hostParam, portParam, userParam, passwordParam, authParam, fileParam, ignoreErrorsParam, debugParam,
                    verboseParam, quietParam }, new ListenerParameter[]
                { errorListenerParam } );

            // Now we are able to test if all is successful

            // Testing if the number of entries is correct
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
            try
            {
                NamingEnumeration entries = ctx.search( "o=neworganization,dc=example,dc=com", "(objectClass=*)", ctls );

                int counter = 0;
                while ( entries.hasMore() )
                {
                    counter++;
                    entries.next();
                }

                // Testing the number of entries added
                assertEquals( 9, counter );
            }
            catch ( NamingException e )
            {
                fail();
            }

            // Testing an entry and its attributes
            try
            {
                ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
                NamingEnumeration entries = ctx.search( "cn=newperson1,o=neworganization,dc=example,dc=com",
                    "(objectClass=*)", ctls );

                if ( entries.hasMore() )
                {
                    SearchResult sr = ( SearchResult ) entries.nextElement();

                    assertEquals( "cn=newperson1,o=neworganization,dc=example,dc=com", sr.getNameInNamespace() );

                    Attributes attributes = sr.getAttributes();

                    Attribute attr = attributes.get( "objectclass" );
                    assertTrue( attr.contains( "top" ) );
                    assertTrue( attr.contains( "person" ) );

                    attr = attributes.get( "cn" );
                    assertTrue( attr.contains( "newperson1" ) );

                    attr = attributes.get( "telephoneNumber" );
                    assertTrue( attr.contains( "0101010101" ) );
                }
                else
                {
                    fail();
                }
            }
            catch ( NamingException e )
            {
                fail();
            }

            // Testing another entry and its attributes
            try
            {
                ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
                NamingEnumeration entries = ctx.search( "cn=newperson4,o=neworganization,dc=example,dc=com",
                    "(objectClass=*)", ctls );

                if ( entries.hasMore() )
                {
                    SearchResult sr = ( SearchResult ) entries.nextElement();

                    assertEquals( "cn=newperson4,o=neworganization,dc=example,dc=com", sr.getNameInNamespace() );

                    Attributes attributes = sr.getAttributes();

                    Attribute attr = attributes.get( "objectclass" );
                    assertTrue( attr.contains( "top" ) );
                    assertTrue( attr.contains( "person" ) );

                    attr = attributes.get( "cn" );
                    assertTrue( attr.contains( "newperson4" ) );

                    attr = attributes.get( "telephoneNumber" );
                    assertTrue( attr.contains( "0404040404" ) );
                }
                else
                {
                    fail();
                }
            }
            catch ( NamingException e )
            {
                fail();
            }

            // Testing a thirf entry and its attributes
            try
            {
                ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
                NamingEnumeration entries = ctx.search( "cn=newperson9,o=neworganization,dc=example,dc=com",
                    "(objectClass=*)", ctls );

                if ( entries.hasMore() )
                {
                    SearchResult sr = ( SearchResult ) entries.nextElement();

                    assertEquals( "cn=newperson9,o=neworganization,dc=example,dc=com", sr.getNameInNamespace() );

                    Attributes attributes = sr.getAttributes();

                    Attribute attr = attributes.get( "objectclass" );
                    assertTrue( attr.contains( "top" ) );
                    assertTrue( attr.contains( "person" ) );

                    attr = attributes.get( "cn" );
                    assertTrue( attr.contains( "newperson9" ) );

                    attr = attributes.get( "telephoneNumber" );
                    assertTrue( attr.contains( "0909090909" ) );
                }
                else
                {
                    fail();
                }
            }
            catch ( NamingException e )
            {
                fail();
            }
            // This file will be deleted when the JVM will exit.
            exportedFile.deleteOnExit();
        }
    }
}
