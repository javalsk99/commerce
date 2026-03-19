package lsk.commerce.query.dto;

import lombok.Builder;

@Builder
public record ProductSearchCond(
        String categoryName,

        String productName,
        Integer minPrice,
        Integer maxPrice,

        String artist,
        String studio,

        String author,
        String isbn,

        String actor,
        String director
) {
}
