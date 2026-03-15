package elotech.taskmanager.dto.common.response;

import java.util.List;

import org.springframework.data.domain.Page;

public record PagedResponse<T>(
        List<T> content,
        long total,
        int currentPage,
        int totalPages) {

    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber(),
                page.getTotalPages());
    }
}