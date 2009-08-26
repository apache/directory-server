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
package org.apache.directory.server.core.schema;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.core.entry.DefaultServerAttribute;
import org.apache.directory.server.core.entry.ServerAttribute;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
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
    
    /** Used for looking up the setSyntaxOid(String) method */
    private final static Class<?>[] setOidParameterTypes = new Class[] { String.class };
    
    private static final List<String> EMPTY_LIST = new ArrayList<String>();
    private static final String[] EMPTY_ARRAY = new String[] {};
    
    /** Used for dependency injection of Registries via setter into schema objects */
    private final Registries bootstrapRegistries;
    /** A special ClassLoader that loads a class from the bytecode attribute */
    private final AttributeClassLoader classLoader;
    private final AttributeType oidAT;
    private final AttributeType byteCodeAT;
    
    
    public SchemaEntityFactory( Registries bootstrapRegistries ) throws NamingException
    {
        this.bootstrapRegistries = bootstrapRegistries;
        this.classLoader = new AttributeClassLoader();
        this.oidAT = bootstrapRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_OID_AT );
        this.byteCodeAT = bootstrapRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_BYTECODE_AT );
    }

    
    public Schema getSchema( ServerEntry entry ) throws NamingException
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
        
        return new AbstractSchema( name, owner, dependencies, isDisabled ){};
    }
    
    
    private SyntaxChecker getSyntaxChecker( String syntaxOid, String className, EntryAttribute bytecode, Registries targetRegistries )
        throws NamingException
    {
        Class<?> clazz = null;
        SyntaxChecker syntaxChecker = null;
        
        try
        {
            if ( bytecode == null )
            {
                clazz = Class.forName( className );
            }
            else
            {
                classLoader.setAttribute( bytecode );
                clazz = classLoader.loadClass( className );
            }
        }
        catch ( ClassNotFoundException e )
        {
            LdapNamingException ne = new LdapNamingException( 
                "Normalizer class "+ className + " was not found", ResultCodeEnum.OTHER );
            ne.setRootCause( e );
            throw ne;
        }
        
        try
        {
            syntaxChecker = ( SyntaxChecker ) clazz.newInstance();
        }
        catch ( InstantiationException e )
        {
            LdapNamingException ne = new LdapNamingException( "Failed to instantiate SyntaxChecker class "+ className 
                + ".\nCheck that a default constructor exists for the class.", ResultCodeEnum.OTHER );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( IllegalAccessException e )
        {
            LdapNamingException ne = new LdapNamingException( "Failed to instantiate SyntaxChecker class "+ className 
                + ".\nCheck that a **PUBLIC** accessible default constructor exists for the class.", 
                ResultCodeEnum.OTHER );
            ne.setRootCause( e );
            throw ne;
        }

        // try now before returning to check if we can inject a Registries object
        injectRegistries( syntaxChecker, targetRegistries );
        injectOid( syntaxOid, syntaxChecker );
        return syntaxChecker;
    }
    
    
    /**
     * Retrieve and load a syntaxChecker class from the DIT.
     * 
     * @param entry the entry to load the syntaxChecker from
     * @return the loaded SyntaxChecker
     * @throws NamingException if anything fails during loading
     */
    public SyntaxChecker getSyntaxChecker( ServerEntry entry, Registries targetRegistries ) throws NamingException
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
        String syntaxOid = entry.get( oidAT ).get().getString();
        return getSyntaxChecker( syntaxOid, className, entry.get( byteCodeAT ), 
            targetRegistries );
    }
    
    
    public SyntaxChecker getSyntaxChecker( SyntaxCheckerDescription syntaxCheckerDescription, 
        Registries targetRegistries ) throws NamingException
    {
        ServerAttribute attr = null;
        
        if ( syntaxCheckerDescription.getBytecode() != null )
        {
            byte[] bytecode = Base64.decode( syntaxCheckerDescription.getBytecode().toCharArray() );
            AttributeType byteCodeAT = targetRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_BYTECODE_AT );
            attr = new DefaultServerAttribute( byteCodeAT, bytecode );
        }
        
        return getSyntaxChecker( syntaxCheckerDescription.getOid(), 
            syntaxCheckerDescription.getFqcn(), attr, targetRegistries );
    }
    
    
    private LdapComparator<?> getLdapComparator( String className, EntryAttribute bytecode, Registries targetRegistries ) 
        throws NamingException
    {
        LdapComparator<?> comparator = null;
        Class<?> clazz = null;
        
        if ( bytecode == null ) 
        {
            try
            {
                clazz = Class.forName( className );
            }
            catch ( ClassNotFoundException e )
            {
                LdapNamingException ne = new LdapNamingException( "Comparator class "+ className + " was not found",
                    ResultCodeEnum.OTHER );
                ne.setRootCause( e );
                throw ne;
            }
        }
        else
        {
            classLoader.setAttribute( bytecode );
            
            try
            {
                clazz = classLoader.loadClass( className );
            }
            catch ( ClassNotFoundException e )
            {
                LdapNamingException ne = new LdapNamingException( "Comparator class "+ className + " was not found",
                    ResultCodeEnum.OTHER );
                ne.setRootCause( e );
                throw ne;
            }
        }
        
        try
        {
            comparator = ( LdapComparator<?> ) clazz.newInstance();
        }
        catch ( InstantiationException e )
        {
            NamingException ne = new NamingException( "Failed to instantiate comparator class "+ className 
                + ".\nCheck that a default constructor exists for the class." );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( IllegalAccessException e )
        {
            NamingException ne = new NamingException( "Failed to instantiate comparator class "+ className 
                + ".\nCheck that a **PUBLIC** accessible default constructor exists for the class." );
            ne.setRootCause( e );
            throw ne;
        }
        
        injectRegistries( comparator, targetRegistries );
        return comparator;
    }
    
    
    public LdapComparator<?> getLdapComparator( LdapComparatorDescription comparatorDescription, Registries targetRegistries ) 
        throws NamingException
    {
        ServerAttribute attr = null;
        
        if ( comparatorDescription.getBytecode() != null )
        { 
            byte[] bytecode = Base64.decode( comparatorDescription.getBytecode().toCharArray() );
            AttributeType byteCodeAT = targetRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_BYTECODE_AT );
            attr = new DefaultServerAttribute( byteCodeAT, bytecode );
        }
        
        return getLdapComparator( comparatorDescription.getFqcn(), attr, targetRegistries );
    }
    
    
    /**
     * Retrieve and load a Comparator class from the DIT.
     * 
     * @param entry the entry to load the Comparator from
     * @return the loaded Comparator
     * @throws NamingException if anything fails during loading
     */
    public LdapComparator<?> getLdapComparator( ServerEntry entry, Registries targetRegistries ) throws NamingException
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
        return getLdapComparator( className, entry.get( MetaSchemaConstants.M_BYTECODE_AT ), targetRegistries );
    }
    
    
    private Normalizer getNormalizer( String className, EntryAttribute bytecode, Registries targetRegistries ) 
        throws NamingException
    {
        Class<?> clazz = null;
        Normalizer normalizer = null;
        
        try
        {
            if ( bytecode == null )
            {
                clazz = Class.forName( className );
            }
            else
            {
                classLoader.setAttribute( bytecode );
                clazz = classLoader.loadClass( className );
            }
        }
        catch ( ClassNotFoundException e )
        {
            LdapNamingException ne = new LdapNamingException( 
                "Normalizer class "+ className + " was not found", ResultCodeEnum.OTHER );
            ne.setRootCause( e );
            throw ne;
        }
        
        try
        {
            normalizer = ( Normalizer ) clazz.newInstance();
        }
        catch ( InstantiationException e )
        {
            LdapNamingException ne = new LdapNamingException( "Failed to instantiate normalizer class "+ className 
                + ".\nCheck that a default constructor exists for the class.", ResultCodeEnum.OTHER );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( IllegalAccessException e )
        {
            LdapNamingException ne = new LdapNamingException( "Failed to instantiate normalizer class "+ className 
                + ".\nCheck that a **PUBLIC** accessible default constructor exists for the class.", 
                ResultCodeEnum.OTHER );
            ne.setRootCause( e );
            throw ne;
        }

        // try now before returning to check if we can inject a Registries object
        injectRegistries( normalizer, targetRegistries );
        return normalizer;
    }

    
    public Normalizer getNormalizer( NormalizerDescription normalizerDescription, Registries targetRegistries )
        throws NamingException
    {
        ServerAttribute attr = null;
        
        if ( normalizerDescription.getBytecode() != null )
        {
            byte[] bytecode = Base64.decode( normalizerDescription.getBytecode().toCharArray() );
            AttributeType byteCodeAT = targetRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_BYTECODE_AT );
            attr = new DefaultServerAttribute( byteCodeAT, bytecode );
        }
        
        return getNormalizer( normalizerDescription.getFqcn(), attr, targetRegistries );
    }
    
    
    /**
     * Retrieve and load a Normalizer class from the DIT.
     * 
     * @param entry the entry to load the Normalizer from
     * @return the loaded Normalizer
     * @throws NamingException if anything fails during loading
     */
    public Normalizer getNormalizer( ServerEntry entry, Registries targetRegistries ) throws NamingException
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
        return getNormalizer( className, entry.get( MetaSchemaConstants.M_BYTECODE_AT ), targetRegistries );
    }
    
    
    /**
     * Uses reflection to see if a setRegistries( Registries ) method exists on the
     * object's class.  If so then the registries are dependency injected into the 
     * new schema object.
     * 
     * @param obj a schema object to have a Registries dependency injected.
     */
    private void injectRegistries( Object obj, Registries targetRegistries ) throws NamingException
    {
        String className = obj.getClass().getName();
        
        try
        {
            Method method = obj.getClass().getMethod( "setRegistries", parameterTypes );
            
            if ( method == null )
            {
                return;
            }
            
            Object[] args = new Object[] { this.bootstrapRegistries };
            method.invoke( obj, args );
        }
        catch ( SecurityException e )
        {
            NamingException ne = new NamingException( "SyntaxChecker class "+ className 
                + " could not have the Registries dependency injected." );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( NoSuchMethodException e )
        {
            // this is ok since not every object may have setRegistries()
        }
        catch ( IllegalArgumentException e )
        {
            NamingException ne = new NamingException( "SyntaxChecker class "+ className 
                + " could not have the Registries dependency injected." );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( IllegalAccessException e )
        {
            NamingException ne = new NamingException( "SyntaxChecker class "+ className 
                + " could not have the Registries dependency injected." );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( InvocationTargetException e )
        {
            NamingException ne = new NamingException( "SyntaxChecker class "+ className 
                + " could not have the Registries dependency injected." );
            ne.setRootCause( e );
            throw ne;
        }
    }


    /**
     * Uses reflection to see if a setSyntaxOid( String ) method exists 
     * on the object's class.  If so then the oid dependency is injected into the 
     * new SyntaxChecker.
     * 
     * @param obj a schema object to have a oid dependency injected.
     */
    private void injectOid( String syntaxOid, SyntaxChecker checker ) throws NamingException
    {
        String className = checker.getClass().getName();
        
        try
        {
            Method method = checker.getClass().getMethod( "setSyntaxOid", setOidParameterTypes );
            
            if ( method == null )
            {
                return;
            }
            
            Object[] args = new Object[] { syntaxOid};
            method.invoke( checker, args );
        }
        catch ( SecurityException e )
        {
            NamingException ne = new NamingException( "SyntaxChecker class "+ className 
                + " could not have the oid dependency injected." );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( NoSuchMethodException e )
        {
            // this is ok since not every object may have setSyntaxOid()
        }
        catch ( IllegalArgumentException e )
        {
            NamingException ne = new NamingException( "SyntaxChecker class "+ className 
                + " could not have the oid dependency injected." );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( IllegalAccessException e )
        {
            NamingException ne = new NamingException( "SyntaxChecker class "+ className 
                + " could not have the oid dependency injected." );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( InvocationTargetException e )
        {
            NamingException ne = new NamingException( "SyntaxChecker class "+ className 
                + " could not have the oid dependency injected." );
            ne.setRootCause( e );
            throw ne;
        }
    }


    public LdapSyntax getSyntax( ServerEntry entry, Registries targetRegistries, String schema ) throws NamingException
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

    
    public MatchingRule getMatchingRule( ServerEntry entry, Registries targetRegistries, String schema ) throws NamingException
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
    
    
    public ObjectClass getObjectClass( ServerEntry entry, Registries targetRegistries, String schema ) throws NamingException
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
    
    
    public AttributeType getAttributeType( ServerEntry entry, Registries targetRegistries, String schema ) throws NamingException
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
    

    private void setSchemaObjectProperties( SchemaObject so, ServerEntry entry ) throws NamingException
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
