package org.delcom.ifs25050springbootcashflow.adapter.repository;

import org.delcom.ifs25050springbootcashflow.domain.entity.Role;
import org.delcom.ifs25050springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25050springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25050springbootcashflow.domain.entity.User;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcFailureTest {

    @Test
    void schemaInitializerFailsWhenConnectionBroken() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("down"));
        assertThrows(IllegalStateException.class, () -> SchemaInitializer.initialize(dataSource));
    }

    @Test
    void userRepositoryPropagatesSqlFailures() throws Exception {
        DataSource dataSource = failingDataSource();
        JdbcUserRepository repository = new JdbcUserRepository(dataSource);

        assertThrows(IllegalStateException.class, repository::findAll);
        assertThrows(IllegalStateException.class, () -> repository.findById(1));
        assertThrows(IllegalStateException.class, () -> repository.findByUsername("x"));
        assertThrows(IllegalStateException.class, () -> repository.saveNew("u", "h", "n", Role.USER));
        assertThrows(IllegalStateException.class, () -> repository.deleteById(1));
        assertThrows(IllegalStateException.class, () ->
                repository.update(new User(1, "u", "h", "n", Role.USER, null)));
    }

    @Test
    void userSaveNewFailsWhenNoGeneratedKey() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet keys = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        when(statement.getGeneratedKeys()).thenReturn(keys);
        when(keys.next()).thenReturn(false);

        JdbcUserRepository repository = new JdbcUserRepository(dataSource);
        assertThrows(IllegalStateException.class, () -> repository.saveNew("u", "h", "n", Role.USER));
    }

    @Test
    void transactionRepositoryPropagatesSqlFailures() throws Exception {
        DataSource dataSource = failingDataSource();
        JdbcTransactionRepository repository = new JdbcTransactionRepository(dataSource);
        Transaction t = new Transaction(1, 1, "Kopi", 25000L, TransactionType.EXPENSE);

        assertThrows(IllegalStateException.class, repository::findAll);
        assertThrows(IllegalStateException.class, () -> repository.findByUserId(1));
        assertThrows(IllegalStateException.class, () -> repository.findById(1));
        assertThrows(IllegalStateException.class, () -> repository.save(1, "Kopi", 25000L, TransactionType.EXPENSE));
        assertThrows(IllegalStateException.class, () -> repository.deleteById(1));
        assertThrows(IllegalStateException.class, () -> repository.update(t));
    }

    @Test
    void transactionSaveFailsWhenNoGeneratedKey() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet keys = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        when(statement.getGeneratedKeys()).thenReturn(keys);
        when(keys.next()).thenReturn(false);

        JdbcTransactionRepository repository = new JdbcTransactionRepository(dataSource);
        assertThrows(IllegalStateException.class, () -> repository.save(1, "Kopi", 25000L, TransactionType.EXPENSE));
    }

    @Test
    void closeFailuresAreHandled() throws Exception {
        JdbcUserRepository userRepo = new JdbcUserRepository(queryWithCloseFailure(false));
        assertTrue(userRepo.findById(1).isEmpty());
        assertTrue(userRepo.findByUsername("andi").isEmpty());

        JdbcUserRepository userRepoFail = new JdbcUserRepository(queryWithCloseFailure(true));
        assertThrows(IllegalStateException.class, () -> userRepoFail.findById(1));

        JdbcTransactionRepository txRepo = new JdbcTransactionRepository(queryWithCloseFailure(false));
        assertTrue(txRepo.findById(1).isEmpty());

        JdbcTransactionRepository txRepoFail = new JdbcTransactionRepository(queryWithCloseFailure(true));
        assertThrows(IllegalStateException.class, () -> txRepoFail.findById(1));
    }

    private DataSource failingDataSource() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("down"));
        return dataSource;
    }

    private DataSource queryWithCloseFailure(boolean failBody) throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(rs);
        if (failBody) {
            when(rs.next()).thenThrow(new SQLException("body"));
        } else {
            when(rs.next()).thenReturn(false);
        }
        doThrow(new SQLException("rs-close")).when(rs).close();
        doThrow(new SQLException("ps-close")).when(statement).close();
        doThrow(new SQLException("conn-close")).when(connection).close();
        return dataSource;
    }
}