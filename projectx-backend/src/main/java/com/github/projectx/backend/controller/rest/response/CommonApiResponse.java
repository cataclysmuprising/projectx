package com.github.projectx.backend.controller.rest.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class CommonApiResponse {
	private int statusCode;
	private String errorCode;
	private String title;
	private String message;
	@JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
	@DateTimeFormat(pattern = "dd-MM-yyyy HH:mm:ss")
	private LocalDateTime createdDate;
	@JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
	@DateTimeFormat(pattern = "dd-MM-yyyy HH:mm:ss")
	private LocalDateTime updatedDate;
}
