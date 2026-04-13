package com.project.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.model.AssistantRequest;
import com.project.model.AssistantResponse;
import com.project.model.Paper;
import com.project.service.PaperSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/papers")
@CrossOrigin(origins = "*")
public class PaperSearchController {

    private static final Logger log = LoggerFactory.getLogger(PaperSearchController.class);

    @Autowired
    private PaperSearchService paperSearchService;

    @GetMapping("/search")
    public Map<String, Object> searchPapers(
            @RequestParam String query, 
            @RequestParam(defaultValue = "20") int limit) {
        
        log.info("🔍 Search request: query='{}', limit={}", query, limit);
        
        if (query == null || query.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("papers", List.of());
            errorResponse.put("count", 0);
            errorResponse.put("query", query);
            errorResponse.put("error", "Query cannot be empty");
            return errorResponse;
        }
        
        List<Paper> papers = paperSearchService.searchPapers(query, limit);
        log.info("✅ Returning {} papers for '{}'", papers.size(), query);
        
        Map<String, Object> response = new HashMap<>();
        response.put("papers", papers);
        response.put("count", papers.size());
        response.put("query", query);
        response.put("status", papers.isEmpty() ? "no_results" : "success");
        
        return response;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "API is running ✅");
        response.put("api", "Semantic Scholar");
        return response;
    }
    @PostMapping("/assistant")
    public AssistantResponse askAssistant(@RequestBody AssistantRequest request) {
        log.info("Assistant request: message='{}', currentQuery='{}', results={}",
                request.getMessage(), request.getCurrentQuery(), request.getCurrentResults().size());
        return paperSearchService.buildAssistantReply(request);
    }
}
