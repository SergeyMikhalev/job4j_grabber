package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.entities.Post;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HabrCareerParse implements Parse {

    private static final String SOURCE_LINK = "https://career.habr.com";
    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer", SOURCE_LINK);
    private static final int PAGES_TO_PARSE = 1;

    private final DateTimeParser dateTimeParser;

    public static void main(String[] args) {
        System.out.println(new HabrCareerParse(new HabrCareerDateTimeParser()).list("_"));
    }

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }


    private String retrieveDescription(String link) throws IOException {
        Connection connection = Jsoup.connect(link);
        Document document = connection.get();
        Element fullDescription = document.select(".style-ugc").first();

        return fullDescription.toString();
    }

    private void writePageToList(List<Post> list, String sourceLink) throws IOException {
        Connection connection = Jsoup.connect(sourceLink);
        Document document = connection.get();
        Elements rows = document.select(".vacancy-card__inner");
        rows.forEach(row -> {
            Element titleElement = row.select(".vacancy-card__title").first();
            Element linkElement = titleElement.child(0);
            String vacancyName = titleElement.text();

            Element dateElement = row.select(".vacancy-card__date").first();
            Element dateInnerElement = dateElement.child(0);
            String dateTime = dateInnerElement.attr("datetime");

            String link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
            String description = "";
            try {
                description = retrieveDescription(link);
            } catch (IOException e) {
                e.printStackTrace();
            }

            list.add(new Post(vacancyName, link, description, dateTimeParser.parse(dateTime)));

        });
    }

    @Override
    public List<Post> list(String link) {
        List<Post> posts = new ArrayList<>(100);
        try {
            for (int i = 1; i <= PAGES_TO_PARSE; i++) {
                writePageToList(posts, PAGE_LINK + String.format("?page=%d", i));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return posts;
    }
}