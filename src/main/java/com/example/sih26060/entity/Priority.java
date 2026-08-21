package com.example.sih26060.entity;

/**
 * Declaration order is the sync priority order: MEDICAL first, ROUTINE last.
 */
public enum Priority {
    MEDICAL,
    EQUIPMENT,
    SUPPLY,
    ROUTINE
}
