package com.fraudit.fraudit.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.view.RedirectView

@Controller
@RequestMapping("/")
class HomeController {

    @GetMapping
    fun home(): RedirectView {
        return RedirectView("/swagger-ui.html")
    }
}