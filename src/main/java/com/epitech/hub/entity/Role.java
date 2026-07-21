package com.epitech.hub.entity;

/**
 * Role d'un utilisateur <b>au sein d'un projet donne</b> (et non un role global).
 * Un meme utilisateur peut donc etre ADMIN sur un projet et VIEWER sur un autre.
 */
public enum Role {

    /** Gestion des membres et suppression du projet, en plus des droits MEMBER. */
    ADMIN,

    /** Creation et modification des taches, en plus des droits VIEWER. */
    MEMBER,

    /** Lecture seule. */
    VIEWER
}
