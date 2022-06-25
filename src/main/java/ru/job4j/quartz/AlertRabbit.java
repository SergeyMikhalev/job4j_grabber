package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class AlertRabbit {

    private static Properties rabbitProps;

    private static void initRabbitProps() throws IOException {
        rabbitProps = new Properties();
        try (InputStream in = AlertRabbit.class.getClassLoader().getResourceAsStream("app.properties")) {
            rabbitProps.load(in);
        }
    }

    public static Connection getDBConnection() throws Exception {

        Class.forName(rabbitProps.getProperty("driver-class-name"));
        return DriverManager.getConnection(
                rabbitProps.getProperty("url"),
                rabbitProps.getProperty("username"),
                rabbitProps.getProperty("password")
        );

    }

    public static void main(String[] args) {
        try {
            initRabbitProps();
            try (Connection cn = getDBConnection()) {
                List<Long> store = new ArrayList<>();
                Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
                scheduler.start();
                JobDataMap data = new JobDataMap();
                data.put("store", store);
                data.put("DBConnection", cn);
                JobDetail job = newJob(Rabbit.class)
                        .usingJobData(data)
                        .build();
                SimpleScheduleBuilder times = simpleSchedule()
                        .withIntervalInSeconds(Integer.valueOf(rabbitProps.getProperty("rabbit.interval")))
                        .repeatForever();
                Trigger trigger = newTrigger()
                        .startNow()
                        .withSchedule(times)
                        .build();
                scheduler.scheduleJob(job, trigger);
                Thread.sleep(10000);
                scheduler.shutdown();
                System.out.println(store);
            }
        } catch (Exception se) {
            se.printStackTrace();
        }
    }

    public static class Rabbit implements Job {

        public Rabbit() {
            System.out.println(hashCode());
        }

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            System.out.println("Rabbit runs here ...");
            List<Long> store = (List<Long>) context.getJobDetail().getJobDataMap().get("store");
            store.add(System.currentTimeMillis());

            Connection cn = (Connection) context.getJobDetail().getJobDataMap().get("DBConnection");
            try (PreparedStatement statement = cn.prepareStatement("INSERT INTO rabbit(created_date) VALUES (?)")) {
                statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                statement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }
    }
}