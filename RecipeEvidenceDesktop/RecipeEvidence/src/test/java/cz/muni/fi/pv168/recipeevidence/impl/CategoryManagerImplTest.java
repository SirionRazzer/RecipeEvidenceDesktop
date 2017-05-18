package cz.muni.fi.pv168.recipeevidence.impl;

import cz.muni.fi.pv168.recipeevidence.CategoryManager;
import cz.muni.fi.pv168.recipeevidence.common.DBUtils;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.junit.*;
import org.junit.rules.ExpectedException;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author Tomas Soukal
 */
public class CategoryManagerImplTest {

    private CategoryManagerImpl manager;
    private DataSource ds;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static DataSource prepareDataSource() throws SQLException {
        EmbeddedDataSource ds = new EmbeddedDataSource();
        ds.setDatabaseName("memory:categorymgr-test");
        ds.setCreateDatabase("create");
        return ds;
    }

    @Before
    public void setUp() throws SQLException {
        ds = prepareDataSource();
        if (ds == null) {
            System.out.println("ds null");
        }
        DBUtils.executeSqlScript(ds, CategoryManager.class.getResource("createTables.sql"));
        manager = new CategoryManagerImpl();
        manager.setDataSource(ds);
    }

    @After
    public void tearDown() throws SQLException {
        DBUtils.executeSqlScript(ds, CategoryManager.class.getResource("dropTables.sql"));
    }

    //--------------------------------------------------------------------------
    // Preparing test data
    //--------------------------------------------------------------------------

    private Category sampleCategory1() {
        Category category = new Category();
        category.setCategoryName("BIO");
        return category;
    }

    private Category sampleCategory2() {
        Category category = new Category();
        category.setCategoryName("Jogurty");
        return category;
    }

    //--------------------------------------------------------------------------
    // Tests for RecipeManager.createRecipe(Recipe) operation
    //--------------------------------------------------------------------------

    @Test
    public void testCreateCategory() {
        Category category = sampleCategory1();
        manager.createCategory(category);

        Long categoryId = category.getId();
        Assert.assertNotNull(categoryId);

        assertThat(manager.findCategoryById(categoryId))
                .isNotSameAs(category)
                .isEqualToComparingFieldByField(category);
    }

    @Test
    public void createCategoryWithNullName() {
        Category category = sampleCategory1();
        category.setCategoryName(null);

        expectedException.expect(IllegalArgumentException.class);
        manager.createCategory(category);
    }

    @Test
    public void createCategoryWithExistingId() {
        Category category = sampleCategory1();
        category.setCategoryID(1L);

        expectedException.expect(IllegalEntityException.class);
        manager.createCategory(category);
    }

    @Test
    public void deleteCategory() {

        Category category1 = sampleCategory1();
        Category category2 = sampleCategory2();
        manager.createCategory(category1);
        manager.createCategory(category2);

        assertThat(manager.findCategoryById(category1.getId())).isNotNull();
        assertThat(manager.findCategoryById(category2.getId())).isNotNull();

        manager.deleteCategory(category1);

        assertThat(manager.findCategoryById(category2.getId())).isNotNull();
        assertThat(manager.findCategoryById(category1.getId())).isNull();
    }


    @Test(expected = IllegalArgumentException.class)
    public void deleteNullCategory() {
        manager.deleteCategory(null);
    }

    @Test
    public void deleteCategoryWithNullId() {
        Category category = sampleCategory1();
        category.setCategoryID(null);
        expectedException.expect(IllegalArgumentException.class);
        manager.deleteCategory(category);
    }

    @Test
    public void deleteCategoryWithNonExistingId() {
        Category category = sampleCategory1();
        category.setCategoryID(1L);
        expectedException.expect(IllegalArgumentException.class);
        manager.deleteCategory(category);
    }

    //--------------------------------------------------------------------------
    // Tests for RecipeManager.updateRecipe(Recipe) operation
    //--------------------------------------------------------------------------

    @FunctionalInterface
    private static interface Operation<T> {
        void callOn(T subjectOfOperation);
    }

    private void testUpdateCategory(Operation<Category> updateOperation) {
        Category categoryForUpdate = sampleCategory1();
        Category anotherCategory = sampleCategory2();
        manager.createCategory(categoryForUpdate);
        manager.createCategory(anotherCategory);


        updateOperation.callOn(categoryForUpdate);

        manager.updateCategory(categoryForUpdate);
        assertThat(manager.findCategoryById(categoryForUpdate.getId()))
                .isEqualToComparingFieldByField(categoryForUpdate);

        assertThat(manager.findCategoryById(anotherCategory.getId()))
                .isEqualToComparingFieldByField(anotherCategory);
    }

    @Test
    public void updateName(){
        testUpdateCategory((category) -> category.setCategoryName("New namef"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateNullCategory() {
        manager.updateCategory(null);
    }

    @Test
    public void updateCategoryWithNullId() {
        Category category= sampleCategory1();
        category.setCategoryID(null);
        expectedException.expect(IllegalEntityException.class);
        manager.updateCategory(category);
    }

    @Test
    public void updateNonExistingCategory() {
        Category category = sampleCategory1();
        category.setCategoryID(1L);
        expectedException.expect(IllegalEntityException.class);
        manager.updateCategory(category);
    }

    @Test
    public void updateCategoryWithNullName() {
        Category category = sampleCategory1();
        manager.createCategory(category);
        category.setCategoryName(null);

        expectedException.expect(IllegalArgumentException.class);
        manager.updateCategory(category);
    }

    //--------------------------------------------------------------------------
    // Tests for find operations
    //--------------------------------------------------------------------------

    @Test
    public void testFindAllCategories() throws Exception {
        assertThat(manager.findAllCategories()).isEmpty();

        Category category = sampleCategory1();
        Category category1 = sampleCategory2();

        manager.createCategory(category);
        manager.createCategory(category1);

        assertThat(manager.findAllCategories())
                .usingFieldByFieldElementComparator()
                .containsOnly(category, category1);
    }

    @Test
    public void testFindCategoryByName() throws Exception {
        assertThat(manager.findCategoryByName("Nic")).isNull();

        Category category = sampleCategory1();
        Category category1 = sampleCategory2();

        manager.createCategory(category);
        manager.createCategory(category1);

        assertThat(manager.findCategoryByName(category.getCategoryName())).isEqualToComparingOnlyGivenFields(category, "categoryName");
    }

    @Test
    public void testFindCategoryByNullName() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        manager.findCategoryByName(null);
    }

    @Test
    public void findCategoryById() throws Exception {
        Category category = sampleCategory1();

        manager.createCategory(category);
        assertThat(manager.findCategoryById(category.getId())).isEqualTo(category);
    }


    //--------------------------------------------------------------------------
    // Operations with DB
    //--------------------------------------------------------------------------

    @Test
    public void createCategoryWithSqlExceptionThrown() throws SQLException {
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

        Category category = sampleCategory1();

        // Try to call Manager.createBody(Body) method and expect that exception
        // will be thrown
        expectedException.expect(ServiceFailureException.class);
        manager.createCategory(category);
    }

    private void testExpectedServiceFailureException(Operation<CategoryManager> operation) throws SQLException {
        SQLException sqlException = new SQLException();
        DataSource failingDataSource = mock(DataSource.class);
        when(failingDataSource.getConnection()).thenThrow(sqlException);
        manager.setDataSource(failingDataSource);
        expectedException.expect(ServiceFailureException.class);
        operation.callOn(manager);
    }

    @Test
    public void updateCategoryWithSqlExceptionThrown() throws SQLException {
        Category category = sampleCategory1();
        manager.createCategory(category);
        testExpectedServiceFailureException((CategoryManager) -> CategoryManager.updateCategory(category));
    }

    @Test
    public void getCategoryWithSqlExceptionThrown() throws SQLException {
        Category category = sampleCategory1();
        manager.createCategory(category);
        testExpectedServiceFailureException((CategoryManager) -> CategoryManager.findCategoryById(category.getId()));
    }


    @Test
    public void deleteCategoryWithSqlExceptionThrown() throws SQLException {
        Category category = sampleCategory1();
        manager.createCategory(category);
        testExpectedServiceFailureException((CategoryManager) -> CategoryManager.deleteCategory(category));
    }

    @Test
    public void findAllCategoriesWithSqlExceptionThrown() throws SQLException {
        testExpectedServiceFailureException(CategoryManager::findAllCategories);
    }
}