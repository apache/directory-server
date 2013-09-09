package org.apache.directory.kerberos.client;

import java.io.File;

import org.apache.directory.kerberos.credentials.cache.Credentials;
import org.apache.directory.kerberos.credentials.cache.CredentialsCache;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.apache.directory.shared.kerberos.components.PrincipalName;

/**
 * Authenticates to the Kerberos server and gets the initial Ticket Granting Ticket,
 * then cache the tgt in credentials cache, as MIT kinit does.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Kinit
{
	
	private KdcConnection kdc;
	private File credCacheFile;
	
	public Kinit( KdcConnection kdc )
	{
		this.kdc = kdc;
	}
	
	public void setCredCacheFile( File credCacheFile )
	{
		this.credCacheFile = credCacheFile;
	}
	
	public File getCredCacheFile()
	{
		return this.credCacheFile;
	}
	
    /**
     * Authenticates to the Kerberos server and gets the initial Ticket Granting Ticket,
     * then cache the tgt in credentials cache, as MIT kinit does.
     * 
     * @param principal the client's principal 
     * @param password password of the client
     * @return
     * @throws Exception
     */
    public void kinit( String principal, String password ) throws Exception
    {
    	if ( principal == null || password == null || credCacheFile == null )
    	{
    		throw new IllegalArgumentException( "Invalid principal, password, or credentials cache file" );
    	}
    	
    	TgTicket tgt = kdc.getTgt( principal, password );
    	
    	CredentialsCache credCache = new CredentialsCache();
    	
    	PrincipalName princ = new PrincipalName( principal, PrincipalNameType.KRB_NT_PRINCIPAL );
    	princ.setRealm( tgt.getRealm() );
    	credCache.setPrimaryPrincipalName( princ );
    	
    	Credentials cred = new Credentials( tgt );
    	credCache.addCredentials( cred );
    	
    	CredentialsCache.store( credCacheFile, credCache );
    }    
}
