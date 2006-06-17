/*
 *   Copyright 2006 The Apache Software Foundation
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

package org.apache.directory.server.core.trigger;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * Unit tests for TriggerService.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$
 */
public class DefaulTriggerServiceTest extends AbstractTriggerServiceTest
{

    /*
    public void testOne() throws NamingException
    {
        
        createTriggerSubentry( "triggerSubentry1", "BEFORE delete CALL \"BackupUtilities.backupDeleted\" ( $deletedEntry )" );
        createTriggerSubentry( "triggerSubentry2", "AFTER delete CALL \"Logger.logDelete\" { language \"Java\" } ( $name )" );
        
        Attributes testEntry = new BasicAttributes( "ou", "testou", true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        testEntry.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "organizationalUnit" );
        sysRoot.createSubcontext( "ou=testou", testEntry );
        
        addEntryTrigger( new LdapDN( "ou=testou" ), "AFTER delete CALL \"Audit.userDeletedAnEntry\" ( $deletedEntry, $operationPrincipal )" );
        
        sysRoot.destroySubcontext( "ou=testou" );

    }
    
    public void testTwo() throws NamingException
    {
        
        createTriggerSubentry( "myTriggerSubentry1", "AFTER delete CALL \"Logger.logDelete\" { language \"Java\" } ( $name )" );
        createTriggerSubentry( "myTriggerSubentry2", "INSTEADOF delete CALL \"Restrictions.noDelete\" ( $deletedEntry )" );
        createTriggerSubentry( "myTriggerSubentry3", "INSTEADOF add CALL \"Restrictions.noAdd\" ( $entry )" );
        
        Attributes testEntry = new BasicAttributes( "ou", "testou", true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        testEntry.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "organizationalUnit" );
        sysRoot.createSubcontext( "ou=testou", testEntry );
        
        sysRoot.destroySubcontext( "ou=testou" );

    }*/
    
    public void testThree() throws NamingException, IOException
    {
        URL url = getClass().getResource( "HelloWorldProcedure.class" );
        InputStream in = getClass().getResourceAsStream( "HelloWorldProcedure.class" );
        File file = new File( url.getFile() );
        int size = ( int ) file.length();
        byte[] buf = new byte[size];
        in.read( buf );
        in.close();
        
        Attributes attributes = new BasicAttributes( "objectClass", "top", true );
        attributes.get( "objectClass" ).add( "javaClass" );
        attributes.put( "fullyQualifiedClassName", HelloWorldProcedure.class.getName() );
        attributes.put( "byteCode", buf );
        sysRoot.createSubcontext( "fullyQualifiedClassName=" + HelloWorldProcedure.class.getName(), attributes );
        
        createTriggerSubentry( "myTriggerSubentry1", "AFTER delete CALL \"" + HelloWorldProcedure.class.getName() + "logDeleted" + "\" ( $name )" );
        
        Attributes testEntry = new BasicAttributes( "ou", "testou", true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        testEntry.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "organizationalUnit" );
        sysRoot.createSubcontext( "ou=testou", testEntry );
        
        sysRoot.destroySubcontext( "ou=testou" );

    }
}
