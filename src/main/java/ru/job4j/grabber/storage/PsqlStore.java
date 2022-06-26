package ru.job4j.grabber.storage;

import ru.job4j.grabber.entities.Post;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PsqlStore implements Store, AutoCloseable {

    private static final String SAVE_QUERY = "INSERT INTO posts(name, link, description, created) VALUES (?,?,?,?) ON CONFLICT (link) DO NOTHING";
    private static final String GET_ALL_QUERY = "SELECT * FROM posts";
    private static final String FIND_BY_ID = "SELECT * FROM posts WHERE id=?";

    private static final int ID_POS = 1;
    private static final int NAME_POS = 2;
    private static final int LINK_POS = 3;
    private static final int DESCRIPTION_POS = 4;
    private static final int CREATED_POS = 5;

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
            statement.setString(2, post.getLink());
            statement.setString(3, post.getDescription());
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
        while (resultSet.next()) {
            resultList.add(getPost(resultSet));
        }
    }

    private Post getPost(ResultSet resultSet) throws SQLException {
        return new Post(resultSet.getInt(ID_POS),
                resultSet.getString(NAME_POS),
                resultSet.getString(LINK_POS),
                resultSet.getString(DESCRIPTION_POS),
                resultSet.getTimestamp(CREATED_POS).toLocalDateTime()
        );
    }
}
