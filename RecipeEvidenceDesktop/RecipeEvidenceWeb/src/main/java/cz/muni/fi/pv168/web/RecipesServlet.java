package cz.muni.fi.pv168.web;

import cz.muni.fi.pv168.recipeevidence.RecipeManager;
import cz.muni.fi.pv168.recipeevidence.impl.Recipe;
import cz.muni.fi.pv168.recipeevidence.impl.RecipeManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Servlet for managing recipes.
 */
@WebServlet(RecipesServlet.URL_MAPPING + "/*")
public class RecipesServlet extends HttpServlet {

    private static final String LIST_JSP = "/list.jsp";
    public static final String URL_MAPPING = "/recipes";

    private final static Logger log = LoggerFactory.getLogger(RecipesServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.debug("GET ...");
        showRecipesList(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //support non-ASCII characters in form
        request.setCharacterEncoding("utf-8");
        //action specified by pathInfo
        String action = request.getPathInfo();
        log.debug("POST ... {}", action);
        switch (action) {
            case "/add":
                //getting POST parameters from form
                String name = request.getParameter("name");
                String procedure = request.getParameter("procedure");
                String ingredience = request.getParameter("ingredience");

                //form data validity check
                if (name == null || name.length() == 0 || procedure == null || procedure.length() == 0) {
                    request.setAttribute("chyba", "Je nutné vyplnit všechny hodnoty !");
                    log.debug("form data invalid");
                    showRecipesList(request, response);
                    return;
                }
                //form data processing - storing to database
                try {
                    Recipe recipe = new Recipe();
                    recipe.setName(name);
                    recipe.setProcedure(procedure);
                    Set<String> ingr = new HashSet<String>();
                    String[] ingrs = ingredience.split(",");
                    for (int i = 0; i < ingrs.length; i++) {
                        ingr.add(ingrs[i]);
                    }
                    recipe.setIngredients(ingr);
                    getRecipeManager().createRecipe(recipe);
                    //redirect-after-POST protects from multiple submission
                    log.debug("redirecting after POST");
                    response.sendRedirect(request.getContextPath() + URL_MAPPING);
                    return;
                } catch (Exception e) {
                    log.error("Cannot add recipe", e);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                    return;
                }
            case "/delete":
                try {
                    Long id = Long.valueOf(request.getParameter("id"));
                    Recipe r = getRecipeManager().findRecipeById(id);
                    getRecipeManager().deleteRecipe(r);
                    log.debug("redirecting after POST");
                    response.sendRedirect(request.getContextPath() + URL_MAPPING);
                    return;
                } catch (Exception e) {
                    log.error("Cannot delete recipe", e);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                    return;
                }
            default:
                log.error("Unknown action " + action);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unknown action " + action);
        }
    }

    /**
     * Gets RecipeManager from ServletContext, where it was stored by {@link StartListener}.
     *
     * @return RecipeManager instance
     */
    private RecipeManager getRecipeManager() {
        return (RecipeManager) getServletContext().getAttribute("recipeManager");
    }

    /**
     * Stores the list of recipes to request attribute "recipes" and forwards to the JSP to display it.
     */
    private void showRecipesList(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            log.debug("showing table of recipes");
            request.setAttribute("recipes", getRecipeManager().findAllRecipes());
            request.getRequestDispatcher(LIST_JSP).forward(request, response);
        } catch (Exception e) {
            log.error("Cannot show recipes", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

}
