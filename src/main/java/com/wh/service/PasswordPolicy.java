
package com.wh.service;

/**
 * Единые правила проверки пароля.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 64;

    private PasswordPolicy() {
        // Запрещаем создание экземпляров класса.
    }

    /**
     * @return текст ошибки или null, если пароль подходит.
     */
    public static String validate(String password) {

        if (password == null || password.length() < MIN_LENGTH) {
            return "Пароль должен быть не короче "
                    + MIN_LENGTH + " символов.";
        }

        if (password.length() > MAX_LENGTH) {
            return "Пароль не должен быть длиннее "
                    + MAX_LENGTH + " символов.";
        }

        if (password.chars().noneMatch(Character::isLetter)) {
            return "В пароле должна быть хотя бы одна буква.";
        }

        if (password.chars().noneMatch(Character::isDigit)) {
            return "В пароле должна быть хотя бы одна цифра.";
        }

        return null;
    }
}
