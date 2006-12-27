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
package org.apache.directory.shared.ldap.aci;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;

import org.apache.directory.shared.ldap.filter.ExprNode;


/**
 * Defines the items to which the access controls apply.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class ProtectedItem implements Serializable
{
    /**
     * The entry contents as a whole. In case of a family member, it also means
     * the entry content of each subordinate family member within the same
     * compound attribute. It does not necessarily include the information in
     * these entries. This element shall be ignored if the classes element is
     * present, since this latter element selects protected entries (and
     * subordinate family members) on the basis of their object class.
     */
    public static final Entry ENTRY = new Entry();

    /**
     * All user attribute type information associated with the entry, but not
     * values associated with those attributes.
     */
    public static final AllUserAttributeTypes ALL_USER_ATTRIBUTE_TYPES = new AllUserAttributeTypes();

    /**
     * All user attribute information associated with the entry, including all
     * values of all user attributes.
     */
    public static final AllUserAttributeTypesAndValues ALL_USER_ATTRIBUTE_TYPES_AND_VALUES = new AllUserAttributeTypesAndValues();


    /**
     * Creates a new instance.
     */
    protected ProtectedItem()
    {
    }

    
    /**
     * Converts this item into its string representation as stored
     * in directory.
     *
     * @param buffer the string buffer
     */
    public abstract void printToBuffer( StringBuffer buffer );

    
    /**
     * The contents of entries (possibly a family member) which are restricted
     * to those that have object class values that satisfy the predicate defined
     * by Refinement (see 12.3.5), together (in the case of an ancestor or other
     * family member) with the entry contents as a whole of each subordinate
     * family member entry; it does not necessarily include the information in
     * these entries.
     */
    public static class Classes extends ProtectedItem
    {
        private static final long serialVersionUID = -8553151906617285325L;

        private final ExprNode classes;


        /**
         * Creates a new instance.
         * 
         * @param classes
         *            refinement
         */
        public Classes(ExprNode classes)
        {
            this.classes = classes;
        }


        public ExprNode getClasses()
        {
            return classes;
        }


        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }

            if ( o instanceof Classes )
            {
                Classes that = ( Classes ) o;
                return this.classes.equals( that.classes );
            }

            return false;
        }


        public void printToBuffer( StringBuffer buffer )
        {
            buffer.append( "classes" );
            buffer.append( ' ' );
            classes.printRefinementToBuffer( buffer );
        }
    }

    /**
     * The entry contents as a whole. In case of a family member, it also means
     * the entry content of each subordinate family member within the same
     * compound attribute. It does not necessarily include the information in
     * these entries. This element shall be ignored if the classes element is
     * present, since this latter element selects protected entries (and
     * subordinate family members) on the basis of their object class.
     */
    public static class Entry extends ProtectedItem
    {
        private static final long serialVersionUID = -6971482229815999874L;


        private Entry()
        {
        }


        public String toString()
        {
            return "entry";
        }
        
        
        public void printToBuffer( StringBuffer buffer )
        {
            buffer.append( "entry" );
        }
    }

    /**
     * All user attribute type information associated with the entry, but not
     * values associated with those attributes.
     */
    public static class AllUserAttributeTypes extends ProtectedItem
    {
        private static final long serialVersionUID = 3728652941148931359L;


        private AllUserAttributeTypes()
        {
        }


        public String toString()
        {
            return "allUserAttributeTypes";
        }
        
        
        public void printToBuffer( StringBuffer buffer )
        {
            buffer.append( "allUserAttributeTypes" );
        }
    }

    /**
     * All user attribute information associated with the entry, including all
     * values of all user attributes.
     */
    public static class AllUserAttributeTypesAndValues extends ProtectedItem
    {
        private static final long serialVersionUID = 7250988885983604442L;


        private AllUserAttributeTypesAndValues()
        {
        }


        public String toString()
        {
            return "allUserAttributeTypesAndValues";
        }
        
        
        public void printToBuffer( StringBuffer buffer )
        {
            buffer.append( "allUserAttributeTypesAndValues" );
        }
    }

    /**
     * A base class for all items which protects attribute types (or its values)
     */
    private abstract static class AttributeTypeProtectedItem extends ProtectedItem
    {
        protected final Collection attributeTypes;


        /**
         * Creates a new instance.
         * 
         * @param attributeTypes
         *            the collection of attirbute IDs
         */
        protected AttributeTypeProtectedItem(Collection attributeTypes)
        {
            Collection tmp = new ArrayList();
            for ( Iterator i = attributeTypes.iterator(); i.hasNext(); )
            {
                Object val = i.next();
                if ( !String.class.isAssignableFrom( val.getClass() ) )
                {
                    throw new IllegalArgumentException( "attributeTypes contains an element which is not a string." );
                }

                tmp.add( ( ( String ) val ).toLowerCase() );
            }

            this.attributeTypes = Collections.unmodifiableCollection( tmp );
        }


        /**
         * Returns an iterator of all attribute IDs.
         */
        public Iterator iterator()
        {
            return attributeTypes.iterator();
        }


        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }

            if ( o == null )
            {
                return false;
            }

            if ( getClass().isAssignableFrom( o.getClass() ) )
            {
                AttributeTypeProtectedItem that = ( AttributeTypeProtectedItem ) o;
                return this.attributeTypes.equals( that.attributeTypes );
            }

            return false;
        }
        
        
        public void printToBuffer( StringBuffer buffer )
        {
            buffer.append( '{' );
            buffer.append( ' ' );
            
            for ( Iterator it = attributeTypes.iterator(); it.hasNext(); )
            {
                String attributeType = ( String ) it.next();
                buffer.append( attributeType );
                
                if(it.hasNext()) {
                    buffer.append( ',' );
                    buffer.append( ' ' );
                }
            }
            
            buffer.append( ' ' );
            buffer.append( '}' );
        }
    }

    /**
     * Attribute type information pertaining to specific attributes but not
     * values associated with the type.
     */
    public static class AttributeType extends AttributeTypeProtectedItem
    {
        private static final long serialVersionUID = -9039274739078220203L;


        /**
         * Creates a new instance.
         * 
         * @param attributeTypes
         *            the collection of attribute IDs.
         */
        public AttributeType(Collection attributeTypes)
        {
            super( attributeTypes );
        }


        public String toString()
        {
            return "attributeType: " + attributeTypes;
        }
        
        
        public void printToBuffer( StringBuffer buffer )
        {
            buffer.append( "attributeType" );
            buffer.append( ' ' );
            super.printToBuffer( buffer );
        }
    }

    /**
     * All attribute value information pertaining to specific attributes.
     */
    public static class AllAttributeValues extends AttributeTypeProtectedItem
    {
        private static final long serialVersionUID = -9039274739078220203L;


        /**
         * Creates a new instance.
         * 
         * @param attributeTypes
         *            the collection of attribute IDs.
         */
        public AllAttributeValues(Collection attributeTypes)
        {
            super( attributeTypes );
        }


        public String toString()
        {
            return "allAttributeValues: " + attributeTypes;
        }
        
        
        public void printToBuffer( StringBuffer buffer )
        {
            buffer.append( "allAttributeValues" );
            buffer.append( ' ' );
            super.printToBuffer( buffer );
        }
    }

    /**
     * The attribute value assertion corresponding to the current requestor. The
     * protected item selfValue applies only when the access controls are to be
     * applied with respect to a specific authenticated user. It can only apply
     * in the specific case where the attribute specified is of DN and the
     * attribute value within the specified attribute matches the DN of the
     * originator of the operation.
     */
    public static class SelfValue extends AttributeTypeProtectedItem
    {
        private static final long serialVersionUID = -7788463918070206609L;


        /**
         * Creates a new instance.
         * 
         * @param attributeTypes
         *            the collection of attribute IDs.
         */
        public SelfValue(Collection attributeTypes)
        {
            super( attributeTypes );
        }


        public String toString()
        {
            return "selfValue: " + attributeTypes;
        }
        
        
        public void printToBuffer( StringBuffer buffer )
        {
            buffer.append( "selfValue" );
            buffer.append( ' ' );
            super.printToBuffer( buffer );
        }
    }

    /**
     * A specific value of specific attributes.
     */
    public static class AttributeValue extends ProtectedItem
    {
        private static final long serialVersionUID = -258318397837951363L;

        private final Collection attributes;


        /**
         * Creates a new instance.
         * 
         * @param attributes
         *            the collection of {@link Attribute}s.
         */
        public AttributeValue(Collection attributes)
        {
            for ( Iterator i = attributes.iterator(); i.hasNext(); )
            {
                if ( !Attribute.class.isAssignableFrom( i.next().getClass() ) )
                {
                    throw new IllegalArgumentException( "attributeTypes contains an element which is not an attribute." );
                }
            }
            this.attributes = Collections.unmodifiableCollection( new ArrayList( attributes ) );
        }


        /**
         * Returns an iterator of all {@link Attribute}s.
         */
        public Iterator iterator()
        {
            return attributes.iterator();
        }


        public boolean equals( Object o )
        {
            if ( !super.equals( o ) )
            {
                return false;
            }

            if ( o instanceof AttributeValue )
            {
                AttributeValue that = ( AttributeValue ) o;
                return this.attributes.equals( that.attributes );
            }

            return false;
        }


        public String toString()
        {
            return "attributeValue: " + attributes;
        }
        
        
        public void printToBuffer( StringBuffer buffer )
        {
            buffer.append( "attributeValue" );
            buffer.append( ' ' );
           
            buffer.append( '{' );
            buffer.append( ' ' );
            
            for ( Iterator it = attributes.iterator(); it.hasNext(); )
            {
                Attribute attribute = ( Attribute ) it.next();
                buffer.append( attribute.getID() );
                buffer.append( '=' );
                try
                {
                    buffer.append( attribute.get( 0 ) );
                }
                catch ( NamingException e )
                {
                    // doesn't occur here, it is a BasicAttribute
                }
                
                if(it.hasNext()) {
                    buffer.append( ',' );
                    buffer.append( ' ' );
                }
            }
            
            buffer.append( ' ' );
            buffer.append( '}' );
        }
    }

    /**
     * Restricts the maximum number of attribute values allowed for a specified
     * attribute type. It is examined if the protected item is an attribute
     * value of the specified type and the permission sought is add. Values of
     * that attribute in the entry are counted without regard to context or
     * access control and as though the operation which adds the values were
     * successful. If the number of values in the attribute exceeds maxCount,
     * the ACI item is treated as not granting add access.
     */
    public static class MaxValueCount extends ProtectedItem
    {
        private static final long serialVersionUID = 5261651541488944572L;

        private final Collection items;


        /**
         * Creates a new instance.
         * 
         * @param items
         *            the collection of {@link MaxValueCountItem}s.
         */
        public MaxValueCount(Collection items)
        {
            for ( Iterator i = items.iterator(); i.hasNext(); )
            {
                if ( !MaxValueCountItem.class.isAssignableFrom( i.next().getClass() ) )
                {
                    throw new IllegalArgumentException( "Max value count contains a wrong element." );
                }
            }

            this.items = Collections.unmodifiableCollection( new ArrayList( items ) );
        }


        /**
         * Returns an iterator of all {@link MaxValueCountItem}s.
         */
        public Iterator iterator()
        {
            return items.iterator();
        }


        public boolean equals( Object o )
        {
            if ( !super.equals( o ) )
            {
                return false;
            }

            if ( o instanceof MaxValueCount )
            {
                MaxValueCount that = ( MaxValueCount ) o;
                return this.items.equals( that.items );
            }

            return false;
        }


        public String toString()
        {
            return "maxValueCount: " + items;
        }
        
        
        public void printToBuffer( StringBuffer buffer )
        {
            buffer.append( "maxValueCount" );
            buffer.append( ' ' );
           
            buffer.append( '{' );
            buffer.append( ' ' );
            
            for ( Iterator it = items.iterator(); it.hasNext(); )
            {
                MaxValueCountItem item = ( MaxValueCountItem ) it.next();
                item.printToBuffer( buffer );
                
                if(it.hasNext()) {
                    buffer.append( ',' );
                    buffer.append( ' ' );
                }
            }
            
            buffer.append( ' ' );
            buffer.append( '}' );
        }
    }

    /**
     * Any attribute value which matches the specified filter, i.e. for which
     * the specified filter evaluated on that attribute value would return TRUE.
     */
    public static class RangeOfValues extends ProtectedItem
    {
        private static final long serialVersionUID = -8553151906617285325L;

        private final ExprNode filter;


        /**
         * Creates a new instance.
         * 
         * @param filter
         *            the expression
         */
        public RangeOfValues(ExprNode filter)
        {
            if ( filter == null )
            {
                throw new NullPointerException( "filter" );
            }

            this.filter = filter;
        }


        /**
         * Returns the expression.
         */
        public ExprNode getFilter()
        {
            return filter;
        }


        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }

            if ( o instanceof RangeOfValues )
            {
                RangeOfValues that = ( RangeOfValues ) o;
                return this.filter.equals( that.filter );
            }

            return false;
        }


        public String toString()
        {
            StringBuffer buf = new StringBuffer();
            buf.append( "rangeOfValues: " );
            filter.printToBuffer( buf );
            return buf.toString();
        }
        
        
        public void printToBuffer( StringBuffer buffer )
        {
            buffer.append( "rangeOfValues" );
            buffer.append( ' ' );
            filter.printToBuffer( buffer );
        }
    }

    /**
     * Restricts the maximum number of immediate subordinates of the superior
     * entry to an entry being added or imported. It is examined if the
     * protected item is an entry, the permission sought is add or import, and
     * the immediate superior entry is in the same DSA as the entry being added
     * or imported. Immediate subordinates of the superior entry are counted
     * without regard to context or access control as though the entry addition
     * or importing were successful. If the number of subordinates exceeds
     * maxImmSub, the ACI item is treated as not granting add or import access.
     */
    public static class MaxImmSub extends ProtectedItem
    {
        private static final long serialVersionUID = -8553151906617285325L;

        private final int value;


        /**
         * Creates a new instance.
         * 
         * @param value
         *            The maximum number of immediate subordinates
         */
        public MaxImmSub(int value)
        {
            this.value = value;
        }


        /**
         * Returns the maximum number of immediate subordinates.
         */
        public int getValue()
        {
            return value;
        }


        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }

            if ( o instanceof MaxImmSub )
            {
                MaxImmSub that = ( MaxImmSub ) o;
                return this.value == that.value;
            }

            return false;
        }


        public String toString()
        {
            return "maxImmSub: " + value;
        }
        
        
        public void printToBuffer( StringBuffer buffer )
        {
            buffer.append( "maxImmSub" );
            buffer.append( ' ' );
            buffer.append( value );
        }
    }

    /**
     * Restricts values added to the attribute type to being values that are
     * already present in the same entry as values of the attribute valuesIn. It
     * is examined if the protected item is an attribute value of the specified
     * type and the permission sought is add. Values of the valuesIn attribute
     * are checked without regard to context or access control and as though the
     * operation which adds the values were successful. If the value to be added
     * is not present in valuesIn the ACI item is treated as not granting add
     * access.
     */
    public static class RestrictedBy extends ProtectedItem
    {
        private static final long serialVersionUID = -8157637446588058799L;

        private final Collection items;


        /**
         * Creates a new instance.
         * 
         * @param items
         *            the collection of {@link RestrictedByItem}s.
         */
        public RestrictedBy(Collection items)
        {
            for ( Iterator i = items.iterator(); i.hasNext(); )
            {
                if ( !RestrictedByItem.class.isAssignableFrom( i.next().getClass() ) )
                {
                    throw new IllegalArgumentException( "RestrictedBy items contains a wrong element." );
                }
            }

            this.items = Collections.unmodifiableCollection( new ArrayList( items ) );
        }


        /**
         * Returns an iterator of all {@link RestrictedByItem}s.
         */
        public Iterator iterator()
        {
            return items.iterator();
        }


        public boolean equals( Object o )
        {
            if ( !super.equals( o ) )
            {
                return false;
            }

            if ( o instanceof RestrictedBy )
            {
                RestrictedBy that = ( RestrictedBy ) o;
                return this.items.equals( that.items );
            }

            return false;
        }


        public String toString()
        {
            return "restrictedBy: " + items;
        }
        
        
        public void printToBuffer( StringBuffer buffer )
        {
            buffer.append( "restrictedBy" );
            buffer.append( ' ' );
            
            buffer.append( '{' );
            buffer.append( ' ' );
            
            for ( Iterator it = items.iterator(); it.hasNext(); )
            {
                RestrictedByItem item = ( RestrictedByItem ) it.next();
                item.printToBuffer( buffer );
                
                if(it.hasNext()) {
                    buffer.append( ',' );
                    buffer.append( ' ' );
                }
            }
            
            buffer.append( ' ' );
            buffer.append( '}' );
        }
    }

    /**
     * An element of {@link MaxValueCount}.
     */
    public static class MaxValueCountItem implements Serializable
    {
        private static final long serialVersionUID = 43697038363452113L;

        private String attributeType;

        private int maxCount;


        /**
         * Creates a new instance.
         * 
         * @param attributeType
         *            the attribute ID to limit the maximum count
         * @param maxCount
         *            the maximum count of the attribute allowed
         */

        public MaxValueCountItem(String attributeType, int maxCount)
        {
            this.attributeType = attributeType;
            this.maxCount = maxCount;
        }


        /**
         * Returns the attribute ID to limit the maximum count.
         */
        public String getAttributeType()
        {
            return attributeType;
        }


        /**
         * Returns the maximum count of the attribute allowed.
         */
        public int getMaxCount()
        {
            return maxCount;
        }


        public String toString()
        {
            return "attributeType=" + attributeType + ", maxCount=" + maxCount;
        }
        
        
        public void printToBuffer( StringBuffer buffer )
        {
            buffer.append( '{' );
            buffer.append( ' ' );
            
            buffer.append( "type" );
            buffer.append( ' ' );
            buffer.append( attributeType );

            buffer.append( ' ' );
            
            buffer.append( "maxCount" );
            buffer.append( ' ' );
            buffer.append( maxCount );
            
            buffer.append( ' ' );
            buffer.append( '}' );
        }
    }

    /**
     * An element of {@link RestrictedBy}.
     */
    public static class RestrictedByItem implements Serializable
    {
        private static final long serialVersionUID = 4319052153538757099L;

        private String attributeType;

        private String valuesIn;


        /**
         * Creates a new instance.
         * 
         * @param attributeType
         *            the attribute type to restrict
         * @param valuesIn
         *            the attribute type only whose values are allowed in
         *            <tt>attributeType</tt>.
         */
        public RestrictedByItem(String attributeType, String valuesIn)
        {
            this.attributeType = attributeType;
            this.valuesIn = valuesIn;
        }


        /**
         * Returns the attribute type to restrict.
         */
        public String getAttributeType()
        {
            return attributeType;
        }


        /**
         * Returns the attribute type only whose values are allowed in
         * <tt>attributeType</tt>.
         */
        public String getValuesIn()
        {
            return valuesIn;
        }


        public String toString()
        {
            return "attributeType=" + attributeType + ", valuesIn=" + valuesIn;
        }
        
        
        public void printToBuffer( StringBuffer buffer )
        {
            buffer.append( '{' );
            buffer.append( ' ' );
            
            buffer.append( "type" );
            buffer.append( ' ' );
            buffer.append( attributeType );

            buffer.append( ' ' );
            
            buffer.append( "valuesIn" );
            buffer.append( ' ' );
            buffer.append( valuesIn );
            
            buffer.append( ' ' );
            buffer.append( '}' );
        }
    }
    
}
