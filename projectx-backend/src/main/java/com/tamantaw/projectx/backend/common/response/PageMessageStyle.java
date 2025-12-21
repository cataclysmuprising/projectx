package com.tamantaw.projectx.backend.common.response;

public enum PageMessageStyle {
	DEFAULT("default"), INFO("info"), SUCCESS("success"), WARNING("warning"), ERROR("error");

	private final String value;

	PageMessageStyle(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
