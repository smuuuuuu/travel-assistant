package com.itbaizhan.travel_ai_service.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class WebScrapingTool {
    @Tool(description = "Scrape the content of a web page")
    public String scrapeWebPage(@ToolParam(description = "URL of the web page to scrape") String url) {
        try {
            String targetUrl = url == null ? "" : url.trim();
            if (targetUrl.isEmpty()) {
                return "Error scraping web page: empty url";
            }
            var response = Jsoup.connect(targetUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .referrer("https://www.google.com/")
                    .timeout(15000)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .execute();
            Document document = response.parse();
            String html = document.html();
            if (response.statusCode() == 403 || response.statusCode() == 429
                    || html.contains("百度安全验证") || html.contains("网络不给力")) {
                return "Error scraping web page: blocked by anti-bot or rate limit, status=" + response.statusCode();
            }
            return html;
        } catch (Exception e) {
            return "Error scraping web page: " + e.getMessage();
        }
    }
}
