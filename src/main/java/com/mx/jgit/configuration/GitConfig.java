package com.mx.jgit.configuration;

import lombok.Getter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@ToString
public class GitConfig {

    @Value("${git.config.destination.dir}")
    private String destinationDir;

    @Value("${git.config.source.dir}")
    private String sourceDir;

    @Value("${copy.source.dir}")
    private String copySourceDir;

    @Value("${copy.destination.dir}")
    private String copyDestinationDir;
}

