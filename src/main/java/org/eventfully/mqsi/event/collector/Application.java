package org.eventfully.mqsi.event.collector;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
@EnableAutoConfiguration(exclude = {GroovyTemplateAutoConfiguration.class, JmsAutoConfiguration.class})
public class Application {

    public static void main(String[] args) throws Exception {
        new SpringApplicationBuilder().sources(Application.class).run(args);
    }
}