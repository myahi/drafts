package fr.lbp.lib.ems;

public class EMSException extends RuntimeException {
    
    public EMSException(String message) {
        super(message);
    }
    
    public EMSException(String message, Throwable cause) {
        super(message, cause);
    }
}
