package nl.johnkruidhof.musiccollection.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import nl.johnkruidhof.musiccollection.config.CorsConfig;
import nl.johnkruidhof.musiccollection.config.PropertiesConfig;
import nl.johnkruidhof.musiccollection.config.SecurityConfig;
import nl.johnkruidhof.musiccollection.dto.admin.AdminReleasePageDto;
import nl.johnkruidhof.musiccollection.service.AdminCatalogService;

@WebMvcTest(AdminCatalogController.class)
@Import({SecurityConfig.class, CorsConfig.class, PropertiesConfig.class})
class AdminCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminCatalogService adminCatalogService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void adminCanSearchCatalog() throws Exception {
        given(adminCatalogService.search(any(), any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt())).willReturn(new AdminReleasePageDto(List.of(), 0, 25, 0, 0, 3));

        mockMvc.perform(get("/api/admin/releases").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unusedCount").value(3));
    }

    @Test
    void regularUserIsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/releases").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_user"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admin/releases/unused").with(jwt()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(adminCatalogService);
    }

    @Test
    void mergeRequiresTarget() throws Exception {
        mockMvc.perform(post("/api/admin/releases/" + UUID.randomUUID() + "/merge").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"));
    }
}
