package com.ninelives.safepaws.repository;

import com.ninelives.safepaws.dto.AnnouncementFilter;
import com.ninelives.safepaws.model.Announcement;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns an {@link AnnouncementFilter} into a WHERE clause. The distance check is not here:
 * the service does it, because it also needs the distance itself for the response.
 */
public final class AnnouncementSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private AnnouncementSpecifications() {
    }

    public static Specification<Announcement> matching(AnnouncementFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), filter.statusOrDefault()));
            if (filter.type() != null) {
                predicates.add(cb.equal(root.get("type"), filter.type()));
            }
            if (filter.species() != null) {
                predicates.add(cb.equal(root.get("species"), filter.species()));
            }
            if (filter.color() != null) {
                predicates.add(contains(cb, root, "color", filter.color()));
            }
            if (filter.breed() != null) {
                predicates.add(contains(cb, root, "breed", filter.breed()));
            }
            if (filter.area() != null) {
                predicates.add(contains(cb, root, "area", filter.area()));
            }
            if (filter.hasLocation()) {
                // without a position there is no distance, so these cannot be in a location search
                predicates.add(cb.isNotNull(root.get("latitude")));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Case- and diacritic-insensitive "contains", so "manastur" finds "Mănăștur".
     * Uses PostgreSQL's unaccent extension (V3__search_support.sql).
     */
    private static Predicate contains(CriteriaBuilder cb, Root<Announcement> root, String field, String text) {
        Expression<String> column = unaccent(cb, cb.lower(root.get(field)));
        Expression<String> pattern = unaccent(cb, cb.lower(cb.literal("%" + escapeLike(text) + "%")));
        return cb.like(column, pattern, LIKE_ESCAPE);
    }

    private static Expression<String> unaccent(CriteriaBuilder cb, Expression<String> value) {
        return cb.function("unaccent", String.class, value);
    }

    /**
     * % and _ typed by the user are matched literally, not as wildcards.
     */
    static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
