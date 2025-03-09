package com.malagapo.OAuth2Login.service;

import com.malagapo.OAuth2Login.model.*;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
 
@Service
public class GoogleContactsService {
 
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
 
    public GoogleContactsService() {
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
    }
 
    // Close the HTTP client when the service is destroyed
    public void close() throws IOException {
        httpClient.close();
    }
 
    // Fetch Contacts
    public List<Contact> getContacts(String accessToken) throws Exception {
        String url = "https://people.googleapis.com/v1/people/me/connections?personFields=names,emailAddresses,phoneNumbers";
    
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + accessToken);
    
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);
    
            // 🔹 Debugging API Response
            System.out.println("API Response: " + responseBody); 
    
            return parseContacts(responseBody);
        }
    }
    
// Add this method to your GoogleContactsService class

    // Create New Contact
    public String createContact(String accessToken, Contact newContact) {
        try {
            String url = "https://people.googleapis.com/v1/people:createContact";
    
            HttpPost request = new HttpPost(url);
            request.setHeader("Authorization", "Bearer " + accessToken);
            request.setHeader("Content-Type", "application/json");
    
            // Build the request body
            ObjectNode rootNode = objectMapper.createObjectNode();
            
            // Names - Properly structured with given and family name fields
            ObjectNode nameNode = objectMapper.createObjectNode();
            nameNode.put("displayName", newContact.getName());
            
            // Split the name into parts and add them
            String[] nameParts = newContact.getName().split(" ", 2);
            nameNode.put("givenName", nameParts[0]);
            if (nameParts.length > 1) {
                nameNode.put("familyName", nameParts[1]);
            }
            
            // Set the correct metadata
            ObjectNode metadataNode = objectMapper.createObjectNode();
            metadataNode.put("primary", true);
            nameNode.set("metadata", metadataNode);
            
            rootNode.set("names", objectMapper.createArrayNode().add(nameNode));
    
            // Emails
            ObjectNode emailNode = objectMapper.createObjectNode();
            emailNode.put("value", newContact.getEmail());
            ObjectNode emailMetadata = objectMapper.createObjectNode();
            emailMetadata.put("primary", true);
            emailNode.set("metadata", emailMetadata);
            rootNode.set("emailAddresses", objectMapper.createArrayNode().add(emailNode));
    
            // Phone Numbers (optional)
            if (newContact.getPhoneNumber() != null && !newContact.getPhoneNumber().trim().isEmpty()) {
                ObjectNode phoneNode = objectMapper.createObjectNode();
                phoneNode.put("value", newContact.getPhoneNumber());
                ObjectNode phoneMetadata = objectMapper.createObjectNode();
                phoneMetadata.put("primary", true);
                phoneNode.set("metadata", phoneMetadata);
                rootNode.set("phoneNumbers", objectMapper.createArrayNode().add(phoneNode));
            }
    
            // Convert the request body to JSON
            String requestBody = objectMapper.writeValueAsString(rootNode);
            
            // Log the request for debugging
            System.out.println("Create Request Body: " + requestBody);
            System.out.println("Create URL: " + url);
    
            // Set the request body
            request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
            
            // Execute the request
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                System.out.println("Create Response Status: " + statusCode);
                System.out.println("Create Response Body: " + responseBody);
    
                // Check if the creation was successful
                if (statusCode >= 200 && statusCode < 300) {
                    // Parse the response to get the resourceName
                    JsonNode responseNode = objectMapper.readTree(responseBody);
                    return responseNode.path("resourceName").asText("");
                } else {
                    System.err.println("Failed to create contact. Response: " + responseBody);
                    return null;
                }
            }
        } catch (Exception e) {
            System.err.println("Error creating contact: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Update Contact
    public boolean updateContact(String accessToken, String resourceName, Contact updatedContact) {
        try {
            // Validate resourceName
            if (resourceName == null || resourceName.trim().isEmpty()) {
                System.err.println("Invalid resourceName: Cannot be null or empty");
                return false;
            }
    
            // Ensure resourceName starts with "people/"
            if (!resourceName.startsWith("people/")) {
                resourceName = "people/" + resourceName;
            }
    
            // Construct the URL with :updateContact suffix
            String url = "https://people.googleapis.com/v1/" + resourceName + ":updateContact?updatePersonFields=names,emailAddresses,phoneNumbers";
    
            // Create the PATCH request
            HttpPatch request = new HttpPatch(url);
            request.setHeader("Authorization", "Bearer " + accessToken);
            request.setHeader("Content-Type", "application/json");
    
            // Build the request body
            ObjectNode rootNode = objectMapper.createObjectNode();
            
            // Add etag to the request body
            rootNode.put("etag", updatedContact.getEtag());
    
            // Names - Properly structured with given and family name fields
            ObjectNode nameNode = objectMapper.createObjectNode();
            nameNode.put("displayName", updatedContact.getName());
            
            // Split the name into parts and add them
            String[] nameParts = updatedContact.getName().split(" ", 2);
            nameNode.put("givenName", nameParts[0]);
            if (nameParts.length > 1) {
                nameNode.put("familyName", nameParts[1]);
            }
            
            // Set the correct metadata
            ObjectNode metadataNode = objectMapper.createObjectNode();
            metadataNode.put("primary", true);
            nameNode.set("metadata", metadataNode);
            
            rootNode.set("names", objectMapper.createArrayNode().add(nameNode));
    
            // Emails
            ObjectNode emailNode = objectMapper.createObjectNode();
            emailNode.put("value", updatedContact.getEmail());
            ObjectNode emailMetadata = objectMapper.createObjectNode();
            emailMetadata.put("primary", true);
            emailNode.set("metadata", emailMetadata);
            rootNode.set("emailAddresses", objectMapper.createArrayNode().add(emailNode));
    
            // Phone Numbers (optional)
            if (updatedContact.getPhoneNumber() != null && !updatedContact.getPhoneNumber().trim().isEmpty()) {
                ObjectNode phoneNode = objectMapper.createObjectNode();
                phoneNode.put("value", updatedContact.getPhoneNumber());
                ObjectNode phoneMetadata = objectMapper.createObjectNode();
                phoneMetadata.put("primary", true);
                phoneNode.set("metadata", phoneMetadata);
                rootNode.set("phoneNumbers", objectMapper.createArrayNode().add(phoneNode));
            }
    
            // Convert the request body to JSON
            String requestBody = objectMapper.writeValueAsString(rootNode);
            
            // Log the request for debugging
            System.out.println("Update Request Body: " + requestBody);
            System.out.println("Update URL: " + url);
    
            // Set the request body
            request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
            
            // Execute the request
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                System.out.println("Update Response Status: " + statusCode);
                System.out.println("Update Response Body: " + responseBody);
    
                // Check if the update was successful
                if (statusCode >= 200 && statusCode < 300) {
                    return true;
                } else {
                    System.err.println("Failed to update contact. Response: " + responseBody);
                    return false;
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating contact: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
        
    // Delete Contact
    public boolean deleteContact(String accessToken, String resourceName) {
        try {
            // Validate resourceName
            if (resourceName == null || resourceName.trim().isEmpty()) {
                System.err.println("Invalid resourceName: Cannot be null or empty");
                return false;
            }

            // Ensure resourceName has the correct format
            if (!resourceName.startsWith("people/")) {
                resourceName = "people/" + resourceName;
            }

            // Log the resourceName for debugging
            System.out.println("Deleting contact with resourceName: " + resourceName);

            // Construct the URL according to the official documentation
            String url = "https://people.googleapis.com/v1/" + resourceName + ":deleteContact";

            // Create the DELETE request
            HttpDelete request = new HttpDelete(url);
            request.setHeader("Authorization", "Bearer " + accessToken);
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                System.out.println("Delete Response Status: " + statusCode);
                System.out.println("Delete Response Body: " + responseBody);

                if (statusCode >= 200 && statusCode < 300) {
                    return true;
                } else {
                    System.err.println("Failed to delete contact. Response: " + responseBody);
                    return false;
                }
            }
        } catch (Exception e) {
            System.err.println("Error deleting contact: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Parsing logic
    private List<Contact> parseContacts(String json) throws Exception {
        List<Contact> contacts = new ArrayList<>();
        JsonNode rootNode = objectMapper.readTree(json);
        JsonNode connections = rootNode.path("connections");
    
        if (!connections.isArray()) {
            System.out.println("No connections found!");
            return contacts;
        }
    
        for (JsonNode node : connections) {
            String resourceName = node.path("resourceName").asText("");
            String name = "Unknown Name";
            String email = "None";
            String phoneNumber = " ";
            String etag = node.path("etag").asText(""); // Extract etag
    
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
    
            // Include etag in the Contact object
            contacts.add(new Contact(resourceName, name, email, phoneNumber, etag));
        }
    
        return contacts;
    }
}