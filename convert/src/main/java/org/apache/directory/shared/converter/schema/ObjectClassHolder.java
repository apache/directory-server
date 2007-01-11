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
package org.apache.directory.shared.converter.schema;


import org.apache.directory.shared.ldap.schema.ObjectClassTypeEnum;
import org.apache.directory.shared.ldap.util.ArrayUtils;


/**
 * A bean used to encapsulate the literal String values of an ObjectClass
 * definition found within an OpenLDAP schema configuration file.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 437016 $
 */
public class ObjectClassHolder
{
    private boolean obsolete = false;

    private String oid;
    private String description;

    private String[] names = ArrayUtils.EMPTY_STRING_ARRAY;
    private String[] superiors = ArrayUtils.EMPTY_STRING_ARRAY;
    private String[] must = ArrayUtils.EMPTY_STRING_ARRAY;
    private String[] may = ArrayUtils.EMPTY_STRING_ARRAY;

    private ObjectClassTypeEnum classType = ObjectClassTypeEnum.STRUCTURAL;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    public ObjectClassHolder(String oid)
    {
        this.oid = oid;
    }


    // ------------------------------------------------------------------------
    // Accessors and mutators
    // ------------------------------------------------------------------------

    public boolean isObsolete()
    {
        return obsolete;
    }


    public void setObsolete( boolean obsolete )
    {
        this.obsolete = obsolete;
    }


    public String getOid()
    {
        return oid;
    }


    public void setOid( String oid )
    {
        this.oid = oid;
    }


    public String getDescription()
    {
        return description;
    }


    public void setDescription( String description )
    {
        this.description = description;
    }


    public String[] getNames()
    {
        return names;
    }


    public void setNames( String[] names )
    {
        this.names = names;
    }


    public String[] getSuperiors()
    {
        return superiors;
    }


    public void setSuperiors( String[] superiors )
    {
        this.superiors = superiors;
    }


    public String[] getMust()
    {
        return must;
    }


    public void setMust( String[] must )
    {
        this.must = must;
    }


    public String[] getMay()
    {
        return may;
    }


    public void setMay( String[] may )
    {
        this.may = may;
    }


    public ObjectClassTypeEnum getClassType()
    {
        return classType;
    }


    public void setClassType( ObjectClassTypeEnum classType )
    {
        this.classType = classType;
    }


    // ------------------------------------------------------------------------
    // Object overrides
    // ------------------------------------------------------------------------

    public int hashCode()
    {
        return getOid().hashCode();
    }


    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( !( obj instanceof ObjectClassHolder ) )
        {
            return false;
        }

        return getOid().equals( ( ( ObjectClassHolder ) obj ).getOid() );
    }


    public String toString()
    {
        return getOid();
    }
}
