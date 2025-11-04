package com.example.syzygy_eventapp;

/**
 * The possible authorization roles a user can have.
 * <p>
 *     Each user may have one role, which implicity contains.
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
    ENTRANT(0),
    ORGANIZER(1),
    ADMIN(2);

    private Integer authority;

    private Role(int authority) {
        this.authority = authority;
    }

    public boolean hasHigherAuthority(Role other) {
        return this.authority > other.authority;
    }

    public Role promote() {
        if (this == ENTRANT) {
            return ORGANIZER;
        } else {
            return ADMIN;
        }
    }

    public Role demote() {
        if (this == ADMIN) {
            return ORGANIZER;
        } else {
            return ENTRANT;
        }
    }
}
