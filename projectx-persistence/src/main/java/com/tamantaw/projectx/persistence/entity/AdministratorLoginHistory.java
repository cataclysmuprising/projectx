package com.tamantaw.projectx.persistence.entity;

import com.tamantaw.projectx.persistence.entity.base.AbstractEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "mjr_admin_login_history")
@Getter
@Setter
@ToString(callSuper = true)
public class AdministratorLoginHistory extends AbstractEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "admin_id", nullable = false, foreignKey = @ForeignKey(name = "mjr_admin_login_history_admin"))
	@ToString.Exclude
	private Administrator administrator;

	@Length(max = 100)
	@Column(name = "ip_address", length = 100)
	private String ipAddress;

	@Length(max = 100)
	@Column(name = "os", length = 100)
	private String os;

	@Length(max = 200)
	@Column(name = "client_agent", length = 200)
	private String clientAgent;

	@NotNull
	@Column(name = "login_date", nullable = false)
	private LocalDateTime loginDate;

	@PrePersist
	protected void onCreate() {
		if (loginDate == null) {
			loginDate = LocalDateTime.now();
		}
	}
}
