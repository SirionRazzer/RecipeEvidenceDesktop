package cz.muni.fi.pv168.recipeevidence.impl.gui;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Created by tom on 18.5.17.
 */
//TODO viz projekt na githubu
public class LocalizationWizard {
    private static final String DEFAULT_SETTINGS = "default_locales";

    public static String getString(String key) {
        try{
            return ResourceBundle.getBundle(DEFAULT_SETTINGS+"_"+ Locale.getDefault().toString()).getString(key);
        } catch(Exception ex){
            return ResourceBundle.getBundle(DEFAULT_SETTINGS).getString(key);
        }
    }
}
