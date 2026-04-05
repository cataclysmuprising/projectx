package com.github.projectx.persistence.entity.rba;

import com.github.projectx.persistence.entity.base.AbstractEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
		name = "mjr_action_route",
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uq_mjr_action_route_app_method_pattern",
						columnNames = {"app_name", "http_method", "route_pattern"}
				)
		}
)
@Getter
@Setter
@ToString(callSuper = true)
public class ActionRoute extends AbstractEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 8998988682877513114L;

	@NotBlank
	@Length(max = 30)
	@Column(name = "app_name", nullable = false)
	private String appName;

	@NotBlank
	@Length(max = 250)
	@Column(name = "route_pattern", nullable = false)
	private String routePattern;

	@NotBlank
	@Length(max = 16)
	@Column(name = "http_method", nullable = false)
	private String httpMethod = "ANY";

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "route_kind", nullable = false, length = 20)
	private RouteKind routeKind;

	@NotNull
	@Column(name = "priority", nullable = false)
	private Integer priority = 100;

	@NotNull
	@Column(name = "active", nullable = false)
	private Boolean active = Boolean.TRUE;

	@NotBlank
	@Length(max = 200)
	@Column(name = "description", nullable = false)
	private String description;

	@OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private Set<ActionRouteTarget> routeTargets = new HashSet<>();

	public enum RouteKind {
		PRIMARY,
		PAGE_SUPPORT,
		SHARED_LOOKUP
	}
}
