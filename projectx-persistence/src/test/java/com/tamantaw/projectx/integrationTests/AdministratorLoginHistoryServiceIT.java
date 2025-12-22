package com.tamantaw.projectx.integrationTests;

import com.tamantaw.projectx.CommonTestBase;
import com.tamantaw.projectx.persistence.criteria.AdministratorLoginHistoryCriteria;
import com.tamantaw.projectx.persistence.dto.AdministratorDTO;
import com.tamantaw.projectx.persistence.dto.AdministratorLoginHistoryDTO;
import com.tamantaw.projectx.persistence.entity.AdministratorLoginHistory;
import com.tamantaw.projectx.persistence.exception.ConsistencyViolationException;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.service.AdministratorLoginHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.testng.Assert.*;

public class AdministratorLoginHistoryServiceIT extends CommonTestBase {

	@Autowired
	private AdministratorLoginHistoryService loginHistoryService;

	@Test
	public void create_persistsLoginHistory() throws ConsistencyViolationException, PersistenceException {
		AdministratorLoginHistoryDTO dto = buildDto(1L, "127.0.0.1", "Ubuntu", "Mozilla");

		AdministratorLoginHistory saved = loginHistoryService.create(dto, TEST_CREATE_USER_ID);

		assertNotNull(saved.getId());
		assertEquals(saved.getAdministrator().getId(), 1L);
		assertEquals(saved.getCreatedBy(), TEST_CREATE_USER_ID);
		assertEquals(saved.getUpdatedBy(), TEST_CREATE_USER_ID);
		assertNotNull(saved.getLoginDate());
	}

	@Test
	public void findById_existingLoginHistory() throws Exception {
		AdministratorLoginHistory saved = loginHistoryService.create(
				buildDto(2L, "10.0.0.2", "Windows", "Chrome"),
				TEST_CREATE_USER_ID
		);

		entityManager.flush();
		entityManager.clear();

		AdministratorLoginHistoryCriteria criteria = new AdministratorLoginHistoryCriteria();
		criteria.setId(saved.getId());
		Optional<AdministratorLoginHistoryDTO> result = loginHistoryService.findOne(criteria, "AdministratorLoginHistory(administrator)");

		assertTrue(result.isPresent());
		assertEquals(result.get().getAdministrator().getId(), 2L);
		assertEquals(result.get().getOs(), "Windows");
		assertEquals(result.get().getClientAgent(), "Chrome");
	}

	@Test
	public void findAll_byAdministratorId() throws Exception {
		loginHistoryService.create(buildDto(1L, "192.168.1.10", "macOS", "Safari"), TEST_CREATE_USER_ID);
		loginHistoryService.create(buildDto(1L, "192.168.1.11", "macOS", "Safari"), TEST_CREATE_USER_ID);

		AdministratorLoginHistoryCriteria criteria = new AdministratorLoginHistoryCriteria();
		criteria.setAdministratorId(1L);

		List<AdministratorLoginHistoryDTO> histories = loginHistoryService.findAll(criteria, "AdministratorLoginHistory(administrator)");

		assertFalse(histories.isEmpty());
		assertTrue(histories.stream().allMatch(h -> h.getAdministrator().getId().equals(1L)));
	}

	@Test
	public void delete_removesLoginHistory() throws ConsistencyViolationException, PersistenceException {
		loginHistoryService.create(buildDto(3L, "10.10.10.10", "Linux", "Firefox"), TEST_CREATE_USER_ID);

		AdministratorLoginHistoryCriteria criteria = new AdministratorLoginHistoryCriteria();
		criteria.setAdministratorId(3L);
		criteria.setIpAddress("10.10.10.10");

		long deleted = loginHistoryService.delete(criteria);

		assertEquals(deleted, 1L);
	}

	private AdministratorLoginHistoryDTO buildDto(Long adminId, String ip, String os, String agent) {
		AdministratorDTO adminRef = new AdministratorDTO();
		adminRef.setId(adminId);

		AdministratorLoginHistoryDTO dto = new AdministratorLoginHistoryDTO();
		dto.setAdministrator(adminRef);
		dto.setIpAddress(ip);
		dto.setOs(os);
		dto.setClientAgent(agent);
		dto.setLoginDate(LocalDateTime.now());
		return dto;
	}
}
