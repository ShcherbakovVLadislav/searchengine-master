package searchengine.dto;

public record SearchDto(String site, String siteName, String url, String title, String snippet, float relevance) {
}
