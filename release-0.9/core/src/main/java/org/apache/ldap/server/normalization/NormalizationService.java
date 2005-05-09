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
package org.apache.ldap.server.normalization;


import org.apache.ldap.server.interceptor.BaseInterceptor;
import org.apache.ldap.server.interceptor.InterceptorContext;
import org.apache.ldap.server.interceptor.NextInterceptor;
import org.apache.ldap.server.invocation.*;
import org.apache.ldap.server.schema.AttributeTypeRegistry;
import org.apache.ldap.common.name.NameComponentNormalizer;
import org.apache.ldap.common.name.DnParser;
import org.apache.ldap.common.schema.AttributeType;

import javax.naming.NamingException;


/**
 * A name normalization service.  This service makes sure all relative and distinuished
 * names are normalized before calls are made against the respective interface methods
 * on the root nexus.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NormalizationService extends BaseInterceptor
{
    private DnParser parser;


    public void init( InterceptorContext context ) throws NamingException
    {
        AttributeTypeRegistry attributeRegistry = context.getGlobalRegistries().getAttributeTypeRegistry();

        parser = new DnParser( new PerComponentNormalizer( attributeRegistry ) );
    }


    public void destroy()
    {
    }


    // ------------------------------------------------------------------------
    // Normalize all Name based arguments for BackingStore interface operations
    // ------------------------------------------------------------------------


    protected void process( NextInterceptor nextInterceptor, Add call ) throws NamingException
    {
        synchronized( parser )
        {
            call.setNormalizedName( parser.parse( call.getNormalizedName().toString() ) );
        }

        super.process( nextInterceptor, call );
    }


    protected void process( NextInterceptor nextInterceptor, Delete call ) throws NamingException
    {
        synchronized( parser )
        {
            call.setName( parser.parse( call.getName().toString() ) );
        }

        super.process( nextInterceptor, call );
    }


    protected void process( NextInterceptor nextInterceptor, Modify call ) throws NamingException
    {
        synchronized( parser )
        {
            call.setName( parser.parse( call.getName().toString() ) );
        }

        super.process( nextInterceptor, call );
    }


    protected void process( NextInterceptor nextInterceptor, ModifyMany call ) throws NamingException
    {
        synchronized( parser )
        {
            call.setName( parser.parse( call.getName().toString() ) );
        }

        super.process( nextInterceptor, call );
    }


    protected void process( NextInterceptor nextInterceptor, ModifyRN call ) throws NamingException
    {
        synchronized( parser )
        {
            call.setName( parser.parse( call.getName().toString() ) );
        }

        super.process( nextInterceptor, call );
    }


    protected void process( NextInterceptor nextInterceptor, Move call ) throws NamingException
    {
        synchronized( parser )
        {
            call.setName( parser.parse( call.getName().toString() ) );

            call.setNewParentName( parser.parse( call.getNewParentName().toString() ) );
        }

        super.process( nextInterceptor, call );
    }


    protected void process( NextInterceptor nextInterceptor, MoveAndModifyRN call ) throws NamingException
    {
        synchronized( parser )
        {
            call.setName( parser.parse( call.getName().toString() ) );

            call.setNewParentName( parser.parse( call.getNewParentName().toString() ) );
        }

        super.process( nextInterceptor, call );
    }


    protected void process( NextInterceptor nextInterceptor, Search call ) throws NamingException
    {
        synchronized( parser )
        {
            call.setBaseName( parser.parse( call.getBaseName().toString() ) );
        }

        super.process( nextInterceptor, call );
    }


    protected void process( NextInterceptor nextInterceptor, HasEntry call ) throws NamingException
    {
        synchronized( parser )
        {
            call.setName( parser.parse( call.getName().toString() ) );
        }

        super.process( nextInterceptor, call );
    }


    protected void process( NextInterceptor nextInterceptor, IsSuffix call ) throws NamingException
    {
        synchronized( parser )
        {
            call.setName( parser.parse( call.getName().toString() ) );
        }

        super.process( nextInterceptor, call );
    }


    protected void process( NextInterceptor nextInterceptor, List call ) throws NamingException
    {
        synchronized( parser )
        {
            call.setBaseName( parser.parse( call.getBaseName().toString() ) );
        }

        super.process( nextInterceptor, call );
    }


    protected void process( NextInterceptor nextInterceptor, Lookup call ) throws NamingException
    {
        synchronized( parser )
        {
            call.setName( parser.parse( call.getName().toString() ) );
        }

        super.process( nextInterceptor, call );
    }


    protected void process( NextInterceptor nextInterceptor, LookupWithAttrIds call ) throws NamingException
    {
        synchronized( parser )
        {
            call.setName( parser.parse( call.getName().toString() ) );
        }

        super.process( nextInterceptor, call );
    }


    // ------------------------------------------------------------------------
    // Normalize all Name based arguments for other interface operations
    // ------------------------------------------------------------------------


    protected void process( NextInterceptor nextInterceptor, GetMatchedDN call ) throws NamingException
    {
        synchronized( parser )
        {
            call.setName( parser.parse( call.getName().toString() ) );
        }

        super.process( nextInterceptor, call );
    }


    protected void process( NextInterceptor nextInterceptor, GetSuffix call ) throws NamingException
    {
        synchronized( parser )
        {
            call.setName( parser.parse( call.getName().toString() ) );
        }

        super.process( nextInterceptor, call );
    }



    /**
     * A normalizer that normalizes each name component specifically according to
     * the attribute type of the name component.
     */
    class PerComponentNormalizer implements NameComponentNormalizer
    {
        /** the attribute type registry we use to lookup component normalizers */
        private final AttributeTypeRegistry registry;


        /**
         * Creates a name component normalizer that looks up normalizers using
         * an AttributeTypeRegistry.
         *
         * @param registry the attribute type registry to get normalizers
         */
        public PerComponentNormalizer( AttributeTypeRegistry registry )
        {
            this.registry = registry;
        }


        public String normalizeByName( String name, String value ) throws NamingException
        {
            AttributeType type = registry.lookup( name );

            return ( String ) type.getEquality().getNormalizer().normalize( value );
        }


        public String normalizeByOid( String oid, String value ) throws NamingException
        {
            AttributeType type = registry.lookup( oid );

            return ( String ) type.getEquality().getNormalizer().normalize( value );
        }
    }
}
