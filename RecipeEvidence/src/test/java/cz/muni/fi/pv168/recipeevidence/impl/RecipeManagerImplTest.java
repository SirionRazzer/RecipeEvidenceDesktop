package cz.muni.fi.pv168.recipeevidence.impl;

import cz.muni.fi.pv168.recipeevidence.CategoryManager;
import cz.muni.fi.pv168.recipeevidence.RecipeManager;
import cz.muni.fi.pv168.recipeevidence.common.DBUtils;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.junit.*;
import org.junit.rules.ExpectedException;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.*;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by lenoch on 11.3.17.
 */
public class RecipeManagerImplTest {

    private RecipeManagerImpl manager;
    private DataSource ds;
    // proc toto? jaky Clock Mock?
    private final static ZonedDateTime NOW
            = LocalDateTime.now().atZone(ZoneId.of("UTC"));

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    //--------------------------------------------------------------------------
    // Test initialization
    //----------------------------
    private static DataSource prepareDataSource() throws SQLException{
        EmbeddedDataSource ds = new EmbeddedDataSource();
        ds.setDatabaseName("memory:recipemgr-test");
        ds.setCreateDatabase("create");
        return ds;
    }

    private static Clock prepareClockMock(ZonedDateTime now){
        return Clock.fixed(now.toInstant(), now.getZone());
    }

    @Before
    public void setUp() throws SQLException {
        ds = prepareDataSource();
        if(ds == null){
            System.out.println("ds null");
        }
        DBUtils.executeSqlScript(ds, RecipeManager.class.getResource("createTables.sql"));
        manager = new RecipeManagerImpl(prepareClockMock(NOW));
        manager.setDataSource(ds);
    }

    //proc GraveManager?
    @After
    public void tearDown() throws SQLException{
        DBUtils.executeSqlScript(ds, CategoryManager.class.getResource("dropTables.sql"));
    }

    //--------------------------------------------------------------------------
    // Preparing test data
    //--------------------------------------------------------------------------

    private Recipe sampleRecipe1(){
        Recipe recipe = new Recipe();
        recipe.setName("Svíčková");
        Set<String> ingredients = new HashSet<>();
        ingredients.add("maso");
        ingredients.add("mrkev");
        recipe.setIngredients(ingredients);
        recipe.setProcedure("Uděláme svíčkovou");
        recipe.setDate(NOW.toLocalDate());
        return recipe;
    }

    private Recipe sampleRecipe2(){
        Recipe recipe = new Recipe();
        recipe.setName("Řízek");
        Set<String> ingredients = new HashSet<String>();
        ingredients.add("maso");
        ingredients.add("strouhanka");
        recipe.setIngredients(ingredients);
        recipe.setProcedure("Osmahneme řízek");
        recipe.setDate(NOW.toLocalDate());
        return recipe;
    }

    //--------------------------------------------------------------------------
    // Tests for RecipeManager.createRecipe(Recipe) operation
    //--------------------------------------------------------------------------

    @Test
    public void testCreateRecipe() {
        Recipe recipe = sampleRecipe1();
        manager.createRecipe(recipe);

        Long recipeId = recipe.getId();
        Assert.assertNotNull(recipeId);

        //isNotSameAs - compares pointers
        assertThat(manager.findRecipeById(recipeId))
                .isNotSameAs(recipe)
                .isEqualToComparingFieldByField(recipe);
    }

    @Test
    public void createRecipeWithNullName() {
        Recipe recipe = sampleRecipe1();
        recipe.setName(null);

        expectedException.expect(IllegalArgumentException.class);
        manager.createRecipe(recipe);
    }

    @Test
    public void createRecipeWithNullIngredients() {
        Recipe recipe = sampleRecipe1();
        recipe.setIngredients(null);

        expectedException.expect(IllegalArgumentException.class);
        manager.createRecipe(recipe);
    }

    @Test
    public void createRecipeWithExistingId() {
        Recipe recipe = sampleRecipe1();
        recipe.setId(1L);

        expectedException.expect(IllegalEntityException.class);
        manager.createRecipe(recipe);
    }

    //--------------------------------------------------------------------------
    // Tests for RecipeManager.deleteRecipe(Recipe) operation
    //--------------------------------------------------------------------------

    @Test
    public void deleteRecipe() {

        Recipe recipe1 = sampleRecipe1();
        Recipe recipe2 = sampleRecipe2();
        manager.createRecipe(recipe1);
        manager.createRecipe(recipe2);

        assertThat(manager.findRecipeById(recipe1.getId())).isNotNull();
        assertThat(manager.findRecipeById(recipe2.getId())).isNotNull();

        manager.deleteRecipe(recipe1);

        assertThat(manager.findRecipeById(recipe2.getId())).isNotNull();
        assertThat(manager.findRecipeById(recipe1.getId())).isNull();
    }

    // Test of delete operation with invalid parameter

    @Test(expected = IllegalArgumentException.class)
    public void deleteNullRecipe() {
        manager.deleteRecipe(null);
    }

    @Test
    public void deleteRecipeWithNullId() {
        Recipe recipe = sampleRecipe1();
        recipe.setId(null);
        expectedException.expect(IllegalEntityException.class);
        manager.deleteRecipe(recipe);
    }

    @Test
    public void deleteRecipeWithNonExistingId() {
        Recipe recipe = sampleRecipe1();
        recipe.setId(1L);
        expectedException.expect(IllegalArgumentException.class);
        manager.deleteRecipe(recipe);
    }


    //--------------------------------------------------------------------------
    // Tests for RecipeManager.updateRecipe(Recipe) operation
    //--------------------------------------------------------------------------

    @FunctionalInterface
    private static interface Operation<T> {
        void callOn(T subjectOfOperation);
    }


    private void testUpdateRecipe(Operation<Recipe> updateOperation) {
        Recipe recipeForUpdate = sampleRecipe1();
        Recipe anotherRecipe = sampleRecipe2();
        manager.createRecipe(recipeForUpdate);
        manager.createRecipe(anotherRecipe);

        updateOperation.callOn(recipeForUpdate);

        manager.updateRecipe(recipeForUpdate);
        Recipe isCorrect = manager.findRecipeById(recipeForUpdate.getId());
        assertThat(manager.findRecipeById(recipeForUpdate.getId()))
                .isEqualToComparingFieldByField(recipeForUpdate);
        // Check if updates didn't affected other records
        assertThat(manager.findRecipeById(anotherRecipe.getId()))
                .isEqualToComparingFieldByField(anotherRecipe);
    }

    @Test
    public void updateName() {
        testUpdateRecipe((recipe) -> recipe.setName("New name"));
    }

    @Test
    public void updateIngredients() {
        Set<String> updateIngredients = new HashSet<>();
        updateIngredients.add("Brambory");
        updateIngredients.add("Mouka");
        testUpdateRecipe((recipe) -> recipe.setIngredients(updateIngredients));
    }

    @Test
    public void updateProcedure() {
        testUpdateRecipe((recipe) -> recipe.setProcedure("Změna receptury"));
    }

    @Test
    public void updateDate() {
        testUpdateRecipe((recipe) -> recipe.setDate(NOW.toLocalDate()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateNullRecipe() {
        manager.updateRecipe(null);
    }

    @Test
    public void updateRecipeWithNullId() {
        Recipe recipe = sampleRecipe1();
        recipe.setId(null);
        expectedException.expect(IllegalEntityException.class);
        manager.updateRecipe(recipe);
    }

    @Test
    public void updateNonExistingRecipe() {
        Recipe recipe = sampleRecipe1();
        recipe.setId(1L);
        expectedException.expect(IllegalEntityException.class);
        manager.updateRecipe(recipe);
    }

    @Test
    public void updateRecipeWithNullName() {
        Recipe recipe = sampleRecipe1();
        manager.createRecipe(recipe);
        recipe.setName(null);

        expectedException.expect(IllegalArgumentException.class);
        manager.updateRecipe(recipe);
    }

    @Test
    public void updateRecipeWithNullIngredients() {
        Recipe recipe = sampleRecipe1();
        manager.createRecipe(recipe);
        recipe.setIngredients(null);

        expectedException.expect(IllegalArgumentException.class);
        manager.updateRecipe(recipe);
    }

    //--------------------------------------------------------------------------
    // Tests for find operations
    //--------------------------------------------------------------------------

    @Test
    public void testFindAllRecipes() throws Exception {
        assertThat(manager.findAllRecipes()).isEmpty();

        Recipe recipe1 = sampleRecipe1();
        Recipe recipe2 = sampleRecipe2();

        manager.createRecipe(recipe1);
        manager.createRecipe(recipe2);

        assertThat(manager.findAllRecipes())
                .usingFieldByFieldElementComparator()
                .containsOnly(recipe1,recipe2);
    }
    /**
     @Test
     public void testFindRecipeByName() throws Exception {
     assertThat(manager.findRecipeByName("Nic")).isEmpty();

     Recipe recipeSameName1 = sampleRecipe1();
     Recipe recipeDifferentName = sampleRecipe2();
     Recipe recipeSameName2 = sampleRecipe2();
     recipeSameName2.setName("Falešná svíčková");
     manager.createRecipe(recipeSameName1);
     manager.createRecipe(recipeSameName2);
     manager.createRecipe(recipeDifferentName);

     assertThat(manager.findRecipeByName(recipeSameName1.getName()))
     .usingFieldByFieldElementComparator()
     .containsOnly(recipeSameName1,recipeSameName2);
     }
     **/
    @Test
    public void testFindRecipeByNullName() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        manager.findRecipeByName(null);
    }

    @Test
    public void testFindRecipeByIngredients() throws Exception {
        Set<String> nonExistingIngredients = new HashSet<>();
        nonExistingIngredients.add("Nic");
        assertThat(manager.findRecipeByIngredients(nonExistingIngredients)).isEmpty();

        Recipe recipeWithDesiredIngredients1 = sampleRecipe1();
        Recipe recipeWithDesiredIngredients2 = sampleRecipe1();
        Recipe recipeWithDifferentIngredients = sampleRecipe2();
        Set<String> moreIngredientsThanDesired = recipeWithDesiredIngredients1.getIngredients();
        moreIngredientsThanDesired.add("petržel");
        recipeWithDesiredIngredients2.setIngredients(moreIngredientsThanDesired);

        manager.createRecipe(recipeWithDesiredIngredients1);
        manager.createRecipe(recipeWithDesiredIngredients2);
        manager.createRecipe(recipeWithDifferentIngredients);

        assertThat(manager.findRecipeByIngredients(recipeWithDesiredIngredients1.getIngredients()))
                .usingFieldByFieldElementComparator()
                .containsOnly(recipeWithDesiredIngredients1, recipeWithDesiredIngredients2);
    }

    @Test
    public void testFindRecipeByNullIngredients() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        manager.findRecipeByIngredients(null);
    }


    //--------------------------------------------------------------------------
    // Operations with DB
    //--------------------------------------------------------------------------
    @Test
    public void createRecipeWithSqlExceptionThrown() throws SQLException {
        // Create sqlException, which will be thrown by our DataSource mock
        // object to simulate DB operation failure
        SQLException sqlException = new SQLException();
        // Create DataSource mock object
        DataSource failingDataSource = mock(DataSource.class);
        // Instruct our DataSource mock object to throw our sqlException when
        // DataSource.getConnection() method is called.
        when(failingDataSource.getConnection()).thenThrow(sqlException);
        // Configure our manager to use DataSource mock object
        manager.setDataSource(failingDataSource);

        Recipe recipe = sampleRecipe1();

        // Try to call Manager.createBody(Body) method and expect that exception
        // will be thrown
        expectedException.expect(ServiceFailureException.class);
        manager.createRecipe(recipe);
    }

    // Now we want to test also other methods of BodyManager. To avoid having
    // couple of method with lots of duplicit code, we will use the similar
    // approach as with testUpdateBody(Operation) method.


    private void testExpectedServiceFailureException(Operation<RecipeManager> operation) throws SQLException {
        SQLException sqlException = new SQLException();
        DataSource failingDataSource = mock(DataSource.class);
        when(failingDataSource.getConnection()).thenThrow(sqlException);
        manager.setDataSource(failingDataSource);
        expectedException.expect(ServiceFailureException.class);
        operation.callOn(manager);
    }

    @Test
    public void updateRecipeWithSqlExceptionThrown() throws SQLException {
        Recipe recipe = sampleRecipe1();
        manager.createRecipe(recipe);
        testExpectedServiceFailureException((RecipeManager) -> RecipeManager.updateRecipe(recipe));
    }

    @Test
    public void getRecipeWithSqlExceptionThrown() throws SQLException {
        Recipe recipe = sampleRecipe1();
        manager.createRecipe(recipe);
        testExpectedServiceFailureException((RecipeManager) -> RecipeManager.findRecipeById(recipe.getId()));
    }

    @Test
    public void deleteRecipeWithSqlExceptionThrown() throws SQLException {
        Recipe recipe = sampleRecipe1();
        manager.createRecipe(recipe);
        testExpectedServiceFailureException((RecipeManager) -> RecipeManager.deleteRecipe(recipe));
    }

    @Test
    public void findAllRecipesWithSqlExceptionThrown() throws SQLException {
        testExpectedServiceFailureException(RecipeManager::findAllRecipes);
    }
}