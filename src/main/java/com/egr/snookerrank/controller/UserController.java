package com.egr.snookerrank.controller;

import com.egr.snookerrank.model.WaitlistEntry;
import com.egr.snookerrank.repositroy.WaitlistRepository;
import com.egr.snookerrank.dto.WaitlistRequestDTO;
import com.egr.snookerrank.dto.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import jakarta.validation.Valid;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/user")
@Tag(name = "User APIs", description = "Endpoints for managing user data")
public class UserController {
    private final WaitlistRepository waitlistRepository;

    public UserController(WaitlistRepository waitlistRepository) {
        this.waitlistRepository = waitlistRepository;
    }

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);



    @Operation(
            summary = "Update waitlist",
            description = "Make an entry in the waitlist",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User added to waitlist"),
            }
    )

    @PostMapping("/addToWaitlist")
    public RestApiResponse<String> addToWaitlist(@Valid @RequestBody WaitlistRequestDTO request) {
        boolean alreadyExists = waitlistRepository.findByEmail(request.getEmail()).isPresent();

        if (alreadyExists) {
            logger.info("already exists");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists.");
        }

        WaitlistEntry entry = new WaitlistEntry(request.getEmail());
        waitlistRepository.save(entry);
                    
        String email = request.getEmail();
        return new RestApiResponse<>("SUCCESS", "Waitlist updated successfully", email);
    }


    @Operation(
            summary = "waitlist",
            description = "Get waitlist users",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Waitlist Users"),
            }
    )

    @GetMapping("/waitlist")
    public List<WaitlistEntry> getAllWaitlistEntries() {

        return waitlistRepository.findAll();
    }

}