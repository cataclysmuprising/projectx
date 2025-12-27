package com.tamantaw.projectx.backend.common.exception;

import lombok.Getter;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;

import java.io.Serial;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class ValidationFailedException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = -8102530211658051563L;

	private final Map<String, Object> modelAttributes;
	private final String errorView;

	public ValidationFailedException(Model model, String errorView) {
		this(model, errorView, null);
	}

	public ValidationFailedException(Model model, String errorView, Throwable cause) {
		super(cause);
		this.errorView = errorView;

		if (model == null) {
			modelAttributes = Collections.emptyMap();
			return;
		}

		// Snapshot model into a safe, immutable-ish map
		Map<String, Object> snapshot = new LinkedHashMap<>();
		if (model instanceof ModelMap mm) {
			snapshot.putAll(mm);
		}
		else {
			// Model interface doesn’t expose a map directly, so try common path:
			// Most Spring Models are ModelMap / BindingAwareModelMap.
			snapshot.putAll(model.asMap());
		}
		modelAttributes = Collections.unmodifiableMap(snapshot);
	}
}

