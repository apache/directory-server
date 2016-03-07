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
 */
package org.apache.directory.server.core.api;


import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.constants.ApacheSchemaConstants;


/**
 * Provides commonly used {@link AttributeType}s.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AttributeTypeProvider
{
    private final AttributeType accessControlSubentries;
    private final AttributeType administrativeRole;
    private final AttributeType collectiveAttributeSubentries;
    private final AttributeType collectiveExclusions;
    private final AttributeType creatorsName;
    private final AttributeType createTimestamp;
    private final AttributeType entryACI;
    private final AttributeType entryCSN;
    private final AttributeType entryDN;
    private final AttributeType entryUUID;
    private final AttributeType member;
    private final AttributeType modifiersName;
    private final AttributeType modifyTimestamp;
    private final AttributeType objectClass;
    private final AttributeType prescriptiveACI;
    private final AttributeType subentryACI;
    private final AttributeType subschemaSubentry;
    private final AttributeType subtreeSpecification;
    private final AttributeType triggerExecutionSubentries;
    private final AttributeType uniqueMember;
    private final AttributeType userPassword;
    private final AttributeType nbChildren;
    private final AttributeType nbSubordinates;

    private final AttributeType[] subentryOperationalAttributes;


    public AttributeTypeProvider( SchemaManager schemaManager )
    {
        accessControlSubentries = schemaManager.getAttributeType( ApacheSchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT );
        administrativeRole = schemaManager.getAttributeType( SchemaConstants.ADMINISTRATIVE_ROLE_AT );
        collectiveAttributeSubentries = schemaManager
            .getAttributeType( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        collectiveExclusions = schemaManager.getAttributeType( SchemaConstants.COLLECTIVE_EXCLUSIONS_AT );
        creatorsName = schemaManager.getAttributeType( SchemaConstants.CREATORS_NAME_AT );
        createTimestamp = schemaManager.getAttributeType( SchemaConstants.CREATE_TIMESTAMP_AT );
        entryACI = schemaManager.getAttributeType( SchemaConstants.ENTRY_ACI_AT_OID );
        entryCSN = schemaManager.getAttributeType( SchemaConstants.ENTRY_CSN_AT );
        entryDN = schemaManager.getAttributeType( SchemaConstants.ENTRY_DN_AT );
        entryUUID = schemaManager.getAttributeType( SchemaConstants.ENTRY_UUID_AT );
        member = schemaManager.getAttributeType( SchemaConstants.MEMBER_AT );
        modifiersName = schemaManager.getAttributeType( SchemaConstants.MODIFIERS_NAME_AT );
        modifyTimestamp = schemaManager.getAttributeType( SchemaConstants.MODIFY_TIMESTAMP_AT );
        objectClass = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
        prescriptiveACI = schemaManager.getAttributeType( SchemaConstants.PRESCRIPTIVE_ACI_AT );
        subentryACI = schemaManager.getAttributeType( SchemaConstants.SUBENTRY_ACI_AT_OID );
        subschemaSubentry = schemaManager.getAttributeType( SchemaConstants.SUBSCHEMA_SUBENTRY_AT );
        subtreeSpecification = schemaManager.getAttributeType( SchemaConstants.SUBTREE_SPECIFICATION_AT );
        triggerExecutionSubentries = schemaManager
            .getAttributeType( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        uniqueMember = schemaManager.getAttributeType( SchemaConstants.UNIQUE_MEMBER_AT_OID );
        userPassword = schemaManager.getAttributeType( SchemaConstants.USER_PASSWORD_AT_OID );
        nbChildren = schemaManager.getAttributeType( ApacheSchemaConstants.NB_CHILDREN_OID );
        nbSubordinates = schemaManager.getAttributeType( ApacheSchemaConstants.NB_SUBORDINATES_OID );

        subentryOperationalAttributes = new AttributeType[]
            {
                accessControlSubentries,
                subschemaSubentry,
                collectiveAttributeSubentries,
                triggerExecutionSubentries
        };
    }


    /** 
     * @return the <code>accessControlSubentries<code> {@link AttributeType}.
     */
    public AttributeType getAccessControlSubentries()
    {
        return accessControlSubentries;
    }


    /** 
     * @return the <code>administrativeRole<code> {@link AttributeType}.
     */
    public AttributeType getAdministrativeRole()
    {
        return administrativeRole;
    }


    /** 
     * @return the <code>collectiveAttributeSubentries<code> {@link AttributeType}.
     */
    public AttributeType getCollectiveAttributeSubentries()
    {
        return collectiveAttributeSubentries;
    }


    /** 
     * @return the <code>collectiveExclusions<code> {@link AttributeType}.
     */
    public AttributeType getCollectiveExclusions()
    {
        return collectiveExclusions;
    }


    /** 
     * @return the <code>creatorsName<code> {@link AttributeType}.
     */
    public AttributeType getCreatorsName()
    {
        return creatorsName;
    }


    /** 
     * @return the <code>createTimestamp<code> {@link AttributeType}.
     */
    public AttributeType getCreateTimestamp()
    {
        return createTimestamp;
    }


    /** 
     * @return the <code>entryACI<code> {@link AttributeType}.
     */
    public AttributeType getEntryACI()
    {
        return entryACI;
    }


    /** 
     * @return the <code>entryCSN<code> {@link AttributeType}.
     */
    public AttributeType getEntryCSN()
    {
        return entryCSN;
    }


    /** 
     * @return the <code>entryDN<code> {@link AttributeType}.
     */
    public AttributeType getEntryDN()
    {
        return entryDN;
    }


    /** 
     * @return the <code>entryUUID<code> {@link AttributeType}.
     */
    public AttributeType getEntryUUID()
    {
        return entryUUID;
    }


    /** 
     * @return the <code>member<code> {@link AttributeType}.
     */
    public AttributeType getMember()
    {
        return member;
    }


    /** 
     * @return the <code>modifiersName<code> {@link AttributeType}.
     */
    public AttributeType getModifiersName()
    {
        return modifiersName;
    }


    /** 
     * @return the <code>modifyTimestamp<code> {@link AttributeType}.
     */
    public AttributeType getModifyTimestamp()
    {
        return modifyTimestamp;
    }


    /** 
     * @return the <code>objectClass<code> {@link AttributeType}.
     */
    public AttributeType getObjectClass()
    {
        return objectClass;
    }


    /** 
     * @return the <code>prescriptiveACI<code> {@link AttributeType}.
     */
    public AttributeType getPrescriptiveACI()
    {
        return prescriptiveACI;
    }


    /** 
     * @return the <code>subentryACI<code> {@link AttributeType}.
     */
    public AttributeType getSubentryACI()
    {
        return subentryACI;
    }


    /** 
     * @return the <code>subschemaSubentry<code> {@link AttributeType}.
     */
    public AttributeType getSubschemaSubentry()
    {
        return subschemaSubentry;
    }


    /** 
     * @return the <code>subtreeSpecification<code> {@link AttributeType}.
     */
    public AttributeType getSubtreeSpecification()
    {
        return subtreeSpecification;
    }


    /** 
     * @return the <code>triggerExecutionSubentries<code> {@link AttributeType}.
     */
    public AttributeType getTriggerExecutionSubentries()
    {
        return triggerExecutionSubentries;
    }


    /** 
     * @return the <code>uniqueMember<code> {@link AttributeType}.
     */
    public AttributeType getUniqueMember()
    {
        return uniqueMember;
    }


    /**
     * @return the operational attributes of n nbChildren
     */
    public AttributeType getNbChildren()
    {
        return nbChildren;
    }


    /**
     * @return the operational attributes of a nbSubordinates
     */
    public AttributeType getNbSubordinates()
    {
        return nbSubordinates;
    }


    /** 
     * @return the <code>userPassword<code> {@link AttributeType}.
     */
    public AttributeType getUserPassword()
    {
        return userPassword;
    }


    /**
     * @return the operational attributes of an subentry
     */
    public AttributeType[] getSubentryOperationalAttributes()
    {
        return subentryOperationalAttributes;
    }
}
