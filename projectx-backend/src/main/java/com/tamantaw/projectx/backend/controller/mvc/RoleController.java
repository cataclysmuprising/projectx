package com.tamantaw.projectx.backend.controller.mvc;

import com.tamantaw.projectx.backend.BackendApplication;
import com.tamantaw.projectx.backend.common.annotation.MVCLoggable;
import com.tamantaw.projectx.backend.common.annotation.ValidateEntity;
import com.tamantaw.projectx.backend.common.response.PageMessageStyle;
import com.tamantaw.projectx.backend.common.response.PageMode;
import com.tamantaw.projectx.backend.validators.RoleValidator;
import com.tamantaw.projectx.persistence.criteria.RoleCriteria;
import com.tamantaw.projectx.persistence.dto.RoleDTO;
import com.tamantaw.projectx.persistence.entity.Role;
import com.tamantaw.projectx.persistence.exception.ConsistencyViolationException;
import com.tamantaw.projectx.persistence.exception.ContentNotFoundException;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.service.RoleActionService;
import com.tamantaw.projectx.persistence.service.RoleService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

import static com.tamantaw.projectx.backend.BackendApplication.SUPER_USER_ROLE_ID;

@Controller
@MVCLoggable(profile = "dev")
@RequestMapping("/web/sec/role")
public class RoleController extends BaseMVCController {

	private static final Logger log =
			LogManager.getLogger("applicationLogs." + RoleController.class.getName());

	@Autowired
	private RoleService roleService;

	@Autowired
	private RoleActionService roleActionService;

	/* ============================================================
	 * Init
	 * ============================================================ */

	@Override
	public void subInit(Model model) {
		setAuthorities(model, "Role");
	}

	/* ============================================================
	 * Home
	 * ============================================================ */

	@GetMapping
	public String home() {
		return "/role/home";
	}

	/* ============================================================
	 * Create
	 * ============================================================ */

	@GetMapping("/add")
	public String add(Model model) {
		model.addAttribute("pageMode", PageMode.CREATE);
		model.addAttribute("roleDto", new RoleDTO());
		return "/role/input";
	}

	@PostMapping("/add")
	@ValidateEntity(
			validator = RoleValidator.class,
			errorView = "redirect:/web/sec/role/add",
			pageMode = PageMode.CREATE
	)
	public String add(
			@ModelAttribute("roleDto") RoleDTO roleDto,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes
	) throws PersistenceException, ConsistencyViolationException {

		roleDto.setRoleType(Role.RoleType.CUSTOM);
		roleDto.setAppName(BackendApplication.APP_NAME);

		RoleDTO created =
				roleService.create(roleDto, getSignInClientId());

		log.info(
				"New role registered id={}",
				created.getId()
		);

		setPageMessage(
				redirectAttributes,
				"Success",
				"Notification.common.Page.SuccessfullyRegistered",
				PageMessageStyle.SUCCESS,
				"Role"
		);

		return "redirect:/web/sec/role";
	}

	/* ============================================================
	 * Edit
	 * ============================================================ */

	@GetMapping("/{id}/edit")
	public String edit(
			@PathVariable long id,
			Model model,
			RedirectAttributes redirectAttributes
	) throws PersistenceException {

		model.addAttribute("pageMode", PageMode.EDIT);

		RoleDTO roleDto = roleService
				.findOne(buildCriteria(id),
						"Role(roleActions(action),administratorRoles(administrator))")
				.orElseThrow(() ->
						new ContentNotFoundException(
								"No matching role found for ID <" + id + ">"
						)
				);

		if (roleDto.getRoleType() == Role.RoleType.BUILT_IN) {
			setPageMessage(
					redirectAttributes,
					"Access Denied!",
					"Editing BUILD IN roles are not allowed!",
					PageMessageStyle.ERROR
			);
			return "redirect:/web/sec/role";
		}

		model.addAttribute("roleDto", roleDto);
		return "/role/input";
	}

	@PostMapping("/{id}/edit")
	@ValidateEntity(
			validator = RoleValidator.class,
			errorView = "redirect:/web/sec/role/{id}/edit",
			pageMode = PageMode.EDIT
	)
	public String edit(
			@PathVariable long id,
			@ModelAttribute("roleDto") RoleDTO roleDto,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes
	) throws PersistenceException, ConsistencyViolationException {

		if (!roleService.exists(buildCriteria(id))) {
			throw new ContentNotFoundException(
					"No matching role found for ID <" + id + ">"
			);
		}

		roleDto.setId(id);

		roleService.updateRoleAndActions(
				roleDto,
				roleDto.getActionIds(),
				getSignInClientId()
		);

		setPageMessage(
				redirectAttributes,
				"Success",
				"Notification.common.Page.SuccessfullyUpdated",
				PageMessageStyle.SUCCESS,
				"Role"
		);

		return "redirect:/web/sec/role";
	}

	/* ============================================================
	 * Delete
	 * ============================================================ */

	@GetMapping("/{id}/delete")
	public String delete(
			@PathVariable long id,
			RedirectAttributes redirectAttributes
	) throws PersistenceException {

		RoleDTO roleDto = roleService
				.findOne(buildCriteria(id))
				.orElseThrow(() ->
						new ContentNotFoundException(
								"No matching role found for ID <" + id + ">"
						)
				);

		if (roleDto.getRoleType() == Role.RoleType.BUILT_IN) {
			setPageMessage(
					redirectAttributes,
					"Access Denied!",
					"Deleting BUILD IN roles are not allowed!",
					PageMessageStyle.ERROR
			);
			return "redirect:/web/sec/role";
		}

		roleService.deleteById(id);

		setPageMessage(
				redirectAttributes,
				"Success",
				"Notification.common.Page.SuccessfullyRemoved",
				PageMessageStyle.SUCCESS,
				"Role"
		);

		return "redirect:/web/sec/role";
	}

	/* ============================================================
	 * Helpers
	 * ============================================================ */

	private RoleCriteria buildCriteria(long id) {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setId(id);
		criteria.setExcludeIds(Set.of(SUPER_USER_ROLE_ID));
		return criteria;
	}
}
