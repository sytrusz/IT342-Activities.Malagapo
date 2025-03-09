package com.malagapo.OAuth2Login.model;

public class Contact {
    private String name;
    private String email;
    private String resourceName;
    private String phoneNumber;
    private String etag; // Add etag field

    // Constructors
    public Contact() {}

    public Contact(String resourceName, String name, String email, String phoneNumber, String etag) {
        this.resourceName = resourceName;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.etag = etag; // Initialize etag
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }
}