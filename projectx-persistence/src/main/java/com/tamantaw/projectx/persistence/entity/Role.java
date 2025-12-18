package com.tamantaw.projectx.persistence.entity;

import com.fasterxml.jackson.annotation.JsonValue;
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
@Table(name = "mjr_role", uniqueConstraints = {@UniqueConstraint(name = "uq_mjr_role_app_name", columnNames = {"app_name", "name"})})
@Getter
@Setter
@ToString(callSuper = true)
public class Role extends AbstractEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = -5089308082245660719L;
	@NotBlank
	@Length(max = 30)
	@Column(name = "app_name", nullable = false)
	private String appName;

	@NotBlank
	@Length(max = 20)
	@Column(name = "name", nullable = false)
	private String name;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 20)
	private RoleType roleType;

	@Length(max = 200)
	@Column(name = "description")
	private String description;

	@OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<RoleAction> roleActions = new ArrayList<>();

	@OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<AdministratorRole> administratorRoles = new ArrayList<>();

	public enum RoleType {
		SUPERUSER("super-user"),// 0
		BUILT_IN("built-in"),   // 1
		CUSTOM("custom");       // 2

		private final String definition;

		RoleType(String definition) {
			this.definition = definition;
		}

		public static RoleType getEnum(String value) {
			RoleType _enum = null;
			RoleType[] var2 = values();
			int var3 = var2.length;

			for (RoleType v : var2) {
				if (v.getDefinition().trim().equalsIgnoreCase(value)) {
					_enum = v;
					break;
				}
			}

			return _enum;
		}

		@JsonValue
		public String getDefinition() {
			return definition;
		}

		@Override
		public String toString() {
			return definition;
		}
	}
}
