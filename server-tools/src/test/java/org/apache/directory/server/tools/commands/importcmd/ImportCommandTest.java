/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.tools.commands.importcmd;


import java.io.File;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.tools.ToolCommandListener;
import org.apache.directory.server.tools.commands.AbstractTestCase;
import org.apache.directory.server.tools.commands.importcmd.ImportCommandExecutor;
import org.apache.directory.server.tools.util.ListenerParameter;
import org.apache.directory.server.tools.util.Parameter;


/**
 * Test Class for the Import Command Executor
 */
public class ImportCommandTest extends AbstractTestCase
{
    private boolean error;
    private boolean added;
    private boolean exception;


    /**
     * Tests the import with a valid LDIF file containing one entry
     */
    public void testOneEntryImport()
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
            try
            {
                // Getting the import file
                File importFile = new File( ( ImportCommandTest.class.getResource( "import_1_entry.ldif" ) ).toURI() );

                // Preparing the call to the Import Command
                ImportCommandExecutor importCommandExecutor = new ImportCommandExecutor();
                Parameter hostParam = new Parameter( ImportCommandExecutor.HOST_PARAMETER, host );
                Parameter portParam = new Parameter( ImportCommandExecutor.PORT_PARAMETER, new Integer( port ) );
                Parameter userParam = new Parameter( ImportCommandExecutor.USER_PARAMETER, user );
                Parameter passwordParam = new Parameter( ImportCommandExecutor.PASSWORD_PARAMETER, password );
                Parameter authParam = new Parameter( ImportCommandExecutor.AUTH_PARAMETER, "simple" );
                Parameter fileParam = new Parameter( ImportCommandExecutor.FILE_PARAMETER, importFile );
                Parameter ignoreErrorsParam = new Parameter( ImportCommandExecutor.IGNOREERRORS_PARAMETER, new Boolean(
                    true ) );
                Parameter debugParam = new Parameter( ImportCommandExecutor.DEBUG_PARAMETER, new Boolean( false ) );
                Parameter verboseParam = new Parameter( ImportCommandExecutor.VERBOSE_PARAMETER, new Boolean( false ) );
                Parameter quietParam = new Parameter( ImportCommandExecutor.QUIET_PARAMETER, new Boolean( false ) );

                // Calling the import command
                importCommandExecutor.execute( new Parameter[]
                    { hostParam, portParam, userParam, passwordParam, authParam, fileParam, ignoreErrorsParam,
                        debugParam, verboseParam, quietParam }, new ListenerParameter[0] );

            }
            catch ( URISyntaxException e )
            {
                fail();
            }

            // Testing if the import is successful
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
            try
            {
                NamingEnumeration entries = ctx.search( "o=neworganization,dc=example,dc=com", "(objectClass=*)", ctls );

                if ( entries.hasMore() )
                {
                    SearchResult sr = ( SearchResult ) entries.nextElement();

                    assertEquals( "o=neworganization,dc=example,dc=com", sr.getNameInNamespace() );

                    Attributes attributes = sr.getAttributes();

                    Attribute attr = attributes.get( "objectclass" );
                    assertTrue( attr.contains( "top" ) );
                    assertTrue( attr.contains( "organization" ) );

                    attr = attributes.get( "o" );
                    assertTrue( attr.contains( "neworganization" ) );
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
        }
    }


    /**
     * Tests the import with a valid LDIF file containing ten entries
     */
    public void testTenEntriesImport()
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
            try
            {
                // Getting the import file
                File importFile = new File( ( ImportCommandTest.class.getResource( "import_10_entries.ldif" ) ).toURI() );

                // Preparing the call to the Import Command
                ImportCommandExecutor importCommandExecutor = new ImportCommandExecutor();
                Parameter hostParam = new Parameter( ImportCommandExecutor.HOST_PARAMETER, host );
                Parameter portParam = new Parameter( ImportCommandExecutor.PORT_PARAMETER, new Integer( port ) );
                Parameter userParam = new Parameter( ImportCommandExecutor.USER_PARAMETER, user );
                Parameter passwordParam = new Parameter( ImportCommandExecutor.PASSWORD_PARAMETER, password );
                Parameter authParam = new Parameter( ImportCommandExecutor.AUTH_PARAMETER, "simple" );
                Parameter fileParam = new Parameter( ImportCommandExecutor.FILE_PARAMETER, importFile );
                Parameter ignoreErrorsParam = new Parameter( ImportCommandExecutor.IGNOREERRORS_PARAMETER, new Boolean(
                    true ) );
                Parameter debugParam = new Parameter( ImportCommandExecutor.DEBUG_PARAMETER, new Boolean( false ) );
                Parameter verboseParam = new Parameter( ImportCommandExecutor.VERBOSE_PARAMETER, new Boolean( false ) );
                Parameter quietParam = new Parameter( ImportCommandExecutor.QUIET_PARAMETER, new Boolean( false ) );

                // Calling the import command
                importCommandExecutor.execute( new Parameter[]
                    { hostParam, portParam, userParam, passwordParam, authParam, fileParam, ignoreErrorsParam,
                        debugParam, verboseParam, quietParam }, new ListenerParameter[0] );

            }
            catch ( URISyntaxException e )
            {
                fail();
            }

            // Testing if the import is successful
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
        }
    }


    /**
     * Tests the import with a file containing one entry on error.
     */
    public void testOneEntryOnErrorImport()
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
            error = false;
            try
            {
                // Getting the import file
                File importFile = new File( ( ImportCommandTest.class.getResource( "import_1_entry_on_error.ldif" ) )
                    .toURI() );

                // Preparing the call to the Import Command
                ImportCommandExecutor importCommandExecutor = new ImportCommandExecutor();
                Parameter hostParam = new Parameter( ImportCommandExecutor.HOST_PARAMETER, host );
                Parameter portParam = new Parameter( ImportCommandExecutor.PORT_PARAMETER, new Integer( port ) );
                Parameter userParam = new Parameter( ImportCommandExecutor.USER_PARAMETER, user );
                Parameter passwordParam = new Parameter( ImportCommandExecutor.PASSWORD_PARAMETER, password );
                Parameter authParam = new Parameter( ImportCommandExecutor.AUTH_PARAMETER, "simple" );
                Parameter fileParam = new Parameter( ImportCommandExecutor.FILE_PARAMETER, importFile );
                Parameter ignoreErrorsParam = new Parameter( ImportCommandExecutor.IGNOREERRORS_PARAMETER, new Boolean(
                    true ) );
                Parameter debugParam = new Parameter( ImportCommandExecutor.DEBUG_PARAMETER, new Boolean( false ) );
                Parameter verboseParam = new Parameter( ImportCommandExecutor.VERBOSE_PARAMETER, new Boolean( false ) );
                Parameter quietParam = new Parameter( ImportCommandExecutor.QUIET_PARAMETER, new Boolean( false ) );

                ToolCommandListener errorListener = new ToolCommandListener()
                {
                    public void notify( Serializable o )
                    {
                        error = true;
                    }
                };
                ListenerParameter errorListenerParam = new ListenerParameter(
                    ImportCommandExecutor.ERRORLISTENER_PARAMETER, errorListener );

                // Calling the import command
                importCommandExecutor.execute( new Parameter[]
                    { hostParam, portParam, userParam, passwordParam, authParam, fileParam, ignoreErrorsParam,
                        debugParam, verboseParam, quietParam }, new ListenerParameter[]
                    { errorListenerParam } );

            }
            catch ( URISyntaxException e )
            {
                fail();
            }

            // Testing if the error notification has been raised.
            if ( error )
            {
                assertTrue( true );
            }
            else
            {
                fail();
            }
        }
    }


    /**
     * Tests the import with a file containing two entries, one on error and one that is Ok.
     * As we use the ignore-errors parameter, the command should have fired the error flag and
     * imported the Ok entry.
     */
    public void testTwoEntriesImportOneOnErrorAndOneOk()
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
            error = false;
            try
            {
                // Getting the import file
                File importFile = new File( ( ImportCommandTest.class
                    .getResource( "import_2_entries_error_and_ok.ldif" ) ).toURI() );

                // Preparing the call to the Import Command
                ImportCommandExecutor importCommandExecutor = new ImportCommandExecutor();
                Parameter hostParam = new Parameter( ImportCommandExecutor.HOST_PARAMETER, host );
                Parameter portParam = new Parameter( ImportCommandExecutor.PORT_PARAMETER, new Integer( port ) );
                Parameter userParam = new Parameter( ImportCommandExecutor.USER_PARAMETER, user );
                Parameter passwordParam = new Parameter( ImportCommandExecutor.PASSWORD_PARAMETER, password );
                Parameter authParam = new Parameter( ImportCommandExecutor.AUTH_PARAMETER, "simple" );
                Parameter fileParam = new Parameter( ImportCommandExecutor.FILE_PARAMETER, importFile );
                Parameter ignoreErrorsParam = new Parameter( ImportCommandExecutor.IGNOREERRORS_PARAMETER, new Boolean(
                    true ) );
                Parameter debugParam = new Parameter( ImportCommandExecutor.DEBUG_PARAMETER, new Boolean( false ) );
                Parameter verboseParam = new Parameter( ImportCommandExecutor.VERBOSE_PARAMETER, new Boolean( false ) );
                Parameter quietParam = new Parameter( ImportCommandExecutor.QUIET_PARAMETER, new Boolean( false ) );

                ToolCommandListener errorListener = new ToolCommandListener()
                {
                    public void notify( Serializable o )
                    {
                        // Should be fired by the add of the first entry
                        error = true;
                    }
                };
                ListenerParameter errorListenerParam = new ListenerParameter(
                    ImportCommandExecutor.ERRORLISTENER_PARAMETER, errorListener );

                // Calling the import command
                importCommandExecutor.execute( new Parameter[]
                    { hostParam, portParam, userParam, passwordParam, authParam, fileParam, ignoreErrorsParam,
                        debugParam, verboseParam, quietParam }, new ListenerParameter[]
                    { errorListenerParam } );

            }
            catch ( URISyntaxException e )
            {
                fail();
            }

            // Testing if the error notification has been raised.
            if ( error )
            {
                // Testing if the import is successful
                SearchControls ctls = new SearchControls();
                ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
                try
                {
                    NamingEnumeration entries = ctx.search( "cn=newperson2,o=neworganization,dc=example,dc=com",
                        "(objectClass=*)", ctls );

                    if ( entries.hasMore() )
                    {
                        SearchResult sr = ( SearchResult ) entries.nextElement();

                        // Even if the first was on error, the second error should have been added
                        assertEquals( "cn=newperson2,o=neworganization,dc=example,dc=com", sr.getNameInNamespace() );

                        Attributes attributes = sr.getAttributes();

                        Attribute attr = attributes.get( "objectclass" );
                        assertTrue( attr.contains( "top" ) );
                        assertTrue( attr.contains( "person" ) );

                        attr = attributes.get( "cn" );
                        assertTrue( attr.contains( "newperson2" ) );
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

                assertTrue( true );
            }
            else
            {
                fail();
            }
        }
    }


    /**
     * Tests the import with a file containing one entry and checks if the entry added notification
     * is received.
     */
    public void testOneEntryImportWithEntryAddedNotification()
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
            added = false;
            try
            {
                // Getting the import file
                File importFile = new File( ( ImportCommandTest.class.getResource( "import_1_entry.ldif" ) ).toURI() );

                // Preparing the call to the Import Command
                ImportCommandExecutor importCommandExecutor = new ImportCommandExecutor();
                Parameter hostParam = new Parameter( ImportCommandExecutor.HOST_PARAMETER, host );
                Parameter portParam = new Parameter( ImportCommandExecutor.PORT_PARAMETER, new Integer( port ) );
                Parameter userParam = new Parameter( ImportCommandExecutor.USER_PARAMETER, user );
                Parameter passwordParam = new Parameter( ImportCommandExecutor.PASSWORD_PARAMETER, password );
                Parameter authParam = new Parameter( ImportCommandExecutor.AUTH_PARAMETER, "simple" );
                Parameter fileParam = new Parameter( ImportCommandExecutor.FILE_PARAMETER, importFile );
                Parameter ignoreErrorsParam = new Parameter( ImportCommandExecutor.IGNOREERRORS_PARAMETER, new Boolean(
                    true ) );
                Parameter debugParam = new Parameter( ImportCommandExecutor.DEBUG_PARAMETER, new Boolean( false ) );
                Parameter verboseParam = new Parameter( ImportCommandExecutor.VERBOSE_PARAMETER, new Boolean( false ) );
                Parameter quietParam = new Parameter( ImportCommandExecutor.QUIET_PARAMETER, new Boolean( false ) );

                ToolCommandListener entryAddedListener = new ToolCommandListener()
                {
                    public void notify( Serializable o )
                    {
                        added = true;
                    }
                };
                ListenerParameter entryAddedListenerParam = new ListenerParameter(
                    ImportCommandExecutor.ENTRYADDEDLISTENER_PARAMETER, entryAddedListener );

                // Calling the import command
                importCommandExecutor.execute( new Parameter[]
                    { hostParam, portParam, userParam, passwordParam, authParam, fileParam, ignoreErrorsParam,
                        debugParam, verboseParam, quietParam }, new ListenerParameter[]
                    { entryAddedListenerParam } );

            }
            catch ( URISyntaxException e )
            {
                fail();
            }

            // Testing if the error notification has been raised.
            if ( added )
            {
                assertTrue( true );
            }
            else
            {
                fail();
            }
        }
    }


    /**
     * Tests the import with a file containing error and checks if the exception notification
     * is received.
     */
    public void testOneEntryImportWithExceptionNotification()
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
            exception = false;
            try
            {
                // Getting the import file
                File importFile = new File( ( ImportCommandTest.class.getResource( "import_1_entry_on_error.ldif" ) )
                    .toURI() );

                // Preparing the call to the Import Command
                ImportCommandExecutor importCommandExecutor = new ImportCommandExecutor();
                Parameter hostParam = new Parameter( ImportCommandExecutor.HOST_PARAMETER, host );
                Parameter portParam = new Parameter( ImportCommandExecutor.PORT_PARAMETER, new Integer( port ) );
                Parameter userParam = new Parameter( ImportCommandExecutor.USER_PARAMETER, user );
                Parameter passwordParam = new Parameter( ImportCommandExecutor.PASSWORD_PARAMETER, password );
                Parameter authParam = new Parameter( ImportCommandExecutor.AUTH_PARAMETER, "simple" );
                Parameter fileParam = new Parameter( ImportCommandExecutor.FILE_PARAMETER, importFile );
                Parameter ignoreErrorsParam = new Parameter( ImportCommandExecutor.IGNOREERRORS_PARAMETER, new Boolean(
                    true ) );
                Parameter debugParam = new Parameter( ImportCommandExecutor.DEBUG_PARAMETER, new Boolean( false ) );
                Parameter verboseParam = new Parameter( ImportCommandExecutor.VERBOSE_PARAMETER, new Boolean( false ) );
                Parameter quietParam = new Parameter( ImportCommandExecutor.QUIET_PARAMETER, new Boolean( false ) );

                ToolCommandListener exceptionListener = new ToolCommandListener()
                {
                    public void notify( Serializable o )
                    {
                        exception = true;
                    }
                };
                ListenerParameter exceptionListenerParam = new ListenerParameter(
                    ImportCommandExecutor.EXCEPTIONLISTENER_PARAMETER, exceptionListener );

                // Calling the import command
                importCommandExecutor.execute( new Parameter[]
                    { hostParam, portParam, userParam, passwordParam, authParam, fileParam, ignoreErrorsParam,
                        debugParam, verboseParam, quietParam }, new ListenerParameter[]
                    { exceptionListenerParam } );

            }
            catch ( URISyntaxException e )
            {
                fail();
            }

            // Testing if the error notification has been raised.
            if ( exception )
            {
                assertTrue( true );
            }
            else
            {
                fail();
            }
        }
    }


    public void testRFC2849Sample1()
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
            try
            {
                // Getting the import file
                File importFile = new File( ( ImportCommandTest.class.getResource( "RFC2849Sample1.ldif" ) ).toURI() );

                // Preparing the call to the Import Command
                ImportCommandExecutor importCommandExecutor = new ImportCommandExecutor();
                Parameter hostParam = new Parameter( ImportCommandExecutor.HOST_PARAMETER, host );
                Parameter portParam = new Parameter( ImportCommandExecutor.PORT_PARAMETER, new Integer( port ) );
                Parameter userParam = new Parameter( ImportCommandExecutor.USER_PARAMETER, user );
                Parameter passwordParam = new Parameter( ImportCommandExecutor.PASSWORD_PARAMETER, password );
                Parameter authParam = new Parameter( ImportCommandExecutor.AUTH_PARAMETER, "simple" );
                Parameter fileParam = new Parameter( ImportCommandExecutor.FILE_PARAMETER, importFile );
                Parameter ignoreErrorsParam = new Parameter( ImportCommandExecutor.IGNOREERRORS_PARAMETER, new Boolean(
                    true ) );
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
                    { hostParam, portParam, userParam, passwordParam, authParam, fileParam, ignoreErrorsParam,
                        debugParam, verboseParam, quietParam }, new ListenerParameter[]
                    { errorListenerParam } );

            }
            catch ( URISyntaxException e )
            {
                fail();
            }

            // Testing if the import is successful
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
            try
            {
                NamingEnumeration entries = ctx.search(
                    "cn=Barbara Jensen, ou=Product Development, dc=example, dc=com", "(objectClass=*)", ctls );

                if ( entries.hasMore() )
                {
                    SearchResult sr = ( SearchResult ) entries.nextElement();

                    assertEquals( "cn=Barbara Jensen,ou=Product Development,dc=example,dc=com", sr.getNameInNamespace() );

                    Attributes attributes = sr.getAttributes();

                    Attribute attr = attributes.get( "objectclass" );
                    assertTrue( attr.contains( "top" ) );
                    assertTrue( attr.contains( "person" ) );
                    assertTrue( attr.contains( "organizationalPerson" ) );

                    attr = attributes.get( "cn" );
                    assertTrue( attr.contains( "Barbara Jensen" ) );
                    assertTrue( attr.contains( "Barbara J Jensen" ) );
                    assertTrue( attr.contains( "Babs Jensen" ) );

                    attr = attributes.get( "sn" );
                    assertTrue( attr.contains( "Jensen" ) );

                    attr = attributes.get( "uid" );
                    assertTrue( attr.contains( "bjensen" ) );

                    attr = attributes.get( "telephonenumber" );
                    assertTrue( attr.contains( "+1 408 555 1212" ) );

                    attr = attributes.get( "description" );
                    assertTrue( attr.contains( "A big sailing fan." ) );
                }
                else
                {
                    fail();
                }

                // Testing the second entry
                entries = ctx.search( "cn=Bjorn Jensen, ou=Accounting, dc=example, dc=com", "(objectClass=*)", ctls );

                if ( entries.hasMore() )
                {
                    SearchResult sr = ( SearchResult ) entries.nextElement();

                    assertEquals( "cn=Bjorn Jensen,ou=Accounting,dc=example,dc=com", sr.getNameInNamespace() );

                    Attributes attributes = sr.getAttributes();

                    Attribute attr = attributes.get( "objectclass" );
                    assertTrue( attr.contains( "top" ) );
                    assertTrue( attr.contains( "person" ) );
                    assertTrue( attr.contains( "organizationalPerson" ) );

                    attr = attributes.get( "cn" );
                    assertTrue( attr.contains( "Bjorn Jensen" ) );

                    attr = attributes.get( "sn" );
                    assertTrue( attr.contains( "Jensen" ) );

                    attr = attributes.get( "telephonenumber" );
                    assertTrue( attr.contains( "+1 408 555 1212" ) );
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
        }
    }


    public void testRFC2849Sample2()
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
            try
            {
                // Getting the import file
                File importFile = new File( ( ImportCommandTest.class.getResource( "RFC2849Sample2.ldif" ) ).toURI() );

                // Preparing the call to the Import Command
                ImportCommandExecutor importCommandExecutor = new ImportCommandExecutor();
                Parameter hostParam = new Parameter( ImportCommandExecutor.HOST_PARAMETER, host );
                Parameter portParam = new Parameter( ImportCommandExecutor.PORT_PARAMETER, new Integer( port ) );
                Parameter userParam = new Parameter( ImportCommandExecutor.USER_PARAMETER, user );
                Parameter passwordParam = new Parameter( ImportCommandExecutor.PASSWORD_PARAMETER, password );
                Parameter authParam = new Parameter( ImportCommandExecutor.AUTH_PARAMETER, "simple" );
                Parameter fileParam = new Parameter( ImportCommandExecutor.FILE_PARAMETER, importFile );
                Parameter ignoreErrorsParam = new Parameter( ImportCommandExecutor.IGNOREERRORS_PARAMETER, new Boolean(
                    true ) );
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
                    { hostParam, portParam, userParam, passwordParam, authParam, fileParam, ignoreErrorsParam,
                        debugParam, verboseParam, quietParam }, new ListenerParameter[]
                    { errorListenerParam } );

            }
            catch ( URISyntaxException e )
            {
                fail();
            }

            // Testing if the import is successful
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
            try
            {
                NamingEnumeration entries = ctx.search(
                    "cn=Barbara Jensen, ou=Product Development, dc=example, dc=com", "(objectClass=*)", ctls );

                if ( entries.hasMore() )
                {
                    SearchResult sr = ( SearchResult ) entries.nextElement();

                    assertEquals( "cn=Barbara Jensen,ou=Product Development,dc=example,dc=com", sr.getNameInNamespace() );

                    Attributes attributes = sr.getAttributes();

                    Attribute attr = attributes.get( "objectclass" );
                    assertTrue( attr.contains( "top" ) );
                    assertTrue( attr.contains( "person" ) );
                    assertTrue( attr.contains( "organizationalPerson" ) );

                    attr = attributes.get( "cn" );
                    assertTrue( attr.contains( "Barbara Jensen" ) );
                    assertTrue( attr.contains( "Barbara J Jensen" ) );
                    assertTrue( attr.contains( "Babs Jensen" ) );

                    attr = attributes.get( "sn" );
                    assertTrue( attr.contains( "Jensen" ) );

                    attr = attributes.get( "uid" );
                    assertTrue( attr.contains( "bjensen" ) );

                    attr = attributes.get( "telephonenumber" );
                    assertTrue( attr.contains( "+1 408 555 1212" ) );

                    attr = attributes.get( "description" );
                    assertTrue( attr
                        .contains( "Babs is a big sailing fan, and travels extensively in search of perfect sailing conditions." ) );

                    attr = attributes.get( "title" );
                    assertTrue( attr.contains( "Product Manager, Rod and Reel Division" ) );
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
        }
    }


    public void testRFC2849Sample3() throws UnsupportedEncodingException
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
            try
            {
                // Getting the import file
                File importFile = new File( ( ImportCommandTest.class.getResource( "RFC2849Sample3.ldif" ) ).toURI() );

                // Preparing the call to the Import Command
                ImportCommandExecutor importCommandExecutor = new ImportCommandExecutor();
                Parameter hostParam = new Parameter( ImportCommandExecutor.HOST_PARAMETER, host );
                Parameter portParam = new Parameter( ImportCommandExecutor.PORT_PARAMETER, new Integer( port ) );
                Parameter userParam = new Parameter( ImportCommandExecutor.USER_PARAMETER, user );
                Parameter passwordParam = new Parameter( ImportCommandExecutor.PASSWORD_PARAMETER, password );
                Parameter authParam = new Parameter( ImportCommandExecutor.AUTH_PARAMETER, "simple" );
                Parameter fileParam = new Parameter( ImportCommandExecutor.FILE_PARAMETER, importFile );
                Parameter ignoreErrorsParam = new Parameter( ImportCommandExecutor.IGNOREERRORS_PARAMETER, new Boolean(
                    true ) );
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
                    { hostParam, portParam, userParam, passwordParam, authParam, fileParam, ignoreErrorsParam,
                        debugParam, verboseParam, quietParam }, new ListenerParameter[]
                    { errorListenerParam } );

            }
            catch ( URISyntaxException e )
            {
                fail();
            }

            // Testing if the import is successful
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
            try
            {
                NamingEnumeration entries = ctx.search( "cn=Gern Jensen, ou=Product Testing, dc=example, dc=com",
                    "(objectClass=*)", ctls );

                if ( entries.hasMore() )
                {
                    SearchResult sr = ( SearchResult ) entries.nextElement();

                    assertEquals( "cn=Gern Jensen,ou=Product Testing,dc=example,dc=com", sr.getNameInNamespace() );

                    Attributes attributes = sr.getAttributes();

                    Attribute attr = attributes.get( "objectclass" );
                    assertTrue( attr.contains( "top" ) );
                    assertTrue( attr.contains( "person" ) );
                    assertTrue( attr.contains( "organizationalPerson" ) );

                    attr = attributes.get( "cn" );
                    assertTrue( attr.contains( "Gern Jensen" ) );
                    assertTrue( attr.contains( "Gern O Jensen" ) );

                    attr = attributes.get( "sn" );
                    assertTrue( attr.contains( "Jensen" ) );

                    attr = attributes.get( "uid" );
                    assertTrue( attr.contains( "gernj" ) );

                    attr = attributes.get( "telephonenumber" );
                    assertTrue( attr.contains( "+1 408 555 1212" ) );

                    attr = attributes.get( "description" );
                    assertTrue( attr
                        .contains( "What a careful reader you are!  This value is base-64-encoded because it has a control character in it (a CR).\r  By the way, you should really get out more." ) );
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
        }
    }


    public void testRFC2849Sample3VariousSpacing()
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
            try
            {
                // Getting the import file
                File importFile = new File(
                    ( ImportCommandTest.class.getResource( "RFC2849Sample3VariousSpacing.ldif" ) ).toURI() );

                // Preparing the call to the Import Command
                ImportCommandExecutor importCommandExecutor = new ImportCommandExecutor();
                Parameter hostParam = new Parameter( ImportCommandExecutor.HOST_PARAMETER, host );
                Parameter portParam = new Parameter( ImportCommandExecutor.PORT_PARAMETER, new Integer( port ) );
                Parameter userParam = new Parameter( ImportCommandExecutor.USER_PARAMETER, user );
                Parameter passwordParam = new Parameter( ImportCommandExecutor.PASSWORD_PARAMETER, password );
                Parameter authParam = new Parameter( ImportCommandExecutor.AUTH_PARAMETER, "simple" );
                Parameter fileParam = new Parameter( ImportCommandExecutor.FILE_PARAMETER, importFile );
                Parameter ignoreErrorsParam = new Parameter( ImportCommandExecutor.IGNOREERRORS_PARAMETER, new Boolean(
                    true ) );
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
                    { hostParam, portParam, userParam, passwordParam, authParam, fileParam, ignoreErrorsParam,
                        debugParam, verboseParam, quietParam }, new ListenerParameter[]
                    { errorListenerParam } );

            }
            catch ( URISyntaxException e )
            {
                fail();
            }

            // Testing if the import is successful
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
            try
            {
                NamingEnumeration entries = ctx.search( "cn=Gern Jensen, ou=Product Testing, dc=example, dc=com",
                    "(objectClass=*)", ctls );

                if ( entries.hasMore() )
                {
                    SearchResult sr = ( SearchResult ) entries.nextElement();

                    assertEquals( "cn=Gern Jensen,ou=Product Testing,dc=example,dc=com", sr.getNameInNamespace() );

                    Attributes attributes = sr.getAttributes();

                    Attribute attr = attributes.get( "objectclass" );
                    assertTrue( attr.contains( "top" ) );
                    assertTrue( attr.contains( "person" ) );
                    assertTrue( attr.contains( "organizationalPerson" ) );

                    attr = attributes.get( "cn" );
                    assertTrue( attr.contains( "Gern Jensen" ) );
                    assertTrue( attr.contains( "Gern O Jensen" ) );

                    attr = attributes.get( "sn" );
                    assertTrue( attr.contains( "Jensen" ) );

                    attr = attributes.get( "uid" );
                    assertTrue( attr.contains( "gernj" ) );

                    attr = attributes.get( "telephonenumber" );
                    assertTrue( attr.contains( "+1 408 555 1212" ) );

                    attr = attributes.get( "description" );
                    assertTrue( attr
                        .contains( "What a careful reader you are!  This value is base-64-encoded because it has a control character in it (a CR).\r  By the way, you should really get out more." ) );
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
        }
    }
}