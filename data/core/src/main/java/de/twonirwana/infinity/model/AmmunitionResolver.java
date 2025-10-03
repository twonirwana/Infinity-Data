package de.twonirwana.infinity.model;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.databind.deser.impl.PropertyBasedObjectIdGenerator;

import java.util.HashMap;
import java.util.Map;


public class AmmunitionResolver implements ObjectIdResolver {
    private final Map<ObjectIdGenerator.IdKey, Ammunition> _items = new HashMap<>();

    public AmmunitionResolver() {
        Ammunition blank = new Ammunition();
        blank.setId(0);
        blank.setName("-");
        _items.put(new ObjectIdGenerator.IdKey(PropertyBasedObjectIdGenerator.class,
                Object.class,
                0), blank);
        //_items.put(, blank);
    }

    @Override
    public void bindItem(ObjectIdGenerator.IdKey id, Object pojo) {
        if (!_items.containsKey(id)) _items.put(id, (Ammunition) pojo);
    }


    @Override
    public Ammunition resolveId(ObjectIdGenerator.IdKey id) {
        return _items.get(id);
    }


    @Override
    public ObjectIdResolver newForDeserialization(Object context) {
        return this;
    }

    @Override
    public boolean canUseFor(ObjectIdResolver resolverType) {
        return resolverType.getClass() == getClass();
    }
}