package com.elvarg.game.entity.impl.player.persistence.jsonfile;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Custom Gson TypeAdapter for Optional fields.
 * This is required because Gson cannot serialize Optional fields in Java 9+ due to module restrictions.
 */
public class OptionalTypeAdapter<T> extends TypeAdapter<Optional<T>> {

    private final Gson gson;
    private final Type innerType;

    public OptionalTypeAdapter(Gson gson, Type innerType) {
        this.gson = gson;
        this.innerType = innerType;
    }

    @Override
    public void write(JsonWriter out, Optional<T> value) throws IOException {
        if (value == null || !value.isPresent()) {
            out.nullValue();
        } else {
            TypeAdapter<T> adapter = (TypeAdapter<T>) gson.getAdapter(TypeToken.get(innerType));
            adapter.write(out, value.get());
        }
    }

    @Override
    public Optional<T> read(JsonReader in) throws IOException {
        JsonElement element = JsonParser.parseReader(in);
        if (element.isJsonNull()) {
            return Optional.empty();
        }
        TypeAdapter<T> adapter = (TypeAdapter<T>) gson.getAdapter(TypeToken.get(innerType));
        T value = adapter.fromJsonTree(element);
        return Optional.ofNullable(value);
    }

    public static class Factory implements TypeAdapterFactory {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (type.getRawType() != Optional.class) {
                return null;
            }

            Type innerType = ((ParameterizedType) type.getType()).getActualTypeArguments()[0];
            return (TypeAdapter<T>) new OptionalTypeAdapter<>(gson, innerType);
        }
    }
}
