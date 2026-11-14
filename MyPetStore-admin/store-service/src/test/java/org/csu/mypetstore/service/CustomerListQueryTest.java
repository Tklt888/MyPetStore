package org.csu.mypetstore.service;

import org.csu.mypetstore.model.query.CustomerListQuery;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerListQueryTest {
    @Test
    void defaultsTrimAndEscapeLiteralLikeCharacters() {
        CustomerListQuery query = query("  a\\b!%c_d  ", null, null, null);
        query.normalize();
        assertEquals("a\\b!!!%c!_d", query.normalizedKeywordPattern());
        assertNull(query.normalizedStatus());
        assertEquals(1L, query.normalizedPage());
        assertEquals(10L, query.normalizedPageSize());
    }

    @Test
    void blankAndAllRemoveFiltersAndNegativePageUsesFirst() {
        CustomerListQuery query = query("　 ", " all ", -2L, 10L);
        query.normalize();
        assertNull(query.normalizedKeywordPattern());
        assertNull(query.normalizedStatus());
        assertEquals(1L, query.normalizedPage());
    }

    @Test
    void acceptsStatusAndPageSizeBoundaries() {
        assertEquals("ENABLED", normalizedStatus("enabled"));
        assertEquals("DISABLED", normalizedStatus("DISABLED"));
        CustomerListQuery one = query(null, null, 1L, 1L);
        CustomerListQuery fifty = query(null, null, 1L, 50L);
        one.normalize(); fifty.normalize();
        assertEquals(1L, one.normalizedPageSize());
        assertEquals(50L, fifty.normalizedPageSize());
    }

    @Test
    void rejectsUnknownStatusLongKeywordAndInvalidPageSize() {
        assertThrows(IllegalArgumentException.class, () -> query(null, "LOCKED", 1L, 10L).normalize());
        assertThrows(IllegalArgumentException.class, () -> query("甲".repeat(51), null, 1L, 10L).normalize());
        assertThrows(IllegalArgumentException.class, () -> query(null, null, 1L, 0L).normalize());
        assertThrows(IllegalArgumentException.class, () -> query(null, null, 1L, 51L).normalize());
    }

    private String normalizedStatus(String status) {
        CustomerListQuery query = query(null, status, 1L, 10L);
        query.normalize();
        return query.normalizedStatus();
    }

    private CustomerListQuery query(String keyword, String status, Long page, Long pageSize) {
        CustomerListQuery query = new CustomerListQuery();
        query.setKeyword(keyword); query.setStatus(status); query.setPage(page); query.setPageSize(pageSize);
        return query;
    }
}
