package cz.muni.fi.pv168.project.zaostan.kalendar.managers;

import cz.muni.fi.pv168.project.zaostan.kalendar.entities.Event;
import cz.muni.fi.pv168.project.zaostan.kalendar.exceptions.db.ServiceFailureException;
import cz.muni.fi.pv168.project.zaostan.kalendar.exceptions.event.CalendarEventException;
import cz.muni.fi.pv168.project.zaostan.kalendar.tools.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Peter Zaoral
 * @version 2015-3-31
 */
public class EventManagerDB implements EventManager {

    final static Logger logger = LoggerFactory.getLogger(EventManagerDB.class);
    private DataSource source = null;

    public EventManagerDB(DataSource source) {
        this.source = source;
    }


    @Override
    public void addEvent(Event event) throws CalendarEventException {
        if (event == null) {
            throw new NullPointerException("User is null.");
        }

        if (event.getId() != 0) {
            throw new IllegalArgumentException("User id is already set..");
        }


        PreparedStatement st = null;
        try (Connection connection = source.getConnection()) {
            st = connection.prepareStatement(
                    FileUtils.readSqlFile(Event.class,"INSERT"),
                    Statement.RETURN_GENERATED_KEYS);

            st.setString(1, event.getName());
            st.setString(2, event.getDescription());
            st.setTimestamp(3, new Timestamp(event.getDateBegin().getTime()));
            st.setTimestamp(4, new Timestamp(event.getDateEnd().getTime()));
            st.setString(5, event.getAddress());

            int addedRows = st.executeUpdate();
            if (addedRows != 1) {
                throw new CalendarEventException("Internal Error: More rows "
                        + "inserted when trying to insert event " + event);
            }

            logger.info("Added new event: "+ event);
            ResultSet keyRS = st.getGeneratedKeys();
            try {
                event.setId(getKey(keyRS, event));
            } catch (ServiceFailureException ex) {
                logger.error("Detected problem with receiving event id.", ex);
                throw new CalendarEventException("Detected problem with receiving event id.", ex);
            }


        } catch (SQLException ex) {
            logger.error("Cannot create event with name: " + event.getName(), ex);
            throw new CalendarEventException("Cannot create event with name: " + event.getName(), ex);
        } catch (IOException ex) {
            logger.error("Can't read file.", ex);
            throw new CalendarEventException("Can't read file.", ex);
        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException ex) {
                    logger.error("Cannot close database.", ex);
                }
            }
        }

    }

    private static Long getKey(ResultSet keyRS, Event event) throws ServiceFailureException, SQLException {
        if (keyRS.next()) {
            if (keyRS.getMetaData().getColumnCount() != 1) {
                throw new ServiceFailureException("Internal Error: Generated key"
                        + " retriving failed when trying to insert event " + event
                        + " - wrong key fields count: " + keyRS.getMetaData().getColumnCount());
            }
            Long result = keyRS.getLong(1);
            if (keyRS.next()) {
                throw new ServiceFailureException("Internal Error: Generated key"
                        + " retriving failed when trying to insert event " + event
                        + " - more keys found");
            }
            return result;
        } else {
            throw new ServiceFailureException("Internal Error: Generated key"
                    + " retriving failed when trying to insert event " + event
                    + " - no key found");
        }
    }


    @Override
    public void removeEvent(long id)  throws CalendarEventException
    {
        {
            if(id <= 0)
                throw new IllegalArgumentException("Id is less than one.");
            PreparedStatement st = null;

            try (Connection connection = source.getConnection()) {
                st = connection.prepareStatement(
                        FileUtils.readSqlFile(Event.class,"DELETE"));
                st.setLong(1, id);

                logger.info("Deleted event with id: " + id);
                st.executeUpdate();
            } catch (SQLException ex) {
                logger.error("Error when deleting event with id = " + id, ex);
                throw new CalendarEventException(
                        "Error when deleting event with id = " + id, ex);
            } catch (IOException ex) {
                logger.error("Can't read file.", ex);
                throw new CalendarEventException("Can't read file.", ex);
            } finally {
                if (st != null) {
                    try {
                        st.close();
                    } catch (SQLException ex) {
                        logger.error("Cannot clone statement. ", ex);
                    }
                }
            }


        }

    }

    static Event resultSetToEvent(ResultSet rs, String prefix) throws SQLException {
        if(prefix == null)
        {
            prefix = "";
        }

        Event event = new Event();
        event.setId(rs.getLong(prefix+ "ID"));
        event.setName(rs.getString(prefix+ "EVENT_NAME"));
        event.setDescription(rs.getString(prefix+ "DESCRIPTION"));
        event.setDateBegin(rs.getTimestamp(prefix+ "DATE_BEGIN"));
        event.setDateEnd(rs.getTimestamp(prefix+ "DATE_END"));
        event.setAddress(rs.getString(prefix+ "ADDRESS"));
        return event;
    }

    static Event resultSetToEvent(ResultSet rs) throws SQLException {
        return resultSetToEvent(rs, "");

    }

    @Override
    public Event getEvent(long id) throws CalendarEventException {
        PreparedStatement st = null;
        try (Connection connection = source.getConnection()) {
            st = connection.prepareStatement(
                    FileUtils.readSqlFile(Event.class,"SELECT_BY_ID"));
            st.setLong(1, id);
            ResultSet rs = st.executeQuery();

            if (rs.next()) {
                Event event = resultSetToEvent(rs);

                if (rs.next()) {
                    throw new CalendarEventException(
                            "Internal error: More entities with the same id found "
                                    + "(source id: " + id + ", found " + " and " + resultSetToEvent(rs));
                }
                logger.info("Retrieved event: "+ event);
                return event;
            } else {
                return null;
            }

        } catch (SQLException ex) {
            logger.error("Error when retrieving event with id " + id, ex);
            throw new CalendarEventException(
                    "Error when retrieving event with id " + id, ex);
        } catch (IOException ex) {
            logger.error("Can't read file.", ex);
            throw new CalendarEventException("Can't read file.", ex);
        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException ex) {
                    logger.error("Cannot clone statement. ", ex);
                }
            }
        }
    }


    @Override
    public List<Event> getEvent(String name) throws CalendarEventException {
        if (name == null) {
            throw new NullPointerException("Name is null");
        }

        PreparedStatement st = null;
        try (Connection connection = source.getConnection()) {
            st = connection.prepareStatement(
                    FileUtils.readSqlFile(Event.class, "SELECT_BY_NAME"));
            st.setString(1, name);
            ResultSet rs = st.executeQuery();

            List<Event> result = new ArrayList<>();
            while (rs.next()) {
                result.add(resultSetToEvent(rs));
            }
            if (result.size() == 0) return null;
            logger.info("Retrieved event: "+ result);

            return result;

        } catch (SQLException ex) {
            logger.error( "Error when retrieving all events.", ex);
            throw new CalendarEventException(
                    "Error when retrieving all events.", ex);
        } catch (IOException ex) {
            logger.error("Can't read sql file",ex);
            throw new CalendarEventException("Can't read sql file",ex);
        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException ex) {
                    logger.error("Cannot clone statement. ", ex);
                }
            }
        }
    }


    @Override
    public List<Event> getAllEvents() throws CalendarEventException {

        PreparedStatement st = null;
        try (Connection connection = source.getConnection()) {
            st = connection.prepareStatement(
                    FileUtils.readSqlFile(Event.class,"SELECT_ALL"));
            ResultSet rs = st.executeQuery();

            List<Event> result = new ArrayList<>();
            while (rs.next()) {
                result.add(resultSetToEvent(rs));
            }
            if (result.size() == 0) return null;
            return result;

        } catch (SQLException ex) {
            logger.error("Error when retrieving all events", ex);
            throw new CalendarEventException(
                    "Error when retrieving all events", ex);
        } catch (IOException ex) {
            logger.error("Can't read sql file",ex);
            throw new CalendarEventException("Can't read sql file",ex);
        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException ex) {
                    logger.error("Cannot clone statement. ", ex);
                }
            }
        }
    }

    @Override
    public void updateEvent(Event event) throws CalendarEventException {
        if(event == null)
        {
            throw new NullPointerException("Event is null.");
        }

        long id = event.getId();
        if(id == 0)
        {
            throw new IllegalArgumentException("Event ID is 0, means that it is NOT in container, you have to use add() method");
        }
        PreparedStatement st = null;
        try (Connection connection = source.getConnection()) {

            st = connection.prepareStatement(
                    //UPDATE EVENTS SET EVENT_NAME=?, DESCRIPTION=?, DATE_BEGIN=?, DATE_END=?, ADDRESS=? WHERE ID=?
                    FileUtils.readSqlFile(Event.class, "UPDATE"));

            st.setString(1, event.getName());
            st.setString(2, event.getDescription());
            st.setTimestamp(3, new Timestamp(event.getDateBegin().getTime()));
            st.setTimestamp(4, new Timestamp(event.getDateEnd().getTime()));
            st.setString(5, event.getAddress());
            st.setLong(6,event.getId());

            int addedRows = st.executeUpdate();

            logger.info("Updated event: " + event);

            if (addedRows != 1) {
                throw new CalendarEventException("Internal Error: More rows "
                        + "inserted when trying to insert user " + event);
            }

        } catch (SQLException ex) {
            logger.error("Cannot create event named: " + event.getName(), ex);
            throw new CalendarEventException("Cannot create event named: " + event.getName(), ex);
        } catch (IOException ex) {
            logger.error("Can't read sql file", ex);
            throw new CalendarEventException("Can't read sql file", ex);
        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException ex) {
                    logger.error("Cannot close database.", ex);
                }
            }
        }


    }

    @Override
    public List<Event> findCurrentEvents() throws CalendarEventException {

        //Timestamp now = new Timestamp(new Date().getTime());
        PreparedStatement st = null;
        try (Connection connection = source.getConnection()) {
            st = connection.prepareStatement(
                    FileUtils.readSqlFile(Event.class,"SELECT_CURRENT"));
            ResultSet rs = st.executeQuery();
            List<Event> result = new ArrayList<>();
            while (rs.next()) {
                result.add(resultSetToEvent(rs));
            }
            if (result.size() == 0) return null;
            return result;

        } catch (SQLException ex) {
            logger.error("Error when retrieving all events", ex);
            throw new CalendarEventException(
                    "Error when retrieving all events", ex);
        } catch (IOException ex) {
            logger.error("Can't read sql file",ex);
            throw new CalendarEventException("Can't read sql file",ex);
        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException ex) {
                    logger.error("Cannot clone statement. ", ex);
                }
            }
        }
    }

    @Override
    public List<Event> findEventInTimePeriod(Date start, Date end) throws CalendarEventException {

        if(start == null || end == null){
            throw new NullPointerException("Start or end date of event is null");
        }
        //zhovievavost
        Event.dateFormat.setLenient(false);


        PreparedStatement st = null;
        try (Connection connection = source.getConnection()) {


            st = connection.prepareStatement(FileUtils.readSqlFile(Event.class, "SELECT_BETWEEN"));
            st.setTimestamp(1, new Timestamp(start.getTime()));
            st.setTimestamp(2, new Timestamp(end.getTime()));
            ResultSet rs = st.executeQuery();
            List<Event> result = new ArrayList<>();
            while (rs.next()) {
                result.add(resultSetToEvent(rs));
            }
            if (result.size() == 0) return null;
            return result;

        } catch (SQLException ex) {
            logger.error("Error when retrieving all events.", ex);
            throw new CalendarEventException(
                    "Error when retrieving all events.", ex);
        } catch (IOException ex) {
            logger.error("Can't read sql file",ex);
            throw new CalendarEventException("Can't read sql file",ex);
        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException ex) {
                    logger.error("Cannot clone statement. ", ex);
                }
            }
        }
    }



    @Override
    public long size() throws CalendarEventException {
        PreparedStatement st = null;
        try (Connection connection = source.getConnection()) {
            st = connection.prepareStatement(
                    FileUtils.readSqlFile(Event.class, "SIZE"));
            ResultSet rs = st.executeQuery();
            long result = 0;
            while (rs.next()) {
                result = rs.getLong("rows_count");
            }
            return result;
        } catch (SQLException ex) {
            logger.error("Error when retrieving all events", ex);
            throw new CalendarEventException("Error when retrieving all events", ex);
        } catch (IOException e) {
            logger.error("Can't read sql file",e);
            throw new CalendarEventException("Can't read sql file");
        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException ex) {
                    logger.error("Cannot clone statement. ", ex);
                }
            }
        }
    }

}
