package br.com.aistech.bluetoothlegatt.exceptions;

/**
 * Created by jonathan on 01/02/17.
 */

public class BTLAGException extends Exception {

    public BTLAGException() {
        super();
    }

    public BTLAGException(String message) {
        super(message);
    }

    public BTLAGException(String message, Throwable cause) {
        super(message, cause);
    }

    public BTLAGException(Throwable cause) {
        super(cause);
    }
}
