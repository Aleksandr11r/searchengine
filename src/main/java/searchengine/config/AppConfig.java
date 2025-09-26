package searchengine.config;

import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public RussianLuceneMorphology russianLuceneMorphology() throws Exception {
        return new RussianLuceneMorphology();
    }

    @Bean
    public EnglishLuceneMorphology englishLuceneMorphology() throws Exception {
        return new EnglishLuceneMorphology();
    }
}
