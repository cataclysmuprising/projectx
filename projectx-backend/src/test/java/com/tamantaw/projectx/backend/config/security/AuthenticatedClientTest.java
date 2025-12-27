package com.tamantaw.projectx.backend.config.security;

import com.tamantaw.projectx.persistence.dto.AdministratorDTO;
import com.tamantaw.projectx.persistence.entity.Administrator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticatedClientTest {

	@Test
	void shouldReportAccountAndCredentialsNonExpiredWhenActive() {
		AdministratorDTO administrator = new AdministratorDTO();
		administrator.setStatus(Administrator.Status.ACTIVE);

		AuthenticatedClient client = new AuthenticatedClient(administrator, Set.of("ROLE_ADMIN"));

		assertTrue(client.isAccountNonExpired());
		assertTrue(client.isAccountNonLocked());
		assertTrue(client.isCredentialsNonExpired());
		assertTrue(client.isEnabled());
	}

	@Test
	void shouldReportAccountAndCredentialsExpiredWhenSuspended() {
		AdministratorDTO administrator = new AdministratorDTO();
		administrator.setStatus(Administrator.Status.SUSPENDED);

		AuthenticatedClient client = new AuthenticatedClient(administrator, Set.of("ROLE_ADMIN"));

		assertFalse(client.isAccountNonExpired());
		assertFalse(client.isAccountNonLocked());
		assertFalse(client.isCredentialsNonExpired());
		assertFalse(client.isEnabled());
	}
}
