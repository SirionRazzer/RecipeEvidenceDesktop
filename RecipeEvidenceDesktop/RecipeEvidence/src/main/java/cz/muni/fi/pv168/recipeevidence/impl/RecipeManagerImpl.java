package cz.muni.fi.pv168.recipeevidence.impl;

import cz.muni.fi.pv168.recipeevidence.RecipeManager;
import cz.muni.fi.pv168.recipeevidence.common.DBUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class RecipeManagerImpl implements RecipeManager {

    private static final Logger logger = Logger.getLogger(RecipeManagerImpl.class.getName());

    private DataSource dataSource;
    private final Clock clock;

    public RecipeManagerImpl(Clock clock) {
        this.clock = clock;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private void checkDataSource() {
        if (dataSource == null) {
            throw new IllegalStateException("DataSource is not set");
        }
    }

    private void validate(Recipe recipe) {
        if (recipe == null) {
            throw new IllegalArgumentException("Recipe is null");
        }
        if (recipe.getName() == null) {
            throw new IllegalArgumentException("Name is null");
        }
        if (recipe.getIngredients() == null) {
            throw new IllegalArgumentException("Recipe without ingredients cannot exist");
        }
        if (recipe.getProcedure() == null) {
            throw new IllegalArgumentException("Recipe without procedure cannot exist");
        }
        LocalDate today = LocalDate.now(clock);
        if (recipe.getDate() != null && !recipe.getDate().equals(today)) {
            throw new IllegalArgumentException("Date of a recipe creation must be today ");
        }
    }


    //--------------------------------------------------------------------------
    // Implementing RecipeManager functions
    //--------------------------------------------------------------------------


    public void createRecipe(Recipe recipe) throws ServiceFailureException {
        checkDataSource();
        validate(recipe);
        if (recipe.getId() != null) {
            throw new IllegalEntityException("recipe id is already set");
        }
        Connection connection = null;
        PreparedStatement statement1 = null;
        PreparedStatement statement2 = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            //TABLE RECIPE
            statement1 = connection.prepareStatement(
                    "INSERT INTO Recipe (NAME,RECIPE_PROCEDURE,DATE) VALUES (?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement1.setString(1, recipe.getName());
            statement1.setString(2, recipe.getProcedure());
            statement1.setDate(3, toSqlDate(recipe.getDate()));

            int count1 = statement1.executeUpdate();
            DBUtils.checkUpdatesCount(count1, recipe, true);

            Long id = DBUtils.getId(statement1.getGeneratedKeys());
            recipe.setId(id);
            //TABLE INGREDIENTS
            for (String element : recipe.getIngredients()) {
                statement2 = connection.prepareStatement("INSERT INTO Ingredients (RECIPE_ID, name) VALUES (?,?)");
                statement2.setLong(1, recipe.getId());
                statement2.setString(2, element);
                int count2 = statement2.executeUpdate();
                DBUtils.checkUpdatesCount(count2, recipe, true);
                connection.commit();
            }
        } catch (SQLException ex) {
            String msg = "Error when inserting recipe into db";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        } finally {
            DBUtils.doRollbackQuietly(connection);
            DBUtils.closeQuietly(connection, statement1, statement2);
        }

    }


    public void updateRecipe(Recipe recipe) throws ServiceFailureException {
        checkDataSource();
        validate(recipe);

        if (recipe.getId() == null) {
            throw new IllegalEntityException("recipe id is null");
        }
        Connection connection = null;
        PreparedStatement statement1 = null;
        PreparedStatement statement2 = null;
        PreparedStatement statement3 = null;
        PreparedStatement statement4 = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            statement1 = connection.prepareStatement(
                    "UPDATE Recipe SET name = ?, RECIPE_PROCEDURE = ?, date  = ? WHERE id = ?");
            statement1.setString(1, recipe.getName());
            statement1.setString(2, recipe.getProcedure());
            statement1.setDate(3, toSqlDate(recipe.getDate()));
            statement1.setLong(4, recipe.getId());
            int count1 = statement1.executeUpdate();
            DBUtils.checkUpdatesCount(count1, recipe, false);
            connection.commit();
            statement3 = connection.prepareStatement("SELECT name FROM INGREDIENTS WHERE RECIPE_ID= ? ");
            statement3.setLong(1, recipe.getId());
            Set<String> existingIngredients = executeQueryForIngredients(statement3);

            statement2 = connection.prepareStatement("INSERT INTO Ingredients (RECIPE_ID, name) VALUES (?,?)");
            statement2.setLong(1, recipe.getId());
            for (String element : recipe.getIngredients()) {
                if (!existingIngredients.contains(element)) {
                    statement2.setString(2, element);
                    int count2 = statement2.executeUpdate();
                    DBUtils.checkUpdatesCount(count2, recipe, true);
                    connection.commit();
                }
            }
            statement4 = connection.prepareStatement("DELETE FROM INGREDIENTS where RECIPE_ID =? and name=?");
            statement4.setLong(1, recipe.getId());
            for (String element : existingIngredients) {
                if (!recipe.getIngredients().contains(element)) {
                    statement4.setString(2, element);
                    int count4 = statement4.executeUpdate();
                    DBUtils.checkUpdatesCount(count4, recipe, false);
                    connection.commit();
                }
            }

        } catch (SQLException ex) {
            String msg = "Error when updating recipe in the DB";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        } finally {
            DBUtils.doRollbackQuietly(connection);
            if (statement3 != null && statement4 != null) {
                DBUtils.closeQuietly(connection, statement1, statement2, statement3, statement4);
            } else if (statement3 != null) {
                DBUtils.closeQuietly(connection, statement1, statement2, statement3);
            } else if (statement4 != null) {
                DBUtils.closeQuietly(connection, statement1, statement2, statement4);
            } else {
                DBUtils.closeQuietly(connection, statement1, statement2);
            }
        }
    }


    public void deleteRecipe(Recipe recipe) throws IllegalArgumentException, IllegalEntityException {
        checkDataSource();
        if (recipe == null) {
            throw new IllegalArgumentException("recipe is null");
        }
        if (recipe.getId() == null) {
            throw new IllegalEntityException("recipe id is null");
        }
        Connection connection = null;
        PreparedStatement statement2 = null;
        PreparedStatement existingID = null;

        RCDependencyManagerImpl dependencyManager = new RCDependencyManagerImpl();
        dependencyManager.setDataSource(dataSource);
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            existingID = connection.prepareStatement(
                    "SELECT NAME,RECIPE_PROCEDURE,DATE FROM Recipe WHERE id = ?");
            existingID.setLong(1, recipe.getId());

            if (executeQueryForSingleRecipe(existingID) == null) {
                throw new IllegalArgumentException("recipe id does not exist");
            }
            List<Category> categories = dependencyManager.findCategoriesForRecipe(recipe);
            for (Category element : categories) {
                RCDependency dependency = new RCDependency();
                dependency.setRecipe(recipe);
                dependency.setCategory(element);
                dependencyManager.deleteDependency(dependency);
            }

            /** statement1 = connection.prepareStatement(
             "DELETE FROM Ingredients WHERE RECIPE_ID = ?");
             statement1.setLong(1, recipe.getId());
             //int count1 = statement1.executeUpdate();
             //DBUtils.checkUpdatesCount(count1, recipe, false);
             **/
            statement2 = connection.prepareStatement(
                    "DELETE FROM Recipe WHERE ID = ?");
            statement2.setLong(1, recipe.getId());
            int count2 = statement2.executeUpdate();
            DBUtils.checkUpdatesCount(count2, recipe, false);

            connection.commit();

        } catch (SQLException ex) {
            String msg = "Error when deleting Recipe from the DB";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        } catch (IllegalArgumentException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
            throw new IllegalArgumentException(ex);
        } finally {
            DBUtils.doRollbackQuietly(connection);
            if (statement2 != null) {
                DBUtils.closeQuietly(connection, statement2, existingID);
            } else {
                DBUtils.closeQuietly(connection, existingID);
            }
        }
    }


    public List<Recipe> findAllRecipes() {
        checkDataSource();
        Connection connection = null;
        PreparedStatement statement1 = null;
        PreparedStatement statement2 = null;
        try {
            connection = dataSource.getConnection();
            statement1 = connection.prepareStatement(
                    "SELECT ID, NAME, date, RECIPE_PROCEDURE  FROM Recipe");
            List<Recipe> recipes = executeQueryForMultipleRecipes(statement1);

            statement2 = connection.prepareStatement(
                    "SELECT name FROM Ingredients WHERE RECIPE_ID = ?");
            for (Recipe element : recipes) {
                statement2.setLong(1, element.getId());
                element.setIngredients(findIngredients(statement2));
            }
            return recipes;

        } catch (SQLException ex) {
            String msg = "Error when getting all recipes from DB";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        } finally {
            DBUtils.closeQuietly(connection, statement1, statement2);
        }
    }


    public Recipe findRecipeById(Long id) throws ServiceFailureException {

        checkDataSource();

        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }

        Connection connection = null;
        PreparedStatement statement1 = null;
        PreparedStatement statement2 = null;
        try {
            connection = dataSource.getConnection();
            statement1 = connection.prepareStatement(
                    "SELECT NAME,RECIPE_PROCEDURE,DATE FROM Recipe WHERE ID = ?");
            statement1.setLong(1, id);
            statement2 = connection.prepareStatement("SELECT name FROM Ingredients WHERE RECIPE_ID = ?");
            statement2.setLong(1, id);
            Recipe recipe = executeQueryForSingleRecipe(statement1);
            if (recipe == null) {
                return null;
                //throw new ServiceFailureException("Recipe is not in db");
            }
            recipe.setIngredients(findIngredients(statement2));
            recipe.setId(id);
            return recipe;
        } catch (SQLException | NullPointerException ex) {
            String msg = "Error when getting recipe with id = " + id + " from DB";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        } finally {
            DBUtils.closeQuietly(connection, statement1, statement2);
        }
    }


    public List<Recipe> findRecipeByName(String nameToFind) throws ServiceFailureException {
        checkDataSource();

        if (nameToFind == null) {
            throw new IllegalArgumentException("name is null");
        }

        Connection connection = null;
        PreparedStatement statement1 = null;
        PreparedStatement statement2 = null;
        try {
            connection = dataSource.getConnection();
            statement1 = connection.prepareStatement(
                    "SELECT id, name, RECIPE_PROCEDURE, date FROM Recipe WHERE name = ?"); // do not know how to transfer it for substring %substr%
            statement1.setString(1, nameToFind);
            List<Recipe> recipes = executeQueryForMultipleRecipes(statement1);
            if (recipes.size() == 0) {
                return recipes;
            }

            statement2 = connection.prepareStatement(
                    "SELECT name FROM Ingredients WHERE id = ?");
            for (Recipe element : recipes) {
                statement2.setLong(1, element.getId());
                element.setIngredients(findIngredients(statement2));
            }
            return recipes;

        } catch (SQLException ex) {
            String msg = "Error when getting recipe with name = " + nameToFind + " from DB";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        } finally {
            if (statement2 != null) {
                DBUtils.closeQuietly(connection, statement1, statement2);
            } else {
                DBUtils.closeQuietly(connection, statement1);
            }
        }
    }


    public List<Recipe> findRecipeByIngredients(Set<String> ingredients) throws ServiceFailureException {
        checkDataSource();

        if (ingredients == null || ingredients.isEmpty()) {
            throw new IllegalArgumentException("No ingredients");
        }

        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = dataSource.getConnection();
            Set<Long> containingAllIngredients = new TreeSet<>();
            String[] arrayOfIngredients = ingredients.toArray(new String[ingredients.size()]);
            statement = connection.prepareStatement(
                    "SELECT RECIPE_ID FROM Ingredients WHERE NAME = ?");
            statement.setString(1, arrayOfIngredients[0]);
            ResultSet rs1 = statement.executeQuery();
            while (rs1.next()) {
                containingAllIngredients.add(rs1.getLong("RECIPE_ID"));
            }

            for (int i = 1; i < arrayOfIngredients.length; i++) {
                Set<Long> set = new TreeSet<>();
                statement.setString(1, arrayOfIngredients[i]);
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    set.add(rs.getLong("RECIPE_ID"));
                }
                containingAllIngredients.retainAll(set);
                if (containingAllIngredients.isEmpty()) {
                    List<Recipe> empty = new ArrayList<>();
                    return empty;
                }
            }
            List<Recipe> recipes = new ArrayList<>();
            for (Long id : containingAllIngredients) {
                recipes.add(findRecipeById(id));
            }
            return recipes;
        } catch (SQLException ex) {
            String msg = "Error when getting ingredients from DB";
            logger.log(Level.SEVERE, msg, ex);
            throw new ServiceFailureException(msg, ex);
        } finally {
            DBUtils.closeQuietly(connection, statement);
        }
    }

    //--------------------------------------------------------------------------
    // Other helpful functions
    //--------------------------------------------------------------------------

    /**
     * Used in createRecipe and updateRecipe to transform date
     *
     * @param localDate date
     * @return date which will be added to DB
     */
    private static Date toSqlDate(LocalDate localDate) {
        return localDate == null ? null : Date.valueOf(localDate);
    }

    static Recipe executeQueryForSingleRecipe(PreparedStatement st) throws SQLException, ServiceFailureException {
        ResultSet rs = st.executeQuery();
        if (rs.next()) {
            Recipe result = rowToRecipe(rs);
            if (rs.next()) {
                throw new ServiceFailureException(
                        "Internal integrity error: more recipes with the same id found!");
            }
            return result;
        } else {
            return null;
        }
    }

    /**
     * Used in findAllRecipes and findRecipe by name
     *
     * @param statement SQL statement
     * @return Recipes from DB which satisfy condition in SQL statement
     * @throws SQLException
     */
    static List<Recipe> executeQueryForMultipleRecipes(PreparedStatement statement) throws SQLException {
        ResultSet rs = statement.executeQuery();
        List<Recipe> result = new ArrayList<Recipe>();
        while (rs.next()) {
            result.add(rowToMultipleRecipes(rs));
        }
        return result;
    }

    static Set<String> executeQueryForIngredients(PreparedStatement statement) throws SQLException {
        ResultSet rs = statement.executeQuery();
        Set<String> result = new HashSet<>();
        while (rs.next()) {
            result.add(rs.getString("NAME"));
        }
        return result;
    }

    /**
     * Used in executeQueryForMultiplyRecipes and
     *
     * @param rs is set of recipes satisfying condition
     * @return object recipe with set parameters
     * @throws SQLException
     */
    static private Recipe rowToRecipe(ResultSet rs) throws SQLException {
        Recipe result = new Recipe();
        result.setName(rs.getString("NAME"));
        result.setProcedure(rs.getString("RECIPE_PROCEDURE"));
        result.setDate(toLocalDate(rs.getDate("DATE")));
        return result;
    }

    static private Recipe rowToMultipleRecipes(ResultSet rs) throws SQLException {
        Recipe result = new Recipe();
        result.setId(rs.getLong("ID"));
        result.setName(rs.getString("NAME"));
        result.setProcedure(rs.getString("RECIPE_PROCEDURE"));
        result.setDate(toLocalDate(rs.getDate("DATE")));
        return result;
    }

    public Set<String> findIngredients(PreparedStatement statement) throws SQLException {
        ResultSet rs = statement.executeQuery();
        Set<String> ingredients = new HashSet<String>();
        while (rs.next()) {
            ingredients.add(rs.getString("name"));
        }
        return ingredients;
    }

    private static LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }
}