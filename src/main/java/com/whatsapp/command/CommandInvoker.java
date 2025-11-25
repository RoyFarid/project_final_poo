package com.whatsapp.command;

import java.util.Stack;

public class CommandInvoker {
    private final Stack<Command> history;

    public CommandInvoker() {
        this.history = new Stack<>();
    }

    public void executeCommand(Command command) {
        command.execute();
        history.push(command);
    }

    public void undoLastCommand() {
        if (!history.isEmpty()) {
            Command command = history.pop();
            command.undo();
        }
    }

    public void clearHistory() {
        history.clear();
    }
}

