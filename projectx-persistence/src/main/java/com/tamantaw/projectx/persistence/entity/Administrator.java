package com.tamantaw.projectx.persistence.entity;

import com.tamantaw.projectx.persistence.entity.base.AbstractEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mjr_admin", uniqueConstraints = {@UniqueConstraint(name = "uq_mjr_admin_login", columnNames = "login_id")})
@Getter
@Setter
@ToString(callSuper = true)
public class Administrator extends AbstractEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	// ------------------------------------------------------
	// Core fields
	// ------------------------------------------------------

	@NotBlank
	@Length(max = 50)
	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@NotBlank
	@Length(max = 50)
	@Column(name = "login_id", nullable = false, length = 50)
	private String loginId;

	@NotBlank
	@Length(max = 200)
	@Column(name = "password", nullable = false, length = 200)
	private String password;

	// ------------------------------------------------------
	// Status
	// ------------------------------------------------------

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private Status status = Status.ACTIVE;

	// ------------------------------------------------------
	// Enum
	// ------------------------------------------------------
	@OneToMany(mappedBy = "administrator", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<AdministratorRole> administratorRoles = new ArrayList<>();
	@OneToMany(mappedBy = "administrator", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<AdministratorLoginHistory> loginHistories = new ArrayList<>();

	/**
	 * Administrator account status.
	 * Stored as STRING for schema stability.
	 */
	public enum Status {
		ACTIVE, SUSPENDED
	}
}
