package com.hetero.model;

/**
 * Priority is an enum that represents how urgent a task is.
 *
 * An enum is a special type that only holds a fixed set of constant values.
 * We use it here so that priority can only ever be one of these five levels,
 * which prevents mistakes like typing "Hihg" instead of "HIGH".
 *
 * The values are listed from most urgent to least urgent.
 */
public enum Priority {

    /** Drop everything — needs to be done right now. */
    CRITICAL,

    /** Very important — should be done soon. */
    HIGH,

    /** Normal priority — regular day-to-day task. */
    MEDIUM,

    /** Not urgent — can be done whenever there is time. */
    LOW,

    /** Almost optional — nice to do but no pressure. */
    MINIMAL
}
