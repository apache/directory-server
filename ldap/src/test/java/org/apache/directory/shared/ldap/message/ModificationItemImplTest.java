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
package org.apache.directory.shared.ldap.message;

import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/**
 * Test the modificationItemImpl class
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ModificationItemImplTest
{
    /**
     * Test ModificationImpl equality
     */
    @Test
    public void testEquals()
    {
        Attribute attr = new AttributeImpl( "cn", "value" );
        attr.add( "another value" );
        
        ModificationItemImpl mod1 = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, attr );
        ModificationItemImpl mod2 = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, attr );
        
        assertEquals( mod1, mod2 );
        
        assertEquals( mod1, mod1 );
        assertEquals( mod2, mod1 );
        
        ModificationItemImpl mod3 = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, new AttributeImpl( "cn" ) );  
        ModificationItemImpl mod4 = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, new AttributeImpl( "cn" ) );
        
        assertEquals( mod3, mod4 );
    }
    
    /**
     * Test ModificationImpl difference
     */
    @Test
    public void testDifferentModificationItemImpl()
    {
        Attribute attr = new AttributeImpl( "cn", "value" );
        Attribute attr2 = new AttributeImpl( "cn", "value" );
        attr.add( "yet another value" );
        
        ModificationItemImpl mod1 = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, attr );
        ModificationItemImpl mod2= new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, attr2 );
        
        assertNotSame( mod1, mod2 );
    }
}
