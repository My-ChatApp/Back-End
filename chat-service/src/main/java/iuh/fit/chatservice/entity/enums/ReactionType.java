package iuh.fit.chatservice.entity.enums;

import java.util.Locale;

public enum ReactionType {
    LIKE,
    LOVE,
    HAHA,
    WOW,
    SAD,
    ANGRY;

    public static ReactionType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("reactionType is required");
        }
        try {
            return ReactionType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid reactionType: " + value);
        }
    }
}
