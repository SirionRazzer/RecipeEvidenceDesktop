package cz.muni.fi.pv168.recipeevidence.impl;

/**
 * Exception to be thrown if a service error occured during runtime
 *
 * @author Tomas Soukal
 */
public class ServiceFailureException extends RuntimeException {

    public ServiceFailureException(String msg) {
        super(msg);
    }

    public ServiceFailureException(Throwable cause) {
        super(cause);
    }

    public ServiceFailureException(String message, Throwable cause) {
        super(message, cause);
    }

}
