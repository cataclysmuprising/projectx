package com.tamantaw.projectx.backend.common.exception;

import org.springframework.ui.Model;

import java.io.Serial;

public class ValidationFailedException extends RuntimeException {
	@Serial
	private static final long serialVersionUID = -8102530211658051563L;
	private Model model;
	private String errorView;

	public ValidationFailedException(Model model, String errorView) {
		super();
		this.model = model;
		this.errorView = errorView;
	}

	public Model getModel() {
		return model;
	}

	public void setModel(Model model) {
		this.model = model;
	}

	public String getErrorView() {
		return errorView;
	}

	public void setErrorView(String errorView) {
		this.errorView = errorView;
	}
}
