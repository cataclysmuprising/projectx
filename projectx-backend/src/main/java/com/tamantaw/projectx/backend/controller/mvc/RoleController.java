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

import java.util.HashSet;

import static com.tamantaw.projectx.backend.BackendApplication.SUPER_USER_ROLE_ID;

@Controller
@MVCLoggable(profile = "dev")
@RequestMapping("/web/sec/role")
public class RoleController extends BaseMVCController {

	private static final Logger applicationLogger = LogManager.getLogger("applicationLogs." + RoleController.class.getName());

	@Autowired
	private RoleService roleService;

	@Autowired
	private RoleActionService roleActionService;

	@Override
	public void subInit(Model model) {
		setAuthorities(model, "Role");
	}

	@GetMapping
	public String home() {
		return "/role/home";
	}

	@GetMapping("/add")
	public String add(Model model) {
		model.addAttribute("pageMode", PageMode.CREATE);
		model.addAttribute("roleDto", new RoleDTO());
		return "/role/input";
	}

	@PostMapping("/add")
	@ValidateEntity(validator = RoleValidator.class, errorView = "/role/input", pageMode = PageMode.CREATE)
	public String add(Model model, RedirectAttributes redirectAttributes, @ModelAttribute("roleDto") RoleDTO roleDto, BindingResult bindResult) throws ConsistencyViolationException, PersistenceException {
		roleDto.setRoleType(Role.RoleType.CUSTOM);
		roleDto.setAppName(BackendApplication.APP_NAME);
		RoleDTO newDto = roleService.create(roleDto, getSignInClientId());
		applicationLogger.info("New role has been successfully registered with ID# <{}>", newDto.getId());
		setPageMessage(redirectAttributes, "Success", "Notification.common.Page.SuccessfullyRegistered", PageMessageStyle.SUCCESS, "Role");
		return "redirect:/web/sec/role";
	}

	@GetMapping("/{id}/edit")
	public String edit(@PathVariable long id, Model model, RedirectAttributes redirectAttributes) throws PersistenceException {
		model.addAttribute("pageMode", PageMode.EDIT);
		RoleCriteria criteria = new RoleCriteria();
		criteria.setId(id);
		HashSet<Long> superUserRoleId = new HashSet<>();
		superUserRoleId.add(SUPER_USER_ROLE_ID);
		criteria.setExcludeIds(superUserRoleId);

		RoleDTO roleDTO = roleService.findOne(criteria, "Role(roleActions(action),administratorRoles(administrator))").orElseThrow(() -> new ContentNotFoundException("No matching role found for give ID # <" + id + ">"));
		if (roleDTO.getRoleType() == Role.RoleType.BUILT_IN) {
			setPageMessage(true, redirectAttributes, "Access Denied!", "Editing BUILD IN roles are not allowed!", PageMessageStyle.ERROR);
			return "redirect:/web/sec/role";
		}

		model.addAttribute("roleDto", roleDTO);
		return "/role/input";
	}

	@PostMapping("/{roleId}/edit")
	@ValidateEntity(validator = RoleValidator.class, errorView = "/web/role/input", pageMode = PageMode.EDIT)
	public String edit(Model model, @PathVariable long roleId, RedirectAttributes redirectAttributes, @ModelAttribute("roleDTO") RoleDTO roleDTO, BindingResult bindResult) throws PersistenceException, ConsistencyViolationException {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setId(roleId);
		HashSet<Long> superUserRoleId = new HashSet<>();
		superUserRoleId.add(SUPER_USER_ROLE_ID);
		criteria.setExcludeIds(superUserRoleId);

		if (!roleService.exists(criteria)) {
			throw new ContentNotFoundException("No matching role found for give ID # <" + roleId + ">");
		}

		roleDTO.setId(roleId);
		roleService.updateRoleAndActions(roleDTO, roleDTO.getActionIds(), getSignInClientId());
		setPageMessage(redirectAttributes, "Success", "Notification.common.Page.SuccessfullyUpdated", PageMessageStyle.SUCCESS, "Role");
		return "redirect:/sec/role";
	}

	@GetMapping("/{id}/delete")
	public String delete(@PathVariable long id, RedirectAttributes redirectAttributes) throws PersistenceException {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setId(id);
		HashSet<Long> superUserRoleId = new HashSet<>();
		superUserRoleId.add(SUPER_USER_ROLE_ID);
		criteria.setExcludeIds(superUserRoleId);
		RoleDTO roleDTO = roleService.findOne(criteria).orElseThrow(() -> new ContentNotFoundException("No matching role found for give ID # <" + id + ">"));

		if (roleDTO.getRoleType() == Role.RoleType.BUILT_IN) {
			setPageMessage(true, redirectAttributes, "Access Denied!", "Deleting BUILD IN roles are not allowed!", PageMessageStyle.ERROR);
			return "redirect:/web/sec/role";
		}
		roleService.deleteById(id);
		setPageMessage(redirectAttributes, "Success", "Notification.common.Page.SuccessfullyRemoved", PageMessageStyle.SUCCESS, "Role");
		return "redirect:/web/sec/role";
	}
}
