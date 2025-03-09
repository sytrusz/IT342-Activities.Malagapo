package com.malagapo.OAuth2Login.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.malagapo.OAuth2Login.model.Contact;
import com.malagapo.OAuth2Login.service.GoogleContactsService;

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
            System.out.println("No authorized client found!");
            model.addAttribute("contacts", List.of());
            return "contacts";
        }
 
        String accessToken = client.getAccessToken().getTokenValue();
        System.out.println("Access Token: " + accessToken);
 
        try {
            List<Contact> contacts = googleContactsService.getContacts(accessToken);
            model.addAttribute("contacts", contacts);
        } catch (Exception e) {
            System.out.println("ERROR fetching contacts: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("contacts", List.of());
        }
 
        return "contacts";
    }

    // Add
    @PostMapping("/add")
    public String addContact(
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
            try {
                // Create a new Contact object
                Contact newContact = new Contact();
                newContact.setName(name);
                newContact.setEmail(email);
                newContact.setPhoneNumber(phoneNumber);

                // Call the service to create the contact
                String resourceName = googleContactsService.createContact(
                    client.getAccessToken().getTokenValue(), 
                    newContact
                );

                if (resourceName == null) {
                    System.err.println("Failed to create new contact");
                } else {
                    System.out.println("Successfully created contact with resourceName: " + resourceName);
                }
            } catch (Exception e) {
                System.err.println("Error creating contact: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return "redirect:/contacts";
    }

    // Edit
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
            try {
                // Fetch the contacts to get the etag for the specific contact
                List<Contact> contacts = googleContactsService.getContacts(client.getAccessToken().getTokenValue());
                
                // Find the contact to update
                Contact contactToUpdate = contacts.stream()
                    .filter(c -> c.getResourceName().equals(resourceName))
                    .findFirst()
                    .orElse(null);
    
                if (contactToUpdate != null) {
                    // Update the contact details
                    contactToUpdate.setName(name);
                    contactToUpdate.setEmail(email);
                    contactToUpdate.setPhoneNumber(phoneNumber);
    
                    // Perform the update
                    boolean updateSuccessful = googleContactsService.updateContact(
                        client.getAccessToken().getTokenValue(), 
                        resourceName, 
                        contactToUpdate
                    );
    
                    if (!updateSuccessful) {
                        System.err.println("Failed to update contact: " + resourceName);
                    }
                } else {
                    System.err.println("Contact not found: " + resourceName);
                }
            } catch (Exception e) {
                System.err.println("Error fetching contacts: " + e.getMessage());
                e.printStackTrace();
            }
        }
    
        return "redirect:/contacts";
    }

    // Delete
    @PostMapping("/delete")
    public String deleteContact(@RequestParam String resourceName, OAuth2AuthenticationToken authentication) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
            authentication.getAuthorizedClientRegistrationId(),
            authentication.getName()
        );

        if (client != null) {
            boolean deleteSuccessful = googleContactsService.deleteContact(
                client.getAccessToken().getTokenValue(), 
                resourceName
            );

            if (!deleteSuccessful) {
                System.err.println("Failed to delete contact: " + resourceName);
            }
        }

        return "redirect:/contacts";
    }
}