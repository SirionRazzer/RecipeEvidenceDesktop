package cz.muni.fi.pv168.recipeevidence.impl;

/**
 * Category represents
 *
 * @author Tomas Soukal
 */
public class Category {
    private Long categoryID;
    private String categoryName;

    /**
     * Constructor creates category with the given categoryID and categoryName
     * @param categoryID is the unique number
     * @param categoryName is the name of this category, i.e. "Diet Recipes"
     */
    public Category(Long categoryID, String categoryName) {
        this.categoryID = categoryID;
        this.categoryName = categoryName;
    }

    public Category() {
    }

    public Long getId() {
        return categoryID;
    }

    public void setCategoryID(Long categoryID) {
        this.categoryID = categoryID;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Category category = (Category) o;

        if (categoryID != null ? !categoryID.equals(category.categoryID) : category.categoryID != null) return false;
        return categoryName != null ? categoryName.equals(category.categoryName) : category.categoryName == null;

    }

    @Override
    public int hashCode() {
        int result = categoryID != null ? categoryID.hashCode() : 0;
        result = 31 * result + (categoryName != null ? categoryName.hashCode() : 0);
        return result;
    }
}
