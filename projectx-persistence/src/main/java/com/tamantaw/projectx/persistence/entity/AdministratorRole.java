package com.tamantaw.projectx.persistence.entity;

import com.tamantaw.projectx.persistence.entity.base.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

@Entity
@Table(name = "mjr_admin_x_role", uniqueConstraints = {@UniqueConstraint(name = "uq_mjr_admin_role", columnNames = {"admin_id", "role_id"})})
@Getter
@Setter
@ToString(callSuper = true)
public class AdministratorRole extends AbstractEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	// ------------------------------------------------------------------
	// RELATIONS (lazy navigation)
	// ------------------------------------------------------------------

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "admin_id",
			nullable = false,
			foreignKey = @ForeignKey(name = "fk_mjr_admin_role_admin")
	)
	@ToString.Exclude
	private Administrator administrator;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "role_id",
			nullable = false,
			foreignKey = @ForeignKey(name = "fk_mjr_admin_role_role")
	)
	@ToString.Exclude
	private Role role;

	// ------------------------------------------------------------------
	// FK IDS (scalar, safe, no SQL)
	// ------------------------------------------------------------------

	@Column(name = "admin_id", nullable = false, insertable = false, updatable = false)
	private Long administratorId;

	@Column(name = "role_id", nullable = false, insertable = false, updatable = false)
	private Long roleId;

	@Override
	public final boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AdministratorRole that)) {
			return false;
		}
		return administrator == that.administrator
				&& role == that.role;
	}

	@Override
	public final int hashCode() {
		return 31 * System.identityHashCode(administrator)
				+ System.identityHashCode(role);
	}
}

