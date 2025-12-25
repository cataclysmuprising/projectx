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
@Table(name = "mjr_action")
@Getter
@Setter
@ToString(callSuper = true)
public class Action extends AbstractEntity<Long> implements Serializable {

	@Serial
	private static final long serialVersionUID = -3067317183068195626L;
	@NotBlank
	@Length(max = 30)
	@Column(name = "app_name", nullable = false)
	private String appName;

	@NotBlank
	@Length(max = 50)
	@Column(name = "page", nullable = false)
	private String page;

	@NotBlank
	@Length(max = 50)
	@Column(name = "action_name", nullable = false)
	private String actionName;

	@NotBlank
	@Length(max = 100)
	@Column(name = "display_name", nullable = false)
	private String displayName;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "action_type", nullable = false, length = 20)
	private ActionType actionType;

	@NotBlank
	@Length(max = 250)
	@Column(name = "url", nullable = false, unique = true)
	private String url;

	@NotBlank
	@Length(max = 200)
	@Column(name = "description", nullable = false)
	private String description;

	@OneToMany(mappedBy = "action", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<RoleAction> roleActions = new ArrayList<>();

	public enum ActionType {
		MAIN, SUB
	}
}
