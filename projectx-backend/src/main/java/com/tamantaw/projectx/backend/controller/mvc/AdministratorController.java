package com.tamantaw.projectx.backend.controller.mvc;

import com.tamantaw.projectx.backend.common.annotation.MVCLoggable;
import com.tamantaw.projectx.backend.common.annotation.ValidateEntity;
import com.tamantaw.projectx.backend.common.response.PageMessageStyle;
import com.tamantaw.projectx.backend.common.response.PageMode;
import com.tamantaw.projectx.backend.validators.AdministratorValidator;
import com.tamantaw.projectx.persistence.criteria.AdministratorCriteria;
import com.tamantaw.projectx.persistence.dto.AdministratorDTO;
import com.tamantaw.projectx.persistence.dto.RoleDTO;
import com.tamantaw.projectx.persistence.entity.Administrator;
import com.tamantaw.projectx.persistence.exception.ConsistencyViolationException;
import com.tamantaw.projectx.persistence.exception.ContentNotFoundException;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.service.AdministratorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

import static com.tamantaw.projectx.backend.BackendApplication.SUPER_USER_ID;

@Controller
@MVCLoggable(profile = "dev")
@RequestMapping("/web/sec/administrator")
public class AdministratorController extends BaseMVCController {

	private static final Logger log =
			LogManager.getLogger("applicationLogs." + AdministratorController.class.getName());

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AdministratorService administratorService;

	/* ============================================================
	 * Home
	 * ============================================================ */

	@GetMapping
	public String home() {
		return "/administrator/home";
	}

	/* ============================================================
	 * Create
	 * ============================================================ */

	@GetMapping("/add")
	public String add(Model model) {

		// Preserve PRG values if present
		if (!model.containsAttribute("pageMode")) {
			model.addAttribute("pageMode", PageMode.CREATE);
		}

		if (!model.containsAttribute("administratorDto")) {
			model.addAttribute("administratorDto", new AdministratorDTO());
		}

		return "/administrator/input";
	}

	@PostMapping("/add")
	@ValidateEntity(
			validator = AdministratorValidator.class,
			errorView = "redirect:/web/sec/administrator/add",
			pageMode = PageMode.CREATE
	)
	public String add(
			@ModelAttribute("administratorDto") AdministratorDTO administratorDto,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes
	) throws PersistenceException, ConsistencyViolationException {

		administratorDto.setPassword(
				passwordEncoder.encode(administratorDto.getPassword())
		);

		Administrator registered =
				administratorService.create(
						administratorDto,
						administratorDto.getRoleIds(),
						getSignInClientId()
				);

		log.info(
				"New administrator registered id={}",
				registered.getId()
		);

		setPageMessage(
				redirectAttributes,
				"Success",
				"Notification.common.Page.SuccessfullyRegistered",
				PageMessageStyle.SUCCESS,
				"Administrator"
		);

		return "redirect:/web/sec/administrator";
	}

	/* ============================================================
	 * Edit
	 * ============================================================ */

	@GetMapping("/{id}/edit")
	public String edit(@PathVariable long id, Model model)
			throws PersistenceException {

		model.addAttribute("pageMode", PageMode.EDIT);

		AdministratorDTO dto = administratorService
				.findOne(buildCriteria(id), "Administrator(administratorRoles(role))")
				.orElseThrow(() ->
						new ContentNotFoundException(
								"No matching administrator found for ID <" + id + ">"
						)
				);

		model.addAttribute("administratorDto", dto);
		return "/administrator/input";
	}

	@PostMapping("/{id}/edit")
	@ValidateEntity(
			validator = AdministratorValidator.class,
			errorView = "redirect:/web/sec/administrator/{id}/edit",
			pageMode = PageMode.EDIT
	)
	public String edit(
			@PathVariable long id,
			@ModelAttribute("administratorDto") AdministratorDTO administratorDto,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes
	) throws PersistenceException, ConsistencyViolationException {

		if (!administratorService.exists(buildCriteria(id))) {
			throw new ContentNotFoundException(
					"No matching administrator found for ID <" + id + ">"
			);
		}

		administratorService.updateAdministratorAndRoles(
				administratorDto,
				administratorDto.getRoleIds(),
				getSignInClientId()
		);

		setPageMessage(
				redirectAttributes,
				"Success",
				"Notification.common.Page.SuccessfullyUpdated",
				PageMessageStyle.SUCCESS,
				"Administrator"
		);

		return "redirect:/web/sec/administrator";
	}

	/* ============================================================
	 * Detail
	 * ============================================================ */

	@GetMapping("/{id}")
	public String detail(@PathVariable long id, Model model)
			throws PersistenceException {

		model.addAttribute("pageMode", PageMode.VIEW);

		AdministratorDTO dto = administratorService
				.findOne(buildCriteria(id), "Administrator(administratorRoles(role))")
				.orElseThrow(() ->
						new ContentNotFoundException(
								"No matching administrator found for ID <" + id + ">"
						)
				);

		model.addAttribute("administratorDto", dto);

		String roleNames = String.join(",", dto.getRoles()
				.stream()
				.map(RoleDTO::getName)
				.toList());

		model.addAttribute("roleNames", roleNames);

		return "/administrator/detail";
	}

	/* ============================================================
	 * Delete
	 * ============================================================ */

	@GetMapping("/{id}/delete")
	public String delete(
			@PathVariable long id,
			RedirectAttributes redirectAttributes
	) throws PersistenceException {

		if (id == getSignInClientId()) {
			setPageMessage(
					redirectAttributes,
					"Error",
					"You can't remove yourself.",
					PageMessageStyle.ERROR
			);
			return "redirect:/web/sec/administrator";
		}

		if (!administratorService.exists(buildCriteria(id))) {
			throw new ContentNotFoundException(
					"No matching administrator found for ID <" + id + ">"
			);
		}

		administratorService.deleteById(id);

		setPageMessage(
				redirectAttributes,
				"Success",
				"Notification.common.Page.SuccessfullyRemoved",
				PageMessageStyle.SUCCESS,
				"Administrator"
		);

		return "redirect:/web/sec/administrator";
	}

	/* ============================================================
	 * Common
	 * ============================================================ */

	@Override
	public void subInit(Model model) {
		setAuthorities(model, "Administrator");
	}

	private AdministratorCriteria buildCriteria(long id) {
		AdministratorCriteria criteria = new AdministratorCriteria();
		criteria.setId(id);
		criteria.setExcludeIds(Set.of(SUPER_USER_ID));
		return criteria;
	}
}

