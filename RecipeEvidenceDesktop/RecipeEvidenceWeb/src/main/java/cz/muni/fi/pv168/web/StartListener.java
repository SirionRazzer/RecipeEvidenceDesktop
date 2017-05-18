package cz.muni.fi.pv168.web;

import cz.muni.fi.pv168.recipeevidence.CategoryManager;
import cz.muni.fi.pv168.recipeevidence.RecipeManager;
import cz.muni.fi.pv168.recipeevidence.common.DBUtils;
import cz.muni.fi.pv168.recipeevidence.impl.Recipe;
import cz.muni.fi.pv168.recipeevidence.impl.RecipeManagerImpl;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;
import javax.xml.crypto.Data;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by tom on 27.4.17.
 */
@WebListener
public class StartListener implements ServletContextListener {

    private final static Logger log = LoggerFactory.getLogger(StartListener.class);
    private DataSource ds;
    private RecipeManagerImpl rm;
    private final static ZonedDateTime NOW
            = LocalDateTime.now().atZone(ZoneId.of("UTC"));

    private static Clock prepareClockMock(ZonedDateTime now){
        return Clock.fixed(now.toInstant(), now.getZone());
    }

    private Recipe sampleRecipe1(){
        Recipe recipe = new Recipe();
        recipe.setName("Svíčková");
        Set<String> ingredients = new HashSet<>();
        ingredients.add("maso");
        recipe.setIngredients(ingredients);
        recipe.setProcedure("Uděláme svíčkovou");
        recipe.setDate(NOW.toLocalDate());
        return recipe;
    }

    public void setUp() throws SQLException {
        ds = prepareDataSource();
        if(ds == null){
            System.out.println("ds null");
        }
        DBUtils.executeSqlScript(ds, RecipeManager.class.getResource("createTables.sql"));
        rm = new RecipeManagerImpl(prepareClockMock(NOW));
        rm.setDataSource(ds);

        //add sample recipe
        Recipe recipe = sampleRecipe1();
        rm.createRecipe(recipe);
    }

    private static DataSource prepareDataSource() throws SQLException {
        EmbeddedDataSource ds = new EmbeddedDataSource();
        ds.setDatabaseName("memory:recipemgr");
        ds.setCreateDatabase("create");
        return ds;
    }

    @Override
    public void contextInitialized(ServletContextEvent ev) {
        log.info("webová aplikace inicializována");
        ServletContext servletContext = ev.getServletContext();
        //DataSource dataSource = Main.createMemoryDatabase();
        try {
            setUp();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        servletContext.setAttribute("recipeManager", rm);
        //servletContext.setAttribute("bookManager", new BookManagerImpl(dataSource));
        log.info("vytvořeny manažery a uloženy do atributů servletContextu");
    }

    @Override
    public void contextDestroyed(ServletContextEvent ev) {
        log.info("aplikace končí");
        try {
            DBUtils.executeSqlScript(ds, CategoryManager.class.getResource("dropTables.sql"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
