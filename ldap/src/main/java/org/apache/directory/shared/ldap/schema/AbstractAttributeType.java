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
package org.apache.directory.shared.ldap.schema;


import java.io.Serializable;


/**
 * Attribute specification bean used to store the schema information for an
 * attributeType definition.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class AbstractAttributeType
    extends AbstractSchemaObject
    implements Serializable, AttributeType
{
    // ------------------------------------------------------------------------
    // Specification Attributes 
    // ------------------------------------------------------------------------

    /** whether or not this type is single valued */
    private boolean isSingleValue = false;
    /** whether or not this type is a collective attribute */
    private boolean isCollective = false;
    /** whether or not this type can be modified by directory users */
    private boolean canUserModify = true;
    /** the usage for this attributeType */
    private UsageEnum usage = UsageEnum.USERAPPLICATIONS;
    /** the length of this attribute in bytes */
    private int length = -1;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates an AttributeType using a unique OID.
     *
     * @param oid the IANA OID number for the attributeType
     */
    protected AbstractAttributeType( String oid )
    {
        super( oid );
    }


    // ------------------------------------------------------------------------
    // Accessor Methods for Specification Properties
    // ------------------------------------------------------------------------


    /**
     * @see AttributeType#isSingleValue()
     */
    public boolean isSingleValue()
    {
        return isSingleValue;
    }


    /**
     * @see AttributeType#isCollective()
     */
    public boolean isCollective()
    {
        return isCollective;
    }


    /**
     * @see AttributeType#isCanUserModify()
     */
    public boolean isCanUserModify()
    {
        return canUserModify;
    }


    /**
     * @see AttributeType#getUsage()
     */
    public UsageEnum getUsage()
    {
        return usage;
    }


    /**
     * @see AttributeType#getLength()
     */
    public int getLength()
    {
        return length;
    }


    // ------------------------------------------------------------------------
    // M U T A T O R S
    // ------------------------------------------------------------------------


    /**
     * Sets whether or not an attribute of this AttributeType single valued or
     * multi-valued.
     *
     * @param singleValue true if its is single valued, false if multi-valued
     */
    protected void setSingleValue( boolean singleValue )
    {
        isSingleValue = singleValue;
    }


    /**
     * Sets whether or not an attribute of this AttributeType is a collective.
     *
     * @param collective true if it is collective, false otherwise
     */
    protected void setCollective( boolean collective )
    {
        isCollective = collective;
    }


    /**
     * Sets whether or not an attribute of this AttributeType can be modified
     * by directory users.
     *
     * @param canUserModify true if directory users can modify, false otherwise
     */
    protected void setCanUserModify( boolean canUserModify )
    {
        this.canUserModify = canUserModify;
    }


    /**
     * The usage class for this attributeType.
     *
     * @param usage the way attributes of this AttributeType are used in the DSA
     */
    protected void setUsage( UsageEnum usage )
    {
        this.usage = usage;
    }


    /**
     * Sets the length limit of this AttributeType based on its associated
     * syntax.
     *
     * @param length the new length to set
     */
    protected void setLength( int length )
    {
        this.length = length;
    }
}
