package it.northleap.backend.exceptions;

// 400 generico, message-carrying, riusabile fuori dal contesto specifico della validazione Record
// (es. ObjectsService: chiave già esistente, object type di sistema non eliminabile)
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
