package com.techelevator.tenmo.services;

import com.techelevator.tenmo.model.AuthenticatedUser;
import com.techelevator.tenmo.model.Transfer;
import com.techelevator.tenmo.model.TransferDto;
import com.techelevator.util.BasicLogger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TransferService {

    private final String BASE_URL;
    private final RestTemplate restTemplate = new RestTemplate();

    public TransferService(String url) {
        this.BASE_URL = url;
    }

    public boolean sendBucks(AuthenticatedUser authenticatedUser, TransferDto transferDto) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authenticatedUser.getToken());
        HttpEntity<TransferDto> entity = new HttpEntity<>(transferDto, headers);

        try {
            restTemplate.exchange(BASE_URL + "/transfer", HttpMethod.POST, entity, Void.class);
            return true;
        } catch (Exception e) {
            BasicLogger.log(e.getMessage());
            System.out.println("Error initiating transfer: " + e.getMessage()); // Debugging output
            return false;
        }
    }



    public List<Transfer> getTransfers(AuthenticatedUser authenticatedUser) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authenticatedUser.getToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        List<Transfer> transfers = new ArrayList<>();
        try {
            ResponseEntity<Transfer[]> response = restTemplate.exchange(
                    BASE_URL + "/transfer?userId=" + authenticatedUser.getUser().getId(),
                    HttpMethod.GET,
                    entity,
                    Transfer[].class
            );
            if (response.getBody() != null) {
                transfers = Arrays.asList(response.getBody());
            }
        } catch (Exception e) {
            BasicLogger.log(e.getMessage());
        }
        return transfers;
    }

    public Transfer getTransferById(AuthenticatedUser authenticatedUser, int transferId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authenticatedUser.getToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        Transfer transfer = null;
        try {
            ResponseEntity<Transfer> response = restTemplate.exchange(
                    BASE_URL + "/transfer/" + transferId,
                    HttpMethod.GET,
                    entity,
                    Transfer.class
            );
            transfer = response.getBody();
        } catch (Exception e) {
            BasicLogger.log(e.getMessage());
        }
        return transfer;
    }

    public boolean requestTransfer(AuthenticatedUser authenticatedUser, TransferDto transferDto) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authenticatedUser.getToken());
        HttpEntity<TransferDto> entity = new HttpEntity<>(transferDto, headers);

        try {
            restTemplate.postForObject(BASE_URL + "/transfer/request", entity, Void.class);
            return true;
        } catch (Exception e) {
            BasicLogger.log(e.getMessage());
            System.out.println("Error requesting transfer: " + e.getMessage());
            return false;
        }
    }





    public List<Transfer> getPendingTransfers(AuthenticatedUser authenticatedUser) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authenticatedUser.getToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Transfer[]> response = restTemplate.exchange(
                    BASE_URL + "/transfer/pending?userId=" + authenticatedUser.getUser().getId(),
                    HttpMethod.GET, entity, Transfer[].class);

            List<Transfer> transfers = Arrays.asList(response.getBody());
            return transfers;
        } catch (Exception e) {
            BasicLogger.log("Error fetching pending transfers: " + e.getMessage());
            return new ArrayList<>();
        }
    }






    public boolean approveTransfer(AuthenticatedUser authenticatedUser, int transferId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authenticatedUser.getToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(BASE_URL + "/transfer/" + transferId + "/approve", HttpMethod.PUT, entity, Void.class);
            return true;
        } catch (Exception e) {
            BasicLogger.log(e.getMessage());
            return false;
        }
    }

    public boolean rejectTransfer(AuthenticatedUser authenticatedUser, int transferId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authenticatedUser.getToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(BASE_URL + "/transfer/" + transferId + "/reject", HttpMethod.PUT, entity, Void.class);
            return true;
        } catch (Exception e) {
            BasicLogger.log(e.getMessage());
            return false;
        }
    }








}
