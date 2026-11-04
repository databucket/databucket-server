package pl.databucket.server.dto;

/** A project marked as public: shown to new users, who can ask for access to it when they register. */
public record PublicProjectDto(int id, String name, String description) {
}
