package com.tamantaw.projectx.backend.controller.mvc;

import com.tamantaw.projectx.backend.common.annotation.MVCLoggable;
import com.tamantaw.projectx.backend.common.annotation.ValidateEntity;
import com.tamantaw.projectx.backend.common.response.PageMessageStyle;
import com.tamantaw.projectx.backend.common.response.PageMode;
import com.tamantaw.projectx.backend.common.validation.BaseValidator;
import com.tamantaw.projectx.backend.validators.AdmministratorValidator;
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

import java.util.HashSet;

import static com.tamantaw.projectx.backend.BackendApplication.SUPER_USER_ID;

@Controller
@MVCLoggable(profile = "dev")
@RequestMapping("/web/sec/administrator")
public class AdministratorController extends BaseMVCController {

	private static final Logger applicationLogger = LogManager.getLogger("applicationLogs." + AdministratorController.class.getName());
	@Autowired
	protected BaseValidator baseValidator;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private AdministratorService administratorService;

	@GetMapping
	public String home() {
		return "/administrator/home";
	}

	@GetMapping("/add")
	public String add(Model model) {
		model.addAttribute("pageMode", PageMode.CREATE);
		model.addAttribute("administratorDto", new AdministratorDTO());
		return "/administrator/input";
	}

	@PostMapping("/add")
	@ValidateEntity(validator = AdmministratorValidator.class, errorView = "/users/input", pageMode = PageMode.CREATE)
	public String add(Model model, RedirectAttributes redirectAttributes, @ModelAttribute("administratorDto") AdministratorDTO administratorDto, BindingResult bindResult) throws ConsistencyViolationException, PersistenceException {
		administratorDto.setPassword(passwordEncoder.encode(administratorDto.getPassword()));
		Administrator registeredAdmin = administratorService.create(administratorDto, administratorDto.getRoleIds(), getSignInClientId());
		applicationLogger.info("New administrator has been successfully registered with ID# <{}>", registeredAdmin.getId());
		setPageMessage(redirectAttributes, "Success", "Notification.common.Page.SuccessfullyRegistered", PageMessageStyle.SUCCESS, "Administrator");
		return "redirect:/web/sec/administrator";
	}

	@GetMapping("/{id}/edit")
	public String edit(@PathVariable long id, Model model) throws PersistenceException {
		model.addAttribute("pageMode", PageMode.EDIT);
		AdministratorCriteria criteria = new AdministratorCriteria();
		HashSet<Long> superUserId = new HashSet<>();
		superUserId.add(SUPER_USER_ID);
		criteria.setExcludeIds(superUserId);
		criteria.setId(id);

		AdministratorDTO administratorDto = administratorService.findOne(criteria, "Administrator(roles)")
				.orElseThrow(() -> new ContentNotFoundException("No matching administrator found for give ID # <" + id + ">"));

		model.addAttribute("administratorDto", administratorDto);

		return "/administrator/input";
	}

	@PostMapping("/{id}/edit")
	@ValidateEntity(validator = AdmministratorValidator.class, errorView = "/users/input", pageMode = PageMode.EDIT)
	public String edit(Model model, @PathVariable long id, RedirectAttributes redirectAttributes, @ModelAttribute("administratorDto") AdministratorDTO administratorDto, BindingResult bindResult) throws PersistenceException, ConsistencyViolationException {
		AdministratorCriteria criteria = new AdministratorCriteria();
		HashSet<Long> superUserId = new HashSet<>();
		superUserId.add(SUPER_USER_ID);
		criteria.setExcludeIds(superUserId);
		criteria.setId(id);

		if (!administratorService.exists(criteria)) {
			throw new ContentNotFoundException("No matching administrator found for give ID # <" + id + ">");
		}

		administratorService.updateAdministratorAndRoles(administratorDto, administratorDto.getRoleIds(), getSignInClientId());
		setPageMessage(redirectAttributes, "Success", "Notification.common.Page.SuccessfullyUpdated", PageMessageStyle.SUCCESS, "Administrator");
		return "redirect:/web/sec/administrator";
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable long id, Model model) throws PersistenceException {
		model.addAttribute("pageMode", PageMode.VIEW);
		AdministratorCriteria criteria = new AdministratorCriteria();
		HashSet<Long> superUserId = new HashSet<>();
		superUserId.add(SUPER_USER_ID);
		criteria.setExcludeIds(superUserId);
		criteria.setId(id);

		AdministratorDTO administratorDto = administratorService.findOne(criteria, "Administrator(roles)")
				.orElseThrow(() -> new ContentNotFoundException("No matching administrator found for give ID # <" + id + ">"));

		model.addAttribute("administratorDto", administratorDto);

		String rolNames = String.join(",", administratorDto.getRoles().stream().map(RoleDTO::getName).toList());
		model.addAttribute("roleNames", rolNames);
		return "/administrator/detail";
	}

	@GetMapping("/{id}/delete")
	public String delete(@PathVariable long id, RedirectAttributes redirectAttributes) throws PersistenceException {
		// can't remove byself
		if (id == getSignInClientId()) {
			setPageMessage(redirectAttributes, "Error", "You can't remove yourself.", PageMessageStyle.ERROR);
			return "redirect:/sec/administrator";
		}
		AdministratorCriteria criteria = new AdministratorCriteria();
		HashSet<Long> superUserId = new HashSet<>();
		superUserId.add(SUPER_USER_ID);
		criteria.setExcludeIds(superUserId);
		criteria.setId(id);

		if (!administratorService.exists(criteria)) {
			throw new ContentNotFoundException("No matching administrator found for give ID # <" + id + ">");
		}
		administratorService.deleteById(id);
		setPageMessage(redirectAttributes, "Success", "Notification.common.Page.SuccessfullyRemoved", PageMessageStyle.SUCCESS, "Administrator");
		return "redirect:/web/sec/administrator";
	}

	@Override
	public void subInit(Model model) {
		setAuthorities(model, "Administrator");
	}
}
