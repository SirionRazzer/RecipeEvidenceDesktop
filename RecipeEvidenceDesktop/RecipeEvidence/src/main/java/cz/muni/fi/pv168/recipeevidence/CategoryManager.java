package cz.muni.fi.pv168.recipeevidence;

import cz.muni.fi.pv168.recipeevidence.impl.Category;
import cz.muni.fi.pv168.recipeevidence.impl.ServiceFailureException;

import java.util.List;

/**
 * Represents functions for handling categories
 *
 * @author Tomas Soukal
 */
public interface CategoryManager {

    /**
     * Stores new category into database
     * @param category
     */
    void createCategory(Category category) throws ServiceFailureException;

    /**
     * Updates category
     * @param category
     * @throws ServiceFailureException
     */
    void updateCategory(Category category) throws ServiceFailureException;

    /**
     * Deletes category
     * @param category
     * @throws ServiceFailureException
     */
    void deleteCategory(Category category) throws ServiceFailureException;

    /**
     * Finds category by the given id
     * @param id is an id of wanted category
     * @return category or null
     * @throws ServiceFailureException
     */
    Category findCategoryById(Long id) throws ServiceFailureException;

    /**
     * Finds category by the given name
     * @param name is a name of wanted category
     * @return category or null
     * @throws ServiceFailureException
     */
    Category findCategoryByName(String name) throws ServiceFailureException;

    /**
     * Return all categories
     * @return all categories or list of nulls
     * @throws ServiceFailureException
     */
    List<Category> findAllCategories() throws ServiceFailureException;
}
