package com.tamantaw.projectx.persistence.dto.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.domain.Sort;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class PaginatedResult<DTO extends AbstractDTO> implements Serializable {

	@Serial
	private static final long serialVersionUID = -112619740430072473L;

	private long recordsTotal;
	private long recordsFiltered;
	private long totalPages;
	private int size;
	private int number;
	private int numberOfElements;
	private Sort sort;

	@ToString.Exclude
	private List<DTO> data;
}
