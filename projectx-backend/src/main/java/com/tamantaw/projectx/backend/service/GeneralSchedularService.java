package com.tamantaw.projectx.backend.service;

import com.tamantaw.projectx.backend.utils.ActionRegistry;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("prd")
public class GeneralSchedularService {

	private final ActionRegistry actionRegistry;

	public GeneralSchedularService(
			ActionRegistry actionRegistry
	) {
		this.actionRegistry = actionRegistry;
	}

	@Scheduled(fixedDelay = 10 * 60 * 1000) // every 10 mins
	public void reloadActions() {
		actionRegistry.reload();
	}
}

