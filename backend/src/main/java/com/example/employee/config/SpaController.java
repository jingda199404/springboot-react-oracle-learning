package com.example.employee.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({"/login", "/register", "/employees", "/permissions", "/batches", "/accounting", "/accounting/input", "/accounting/records", "/accounting/assets"})
    public String forwardRoutes() {
        return "forward:/index.html";
    }
}
