package com.techelevator.tenmo.services;

import com.techelevator.tenmo.model.AuthenticatedUser;
import com.techelevator.util.BasicLogger;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;

public class AccountService {

    private final String BASE_URL;
    private final RestTemplate restTemplate = new RestTemplate();

    public AccountService(String url) {
        this.BASE_URL = url;
    }

    public BigDecimal getBalance(AuthenticatedUser authenticatedUser) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authenticatedUser.getToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        BigDecimal balance = BigDecimal.ZERO;
        try {
            ResponseEntity<BigDecimal> response = restTemplate.exchange(
                    BASE_URL + "/account/balance?userId=" + authenticatedUser.getUser().getId(),
                    HttpMethod.GET,
                    entity,
                    BigDecimal.class
            );
            balance = response.getBody();
        } catch (Exception e) {
            BasicLogger.log(e.getMessage());
        }
        return balance;
    }
}
