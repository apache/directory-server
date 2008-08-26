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
package org.apache.directory.mitosis.configuration;


import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.util.regex.Pattern;

import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A {@link PropertyEditor} that converts strings into ReplicaIds
 * and vice versa.
 *
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev: 95 $, $Date: 2006-09-16 13:04:28 +0200 (Sat, 16 Sep 2006) $
 */
public class ReplicaIdPropertyEditor extends PropertyEditorSupport
{
    /** The replica pattern. */
    private static final Pattern REPLICA_ID_PATTERN = Pattern.compile( "[-_A-Z0-9]{1,16}" );
    
    public ReplicaIdPropertyEditor()
    {
        super();
    }


    public ReplicaIdPropertyEditor( Object source )
    {
        super( source );
    }


    /**
     * Check a new instance of ReplicaId. The id must be a String 
     * which respect the pattern :
     * 
     * [-_a-zA-Z0-9]*
     * 
     * and must be between 1 and 16 chars length
     *
     * @param id The replica to check
     * @return true if the replicaId is well formed
     */
    public static boolean check( String id )
    {
        if ( StringTools.isEmpty( id ) )
        {
            throw new IllegalArgumentException( "Empty ID: " + id );
        }

        String tmpId = id.trim().toUpperCase();

        if ( !REPLICA_ID_PATTERN.matcher( tmpId ).matches() )
        {
            return false;
        }

        return true;
    }

    
    public String getAsText()
    {
        Object val = getValue();
        
        if ( val == null )
        {
            return "";
        }
        else
        {
            return val.toString();
        }
    }


    public void setAsText( String text ) throws IllegalArgumentException
    {
        text = text.trim();
        
        if ( text.length() == 0 )
        {
            setValue( null );
        }
        else
        {
            if ( check( text ) )
            {
                setValue( text );
            }
            else
            {
                setValue( null );
            }        
        }
    }
}
