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

package org.apache.directory.shared.ldap.codec.extended.operations;


import java.nio.ByteBuffer;
import java.util.List;

import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * A bean representing a Stored Procedure Call Extended Operation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoredProcedureCall extends AbstractAsn1Object
{

    private String name;

    /** Language/Scheme option */
    private String languageScheme;

    /**  Search Context option */
    private SearchContext searchContext;

    private List/*<StoredProcedureParameter>*/parameters;

    private StoredProcedureParameter currentParameter;

    /**
     * TODO: Add more length variables.
     */

    public StoredProcedureParameter getCurrentParameter()
    {
        return currentParameter;
    }


    public void setCurrentParameter( StoredProcedureParameter currentParameter )
    {
        this.currentParameter = currentParameter;
    }


    public String getLanguageScheme()
    {
        return languageScheme;
    }


    public void setLanguageScheme( String languageScheme )
    {
        this.languageScheme = languageScheme;
    }


    public String getName()
    {
        return name;
    }


    public void setName( String name )
    {
        this.name = name;
    }


    public List getParameters()
    {
        return parameters;
    }


    public void setParameters( List parameters )
    {
        this.parameters = parameters;
    }


    public SearchContext getSearchContext()
    {
        return searchContext;
    }


    public void setSearchContext( SearchContext searchContext )
    {
        this.searchContext = searchContext;
    }

    public static class SearchContext
    {
        private LdapDN context;

        private Scope scope = Scope.BASE_OBJECT;


        public LdapDN getContext()
        {
            return context;
        }


        public void setContext( LdapDN context )
        {
            this.context = context;
        }


        public Scope getScope()
        {
            return scope;
        }


        public void setScope( Scope scope )
        {
            this.scope = scope;
        }

        public static class Scope
        {
            public static final Scope BASE_OBJECT = new Scope( "baseObject" );

            public static final Scope SINGLE_LEVEL = new Scope( "scopeLevel" );

            public static final Scope WHOLE_SUBTREE = new Scope( "wholeSubtree" );

            private String name;


            private Scope( String name )
            {
                this.name = name;
            }


            public String getName()
            {
                return name;
            }


            public String toString()
            {
                return getName();
            }

        }

    }

    public static class StoredProcedureParameter
    {
        private byte[] type;

        private byte[] value;


        public byte[] getType()
        {
            return type;
        }


        public void setType( byte[] type )
        {
            this.type = type;
        }


        public byte[] getValue()
        {
            return value;
        }


        public void setValue( byte[] value )
        {
            this.value = value;
        }
    }


    public int computeLength()
    {
        // TODO Auto-generated method stub
        return 0;
    }


    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        // TODO Auto-generated method stub
        return super.encode( buffer );
    }


    public String toString()
    {
        // TODO Auto-generated method stub
        return super.toString();
    }

}
