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


import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.LeafNode;
import org.apache.ldap.common.filter.BranchNode;
import org.apache.ldap.common.name.DnParser;
import org.apache.ldap.common.name.NameComponentNormalizer;
import org.apache.ldap.common.schema.AttributeType;
import org.apache.ldap.common.util.EmptyEnumeration;
import org.apache.ldap.server.DirectoryServiceConfiguration;
import org.apache.ldap.server.configuration.InterceptorConfiguration;
import org.apache.ldap.server.interceptor.BaseInterceptor;
import org.apache.ldap.server.interceptor.NextInterceptor;
import org.apache.ldap.server.partition.DirectoryPartitionNexus;
import org.apache.ldap.server.schema.AttributeTypeRegistry;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


/**
 * A name normalization service.  This service makes sure all relative and distinuished
 * names are normalized before calls are made against the respective interface methods
 * on {@link DirectoryPartitionNexus}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NormalizationService extends BaseInterceptor
{
    /** logger used by this class */
    private static final Logger log = LoggerFactory.getLogger( NormalizationService.class );

    /** the parser used for normalizing distinguished names */
    private DnParser parser;
    /** a filter node value normalizer and undefined node remover */
    private ValueNormalizingVisitor visitor;
    /** the attributeType registry used for normalization and determining if some filter nodes are undefined */
    private AttributeTypeRegistry registry;


    public void init( DirectoryServiceConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException
    {
        registry = factoryCfg.getGlobalRegistries().getAttributeTypeRegistry();
        NameComponentNormalizer ncn = new PerComponentNormalizer();
        parser = new DnParser( ncn );
        visitor = new ValueNormalizingVisitor( ncn );
    }


    public void destroy() {}


    // ------------------------------------------------------------------------
    // Normalize all Name based arguments for ContextPartition interface operations
    // ------------------------------------------------------------------------


    public void add( NextInterceptor nextInterceptor, String upName, Name normName, Attributes attrs ) throws NamingException
    {
        normName = parser.parse( normName.toString() );
        nextInterceptor.add( upName, normName, attrs );
    }


    public void delete( NextInterceptor nextInterceptor, Name name ) throws NamingException
    {
        name = parser.parse( name.toString() );
        nextInterceptor.delete( name );
    }


    public void modify( NextInterceptor nextInterceptor, Name name, int modOp, Attributes attrs ) throws NamingException
    {
        name = parser.parse( name.toString() );
        nextInterceptor.modify( name, modOp, attrs );
    }


    public void modify( NextInterceptor nextInterceptor, Name name, ModificationItem[] items ) throws NamingException
    {
        name = parser.parse( name.toString() );
        nextInterceptor.modify( name, items );
    }


    public void modifyRn( NextInterceptor nextInterceptor, Name name, String newRn, boolean deleteOldRn ) throws NamingException
    {
        name = parser.parse( name.toString() );
        nextInterceptor.modifyRn( name, newRn, deleteOldRn );
    }


    public void move( NextInterceptor nextInterceptor, Name name, Name newParentName ) throws NamingException
    {
        name = parser.parse( name.toString() );
        newParentName = parser.parse( newParentName.toString() );
        nextInterceptor.move( name, newParentName );
    }


    public void move( NextInterceptor nextInterceptor, Name name, Name newParentName, String newRn, boolean deleteOldRn ) throws NamingException
    {
        name = parser.parse( name.toString() );
        newParentName = parser.parse( newParentName.toString() );
        nextInterceptor.move( name, newParentName, newRn, deleteOldRn );
    }


    public NamingEnumeration search( NextInterceptor nextInterceptor,
                                     Name base, Map env, ExprNode filter,
                                     SearchControls searchCtls ) throws NamingException
    {
        base = parser.parse( base.toString() );

        if ( filter.isLeaf() )
        {
            LeafNode ln = ( LeafNode ) filter;
            if ( ! registry.hasAttributeType( ln.getAttribute() ) )
            {
                StringBuffer buf = new StringBuffer();
                buf.append( "undefined filter based on undefined attributeType '" );
                buf.append( ln.getAttribute() );
                buf.append( "' not evaluted at all.  Returning empty enumeration." );
                log.warn( buf.toString() );
                return new EmptyEnumeration();
            }
        }

        filter.accept( visitor );

        // check that after pruning we have valid branch node at the top
        if ( ! filter.isLeaf() )
        {
            BranchNode child = ( BranchNode ) filter;

            // if the remaining filter branch node has no children return an empty enumeration
            if ( child.getChildren().size() == 0 )
            {
                log.warn( "Undefined branchnode filter without child nodes not evaluted at all.  Returning empty enumeration." );
                return new EmptyEnumeration();
            }

            // now for AND & OR nodes with a single child left replace them with their child
            if ( child.getChildren().size() == 1 && child.getOperator() != BranchNode.NOT )
            {
                filter = child.getChild();
            }
        }
        return nextInterceptor.search( base, env, filter, searchCtls );
    }


    public boolean hasEntry( NextInterceptor nextInterceptor, Name name ) throws NamingException
    {
        name = parser.parse( name.toString() );
        return nextInterceptor.hasEntry( name );
    }


    public boolean isSuffix( NextInterceptor nextInterceptor, Name name ) throws NamingException
    {
        name = parser.parse( name.toString() );
        return nextInterceptor.isSuffix( name );
    }


    public NamingEnumeration list( NextInterceptor nextInterceptor, Name base ) throws NamingException
    {
        base = parser.parse( base.toString() );
        return nextInterceptor.list( base );
    }


    public Attributes lookup( NextInterceptor nextInterceptor, Name name ) throws NamingException
    {
        name = parser.parse( name.toString() );
        return nextInterceptor.lookup( name );
    }


    public Attributes lookup( NextInterceptor nextInterceptor, Name name, String[] attrIds ) throws NamingException
    {
        name = parser.parse( name.toString() );
        return nextInterceptor.lookup( name, attrIds );
    }


    // ------------------------------------------------------------------------
    // Normalize all Name based arguments for other interface operations
    // ------------------------------------------------------------------------


    public Name getMatchedName( NextInterceptor nextInterceptor, Name name, boolean normalized ) throws NamingException
    {
        name = parser.parse( name.toString() );
        return nextInterceptor.getMatchedName( name, normalized );
    }


    public Name getSuffix( NextInterceptor nextInterceptor, Name name, boolean normalized ) throws NamingException
    {
        name = parser.parse( name.toString() );
        return nextInterceptor.getSuffix( name, normalized );
    }


    public boolean compare( NextInterceptor next, Name name, String oid, Object value ) throws NamingException
    {
        name = parser.parse( name.toString() );
        return next.compare( name, oid, value );
    }


    /**
     * A normalizer that normalizes each name component specifically according to
     * the attribute type of the name component.
     */
    private class PerComponentNormalizer implements NameComponentNormalizer
    {
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


        public boolean isDefined( String id )
        {
            return registry.hasAttributeType( id );
        }
    }
}
