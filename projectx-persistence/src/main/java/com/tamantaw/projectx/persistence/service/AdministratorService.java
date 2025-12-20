package com.tamantaw.projectx.persistence.service;

import com.tamantaw.projectx.persistence.criteria.AdministratorCriteria;
import com.tamantaw.projectx.persistence.dto.AdministratorDTO;
import com.tamantaw.projectx.persistence.entity.Administrator;
import com.tamantaw.projectx.persistence.entity.QAdministrator;
import com.tamantaw.projectx.persistence.mapper.AdministratorMapper;
import com.tamantaw.projectx.persistence.repository.AdministratorRepository;
import com.tamantaw.projectx.persistence.service.base.BaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdministratorService
		extends BaseService<
		Administrator,
		QAdministrator,
		AdministratorCriteria,
		AdministratorDTO,
		AdministratorMapper> {

	private static final Logger log =
			LogManager.getLogger("serviceLogs." + AdministratorService.class.getSimpleName());

	@Autowired
	public AdministratorService(
			AdministratorRepository administratorRepository,
			AdministratorMapper mapper
	) {
		super(administratorRepository, mapper);
	}
}
