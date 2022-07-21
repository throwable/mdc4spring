package com.github.throwable.mdc4spring.spring.spel;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.lang.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

class PrivateFieldPropertyAccessor implements PropertyAccessor {
    private final static ConcurrentHashMap<String, Accessor> resolvedAccessors = new ConcurrentHashMap<>();

    private final Class<?> clazz;

    interface Accessor {
        Object getValue(Object target) throws AccessException;
    }

    PrivateFieldPropertyAccessor(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class[] {clazz};
    }

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
        if (target == null) return false;
        resolveAccessor(target.getClass(), name);
        return true;
    }

    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
        if (target == null)
            throw new AccessException("Unable to read a property of null target");
        return new TypedValue(resolveAccessor(target.getClass(), name).getValue(target));
    }

    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) {
        return false;
    }

    @Override
    public void write(EvaluationContext context, Object target, String name, Object newValue) {
    }

    @NonNull
    private static Accessor resolveAccessor(Class<?> clazz, String name) throws AccessException {
        String accessorId = clazz.getName() + "/" + name;
        Accessor accessor = resolvedAccessors.get(accessorId);

        if (accessor == null) {
            for (Class<?> aClass = clazz; !Object.class.equals(aClass); aClass = aClass.getSuperclass()) {
                // Try to find accessor field
                try {
                    Field propertyField = aClass.getDeclaredField(name);
                    propertyField.setAccessible(true);
                    accessor = target -> {
                        try {
                            return propertyField.get(target);
                        } catch (IllegalAccessException ex) {
                            throw new AccessException(ex.getMessage());
                        }
                    };
                    break;
                } catch (NoSuchFieldException ignore) {
                }

                // Try to find accessor method
                String nameCapitalized = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                Method accessorMethod = null;
                try {
                    accessorMethod = aClass.getDeclaredMethod("get" + nameCapitalized);
                    accessorMethod.setAccessible(true);
                } catch (NoSuchMethodException ignore) {
                    try {
                        accessorMethod = aClass.getDeclaredMethod("is" + nameCapitalized);
                        if (!Boolean.class.isAssignableFrom(accessorMethod.getReturnType())) {
                            accessorMethod = null;
                        } else {
                            accessorMethod.setAccessible(true);
                        }
                    } catch (NoSuchMethodException ignore1) {
                    }
                }

                if (accessorMethod == null)
                    continue;

                Method finalAccessorMethod = accessorMethod;
                accessor = target -> {
                    try {
                        return finalAccessorMethod.invoke(target);
                    } catch (InvocationTargetException | IllegalAccessException ex) {
                        throw new AccessException(ex.getMessage());
                    }
                };
            }
        }
        else
            return accessor;

        if (accessor == null)
            throw new AccessException("Property accessor or field was not found for property '" +
                    name + " in class " + clazz.getName());

        final Accessor accessorUpdated = resolvedAccessors.putIfAbsent(accessorId, accessor);
        return accessorUpdated != null ? accessorUpdated : accessor;
    }
}
