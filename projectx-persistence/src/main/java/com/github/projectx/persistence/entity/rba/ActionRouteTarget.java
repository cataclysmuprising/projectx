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
		name = "mjr_action_route_x_action",
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uq_mjr_action_route_target",
						columnNames = {"route_id", "action_id"}
				)
		}
)
@Getter
@Setter
@ToString(callSuper = true)
public class ActionRouteTarget extends AbstractEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = -7841787588140278310L;

	@NotNull
	@Column(name = "route_id", nullable = false)
	private Long routeId;

	@NotNull
	@Column(name = "action_id", nullable = false)
	private Long actionId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "route_id",
			nullable = false,
			insertable = false,
			updatable = false,
			foreignKey = @ForeignKey(name = "fk_mjr_action_route_target_route")
	)
	@ToString.Exclude
	private ActionRoute route;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "action_id",
			nullable = false,
			insertable = false,
			updatable = false,
			foreignKey = @ForeignKey(name = "fk_mjr_action_route_target_action")
	)
	@ToString.Exclude
	private Action action;

	@Override
	public final boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ActionRouteTarget that)) {
			return false;
		}

		if (routeId == null || actionId == null
				|| that.routeId == null || that.actionId == null) {
			return false;
		}
		return routeId.equals(that.routeId)
				&& actionId.equals(that.actionId);
	}

	@Override
	public final int hashCode() {
		if (routeId == null || actionId == null) {
			return System.identityHashCode(this);
		}
		return Objects.hash(routeId, actionId);
	}
}
