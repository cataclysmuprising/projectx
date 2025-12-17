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
@Table(name = "mjr_role_x_action", uniqueConstraints = {@UniqueConstraint(name = "uq_mjr_role_action", columnNames = {"role_id", "action_id"})})
@Getter
@Setter
@ToString(callSuper = true)
public class RoleAction extends AbstractEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 6350526648908543212L;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mjr_role_action_role"))
	private Role role;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "action_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mjr_role_action_action"))
	private Action action;
}

