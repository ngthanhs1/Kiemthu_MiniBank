package com.minibank.backend.mobile.services.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minibank.backend.mobile.services.service.MobileServicesService;

@RestController
@RequestMapping("/api/mobile/services")
public class MobileServicesController {
    private final MobileServicesService mobileServicesService;

    public MobileServicesController(MobileServicesService mobileServicesService) {
        this.mobileServicesService = mobileServicesService;
    }

    @GetMapping("/ping")
    public String ping() {
        return mobileServicesService.ping();
    }
}
