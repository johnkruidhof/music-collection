package nl.johnkruidhof.musiccollection.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import nl.johnkruidhof.musiccollection.config.CorsConfig;
import nl.johnkruidhof.musiccollection.config.PropertiesConfig;
import nl.johnkruidhof.musiccollection.config.SecurityConfig;
import nl.johnkruidhof.musiccollection.dto.user.UserDto;
import nl.johnkruidhof.musiccollection.service.UserService;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, CorsConfig.class, PropertiesConfig.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void meZonderTokenGeeft401() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meMetTokenGeeftGebruiker() throws Exception {
        UserDto dto = new UserDto(UUID.randomUUID(), "alice", "alice@example.com", "Alice Test", List.of("user"));
        given(userService.getCurrentUser(any(Jwt.class))).willReturn(dto);

        mockMvc.perform(get("/api/me").with(jwt().jwt(j -> j.subject("abc").claim("preferred_username", "alice"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.roles[0]").value("user"));
    }
}
