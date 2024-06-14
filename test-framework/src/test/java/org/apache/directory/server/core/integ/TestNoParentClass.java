package org.apache.directory.server.core.integ;

import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.api.DirectoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith( CreateDSTestExtension.class )
@CreateDS(name = "ClassDS")
@ApplyLdifs({
    "dn: cn=testClassA,ou=system",
    "objectClass: person",
    "cn: testClassA",
    "sn: sn_testClassA",

    "dn: cn=testClassA2,ou=system",
    "objectClass: person",
    "cn: testClassA2",
    "sn: sn_testClassA2" })
public class TestNoParentClass {

    @Test
    @CreateDS(name = "testDS")
    @ApplyLdifs({
        "dn: cn=testMethodA,ou=system",
        "objectClass: person",
        "cn: testMethodA",
        "sn: sn_testMethodA" })
    public void testWithFactoryAnnotation(DirectoryService directoryService) throws Exception
    {
        assertTrue( directoryService.getAdminSession().exists( new Dn( "cn=testClassA,ou=system" ) ) );
        assertTrue( directoryService.getAdminSession().exists( new Dn( "cn=testMethodA,ou=system" ) ) );
    }

    @Test
    @ApplyLdifs({
        "dn: cn=testMethodWithApplyLdif,ou=system",
        "objectClass: person",
        "cn: testMethodWithApplyLdif",
        "sn: sn_testMethodWithApplyLdif" })
    public void testWithoutFactoryAnnotation(DirectoryService directoryService) throws Exception
    {
        assertTrue( directoryService.getAdminSession().exists( new Dn( "cn=testClassA,ou=system" ) ) );
        assertTrue( directoryService.getAdminSession().exists( new Dn( "cn=testClassA2,ou=system" ) ) );
        assertFalse( directoryService.getAdminSession().exists( new Dn( "cn=testMethodA,ou=system" ) ) );
        assertTrue( directoryService.getAdminSession().exists( new Dn( "cn=testMethodWithApplyLdif,ou=system" ) ) );
    }
}
