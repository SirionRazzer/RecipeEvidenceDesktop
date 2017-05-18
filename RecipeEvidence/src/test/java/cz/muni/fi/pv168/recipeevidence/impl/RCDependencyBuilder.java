package cz.muni.fi.pv168.recipeevidence.impl;

/**
 * TODO: create javadoc you lazy bitch
 *
 * @author Tomas Soukal
 */
public class RCDependencyBuilder {

    private Long id;
    private Recipe recipe;
    private Category category;

    public RCDependencyBuilder id(Long id) {
        this.id = id;
        return this;
    }

    public RCDependencyBuilder recipe(Recipe recipe) {
        this.recipe = recipe;
        return this;
    }

    public RCDependencyBuilder category(Category category) {
        this.category = category;
        return this;
    }

    public RCDependency build() {
        RCDependency dependency = new RCDependency();
        dependency.setId(id);
        dependency.setCategory(category);
        dependency.setRecipe(recipe);
        return dependency;
    }
}
