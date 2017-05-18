package cz.muni.fi.pv168.recipeevidence.impl;

/**
 * Class represents relation between Category and Recipe.
 * Category can contain more than one Recipe.
 * Recipe can be assigned to more than one Category.
 * @author Petra Halova
 */
public class RCDependency {
    private Long id;
    private Recipe recipe;
    private Category category;

    public RCDependency() {
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public Category getCategory() {
        return category;
    }
}