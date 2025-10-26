package com.example.syzygy_eventapp;

/**
 * The possible authorization roles a user can have.
 * <p>
 *     Each user may have one or more roles at the same time.
 *     Roles determine the features, navigation, and privileges available to that user.
 *     The users current active role must be one of the roles assigned to them.
 * </p>
 *
 * <ul>
 *     <li>ENTRANT - A normal user who can browse and join event waiting lists.</li>
 *     <li>ORGANIZER - A user who can create, edit, manage events and waiting lists, and send notifications.</li>
 *     <li>ADMIN - A privileged user who can manage all parts of the system, including removing events, users, and images.</li>
 * </ul>
 */
public enum Role {
    ENTRANT,
    ORGANIZER,
    ADMIN
}
