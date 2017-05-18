package cz.muni.fi.pv168.recipeevidence;

import cz.muni.fi.pv168.recipeevidence.impl.Recipe;

import java.util.List;
import java.util.Set;

/**
 * Interface for Recipe objects
 * @author Petra Halova
 */
public interface RecipeManager {

    /**
     * Stores new recipe into database
     * @param recipe is new recipe
     */
    void createRecipe(Recipe recipe);

    /**
     * Updates recipe
     * @param recipe is a recipe for update
     */
    void updateRecipe(Recipe recipe);

    /**
     * Delete given recipe from the database
     * @param recipe is recipe to be deleted
     */
    void deleteRecipe(Recipe recipe);

    /**
     * Get all recipes
     * @return all recipes currently in database
     */
    List<Recipe> findAllRecipes();

    /**
     * Get recipe by the given ID
     * @param id is ID of the wanted recipe
     * @return recipe with given id or null if doesn't exist
     */
    Recipe findRecipeById(Long id);

    /**
     * Get all recipes with the given nam
     * @param name is the name of recipe(s)
     * @return all recipes with same names or empty list if there is none
     */
    List<Recipe> findRecipeByName(String name);

    /**
     * @param ingredients is list of desired ingredients
     * @return list of Recipes containing all given ingredients
     */
    List<Recipe> findRecipeByIngredients(Set<String> ingredients);

}