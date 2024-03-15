package ua.kiev.splash.mathhelper.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.kiev.splash.mathhelper.api.EquationRepository;
import ua.kiev.splash.mathhelper.api.StorageSupport;
import ua.kiev.splash.mathhelper.dto.EquationDto;
import ua.kiev.splash.mathhelper.dto.RootDto;
import ua.kiev.splash.mathhelper.exceptions.DuplicateKeyException;
import ua.kiev.splash.mathhelper.exceptions.UnexpectedException;
import ua.kiev.splash.mathhelper.utils.Utils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StorageService implements StorageSupport, EquationRepository {
    private static final String EQUATIONS_TABLE = "equations"; // lower case
    private static final String ROOTS_TABLE = "roots";
    private static final Logger log = LoggerFactory.getLogger(StorageService.class);
    private static final String TABLE_SEARCH_PARAMETER_NAME = "TABLE";
    private static final String TABLE_SEARCH_RESULT_NAME = "TABLE_NAME";
    private static final String TABLE_CREATION_SCRIPT = "db_tables_init.sql";
    private static final String DUPLICATE_KEY_EXCEPTION_STATE = "23505";

    private static final String SQL_INSERT_EQUATION = "insert into " + EQUATIONS_TABLE + " (expression) values (?)";
    private static final String SQL_INSERT_ROOT_FOR_EQUATION = "insert into " + ROOTS_TABLE + " (eq_id, value) values (?, ?)";
    private static final String SQL_SELECT_EQUATION_BY_ID = "select expression from " + EQUATIONS_TABLE + " where id = ?";
    private static final String SQL_SELECT_ROOTS_BY_EQUATION_ID =
            "select id, eq_id, value from " + ROOTS_TABLE + " where eq_id = ? order by id";
    private static final String SQL_SELECT_EQUATIONS_BY_ROOT = "select eq.id, eq.expression from " +
            ROOTS_TABLE + " rt, " + EQUATIONS_TABLE + " eq where rt.value = ? and eq.id = rt.eq_id order by eq.id";
    private static final String SQL_SELECT_EQUATIONS_WITH_SINGLE_ROOT =
            "select eq.id, eq.expression\n" +
            "  from " + EQUATIONS_TABLE + " eq, (\n" +
            "    select eq_id\n" +
            "      from " + ROOTS_TABLE + "\n" +
            "     group by eq_id\n" +
            "    having count(eq_id) = 1) singles\n" +
            " where eq.id = singles.eq_id\n" +
            " order by eq.id\n";

    @Override
    public boolean isDataBaseTableCreated() {
        boolean result = false;
        try (Connection con = DB.connect()) {
            log.debug("Connected to the database " + con); // PostgreSQL
            try (ResultSet tables = con.getMetaData().getTables(
                    null, null, EQUATIONS_TABLE, new String[] {TABLE_SEARCH_PARAMETER_NAME})) {
                while (tables.next()) {
                    String tableName = tables.getString(TABLE_SEARCH_RESULT_NAME);
                    if (EQUATIONS_TABLE.equals(tableName)) {
                        log.info("DB table {} was found", EQUATIONS_TABLE);
                        result = true;
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            throw new UnexpectedException(e);
        }
        return result;
    }

    @Override
    public void createDataBaseTables() {
        try (Connection con = DB.connect()) {
            try (Statement statement = con.createStatement()) {
                String sql = Utils.getResourceFileAsString(TABLE_CREATION_SCRIPT);
                log.debug("Creating database tables:\n{}", sql);
                statement.execute(sql);
                log.debug("Database tables created");
            }
        } catch (SQLException e) {
            throw new UnexpectedException(e);
        }
    }

    @Override
    public void saveNewEquation(EquationDto equation) {
        log.debug("Saving equation: {}", equation);
        try (Connection con = DB.connect()) {
            try (PreparedStatement statement =
                         con.prepareStatement(SQL_INSERT_EQUATION, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, equation.getExpression());
                int affectedRows = statement.executeUpdate();
                assert affectedRows > 0;
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    keys.next();
                    equation.setId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            String sqlState = e.getSQLState();
            if (DUPLICATE_KEY_EXCEPTION_STATE.equals(sqlState)) { // or use e.getErrorCode()
                throw new DuplicateKeyException();
            }
            log.error("Save error: SQLException with sqlState {}", sqlState, e);
            throw new UnexpectedException(e);
        }
    }

    /**
     * Finds an equation by its identifier
     * @param equationId stored equation identifier
     * @return null or equation
     */
    @Override
    public EquationDto findEquationById(long equationId) {
        EquationDto result = null;
        log.debug("Searching for equation by Id: {}", equationId);
        try (Connection con = DB.connect()) {
            try (PreparedStatement statement = con.prepareStatement(SQL_SELECT_EQUATION_BY_ID)) {
                statement.setLong(1, equationId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        result = new EquationDto(equationId, rs.getString(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new UnexpectedException(e);
        }
        return result;
    }

    @Override
    public List<RootDto> findRootsByEquationId(long equationId) {
        log.debug("Searching roots by equation Id: {}", equationId);
        List<RootDto> result = new ArrayList<>();
        try (Connection con = DB.connect()) {
            try (PreparedStatement statement = con.prepareStatement(SQL_SELECT_ROOTS_BY_EQUATION_ID)) {
                statement.setLong(1, equationId);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        result.add(new RootDto(rs.getLong(1), rs.getLong(2), rs.getDouble(3)));
                    }
                }
            }
        } catch (SQLException e) {
            throw new UnexpectedException(e);
        }
        return result;
    }

    @Override
    public void saveNewRoot(RootDto root) {
        log.debug("Saving root: {}", root);
        try (Connection con = DB.connect()) {
            try (PreparedStatement statement =
                         con.prepareStatement(SQL_INSERT_ROOT_FOR_EQUATION, Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, root.getEquationId());
                statement.setDouble(2, root.getValue());
                int affectedRows = statement.executeUpdate();
                assert affectedRows > 0;
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    keys.next();
                    root.setId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            String sqlState = e.getSQLState();
            if (DUPLICATE_KEY_EXCEPTION_STATE.equals(sqlState)) { // or use e.getErrorCode()
                throw new DuplicateKeyException();
            }
            log.error("Save error: SQLException with sqlState {}", sqlState, e);
            throw new UnexpectedException(e);
        }
    }

    @Override
    public List<EquationDto> findEquationsByRootValue(double rootValue) {
        log.debug("Searching equations by root: {}", rootValue);
        List<EquationDto> result = new ArrayList<>();
        try (Connection con = DB.connect()) {
            try (PreparedStatement statement = con.prepareStatement(SQL_SELECT_EQUATIONS_BY_ROOT)) {
                statement.setDouble(1, rootValue);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        result.add(new EquationDto(rs.getLong(1), rs.getString(2)));
                    }
                }
            }
        } catch (SQLException e) {
            throw new UnexpectedException(e);
        }
        return result;
    }

    @Override
    public List<EquationDto> findEquationsWithSingleSavedRoot() {
        log.debug("Searching equations with single saved root");
        List<EquationDto> result = new ArrayList<>();
        try (Connection con = DB.connect()) {
            try (Statement statement = con.createStatement()) {
                try (ResultSet rs = statement.executeQuery(SQL_SELECT_EQUATIONS_WITH_SINGLE_ROOT)) {
                    while (rs.next()) {
                        result.add(new EquationDto(rs.getLong(1), rs.getString(2)));
                    }
                }
            }
        } catch (SQLException e) {
            throw new UnexpectedException(e);
        }
        return result;
    }
}
