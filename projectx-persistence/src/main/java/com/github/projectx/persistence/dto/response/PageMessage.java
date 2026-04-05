package com.github.projectx.persistence.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PageMessage {
	private String title;
	private String message;
	private String style;
}
