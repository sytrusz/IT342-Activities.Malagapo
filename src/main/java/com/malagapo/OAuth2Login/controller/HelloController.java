package com.malagapo.OAuth2Login.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

@Controller
public class HelloController {
    
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("message", "Welcome to the application!");
        return "index";
    }
 
    @GetMapping("/secured")
    public String secured() {
        return "This is a secured endpoint!";
    }
}

