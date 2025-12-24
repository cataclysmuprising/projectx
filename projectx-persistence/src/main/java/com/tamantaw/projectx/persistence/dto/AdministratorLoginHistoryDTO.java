package com.tamantaw.projectx.persistence.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tamantaw.projectx.persistence.dto.base.AbstractDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString(callSuper = true)
public class AdministratorLoginHistoryDTO extends AbstractDTO {

	private Long administratorId;

	private AdministratorDTO administrator;

	private String ipAddress;

	private String os;

	private String clientAgent;

	@JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
	@DateTimeFormat(pattern = "dd-MM-yyyy HH:mm:ss")
	private LocalDateTime loginDate;
}
