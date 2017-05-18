package cz.muni.fi.pv168.recipeevidence.impl.gui;

import com.sun.org.apache.regexp.internal.RE;
import com.sun.xml.internal.ws.policy.privateutil.LocalizationMessages;
import cz.muni.fi.pv168.recipeevidence.RecipeManager;
import cz.muni.fi.pv168.recipeevidence.impl.Recipe;
import cz.muni.fi.pv168.recipeevidence.impl.RecipeManagerImpl;

import javax.activation.DataSource;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.time.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Created by tom on 18.5.17.
 */
public class RecipesTableModel extends AbstractTableModel {

    private final List<Recipe> recipes;
    private final DataSource dataSource;
    private final RecipeManager recipeManager;
    private final JTable table;

    private final static ZonedDateTime NOW
            = LocalDateTime.now().atZone(ZoneId.of("UTC"));

    private static Clock prepareClockMock(ZonedDateTime now){
        return Clock.fixed(now.toInstant(), now.getZone());
    }

    public RecipesTableModel(JTable table) {
        this.dataSource = dataSource; //TODO
        this.recipeManager = new RecipeManagerImpl(prepareClockMock(NOW));
        this.recipes = recipeManager.findAllRecipes();
        this.table = table;
    }

    public List<Recipe> getRecipes() {
        return recipes;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public void setRecipes(List<Recipe> recipes) {
        clearRecipeTable();
        synchronized (recipes) {
            recipes.stream().forEach((recipe -> {
                this.recipes.add(recipe);
            }));
        }
    }

    public void clearRecipeTable() {
        this.recipes.clear();
        this.fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return recipes.size();
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Recipe recipe = recipes.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return recipe.getId();
            case 1:
                return recipe.getDate();
            case 2:
                return recipe.getName();
            case 3:
                return recipe.getIngredients();
            case 4:
                return recipe.getProcedure();
            default:
                throw new IllegalArgumentException("columnIndex");
        }
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "ID";
            case 1:
                return LocalizationWizard.getString("Date");
            case 2:
                return LocalizationWizard.getString("Name");
            case 3:
                return LocalizationWizard.getString("Ingredients");
            case 4:
                return LocalizationWizard.getString("Recept");
            default:
                throw new IllegalArgumentException("columnIndex");
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return Long.class;
            case 1:
                return LocalDate.class;
            case 2:
                return String.class;
            case 3:
                return Set.class;
            case 4:
                return  String.class;
            default:
                throw new IllegalArgumentException("columnIndex");
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                return false;
            case 1:
                return true;
            case 2:
                return true;
            case 3:
                return true;
            case 4:
                return true;
            default:
                throw new IllegalArgumentException("columnIndex");
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        Recipe recipe = recipes.get(rowIndex);
        switch (columnIndex) {
            case 0:
                recipe.setId((Long) aValue);
                break;
            case 1:
                recipe.setDate((LocalDate) aValue);
                break;
            case 2:
                recipe.setName((String) aValue);
                break;
            case 3:
                recipe.setIngredients((Set<String>) aValue);
            case 4:
                recipe.setProcedure((String) aValue);
            default:
                throw new IllegalArgumentException("columnIndex");
        }
        updateRecipe(recipe, rowIndex, columnIndex);
    }

    public void updateRecipe(Recipe recipe, int rowIndex, int columnIndex) {
        UpdateRecipeWorker updateRecipeWorker = new UpdateRecipeWorker(recipe, rowIndex, columnIndex, RecipesTableModel.this);
        updateRecipeWorker.execute();
    }

    private class UpdateRecipeWorker extends SwingWorker<Recipe, Void> {
        private final Recipe recipe;
        private int rowIndex;
        private int columnIndex;

        public UpdateRecipeWorker(Recipe recipe, int rowIndex, int columnIndex, RecipesTableModel tableModel) {
            this.recipe = recipe;
            this.rowIndex = rowIndex;
            this.columnIndex = columnIndex;
        }

        @Override
        protected Recipe doInBackground() throws Exception {
            Recipe oldRecipe = recipeManager.findRecipeById(recipe.getId());
            RecipesTableModel.this.recipeManager.updateRecipe(recipe);
            //TODO logger
            return recipe;
        }

        protected void done() {
            try {
                get();
            } catch (InterruptedException e) {
                //TODO logger
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    public void createRecipe(String name, )
}
