/*
 * TaskTransactionMessage.java  
 *
 * Creator:
 * 10/03/2015 15:12 Sippel
 *
 * Maintainer:
 * 10/03/2015 15:12 Sippel
 *
 * Last Modification:
 * $Id: $
 *
 * Copyright (c) 2003 ABACUS Research AG, All Rights Reserved
 */
package ch.abacus.abaconnecttools;

public class TaskTransactionMessage {

    int mTaskIndex;  // 1-based
    int mTransactionIndex;  // Zero-based

    String mMessage;

    public TaskTransactionMessage(int taskIndex, int transactionIndex, String message) {
        mMessage = message;
        mTaskIndex = taskIndex;
        mTransactionIndex = transactionIndex;
    }

    @Override
    public boolean equals(Object obj) {
        if ( obj instanceof TaskTransactionMessage) {
            return ((TaskTransactionMessage)obj).mTaskIndex == mTaskIndex && ((TaskTransactionMessage)obj).mTransactionIndex == mTransactionIndex;
        }
        return false;
    }
}
