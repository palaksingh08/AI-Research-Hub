# Research Paper Finder

AI-Powered Research Paper Search App using Spring Boot + Semantic Scholar API.

## Features
- Search research papers by keyword/topic
- Displays title, authors, venue, year, abstract preview, PDF links
- Responsive frontend
- Tomcat WAR deployment ready

## Tech Stack
- Spring Boot 3.3.4 (Java 17)
- Semantic Scholar API (free, no key)
- Maven WAR for Tomcat
- Vanilla HTML/CSS/JS frontend

## Local Run (Embedded Tomcat)
```bash
mvn clean spring-boot:run
```
Open: http://localhost:9090/research-paper-finder/

## Build & Deploy to Tomcat
```bash
mvn clean install
```
Copy `target/research-paper-finder-0.0.1-SNAPSHOT.war` to Tomcat webapps/.

Access: http://localhost:9090/research-paper-finder/

## API Endpoints
```
GET /research-paper-finder/api/papers/search?query={topic}&limit=10
```
Returns JSON list of Paper objects.

## Project Structure
```
- src/main/java/com/project/
  - ResearchPaperFinderApplication.java (Main)
  - controller/PaperSearchController.java
  - service/PaperSearchService.java
  - model/Paper.java
  - config/RestTemplateConfig.java
- src/main/resources/
  - application.properties
  - static/index.html
pom.xml
```

## Improvements Made
- Fixed naming/typos
- Consistent paths: `/research-paper-finder`
- Added validation, lombok deps
- DI for RestTemplate/ObjectMapper
- Responsive UI updates

**Test Search:** "machine learning transformers"
