package com.github.projectx.persistence.entity.rba;

import com.github.projectx.persistence.entity.base.AbstractEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "mjr_admin_x_role", uniqueConstraints = {@UniqueConstraint(name = "uq_mjr_admin_role", columnNames = {"admin_id", "role_id"})})
@Getter
@Setter
@ToString(callSuper = true)
public class AdministratorRole extends AbstractEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	// ------------------------------------------------------------------
	// FK IDS (writeable source of truth)
	// ------------------------------------------------------------------
	@NotNull
	@Column(name = "admin_id", nullable = false)
	private Long administratorId;

	@NotNull
	@Column(name = "role_id", nullable = false)
	private Long roleId;

	// ------------------------------------------------------------------
	// RELATIONS (lazy navigation)
	// ------------------------------------------------------------------
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "admin_id",
			nullable = false,
			insertable = false,
			updatable = false,
			foreignKey = @ForeignKey(name = "fk_mjr_admin_role_admin")
	)
	@ToString.Exclude
	private Administrator administrator;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "role_id",
			nullable = false,
			insertable = false,
			updatable = false,
			foreignKey = @ForeignKey(name = "fk_mjr_admin_role_role")
	)
	@ToString.Exclude
	private Role role;

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	@Override
	public final boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AdministratorRole that)) {
			return false;
		}

		if (administratorId == null || roleId == null
				|| that.administratorId == null || that.roleId == null) {
			return false;
		}
		return administratorId.equals(that.administratorId)
				&& roleId.equals(that.roleId);
	}

	/**
	 * Centralizes this decision contract so boolean checks stay consistent across call sites.
	 */
	@Override
	public final int hashCode() {
		if (administratorId == null || roleId == null) {
			return System.identityHashCode(this);
		}
		return Objects.hash(administratorId, roleId);
	}
}

