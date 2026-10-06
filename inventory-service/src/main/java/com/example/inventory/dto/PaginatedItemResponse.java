package com.example.inventory.dto;

import java.util.List;

public record PaginatedItemResponse(
        List<ItemResponse> items,
        int page,
        int perPage,
        long totalItems,
        int totalPages
) {

}
