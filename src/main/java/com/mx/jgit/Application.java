package com.mx.jgit;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan
public class Application {

    @Autowired
    static JGit jGit;

    public static void main(String[] args) throws GitAPIException, IOException {

        SpringApplication.run(Application.class, args);
        //jGit.sourceGit();
        //jGit.destinationGit();
    }
}
