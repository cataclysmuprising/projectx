package com.tamantaw.projectx.persistence.entity;

import com.tamantaw.projectx.persistence.entity.base.AbstractEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

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
	// RELATIONS (lazy navigation only)
	// ------------------------------------------------------------------

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "role_id",
			nullable = false,
			foreignKey = @ForeignKey(name = "fk_mjr_role_action_role")
	)
	@ToString.Exclude
	private Role role;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "action_id",
			nullable = false,
			foreignKey = @ForeignKey(name = "fk_mjr_role_action_action")
	)
	@ToString.Exclude
	private Action action;

	// ------------------------------------------------------------------
	// FK IDS (scalar, safe, zero-SQL)
	// ------------------------------------------------------------------

	@Column(name = "role_id", nullable = false, insertable = false, updatable = false)
	private Long roleId;

	@Column(name = "action_id", nullable = false, insertable = false, updatable = false)
	private Long actionId;

	@Override
	public final boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof RoleAction that)) {
			return false;
		}
		return role == that.role
				&& action == that.action;
	}

	@Override
	public final int hashCode() {
		return 31 * System.identityHashCode(role)
				+ System.identityHashCode(action);
	}
}

