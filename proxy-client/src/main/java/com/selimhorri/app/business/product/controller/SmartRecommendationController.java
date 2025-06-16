package com.selimhorri.app.business.product.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(value = {"/api/beta/smart-recommendations"})
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "features.smart-recommendations", havingValue = "true")
public class SmartRecommendationController {
	
	@GetMapping
	public ResponseEntity<String> smartRecommendations() {
		log.info("**** Smart Recommendations Feature ****");
		return ResponseEntity.ok("AI-powered smart product recommendations based on user preferences and behavior.");
	}
	
}
