package org.apache.directory.server.core.schema;


import javax.naming.NamingException;

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.registries.Registries;


public interface SchemaChangeManager
{

    public abstract Registries getGlobalRegistries();


    public abstract Registries getRegistries( LdapDN dn );


    public abstract void add( AddOperationContext opContext ) throws Exception;


    public abstract void delete( DeleteOperationContext opContext, ClonedServerEntry entry, boolean doCascadeDelete )
        throws Exception;


    public abstract void modify( ModifyOperationContext opContext, ModificationOperation modOp, ServerEntry mods,
        ServerEntry entry, ServerEntry targetEntry, boolean cascade ) throws Exception;


    public abstract void modify( ModifyOperationContext opContext, ServerEntry entry, ServerEntry targetEntry,
        boolean doCascadeModify ) throws Exception;


    public abstract void modifyRn( RenameOperationContext opContext, ServerEntry entry, boolean doCascadeModify )
        throws Exception;


    public abstract void replace( MoveOperationContext opContext, ServerEntry entry, boolean cascade ) throws Exception;


    public abstract void move( MoveAndRenameOperationContext opContext, ServerEntry entry, boolean cascade )
        throws Exception;


    /**
     * Translates modify operations on schema subentries into one or more operations 
     * on meta schema entities within the ou=schema partition and updates the registries
     * accordingly.  This uses direct access to the partition to bypass all interceptors.
     * 
     * @param name the name of the subentry
     * @param mods the modification operations performed on the subentry
     * @param subentry the attributes of the subentry
     * @param targetSubentry the target subentry after being modified
     * @param doCascadeModify determines if a cascading operation should be performed
     * to effect all dependents on the changed entity
     * @throws NamingException if the operation fails
     */
    public abstract void modifySchemaSubentry( ModifyOperationContext opContext, ServerEntry subentry,
        ServerEntry targetSubentry, boolean doCascadeModify ) throws Exception;


    /**
     * Translates modify operations on schema subentries into one or more operations 
     * on meta schema entities within the ou=schema partition and updates the registries
     * accordingly.  This uses direct access to the partition to bypass all interceptors.
     * 
     * @param name the name of the subentry
     * @param modOp the modification operation performed on the subentry
     * @param mods the modification operations performed on the subentry
     * @param subentry the attributes of the subentry
     * @param targetSubentry the target subentry after being modified
     * @param doCascadeModify determines if a cascading operation should be performed
     * to effect all dependents on the changed entity
     * @throws NamingException if the modify fails
     */
    public abstract void modifySchemaSubentry( ModifyOperationContext opContext, LdapDN name, int modOp,
        ServerEntry mods, ServerEntry subentry, ServerEntry targetSubentry, boolean doCascadeModify ) throws Exception;


    public abstract String getSchema( SchemaObject schemaObject );

}