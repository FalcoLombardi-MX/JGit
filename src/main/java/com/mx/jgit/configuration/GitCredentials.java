package com.mx.jgit.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@ToString
public class GitCredentials {

    @Value("${git.username}")
    private String username;

    @Value("${git.password}")
    private String password;

    @Value("${git.token}")
    private String token;

    @Value("${git.url}")
    private String url;
}

