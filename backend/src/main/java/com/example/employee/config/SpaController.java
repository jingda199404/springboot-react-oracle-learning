package com.example.employee.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({
            "/login",
            "/register",
            "/admin",
            "/users",
            "/permissions",
            "/batches",
            "/accounting",
            "/accounting/input",
            "/accounting/records",
            "/accounting/assets",
            "/japanese",
            "/japanese/words",
            "/japanese/memory",
            "/japanese/quiz",
            "/japanese/articles",
            "/stocks"
    })
    public String forwardRoutes() {
        return "forward:/index.html";
    }
}
