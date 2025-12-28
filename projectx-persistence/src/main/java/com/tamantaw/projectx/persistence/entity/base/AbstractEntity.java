package com.tamantaw.projectx.persistence.entity.base;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@ToString
@EntityListeners(AuditingEntityListener.class)
public abstract class AbstractEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 6606582588992858115L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * ID of admin/user who created this record
	 * (Asia/Rangoon local time system)
	 */
	@NotNull
	@Column(name = "created_by", nullable = false, updatable = false)
	private Long createdBy;

	/**
	 * Record creation time (local Asia/Rangoon, no timezone stored)
	 */
	@CreatedDate
	@Column(name = "created_date", nullable = false, updatable = false)
	private LocalDateTime createdDate;

	/**
	 * ID of admin/user who last updated this record
	 */
	@NotNull
	@Column(name = "updated_by", nullable = false)
	private Long updatedBy;

	/**
	 * Record last update time (local Asia/Rangoon, no timezone stored)
	 */
	@LastModifiedDate
	@Column(name = "updated_date", nullable = false)
	private LocalDateTime updatedDate;
}