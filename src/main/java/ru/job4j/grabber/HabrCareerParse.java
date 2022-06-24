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
    private static final int PAGES_TO_PARSE = 5;

    public static void main(String[] args) {
        System.out.println(new HabrCareerParse(new HabrCareerDateTimeParser()).list(PAGE_LINK));
    }

    private final DateTimeParser dateTimeParser;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    @Override
    public List<Post> list(String link) {
        List<Post> posts = new ArrayList<>(125);
        try {
            for (int i = 1; i <= PAGES_TO_PARSE; i++) {
                writePageToList(posts, link + String.format("?page=%d", i));
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
        return posts;
    }

    private void writePageToList(List<Post> list, String sourceLink) throws IOException {
        Connection connection = Jsoup.connect(sourceLink);
        Document document = connection.get();
        Elements rows = document.select(".vacancy-card__inner");
        rows.forEach(row -> {
            list.add(makePost(row));
        });
    }

    private Post makePost(Element row) {
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

        return new Post(vacancyName, link, description, dateTimeParser.parse(dateTime));
    }

    private String retrieveDescription(String link) throws IOException {
        Connection connection = Jsoup.connect(link);
        Document document = connection.get();
        Element fullDescription = document.select(".style-ugc").first();

        return fullDescription.toString();
    }


}