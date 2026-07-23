package com.junsong.finance.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

class FinProductControllerRouteTest
{
    @Test
    void detailAndDeleteRoutesOnlyAcceptNumericIds()
    {
        Method detail = getMethod("getInfo");
        Method remove = getMethod("remove");

        assertEquals("/{productId:\\d+}", detail.getAnnotation(GetMapping.class).value()[0]);
        assertEquals("/{productIds:\\d+(,\\d+)*}", remove.getAnnotation(DeleteMapping.class).value()[0]);
    }

    private Method getMethod(String name)
    {
        return java.util.Arrays.stream(FinProductController.class.getDeclaredMethods())
            .filter(method -> method.getName().equals(name))
            .findFirst()
            .orElseThrow();
    }
}
