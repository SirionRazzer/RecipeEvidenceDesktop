package cz.muni.fi.pv168.recipeevidence.impl;

import cz.muni.fi.pv168.recipeevidence.CategoryManager;
import cz.muni.fi.pv168.recipeevidence.RCDependencyManager;

import cz.muni.fi.pv168.recipeevidence.RecipeManager;
import cz.muni.fi.pv168.recipeevidence.common.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * This class represents manager for handling dependencies
 *
 * @author Tomas Soukal
 */

public class RCDependencyManagerImpl implements RCDependencyManager {

    private static final Logger logger = Logger.getLogger(
            CategoryManager.class.getName());

    private DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private void checkDataSource() {
        if (dataSource == null) {
            throw new IllegalStateException("DataSource is not set");
        }
    }

    private static Clock prepareClockMock(ZonedDateTime now){
        return Clock.fixed(now.toInstant(), now.getZone());
    }
    private final static ZonedDateTime NOW
            = LocalDateTime.of(2016, Month.FEBRUARY, 29, 14, 00).atZone(ZoneId.of("UTC"));


    @Override
    public List<RCDependency> findAllDependencies() throws ServiceFailureException {
        checkDataSource();
        Connection connection = null;
        PreparedStatement st = null;

        try {
            connection = dataSource.getConnection();
            st = connection.prepareStatement(
                    "SELECT recipe_id, category_id FROM Rc_dependency");

            ResultSet rs = st.executeQuery();
            List<RCDependency> result = new ArrayList<>();
            while (rs.next()) {
                Long[] ids = resultSetToRCDependency(rs);
                CategoryManagerImpl categoryManager = new CategoryManagerImpl();
                RecipeManagerImpl recipeManager = new RecipeManagerImpl(prepareClockMock(NOW));
                categoryManager.setDataSource(dataSource);
                recipeManager.setDataSource(dataSource);
                Category category = categoryManager.findCategoryById(ids[0]);
                Recipe recipe = recipeManager.findRecipeById(ids[1]);
                RCDependency dependency = new RCDependency();
                dependency.setCategory(category);
                dependency.setRecipe(recipe);
                result.add(dependency);
            }
            return result;
        } catch (SQLException ex) {
            String msg = "Error when getting all rcdependencies from DB";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        } finally {
            DBUtils.closeQuietly(connection, st);
        }
    }


    private static Long[] resultSetToRCDependency(ResultSet rs) throws SQLException {
        Long[] dependency = new Long[2];
        dependency[0] = rs.getLong("category_id");
        dependency[1] = rs.getLong("recipe_id");
        return dependency;
    }

    public boolean findDependency(Recipe recipe, Category category) throws ServiceFailureException {
        checkDataSource();
        if (recipe == null) {
            throw new IllegalArgumentException("recipe is null");
        }
        if (recipe.getId() == null) {
            throw new IllegalArgumentException("recipe id is null");
        }
        if (category == null) {
            throw new IllegalArgumentException("category is null");
        }
        if (category.getId() == null) {
            throw new IllegalArgumentException("category id is null");
        }

        Connection conn = null;
        PreparedStatement st = null;
        try {
            conn = dataSource.getConnection();
            st = conn.prepareStatement(
                    "SELECT recipe_id,category_id FROM Rc_dependency WHERE RECIPE_ID = ? and CATEGORY_ID = ?");
            st.setLong(1, recipe.getId());
            st.setLong(2, category.getId());
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException ex) {
            String msg = "Error in category " + category + "or recipe" + recipe;
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        } finally {
            DBUtils.closeQuietly(conn, st);
        }
    }

    @Override
    public void createDependency(RCDependency dependency) throws ServiceFailureException {
        checkDataSource();
        if(dependency == null){
            throw new IllegalArgumentException("Dependency is null");
        }
        if (dependency.getCategory() == null
                || dependency.getRecipe() == null) {
            throw new IllegalArgumentException("Invalid dependency");
        }

        if (dependency.getCategory().getId() == null
                || dependency.getRecipe().getId() == null) {
            throw new IllegalEntityException("Invalid dependency");
        }

        if (findDependency(dependency.getRecipe(), dependency.getCategory())){
            throw new IllegalArgumentException("Recipe is already in category");
        }

        RecipeManagerImpl existingRecipe = new RecipeManagerImpl(prepareClockMock(NOW));
        existingRecipe.setDataSource(dataSource);
        CategoryManagerImpl existingCategory = new CategoryManagerImpl();
        existingCategory.setDataSource(dataSource);

        if (existingRecipe.findRecipeById(dependency.getRecipe().getId()) == null){
            throw new IllegalEntityException("Recipe not in DB");
        }

        if (existingCategory.findCategoryById(dependency.getCategory().getId()) == null){
            throw new IllegalEntityException("Category not in DB");
        }

        Connection conn = null;
        PreparedStatement st = null;

        try {
            conn = dataSource.getConnection();
            // Temporary turn autocommit mode off. It is turned back on in
            // method DBUtils.closeQuietly(...)
            conn.setAutoCommit(false);
            st = conn.prepareStatement(
                    "INSERT INTO Rc_dependency (RECIPE_ID, CATEGORY_ID) VALUES (?,?)");
            st.setLong(1, dependency.getRecipe().getId());
            st.setLong(2, dependency.getCategory().getId());

            int count = st.executeUpdate();
            DBUtils.checkUpdatesCount(count, dependency, true);
            conn.commit();
        } catch (SQLException ex) {
            String msg = "Error when inserting dependency into db";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        } finally {
            DBUtils.doRollbackQuietly(conn);
            DBUtils.closeQuietly(conn, st);
        }

    }

    @Override
    public void deleteDependency(RCDependency dependency) throws ServiceFailureException {
        checkDataSource();
        if (dependency == null || dependency.getCategory() ==null || dependency.getRecipe()==null) {
            throw new IllegalArgumentException("dependency is null");
        }

        Connection conn = null;
        PreparedStatement st = null;
        try {
            conn = dataSource.getConnection();
            // Temporary turn autocommit mode off. It is turned back on in
            // method DBUtils.closeQuietly(...)
            conn.setAutoCommit(false);
            st = conn.prepareStatement(
                    "DELETE FROM Rc_dependency WHERE Recipe_id = ? and Category_id = ?");
            st.setLong(1, dependency.getRecipe().getId());
            st.setLong(2, dependency.getCategory().getId());
            int count = st.executeUpdate();
            DBUtils.checkUpdatesCount(count, dependency, false);
            conn.commit();
        } catch (SQLException ex) {
            String msg = "Error when deleting dependency from the db";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        } finally {
            DBUtils.doRollbackQuietly(conn);
            DBUtils.closeQuietly(conn, st);
        }
    }


    public void insertRecipeIntoCategory(Recipe recipe, Category category) throws ServiceFailureException {
        checkDataSource();
        if (recipe == null) {
            throw new IllegalArgumentException("recipe is null");
        }
        if (recipe.getId() == null) {
            throw new IllegalArgumentException("recipe id is null");
        }
        if (category == null) {
            throw new IllegalArgumentException("category is null");
        }
        if (category.getId() == null) {
            throw new IllegalArgumentException("category id is null");
        }

        RCDependency dependency = new RCDependency();
        dependency.setRecipe(recipe);
        dependency.setCategory(category);
        if (!findDependency(recipe, category)){
            createDependency(dependency);
        }
    }

    @Override
    public List<Recipe> findRecipesInCategory(Category category) throws ServiceFailureException {
        checkDataSource();
        if (category == null) {
            throw new IllegalArgumentException("Cat is null");
        }
        if (category.getId() == null) {
            throw new IllegalEntityException("Cat id is null");
        }
        Connection conn = null;
        PreparedStatement st = null;
        try {
            conn = dataSource.getConnection();
            st = conn.prepareStatement(
                    "SELECT RECIPE_ID FROM Rc_dependency WHERE CATEGORY_ID = ?");
            st.setLong(1, category.getId());

            List<Long> ids = RCDependencyManagerImpl.executeQueryForMultipleRCDependenciesRecipe(st);
            List<Recipe> resultSet = new ArrayList<>();
            RecipeManagerImpl manager = new RecipeManagerImpl(prepareClockMock(ZonedDateTime.now()));
            manager.setDataSource(dataSource);
            for( Long id: ids){
                Recipe foundRecipe = manager.findRecipeById(id);
                resultSet.add(foundRecipe);
            }
            return resultSet;
        } catch (SQLException ex) {
            String msg = "Error " + category;
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        } finally {
            DBUtils.closeQuietly(conn, st);
        }
    }

    private static List<Long> executeQueryForMultipleRCDependenciesRecipe(PreparedStatement st) throws SQLException {
        ResultSet rs = st.executeQuery();
        List<Long> result = new ArrayList<>();
        while (rs.next()) {
            result.add(rs.getLong("RECIPE_ID")); //rowToGrave
        }
        return result;
    }


    static List<Long> executeQueryForMultipleRCDependenciesCategory(PreparedStatement st) throws SQLException {
        ResultSet rs = st.executeQuery();
        List<Long> result = new ArrayList<>();
        while (rs.next()) {
            result.add(rs.getLong("CATEGORY_ID")); //rowToGrave
        }
        return result;
    }

    @Override
    public List<Category> findCategoriesForRecipe(Recipe recipe) throws ServiceFailureException {
        checkDataSource();
        if (recipe == null) {
            throw new IllegalArgumentException("Recipe is null");
        }
        if (recipe.getId() == null) {
            throw new IllegalEntityException("Recipe id is null");
        }
        Connection conn = null;
        PreparedStatement st = null;
        try {
            conn = dataSource.getConnection();
            st = conn.prepareStatement(
                    "SELECT Category_id FROM Rc_dependency WHERE RECIPE_ID = ?");
            st.setLong(1, recipe.getId());

            List<Long> ids = RCDependencyManagerImpl.executeQueryForMultipleRCDependenciesCategory(st);
            List<Category> resultSet = new ArrayList<>();
            CategoryManagerImpl manager = new CategoryManagerImpl();
            manager.setDataSource(dataSource);
            for( Long id: ids){
                Category foundCategory = manager.findCategoryById(id);
                resultSet.add(foundCategory);
            }
            return resultSet;

        } catch (SQLException ex) {
            String msg = "Error " + recipe;
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        } finally {
            DBUtils.closeQuietly(conn, st);
        }

    }


}