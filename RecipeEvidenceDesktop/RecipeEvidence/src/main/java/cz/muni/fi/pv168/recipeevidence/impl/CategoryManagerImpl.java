package cz.muni.fi.pv168.recipeevidence.impl;

import cz.muni.fi.pv168.recipeevidence.CategoryManager;
import cz.muni.fi.pv168.recipeevidence.common.DBUtils;

import javax.sql.DataSource;
import javax.sql.rowset.serial.SerialException;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Category manager provides methods for handling categorie
 *
 * @author Tomas Soukal
 */
public class CategoryManagerImpl implements CategoryManager {

    private static final Logger logger = Logger.getLogger(
            CategoryManagerImpl.class.getName());

    private DataSource dataSource;


    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private void checkDataSource() {
        if (dataSource == null) {
            throw new IllegalStateException("DataSource is not set");
        }
    }

    private void validate(Category category) throws IllegalArgumentException {
        if (category == null) {
            throw new IllegalArgumentException("Category is null");
        }
        if (category.getCategoryName() == null) {
            throw new IllegalArgumentException("Category name is null");
        }
    }

    @Override
    public void createCategory(Category category) throws ServiceFailureException {
        checkDataSource();
        validate(category);
        if (category.getId() != null) {
            throw new IllegalEntityException("Category id is already set");
        }
        Connection conn = null;
        PreparedStatement st = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            //TABLE CATEGORY
            st = conn.prepareStatement(
                    "INSERT INTO Category (NAME) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS);
            st.setString(1, category.getCategoryName());

            int count = st.executeUpdate();
            DBUtils.checkUpdatesCount(count, category, true);

            Long id = DBUtils.getId(st.getGeneratedKeys());
            category.setCategoryID(id);
            conn.commit();
        } catch (SQLException ex) {
            String msg = "Error when inserting category into db";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        } finally {
            DBUtils.doRollbackQuietly(conn);
            DBUtils.closeQuietly(conn, st);
        }
    }

    @Override
    public void updateCategory(Category category) throws ServiceFailureException {
        checkDataSource();
        validate(category);

        if (category.getId() == null) {
            throw new IllegalEntityException("Category id is null");
        }
        Connection connection = null;
        PreparedStatement st = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            st = connection.prepareStatement(
                    "UPDATE Category SET name = ? WHERE id = ?");

            st.setString(1, category.getCategoryName());
            st.setLong(2, category.getId());

            int count = st.executeUpdate();
            DBUtils.checkUpdatesCount(count, category, false);
            connection.commit();

        } catch (SQLException ex) {
            String msg = "Error when updating category in the DB";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg , ex);
        } finally {
            DBUtils.doRollbackQuietly(connection);
            DBUtils.closeQuietly(connection, st);
        }
    }

    @Override
    public void deleteCategory(Category category) throws IllegalArgumentException, IllegalEntityException {
        checkDataSource();
        if (category == null) {
            throw new IllegalArgumentException("Category is null");
        }
        if (category.getId() == null) {
            throw new IllegalArgumentException("Category id is null");
        }
        Connection connection = null;
        PreparedStatement st = null;
        PreparedStatement st2 = null;
        RCDependencyManagerImpl rcDependencyManager = new RCDependencyManagerImpl();
        rcDependencyManager.setDataSource(dataSource);
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            st = connection.prepareStatement(
                    "SELECT NAME FROM CATEGORY WHERE ID = ?");

            st.setLong(1, category.getId());

            if (executeQueryForSingleCategory(st) == null) {
                throw new IllegalArgumentException("Category " + category + " was not found in database!");
            }

            List<Recipe> recipes = rcDependencyManager.findRecipesInCategory(category);
            for (Recipe r : recipes) {
                RCDependency dependency = new RCDependency();
                dependency.setRecipe(r);
                dependency.setCategory(category);
                rcDependencyManager.deleteDependency(dependency);
            }
            st2 = connection.prepareStatement(
                    "DELETE FROM Category WHERE ID = ?");
            st2.setLong(1, category.getId());
            int count = st2.executeUpdate();
            DBUtils.checkUpdatesCount(count, category, false);
            connection.commit();
        } catch (SQLException ex) {
            String msg = "Error when deleting category from DB";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        } catch (IllegalArgumentException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
            throw new IllegalArgumentException(ex);
        } finally {
            DBUtils.doRollbackQuietly(connection);
            if (st2 != null) {
                DBUtils.closeQuietly(connection, st2, st);
            } else {
                DBUtils.closeQuietly(connection, st);
            }
        }
    }

    @Override
    public List<Category> findAllCategories() {
        checkDataSource();
        Connection conn = null;
        PreparedStatement st = null;
        try {
            conn = dataSource.getConnection();
            st = conn.prepareStatement(
                    "SELECT ID, NAME FROM Category");
            List<Category> categories = executeQueryForMultipleCategories(st);
            return categories;
        } catch (SQLException ex) {
            String msg = "Error when getting all categories from DB";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        } finally {
            DBUtils.closeQuietly(conn, st);
        }
    }

    @Override
    public Category findCategoryById(Long id) throws ServiceFailureException {
        checkDataSource();

        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }

        Connection connection = null;
        PreparedStatement st = null;

        try {
            connection = dataSource.getConnection();
            st = connection.prepareStatement(
                    "SELECT NAME FROM Category WHERE ID = ?");

            st.setLong(1, id);
            Category category = executeQueryForSingleCategory(st);
            if (category == null) {
                return null;
            }
            category.setCategoryID(id);
            return category;
        } catch (SQLException|NullPointerException ex) {
            String msg = "Error when getting category with id = " + id + " from DB";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(
                    "Error when retrieving category with id " + id, ex);
        } finally {
            DBUtils.closeQuietly(connection, st);
        }
    }

    @Override
    public Category findCategoryByName(String name) {
        checkDataSource();

        if (name == null) {
            throw new IllegalArgumentException("Name is null");
        }

        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = dataSource.getConnection();
            ps = connection.prepareStatement(
                    "SELECT ID, NAME FROM Category WHERE name = ?"
            );

            ps.setString(1, name);
            List<Category> categories = executeQueryForMultipleCategories(ps);
            if (categories.size() == 0) {
                return null;
            } else if (categories.size() == 1) {
                return categories.get(0);
            } else {
                throw new ServiceFailureException("Multiple categories with same name (" + name + ")");
            }


        } catch (SQLException ex) {
            String msg = "Error when getting category with name = " + name + " from DB";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        } finally {
            DBUtils.closeQuietly(connection, ps);
        }
    }

    static private Category rowToCategory(ResultSet rs) throws SQLException {
        if (rs == null) {
            throw new IllegalArgumentException("Null rs");
        }
        Category category = new Category();
        category.setCategoryName(rs.getString("NAME"));
        return category;
    }



    static private Category executeQueryForSingleCategory(PreparedStatement st) throws SQLException, ServiceFailureException {
        ResultSet rs = st.executeQuery();
        if (rs.next()) {
            Category category = rowToCategory(rs);
            if (rs.next()) {
                throw new SerialException("Internal integrity error: More categories with the same id found");
            }
            return category;
        } else {
            return null;
        }
    }

    static private List<Category> executeQueryForMultipleCategories(PreparedStatement st) throws SQLException {
        ResultSet rs = st.executeQuery();
        List<Category> result = new ArrayList<Category>();
        while (rs.next()) {
            result.add(rowToMultipleCategories(rs));
        }
        return result;
    }

    private static Category rowToMultipleCategories(ResultSet rs) throws SQLException {
        Category category = new Category();
        category.setCategoryID(rs.getLong("ID"));
        category.setCategoryName(rs.getString("NAME"));
        return category;
    }
}