package com.huadows.fastapp.client;

import androidx.annotation.NonNull;

public interface ApiCallback<T> {
    void onSuccess(T result);
    void onFailure(@NonNull Exception e);
}