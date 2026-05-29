package iuh.fit.userservice.service;

import iuh.fit.userservice.entity.Gender;
import iuh.fit.userservice.entity.User;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

final class ProfileFieldsSupport {

    private ProfileFieldsSupport() {
    }

    static void applyPhone(User user, String phone) {
        if (phone == null) {
            return;
        }
        String trimmed = phone.trim();
        user.setPhone(trimmed.isEmpty() ? null : trimmed);
    }

    static void applyDateOfBirth(User user, String dateOfBirth) {
        if (dateOfBirth == null) {
            return;
        }
        String trimmed = dateOfBirth.trim();
        if (trimmed.isEmpty()) {
            user.setDateOfBirth(null);
            return;
        }
        try {
            LocalDate parsed = LocalDate.parse(trimmed);
            if (parsed.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Date of birth cannot be in the future");
            }
            user.setDateOfBirth(parsed);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date of birth format, use yyyy-MM-dd");
        }
    }

    static void applyGender(User user, String gender) {
        if (gender == null) {
            return;
        }
        String trimmed = gender.trim();
        if (trimmed.isEmpty()) {
            user.setGender(null);
            return;
        }
        try {
            user.setGender(Gender.valueOf(trimmed.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid gender, use MALE, FEMALE, or OTHER");
        }
    }
}
