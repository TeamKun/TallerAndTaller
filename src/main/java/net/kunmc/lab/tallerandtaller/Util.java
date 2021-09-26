package net.kunmc.lab.tallerandtaller;

import java.lang.reflect.Field;

public class Util {
    public static Field getFieldFromClass(Class clazz, String fieldName) throws NoSuchFieldException {
        Field field = null;
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }

        if (field == null) {
            throw new NoSuchFieldException();
        }
        return field;
    }
}
