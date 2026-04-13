package com.project.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Paper model representing a research paper
 */
public class Paper {
    
    @JsonProperty("paperId")
    private String paperId;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("authors")
    private String[] authors;
    
    @JsonProperty("venue")
    private String venue;
    
    @JsonProperty("year")
    private int year;
    
    @JsonProperty("abstractText")
    private String abstractText;
    
    @JsonProperty("url")
    private String url;

    @JsonProperty("citationCount")
    private int citationCount;
    
    @JsonProperty("score")
    private float score;

    // Constructors
    public Paper() {
        this.score = 0.0f;
        this.year = 0;
        this.citationCount = 0;
    }

    public Paper(String paperId, String title, String[] authors, String venue, int year, 
                 String abstractText, String url) {
        this.paperId = paperId;
        this.title = title;
        this.authors = authors;
        this.venue = venue;
        this.year = year;
        this.abstractText = abstractText;
        this.url = url;
        this.citationCount = 0;
        this.score = 1.0f;
    }

    // Getters & Setters
    public String getPaperId() {
        return paperId;
    }

    public void setPaperId(String paperId) {
        this.paperId = paperId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String[] getAuthors() {
        return authors;
    }

    public void setAuthors(String[] authors) {
        this.authors = authors;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getCitationCount() {
        return citationCount;
    }

    public void setCitationCount(int citationCount) {
        this.citationCount = citationCount;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "Paper{" +
                "title='" + title + '\'' +
                ", authors=" + java.util.Arrays.toString(authors) +
                ", venue='" + venue + '\'' +
                ", year=" + year +
                ", citationCount=" + citationCount +
                ", url='" + url + '\'' +
                ", score=" + score +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Paper paper = (Paper) o;

        if (paperId != null ? !paperId.equals(paper.paperId) : paper.paperId != null)
            return false;
        return title != null ? title.equals(paper.title) : paper.title == null;
    }

    @Override
    public int hashCode() {
        int result = paperId != null ? paperId.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        return result;
    }
}
