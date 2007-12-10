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
package org.apache.directory.server.core.normalization;


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.*;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.ConcreteNameComponentNormalizer;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;
import org.apache.directory.shared.ldap.schema.OidNormalizer;
import org.apache.directory.shared.ldap.util.EmptyEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import java.util.Map;


/**
 * A name normalization service.  This service makes sure all relative and distinuished
 * names are normalized before calls are made against the respective interface methods
 * on {@link PartitionNexus}.
 *
 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NormalizationInterceptor extends BaseInterceptor
{
    /** logger used by this class */
    private static final Logger LOG = LoggerFactory.getLogger( NormalizationInterceptor.class );

    /** a filter node value normalizer and undefined node remover */
    private NormalizingVisitor normVisitor;

    /** The association between attributeTypes and their normalizers */
    private Map<String, OidNormalizer> attrNormalizers; 

    /**
     * Initialize the registries, normalizers. 
     */
    public void init( DirectoryService directoryService ) throws NamingException
    {
        OidRegistry oidRegistry = directoryService.getRegistries().getOidRegistry();
        AttributeTypeRegistry attributeRegistry = directoryService.getRegistries().getAttributeTypeRegistry();
        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( attributeRegistry, oidRegistry );
        normVisitor = new NormalizingVisitor( ncn, oidRegistry );
        //expVisitor = new ExpandingVisitor( attributeRegistry );
        attrNormalizers = attributeRegistry.getNormalizerMapping();
    }

    /**
     * The destroy method does nothing
     */
    public void destroy()
    {
    }

    // ------------------------------------------------------------------------
    // Normalize all Name based arguments for ContextPartition interface operations
    // ------------------------------------------------------------------------

    public void add( NextInterceptor nextInterceptor, AddOperationContext opContext ) throws NamingException
    {
        opContext.getDn().normalize( attrNormalizers );
        nextInterceptor.add( opContext );
    }


    public void delete( NextInterceptor nextInterceptor, DeleteOperationContext opContext ) throws NamingException
    {
        opContext.getDn().normalize( attrNormalizers );
        nextInterceptor.delete( opContext );
    }


    public void modify( NextInterceptor nextInterceptor, ModifyOperationContext opContext ) throws NamingException
    {
        opContext.getDn().normalize( attrNormalizers );
        nextInterceptor.modify( opContext );
    }


    public void rename( NextInterceptor nextInterceptor, RenameOperationContext opContext ) throws NamingException
    {
        LdapDN rdn = new LdapDN();
        rdn.add( opContext.getNewRdn() );
        rdn.normalize( attrNormalizers );
        opContext.setNewRdn( rdn.getRdn() );

        opContext.getDn().normalize( attrNormalizers );
        nextInterceptor.rename( opContext );
    }


    public void move( NextInterceptor nextInterceptor, MoveOperationContext opContext ) throws NamingException
    {
        opContext.getDn().normalize( attrNormalizers );
        opContext.getParent().normalize( attrNormalizers);
        nextInterceptor.move( opContext );
    }


    public void moveAndRename( NextInterceptor nextInterceptor, MoveAndRenameOperationContext opContext )
        throws NamingException
    {
        LdapDN rdn = new LdapDN();
        rdn.add( opContext.getNewRdn() );
        rdn.normalize( attrNormalizers );
        opContext.setNewRdn( rdn.getRdn() );

        opContext.getDn().normalize( attrNormalizers );
        opContext.getParent().normalize( attrNormalizers );
        nextInterceptor.moveAndRename( opContext );
    }


    public NamingEnumeration<SearchResult> search( NextInterceptor nextInterceptor, SearchOperationContext opContext ) throws NamingException
    {
        ExprNode filter = opContext.getFilter();
        opContext.getDn().normalize( attrNormalizers );
        ExprNode result = ( ExprNode ) filter.accept( normVisitor );

        if ( result == null )
        {
            LOG.warn( "undefined filter based on undefined attributeType not evaluted at all.  Returning empty enumeration." );
            return new EmptyEnumeration<SearchResult>();
        }
        else
        {
            opContext.setFilter( result );
            return nextInterceptor.search( opContext );
        }
    }


    public boolean hasEntry( NextInterceptor nextInterceptor, EntryOperationContext opContext ) throws NamingException
    {
        opContext.getDn().normalize( attrNormalizers );
        return nextInterceptor.hasEntry( opContext );
    }


    public NamingEnumeration<SearchResult> list( NextInterceptor nextInterceptor, ListOperationContext opContext ) throws NamingException
    {
        opContext.getDn().normalize( attrNormalizers );
        return nextInterceptor.list( opContext );
    }


    public Attributes lookup( NextInterceptor nextInterceptor, LookupOperationContext opContext ) throws NamingException
    {
        opContext.getDn().normalize( attrNormalizers );
        return nextInterceptor.lookup( opContext );
    }


    // ------------------------------------------------------------------------
    // Normalize all Name based arguments for other interface operations
    // ------------------------------------------------------------------------

    public LdapDN getMatchedName ( NextInterceptor nextInterceptor, GetMatchedNameOperationContext opContext ) throws NamingException
    {
        opContext.getDn().normalize( attrNormalizers );
        return nextInterceptor.getMatchedName( opContext );
    }


    public LdapDN getSuffix ( NextInterceptor nextInterceptor, GetSuffixOperationContext opContext ) throws NamingException
    {
        opContext.getDn().normalize( attrNormalizers );
        return nextInterceptor.getSuffix( opContext );
    }


    public boolean compare( NextInterceptor next, CompareOperationContext opContext ) throws NamingException
    {
        opContext.getDn().normalize( attrNormalizers );
        return next.compare( opContext );
    }
    
    
    public void bind( NextInterceptor next, BindOperationContext opContext )  throws NamingException
    {
        opContext.getDn().normalize( attrNormalizers );
        next.bind( opContext );
    }


    public void addContextPartition( NextInterceptor next, AddContextPartitionOperationContext opContext ) throws NamingException
    {
        next.addContextPartition( opContext );
    }


    public void removeContextPartition( NextInterceptor next, RemoveContextPartitionOperationContext opContext ) throws NamingException
    {
        opContext.getDn().normalize( attrNormalizers );
        next.removeContextPartition( opContext );
    }
}
