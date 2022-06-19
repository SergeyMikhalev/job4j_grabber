package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class HabrCareerParse {

    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer", SOURCE_LINK);

    public static void main(String[] args) throws IOException {
        for (int i = 1; i < 6; i++) {
            printPage(i);
        }
        new HabrCareerParse().retrieveDescription("https://career.habr.com/vacancies/1000101200");
    }

    public static void printPage(int pageNumber) throws IOException {

        System.out.println(String.format("----- Страница %d -------", pageNumber));

        Connection connection = Jsoup.connect(PAGE_LINK + String.format("?page=%d", pageNumber));
        Document document = connection.get();
        Elements rows = document.select(".vacancy-card__inner");
        rows.forEach(row -> {
            Element titleElement = row.select(".vacancy-card__title").first();
            Element linkElement = titleElement.child(0);
            String vacancyName = titleElement.text();

            Element dateElement = row.select(".vacancy-card__date").first();
            Element dateLinkElement = dateElement.child(0);
            String dateTime = dateLinkElement.attr("datetime");

            String link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
            System.out.printf("%s %s %s %n", vacancyName, link, dateTime);
        });
    }

    private String retrieveDescription(String link) throws IOException {
        Connection connection = Jsoup.connect(link);
        Document document = connection.get();
        Element fullDescription = document.select(".style-ugc").first();

        return fullDescription.toString();
    }
}