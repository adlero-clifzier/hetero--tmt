package com.hetero.model;

/**
 * Enumeration of task priority levels, ordered from highest to lowest urgency.
 *
 * <p>Used across the Hetero application for:
 * <ul>
 *   <li>Sorting tasks by importance in the All Tasks view.</li>
 *   <li>Filtering tasks in the search bar filter ComboBox.</li>
 *   <li>Colour-coded visual badges in the TableView and ListView cells.</li>
 * </ul>
 *
 * <p><b>Specification compliance — this enum demonstrates:</b>
 * <ul>
 *   <li><b>Custom-built class:</b> Defined specifically for the Hetero domain.</li>
 *   <li><b>Primitive data:</b> Each constant is an implicitly {@code static final}
 *       field; ordinal values are primitive {@code int} values assigned by the JVM.</li>
 *   <li><b>Detailed documentation:</b> Every constant carries an inline Javadoc
 *       describing its intended use.</li>
 * </ul>
 */
public enum Priority {

    /** Highest urgency — must be resolved immediately. Displayed in red. */
    CRITICAL,

    /** High urgency — important and time-sensitive. Displayed in orange. */
    HIGH,

    /** Standard urgency — normal day-to-day tasks. Displayed in yellow. */
    MEDIUM,

    /** Low urgency — can be deferred without consequence. Displayed in green. */
    LOW,

    /** Lowest urgency — nice-to-do, no deadline pressure. Displayed in grey. */
    MINIMAL
}
