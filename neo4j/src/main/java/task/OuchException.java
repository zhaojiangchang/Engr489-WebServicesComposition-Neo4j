package task;

public class OuchException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public OuchException(String message){
		super(message);
	}
	 public OuchException(String message, Throwable throwable) {
	        super(message, throwable);
	    }
}
