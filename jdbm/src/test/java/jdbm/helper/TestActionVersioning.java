package jdbm.helper;

import org.junit.runner.RunWith;
import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class TestActionVersioning
{
    @Test
    public void testVersioning()
    {
        ActionVersioning.Version version1, version2;
        ActionVersioning.Version writeVersion;
        ActionVersioning.Version minVersion;
        
        ActionVersioning versioning = new ActionVersioning();
        version1 = versioning.beginReadAction();
        assertEquals( version1.getVersion(),  0 );
        
        writeVersion = versioning.beginWriteAction();      
        assertEquals( writeVersion.getVersion(), 1 );
        
        version2 = versioning.beginReadAction();
        assertEquals( version2.getVersion(), 0 );
        
        minVersion = versioning.endWriteAction();
        assertEquals( minVersion.getVersion(), 0 );
        
        writeVersion = versioning.beginWriteAction();
        assertEquals( writeVersion.getVersion(), 2 );
        
        minVersion = versioning.endWriteAction();
        assertEquals( minVersion.getVersion(), 0 );
        
        versioning.endReadAction( version1 );
        minVersion = versioning.endReadAction( version2 );
        assertEquals( minVersion.getVersion(), 2 );
        
        version1  = versioning.beginReadAction();
        assertEquals( version1.getVersion(), 2 );
        
        minVersion = versioning.endReadAction( version1 );
        assertEquals( minVersion, null );
        
    }
}