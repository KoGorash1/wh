
package com.wh.controller;

import com.wh.model.User;
import com.wh.repository.UserRepository;
import com.wh.service.PasswordPolicy;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ---------- Вход для всех пользователей ----------

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session) {

        User user = findUser(username);

        if (user == null || !passwordMatches(user, password)) {
            return "redirect:/login?error";
        }

        startSession(session, user);

        if (isStaffRole(user.getRole())) {
            return "redirect:/admin";
        }

        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout";
    }

    // ---------- Поиск пользователя ----------

    private User findUser(String login) {
        String value = login == null ? "" : login.trim();

        return userRepository.findByUsername(value)
                .or(() -> userRepository.findByEmail(value.toLowerCase()))
                .orElse(null);
    }

    // ---------- Проверка ролей ----------

    private static boolean isStaffRole(String role) {
        return "ADMIN".equalsIgnoreCase(role)
                || "STAFF".equalsIgnoreCase(role);
    }

    // ---------- Создание сессии ----------

    private void startSession(HttpSession session, User user) {
        session.setAttribute("username", user.getUsername());

        session.setAttribute(
                "isAdmin",
                "ADMIN".equalsIgnoreCase(user.getRole())
        );

        session.setAttribute("isStaff", isStaffRole(user.getRole()));
    }

    // ---------- Проверка пароля ----------

    private boolean passwordMatches(User user, String rawPassword) {
        String stored = user.getPassword();

        if (stored == null || rawPassword == null) {
            return false;
        }

        // Проверка BCrypt-хеша
        if (stored.startsWith("$2")) {
            return encoder.matches(rawPassword, stored);
        }

        // Поддержка старых паролей в открытом виде
        if (stored.equals(rawPassword)) {
            user.setPassword(encoder.encode(rawPassword));
            userRepository.save(user);
            return true;
        }

        return false;
    }

    // ---------- Регистрация ----------

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("user") User form,
                           Model model) {

        String fullName = clean(form.getFullName());
        String phone = normalizePhone(form.getPhone());
        String email = clean(form.getEmail()).toLowerCase();
        String username = clean(form.getUsername());

        String password = form.getPassword() == null
                ? ""
                : form.getPassword();

        String error = null;

        // Проверка ФИО
        if (fullName.length() < 5
                || fullName.split("\\s+").length < 2
                || !fullName.matches("\\p{L}[\\p{L}\\s.'-]*")) {

            error = "Введите ФИО: фамилию и имя (отчество при наличии), только буквами.";

        } else if (phone == null) {

            error = "Введите номер телефона, например +7 999 123-45-67.";

        } else if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {

            error = "Введите корректную почту, например name@mail.ru.";

        } else if (username.length() < 3) {

            error = "Логин должен быть не короче 3 символов.";

        } else if (PasswordPolicy.validate(password) != null) {

            error = PasswordPolicy.validate(password);

        } else if (userRepository.findByUsername(username).isPresent()) {

            error = "Такой логин уже занят.";

        } else if (userRepository.findByEmail(email).isPresent()) {

            error = "Пользователь с такой почтой уже зарегистрирован.";
        }

        if (error != null) {
            model.addAttribute("error", error);
            return "register";
        }

        // Создание нового пользователя
        User user = new User();

        user.setFullName(fullName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(encoder.encode(password));
        user.setRole("USER");

        userRepository.save(user);

        return "redirect:/login";
    }

    // ---------- Очистка данных ----------

    private static String clean(String value) {
        return value == null
                ? ""
                : value.trim().replaceAll("\\s+", " ");
    }

    // ---------- Нормализация телефона ----------

    private static String normalizePhone(String raw) {
        if (raw == null) {
            return null;
        }

        String digits = raw.replaceAll("\\D", "");

        if (digits.length() == 11 && digits.startsWith("8")) {
            digits = "7" + digits.substring(1);

        } else if (digits.length() == 10) {
            digits = "7" + digits;
        }

        if (digits.length() < 11 || digits.length() > 15) {
            return null;
        }

        return "+" + digits;
    }

    // ---------- Дополнительные маршруты ----------

    @GetMapping("/account/login")
    public String accountLogin() {
        return "redirect:/login";
    }

    @GetMapping("/account/register")
    public String accountRegister() {
        return "redirect:/register";
    }

    // Вход для администратора
    @GetMapping("/admin/login")
    public String adminLogin() {
        return "redirect:/login";
    }

    // ---------- Успешный заказ ----------

    @GetMapping("/order/success")
    public String orderSuccess() {
        return "order-success";
    }
}
