package cz.muni.fi.pv168.recipeevidence.impl;

import java.time.LocalDate;
import java.util.Set;

/**
 * Recipe contains ingredients and procedure for preparing meal specified by its name.
 * Each Recipe belongs to at least one Category.
 * @author Petra Halova
 */
public class Recipe {

    private Long id;
    private String name;
    private Set<String> ingredients;
    private String procedure;
    private LocalDate date;

    public Recipe() {
    }

    //mazani receptu
    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIngredients(Set<String> ingredients) {
        this.ingredients = ingredients;
    }

    public void setProcedure(String procedure) {
        this.procedure = procedure;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<String> getIngredients() {
        return ingredients;
    }

    public String getProcedure() {
        return procedure;
    }

    public LocalDate getDate() {
        return date;
    }
}