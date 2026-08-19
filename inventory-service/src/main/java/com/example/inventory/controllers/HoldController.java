package com.example.inventory.controllers;

import com.example.inventory.dto.CreateHoldRequest;
import com.example.inventory.dto.CreateHoldResponse;
import com.example.inventory.services.HoldService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/holds")
public class HoldController {
    private final HoldService holdService;

    public HoldController(HoldService holdService) {
        this.holdService = holdService;
    }

    @PostMapping
    public ResponseEntity<CreateHoldResponse> createHold(@RequestBody @Valid CreateHoldRequest request){
        CreateHoldResponse response = holdService.createHold(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
