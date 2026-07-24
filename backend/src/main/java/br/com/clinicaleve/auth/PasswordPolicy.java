package br.com.clinicaleve.auth;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    public static final int MINIMUM_LENGTH = 10;

    public void validate(String password) {
        if (password == null || password.length() < MINIMUM_LENGTH || password.length() > 72) {
            throw new IllegalArgumentException("A senha deve ter entre 10 e 72 caracteres");
        }
        if (password.chars().noneMatch(Character::isUpperCase)
                || password.chars().noneMatch(Character::isLowerCase)
                || password.chars().noneMatch(Character::isDigit)
                || password.chars().noneMatch(value -> !Character.isLetterOrDigit(value))) {
            throw new IllegalArgumentException(
                    "Use uma senha com letra maiúscula, minúscula, número e caractere especial"
            );
        }
    }
}
