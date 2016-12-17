package fi.aalto.mobileoffloading.models;

public class LoginData {
    private String username;
    private String password;
    public String token;
    public LoginData(String login, String password) {
        this.username = login;
        this.password = password;
    }
}
