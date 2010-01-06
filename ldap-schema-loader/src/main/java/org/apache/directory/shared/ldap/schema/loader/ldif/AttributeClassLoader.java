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
package org.apache.directory.shared.ldap.schema.loader.ldif;


import javax.naming.NamingException;
import javax.naming.directory.InvalidAttributeValueException;

import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;


/**
 * A class loader that loads classes from an attribute within an entry.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$ $Date$
 */
public class AttributeClassLoader extends ClassLoader
{
    public EntryAttribute attribute;
    

    public AttributeClassLoader()
    {
        super( AttributeClassLoader.class.getClassLoader() );
    }
    
    
    public void setAttribute( EntryAttribute attribute ) throws NamingException
    {
        if ( attribute.isHR() )
        {
            throw new InvalidAttributeValueException( "The attribute must be binary" );
        }
        
        this.attribute = attribute;
    }

    
    public Class<?> findClass( String name ) throws ClassNotFoundException
    {
        byte[] classBytes = null;
        
        Value<?> value = attribute.get();
        
        if ( value.isBinary() )
        {
            classBytes = value.getBytes();

            return defineClass( name, classBytes, 0, classBytes.length );
        }
        else
        {
            throw new ClassNotFoundException( "Failed to access attribute bytes." );
        }
    }
}
