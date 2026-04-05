package com.github.projectx.integrationTests;

import com.github.projectx.CommonTestBase;
import com.github.projectx.persistence.criteria.rba.AdministratorLoginHistoryCriteria;
import com.github.projectx.persistence.dto.base.PaginatedResult;
import com.github.projectx.persistence.dto.rba.AdministratorDTO;
import com.github.projectx.persistence.dto.rba.AdministratorLoginHistoryDTO;
import com.github.projectx.persistence.entity.rba.Administrator;
import com.github.projectx.persistence.entity.rba.QAdministratorLoginHistory;
import com.github.projectx.persistence.service.rba.AdministratorLoginHistoryService;
import com.github.projectx.persistence.service.rba.AdministratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.*;

public class AdministratorLoginHistoryServiceIT extends CommonTestBase {

	private static final String FETCH_GRAPH = "AdministratorLoginHistory(administrator)";

	@Autowired
	private AdministratorLoginHistoryService loginHistoryService;

	@Autowired
	private AdministratorService administratorService;

	@Test(groups = "fetch")
	public void create_persistsLoginHistory() throws Exception {
		AdministratorDTO admin = getSeedAdministrator();
		AdministratorLoginHistoryDTO dto = buildDto(admin.getId(), "127.0.0.1", "Ubuntu", "Mozilla");

		AdministratorLoginHistoryDTO saved = loginHistoryService.create(dto, TEST_CREATE_USER_ID);

		logger.info("Saved AdministratorLoginHistory info : " + saved);

		assertNotNull(saved.getId());
		assertEquals(saved.getCreatedBy(), TEST_CREATE_USER_ID);
		assertEquals(saved.getUpdatedBy(), TEST_CREATE_USER_ID);
		assertNotNull(saved.getLoginDate());
	}

	@Test(groups = "fetch")
	public void findById_existingLoginHistory() throws Exception {
		AdministratorDTO admin = getSeedAdministrator();
		AdministratorLoginHistoryDTO saved = loginHistoryService.create(
				buildDto(admin.getId(), "10.0.0.2", "Windows", "Chrome"),
				TEST_CREATE_USER_ID
		);

		AdministratorLoginHistoryCriteria criteria = new AdministratorLoginHistoryCriteria();
		criteria.setId(saved.getId());
		Optional<AdministratorLoginHistoryDTO> result = loginHistoryService.findOne(criteria);

		assertTrue(result.isPresent());
		assertEquals(result.get().getAdministratorId(), admin.getId());
		assertEquals(result.get().getOs(), "Windows");
		assertEquals(result.get().getClientAgent(), "Chrome");
	}

	@Test(groups = "fetch")
	public void findAll_byAdministratorId() throws Exception {
		AdministratorDTO admin = getSeedAdministrator();
		loginHistoryService.create(buildDto(admin.getId(), "192.168.1.10", "macOS", "Safari"), TEST_CREATE_USER_ID);
		loginHistoryService.create(buildDto(admin.getId(), "192.168.1.11", "macOS", "Safari"), TEST_CREATE_USER_ID);

		AdministratorLoginHistoryCriteria criteria = new AdministratorLoginHistoryCriteria();
		criteria.setAdministratorId(admin.getId());
		//criteria.addSort(QAdministratorLoginHistory.administratorLoginHistory.administrator.name, Sort.Direction.ASC);

		// this is String based sorting
		criteria.addSortKey("administrator.name", Sort.Direction.ASC);

		List<AdministratorLoginHistoryDTO> histories = loginHistoryService.findAll(criteria);

		assertFalse(histories.isEmpty());
		assertTrue(histories.stream().allMatch(h -> h.getAdministratorId().equals(admin.getId())));
	}

	@Test(groups = "fetch")
	public void findAll_withFetchGraph_includesAdministrator() throws Exception {
		AdministratorDTO admin = getSeedAdministrator();
		Long adminId = admin.getId();
		AdministratorLoginHistoryDTO saved = loginHistoryService.create(
				buildDto(adminId, "172.16.0.1", "Linux", "Edge"),
				TEST_CREATE_USER_ID
		);

		AdministratorLoginHistoryCriteria criteria = new AdministratorLoginHistoryCriteria();
		criteria.setId(saved.getId());
		criteria.setAdministratorId(adminId);

		var history =
				loginHistoryService.findOne(criteria);

		assertTrue(history.isPresent());

		AdministratorLoginHistoryDTO dto = history.get();
		assertEquals(dto.getAdministratorId(), adminId);
	}

	@Test(groups = "fetch")
	public void findByPaging_byAdministratorId_sortedByAdministratorName() throws Exception {

		// ------------------------------------------------------------------
		// Arrange
		// ------------------------------------------------------------------
		AdministratorDTO admin = getSeedAdministrator();
		loginHistoryService.create(
				buildDto(admin.getId(), "192.168.1.10", "macOS", "Safari"),
				TEST_CREATE_USER_ID
		);
		loginHistoryService.create(
				buildDto(admin.getId(), "192.168.1.11", "macOS", "Safari"),
				TEST_CREATE_USER_ID
		);

		AdministratorLoginHistoryCriteria criteria =
				new AdministratorLoginHistoryCriteria();

		criteria.setAdministratorId(admin.getId());
		criteria.setLimit(10);
		criteria.setOffset(0);

		// ✅ SAFE: to-one sorting
		criteria.addSort(
				QAdministratorLoginHistory
						.administratorLoginHistory
						.administrator
						.name,
				Sort.Direction.ASC
		);

		// ------------------------------------------------------------------
		// Act
		// ------------------------------------------------------------------
		PaginatedResult<AdministratorLoginHistoryDTO> page =
				loginHistoryService.findByPaging(
						criteria
				);

		// ------------------------------------------------------------------
		// Assert
		// ------------------------------------------------------------------
		assertNotNull(page);
		assertFalse(page.getData().isEmpty());

		// All records must belong to administratorId = 1
		assertTrue(
				page.getData().stream()
						.allMatch(h ->
								h.getAdministratorId() != null
										&& h.getAdministratorId().equals(admin.getId())
						)
		);
	}

	@Test(groups = "fetch")
	public void delete_removesLoginHistory() throws Exception {
		AdministratorDTO admin = getSeedAdministrator();
		loginHistoryService.create(buildDto(admin.getId(), "10.10.10.10", "Linux", "Firefox"), TEST_CREATE_USER_ID);

		AdministratorLoginHistoryCriteria criteria = new AdministratorLoginHistoryCriteria();
		criteria.setAdministratorId(admin.getId());
		criteria.setIpAddress("10.10.10.10");

		long deleted = loginHistoryService.delete(criteria);

		assertEquals(deleted, 1L);
	}

	@Test(groups = "fetch")
	public void update_withDto_updatesFieldsAndAudit() throws Exception {

		// --------------------------------------------------
		// GIVEN: a real administrator
		// --------------------------------------------------
		AdministratorDTO admin = getSeedAdministrator();

		AdministratorLoginHistoryDTO saved =
				loginHistoryService.create(
						buildDto(admin.getId(), "172.16.0.1", "macOS", "Safari"),
						TEST_CREATE_USER_ID
				);

		// --------------------------------------------------
		// WHEN: update non-relational fields
		// --------------------------------------------------
		AdministratorLoginHistoryDTO updateDto =
				new AdministratorLoginHistoryDTO();

		updateDto.setId(saved.getId());
		updateDto.setOs("Linux");
		updateDto.setClientAgent("Firefox");

		AdministratorLoginHistoryDTO updated =
				loginHistoryService.update(updateDto, QAdministratorLoginHistory.administratorLoginHistory, TEST_UPDATE_USER_ID);

		// --------------------------------------------------
		// THEN
		// --------------------------------------------------
		assertEquals(updated.getOs(), "Linux");
		assertEquals(updated.getClientAgent(), "Firefox");
		assertEquals(updated.getUpdatedBy(), TEST_UPDATE_USER_ID);
	}

	@Test(groups = "fetch")
	public void deleteById_removesLoginHistoryByIdentifier() throws Exception {

		// --------------------------------------------------
		// GIVEN: a real administrator
		// --------------------------------------------------
		AdministratorDTO admin = getSeedAdministrator();

		AdministratorLoginHistoryDTO saved =
				loginHistoryService.create(
						buildDto(admin.getId(), "172.16.0.2", "Windows", "Edge"),
						TEST_CREATE_USER_ID
				);

		// --------------------------------------------------
		// WHEN
		// --------------------------------------------------
		boolean deleted =
				loginHistoryService.deleteById(saved.getId());

		// --------------------------------------------------
		// THEN
		// --------------------------------------------------
		assertTrue(deleted);

		assertTrue(
				loginHistoryService.findById(saved.getId()).isEmpty()
		);
	}

	private AdministratorLoginHistoryDTO buildDto(Long adminId, String ip, String os, String agent) {
		AdministratorLoginHistoryDTO dto = new AdministratorLoginHistoryDTO();
		dto.setAdministratorId(adminId);
		dto.setIpAddress(ip);
		dto.setOs(os);
		dto.setClientAgent(agent);
		dto.setLoginDate(LocalDateTime.now());
		return dto;
	}

	private AdministratorDTO getSeedAdministrator() throws Exception {
		AdministratorDTO dto = new AdministratorDTO();
		dto.setName("LoginHistory Admin");
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		dto.setLoginId("it-loginhistory-" + suffix);
		dto.setPassword("secret");
		dto.setStatus(Administrator.Status.ACTIVE);
		return administratorService.create(dto, TEST_CREATE_USER_ID);
	}
}


