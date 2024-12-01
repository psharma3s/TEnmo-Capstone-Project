package com.techelevator.tenmo.services;

import com.techelevator.tenmo.model.AuthenticatedUser;
import com.techelevator.tenmo.model.User;
import com.techelevator.util.BasicLogger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserService {

    private final String BASE_URL;
    private final RestTemplate restTemplate = new RestTemplate();

    public UserService(String url) {
        this.BASE_URL = url;
    }

    public List<User> listUsers(AuthenticatedUser authenticatedUser) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authenticatedUser.getToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        List<User> users = new ArrayList<>();
        try {
            User[] userArray = restTemplate.exchange(
                    BASE_URL + "/users?currentUserId=" + authenticatedUser.getUser().getId(),
                    HttpMethod.GET,
                    entity,
                    User[].class
            ).getBody();
            if (userArray != null) {
                users = Arrays.asList(userArray);
            }
        } catch (Exception e) {
            BasicLogger.log(e.getMessage());
        }
        return users;
    }

}
