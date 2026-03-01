package lsk.commerce.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class InitialExtractorTest {

    @ParameterizedTest
    @CsvSource({
            "초성 추출기, ㅊㅅ ㅊㅊㄱ",
            "InitialExtractor, InitialExtractor",
            "ㄱㄴㄷㄹ, ㄱㄴㄷㄹ",
            "끝, ㄲ",
            "ㅏㅑㅓㅕ, ㅏㅑㅓㅕ",
            "가힣, ㄱㅎ",
            "dfkj가나34!@$$%다sidf@!Fds라, dfkjㄱㄴ34!@$$%ㄷsidf@!Fdsㄹ"
    })
    void initialExtractor(String input, String expected) {
        //when
        String result = InitialExtractor.extract(input);

        //then
        assertThat(result).isEqualTo(expected);
    }
}