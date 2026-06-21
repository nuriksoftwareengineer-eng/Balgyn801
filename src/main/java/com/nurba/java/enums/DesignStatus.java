package com.nurba.java.enums;

/**
 * Lifecycle state of a Design.
 *
 * DRAFT     – just created; publication requirements not yet met
 * READY     – all requirements satisfied; awaiting admin explicit publish
 * PUBLISHED – visible in public catalog
 * ARCHIVED  – removed from catalog but preserved for history
 */
public enum DesignStatus {
    DRAFT,
    READY,
    PUBLISHED,
    ARCHIVED
}
