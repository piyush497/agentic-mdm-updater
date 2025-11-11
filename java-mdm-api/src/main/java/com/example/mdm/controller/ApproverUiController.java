package com.example.mdm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.context.annotation.Profile;

@Controller
@Profile("local")
public class ApproverUiController {

    @GetMapping("/ui/cr/{id}")
    public String crPage(@PathVariable String id, Model model) {
        model.addAttribute("crId", id);
        return "cr"; // resolves to templates/cr.html
    }
}
