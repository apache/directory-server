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
package org.apache.directory.server.schema.loader.ldif;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.entry.client.DefaultClientAttribute;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.ObjectClassTypeEnum;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.schema.UsageEnum;
import org.apache.directory.shared.ldap.schema.parsers.LdapComparatorDescription;
import org.apache.directory.shared.ldap.schema.parsers.NormalizerDescription;
import org.apache.directory.shared.ldap.schema.parsers.SyntaxCheckerDescription;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.registries.DefaultSchema;
import org.apache.directory.shared.ldap.util.Base64;


/**
 * Showing how it's done ...
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SchemaEntityFactory
{
    /** Used for looking up the setRegistries(Registries) method */
    private final static Class<?>[] parameterTypes = new Class[] { Registries.class };
    
    private static final List<String> EMPTY_LIST = new ArrayList<String>();
    private static final String[] EMPTY_ARRAY = new String[] {};
    
    /** A special ClassLoader that loads a class from the bytecode attribute */
    private final AttributeClassLoader classLoader;
    
    
    public SchemaEntityFactory() throws Exception
    {
        this.classLoader = new AttributeClassLoader();
    }

    
    public Schema getSchema( Entry entry ) throws Exception
    {
        String name;
        String owner;
        String[] dependencies = EMPTY_ARRAY;
        boolean isDisabled = false;
        
        if ( entry == null )
        {
            throw new NullPointerException( "entry cannot be null" );
        }
        
        if ( entry.get( SchemaConstants.CN_AT ) == null )
        {
            throw new NullPointerException( "entry must have a valid cn attribute" );
        }
        
        name = entry.get( SchemaConstants.CN_AT ).getString();
        
        if ( entry.get( SchemaConstants.CREATORS_NAME_AT ) == null )
        {
            throw new NullPointerException( "entry must have a valid " 
                + SchemaConstants.CREATORS_NAME_AT + " attribute" );
        }
        
        owner = entry.get( SchemaConstants.CREATORS_NAME_AT ).getString();
        
        if ( entry.get( MetaSchemaConstants.M_DISABLED_AT ) != null )
        {
            String value = entry.get( MetaSchemaConstants.M_DISABLED_AT ).getString();
            value = value.toUpperCase();
            isDisabled = value.equals( "TRUE" );
        }
        
        if ( entry.get( MetaSchemaConstants.M_DEPENDENCIES_AT ) != null )
        {
            Set<String> depsSet = new HashSet<String>();
            EntryAttribute depsAttr = entry.get( MetaSchemaConstants.M_DEPENDENCIES_AT );
            
            for ( Value<?> value:depsAttr )
            {
                depsSet.add( value.getString() );
            }

            dependencies = depsSet.toArray( EMPTY_ARRAY );
        }
        
        return new DefaultSchema( name, owner, dependencies, isDisabled ){};
    }
    
    
    private SyntaxChecker getSyntaxChecker( String syntaxOid, String className, 
        EntryAttribute bytecode, Registries targetRegistries ) throws Exception
    {
        Class<?> clazz = null;
        SyntaxChecker syntaxChecker = null;
        
        if ( bytecode == null )
        {
            clazz = Class.forName( className );
        }
        else
        {
            classLoader.setAttribute( bytecode );
            clazz = classLoader.loadClass( className );
        }
        
        syntaxChecker = ( SyntaxChecker ) clazz.newInstance();

        // try now before returning to check if we can inject a Registries object
        syntaxChecker.setOid( syntaxOid );
        injectRegistries( syntaxChecker, targetRegistries );
        return syntaxChecker;
    }
    
    
    /**
     * Retrieve and load a syntaxChecker class from the DIT.
     * 
     * @param entry the entry to load the syntaxChecker from
     * @return the loaded SyntaxChecker
     * @throws NamingException if anything fails during loading
     */
    public SyntaxChecker getSyntaxChecker( Entry entry, Registries targetRegistries ) throws Exception
    {
        if ( entry == null )
        {
            throw new NullPointerException( "entry cannot be null" );
        }
        
        if ( entry.get( MetaSchemaConstants.M_FQCN_AT ) == null )
        {
            throw new NullPointerException( "entry must have a valid "
                + MetaSchemaConstants.M_FQCN_AT + " attribute" );
        }

        String className = entry.get( MetaSchemaConstants.M_FQCN_AT ).get().getString();
        String syntaxOid = entry.get( MetaSchemaConstants.M_OID_AT ).get().getString();
        return getSyntaxChecker( syntaxOid, className, entry.get( 
            MetaSchemaConstants.M_BYTECODE_AT ), targetRegistries );
    }
    
    
    public SyntaxChecker getSyntaxChecker( SyntaxCheckerDescription syntaxCheckerDescription, 
        Registries targetRegistries ) throws Exception
    {
        EntryAttribute attr = null;
        
        if ( syntaxCheckerDescription.getBytecode() != null )
        {
            byte[] bytecode = Base64.decode( syntaxCheckerDescription.getBytecode().toCharArray() );
            attr = new DefaultClientAttribute( MetaSchemaConstants.M_BYTECODE_AT, bytecode );
        }
        
        return getSyntaxChecker( syntaxCheckerDescription.getOid(), 
            syntaxCheckerDescription.getFqcn(), attr, targetRegistries );
    }
    
    
    private LdapComparator<?> getLdapComparator( String oid, String className, 
        EntryAttribute bytecode, Registries targetRegistries ) throws Exception
    {
        LdapComparator<?> comparator = null;
        Class<?> clazz = null;
        
        if ( bytecode == null ) 
        {
            clazz = Class.forName( className );
        }
        else
        {
            classLoader.setAttribute( bytecode );
            clazz = classLoader.loadClass( className );
        }
        
        comparator = ( LdapComparator<?> ) clazz.newInstance();
        comparator.setOid( oid );
        injectRegistries( comparator, targetRegistries );
        return comparator;
    }
    
    
    public LdapComparator<?> getLdapComparator( 
        LdapComparatorDescription comparatorDescription, 
        Registries targetRegistries ) throws Exception
    {
        EntryAttribute attr = null;
        
        if ( comparatorDescription.getBytecode() != null )
        { 
            byte[] bytecode = Base64.decode( comparatorDescription.getBytecode().toCharArray() );
            attr = new DefaultClientAttribute( MetaSchemaConstants.M_BYTECODE_AT, bytecode );
        }
        
        return getLdapComparator( comparatorDescription.getOid(), 
            comparatorDescription.getFqcn(), attr, targetRegistries );
    }
    
    
    /**
     * Retrieve and load a Comparator class from the DIT.
     * 
     * @param entry the entry to load the Comparator from
     * @return the loaded Comparator
     * @throws NamingException if anything fails during loading
     */
    public LdapComparator<?> getLdapComparator( Entry entry, 
        Registries targetRegistries ) throws Exception
    {
        if ( entry == null )
        {
            throw new NullPointerException( "entry cannot be null" );
        }
        
        if ( entry.get( MetaSchemaConstants.M_FQCN_AT ) == null )
        {
            throw new NullPointerException( "entry must have a valid " 
                + MetaSchemaConstants.M_FQCN_AT + " attribute" );
        }
        
        String className = entry.get( MetaSchemaConstants.M_FQCN_AT ).get().getString();
        return getLdapComparator( entry.get( MetaSchemaConstants.M_OID_AT ).toString(), 
            className, entry.get( MetaSchemaConstants.M_BYTECODE_AT ), targetRegistries );
    }
    
    
    private Normalizer getNormalizer( String oid, String className, 
        EntryAttribute bytecode, Registries targetRegistries ) throws Exception
    {
        Class<?> clazz = null;
        Normalizer normalizer = null;
        
        if ( bytecode == null )
        {
            clazz = Class.forName( className );
        }
        else
        {
            classLoader.setAttribute( bytecode );
            clazz = classLoader.loadClass( className );
        }

        normalizer = ( Normalizer ) clazz.newInstance();
        normalizer.setOid( oid );
        injectRegistries( normalizer, targetRegistries );
        return normalizer;
    }

    
    public Normalizer getNormalizer( NormalizerDescription normalizerDescription, 
        Registries targetRegistries ) throws Exception
    {
        EntryAttribute attr = null;
        
        if ( normalizerDescription.getBytecode() != null )
        {
            byte[] bytecode = Base64.decode( normalizerDescription.getBytecode().toCharArray() );
            attr = new DefaultClientAttribute( MetaSchemaConstants.M_BYTECODE_AT, bytecode );
        }
        
        return getNormalizer( normalizerDescription.getOid(), 
            normalizerDescription.getFqcn(), attr, targetRegistries );
    }
    
    
    /**
     * Retrieve and load a Normalizer class from the DIT.
     * 
     * @param entry the entry to load the Normalizer from
     * @return the loaded Normalizer
     * @throws NamingException if anything fails during loading
     */
    public Normalizer getNormalizer( Entry entry, Registries targetRegistries ) 
        throws Exception
    {
        if ( entry == null )
        {
            throw new NullPointerException( "entry cannot be null" );
        }
        
        if ( entry.get( MetaSchemaConstants.M_FQCN_AT ) == null )
        {
            throw new NullPointerException( "entry must have a valid " 
                + MetaSchemaConstants.M_FQCN_AT + " attribute" );
        }
        
        String className = entry.get( MetaSchemaConstants.M_FQCN_AT ).getString();
        String oid = entry.get( MetaSchemaConstants.M_OID_AT ).getString();
        EntryAttribute bytecode = entry.get( MetaSchemaConstants.M_BYTECODE_AT );
        return getNormalizer( oid, className, bytecode, targetRegistries );
    }
    
    
    /**
     * Uses reflection to see if a setRegistries( Registries ) method exists on the
     * object's class.  If so then the registries are dependency injected into the 
     * new schema object.
     * 
     * @param obj a schema object to have a Registries dependency injected.
     */
    private void injectRegistries( Object obj, Registries targetRegistries ) throws Exception
    {
        Method method = obj.getClass().getMethod( "setRegistries", parameterTypes );
        
        if ( method == null )
        {
            return;
        }
        
        Object[] args = new Object[] { targetRegistries };
        method.invoke( obj, args );
    }


    public LdapSyntax getSyntax( Entry entry, Registries targetRegistries, String schema ) throws NamingException
    {
        String oid = entry.get( MetaSchemaConstants.M_OID_AT ).getString();
        LdapSyntax syntax = new LdapSyntax( oid );
        syntax.setSchemaName( schema );
        
        if ( entry.get( MetaSchemaConstants.X_HUMAN_READABLE_AT ) != null )
        {
            String val = entry.get( MetaSchemaConstants.X_HUMAN_READABLE_AT ).getString();
            syntax.setHumanReadable( val.toUpperCase().equals( "TRUE" ) );
        }
        
        if ( entry.get( MetaSchemaConstants.M_DESCRIPTION_AT ) != null )
        {
            syntax.setDescription( entry.get( MetaSchemaConstants.M_DESCRIPTION_AT ).getString() ); 
        }
        
        return syntax;
    }

    
    public MatchingRule getMatchingRule( Entry entry, Registries targetRegistries, String schema ) throws NamingException
    {
        String oid = entry.get( MetaSchemaConstants.M_OID_AT ).getString();
        String syntaxOid = entry.get( MetaSchemaConstants.M_SYNTAX_AT ).getString();
        MatchingRule mr = new MatchingRule( oid );
        mr.setSyntaxOid( syntaxOid );
        mr.setSchemaName( schema );
        mr.applyRegistries( targetRegistries );
        setSchemaObjectProperties( mr, entry );
        return mr;
    }
    
    
    private List<String> getStrings( EntryAttribute attr ) throws NamingException
    {
        if ( attr == null )
        {
            return EMPTY_LIST;
        }
        
        List<String> strings = new ArrayList<String>( attr.size() );
        
        for ( Value<?> value:attr )
        {
            strings.add( value.getString() );
        }
        
        return strings;
    }
    
    
    public ObjectClass getObjectClass( Entry entry, Registries targetRegistries, String schema ) throws Exception
    {
        String oid = entry.get( MetaSchemaConstants.M_OID_AT ).getString();
        ObjectClass oc = new ObjectClass( oid );
        oc.applyRegistries( targetRegistries );
        oc.setSchemaName( schema );
        
        if ( entry.get( MetaSchemaConstants.M_SUP_OBJECT_CLASS_AT ) != null )
        {
            oc.setSuperiorOids( getStrings( entry.get( MetaSchemaConstants.M_SUP_OBJECT_CLASS_AT ) ) );
        }
        
        if ( entry.get( MetaSchemaConstants.M_MAY_AT ) != null )
        {
            oc.setMayAttributeTypeOids( getStrings( entry.get( MetaSchemaConstants.M_MAY_AT ) ) );
        }
        
        if ( entry.get( MetaSchemaConstants.M_MUST_AT ) != null )
        {
            oc.setMustAttributeTypeOids( getStrings( entry.get( MetaSchemaConstants.M_MUST_AT ) ) );
        }
        
        if ( entry.get( MetaSchemaConstants.M_TYPE_OBJECT_CLASS_AT ) != null )
        {
            String type = entry.get( MetaSchemaConstants.M_TYPE_OBJECT_CLASS_AT ).getString();
            oc.setType( ObjectClassTypeEnum.getClassType( type ) );
        }
        else
        {
            oc.setType( ObjectClassTypeEnum.STRUCTURAL );
        }
        
        setSchemaObjectProperties( oc, entry );
        
        return oc;
    }
    
    
    public AttributeType getAttributeType( Entry entry, Registries targetRegistries, String schema ) throws NamingException
    {
        String oid = entry.get( MetaSchemaConstants.M_OID_AT ).getString();
        AttributeType at = new AttributeType( oid );
        
        at.applyRegistries( targetRegistries );
        at.setSchemaName( schema );
        setSchemaObjectProperties( at, entry );
        
        if ( entry.get( MetaSchemaConstants.M_SYNTAX_AT ) != null )
        {
            at.setSyntaxOid( entry.get( MetaSchemaConstants.M_SYNTAX_AT ).getString() );
        }
        
        if ( entry.get( MetaSchemaConstants.M_EQUALITY_AT ) != null )
        {
            at.setEqualityOid( entry.get( MetaSchemaConstants.M_EQUALITY_AT ).getString() );
        }
        
        if ( entry.get( MetaSchemaConstants.M_ORDERING_AT ) != null )
        {
            at.setOrderingOid( entry.get( MetaSchemaConstants.M_ORDERING_AT ).getString() );
        }
        
        if ( entry.get( MetaSchemaConstants.M_SUBSTR_AT ) != null )
        {
            at.setSubstrOid( entry.get( MetaSchemaConstants.M_SUBSTR_AT ).getString() );
        }
        
        if ( entry.get( MetaSchemaConstants.M_SUP_ATTRIBUTE_TYPE_AT ) != null )
        {
            at.setSuperiorOid( entry.get( MetaSchemaConstants.M_SUP_ATTRIBUTE_TYPE_AT ).getString() );
        }
        
        if ( entry.get( MetaSchemaConstants.M_COLLECTIVE_AT ) != null )
        {
            String val = entry.get( MetaSchemaConstants.M_COLLECTIVE_AT ).getString();
            at.setCollective( val.equalsIgnoreCase( "TRUE" ) );
        }
        
        if ( entry.get( MetaSchemaConstants.M_SINGLE_VALUE_AT ) != null )
        {
            String val = entry.get( MetaSchemaConstants.M_SINGLE_VALUE_AT ).getString();
            at.setSingleValue( val.equalsIgnoreCase( "TRUE" ) );
        }
        
        if ( entry.get( MetaSchemaConstants.M_NO_USER_MODIFICATION_AT ) != null )
        {
            String val = entry.get( MetaSchemaConstants.M_NO_USER_MODIFICATION_AT ).getString();
            at.setCanUserModify( ! val.equalsIgnoreCase( "TRUE" ) );
        }
        
        if ( entry.get( MetaSchemaConstants.M_USAGE_AT ) != null )
        {
            at.setUsage( UsageEnum.getUsage( entry.get( MetaSchemaConstants.M_USAGE_AT ).getString() ) );
        }
        
        return at;
    }
    

    private void setSchemaObjectProperties( SchemaObject so, Entry entry ) throws NamingException
    {
        if ( entry.get( MetaSchemaConstants.M_OBSOLETE_AT ) != null )
        {
            String val = entry.get( MetaSchemaConstants.M_OBSOLETE_AT ).getString();
            so.setObsolete( val.equalsIgnoreCase( "TRUE" ) );
        }
        
        if ( entry.get( MetaSchemaConstants.M_DESCRIPTION_AT ) != null )
        {
            so.setDescription( entry.get( MetaSchemaConstants.M_DESCRIPTION_AT ).getString() ); 
        }

        EntryAttribute names = entry.get( MetaSchemaConstants.M_NAME_AT );
        
        if ( names != null )
        {
            List<String> values = new ArrayList<String>();
            
            for ( Value<?> name:names )
            {
                values.add( name.getString() );
            }
            
            so.setNames( values );
        }
    }
}
