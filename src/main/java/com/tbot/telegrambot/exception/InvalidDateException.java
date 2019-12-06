package com.tbot.telegrambot.exception;

/**
 * @author Vladislav Marchenko
 */
public class InvalidDateException extends RuntimeException {
    public InvalidDateException(String method, String taskID) {
        super("Error occured in method - [" + method + "] while working with task number - [" + taskID + "]. Error type - [Invalid date format]");
    }
}
