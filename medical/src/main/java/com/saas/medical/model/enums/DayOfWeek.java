package com.saas.medical.model.enums;

import java.time.LocalDate;

/**
 * Enum para representar los días de la semana
 * Compatible con java.time.DayOfWeek pero adaptado para uso en BD
 */
public enum DayOfWeek {
    MONDAY(1, "Lunes"),
    TUESDAY(2, "Martes"),
    WEDNESDAY(3, "Miércoles"),
    THURSDAY(4, "Jueves"),
    FRIDAY(5, "Viernes"),
    SATURDAY(6, "Sábado"),
    SUNDAY(7, "Domingo");

    private final int value;
    private final String displayName;

    DayOfWeek(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public int getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Convierte desde integer (1-7) a DayOfWeek
     */
    public static DayOfWeek fromValue(int value) {
        for (DayOfWeek day : values()) {
            if (day.value == value) {
                return day;
            }
        }
        throw new IllegalArgumentException("Valor inválido para DayOfWeek: " + value);
    }

    /**
     * Convierte desde java.time.DayOfWeek
     */
    public static DayOfWeek fromJavaTime(java.time.DayOfWeek dayOfWeek) {
        return fromValue(dayOfWeek.getValue());
    }

    /**
     * Convierte desde LocalDate obteniendo el día de la semana
     */
    public static DayOfWeek fromLocalDate(LocalDate date) {
        return fromJavaTime(date.getDayOfWeek());
    }

    /**
     * Convierte a java.time.DayOfWeek
     */
    public java.time.DayOfWeek toJavaTime() {
        return java.time.DayOfWeek.of(this.value);
    }
}