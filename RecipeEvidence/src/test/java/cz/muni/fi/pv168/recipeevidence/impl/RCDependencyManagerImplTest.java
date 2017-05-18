package cz.muni.fi.pv168.recipeevidence.impl;

import cz.muni.fi.pv168.recipeevidence.RCDependencyManager;
import cz.muni.fi.pv168.recipeevidence.RecipeManager;
import cz.muni.fi.pv168.recipeevidence.common.DBUtils;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Petra Halová
 */
public class RCDependencyManagerImplTest {

    private RCDependencyManagerImpl dependencyManager;
    private RecipeManagerImpl recipeManager;
    private CategoryManagerImpl categoryManager;
    private DataSource ds;

    private final static ZonedDateTime NOW
            = LocalDateTime.now().atZone(ZoneId.of("UTC"));

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    //--------------------------------------------------------------------------
    // Test initialization
    //--------------------------------------------------------------------------
    private static DataSource prepareDataSource() throws SQLException {
        EmbeddedDataSource ds = new EmbeddedDataSource();
        // we will use in memory database
        ds.setDatabaseName("memory:dependencymgr-test");
        // database is created automatically if it does not exist yet
        ds.setCreateDatabase("create");
        return ds;
    }

    private static Clock prepareClockMock(ZonedDateTime now){
        return Clock.fixed(now.toInstant(), now.getZone());
    }

    @Before
    public void setUp() throws SQLException {
        ds = prepareDataSource();
        DBUtils.executeSqlScript(ds, RCDependencyManager.class.getResource("createTables.sql"));
        dependencyManager = new RCDependencyManagerImpl();
        dependencyManager.setDataSource(ds);
        recipeManager = new RecipeManagerImpl(prepareClockMock(NOW));
        recipeManager.setDataSource(ds);
        categoryManager = new CategoryManagerImpl();
        categoryManager.setDataSource(ds);
        prepareTestData();
    }

    @After
    public void tearDown() throws SQLException {
        DBUtils.executeSqlScript(ds, RecipeManager.class.getResource("dropTables.sql"));
    }

    //--------------------------------------------------------------------------

    //--------------------------------------------------------------------------
    // Preparing test data
    //--------------------------------------------------------------------------

    private Category maso, omacky, emptyCategory, categoryWithNullId, categoryNotInDatabase;
    private Recipe recipe1, recipe2, recipe3, recipeInNonCategory ,recipeWithNullId, recipeNotInDatabase;
    private void prepareTestData() {
        recipe1 = new Recipe();
        recipe1.setName("Svíčková");
        Set<String> ingredients1 = new HashSet<>();
        ingredients1.add("maso");
        ingredients1.add("mrkev");
        recipe1.setIngredients(ingredients1);
        recipe1.setProcedure("Uděláme svíčkovou");
        recipe1.setDate(NOW.toLocalDate());

        recipe2 = new Recipe();
        recipe2.setName("Řízek");
        Set<String> ingredients2 = new HashSet<String>();
        ingredients2.add("maso");
        ingredients2.add("strouhanka");
        recipe2.setIngredients(ingredients2);
        recipe2.setProcedure("Osmahneme řízek");
        recipe2.setDate(NOW.toLocalDate());


        recipe3 = new Recipe();
        recipe3.setName("Dalsi recept");
        recipe3.setIngredients(recipe1.getIngredients());
        recipe3.setProcedure("Just do it!");

        recipeInNonCategory = new Recipe();
        recipeInNonCategory.setName("Jsem nikde");
        recipeInNonCategory.setIngredients(recipe2.getIngredients());
        recipeInNonCategory.setProcedure("Lost");

        recipeManager.createRecipe(recipe1);
        recipeManager.createRecipe(recipe2);
        recipeManager.createRecipe(recipe3);
        recipeManager.createRecipe(recipeInNonCategory);

        maso = new Category();
        maso.setCategoryName("Maso");

        omacky = new Category();
        omacky.setCategoryName("Omacky");

        emptyCategory = new Category();
        emptyCategory.setCategoryName("Jsem prazdna");

        categoryManager.createCategory(maso);
        categoryManager.createCategory(omacky);
        categoryManager.createCategory(emptyCategory);

        categoryNotInDatabase = new Category();
        categoryNotInDatabase.setCategoryName("NotInDB");
        categoryNotInDatabase.setCategoryID(maso.getId()+100);

        assertThat(categoryManager.findCategoryById(categoryNotInDatabase.getId())).isNull();

        recipeNotInDatabase = new Recipe();
        recipeNotInDatabase.setId(recipe1.getId() + 100);
        recipeNotInDatabase.setName("Recipe not in DB");
        recipeNotInDatabase.setIngredients(recipe2.getIngredients());
        recipeNotInDatabase.setProcedure("None");

        assertThat(recipeManager.findRecipeById(recipeNotInDatabase.getId())).isNull();
    }


    private void sampleNullIdCategory() {
        categoryWithNullId = new Category();
        categoryWithNullId.setCategoryID(null);
        categoryWithNullId.setCategoryName("Bez id");

    }

    private void sampleNullIdRecipe() {
        recipeWithNullId = new Recipe();
        recipeWithNullId.setId(null);
        recipeWithNullId.setName("Nemam id");

    }


    //--------------------------------------------------------------------------
    // Tests for create operations
    //--------------------------------------------------------------------------

    @Test
    public void createDependency() {
        prepareTestData();
        assertThat(dependencyManager.findCategoriesForRecipe(recipe1)).isEmpty();
        assertThat(dependencyManager.findCategoriesForRecipe(recipe2)).isEmpty();

        RCDependency recipe1InMaso = new RCDependencyBuilder()
                .category(maso).recipe(recipe1).build();
        RCDependency recipe2InMaso = new RCDependencyBuilder()
                .category(maso).recipe(recipe2).build();
        RCDependency recipe1InOmacky = new RCDependencyBuilder()
                .category(omacky).recipe(recipe1).build();
        dependencyManager.createDependency(recipe1InMaso);
        dependencyManager.createDependency(recipe2InMaso);
        dependencyManager.createDependency(recipe1InOmacky);

        assertThat(dependencyManager.findRecipesInCategory(maso))
                .usingFieldByFieldElementComparator().containsOnly(recipe1, recipe2);

        assertThat(dependencyManager.findRecipesInCategory(omacky))
                .usingFieldByFieldElementComparator().containsOnly(recipe1);
        assertThat(dependencyManager.findRecipesInCategory(emptyCategory)).isEmpty();

        assertThat(dependencyManager.findCategoriesForRecipe(recipe1))
                .usingFieldByFieldElementComparator().containsOnly(maso, omacky);
        assertThat(dependencyManager.findCategoriesForRecipe(recipe2))
                .usingFieldByFieldElementComparator().containsOnly(maso);
        assertThat(dependencyManager.findCategoriesForRecipe(recipeInNonCategory)).isEmpty();

    }

    @Test
    public void putRecipeIntoCategoryMultipleTime() {
        prepareTestData();
        RCDependency firstDependency = new RCDependencyBuilder()
                .category(maso).recipe(recipe1).build();
        RCDependency sameDependency = new RCDependencyBuilder()
                .category(maso).recipe(recipe1).build();
        RCDependency secondDependency = new RCDependencyBuilder()
                .category(maso).recipe(recipe2).build();
        RCDependency thirdDependency = new RCDependencyBuilder()
                .category(omacky).recipe(recipe3).build();

        dependencyManager.createDependency(firstDependency);
        dependencyManager.createDependency(secondDependency);
        dependencyManager.createDependency(thirdDependency);
        expectedException.expect(IllegalArgumentException.class);
        dependencyManager.createDependency(sameDependency);

        // verify that failure was atomic and no data was changed
        assertThat(dependencyManager.findRecipesInCategory(maso))
                .usingFieldByFieldElementComparator()
                .containsOnly(recipe1,recipe2);
        assertThat(dependencyManager.findRecipesInCategory(emptyCategory))
                .isEmpty();
        assertThat(dependencyManager.findRecipesInCategory(omacky))
                .usingFieldByFieldElementComparator()
                .containsOnly(recipe3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createNullDependency() {
        dependencyManager.createDependency(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createDependencyWithNullRecipe() {
        prepareTestData();
        RCDependency nullRecipe = new RCDependencyBuilder().category(maso).recipe(null).build();
        dependencyManager.createDependency(nullRecipe);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createDependencyWithNullCategory() {
        prepareTestData();
        RCDependency nullCategory = new RCDependencyBuilder()
                .category(null).recipe(recipe1).build();
        dependencyManager.createDependency(nullCategory);
    }

    @Test(expected = IllegalEntityException.class)
    public void createDependencyWithRecipeHavingNullId() {
        sampleNullIdRecipe();
        RCDependency nullRecipeId = new RCDependencyBuilder().category(maso).
                recipe(recipeWithNullId).build();
        dependencyManager.createDependency(nullRecipeId);
    }

    @Test(expected = IllegalEntityException.class)
    public void createDependencyWithCategoryHavingNullId() {
        sampleNullIdCategory();
        RCDependency nullCategoryId = new RCDependencyBuilder().category(categoryWithNullId).
                recipe(recipe1).build();
        dependencyManager.createDependency(nullCategoryId);
    }

    @Test(expected = IllegalEntityException.class)
    public void createDependencyWithRecipeNotInDB() {
        prepareTestData();
        RCDependency recipeNotInDB = new RCDependencyBuilder().category(maso).
                recipe(recipeNotInDatabase).build();
        dependencyManager.createDependency(recipeNotInDB);
    }

    @Test(expected = IllegalEntityException.class)
    public void createDependencyWithCategoryNotInDB() {
        prepareTestData();
        RCDependency categoryNotInDB = new RCDependencyBuilder().category(categoryNotInDatabase).
                recipe(recipe1).build();
        dependencyManager.createDependency(categoryNotInDB);
    }

    //--------------------------------------------------------------------------
    // Tests for delete operation
    //--------------------------------------------------------------------------

    @Test
    public void testDeleteDependency() throws Exception {
        prepareTestData();
        RCDependency recipe1inMaso = new RCDependencyBuilder()
                .category(maso).recipe(recipe1).build();
        RCDependency recipe2inMaso = new RCDependencyBuilder()
                .category(maso).recipe(recipe2).build();
        RCDependency recipe1inOmacky = new RCDependencyBuilder()
                .category(omacky).recipe(recipe1).build();

        dependencyManager.createDependency(recipe1inMaso);
        dependencyManager.createDependency(recipe2inMaso);
        dependencyManager.createDependency(recipe1inOmacky);

        assertThat(dependencyManager.findCategoriesForRecipe(recipe1))
                .usingFieldByFieldElementComparator().containsOnly(maso, omacky);

        assertThat(dependencyManager.findCategoriesForRecipe(recipe2))
                .usingFieldByFieldElementComparator().containsOnly(maso);

        assertThat(dependencyManager.findCategoriesForRecipe(recipeInNonCategory))
                .isEmpty();

        dependencyManager.deleteDependency(recipe1inMaso);

        assertThat(dependencyManager.findRecipesInCategory(maso))
                .usingFieldByFieldElementComparator()
                .containsOnly(recipe2);

        assertThat(dependencyManager.findRecipesInCategory(omacky))
                .usingFieldByFieldElementComparator()
                .containsOnly(recipe1);

        assertThat(dependencyManager.findRecipesInCategory(emptyCategory))
                .isEmpty();

        assertThat(dependencyManager.findCategoriesForRecipe(recipe1))
                .usingFieldByFieldElementComparator()
                .containsOnly(omacky);

        assertThat(dependencyManager.findCategoriesForRecipe(recipe2))
                .usingFieldByFieldElementComparator()
                .containsOnly(maso);

        assertThat(dependencyManager.findCategoriesForRecipe(recipeInNonCategory)).isEmpty();
    }


    //--------------------------------------------------------------------------
    // Tests for find operation
    //--------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void testFindCategoryWithNullRecipe() throws Exception {
        dependencyManager.findCategoriesForRecipe(null);
    }

    @Test(expected = IllegalEntityException.class)
    public void testFindCategoryWithRecipeHavingNullId() throws Exception {
        sampleNullIdRecipe();
        dependencyManager.findCategoriesForRecipe(recipeWithNullId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindRecipeWithNullCategory() throws Exception {
        dependencyManager.findRecipesInCategory(null);
    }

    @Test(expected = IllegalEntityException.class)
    public void testFindRecipeWithCategoryHavingNullId() throws Exception {
        sampleNullIdCategory();
        dependencyManager.findRecipesInCategory(categoryWithNullId);
    }

    @Test
    public void testFindRecipesInCategory() throws Exception {
        prepareTestData();
        assertThat(dependencyManager.findRecipesInCategory(maso)).isEmpty();
        assertThat(dependencyManager.findRecipesInCategory(omacky)).isEmpty();

        RCDependency recipe1InMaso = new RCDependencyBuilder()
                .category(maso).recipe(recipe1).build();
        RCDependency recipe2InMaso = new RCDependencyBuilder()
                .category(maso).recipe(recipe2).build();

        dependencyManager.createDependency(recipe1InMaso);
        dependencyManager.createDependency(recipe2InMaso);
        assertThat(dependencyManager.findRecipesInCategory(omacky)).isEmpty();
        List<Recipe> nok = dependencyManager.findRecipesInCategory(maso);
        assertThat(dependencyManager.findRecipesInCategory(maso))
                .usingFieldByFieldElementComparator().containsOnly(recipe1, recipe2);
    }

    @Test
    public void testFindCategoriesForRecipe() {
        prepareTestData();
        assertThat(dependencyManager.findCategoriesForRecipe(recipe1)).isEmpty();
        assertThat(dependencyManager.findCategoriesForRecipe(recipe2)).isEmpty();

        RCDependency recipeInMasoCategory = new RCDependencyBuilder()
                .category(maso).recipe(recipe1).build();

        RCDependency recipeInOmackyCategory = new RCDependencyBuilder()
                .category(omacky).recipe(recipe1).build();

        dependencyManager.createDependency(recipeInMasoCategory);
        dependencyManager.createDependency(recipeInOmackyCategory);

        assertThat(dependencyManager.findCategoriesForRecipe(recipe1)).containsOnly(maso, omacky);
        assertThat(dependencyManager.findCategoriesForRecipe(recipe2)).isEmpty();
    }

    @Test
    public void findAllDependencies() throws Exception {
        prepareTestData();
        assertThat(dependencyManager.findAllDependencies()).isEmpty();

        RCDependency dependency1 = new RCDependencyBuilder().category(maso).recipe(recipe1).build();
        RCDependency dependency2 = new RCDependencyBuilder().category(omacky).recipe(recipe2).build();

        dependencyManager.createDependency(dependency1);
        dependencyManager.createDependency(dependency2);

        List<RCDependency> ok = dependencyManager.findAllDependencies();

        assertThat(ok.get(0)).isEqualToComparingFieldByFieldRecursively(dependency1);
        assertThat(ok.get(1)).isEqualToComparingFieldByFieldRecursively(dependency2);
    }
}