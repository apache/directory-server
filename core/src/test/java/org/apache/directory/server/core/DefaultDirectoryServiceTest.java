package org.apache.directory.server.core;

import static org.junit.Assert.assertEquals;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.junit.Test;

public class DefaultDirectoryServiceTest {

  @Test
  public void testAddAfterExistingInterceptor() throws LdapException {
    // given
    final String existingInterceptorName = InterceptorEnum.AUTHENTICATION_INTERCEPTOR.getName();
    DefaultDirectoryService service = new DefaultDirectoryService();

    // when
    service.addAfter(existingInterceptorName, new FooInterceptor());

    // then
    for (int i = 0; i < service.getInterceptors().size(); i++) {
      Interceptor interceptor = service.getInterceptors().get(i);
      if (existingInterceptorName.equals(interceptor.getName())) {
        final Interceptor nextInterceptor = service.getInterceptors().get(i + 1);
        assertEquals("foo", nextInterceptor.getName());
      }
    }
  }

  @Test
  public void testAddAfterForUnknownPredecessor() throws LdapException {
    // given
    DefaultDirectoryService service = new DefaultDirectoryService();

    // when
    service.addAfter("-unknown-", new FooInterceptor());

    // then
    final Interceptor lastInterceptor = service.getInterceptors()
        .get(service.getInterceptors().size() - 1);
    assertEquals("foo", lastInterceptor.getName());
  }

  static class FooInterceptor extends BaseInterceptor {

    @Override
    public String getName() {
      return "foo";
    }
  }
}