package org.apache.directory.server.core.schema;


import javax.naming.NamingException;

import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.shared.ldap.schema.SchemaManager;


public interface SchemaService
{

    /**
     * Tells if the given DN is the schemaSubentry DN
     * 
     * @param dnString The DN we want to check
     * @return <code>true</code> if the given DN is the Schema subentry DN
     * @throws NamingException If the given DN is not valid
     */
    abstract boolean isSchemaSubentry( String dnString ) throws NamingException;


    /**
     * @return the schemaManager loaded from schemaPartition
     */
    abstract SchemaManager getSchemaManager();


    abstract SchemaPartition getSchemaPartition();


    abstract void setSchemaPartition( SchemaPartition schemaPartition );
    
    
    /**
     * Initializes the SchemaService
     *
     * @throws Exception If the initializaion fails
     */
    void initialize() throws Exception;


    /**
     * A seriously unsafe (unsynchronized) means to access the schemaSubentry.
     *
     * @return the schemaSubentry
     * @throws NamingException if there is a failure to access schema timestamps
     */
    abstract ServerEntry getSubschemaEntryImmutable() throws Exception;


    /**
     * A seriously unsafe (unsynchronized) means to access the schemaSubentry.
     *
     * @return the schemaSubentry
     * @throws NamingException if there is a failure to access schema timestamps
     */
    abstract ServerEntry getSubschemaEntryCloned() throws Exception;


    /**
     * Gets the schemaSubentry based on specific search id parameters which
     * include the special '*' and '+' operators.
     *
     * @param ids the ids of the attributes that should be returned from a search
     * @return the subschema entry with the ids provided
     * @throws NamingException if there are failures during schema info access
     */
    abstract ServerEntry getSubschemaEntry( String[] ids ) throws Exception;
}