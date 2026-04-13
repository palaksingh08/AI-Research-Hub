package com.project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.project.model.AssistantRequest;
import com.project.model.AssistantResponse;
import com.project.model.Paper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class PaperSearchService {

    private static final Logger log = LoggerFactory.getLogger(PaperSearchService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String ARXIV_API = "https://export.arxiv.org/api/query?";
    private static final String CROSSREF_API = "https://api.crossref.org/works?";

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = TimeUnit.SECONDS.toMillis(300);

    static class CacheEntry {
        final List<Paper> papers;
        final long timestamp;

        CacheEntry(List<Paper> papers) {
            this.papers = new ArrayList<>(papers);
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp >= CACHE_TTL;
        }
    }

    @Autowired
    public PaperSearchService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        log.info("✅ PaperSearchService initialized");
    }

    // ================= MAIN SEARCH =================
    public List<Paper> searchPapers(String query, int limit) {
        query = query.trim();
        log.info("🔍 Search: {}", query);

        CacheEntry cached = cache.get(query);
        if (cached != null && !cached.isExpired()) {
            return cached.papers;
        }

        List<Paper> papers = new ArrayList<>();

        papers.addAll(searchArxiv(query, limit));
        papers.addAll(searchCrossRef(query, limit));

        // Remove duplicates
        Map<String, Paper> unique = new LinkedHashMap<>();
        for (Paper p : papers) {
            unique.putIfAbsent(p.getPaperId(), p);
        }

        List<Paper> result = new ArrayList<>(unique.values())
                .stream()
                .limit(limit)
                .collect(Collectors.toList());

        cache.put(query, new CacheEntry(result));

        log.info("✅ Returning {} papers", result.size());
        return result;
    }

    public AssistantResponse buildAssistantReply(AssistantRequest request) {
        String message = safeText(request.getMessage());
        String currentQuery = safeText(request.getCurrentQuery());
        List<Paper> papers = request.getCurrentResults() != null ? request.getCurrentResults() : Collections.emptyList();
        String normalized = message.toLowerCase(Locale.ROOT);

        if (normalized.isBlank()) {
            return new AssistantResponse(
                    "Ask me to summarize results, suggest a better query, show recent papers, or recommend where to start.",
                    currentQuery,
                    defaultFollowUps());
        }

        if (containsAny(normalized, "hello", "hi", "hey", "help")) {
            return new AssistantResponse(
                    "I can refine your topic, summarize current results, recommend strong papers, and help you search faster.",
                    currentQuery,
                    defaultFollowUps());
        }

        if (containsAny(normalized, "recent", "latest", "new", "newest")) {
            List<Paper> recent = papers.stream()
                    .filter(p -> p.getYear() > 0)
                    .sorted(Comparator.comparingInt(Paper::getYear).reversed())
                    .limit(3)
                    .collect(Collectors.toList());

            if (recent.isEmpty()) {
                String suggested = currentQuery.isBlank() ? buildSuggestedQuery(message, currentQuery, papers) : currentQuery + " recent review";
                return new AssistantResponse(
                        "I do not have dated results yet. Try this search: " + suggested,
                        suggested,
                        Arrays.asList("summarize these results", "suggest a broader query", "show top 3 papers"));
            }

            return new AssistantResponse(
                    "Most recent papers in the current results:\n" + formatPaperBullets(recent)
                            + "\nTip: keep the year filter high if you want fresher research.",
                    currentQuery,
                    Arrays.asList("summarize these results", "show top 3 papers", "suggest a better query"));
        }

        if (containsAny(normalized, "summarize", "summary", "explain", "what did you find")) {
            return new AssistantResponse(
                    buildSummaryReply(currentQuery, papers),
                    currentQuery,
                    Arrays.asList("show top 3 papers", "show recent papers", "suggest a better query"));
        }

        if (containsAny(normalized, "recommend", "top", "best", "start with")) {
            return new AssistantResponse(
                    buildRecommendationReply(currentQuery, papers),
                    currentQuery,
                    Arrays.asList("summarize these results", "show recent papers", "suggest a better query"));
        }

        if (containsAny(normalized, "query", "search", "keyword", "keywords", "refine", "improve")) {
            String suggestedQuery = buildSuggestedQuery(message, currentQuery, papers);
            return new AssistantResponse(
                    "Try this refined search: " + suggestedQuery,
                    suggestedQuery,
                    Arrays.asList("search this topic", "show recent papers", "recommend top 3 papers"));
        }

        String suggestedQuery = buildSuggestedQuery(message, currentQuery, papers);
        return new AssistantResponse(
                "A strong next search would be: " + suggestedQuery + ". You can also ask me to summarize or rank the current results.",
                suggestedQuery,
                defaultFollowUps());
    }

    // ================= ARXIV =================
    private List<Paper> searchArxiv(String query, int limit) {
        try {
            String url = ARXIV_API + "search_query=all:" +
                    java.net.URLEncoder.encode(query, "UTF-8") +
                    "&start=0&max_results=" + limit;

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET,
                    new HttpEntity<>(createHeaders("xml")),
                    String.class
            );

            if (response.getStatusCodeValue() == 429) {
                Thread.sleep(2000);
                response = restTemplate.exchange(url, HttpMethod.GET,
                        new HttpEntity<>(createHeaders("xml")), String.class);
            }

            if (response.getStatusCode().is2xxSuccessful()) {
                return parseArxivResponse(response.getBody());
            }

        } catch (Exception e) {
            log.warn("arXiv failed: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    // ================= CROSSREF =================
    private List<Paper> searchCrossRef(String query, int limit) {
        try {
            String url = CROSSREF_API + "query=" +
                    java.net.URLEncoder.encode(query, "UTF-8") +
                    "&rows=" + limit;

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders("json")),
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return parseCrossRefResponse(response.getBody());
            }

        } catch (Exception e) {
            log.warn("CrossRef failed: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    // ================= ARXIV PARSER (FIXED) =================
    private List<Paper> parseArxivResponse(String xml) {
        List<Paper> papers = new ArrayList<>();
        if (xml == null) return papers;

        String[] entries = xml.split("<entry>");

        for (int i = 1; i < entries.length; i++) { // 🔥 skip first
            String entry = entries[i];

            try {
                String title = extractXmlValue(entry, "<title>", "</title>");
                if (title == null || title.isEmpty()) continue;

                title = title.replaceAll("\\s+", " ").trim();

                Paper p = new Paper();
                p.setTitle(title);

                String id = extractXmlValue(entry, "<id>", "</id>");
                if (id != null) {
                    p.setPaperId(id.replace("http://arxiv.org/abs/", ""));
                    p.setUrl(id);
                }

                p.setAuthors(extractAuthors(entry));

                String summary = extractXmlValue(entry, "<summary>", "</summary>");
                p.setAbstractText(summary != null ? summary : "");

                String published = extractXmlValue(entry, "<published>", "</published>");
                if (published != null && published.length() >= 4) {
                    p.setYear(Integer.parseInt(published.substring(0, 4)));
                }

                p.setVenue("arXiv");

                papers.add(p);

            } catch (Exception ignored) {}
        }

        return papers;
    }

    // ================= CROSSREF PARSER =================
    private List<Paper> parseCrossRefResponse(String json) {
        List<Paper> papers = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode items = root.path("message").path("items");

            for (JsonNode item : items) {
                String title = item.path("title").get(0).asText("");
                if (title.isEmpty()) continue;

                Paper p = new Paper();
                p.setTitle(title);

                String doi = item.path("DOI").asText();
                p.setPaperId(doi);
                p.setUrl(item.path("URL").asText());

                List<String> authors = new ArrayList<>();
                for (JsonNode a : item.path("author")) {
                    authors.add(a.path("given").asText("") + " " + a.path("family").asText(""));
                }
                p.setAuthors(authors.toArray(new String[0]));

                String abstractText = item.path("abstract").asText("");
                p.setAbstractText(cleanCrossRefAbstract(abstractText));
                p.setCitationCount(item.path("is-referenced-by-count").asInt(0));

                JsonNode date = item.path("issued").path("date-parts").get(0);
                if (date != null) {
                    p.setYear(date.get(0).asInt());
                }

                p.setVenue("CrossRef");

                papers.add(p);
            }

        } catch (Exception e) {
            log.warn("CrossRef parse error: {}", e.getMessage());
        }
        return papers;
    }

    // ================= HELPERS =================
    private String[] extractAuthors(String xml) {
        List<String> authors = new ArrayList<>();
        String[] parts = xml.split("<author>");

        for (String p : parts) {
            String name = extractXmlValue(p, "<name>", "</name>");
            if (name != null) authors.add(name);
        }

        if (authors.isEmpty()) authors.add("Anonymous");

        return authors.toArray(new String[0]);
    }

    private String extractXmlValue(String xml, String start, String end) {
        int s = xml.indexOf(start);
        if (s == -1) return null;

        s += start.length();
        int e = xml.indexOf(end, s);
        if (e == -1) return null;

        return xml.substring(s, e).trim();
    }

    private String buildSummaryReply(String currentQuery, List<Paper> papers) {
        if (papers.isEmpty()) {
            String fallback = currentQuery.isBlank() ? "your topic" : currentQuery;
            return "No current results are loaded yet. Search for \"" + fallback + "\" first, then I can summarize the findings.";
        }

        IntSummaryStatistics yearStats = papers.stream()
                .filter(p -> p.getYear() > 0)
                .mapToInt(Paper::getYear)
                .summaryStatistics();

        Set<String> venues = papers.stream()
                .map(Paper::getVenue)
                .filter(Objects::nonNull)
                .filter(v -> !v.isBlank())
                .limit(4)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> keywords = extractKeywords(papers);
        String timeWindow = yearStats.getCount() > 0
                ? yearStats.getMin() + " to " + yearStats.getMax()
                : "mixed publication years";

        return "Quick summary for \"" + (currentQuery.isBlank() ? "your current search" : currentQuery) + "\":\n"
                + "- Results found: " + papers.size() + "\n"
                + "- Publication window: " + timeWindow + "\n"
                + "- Main sources: " + (venues.isEmpty() ? "mixed sources" : String.join(", ", venues)) + "\n"
                + "- Common themes: " + (keywords.isEmpty() ? "topic-specific research patterns" : String.join(", ", keywords)) + "\n"
                + "- Best next step: ask for top papers or refine by method, dataset, or year.";
    }

    private String buildRecommendationReply(String currentQuery, List<Paper> papers) {
        if (papers.isEmpty()) {
            return "I need search results before I can rank papers. Try searching for: "
                    + buildSuggestedQuery(currentQuery, currentQuery, papers);
        }

        List<Paper> picks = papers.stream()
                .sorted(Comparator.comparingInt(Paper::getYear).reversed()
                        .thenComparingInt(p -> safeText(p.getAbstractText()).length()).reversed())
                .limit(3)
                .collect(Collectors.toList());

        return "Start with these papers:\n" + formatPaperBullets(picks)
                + "\nWhy these: they are among the strongest results by recency and metadata completeness.";
    }

    private String buildSuggestedQuery(String message, String currentQuery, List<Paper> papers) {
        String base = !currentQuery.isBlank() ? currentQuery : message;
        if (base == null || base.isBlank()) {
            base = papers.stream()
                    .map(Paper::getTitle)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse("machine learning");
        }

        String normalized = base.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.isBlank()) {
            normalized = "machine learning";
        }

        if (normalized.toLowerCase(Locale.ROOT).contains("review")
                || normalized.toLowerCase(Locale.ROOT).contains("survey")) {
            return normalized;
        }
        return normalized + " review OR survey";
    }

    private List<String> extractKeywords(List<Paper> papers) {
        Map<String, Long> counts = new HashMap<>();
        Set<String> stopWords = Set.of(
                "the", "and", "for", "with", "from", "that", "this", "using", "into", "based",
                "study", "analysis", "research", "paper", "method", "methods", "approach", "approaches");

        for (Paper paper : papers.stream().limit(8).collect(Collectors.toList())) {
            String text = (safeText(paper.getTitle()) + " " + safeText(paper.getAbstractText())).toLowerCase(Locale.ROOT);
            for (String token : text.split("[^a-z0-9]+")) {
                if (token.length() < 4 || stopWords.contains(token)) {
                    continue;
                }
                counts.merge(token, 1L, Long::sum);
            }
        }

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private String formatPaperBullets(List<Paper> papers) {
        return papers.stream()
                .map(p -> "- " + safeText(p.getTitle())
                        + " (" + (p.getYear() > 0 ? p.getYear() : "Year N/A") + ", "
                        + (safeText(p.getVenue()).isBlank() ? "Unknown source" : safeText(p.getVenue())) + ")")
                .collect(Collectors.joining("\n"));
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private List<String> defaultFollowUps() {
        return Arrays.asList("summarize these results", "show top 3 papers", "suggest a better query");
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanCrossRefAbstract(String abstractText) {
        if (abstractText == null || abstractText.isBlank()) {
            return "";
        }
        return abstractText
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&lt;[^&]*&gt;", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private HttpHeaders createHeaders(String type) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "ResearchPaperFinder/1.0 (mailto:test@example.com)");

        if ("json".equals(type)) {
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        } else {
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_XML));
        }

        return headers;
    }
}
