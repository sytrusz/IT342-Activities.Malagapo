package com.malagapo.OAuth2Login.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.malagapo.OAuth2Login.model.Contact;
import com.malagapo.OAuth2Login.service.GoogleContactsService;
 
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

@Controller
@RequestMapping("/contacts")
public class ContactsController {
 
    private final GoogleContactsService googleContactsService;
    private final OAuth2AuthorizedClientService authorizedClientService;
 
    public ContactsController(GoogleContactsService googleContactsService, OAuth2AuthorizedClientService authorizedClientService) {
        this.googleContactsService = googleContactsService;
        this.authorizedClientService = authorizedClientService;
    }
 
    @GetMapping
    public String getContacts(OAuth2AuthenticationToken authentication, Model model) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
            authentication.getAuthorizedClientRegistrationId(),
            authentication.getName()
        );
 
        if (client == null) {
            System.out.println("❌ No authorized client found!");
            model.addAttribute("contacts", List.of());
            return "contacts";
        }
 
        String accessToken = client.getAccessToken().getTokenValue();
        System.out.println("✅ Access Token: " + accessToken);
 
        try {
            List<Contact> contacts = googleContactsService.getContacts(accessToken);
            model.addAttribute("contacts", contacts);
        } catch (Exception e) {
            System.out.println("❌ ERROR fetching contacts: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("contacts", List.of());
        }
 
        return "contacts";
    }


    @PostMapping("/edit")
    public String editContact(
        @RequestParam String resourceName, 
        @RequestParam String name, 
        @RequestParam String email, 
        @RequestParam(required = false, defaultValue = "") String phoneNumber, 
        OAuth2AuthenticationToken authentication
    ) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
            authentication.getAuthorizedClientRegistrationId(),
            authentication.getName()
        );

        if (client != null) {
            Contact contact = new Contact(resourceName, name, email, phoneNumber);
            boolean updateSuccessful = googleContactsService.updateContact(
                client.getAccessToken().getTokenValue(), 
                resourceName, 
                contact
            );

            if (!updateSuccessful) {
                // Add error handling or logging
                System.err.println("Failed to update contact: " + resourceName);
            }
        }

        return "redirect:/contacts";
    }

    @PostMapping("/delete")
    public String deleteContact(@RequestParam String resourceName, OAuth2AuthenticationToken authentication) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
            authentication.getAuthorizedClientRegistrationId(),
            authentication.getName()
        );

        if (client != null) {
            googleContactsService.deleteContact(client.getAccessToken().getTokenValue(), resourceName);
        }

        return "redirect:/contacts";
    }
}