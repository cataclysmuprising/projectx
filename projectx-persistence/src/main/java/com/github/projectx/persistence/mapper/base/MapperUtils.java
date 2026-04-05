package com.github.projectx.persistence.mapper.base;

import com.github.projectx.persistence.dto.base.AbstractDTO;
import com.github.projectx.persistence.dto.base.PaginatedResult;
import com.github.projectx.persistence.dto.base.SortItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public class MapperUtils<DTO extends AbstractDTO> {
	public static <T>
	PaginatedResult<T> toPaginatedResult(
			Page<?> page,
			List<T> data
	) {

		List<SortItem> sortItems = toSortItems(page.getSort());
		int responsePageNumber = resolveResponsePageNumber(page);

		return new PaginatedResult<>(
				page.getTotalElements(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.getSize(),
				responsePageNumber,
				page.getNumberOfElements(),
				sortItems,
				data
		);
	}

	public static <T>
	PaginatedResult<T> toPaginatedResult(
			Pageable pageable,
			long total,
			List<T> data
	) {
		int pageSize = pageable == null ? 0 : pageable.getPageSize();
		int pageNumber = resolveResponsePageNumber(pageable, total);
		int totalPages = pageSize <= 0 ? 0 : (int) Math.ceil((double) total / (double) pageSize);
		List<SortItem> sortItems = toSortItems(pageable == null ? Sort.unsorted() : pageable.getSort());

		return new PaginatedResult<>(
				total,
				total,
				totalPages,
				pageSize,
				pageNumber,
				data == null ? 0 : data.size(),
				sortItems,
				data
		);
	}

	private static int resolveResponsePageNumber(Page<?> page) {
		if (page == null) {
			return 0;
		}

		int totalPages = page.getTotalPages();
		if (totalPages <= 0) {
			return 0;
		}

		return Math.max(0, Math.min(page.getNumber(), totalPages - 1));
	}

	private static int resolveResponsePageNumber(Pageable pageable, long total) {
		if (pageable == null) {
			return 0;
		}

		int pageSize = pageable.getPageSize();
		if (pageSize <= 0) {
			return 0;
		}

		int totalPages = (int) Math.ceil((double) total / (double) pageSize);
		if (totalPages <= 0) {
			return 0;
		}

		int requestedPage = (int) (pageable.getOffset() / pageSize);
		return Math.max(0, Math.min(requestedPage, totalPages - 1));
	}

	private static List<SortItem> toSortItems(Sort sort) {
		if (sort == null || sort.isUnsorted()) {
			return List.of();
		}

		List<SortItem> items = new ArrayList<>();
		for (Sort.Order o : sort) {
			items.add(
					new SortItem(
							o.getProperty(),
							o.getDirection().name()
					)
			);
		}
		return items;
	}
}

