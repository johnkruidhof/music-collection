package nl.johnkruidhof.musiccollection.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import nl.johnkruidhof.musiccollection.dto.user.UserDto;
import nl.johnkruidhof.musiccollection.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** De ingelogde gebruiker; maakt bij de eerste aanroep een lokaal account aan. */
    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return userService.getCurrentUser(jwt);
    }
}
