package com.mentorconnect;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
public class MentorConnectControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void testGetAllTagsReturnsOk() throws Exception {
        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(8))))
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    void testCreateTagBlankNameReturnsBadRequest() throws Exception {
        String invalidPayload = "{\"name\":\"\",\"category\":\"Test\"}";
        mockMvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Validation Failed")));
    }

    @Test
    void testGetAlumniByIdNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/alumni/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    void testGetTypeMismatchReturnsBadRequestInsteadOfInternalError() throws Exception {
        mockMvc.perform(get("/api/alumni/invalid-id-format"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Type Mismatch")));
    }

    @Test
    void testMethodNotAllowedReturns405() throws Exception {
        mockMvc.perform(post("/api/reports/engagement"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status", is(405)))
                .andExpect(jsonPath("$.error", is("Method Not Allowed")));
    }

    @Test
    void testSuggestMentorsForStudentReturnsRankedSuggestionsWithAlreadyMatched() throws Exception {
        mockMvc.perform(get("/api/matching/suggest/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].alumniName").exists())
                .andExpect(jsonPath("$[0].matchingTags").isArray());
    }

    @Test
    void testSuggestMentorsCustomTagsReturnsOk() throws Exception {
        String customTags = "[\"Java & Spring Boot\", \"Cloud Architecture (AWS)\"]";
        mockMvc.perform(post("/api/matching/suggest-custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customTags))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].matchCount").exists());
    }

    @Test
    void testNegativeIdInPairRequestReturnsBadRequest() throws Exception {
        String invalidReq = "{\"alumniId\": -1, \"studentId\": 1, \"notes\": \"Invalid test\"}";
        mockMvc.perform(post("/api/matching/pair")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidReq))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Failed")));
    }

    @Test
    void testGetEngagementReportReturnsOkWithAllAggregations() throws Exception {
        mockMvc.perform(get("/api/reports/engagement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAlumni", greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$.totalStudents", greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.totalPairs", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.pairReports").isArray());
    }

    @Test
    void testGetAllAuditLogsReturnsOk() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].action").exists())
                .andExpect(jsonPath("$[0].entityType").exists())
                .andExpect(jsonPath("$[0].timestamp").exists());
    }

    @Test
    void testGetAuditLogsPagedReturnsOk() throws Exception {
        mockMvc.perform(get("/api/audit-logs/paged?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.number", is(0)));
    }

    @Test
    void testGetAlumniPagedReturnsOk() throws Exception {
        mockMvc.perform(get("/api/alumni/paged?page=0&size=3&sort=name,asc"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$.size", is(3)));
    }

    @Test
    void testGetSessionsPagedReturnsOk() throws Exception {
        mockMvc.perform(get("/api/sessions/paged?page=0&size=2&sort=sessionDate,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$.size", is(2)));
    }

    @Test
    void testGetStudentsPagedReturnsOk() throws Exception {
        mockMvc.perform(get("/api/students/paged?page=0&size=2&sort=name,asc"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.size", is(2)));
    }

    @Test
    void testGetMentorshipPairsPagedReturnsOk() throws Exception {
        mockMvc.perform(get("/api/mentorship-pairs/paged?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.size", is(2)));
    }

    @Test
    void testInvalidSortPropertyReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/alumni/paged?sort=nonExistentProperty,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Invalid Sort Property")))
                .andExpect(jsonPath("$.message", containsString("nonExistentProperty")));
    }
}
