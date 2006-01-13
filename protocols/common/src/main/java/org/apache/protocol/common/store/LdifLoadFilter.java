package org.apache.protocol.common.store;


import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.NamingException;
import java.io.File;


/**
 * A filter interface for the LDIF loader.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface LdifLoadFilter
{
    /**
     * Filters entries loaded from LDIF files by a LdifFileLoader.
     *
     * @param file the file being loaded
     * @param dn the distinguished name of the entry being loaded
     * @param entry the entry attributes within the LDIF file
     * @param ctx context to be used for loading the entry into the DIT
     * @return true if the entry will be created in the DIT, false if it is to be skipped
     * @throws NamingException
     */
    boolean filter( File file, String dn, Attributes entry, DirContext ctx ) throws NamingException;
}
