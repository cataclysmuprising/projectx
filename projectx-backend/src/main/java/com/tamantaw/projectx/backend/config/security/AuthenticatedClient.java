package com.tamantaw.projectx.backend.config.security;

import com.tamantaw.projectx.persistence.dto.AdministratorDTO;
import com.tamantaw.projectx.persistence.entity.Administrator;
import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class AuthenticatedClient implements UserDetails {
	@Serial
	private static final long serialVersionUID = 1774247443506921090L;
	private AdministratorDTO administrator;
	private Set<String> roleNames;
	private String requestUrl;

	public AuthenticatedClient(AdministratorDTO administrator, Set<String> roleNames) {
		this.administrator = administrator;
		this.roleNames = roleNames;
	}

	@Override
	@Nonnull
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return roleNames.stream()
				.map(SimpleGrantedAuthority::new)
				.toList();
	}

	@Override
	public String getPassword() {
		return administrator.getPassword();
	}

	@Override
	@Nonnull
	public String getUsername() {
		return administrator.getLoginId();
	}

	public Long getId() {
		return administrator.getId();
	}

	public String getLoginId() {
		return administrator.getLoginId();
	}

	public String getClientName() {
		return administrator.getName();
	}

	public AdministratorDTO getUserDetail() {
		return administrator;
	}

	@Override
	public boolean isAccountNonLocked() {
		return administrator.getStatus() == Administrator.Status.ACTIVE;
	}

	@Override
	public boolean isEnabled() {
		return administrator.getStatus() == Administrator.Status.ACTIVE;
	}
}
