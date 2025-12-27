package com.tamantaw.projectx.backend.common.aspect;

import com.tamantaw.projectx.backend.common.annotation.ValidateEntity;
import com.tamantaw.projectx.backend.common.exception.ValidationFailedException;
import com.tamantaw.projectx.backend.common.response.PageMessage;
import com.tamantaw.projectx.backend.common.response.PageMessageStyle;
import com.tamantaw.projectx.backend.common.validation.BaseValidator;
import com.tamantaw.projectx.persistence.dto.base.AbstractDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
@Aspect
@Order(1)
public class CheckValidationAspect extends BaseAspect {

	private static final Logger log =
			LogManager.getLogger("applicationLogs.validation");

	private final ApplicationContext appContext;
	private final MessageSource messageSource;

	public CheckValidationAspect(
			ApplicationContext appContext,
			MessageSource messageSource
	) {
		this.appContext = appContext;
		this.messageSource = messageSource;
	}

	/**
	 * Server-side validation before controller method execution.
	 *
	 * <p>Supports both:</p>
	 * <ul>
	 *   <li><b>Forward rendering</b> - put errors into {@link Model}</li>
	 *   <li><b>PRG redirect</b> - put DTO + BindingResult into flash attributes (Spring-compatible)</li>
	 * </ul>
	 *
	 * <p><b>Hardening included:</b></p>
	 * <ul>
	 *   <li>Ensures BindingResult corresponds to the validated DTO parameter</li>
	 *   <li>Localizes messages using MessageSource</li>
	 *   <li>Fails fast with clear messages for misconfiguration</li>
	 * </ul>
	 */
	@Before("@annotation(validateEntity)")
	public void validateBefore(JoinPoint joinPoint, ValidateEntity validateEntity) {

		// ------------------------------------------------------------
		// 1) Guard: only apply to MVC controllers
		// ------------------------------------------------------------
		Class<?> targetClass = joinPoint.getTarget().getClass();
		if (!isMvcController(targetClass)) {
			return;
		}

		Locale locale = LocaleContextHolder.getLocale();
		Object[] args = joinPoint.getArgs();
		Method method = resolveMethod(joinPoint);

		/* ------------------------------------------------------------
		 * 2a) HARDENING: enforce exactly ONE AbstractDTO parameter
		 * ------------------------------------------------------------ */
		long dtoCount = Arrays.stream(args)
				.filter(AbstractDTO.class::isInstance)
				.count();

		if (dtoCount > 1) {
			throw new IllegalStateException(
					"@ValidateEntity supports only ONE AbstractDTO parameter per method: " +
							targetClass.getSimpleName() + "#" + method.getName()
			);
		}

		/* ------------------------------------------------------------
		 * 2b) Extract DTO + matching BindingResult + Model/RedirectAttributes
		 * ------------------------------------------------------------ */
		Extraction extraction = extractDtoAndBindingResult(method, args);

		AbstractDTO dto = extraction.dto;
		BindingResult bindingResult = extraction.bindingResult;

		if (dto == null) {
			throw new IllegalStateException(
					"@ValidateEntity requires a parameter extending AbstractDTO in "
							+ targetClass.getSimpleName() + "#" + method.getName()
			);
		}

		if (bindingResult == null) {
			throw new IllegalStateException(
					"@ValidateEntity requires a BindingResult parameter immediately after the DTO in "
							+ targetClass.getSimpleName() + "#" + method.getName()
			);
		}

		// Model / RedirectAttributes are optional
		Model model = findArg(args, Model.class);
		RedirectAttributes redirectAttributes = findArg(args, RedirectAttributes.class);

		// ------------------------------------------------------------
		// 3) Execute validator (stateless, type-safe)
		// ------------------------------------------------------------
		@SuppressWarnings("unchecked")
		BaseValidator<AbstractDTO> validator =
				(BaseValidator<AbstractDTO>) appContext.getBean(validateEntity.validator());

		if (!validator.supports(dto.getClass())) {
			throw new IllegalStateException(
					"Validator " + validator.getClass().getSimpleName() +
							" does not support DTO type " + dto.getClass().getSimpleName()
			);
		}

		validator.validate(dto, bindingResult, validateEntity.pageMode());

		// ------------------------------------------------------------
		// 4) Exit early if validation passed
		// ------------------------------------------------------------
		if (!bindingResult.hasErrors()) {
			return;
		}

		// ------------------------------------------------------------
		// 5) Build validation error map (localized; first error per field)
		// ------------------------------------------------------------
		Map<String, String> validationErrors = buildValidationErrorMap(bindingResult, locale);

		// ------------------------------------------------------------
		// 6) Log (expected business failure)
		// ------------------------------------------------------------
		String c = "[controller=" + targetClass.getSimpleName()
				+ "][method=" + method.getName()
				+ "][dto=" + dto.getClass().getSimpleName() + "]";

		log.warn("{} Validation failed fields={}", c, validationErrors.keySet());
		if (log.isDebugEnabled()) {
			validationErrors.forEach((field, msg) -> log.debug("{} - {}: {}", c, field, msg));
		}

		// ------------------------------------------------------------
		// 7) Prepare common page message (localized)
		// ------------------------------------------------------------
		String pageMessageText = messageSource.getMessage(
				"Validation.common.Page.ValidationErrorMessage",
				null,
				locale
		);

		PageMessage pageMessage = new PageMessage(
				"Validation Error",
				pageMessageText,
				PageMessageStyle.ERROR.getValue()
		);

		// ------------------------------------------------------------
		// 8) Publish errors for view (PRG via flash OR forward via model)
		// ------------------------------------------------------------
		if (redirectAttributes != null) {

			String attrName = extraction.modelAttributeName != null
					? extraction.modelAttributeName
					: (dto.getClass().getSimpleName().replaceFirst("DTO$", "") + "Dto");

			// --- existing ---
			redirectAttributes.addFlashAttribute(attrName, dto);
			redirectAttributes.addFlashAttribute(
					BindingResult.MODEL_KEY_PREFIX + attrName,
					bindingResult
			);
			redirectAttributes.addFlashAttribute("pageMode", validateEntity.pageMode());
			redirectAttributes.addFlashAttribute("validationErrors", validationErrors);
			redirectAttributes.addFlashAttribute("pageMessage", pageMessage);

			// --- CRITICAL FIX: also write to FlashMap (because we throw before controller returns) ---
			putFlash(attrName, dto);
			putFlash(BindingResult.MODEL_KEY_PREFIX + attrName, bindingResult);
			putFlash("pageMode", validateEntity.pageMode());
			putFlash("validationErrors", validationErrors);
			putFlash("pageMessage", pageMessage);
		}

		else if (model != null) {
			// Forward render
			model.addAttribute("pageMode", validateEntity.pageMode());
			model.addAttribute("validationErrors", validationErrors);
			model.addAttribute("pageMessage", pageMessage);

			// Also keep DTO + BindingResult in model in case your view uses Spring form bindings
			if (extraction.modelAttributeName != null) {
				model.addAttribute(extraction.modelAttributeName, dto);
				model.addAttribute(
						BindingResult.MODEL_KEY_PREFIX + extraction.modelAttributeName,
						bindingResult
				);
			}
		}

		// ------------------------------------------------------------
		// 9) Stop execution and return error view
		// ------------------------------------------------------------
		String errorView = validateEntity.errorView();
		if (errorView == null || errorView.isBlank()) {
			throw new IllegalStateException(
					"@ValidateEntity.errorView must be defined for " +
							targetClass.getSimpleName() + "#" + method.getName()
			);
		}

		throw new ValidationFailedException(
				validationErrors,
				pageMessage,
				errorView
		);
	}

	// ---------------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------------

	/**
	 * Extracts:
	 * - first AbstractDTO parameter
	 * - BindingResult that immediately follows that DTO parameter (critical correctness)
	 * - @ModelAttribute name if present on that DTO parameter
	 */
	private Extraction extractDtoAndBindingResult(Method method, Object[] args) {
		if (args == null || args.length == 0) {
			return new Extraction(null, null, null);
		}

		Parameter[] params = method.getParameters();

		int dtoIndex = -1;
		AbstractDTO dto = null;

		for (int i = 0; i < args.length && i < params.length; i++) {
			Object a = args[i];
			if (a instanceof AbstractDTO) {
				dtoIndex = i;
				dto = (AbstractDTO) a;
				break;
			}
		}

		if (dtoIndex < 0) {
			return new Extraction(null, null, null);
		}

		// BindingResult must be immediately after the DTO param by Spring MVC convention
		BindingResult br = null;
		if (dtoIndex + 1 < args.length && args[dtoIndex + 1] instanceof BindingResult b) {
			br = b;
		}

		// Determine model attribute name (for PRG-friendly flash keys)
		String modelAttrName = null;
		ModelAttribute ma = params[dtoIndex].getAnnotation(ModelAttribute.class);
		if (ma != null && !ma.value().isBlank()) {
			modelAttrName = ma.value().trim();
		}

		return new Extraction(dto, br, modelAttrName);
	}

	private Map<String, String> buildValidationErrorMap(BindingResult br, Locale locale) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError fe : br.getFieldErrors()) {
			// Localize via MessageSource if possible (preferred)
			String msg;
			try {
				msg = messageSource.getMessage(fe, locale);
			}
			catch (Exception ignore) {
				msg = fe.getDefaultMessage();
			}
			errors.putIfAbsent(fe.getField(), msg);
		}
		return errors;
	}

	private <T> T findArg(Object[] args, Class<T> type) {
		if (args == null) {
			return null;
		}
		for (Object a : args) {
			if (type.isInstance(a)) {
				return type.cast(a);
			}
		}
		return null;
	}

	private void putFlash(String key, Object value) {
		var ra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (ra == null) {
			return;
		}
		HttpServletRequest request = ra.getRequest();
		FlashMap flashMap = RequestContextUtils.getOutputFlashMap(request);
		flashMap.putIfAbsent(key, value);
	}

	private record Extraction(AbstractDTO dto, BindingResult bindingResult, String modelAttributeName) {
	}
}
