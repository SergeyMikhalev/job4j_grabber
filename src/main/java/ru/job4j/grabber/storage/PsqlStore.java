package ru.job4j.grabber.storage;

import ru.job4j.grabber.entities.Post;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

public class PsqlStore implements Store, AutoCloseable {

    private static final String SAVE_QUERY = "INSERT INTO posts(name, description, link, created) VALUES (?,?,?,?);";
    private static final String GET_ALL_QUERY = "SELECT * FROM posts;";
    private static final String FIND_BY_ID = "SELECT * FROM posts WHERE id=? ;";

    private Connection cnn;

    public PsqlStore(Properties cfg) {
        try {
            Class.forName(cfg.getProperty("driver-class-name"));
            cnn = DriverManager.getConnection(
                    cfg.getProperty("url"),
                    cfg.getProperty("username"),
                    cfg.getProperty("password")
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void save(Post post) {
        try (PreparedStatement statement = cnn.prepareStatement(SAVE_QUERY)) {
            statement.setString(1, post.getTitle());
            statement.setString(2, post.getDescription());
            statement.setString(3, post.getLink());
            statement.setTimestamp(4, Timestamp.valueOf(post.getCreated()));

            statement.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Post> getAll() {
        List<Post> resultList = new ArrayList<>(125);
        try (PreparedStatement statement = cnn.prepareStatement(GET_ALL_QUERY);
             ResultSet resultSet = statement.executeQuery()) {
            listFromResultSet(resultSet, resultList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultList;
    }

    @Override
    public Post findById(int id) {
        Post resPost = null;
        try (PreparedStatement statement = cnn.prepareStatement(FIND_BY_ID)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    resPost = getPost(resultSet);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return resPost;
    }

    @Override
    public void close() throws Exception {
        if (cnn != null) {
            cnn.close();
        }
    }

    private void listFromResultSet(ResultSet resultSet, List<Post> resultList) throws SQLException {
        if (Objects.isNull(resultSet) || Objects.isNull(resultList)) {
            throw new IllegalArgumentException();
        }
        while (resultSet.next()) {
            resultList.add(getPost(resultSet));
        }
    }

    private Post getPost(ResultSet resultSet) throws SQLException {
        return new Post(resultSet.getInt(1),
                resultSet.getString(2),
                resultSet.getString(3),
                resultSet.getString(4),
                resultSet.getTimestamp(5).toLocalDateTime()
        );
    }

    public static void main(String... args) {
        try (InputStream in = PsqlStore.class.getClassLoader().getResourceAsStream("rabbit.properties")) {
            Properties config = new Properties();
            config.load(in);
            Store store = new PsqlStore(config);
            store.save(new Post("title", "description", "link " + LocalDateTime.now(), LocalDateTime.now()));
            System.out.println(store.getAll());
            System.out.println(store.findById(1));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
