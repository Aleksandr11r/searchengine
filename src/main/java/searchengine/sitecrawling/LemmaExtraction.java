package searchengine.sitecrawling;


import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Component
@RequiredArgsConstructor
public class LemmaExtraction {
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "ЧАСТ", "IN", "CC", "RP", "UH"};
    private final RussianLuceneMorphology russianMorphology;
    private final EnglishLuceneMorphology englishMorphology;
    private static final String RUSSIAN_WORD_PATTERN = "^[а-я]+$";
    private static final String ENGLISH_WORD_PATTERN = "^[a-z]+$";
    public HashMap<String, Integer> searchLemma(String text) {
        Set <String> words = arrayContainsWords(text);
        HashMap<String, Integer> lemmas = new HashMap<>();
        for (String word : words) {
            if (isRussianWord(word)) {
                List<String> wordBaseForms = russianMorphology.getMorphInfo(word);
                if (anyWordBaseBelongToParticle(wordBaseForms)) {
                    continue;
                }
                List<String> normalForms = russianMorphology.getNormalForms(word);
                if (!normalForms.isEmpty()) {
                    String normalWord = normalForms.get(0);
                    if (normalWord.length() >= 3) {
                        lemmas.merge(normalWord, 1, Integer::sum);
                    }
                }
            } else if (isEnglishWord(word)) {
                List<String> wordBaseForms = englishMorphology.getMorphInfo(word);
                if (anyWordBaseBelongToParticle(wordBaseForms)) {
                    continue;
                }
                List<String> normalForms = englishMorphology.getNormalForms(word);
                if (!normalForms.isEmpty()) {
                    String normalWord = normalForms.get(0);
                    if (normalWord.length() >= 3) {
                        lemmas.merge(normalWord, 1, Integer::sum);
                    }
                }
            }
        }
        return lemmas;
    }

    private Set<String> arrayContainsWords(String content) {
        String text = Jsoup.parse(content).text();
        String[] wordsArray = text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-яa-z\\s])", " ")
                .trim()
                .split("\\s+");

        return Arrays.stream(wordsArray).
                filter(w -> w.length() > 2 && !w.isBlank()).
                collect(Collectors.toCollection(HashSet::new));
    }
    private boolean isRussianWord(String word) {
        return word.matches(RUSSIAN_WORD_PATTERN);
    }
    private boolean isEnglishWord(String word) {
        return word.matches(ENGLISH_WORD_PATTERN);
    }
    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }
    private boolean hasParticleProperty(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getLemmaSet(String text) {
        Set <String> textArray = arrayContainsWords(text);
        Set<String> lemmaSet = new HashSet<>();
        for (String word : textArray) {
            if (!word.isEmpty() && word.length() >= 3) {
                if (isRussianWord(word)) {
                    List<String> wordBaseFormsRus = russianMorphology.getMorphInfo(word);
                    if (anyWordBaseBelongToParticle(wordBaseFormsRus)) {
                        continue;
                    }
                    lemmaSet.addAll(russianMorphology.getNormalForms(word));
                }
                else if (isEnglishWord(word)) {
                    List<String> wordBaseFormsEng = englishMorphology.getMorphInfo(word);
                    if ( anyWordBaseBelongToParticle(wordBaseFormsEng)) {
                        continue;
                    }
                    lemmaSet.addAll(englishMorphology.getNormalForms(word));
                }
            }
        }
        return lemmaSet;
    }
}

