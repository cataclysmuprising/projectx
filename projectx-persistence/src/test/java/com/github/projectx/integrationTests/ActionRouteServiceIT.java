package com.github.projectx.integrationTests;

import com.github.projectx.CommonTestBase;
import com.github.projectx.persistence.dto.rba.ActionDTO;
import com.github.projectx.persistence.dto.rba.ActionRouteDTO;
import com.github.projectx.persistence.entity.rba.ActionRoute;
import com.github.projectx.persistence.service.rba.ActionRouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

public class ActionRouteServiceIT extends CommonTestBase {

	private static final String FETCH_GRAPH = "ActionRoute(routeTargets(action))";

	@Autowired
	private ActionRouteService actionRouteService;

	@Test(groups = "fetch")
	public void findActiveRoutesByAppName_includesDirectAndHelperRoutes() throws Exception {
		List<ActionRouteDTO> routes = actionRouteService.findActiveRoutesByAppName("projectx");

		assertFalse(routes.isEmpty());
		assertTrue(
				routes.stream().anyMatch(route ->
						"/web/sec/dashboard".equals(route.getRoutePattern())
								&& route.getRouteKind() == ActionRoute.RouteKind.PRIMARY
				)
		);
		assertTrue(
				routes.stream().anyMatch(route ->
						"/api/web/sec/dashboard/summary".equals(route.getRoutePattern())
								&& route.getRouteKind() == ActionRoute.RouteKind.PAGE_SUPPORT
				)
		);
	}

	@Test(groups = "fetch")
	public void applyCoverage_whenRouteRegistryHasNoRows_fallsBackToLegacyUrl() throws Exception {
		ActionDTO action = new ActionDTO();
		action.setId(Long.MAX_VALUE);
		action.setActionName("syntheticAction");
		action.setUrl("/web/sec/synthetic");

		actionRouteService.applyCoverage(List.of(action));

		assertEquals(action.getPrimaryRoute(), "/web/sec/synthetic");
		assertEquals(action.getTotalRouteCount(), Long.valueOf(1L));
		assertEquals(action.getPageSupportRouteCount(), Long.valueOf(0L));
		assertEquals(action.getSharedLookupRouteCount(), Long.valueOf(0L));
	}
}
