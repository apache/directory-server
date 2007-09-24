package org.apache.directory.server.kerberos.shared.messages.value;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import junit.framework.TestCase;

public class KerberosTimeTest extends TestCase
{

    public void testKerberosTime()
    {
        fail( "Not yet implemented" );
    }


    public void testKerberosTimeLong()
    {
        fail( "Not yet implemented" );
    }


    public void testKerberosTimeDate()
    {
        fail( "Not yet implemented" );
    }


    public void testGetTimeString()
    {
        fail( "Not yet implemented" );
    }


    public void testGetTime() throws ParseException
    {
        String zuluTime = "20070708224829Z";
        KerberosTime kt = KerberosTime.getTime( zuluTime );
        
        SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyyMMddHHmmss'Z'" );
        TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "UTC" );
        dateFormat.setTimeZone( UTC_TIME_ZONE );
        Date date = dateFormat.parse( zuluTime );
        
        assertEquals( date, kt .toDate() );
    }


    public void testCompareTo()
    {
        fail( "Not yet implemented" );
    }


    public void testToDate()
    {
        fail( "Not yet implemented" );
    }


    public void testIsInClockSkew()
    {
        fail( "Not yet implemented" );
    }


    public void testGreaterThan()
    {
        fail( "Not yet implemented" );
    }


    public void testLessThan()
    {
        fail( "Not yet implemented" );
    }


    public void testEqualsKerberosTime()
    {
        fail( "Not yet implemented" );
    }


    public void testIsZero()
    {
        fail( "Not yet implemented" );
    }

}
