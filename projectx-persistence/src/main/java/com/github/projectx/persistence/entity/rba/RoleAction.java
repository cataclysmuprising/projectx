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
@Table(
		name = "mjr_role_x_action",
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uq_mjr_role_action",
						columnNames = {"role_id", "action_id"}
				)
		}
)
@Getter
@Setter
@ToString(callSuper = true)
public class RoleAction extends AbstractEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 6350526648908543212L;

	// ------------------------------------------------------------------
	// FK IDS (writeable source of truth)
	// ------------------------------------------------------------------
	@NotNull
	@Column(name = "role_id", nullable = false)
	private Long roleId;

	@NotNull
	@Column(name = "action_id", nullable = false)
	private Long actionId;

	// ------------------------------------------------------------------
	// RELATIONS (lazy navigation only)
	// ------------------------------------------------------------------
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "role_id",
			nullable = false,
			insertable = false,
			updatable = false,
			foreignKey = @ForeignKey(name = "fk_mjr_role_action_role")
	)
	@ToString.Exclude
	private Role role;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "action_id",
			nullable = false,
			insertable = false,
			updatable = false,
			foreignKey = @ForeignKey(name = "fk_mjr_role_action_action")
	)
	@ToString.Exclude
	private Action action;

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	@Override
	public final boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof RoleAction that)) {
			return false;
		}

		if (roleId == null || actionId == null
				|| that.roleId == null || that.actionId == null) {
			return false;
		}
		return roleId.equals(that.roleId)
				&& actionId.equals(that.actionId);
	}

	/**
	 * Centralizes this decision contract so boolean checks stay consistent across call sites.
	 */
	@Override
	public final int hashCode() {
		if (roleId == null || actionId == null) {
			return System.identityHashCode(this);
		}
		return Objects.hash(roleId, actionId);
	}
}

