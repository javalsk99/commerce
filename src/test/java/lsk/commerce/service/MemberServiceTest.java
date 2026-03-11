package lsk.commerce.service;

import lsk.commerce.domain.Grade;
import lsk.commerce.domain.Member;
import lsk.commerce.dto.request.MemberRequest;
import lsk.commerce.dto.response.MemberResponse;
import lsk.commerce.repository.MemberRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.times;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    MemberRepository memberRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    MemberService memberService;

    String loginId = "id_A";
    String rawPassword = "12345678";
    String encodedPassword = "$2a$diioffd783294fkdj";
    String newEncodedPassword = "$2a$jgio2383dshnj987";

    @Nested
    class Join {

        @Nested
        class SuccessCase {

            @Test
            void user() {
                //given
                MemberRequest request = MemberRequest.builder()
                        .loginId(loginId)
                        .password(rawPassword)
                        .build();

                given(passwordEncoder.encode(anyString())).willReturn(encodedPassword);
                given(memberRepository.existsByLoginId(anyString())).willReturn(false);

                //when
                memberService.join(request);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(passwordEncoder).should().encode(anyString()));
                    softly.check(() -> BDDMockito.then(memberRepository).should().existsByLoginId(anyString()));
                    softly.check(() -> BDDMockito.then(memberRepository).should().save(argThat(m ->
                            m.getPassword().equals(encodedPassword) && m.getGrade() == Grade.USER)));
                });
            }

            @Test
            void admin() {
                //given
                MemberRequest request = MemberRequest.builder()
                        .loginId(loginId)
                        .password(rawPassword)
                        .build();

                given(passwordEncoder.encode(anyString())).willReturn(encodedPassword);
                given(memberRepository.existsByLoginId(anyString())).willReturn(false);

                //when
                memberService.adminJoin(request);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(passwordEncoder).should().encode(anyString()));
                    softly.check(() -> BDDMockito.then(memberRepository).should().existsByLoginId(anyString()));
                    softly.check(() -> BDDMockito.then(memberRepository).should().save(argThat(m -> m.getGrade() == Grade.ADMIN)));
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void existsLoginId() {
                //given
                MemberRequest request = MemberRequest.builder()
                        .loginId(loginId)
                        .password(rawPassword)
                        .build();

                given(passwordEncoder.encode(anyString())).willReturn(encodedPassword);
                given(memberRepository.existsByLoginId(anyString())).willReturn(true);

                //when & then
                thenThrownBy(() -> memberService.join(request))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("이미 사용 중인 아이디입니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(passwordEncoder).should().encode(anyString()));
                    softly.check(() -> BDDMockito.then(memberRepository).should().existsByLoginId(anyString()));
                    softly.check(() -> BDDMockito.then(memberRepository).should(never()).save(any()));
                });
            }

            @Test
            void passwordBlank() {
                //given
                MemberRequest request1 = MemberRequest.builder()
                        .password(null)
                        .build();
                MemberRequest request2 = MemberRequest.builder()
                        .password(" ")
                        .build();

                //when & then
                thenSoftly(softly -> {
                    softly.thenThrownBy(() -> memberService.join(request1))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("비밀번호가 비어있습니다");
                    softly.thenThrownBy(() -> memberService.join(request2))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("비밀번호가 비어있습니다");
                });

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(passwordEncoder).should(never()).encode(any()));
                    softly.check(() -> BDDMockito.then(memberRepository).should(never()).save(any()));
                });
            }

            @Test
            void rawPassword() {
                //given
                MemberRequest request = MemberRequest.builder()
                        .password(rawPassword)
                        .build();

                given(passwordEncoder.encode(anyString())).willReturn(rawPassword);

                //when & then
                thenThrownBy(() -> memberService.join(request))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("암호화되지 않은 비밀번호입니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(passwordEncoder).should().encode(anyString()));
                    softly.check(() -> BDDMockito.then(memberRepository).should(never()).save(any()));
                });
            }
        }
    }

    @Nested
    class Find {

        @Nested
        class SuccessCase {

            @Test
            void byLoginId() {
                //given
                Member member = Member.builder()
                        .loginId(loginId)
                        .build();

                given(memberRepository.findByLoginId(anyString())).willReturn(Optional.of(member));

                //when
                memberService.findMemberByLoginId(loginId);

                //then
                BDDMockito.then(memberRepository).should().findByLoginId(anyString());
            }

            @Test
            void forLogin() {
                //given
                Member member = Member.builder()
                        .loginId(loginId)
                        .build();

                given(memberRepository.findByLoginId(anyString())).willReturn(Optional.of(member));

                //when
                memberService.findMemberForLogin(loginId);

                //then
                BDDMockito.then(memberRepository).should().findByLoginId(anyString());
            }
        }

        @Nested
        class FailureCase {

            @Test
            void byLoginId_MemberNotFound() {
                //given
                given(memberRepository.findByLoginId(anyString())).willReturn(Optional.empty());

                //when & then
                thenThrownBy(() -> memberService.findMemberByLoginId(loginId))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("존재하지 않는 아이디입니다");

                //then
                BDDMockito.then(memberRepository).should().findByLoginId(anyString());
            }

            @Test
            void forLogin_MemberNotFound() {
                //given
                given(memberRepository.findByLoginId(anyString())).willReturn(Optional.empty());

                //when & then
                thenThrownBy(() -> memberService.findMemberForLogin(loginId))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("아이디 또는 비밀번호가 틀렸습니다");

                //then
                BDDMockito.then(memberRepository).should().findByLoginId(anyString());
            }
        }
    }

    @Nested
    class ChangePassword {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Member member = Member.builder()
                        .loginId(loginId)
                        .password(encodedPassword)
                        .build();

                given(memberRepository.findByLoginId(anyString())).willReturn(Optional.of(member));
                given(passwordEncoder.encode(anyString())).willReturn(newEncodedPassword);
                given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

                //when
                memberService.changePassword(loginId, "11111111");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberRepository).should().findByLoginId(anyString()));
                    softly.check(() -> BDDMockito.then(passwordEncoder).should().encode(anyString()));
                    softly.check(() -> BDDMockito.then(passwordEncoder).should().matches(anyString(), anyString()));
                    softly.then(member.getPassword()).isEqualTo(newEncodedPassword);
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void memberNotFound() {
                //given
                given(memberRepository.findByLoginId(anyString())).willReturn(Optional.empty());

                //when & then
                thenThrownBy(() -> memberService.changePassword(loginId, "11111111"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("존재하지 않는 아이디입니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberRepository).should().findByLoginId(anyString()));
                    softly.check(() -> BDDMockito.then(passwordEncoder).should(never()).encode(any()));
                    softly.check(() -> BDDMockito.then(passwordEncoder).should(never()).matches(any(), any()));
                });
            }
        }
    }

    @Nested
    class ChangeAddress {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Member member = Member.builder()
                        .loginId(loginId)
                        .city("Seoul")
                        .street("Gangnam")
                        .street("01234")
                        .build();

                given(memberRepository.findByLoginId(anyString())).willReturn(Optional.of(member));

                //when
                memberService.changeAddress(loginId, "Gyeonggi-do", "Gangbuk", "01235");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberRepository).should().findByLoginId(anyString()));
                    softly.then(member.getAddress())
                            .extracting("city", "street", "zipcode")
                            .contains("Gyeonggi-do", "Gangbuk", "01235");
                });
            }
        }
    }

    @Nested
    class Delete {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Member member = Member.builder()
                        .loginId(loginId)
                        .build();

                given(memberRepository.findByLoginId(anyString())).willReturn(Optional.of(member));

                //when
                memberService.deleteMember(loginId);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberRepository).should().findByLoginId(anyString()));
                    softly.check(() -> BDDMockito.then(memberRepository).should().delete(member));
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void memberNotFound() {
                //given
                given(memberRepository.findByLoginId(anyString())).willReturn(Optional.empty());

                //when & then
                thenThrownBy(() -> memberService.deleteMember(loginId))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("존재하지 않는 아이디입니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberRepository).should().findByLoginId(anyString()));
                    softly.check(() -> BDDMockito.then(memberRepository).should(never()).delete(any()));
                });
            }

            @Test
            void alreadyDeleted() {
                //given
                Member member = Member.builder()
                        .loginId(loginId)
                        .build();

                given(memberRepository.findByLoginId(anyString()))
                        .willReturn(Optional.of(member))
                        .willReturn(Optional.empty());

                //when 첫 번째 호출
                memberService.deleteMember(loginId);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberRepository).should(times(1)).findByLoginId(anyString()));
                    softly.check(() -> BDDMockito.then(memberRepository).should(times(1)).delete(member));
                });

                //when & then 두 번째 호출
                thenThrownBy(() -> memberService.deleteMember(loginId))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("존재하지 않는 아이디입니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(memberRepository).should(times(2)).findByLoginId(anyString()));
                    softly.check(() -> BDDMockito.then(memberRepository).should(times(1)).delete(any()));
                });
            }
        }
    }

    @Nested
    class ChangeDto {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Member member = Member.builder()
                        .loginId(loginId)
                        .build();

                //when
                MemberResponse memberDto = memberService.getMemberDto(member);

                //then
                thenSoftly(softly -> {
                    softly.then(memberDto.getLoginId()).isEqualTo(loginId);
                    softly.then(memberDto.getGrade()).isEqualTo(Grade.USER);
                });
            }
        }
    }
}