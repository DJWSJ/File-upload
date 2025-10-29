// src/main/java/com/djwsj/filemanager/controller/HomeController.java
package com.djwsj.filemanager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/api/file-manager";
    }
}