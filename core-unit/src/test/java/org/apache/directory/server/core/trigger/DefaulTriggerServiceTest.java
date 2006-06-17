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


/**
 * Integration tests for TriggerService.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$
 */
public class DefaulTriggerServiceTest extends AbstractTriggerServiceTest
{
    private void loadStoredProcedure( String spClass ) throws NamingException
    {
        URL url = getClass().getResource( spClass + ".class" );
        InputStream in = getClass().getResourceAsStream( spClass + ".class" );
        File file = new File( url.getFile() );
        int size = ( int ) file.length();
        byte[] buf = new byte[size];
        try
        {
            in.read( buf );
            in.close();
        }
        catch ( IOException e )
        {
            NamingException ne = new NamingException();
            ne.setRootCause( e );
            throw ne;
        }
        
        String fullClassName = getClass().getPackage().getName() + "." + spClass;
        
        Attributes attributes = new BasicAttributes( "objectClass", "top", true );
        attributes.get( "objectClass" ).add( "javaClass" );
        attributes.put( "fullyQualifiedClassName", fullClassName );
        attributes.put( "byteCode", buf );
        
        sysRoot.createSubcontext( "fullyQualifiedClassName=" + fullClassName, attributes );
    }
    
    public void testAfterDeleteBackupDeletedEntry() throws NamingException
    {
        // Load the stored procedure to be triggered.
        loadStoredProcedure( "BackupUtilities" );
        
        // Create a container for backing up deleted entries.
        Attributes backupContext = new BasicAttributes( "ou", "backupContext", true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        backupContext.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "organizationalUnit" );
        sysRoot.createSubcontext( "ou=backupContext", backupContext );
        
        // Create the Triger Specification via the Trigger Subentry.
        createTriggerSubentry( "triggerSubentry1", "AFTER delete CALL \"" +
            "org.apache.directory.server.core.trigger.BackupUtilities.backupDeleted\" ( $name, $operationPrincipal, $deletedEntry )" );
        
        // Create a test entry which is selected by the Trigger Subentry.
        Attributes testEntry = new BasicAttributes( "ou", "testou", true );
        objectClass = new BasicAttribute( "objectClass" );
        testEntry.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "organizationalUnit" );
        sysRoot.createSubcontext( "ou=testou", testEntry );
        
        // Delete the test entry in order to fire the Trigger.
        sysRoot.destroySubcontext( "ou=testou" );
        
        // ------------------------------------------
        // The trigger should be fired at this point.
        // ------------------------------------------
        
        // Check if the Trigger really worked (backed up the deleted entry).
        assertNotNull( sysRoot.lookup( "ou=testou,ou=backupContext" ) );
    }
    
}
