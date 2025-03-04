package com.malagapo.OAuth2Login.service;

import com.malagapo.OAuth2Login.model.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
 
@Service
public class GoogleContactsService {
 
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
 
    // Fetch Contacts
    public List<Contact> getContacts(String accessToken) throws Exception {
        String url = "https://people.googleapis.com/v1/people/me/connections?personFields=names,emailAddresses,phoneNumbers";
    
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
    
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        String responseBody = response.getBody();
    
        // 🔹 Debugging API Response
        System.out.println("📌 API Response: " + responseBody); 
    
        return parseContacts(responseBody);
    }
    
    // 📝 Update Contact
    public boolean updateContact(String accessToken, String resourceName, Contact updatedContact) {
        try {
            if (resourceName == null || resourceName.trim().isEmpty()) {
                System.err.println("❌ Invalid resourceName: Cannot be null or empty");
                return false;
            }

            String url = "https://people.googleapis.com/v1/" + resourceName + "?updatePersonFields=names,emailAddresses,phoneNumbers";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create a robust JSON structure using ObjectMapper
            ObjectNode rootNode = objectMapper.createObjectNode();
            
            // Names
            ObjectNode nameNode = objectMapper.createObjectNode();
            nameNode.put("displayName", updatedContact.getName());
            rootNode.set("names", objectMapper.createArrayNode().add(nameNode));

            // Emails
            ObjectNode emailNode = objectMapper.createObjectNode();
            emailNode.put("value", updatedContact.getEmail());
            rootNode.set("emailAddresses", objectMapper.createArrayNode().add(emailNode));

            // Phone Numbers (optional)
            if (updatedContact.getPhoneNumber() != null && !updatedContact.getPhoneNumber().trim().isEmpty()) {
                ObjectNode phoneNode = objectMapper.createObjectNode();
                phoneNode.put("value", updatedContact.getPhoneNumber());
                rootNode.set("phoneNumbers", objectMapper.createArrayNode().add(phoneNode));
            }

            String requestBody = objectMapper.writeValueAsString(rootNode);
            
            System.out.println("📌 Update Request Body: " + requestBody);
            System.out.println("📌 Update URL: " + url);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PATCH, entity, String.class);

            System.out.println("📌 Update Response Status: " + response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.err.println("❌ Error updating contact: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // ❌ Delete Contact
    public boolean deleteContact(String accessToken, String resourceName) {
        String url = "https://people.googleapis.com/v1/" + resourceName;
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
        
        return response.getStatusCode().is2xxSuccessful();
    }
 
    // Parsing logic
    private List<Contact> parseContacts(String json) throws Exception {
        List<Contact> contacts = new ArrayList<>();
        JsonNode rootNode = objectMapper.readTree(json);
        JsonNode connections = rootNode.path("connections");

        if (!connections.isArray()) {
            System.out.println("⚠️ No connections found!");
            return contacts;
        }

        for (JsonNode node : connections) { 
            // Capture resourceName directly from the node
            String resourceName = node.path("resourceName").asText("");
            String name = "Unknown Name";
            String email = "None";
            String phoneNumber = " ";

            JsonNode namesNode = node.path("names");
            if (namesNode.isArray() && namesNode.size() > 0) {
                name = namesNode.get(0).path("displayName").asText("Unknown Name");
            }

            JsonNode emailsNode = node.path("emailAddresses");
            if (emailsNode.isArray() && emailsNode.size() > 0) {
                email = emailsNode.get(0).path("value").asText("None");
            }

            JsonNode phoneNumbersNode = node.path("phoneNumbers");
            if (phoneNumbersNode.isArray() && phoneNumbersNode.size() > 0) {
                phoneNumber = phoneNumbersNode.get(0).path("value").asText(" ");
            }

            contacts.add(new Contact(resourceName, name, email, phoneNumber));
        }

        return contacts;
    }
    
}