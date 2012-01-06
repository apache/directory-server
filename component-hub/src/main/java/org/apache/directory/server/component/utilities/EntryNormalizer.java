package org.apache.directory.server.component.utilities;


import java.util.UUID;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.csn.CsnFactory;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.util.DateUtils;


/**
 * Since ComponentHub is accessing raw schema and config partition references.
 * Entries must be processed as NormalizationInterceptor and OperationalInterceptor do.
 * This class provides normalization and attribution facilities for ldif formatted entries.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EntryNormalizer
{
    /** SchemaManager reference for normalizing */
    private static SchemaManager schemaManager;
    private static CsnFactory csnFactory;


    /**
     * Sets the SchemaManager reference for normalizing.
     *
     * @param schemaManager SchemaManager to use in normalizing.
     */
    public static void init( SchemaManager schemaManager, int replicaId )
    {
        EntryNormalizer.schemaManager = schemaManager;
        csnFactory = new CsnFactory( replicaId );
    }


    /**
     * Puts the operational attributes to given entry,
     * and normalizes its Dn and Attribute names.
     *
     * @param entry Entry to be normalized.
     * @return Normalized entry, null if error occurs.
     */
    public static Entry normalizeEntry( Entry entry )
    {
        // Put the operational attributes first.
        entry.put( SchemaConstants.ENTRY_UUID_AT, UUID.randomUUID().toString() );
        entry.put( SchemaConstants.ENTRY_CSN_AT, csnFactory.newInstance().toString() );
        entry.put( SchemaConstants.CREATORS_NAME_AT, ServerDNConstants.ADMIN_SYSTEM_DN );
        entry.put( SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );

        // This will cause the Dn and attribute names to be normalized,
        Entry normalizedEntry;
        try
        {
            normalizedEntry = new DefaultEntry( schemaManager, entry );
        }
        catch ( LdapException e )
        {
            e.printStackTrace();
            return null;
        }

        return normalizedEntry;
    }
}
